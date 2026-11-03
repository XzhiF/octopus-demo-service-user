package com.octopus.demo.userservice.mapper;

import com.octopus.demo.userservice.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    List<User> findAll();

    long count();

    List<User> findPage(@Param("offset") int offset, @Param("size") int size);

    User findById(Long id);

    User findByUsername(String username);

    void insert(User user);

    int update(User user);

    int deleteById(Long id);
}