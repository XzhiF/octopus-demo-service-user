package com.octopus.demo.userservice.dao;

import com.octopus.demo.userservice.model.Address;

import java.util.List;
import java.util.Optional;

public interface AddressDao {

    List<Address> findByUserId(Long userId);

    Optional<Address> findById(Long id);

    Optional<Address> findDefaultByUserId(Long userId);

    Address save(Address address);

    Optional<Address> update(Address address);

    boolean deleteById(Long id);
}