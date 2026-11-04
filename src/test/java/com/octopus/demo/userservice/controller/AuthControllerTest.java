package com.octopus.demo.userservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.octopus.demo.userservice.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthControllerTest {

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
    private JwtProperties jwtProperties;

    private String uniqueUsername() {
        return "auth_" + UUID.randomUUID().toString().replace("-", "");
    }

    private long createUser(String username) throws Exception {
        String body = """
                {
                    "username": "%s",
                    "password": "%s",
                    "email": "%s@example.com",
                    "age": 30
                }""".formatted(username, PASSWORD, username);

        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private JsonNode login(String username, String password) throws Exception {
        String body = """
                {
                    "username": "%s",
                    "password": "%s"
                }""".formatted(username, password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).path("data");
    }

    @Test
    void shouldLoginAndAccessMeWithBearerToken() throws Exception {
        String username = uniqueUsername();
        long userId = createUser(username);

        JsonNode loginData = login(username, PASSWORD);
        String token = loginData.path("token").asText();

        assertThat(token).isNotBlank();
        assertThat(loginData.path("tokenType").asText()).isEqualTo("Bearer");
        assertThat(loginData.path("expiresInSeconds").asLong()).isEqualTo(EXPECTED_EXPIRES_IN_SECONDS);
        assertThat(loginData.path("user").path("id").asLong()).isEqualTo(userId);
        assertThat(loginData.path("user").has("password")).isFalse();
        assertThat(loginData.path("user").has("passwordHash")).isFalse();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void shouldReturn401WithUnifiedMessageWhenPasswordWrong() throws Exception {
        String username = uniqueUsername();
        createUser(username);

        String body = """
                {
                    "username": "%s",
                    "password": "WrongPassw0rd!"
                }""".formatted(username);

        mockMvc.perform(post("/api/auth/login")
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
                    "username": "auth_missing_%s",
                    "password": "%s"
                }""".formatted(UUID.randomUUID().toString().replace("-", ""), PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void shouldReturn401WhenMeWithoutAuthorization() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturn401WhenMeWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturn401WhenMeWithExpiredToken() throws Exception {
        long now = System.currentTimeMillis();
        String expiredToken = Jwts.builder()
                .subject("1")
                .issuedAt(new Date(now - 7_200_000L))
                .expiration(new Date(now - 3_600_000L))
                .signWith(Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("Token expired"));
    }

    @Test
    void shouldReturn401WhenMeWithOnlyXUserIdHeader() throws Exception {
        long userId = createUser(uniqueUsername());

        mockMvc.perform(get("/api/auth/me")
                        .header("X-User-Id", String.valueOf(userId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }
}
