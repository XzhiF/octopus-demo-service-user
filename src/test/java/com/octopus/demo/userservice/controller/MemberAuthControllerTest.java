package com.octopus.demo.userservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class MemberAuthControllerTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("octopus_demo")
            .withUsername("octopus")
            .withPassword("octopus")
            .withInitScript("db/schema.sql");

    private static final String PASSWORD = "Passw0rd!123";
    private static final long EXPECTED_EXPIRES_IN_SECONDS = 30L * 86400L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String uniqueUsername() {
        return "member_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String registerBody(String username, String password, String email) {
        return """
                {
                    "username": "%s",
                    "password": "%s",
                    "email": "%s",
                    "age": 30,
                    "phone": "13812348888"
                }""".formatted(username, password, email);
    }

    private long register(String username) throws Exception {
        String response = mockMvc.perform(post("/api/member/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(username, PASSWORD, username + "@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private JsonNode login(String username, String password) throws Exception {
        String body = """
                {
                    "username": "%s",
                    "password": "%s"
                }""".formatted(username, password);

        String response = mockMvc.perform(post("/api/member/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).path("data");
    }

    private void disable(long userId) {
        jdbcTemplate.update("UPDATE users SET status = 0 WHERE id = ?", userId);
    }

    @Test
    void shouldRegisterActiveUserAndPersistToDb() throws Exception {
        String username = uniqueUsername();

        String response = mockMvc.perform(post("/api/member/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(username, PASSWORD, username + "@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(response).path("data").path("id").asLong();
        assertThat(id).isPositive();

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT username, status, phone FROM users WHERE username = ?", username);
        assertThat(row.get("username")).isEqualTo(username);
        assertThat(((Number) row.get("status")).intValue()).isEqualTo(1);
        assertThat(row.get("phone")).isEqualTo("13812348888");
    }

    @Test
    void shouldReturn400WhenUsernameAlreadyExists() throws Exception {
        String username = uniqueUsername();
        register(username);

        mockMvc.perform(post("/api/member/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(username, PASSWORD, username + "@example.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("用户名已存在"));
    }

    @Test
    void shouldReturn400WhenEmailMissing() throws Exception {
        String body = """
                {
                    "username": "%s",
                    "password": "%s"
                }""".formatted(uniqueUsername(), PASSWORD);

        mockMvc.perform(post("/api/member/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenPasswordTooShort() throws Exception {
        String username = uniqueUsername();
        String body = """
                {
                    "username": "%s",
                    "password": "short",
                    "email": "%s@example.com"
                }""".formatted(username, username);

        mockMvc.perform(post("/api/member/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldLoginAndAccessMeWithBearerToken() throws Exception {
        String username = uniqueUsername();
        long userId = register(username);

        JsonNode loginData = login(username, PASSWORD);
        String token = loginData.path("token").asText();

        assertThat(token).isNotBlank();
        assertThat(loginData.path("tokenType").asText()).isEqualTo("Bearer");
        assertThat(loginData.path("expiresInSeconds").asLong()).isEqualTo(EXPECTED_EXPIRES_IN_SECONDS);
        assertThat(loginData.path("user").path("id").asLong()).isEqualTo(userId);
        assertThat(loginData.path("user").path("status").asInt()).isEqualTo(1);
        assertThat(loginData.path("user").has("password")).isFalse();
        assertThat(loginData.path("user").has("passwordHash")).isFalse();

        mockMvc.perform(get("/api/member/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void shouldReturn401WithUnifiedMessageWhenPasswordWrong() throws Exception {
        String username = uniqueUsername();
        register(username);

        String body = """
                {
                    "username": "%s",
                    "password": "WrongPassw0rd!"
                }""".formatted(username);

        mockMvc.perform(post("/api/member/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void shouldReturn401WithUnifiedMessageWhenUserNotExist() throws Exception {
        String body = """
                {
                    "username": "member_missing_%s",
                    "password": "%s"
                }""".formatted(UUID.randomUUID().toString().replace("-", ""), PASSWORD);

        mockMvc.perform(post("/api/member/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void shouldReturn403WhenDisabledUserLogsIn() throws Exception {
        String username = uniqueUsername();
        long userId = register(username);
        disable(userId);

        String body = """
                {
                    "username": "%s",
                    "password": "%s"
                }""".formatted(username, PASSWORD);

        mockMvc.perform(post("/api/member/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("账号已停用"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void shouldReturn403WhenDisabledUserCallsMeWithExistingToken() throws Exception {
        String username = uniqueUsername();
        long userId = register(username);
        String token = login(username, PASSWORD).path("token").asText();
        disable(userId);

        mockMvc.perform(get("/api/member/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("账号已停用"));
    }

    @Test
    void shouldReturn401WhenMeWithoutAuthorization() throws Exception {
        mockMvc.perform(get("/api/member/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturn401WhenMeWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/member/auth/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldLogoutWithBearerToken() throws Exception {
        String username = uniqueUsername();
        register(username);
        String token = login(username, PASSWORD).path("token").asText();

        mockMvc.perform(post("/api/member/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
