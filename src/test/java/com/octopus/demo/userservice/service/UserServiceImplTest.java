package com.octopus.demo.userservice.service;

import com.octopus.demo.common.audit.AuditEvent;
import com.octopus.demo.common.audit.AuditLogger;
import com.octopus.demo.userservice.dao.UserDao;
import com.octopus.demo.userservice.model.User;
import com.octopus.demo.userservice.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    @Mock
    private AuditLogger auditLogger;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userDao, auditLogger);
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

    @Test
    @DisplayName("createUser logs CREATE audit event")
    void createUser_logsAuditEvent() {
        var user = createUser(1L, "new", "new@example.com");
        when(userDao.save(any(User.class))).thenReturn(user);

        userService.createUser(new User());

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditLogger).log(captor.capture());
        assertEquals("CREATE", captor.getValue().action());
        assertEquals("USER", captor.getValue().entityType());
    }

    @Test
    @DisplayName("updateUser logs UPDATE audit event on success")
    void updateUser_logsAuditEvent() {
        var existing = createUser(1L, "old", "old@example.com");
        var updated = createUser(1L, "new", "new@example.com");
        when(userDao.findById(1L)).thenReturn(Optional.of(existing));
        when(userDao.update(any(User.class))).thenReturn(Optional.of(updated));

        userService.updateUser(1L, updated);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditLogger).log(captor.capture());
        assertEquals("UPDATE", captor.getValue().action());
    }

    @Test
    @DisplayName("deleteUser logs DELETE audit event when user exists")
    void deleteUser_logsAuditEvent() {
        when(userDao.deleteById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(auditLogger).log(any(AuditEvent.class));
    }

    @Test
    @DisplayName("deleteUser does not log audit when user not found")
    void deleteUser_noAuditWhenNotFound() {
        when(userDao.deleteById(999L)).thenReturn(false);

        userService.deleteUser(999L);

        verify(auditLogger, never()).log(any());
    }

    @Test
    @DisplayName("createUser succeeds even when audit logging fails")
    void createUser_auditFails_businessSucceeds() {
        var user = createUser(1L, "new", "new@example.com");
        when(userDao.save(any(User.class))).thenReturn(user);
        doThrow(new RuntimeException("audit unavailable")).when(auditLogger).log(any(AuditEvent.class));

        User created = userService.createUser(new User());

        assertNotNull(created);
        verify(userDao).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser succeeds even when audit logging fails")
    void updateUser_auditFails_businessSucceeds() {
        var existing = createUser(1L, "old", "old@example.com");
        var updated = createUser(1L, "new", "new@example.com");
        when(userDao.findById(1L)).thenReturn(Optional.of(existing));
        when(userDao.update(any(User.class))).thenReturn(Optional.of(updated));
        doThrow(new RuntimeException("audit unavailable")).when(auditLogger).log(any(AuditEvent.class));

        Optional<User> result = userService.updateUser(1L, updated);

        assertTrue(result.isPresent());
        assertEquals("new", result.get().getUsername());
    }

    @Test
    @DisplayName("deleteUser succeeds even when audit logging fails")
    void deleteUser_auditFails_businessSucceeds() {
        when(userDao.deleteById(1L)).thenReturn(true);
        doThrow(new RuntimeException("audit unavailable")).when(auditLogger).log(any(AuditEvent.class));

        boolean deleted = userService.deleteUser(1L);

        assertTrue(deleted);
        verify(userDao).deleteById(1L);
    }

    private User createUser(Long id, String username, String email) {
        return new User(id, username, email, 25,
                LocalDateTime.now(), LocalDateTime.now());
    }
}