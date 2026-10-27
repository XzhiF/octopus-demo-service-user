package com.octopus.demo.userservice.service.impl;

import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import com.octopus.demo.common.sensitive.SensitiveCheckResult;
import com.octopus.demo.common.sensitive.SensitiveWordFilter;
import com.octopus.demo.userservice.dao.UserDao;
import com.octopus.demo.userservice.model.User;
import com.octopus.demo.userservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserDao userDao;
    private final SensitiveWordFilter sensitiveWordFilter;

    public UserServiceImpl(UserDao userDao, SensitiveWordFilter sensitiveWordFilter) {
        this.userDao = userDao;
        this.sensitiveWordFilter = sensitiveWordFilter;
    }

    @Override
    public List<User> getAllUsers() {
        return userDao.findAll();
    }

    @Override
    public PageResultBean<User> getAllUsers(PageQueryBean query) {
        return userDao.findAll(query);
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return userDao.findById(id);
    }

    @Override
    public User createUser(User user) {
        checkNickname(user);
        return userDao.save(user);
    }

    @Override
    public Optional<User> updateUser(Long id, User user) {
        checkNickname(user);
        return userDao.findById(id)
                .flatMap(existing -> {
                    user.setId(id);
                    return userDao.update(user);
                });
    }

    @Override
    public boolean deleteUser(Long id) {
        return userDao.deleteById(id);
    }

    private void checkNickname(User user) {
        SensitiveCheckResult result = sensitiveWordFilter.check(user.getNickname());
        if (result.hit()) {
            log.warn("Sensitive nickname rejected on field 'nickname' (category={})", result.category());
            throw new IllegalArgumentException("昵称包含违规内容，请修改");
        }
    }
}
