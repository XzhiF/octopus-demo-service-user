package com.octopus.demo.userservice.dao.impl;

import com.octopus.demo.userservice.dao.AddressDao;
import com.octopus.demo.userservice.model.Address;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryAddressDao implements AddressDao {

    private final ConcurrentHashMap<Long, Address> addressStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> defaultAddressMap = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public List<Address> findByUserId(Long userId) {
        List<Address> result = new ArrayList<>();
        for (Address address : addressStore.values()) {
            if (address.getUserId().equals(userId)) {
                result.add(address);
            }
        }
        return result;
    }

    @Override
    public Optional<Address> findById(Long id) {
        return Optional.ofNullable(addressStore.get(id));
    }

    @Override
    public Optional<Address> findDefaultByUserId(Long userId) {
        Long defaultAddressId = defaultAddressMap.get(userId);
        if (defaultAddressId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(addressStore.get(defaultAddressId));
    }

    @Override
    public Address save(Address address) {
        if (address.getIsDefault() == null) {
            address.setIsDefault(false);
        }

        Long id = idGenerator.getAndIncrement();
        address.setId(id);
        LocalDateTime now = LocalDateTime.now();
        address.setCreatedAt(now);
        address.setUpdatedAt(now);

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            unsetPreviousDefault(address.getUserId());
            defaultAddressMap.put(address.getUserId(), id);
        }

        addressStore.put(id, address);
        return address;
    }

    @Override
    public Optional<Address> update(Address address) {
        if (!addressStore.containsKey(address.getId())) {
            return Optional.empty();
        }

        address.setUpdatedAt(LocalDateTime.now());

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            unsetPreviousDefault(address.getUserId());
            defaultAddressMap.put(address.getUserId(), address.getId());
        } else {
            Long currentDefaultId = defaultAddressMap.get(address.getUserId());
            if (currentDefaultId != null && currentDefaultId.equals(address.getId())) {
                defaultAddressMap.remove(address.getUserId());
            }
        }

        addressStore.put(address.getId(), address);
        return Optional.of(address);
    }

    @Override
    public boolean deleteById(Long id) {
        Address removed = addressStore.remove(id);
        if (removed == null) {
            return false;
        }

        Long currentDefaultId = defaultAddressMap.get(removed.getUserId());
        if (currentDefaultId != null && currentDefaultId.equals(id)) {
            defaultAddressMap.remove(removed.getUserId());
        }

        return true;
    }

    private void unsetPreviousDefault(Long userId) {
        Long previousDefaultId = defaultAddressMap.get(userId);
        if (previousDefaultId != null) {
            Address previousDefault = addressStore.get(previousDefaultId);
            if (previousDefault != null) {
                previousDefault.setIsDefault(false);
                addressStore.put(previousDefaultId, previousDefault);
            }
        }
    }
}