package com.octopus.demo.userservice.dao;

import com.octopus.demo.userservice.dao.impl.InMemoryUserDao;
import com.octopus.demo.userservice.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryUserDaoTest {

    private InMemoryUserDao userDao;

    @BeforeEach
    void setUp() {
        userDao = new InMemoryUserDao();
    }

    @Test
    void shouldSaveAndFindUser() {
        User user = new User();
        user.setUsername("test");
        user.setEmail("test@example.com");

        User saved = userDao.save(user);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        Optional<User> found = userDao.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("test", found.get().getUsername());
    }

    @Test
    void shouldFindAllUsers() {
        User user1 = new User();
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");

        User user2 = new User();
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");

        userDao.save(user1);
        userDao.save(user2);

        List<User> users = userDao.findAll();
        assertEquals(2, users.size());
    }

    @Test
    void shouldReturnEmptyWhenUserNotFound() {
        Optional<User> found = userDao.findById(999L);
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldUpdateUser() {
        User user = new User();
        user.setUsername("original");
        user.setEmail("original@example.com");
        User saved = userDao.save(user);

        saved.setUsername("updated");
        Optional<User> updated = userDao.update(saved);

        assertTrue(updated.isPresent());
        assertEquals("updated", updated.get().getUsername());
    }

    @Test
    void shouldReturnEmptyWhenUpdatingNonExistentUser() {
        User user = new User();
        user.setId(999L);
        user.setUsername("ghost");

        Optional<User> updated = userDao.update(user);
        assertTrue(updated.isEmpty());
    }

    @Test
    void shouldDeleteUser() {
        User user = new User();
        user.setUsername("delete-me");
        user.setEmail("delete@example.com");
        User saved = userDao.save(user);

        boolean deleted = userDao.deleteById(saved.getId());
        assertTrue(deleted);
        assertTrue(userDao.findById(saved.getId()).isEmpty());
    }

    @Test
    void shouldReturnFalseWhenDeletingNonExistentUser() {
        boolean deleted = userDao.deleteById(999L);
        assertFalse(deleted);
    }

    @Test
    void shouldGenerateIncrementalIds() {
        User user1 = new User();
        user1.setUsername("u1");
        user1.setEmail("u1@example.com");

        User user2 = new User();
        user2.setUsername("u2");
        user2.setEmail("u2@example.com");

        User saved1 = userDao.save(user1);
        User saved2 = userDao.save(user2);

        assertEquals(1L, saved1.getId());
        assertEquals(2L, saved2.getId());
    }
}