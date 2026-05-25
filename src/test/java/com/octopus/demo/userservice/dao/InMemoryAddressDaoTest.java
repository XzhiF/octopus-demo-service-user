package com.octopus.demo.userservice.dao;

import com.octopus.demo.userservice.dao.impl.InMemoryAddressDao;
import com.octopus.demo.userservice.model.Address;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryAddressDaoTest {

    private InMemoryAddressDao addressDao;

    @BeforeEach
    void setUp() {
        addressDao = new InMemoryAddressDao();
    }

    private Address createAddress(Long userId, String receiverName, Boolean isDefault) {
        Address address = new Address();
        address.setUserId(userId);
        address.setReceiverName(receiverName);
        address.setReceiverPhone("13800138000");
        address.setProvince("北京市");
        address.setCity("北京市");
        address.setDistrict("朝阳区");
        address.setDetailAddress("某某街道某某号");
        address.setIsDefault(isDefault);
        return address;
    }

    @Test
    void shouldSaveAndFindAddress() {
        Address address = createAddress(1L, "张三", true);

        Address saved = addressDao.save(address);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        List<Address> found = addressDao.findByUserId(1L);
        assertEquals(1, found.size());
        assertEquals("张三", found.get(0).getReceiverName());
    }

    @Test
    void shouldFindAllAddressesByUserId() {
        Address address1 = createAddress(1L, "张三", true);
        Address address2 = createAddress(1L, "李四", false);
        Address address3 = createAddress(2L, "王五", true);

        addressDao.save(address1);
        addressDao.save(address2);
        addressDao.save(address3);

        List<Address> user1Addresses = addressDao.findByUserId(1L);
        assertEquals(2, user1Addresses.size());

        List<Address> user2Addresses = addressDao.findByUserId(2L);
        assertEquals(1, user2Addresses.size());
        assertEquals("王五", user2Addresses.get(0).getReceiverName());
    }

    @Test
    void shouldReturnEmptyWhenAddressNotFound() {
        Optional<Address> found = addressDao.findById(999L);
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldFindDefaultAddressByUserId() {
        Address address1 = createAddress(1L, "张三", true);
        Address address2 = createAddress(1L, "李四", false);

        addressDao.save(address1);
        addressDao.save(address2);

        Optional<Address> defaultAddress = addressDao.findDefaultByUserId(1L);
        assertTrue(defaultAddress.isPresent());
        assertEquals("张三", defaultAddress.get().getReceiverName());
    }

    @Test
    void shouldUpdateAddress() {
        Address address = createAddress(1L, "张三", false);
        Address saved = addressDao.save(address);

        saved.setReceiverName("张三丰");
        saved.setIsDefault(true);
        Optional<Address> updated = addressDao.update(saved);

        assertTrue(updated.isPresent());
        assertEquals("张三丰", updated.get().getReceiverName());
        assertTrue(updated.get().getIsDefault());
    }

    @Test
    void shouldReturnEmptyWhenUpdatingNonExistentAddress() {
        Address address = createAddress(1L, "幽灵", false);
        address.setId(999L);

        Optional<Address> updated = addressDao.update(address);
        assertTrue(updated.isEmpty());
    }

    @Test
    void shouldDeleteAddress() {
        Address address = createAddress(1L, "张三", false);
        Address saved = addressDao.save(address);

        boolean deleted = addressDao.deleteById(saved.getId());
        assertTrue(deleted);
        assertTrue(addressDao.findById(saved.getId()).isEmpty());
        assertTrue(addressDao.findByUserId(1L).isEmpty());
    }

    @Test
    void shouldReturnFalseWhenDeletingNonExistentAddress() {
        boolean deleted = addressDao.deleteById(999L);
        assertFalse(deleted);
    }

    @Test
    void shouldGenerateIncrementalIds() {
        Address address1 = createAddress(1L, "张三", false);
        Address address2 = createAddress(1L, "李四", false);

        Address saved1 = addressDao.save(address1);
        Address saved2 = addressDao.save(address2);

        assertEquals(1L, saved1.getId());
        assertEquals(2L, saved2.getId());
    }

    @Test
    void shouldMutuallyExclusiveDefaultAddress() {
        Address address1 = createAddress(1L, "张三", true);
        Address address2 = createAddress(1L, "李四", false);

        Address saved1 = addressDao.save(address1);
        addressDao.save(address2);

        // 张三 is default
        Optional<Address> defaultAddress = addressDao.findDefaultByUserId(1L);
        assertTrue(defaultAddress.isPresent());
        assertEquals(saved1.getId(), defaultAddress.get().getId());

        // Now set 李四 as default via update
        saved1.setIsDefault(false);
        addressDao.update(saved1);
        address2.setIsDefault(true);
        // address2 needs its id - it was the second save
        Address saved2 = addressDao.findByUserId(1L).stream()
                .filter(a -> a.getReceiverName().equals("李四"))
                .findFirst()
                .orElseThrow();
        saved2.setIsDefault(true);
        addressDao.update(saved2);

        // 李四 should now be default, 张三 should not
        Optional<Address> newDefault = addressDao.findDefaultByUserId(1L);
        assertTrue(newDefault.isPresent());
        assertEquals(saved2.getId(), newDefault.get().getId());

        // Verify 张三 is no longer default
        Address zhangSan = addressDao.findById(saved1.getId()).orElseThrow();
        assertFalse(zhangSan.getIsDefault());
    }

    @Test
    void shouldMutuallyExclusiveDefaultAddressOnSave() {
        Address address1 = createAddress(1L, "张三", true);
        Address saved1 = addressDao.save(address1);

        // Saving a second default address for the same user should unset the first
        Address address2 = createAddress(1L, "李四", true);
        Address saved2 = addressDao.save(address2);

        // The new default should be 李四
        Optional<Address> defaultAddress = addressDao.findDefaultByUserId(1L);
        assertTrue(defaultAddress.isPresent());
        assertEquals(saved2.getId(), defaultAddress.get().getId());

        // 张三 should no longer be default
        Address zhangSan = addressDao.findById(saved1.getId()).orElseThrow();
        assertFalse(zhangSan.getIsDefault());
    }

    @Test
    void shouldRemoveDefaultMappingWhenDeletingDefaultAddress() {
        Address address = createAddress(1L, "张三", true);
        Address saved = addressDao.save(address);

        assertTrue(addressDao.findDefaultByUserId(1L).isPresent());

        addressDao.deleteById(saved.getId());

        assertTrue(addressDao.findDefaultByUserId(1L).isEmpty());
    }
}