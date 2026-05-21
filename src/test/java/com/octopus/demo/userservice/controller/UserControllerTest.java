package com.octopus.demo.userservice.controller;

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
        var user = new User(1L, "test", "test@example.com", 25,
                LocalDateTime.now(), LocalDateTime.now());
        when(userService.getAllUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("test"));
    }

    @Test
    void shouldGetUserById() throws Exception {
        var user = new User(1L, "test", "test@example.com", 25,
                LocalDateTime.now(), LocalDateTime.now());
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("test"));
    }

    @Test
    void shouldReturn404WhenUserNotFound() throws Exception {
        when(userService.getUserById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldCreateUser() throws Exception {
        var user = new User(1L, "new", "new@example.com", 30,
                LocalDateTime.now(), LocalDateTime.now());
        when(userService.createUser(any(User.class))).thenReturn(user);

        String body = """
                {
                    "username": "new",
                    "email": "new@example.com",
                    "age": 30
                }""";

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new"));
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
        var user = new User(1L, "updated", "updated@example.com", 28,
                LocalDateTime.now(), LocalDateTime.now());
        when(userService.updateUser(eq(1L), any(User.class))).thenReturn(Optional.of(user));

        String body = """
                {
                    "username": "updated",
                    "email": "updated@example.com",
                    "age": 28
                }""";

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updated"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentUser() throws Exception {
        when(userService.updateUser(eq(999L), any(User.class))).thenReturn(Optional.empty());

        String body = """
                {
                    "username": "ghost",
                    "email": "ghost@example.com"
                }""";

        mockMvc.perform(put("/api/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldDeleteUser() throws Exception {
        when(userService.deleteUser(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentUser() throws Exception {
        when(userService.deleteUser(999L)).thenReturn(false);

        mockMvc.perform(delete("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}