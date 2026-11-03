package com.octopus.demo.userservice.dao.impl;

import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import com.octopus.demo.userservice.dao.UserDao;
import com.octopus.demo.userservice.mapper.UserMapper;
import com.octopus.demo.userservice.model.User;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisUserDao implements UserDao {

    private final UserMapper userMapper;

    public MybatisUserDao(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public List<User> findAll() {
        return userMapper.findAll();
    }

    @Override
    public PageResultBean<User> findAll(PageQueryBean query) {
        PageResultBean<User> result = new PageResultBean<>();
        long count = userMapper.count();
        result.setCount(count);

        long offset = (long) (query.getPage() - 1) * query.getSize();
        if (offset >= count || offset > Integer.MAX_VALUE) {
            result.setList(List.of());
            return result;
        }
        result.setList(userMapper.findPage((int) offset, query.getSize()));
        return result;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userMapper.findById(id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(userMapper.findByUsername(username));
    }

    @Override
    public User save(User user) {
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("用户名已存在");
        }
        return Optional.ofNullable(userMapper.findById(user.getId()))
                .orElseThrow(() -> new IllegalStateException("用户保存后未找到, id: " + user.getId()));
    }

    @Override
    public Optional<User> update(User user) {
        try {
            if (userMapper.update(user) == 0) {
                return Optional.empty();
            }
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("用户名已存在");
        }
        return Optional.ofNullable(userMapper.findById(user.getId()));
    }

    @Override
    public boolean deleteById(Long id) {
        return userMapper.deleteById(id) > 0;
    }
}