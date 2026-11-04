package com.octopus.demo.userservice.mapper;

import com.octopus.demo.userservice.model.Address;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AddressMapper {

    List<Address> findByUserId(Long userId);

    long countByUserId(Long userId);

    List<Address> findPageByUserId(@Param("userId") Long userId,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    Address findById(Long id);

    Address findDefaultByUserId(Long userId);

    void insert(Address address);

    int update(Address address);

    int deleteById(Long id);

    int clearDefault(@Param("userId") Long userId, @Param("exceptId") Long exceptId);
}
