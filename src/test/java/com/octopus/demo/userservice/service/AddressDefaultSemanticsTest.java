package com.octopus.demo.userservice.service;

import com.octopus.demo.userservice.dao.UserDao;
import com.octopus.demo.userservice.model.Address;
import com.octopus.demo.userservice.model.User;
import com.octopus.demo.userservice.service.impl.AddressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AddressDefaultSemanticsTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("octopus_demo")
            .withUsername("octopus")
            .withPassword("octopus")
            .withInitScript("db/schema.sql");

    @Autowired
    private AddressService addressService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.execute("DELETE FROM addresses");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    void onlyWriteMethodsAreTransactional() throws Exception {
        assertNull(AddressServiceImpl.class.getAnnotation(Transactional.class),
                "事务应加在写方法上, 而非类级");
        for (String name : List.of("createAddress", "updateAddress", "deleteAddress", "setDefaultAddress")) {
            Method method = findMethod(name);
            assertNotNull(method.getAnnotation(Transactional.class), name + " 应标注 @Transactional");
        }
        for (String name : List.of("findByUserId", "findById")) {
            for (Method method : AddressServiceImpl.class.getDeclaredMethods()) {
                if (method.getName().equals(name)) {
                    assertNull(method.getAnnotation(Transactional.class), name + " 不应标注 @Transactional");
                }
            }
        }
    }

    @Test
    void savingAnotherDefaultClearsPreviousDefault() {
        Long userId = newUser("mysql_phase2_default_save", "defsave@example.com");

        Address first = addressService.createAddress(userId, newAddress("Receiver A", true)).orElseThrow();
        Address second = addressService.createAddress(userId, newAddress("Receiver B", true)).orElseThrow();

        assertEquals(List.of(second.getId()), defaultIdsOf(userId));
        assertEquals(0, defaultFlagOf(first.getId()));
        assertEquals(1, defaultFlagOf(second.getId()));
    }

    @Test
    void savingNonDefaultAddressKeepsExistingDefault() {
        Long userId = newUser("mysql_phase2_default_keep", "defkeep@example.com");

        Address first = addressService.createAddress(userId, newAddress("Receiver A", true)).orElseThrow();
        Address second = addressService.createAddress(userId, newAddress("Receiver B", false)).orElseThrow();

        assertEquals(List.of(first.getId()), defaultIdsOf(userId));
        assertEquals(0, defaultFlagOf(second.getId()));
    }

    @Test
    void updatingAddressToDefaultClearsPreviousDefault() {
        Long userId = newUser("mysql_phase2_default_update", "defupdate@example.com");
        Address first = addressService.createAddress(userId, newAddress("Receiver A", true)).orElseThrow();
        Address second = addressService.createAddress(userId, newAddress("Receiver B", false)).orElseThrow();

        Address patch = new Address();
        patch.setIsDefault(true);
        assertTrue(addressService.updateAddress(second.getId(), userId, patch).isPresent());

        assertEquals(List.of(second.getId()), defaultIdsOf(userId));
        assertEquals(0, defaultFlagOf(first.getId()));
    }

    @Test
    void settingDefaultSwitchesDefaultWithinSameUser() {
        Long userId = newUser("mysql_phase2_default_switch", "defswitch@example.com");
        Address first = addressService.createAddress(userId, newAddress("Receiver A", true)).orElseThrow();
        Address second = addressService.createAddress(userId, newAddress("Receiver B", false)).orElseThrow();

        Optional<Address> result = addressService.setDefaultAddress(second.getId(), userId);

        assertTrue(result.isPresent());
        assertTrue(result.get().getIsDefault());
        assertEquals(List.of(second.getId()), defaultIdsOf(userId));
        assertEquals(0, defaultFlagOf(first.getId()));
        assertEquals(1, defaultFlagOf(second.getId()));
    }

    @Test
    void deletingDefaultPromotesSmallestIdRemainingAddress() {
        Long userId = newUser("mysql_phase2_default_delete", "defdelete@example.com");
        Address first = addressService.createAddress(userId, newAddress("Receiver A", true)).orElseThrow();
        Address second = addressService.createAddress(userId, newAddress("Receiver B", false)).orElseThrow();
        Address third = addressService.createAddress(userId, newAddress("Receiver C", false)).orElseThrow();

        assertTrue(addressService.deleteAddress(first.getId(), userId));

        assertEquals(List.of(second.getId()), defaultIdsOf(userId));
        assertEquals(0, defaultFlagOf(third.getId()));
        assertTrue(second.getId() < third.getId());
    }

    @Test
    void deletingNonDefaultKeepsCurrentDefault() {
        Long userId = newUser("mysql_phase2_default_nodelete", "defnodelete@example.com");
        Address first = addressService.createAddress(userId, newAddress("Receiver A", true)).orElseThrow();
        Address second = addressService.createAddress(userId, newAddress("Receiver B", false)).orElseThrow();

        assertTrue(addressService.deleteAddress(second.getId(), userId));

        assertEquals(List.of(first.getId()), defaultIdsOf(userId));
        assertEquals(1, defaultFlagOf(first.getId()));
    }

    @Test
    void deletingLastDefaultLeavesNoDefault() {
        Long userId = newUser("mysql_phase2_default_last", "deflast@example.com");
        Address only = addressService.createAddress(userId, newAddress("Receiver A", true)).orElseThrow();

        assertTrue(addressService.deleteAddress(only.getId(), userId));

        assertTrue(defaultIdsOf(userId).isEmpty());
    }

    @Test
    void defaultChangesAreScopedPerUser() {
        Long firstUser = newUser("mysql_phase2_default_scope_a", "scopea@example.com");
        Long secondUser = newUser("mysql_phase2_default_scope_b", "scopeb@example.com");
        addressService.createAddress(firstUser, newAddress("Receiver A1", true)).orElseThrow();
        Address firstUserOther = addressService.createAddress(firstUser, newAddress("Receiver A2", false)).orElseThrow();
        Address secondUserDefault = addressService.createAddress(secondUser, newAddress("Receiver B1", true)).orElseThrow();

        assertTrue(addressService.setDefaultAddress(firstUserOther.getId(), firstUser).isPresent());

        assertEquals(List.of(firstUserOther.getId()), defaultIdsOf(firstUser));
        assertEquals(List.of(secondUserDefault.getId()), defaultIdsOf(secondUser));
        assertEquals(1, defaultFlagOf(secondUserDefault.getId()));
    }

    @Test
    void crossUserOperationsOnForeignAddressReturnEmptyAndLeaveDataUntouched() {
        Long owner = newUser("mysql_phase2_default_owner", "owner@example.com");
        Long intruder = newUser("mysql_phase2_default_intruder", "intruder@example.com");
        Address owned = addressService.createAddress(owner, newAddress("Receiver A", true)).orElseThrow();
        Address other = addressService.createAddress(owner, newAddress("Receiver B", false)).orElseThrow();

        Address patch = new Address();
        patch.setReceiverName("Hijacked");
        assertTrue(addressService.updateAddress(owned.getId(), intruder, patch).isEmpty());
        assertTrue(addressService.setDefaultAddress(other.getId(), intruder).isEmpty());
        assertFalse(addressService.deleteAddress(owned.getId(), intruder));

        assertEquals(List.of(owned.getId()), defaultIdsOf(owner));
        assertEquals("Receiver A", receiverNameOf(owned.getId()));
        assertEquals(2, countAddressesOf(owner));
    }

    @Test
    void missingAddressReturnsEmpty() {
        Long userId = newUser("mysql_phase2_default_missing", "missing@example.com");

        assertTrue(addressService.updateAddress(Long.MAX_VALUE, userId, new Address()).isEmpty());
        assertTrue(addressService.setDefaultAddress(Long.MAX_VALUE, userId).isEmpty());
        assertFalse(addressService.deleteAddress(Long.MAX_VALUE, userId));
    }

    private List<Long> defaultIdsOf(Long userId) {
        return jdbcTemplate.queryForList(
                "SELECT id FROM addresses WHERE user_id = ? AND is_default = 1 ORDER BY id ASC",
                Long.class, userId);
    }

    private int defaultFlagOf(Long addressId) {
        Integer flag = jdbcTemplate.queryForObject(
                "SELECT is_default FROM addresses WHERE id = ?", Integer.class, addressId);
        return flag == null ? -1 : flag;
    }

    private String receiverNameOf(Long addressId) {
        return jdbcTemplate.queryForObject(
                "SELECT receiver_name FROM addresses WHERE id = ?", String.class, addressId);
    }

    private int countAddressesOf(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM addresses WHERE user_id = ?", Integer.class, userId);
        return count == null ? -1 : count;
    }

    private Method findMethod(String name) {
        for (Method method : AddressServiceImpl.class.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new IllegalStateException("未找到方法: " + name);
    }

    private Long newUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setNickname(username);
        user.setEmail(email);
        user.setAge(30);
        user.setPasswordHash("hash-value");
        return userDao.save(user).getId();
    }

    private Address newAddress(String receiverName, boolean isDefault) {
        Address address = new Address();
        address.setReceiverName(receiverName);
        address.setReceiverPhone("13800138000");
        address.setProvince("北京市");
        address.setCity("北京市");
        address.setDistrict("朝阳区");
        address.setDetailAddress("某某街道某某号");
        address.setPostalCode("100000");
        address.setIsDefault(isDefault);
        return address;
    }
}
