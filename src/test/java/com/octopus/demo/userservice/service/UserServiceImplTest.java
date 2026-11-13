package com.octopus.demo.userservice.service;

import com.octopus.demo.common.util.PasswordUtils;
import com.octopus.demo.userservice.dao.UserDao;
import com.octopus.demo.userservice.model.User;
import com.octopus.demo.userservice.service.impl.UserServiceImpl;
import com.octopus.demo.userservice.vo.UserVO;
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

        User created = userService.createUser(new User(), "Passw0rd!123");
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
    void shouldGenerateNicknameWhenCreatingWithBlankNickname() {
        when(userDao.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.createUser(new User(), "Passw0rd!123");

        assertNotNull(created.getNickname());
        assertTrue(created.getNickname().matches("用户\\d{6}"),
                "generated nickname should match 用户\\d{6} but was: " + created.getNickname());
        verify(userDao).save(any(User.class));
    }

    @Test
    void shouldGenerateNicknameWhenCreatingWithWhitespaceNickname() {
        when(userDao.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var user = new User();
        user.setNickname("   ");

        User created = userService.createUser(user, "Passw0rd!123");

        assertTrue(created.getNickname().matches("用户\\d{6}"));
    }

    @Test
    void shouldTrimNicknameWhenCreatingWithPaddedNickname() {
        when(userDao.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var user = new User();
        user.setNickname("  张三  ");

        User created = userService.createUser(user, "Passw0rd!123");

        assertEquals("张三", created.getNickname());
    }

    @Test
    void shouldKeepExistingNicknameWhenUpdatingWithBlankNickname() {
        var existing = createUser(1L, "original", "原昵称", "original@example.com");
        var updated = createUser(1L, "updated", null, "updated@example.com");
        when(userDao.findById(1L)).thenReturn(Optional.of(existing));
        when(userDao.update(any(User.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));

        Optional<User> result = userService.updateUser(1L, updated);

        assertTrue(result.isPresent());
        assertEquals("原昵称", result.get().getNickname());
    }

    @Test
    void shouldOverwriteNicknameWhenUpdatingWithNonBlankNickname() {
        var existing = createUser(1L, "original", "原昵称", "original@example.com");
        var updated = createUser(1L, "updated", "  新昵称  ", "updated@example.com");
        when(userDao.findById(1L)).thenReturn(Optional.of(existing));
        when(userDao.update(any(User.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));

        Optional<User> result = userService.updateUser(1L, updated);

        assertTrue(result.isPresent());
        assertEquals("新昵称", result.get().getNickname());
    }

    @Test
    void shouldEncodePasswordWhenCreatingUser() {
        when(userDao.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var user = new User();
        user.setUsername("new");
        user.setEmail("new@example.com");

        User created = userService.createUser(user, "Passw0rd!123");

        assertNotNull(created.getPasswordHash());
        assertTrue(created.getPasswordHash().startsWith("pbkdf2$"));
        assertNotEquals("Passw0rd!123", created.getPasswordHash());
        assertTrue(PasswordUtils.matches("Passw0rd!123", created.getPasswordHash()));
    }

    @Test
    void shouldRejectDuplicateUsernameWhenCreatingUser() {
        var existing = createUser(1L, "dup", "dup@example.com");
        when(userDao.findByUsername("dup")).thenReturn(Optional.of(existing));
        var user = createUser(null, "dup", "dup@example.com");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(user, "Passw0rd!123"));

        assertEquals("用户名已存在", ex.getMessage());
        verify(userDao, never()).save(any(User.class));
    }

    @Test
    void shouldRejectDuplicateUsernameWhenUpdatingUser() {
        var existing = createUser(1L, "alice", "alice@example.com");
        var other = createUser(2L, "bob", "bob@example.com");
        var updated = createUser(1L, "bob", "alice@example.com");
        when(userDao.findById(1L)).thenReturn(Optional.of(existing));
        when(userDao.findByUsername("bob")).thenReturn(Optional.of(other));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.updateUser(1L, updated));

        assertEquals("用户名已存在", ex.getMessage());
        verify(userDao, never()).update(any(User.class));
    }

    @Test
    void shouldAuthenticateWithCorrectPassword() {
        String rawPassword = "Passw0rd!123";
        var user = createUser(1L, "alice", "alice@example.com");
        user.setPasswordHash(PasswordUtils.encode(rawPassword));
        when(userDao.findByUsername("alice")).thenReturn(Optional.of(user));

        Optional<User> result = userService.authenticate("alice", rawPassword);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void shouldReturnEmptyWhenAuthenticatingWithWrongPassword() {
        var user = createUser(1L, "alice", "alice@example.com");
        user.setPasswordHash(PasswordUtils.encode("Passw0rd!123"));
        when(userDao.findByUsername("alice")).thenReturn(Optional.of(user));

        assertTrue(userService.authenticate("alice", "WrongPassw0rd!").isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenAuthenticatingUnknownUser() {
        when(userDao.findByUsername("ghost")).thenReturn(Optional.empty());

        assertTrue(userService.authenticate("ghost", "Passw0rd!123").isEmpty());
    }

    @Test
    void shouldPreservePasswordHashWhenUpdatingUser() {
        var existing = createUser(1L, "original", "original@example.com");
        existing.setPasswordHash(PasswordUtils.encode("Passw0rd!123"));
        var updated = createUser(1L, "updated", "updated@example.com");
        when(userDao.findById(1L)).thenReturn(Optional.of(existing));
        when(userDao.update(any(User.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));

        Optional<User> result = userService.updateUser(1L, updated);

        assertTrue(result.isPresent());
        assertEquals(existing.getPasswordHash(), result.get().getPasswordHash());
    }

    @Test
    void shouldDefaultStatusToOneWhenCreatingUser() {
        when(userDao.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var user = new User();
        user.setStatus(null);

        User created = userService.createUser(user, "Passw0rd!123");

        assertEquals(1, created.getStatus());
    }

    @Test
    void shouldMaskPhoneAndExposeStatusInUserVO() {
        var user = createUser(1L, "alice", "alice@example.com");
        user.setPhone("13812348888");
        user.setStatus(1);

        UserVO vo = UserVO.from(user);

        assertEquals("138****8888", vo.getPhone());
        assertEquals(1, vo.getStatus());
    }

    private User createUser(Long id, String username, String email) {
        return createUser(id, username, null, email);
    }

    private User createUser(Long id, String username, String nickname, String email) {
        return new User(id, username, nickname, email, 25,
                LocalDateTime.now(), LocalDateTime.now());
    }
}