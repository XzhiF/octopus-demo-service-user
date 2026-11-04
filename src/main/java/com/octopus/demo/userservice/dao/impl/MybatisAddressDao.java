package com.octopus.demo.userservice.dao.impl;

import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import com.octopus.demo.userservice.dao.AddressDao;
import com.octopus.demo.userservice.mapper.AddressMapper;
import com.octopus.demo.userservice.model.Address;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisAddressDao implements AddressDao {

    private final AddressMapper addressMapper;

    public MybatisAddressDao(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    @Override
    public List<Address> findByUserId(Long userId) {
        return addressMapper.findByUserId(userId);
    }

    @Override
    public PageResultBean<Address> findByUserId(Long userId, PageQueryBean query) {
        PageResultBean<Address> result = new PageResultBean<>();
        long count = addressMapper.countByUserId(userId);
        result.setCount(count);

        long offset = (long) (query.getPage() - 1) * query.getSize();
        if (offset >= count || offset > Integer.MAX_VALUE) {
            result.setList(List.of());
            return result;
        }
        result.setList(addressMapper.findPageByUserId(userId, (int) offset, query.getSize()));
        return result;
    }

    @Override
    public Optional<Address> findById(Long id) {
        return Optional.ofNullable(addressMapper.findById(id));
    }

    @Override
    public Optional<Address> findDefaultByUserId(Long userId) {
        return Optional.ofNullable(addressMapper.findDefaultByUserId(userId));
    }

    @Override
    public Address save(Address address) {
        if (address.getIsDefault() == null) {
            address.setIsDefault(false);
        }
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            addressMapper.clearDefault(address.getUserId(), null);
        }
        addressMapper.insert(address);
        return Optional.ofNullable(addressMapper.findById(address.getId()))
                .orElseThrow(() -> new IllegalStateException("地址保存后未找到, id: " + address.getId()));
    }

    @Override
    public Optional<Address> update(Address address) {
        if (addressMapper.findById(address.getId()) == null) {
            return Optional.empty();
        }
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            addressMapper.clearDefault(address.getUserId(), address.getId());
        }
        addressMapper.update(address);
        return Optional.ofNullable(addressMapper.findById(address.getId()));
    }

    @Override
    public boolean deleteById(Long id) {
        return addressMapper.deleteById(id) > 0;
    }
}
