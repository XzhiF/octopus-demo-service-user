package com.octopus.demo.userservice.service;

import com.octopus.demo.userservice.dao.AddressDao;
import com.octopus.demo.userservice.dao.UserDao;
import com.octopus.demo.userservice.model.Address;
import com.octopus.demo.userservice.model.User;
import com.octopus.demo.userservice.service.impl.AddressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private UserDao userDao;

    @Mock
    private AddressDao addressDao;

    private AddressServiceImpl addressService;

    @BeforeEach
    void setUp() {
        addressService = new AddressServiceImpl(userDao, addressDao);
    }

    @Test
    void shouldFindByUserId() {
        var address = createAddress(1L, 10L, "Zhang San", "13800138000",
                "Beijing", "Beijing", "Haidian", "No.1 Road", "100000", false);
        when(addressDao.findByUserId(10L)).thenReturn(List.of(address));

        List<Address> addresses = addressService.findByUserId(10L);
        assertEquals(1, addresses.size());
        assertEquals("Zhang San", addresses.get(0).getReceiverName());
        verify(addressDao).findByUserId(10L);
    }

    @Test
    void shouldFindById() {
        var address = createAddress(1L, 10L, "Zhang San", "13800138000",
                "Beijing", "Beijing", "Haidian", "No.1 Road", "100000", false);
        when(addressDao.findById(1L)).thenReturn(Optional.of(address));

        Optional<Address> found = addressService.findById(1L);
        assertTrue(found.isPresent());
        assertEquals("Zhang San", found.get().getReceiverName());
    }

    @Test
    void shouldReturnEmptyWhenAddressNotFound() {
        when(addressDao.findById(999L)).thenReturn(Optional.empty());

        Optional<Address> found = addressService.findById(999L);
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldCreateAddress() {
        var user = createUser(10L, "testuser", "test@example.com");
        var newAddress = createAddress(null, null, "Zhang San", "13800138000",
                "Beijing", "Beijing", "Haidian", "No.1 Road", "100000", false);
        var savedAddress = createAddress(1L, 10L, "Zhang San", "13800138000",
                "Beijing", "Beijing", "Haidian", "No.1 Road", "100000", false);
        when(userDao.findById(10L)).thenReturn(Optional.of(user));
        when(addressDao.save(any(Address.class))).thenReturn(savedAddress);

        Optional<Address> result = addressService.createAddress(10L, newAddress);
        assertTrue(result.isPresent());
        assertEquals(10L, result.get().getUserId());
        verify(addressDao).save(any(Address.class));
    }

    @Test
    void shouldReturnEmptyWhenCreatingAddressForNonExistentUser() {
        var newAddress = createAddress(null, null, "Zhang San", "13800138000",
                "Beijing", "Beijing", "Haidian", "No.1 Road", "100000", false);
        when(userDao.findById(999L)).thenReturn(Optional.empty());

        Optional<Address> result = addressService.createAddress(999L, newAddress);
        assertTrue(result.isEmpty());
        verify(addressDao, never()).save(any());
    }

    @Test
    void shouldUpdateAddress() {
        var existing = createAddress(1L, 10L, "Zhang San", "13800138000",
                "Beijing", "Beijing", "Haidian", "No.1 Road", "100000", false);
        var updated = createAddress(1L, 10L, "Li Si", "13900139000",
                "Shanghai", "Shanghai", "Pudong", "No.2 Road", "200000", false);
        when(addressDao.findById(1L)).thenReturn(Optional.of(existing));
        when(addressDao.update(any(Address.class))).thenReturn(Optional.of(updated));

        Optional<Address> result = addressService.updateAddress(1L, 10L, updated);
        assertTrue(result.isPresent());
        assertEquals("Li Si", result.get().getReceiverName());
        verify(addressDao).update(any(Address.class));
    }

    @Test
    void shouldReturnEmptyWhenUpdatingAddressNotBelongingToUser() {
        var existing = createAddress(1L, 10L, "Zhang San", "13800138000",
                "Beijing", "Beijing", "Haidian", "No.1 Road", "100000", false);
        var updated = createAddress(1L, 20L, "Li Si", "13900139000",
                "Shanghai", "Shanghai", "Pudong", "No.2 Road", "200000", false);
        when(addressDao.findById(1L)).thenReturn(Optional.of(existing));

        Optional<Address> result = addressService.updateAddress(1L, 20L, updated);
        assertTrue(result.isEmpty());
        verify(addressDao, never()).update(any());
    }

    @Test
    void shouldReturnEmptyWhenUpdatingNonExistentAddress() {
        when(addressDao.findById(999L)).thenReturn(Optional.empty());

        Optional<Address> result = addressService.updateAddress(999L, 10L, new Address());
        assertTrue(result.isEmpty());
        verify(addressDao, never()).update(any());
    }

    @Test
    void shouldDeleteAddress() {
        var existing = createAddress(1L, 10L, "Zhang San", "13800138000",
                "Beijing", "Beijing", "Haidian", "No.1 Road", "100000", false);
        when(addressDao.findById(1L)).thenReturn(Optional.of(existing));
        when(addressDao.deleteById(1L)).thenReturn(true);

        boolean result = addressService.deleteAddress(1L, 10L);
        assertTrue(result);
        verify(addressDao).deleteById(1L);
    }

    @Test
    void shouldReturnFalseWhenDeletingAddressNotBelongingToUser() {
        var existing = createAddress(1L, 10L, "Zhang San", "13800138000",
                "Beijing", "Beijing", "Haidian", "No.1 Road", "100000", false);
        when(addressDao.findById(1L)).thenReturn(Optional.of(existing));

        boolean result = addressService.deleteAddress(1L, 20L);
        assertFalse(result);
        verify(addressDao, never()).deleteById(any());
    }

    @Test
    void shouldReturnFalseWhenDeletingNonExistentAddress() {
        when(addressDao.findById(999L)).thenReturn(Optional.empty());

        boolean result = addressService.deleteAddress(999L, 10L);
        assertFalse(result);
        verify(addressDao, never()).deleteById(any());
    }

    @Test
    void shouldSetDefaultAddress() {
        var existing = createAddress(1L, 10L, "Zhang San", "13800138000",
                "Beijing", "Beijing", "Haidian", "No.1 Road", "100000", false);
        var defaulted = createAddress(1L, 10L, "Zhang San", "13800138000",
                "Beijing", "Beijing", "Haidian", "No.1 Road", "100000", true);
        when(addressDao.findById(1L)).thenReturn(Optional.of(existing));
        when(addressDao.update(any(Address.class))).thenReturn(Optional.of(defaulted));

        Optional<Address> result = addressService.setDefaultAddress(1L, 10L);
        assertTrue(result.isPresent());
        assertTrue(result.get().getIsDefault());
        verify(addressDao).update(any(Address.class));
    }

    @Test
    void shouldReturnEmptyWhenSettingDefaultAddressNotBelongingToUser() {
        var existing = createAddress(1L, 10L, "Zhang San", "13800138000",
                "Beijing", "Beijing", "Haidian", "No.1 Road", "100000", false);
        when(addressDao.findById(1L)).thenReturn(Optional.of(existing));

        Optional<Address> result = addressService.setDefaultAddress(1L, 20L);
        assertTrue(result.isEmpty());
        verify(addressDao, never()).update(any());
    }

    private Address createAddress(Long id, Long userId, String receiverName, String receiverPhone,
                                  String province, String city, String district,
                                  String detailAddress, String postalCode, Boolean isDefault) {
        return new Address(id, userId, receiverName, receiverPhone, province, city,
                district, detailAddress, postalCode, isDefault,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private User createUser(Long id, String username, String email) {
        return new User(id, username, email, 25,
                LocalDateTime.now(), LocalDateTime.now());
    }
}