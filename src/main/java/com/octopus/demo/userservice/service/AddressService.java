package com.octopus.demo.userservice.service;

import com.octopus.demo.userservice.model.Address;

import java.util.List;
import java.util.Optional;

public interface AddressService {

    List<Address> findByUserId(Long userId);

    Optional<Address> findById(Long id);

    Optional<Address> createAddress(Long userId, Address address);

    Optional<Address> updateAddress(Long id, Long userId, Address address);

    boolean deleteAddress(Long id, Long userId);

    Optional<Address> setDefaultAddress(Long id, Long userId);
}