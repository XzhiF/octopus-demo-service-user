package com.octopus.demo.userservice.dao;

import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import com.octopus.demo.userservice.model.Address;

import java.util.List;
import java.util.Optional;

public interface AddressDao {

    List<Address> findByUserId(Long userId);

    PageResultBean<Address> findByUserId(Long userId, PageQueryBean query);

    Optional<Address> findById(Long id);

    Optional<Address> findDefaultByUserId(Long userId);

    Address save(Address address);

    Optional<Address> update(Address address);

    boolean deleteById(Long id);
}
