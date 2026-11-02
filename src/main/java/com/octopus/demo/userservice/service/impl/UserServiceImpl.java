package com.octopus.demo.userservice.service.impl;

import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import com.octopus.demo.common.util.PasswordUtils;
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
    public User createUser(User user, String rawPassword) {
        if (userDao.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("用户名已存在");
        }
        user.setPasswordHash(PasswordUtils.encode(rawPassword));
        user.setNickname(resolveNicknameForCreate(user.getNickname()));
        return userDao.save(user);
    }

    @Override
    public Optional<User> authenticate(String username, String rawPassword) {
        if (username == null || rawPassword == null) {
            return Optional.empty();
        }
        return userDao.findByUsername(username)
                .filter(user -> PasswordUtils.matches(rawPassword, user.getPasswordHash()));
    }

    @Override
    public Optional<User> updateUser(Long id, User user) {
        return userDao.findById(id)
                .flatMap(existing -> {
                    String newUsername = user.getUsername();
                    if (newUsername != null && !newUsername.equals(existing.getUsername())
                            && userDao.findByUsername(newUsername).isPresent()) {
                        throw new IllegalArgumentException("用户名已存在");
                    }
                    user.setId(id);
                    user.setPasswordHash(existing.getPasswordHash());
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
