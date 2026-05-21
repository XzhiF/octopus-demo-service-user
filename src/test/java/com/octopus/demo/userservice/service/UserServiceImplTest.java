package com.octopus.demo.userservice.service;

import com.octopus.demo.userservice.dao.UserDao;
import com.octopus.demo.userservice.model.User;
import com.octopus.demo.userservice.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserDao userDao;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userDao);
    }

    @Test
    void shouldGetAllUsers() {
        var user = createUser(1L, "test", "test@example.com");
        when(userDao.findAll()).thenReturn(List.of(user));

        List<User> users = userService.getAllUsers();
        assertEquals(1, users.size());
        verify(userDao).findAll();
    }

    @Test
    void shouldGetUserById() {
        var user = createUser(1L, "test", "test@example.com");
        when(userDao.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> found = userService.getUserById(1L);
        assertTrue(found.isPresent());
        assertEquals("test", found.get().getUsername());
    }

    @Test
    void shouldReturnEmptyWhenUserNotFound() {
        when(userDao.findById(999L)).thenReturn(Optional.empty());

        Optional<User> found = userService.getUserById(999L);
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldCreateUser() {
        var user = createUser(null, "new", "new@example.com");
        when(userDao.save(any(User.class))).thenReturn(user);

        User created = userService.createUser(new User());
        assertNotNull(created);
        verify(userDao).save(any(User.class));
    }

    @Test
    void shouldUpdateUser() {
        var existing = createUser(1L, "original", "original@example.com");
        var updated = createUser(1L, "updated", "updated@example.com");
        when(userDao.findById(1L)).thenReturn(Optional.of(existing));
        when(userDao.update(any(User.class))).thenReturn(Optional.of(updated));

        Optional<User> result = userService.updateUser(1L, updated);
        assertTrue(result.isPresent());
        assertEquals("updated", result.get().getUsername());
    }

    @Test
    void shouldReturnEmptyWhenUpdatingNonExistentUser() {
        when(userDao.findById(999L)).thenReturn(Optional.empty());

        Optional<User> result = userService.updateUser(999L, new User());
        assertTrue(result.isEmpty());
        verify(userDao, never()).update(any());
    }

    @Test
    void shouldDeleteUser() {
        when(userDao.deleteById(1L)).thenReturn(true);

        boolean deleted = userService.deleteUser(1L);
        assertTrue(deleted);
    }

    @Test
    void shouldReturnFalseWhenDeletingNonExistentUser() {
        when(userDao.deleteById(999L)).thenReturn(false);

        boolean deleted = userService.deleteUser(999L);
        assertFalse(deleted);
    }

    private User createUser(Long id, String username, String email) {
        return new User(id, username, email, 25,
                LocalDateTime.now(), LocalDateTime.now());
    }
}