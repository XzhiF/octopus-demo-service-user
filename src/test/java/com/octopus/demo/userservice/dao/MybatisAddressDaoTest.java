package com.octopus.demo.userservice.dao;

import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import com.octopus.demo.userservice.dao.impl.MybatisAddressDao;
import com.octopus.demo.userservice.model.Address;
import com.octopus.demo.userservice.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MybatisAddressDaoTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("octopus_demo")
            .withUsername("octopus")
            .withPassword("octopus")
            .withInitScript("db/schema.sql");

    @Autowired
    private MybatisAddressDao addressDao;

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
    void saveAssignsGeneratedIdAndTimestamps() {
        Long userId = createUser("mysql_phase2_save");

        Address saved = addressDao.save(newAddress(userId, "张三", false));

        assertNotNull(saved.getId());
        assertTrue(saved.getId() > 0);
        assertEquals(userId, saved.getUserId());
        assertEquals("张三", saved.getReceiverName());
        assertEquals("13800138000", saved.getReceiverPhone());
        assertEquals("北京市", saved.getProvince());
        assertEquals("北京市", saved.getCity());
        assertEquals("朝阳区", saved.getDistrict());
        assertEquals("某某街道某某号", saved.getDetailAddress());
        assertEquals("100000", saved.getPostalCode());
        assertFalse(saved.getIsDefault());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void saveWithoutDefaultFlagStoresFalse() {
        Long userId = createUser("mysql_phase2_save_null");

        Address address = newAddress(userId, "张三", null);
        Address saved = addressDao.save(address);

        assertFalse(addressDao.findById(saved.getId()).orElseThrow().getIsDefault());
    }

    @Test
    void findByIdReturnsAddressWhenExistsAndEmptyWhenMissing() {
        Long userId = createUser("mysql_phase2_find_id");
        Address saved = addressDao.save(newAddress(userId, "张三", false));

        Optional<Address> found = addressDao.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals("张三", found.get().getReceiverName());

        assertTrue(addressDao.findById(Long.MAX_VALUE).isEmpty());
    }

    @Test
    void findByUserIdReturnsOnlyThatUsersAddressesOrderedByIdAscending() {
        Long userId = createUser("mysql_phase2_find_user");
        Long otherUserId = createUser("mysql_phase2_find_user_other");
        Address first = addressDao.save(newAddress(userId, "张三", false));
        Address second = addressDao.save(newAddress(userId, "李四", false));
        addressDao.save(newAddress(otherUserId, "王五", false));

        List<Address> addresses = addressDao.findByUserId(userId);

        assertEquals(2, addresses.size());
        assertEquals(List.of(first.getId(), second.getId()),
                addresses.stream().map(Address::getId).toList());
        assertTrue(addressDao.findByUserId(Long.MAX_VALUE).isEmpty());
    }

    @Test
    void paginationReturnsFirstPage() {
        Long userId = createUser("mysql_phase2_page_first");
        saveAddresses(userId, 5);

        PageResultBean<Address> result = addressDao.findByUserId(userId, pageQuery(1, 2));

        assertEquals(5, result.getCount());
        assertEquals(2, result.getList().size());
        assertTrue(result.getList().get(0).getId() < result.getList().get(1).getId());
    }

    @Test
    void paginationReturnsLastPartialPage() {
        Long userId = createUser("mysql_phase2_page_last");
        saveAddresses(userId, 5);

        PageResultBean<Address> result = addressDao.findByUserId(userId, pageQuery(3, 2));

        assertEquals(5, result.getCount());
        assertEquals(1, result.getList().size());
    }

    @Test
    void paginationBeyondRangeReturnsEmptyListKeepingCount() {
        Long userId = createUser("mysql_phase2_page_beyond");
        saveAddresses(userId, 5);

        PageResultBean<Address> result = addressDao.findByUserId(userId, pageQuery(4, 2));

        assertEquals(5, result.getCount());
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void updateChangesFieldsAndRefreshesUpdatedAt() {
        Long userId = createUser("mysql_phase2_update");
        Address saved = addressDao.save(newAddress(userId, "旧名", false));
        jdbcTemplate.update("UPDATE addresses SET updated_at = '2000-01-01 00:00:00' WHERE id = ?", saved.getId());

        saved.setReceiverName("新名");
        saved.setReceiverPhone("13900139000");
        Optional<Address> updated = addressDao.update(saved);

        assertTrue(updated.isPresent());
        assertEquals("新名", updated.get().getReceiverName());
        assertEquals("13900139000", updated.get().getReceiverPhone());
        assertTrue(updated.get().getUpdatedAt().isAfter(LocalDateTime.of(2000, 1, 1, 0, 0, 0)));
    }

    @Test
    void updateReturnsEmptyWhenAddressDoesNotExist() {
        Long userId = createUser("mysql_phase2_update_missing");
        Address missing = newAddress(userId, "不存在", false);
        missing.setId(Long.MAX_VALUE);

        assertTrue(addressDao.update(missing).isEmpty());
    }

    @Test
    void deleteByIdReturnsTrueWhenDeletedAndFalseOtherwise() {
        Long userId = createUser("mysql_phase2_delete");
        Address saved = addressDao.save(newAddress(userId, "张三", false));

        assertTrue(addressDao.deleteById(saved.getId()));
        assertTrue(addressDao.findById(saved.getId()).isEmpty());
        assertFalse(addressDao.deleteById(saved.getId()));
    }

    @Test
    void findDefaultByUserIdReturnsEmptyWhenNoDefault() {
        Long userId = createUser("mysql_phase2_default_none");
        addressDao.save(newAddress(userId, "张三", false));

        assertTrue(addressDao.findDefaultByUserId(userId).isEmpty());
        assertTrue(addressDao.findDefaultByUserId(Long.MAX_VALUE).isEmpty());
    }

    @Test
    void findDefaultByUserIdReturnsDefaultAddress() {
        Long userId = createUser("mysql_phase2_default_found");
        addressDao.save(newAddress(userId, "张三", false));
        Address defaultAddress = addressDao.save(newAddress(userId, "李四", true));

        Optional<Address> found = addressDao.findDefaultByUserId(userId);

        assertTrue(found.isPresent());
        assertEquals(defaultAddress.getId(), found.get().getId());
        assertEquals("李四", found.get().getReceiverName());
    }

    @Test
    void deletingUserCascadesAddresses() {
        Long userId = createUser("mysql_phase2_cascade");
        addressDao.save(newAddress(userId, "张三", false));
        addressDao.save(newAddress(userId, "李四", true));

        assertTrue(userDao.deleteById(userId));

        Long remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM addresses WHERE user_id = ?", Long.class, userId);
        assertEquals(0L, remaining);
    }

    private Long createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setNickname("地址测试");
        user.setEmail(username + "@example.com");
        user.setPasswordHash("hash-value");
        return userDao.save(user).getId();
    }

    private void saveAddresses(Long userId, int count) {
        for (int i = 1; i <= count; i++) {
            addressDao.save(newAddress(userId, "收件人" + i, false));
        }
    }

    private static PageQueryBean pageQuery(int page, int size) {
        PageQueryBean query = new PageQueryBean();
        query.setPage(page);
        query.setSize(size);
        return query;
    }

    private static Address newAddress(Long userId, String receiverName, Boolean isDefault) {
        Address address = new Address();
        address.setUserId(userId);
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
