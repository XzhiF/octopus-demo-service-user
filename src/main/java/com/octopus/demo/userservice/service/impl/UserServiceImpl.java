package com.octopus.demo.userservice.service.impl;

import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import com.octopus.demo.userservice.dao.UserDao;
import com.octopus.demo.userservice.model.User;
import com.octopus.demo.userservice.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class UserServiceImpl implements UserService {

    private final UserDao userDao;

    public UserServiceImpl(UserDao userDao) {
        this.userDao = userDao;
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
        user.setNickname(resolveNicknameForCreate(user.getNickname()));
        return userDao.save(user);
    }

    @Override
    public Optional<User> updateUser(Long id, User user) {
        return userDao.findById(id)
                .flatMap(existing -> {
                    user.setId(id);
                    if (user.getNickname() == null || user.getNickname().trim().isEmpty()) {
                        user.setNickname(existing.getNickname());
                    } else {
                        user.setNickname(user.getNickname().trim());
                    }
                    return userDao.update(user);
                });
    }

    private String resolveNicknameForCreate(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return "用户" + ThreadLocalRandom.current().nextInt(100000, 1000000);
        }
        return nickname.trim();
    }

    @Override
    public boolean deleteUser(Long id) {
        return userDao.deleteById(id);
    }
}
