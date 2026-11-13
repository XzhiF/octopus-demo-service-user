package com.octopus.demo.userservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Legacy C-side paths must be gone: /api/auth/** and /api/users/** belonged to service-admin.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LegacyPathRemovedTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("octopus_demo")
            .withUsername("octopus")
            .withPassword("octopus")
            .withInitScript("db/schema.sql");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn404ForLegacyLoginPath() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"a\",\"password\":\"b\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404ForLegacyUsersList() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404ForLegacyUserById() throws Exception {
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isNotFound());
    }
}
