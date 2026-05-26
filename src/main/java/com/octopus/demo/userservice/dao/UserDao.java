package com.octopus.demo.userservice.dao;

import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import com.octopus.demo.userservice.model.User;

import java.util.List;
import java.util.Optional;

public interface UserDao {

    List<User> findAll();

    PageResultBean<User> findAll(PageQueryBean query);

    Optional<User> findById(Long id);

    User save(User user);

    Optional<User> update(User user);

    boolean deleteById(Long id);
}
