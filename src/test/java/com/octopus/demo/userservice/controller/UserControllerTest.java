package com.octopus.demo.userservice.controller;

import com.octopus.demo.common.bean.PageQueryBean;
import com.octopus.demo.common.bean.PageResultBean;
import com.octopus.demo.userservice.model.User;
import com.octopus.demo.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void shouldGetAllUsers() throws Exception {
        var user = new User(1L, "test", "测试昵称", "test@example.com", 25,
                LocalDateTime.now(), LocalDateTime.now());
        PageResultBean<User> result = new PageResultBean<>();
        result.setCount(1);
        result.setList(List.of(user));
        when(userService.getAllUsers(any(PageQueryBean.class))).thenReturn(result);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].username").value("test"))
                .andExpect(jsonPath("$.data.list[0].nickname").value("测试昵称"));
    }

    @Test
    void shouldGetUserById() throws Exception {
        var user = new User(1L, "test", "测试昵称", "test@example.com", 25,
                LocalDateTime.now(), LocalDateTime.now());
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("test"))
                .andExpect(jsonPath("$.data.nickname").value("测试昵称"));
    }

    @Test
    void shouldReturn404WhenUserNotFound() throws Exception {
        when(userService.getUserById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("用户不存在, id: 999"));
    }

    @Test
    void shouldCreateUser() throws Exception {
        when(userService.createUser(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        String body = """
                {
                    "username": "new",
                    "nickname": "小明",
                    "email": "new@example.com",
                    "age": 30
                }""";

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("new"))
                .andExpect(jsonPath("$.data.nickname").value("小明"));
    }

    @Test
    void shouldReturn400WhenCreatingWithBlankNickname() throws Exception {
        String body = """
                {
                    "username": "new",
                    "nickname": "  ",
                    "email": "new@example.com"
                }""";

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any(User.class));
    }

    @Test
    void shouldReturn400WhenCreatingWithMissingNickname() throws Exception {
        String body = """
                {
                    "username": "new",
                    "email": "new@example.com"
                }""";

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any(User.class));
    }

    @Test
    void shouldReturn400WhenCreatingWithInvalidData() throws Exception {
        String body = """
                {
                    "username": "",
                    "email": "invalid-email"
                }""";

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateUser() throws Exception {
        when(userService.updateUser(eq(1L), any(User.class)))
                .thenAnswer(inv -> Optional.of(inv.getArgument(1)));

        String body = """
                {
                    "username": "updated",
                    "nickname": "更新后的昵称",
                    "email": "updated@example.com",
                    "age": 28
                }""";

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("updated"))
                .andExpect(jsonPath("$.data.nickname").value("更新后的昵称"));
    }

    @Test
    void shouldReturn400WhenUpdatingWithBlankNickname() throws Exception {
        String body = """
                {
                    "username": "updated",
                    "nickname": "",
                    "email": "updated@example.com"
                }""";

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateUser(any(), any(User.class));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentUser() throws Exception {
        when(userService.updateUser(eq(999L), any(User.class))).thenReturn(Optional.empty());

        String body = """
                {
                    "username": "ghost",
                    "nickname": "幽灵",
                    "email": "ghost@example.com"
                }""";

        mockMvc.perform(put("/api/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void shouldDeleteUser() throws Exception {
        when(userService.deleteUser(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentUser() throws Exception {
        when(userService.deleteUser(999L)).thenReturn(false);

        mockMvc.perform(delete("/api/users/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }
}
