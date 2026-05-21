package com.octopus.demo.userservice.service.impl;

import com.octopus.demo.userservice.dao.UserDao;
import com.octopus.demo.userservice.model.User;
import com.octopus.demo.userservice.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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
    public Optional<User> getUserById(Long id) {
        return userDao.findById(id);
    }

    @Override
    public User createUser(User user) {
        return userDao.save(user);
    }

    @Override
    public Optional<User> updateUser(Long id, User user) {
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
}