package com.octopus.demo.userservice.service;

import com.octopus.demo.userservice.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    List<User> getAllUsers();

    Optional<User> getUserById(Long id);

    User createUser(User user);

    Optional<User> updateUser(Long id, User user);

    boolean deleteUser(Long id);
}