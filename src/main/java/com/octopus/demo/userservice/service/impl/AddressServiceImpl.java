package com.octopus.demo.userservice.service.impl;

import com.octopus.demo.userservice.dao.AddressDao;
import com.octopus.demo.userservice.dao.UserDao;
import com.octopus.demo.userservice.model.Address;
import com.octopus.demo.userservice.service.AddressService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AddressServiceImpl implements AddressService {

    private final UserDao userDao;
    private final AddressDao addressDao;

    public AddressServiceImpl(UserDao userDao, AddressDao addressDao) {
        this.userDao = userDao;
        this.addressDao = addressDao;
    }

    @Override
    public List<Address> findByUserId(Long userId) {
        return addressDao.findByUserId(userId);
    }

    @Override
    public Optional<Address> findById(Long id) {
        return addressDao.findById(id);
    }

    @Override
    public Optional<Address> createAddress(Long userId, Address address) {
        if (userDao.findById(userId).isEmpty()) {
            return Optional.empty();
        }
        address.setUserId(userId);
        Address saved = addressDao.save(address);
        return Optional.of(saved);
    }

    @Override
    public Optional<Address> updateAddress(Long id, Long userId, Address address) {
        Optional<Address> existing = addressDao.findById(id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        if (!existing.get().getUserId().equals(userId)) {
            return Optional.empty();
        }
        address.setId(id);
        address.setUserId(userId);
        return addressDao.update(address);
    }

    @Override
    public boolean deleteAddress(Long id, Long userId) {
        Optional<Address> existing = addressDao.findById(id);
        if (existing.isEmpty()) {
            return false;
        }
        if (!existing.get().getUserId().equals(userId)) {
            return false;
        }
        boolean wasDefault = Boolean.TRUE.equals(existing.get().getIsDefault());
        boolean deleted = addressDao.deleteById(id);
        if (deleted && wasDefault) {
            autoSetNextDefault(userId);
        }
        return deleted;
    }

    private void autoSetNextDefault(Long userId) {
        List<Address> remaining = addressDao.findByUserId(userId);
        if (!remaining.isEmpty()) {
            Address nextDefault = remaining.get(0);
            nextDefault.setIsDefault(true);
            addressDao.update(nextDefault);
        }
    }

    @Override
    public Optional<Address> setDefaultAddress(Long id, Long userId) {
        Optional<Address> existing = addressDao.findById(id);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        if (!existing.get().getUserId().equals(userId)) {
            return Optional.empty();
        }
        Address address = existing.get();
        address.setIsDefault(true);
        return addressDao.update(address);
    }
}