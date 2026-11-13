package com.octopus.demo.userservice.controller;

import com.octopus.demo.common.auth.RequireAuth;
import com.octopus.demo.common.bean.R;
import com.octopus.demo.common.util.JwtUtil;
import com.octopus.demo.userservice.dao.UserDao;
import com.octopus.demo.userservice.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class MemberStatusInterceptorTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("octopus_demo")
            .withUsername("octopus")
            .withPassword("octopus")
            .withInitScript("db/schema.sql");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDao userDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldAllowActiveMemberWithValidToken() throws Exception {
        User user = createUser(1);
        String token = JwtUtil.generateToken(user.getId());

        mockMvc.perform(get("/api/member/test/ping")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("pong"));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT status, phone FROM users WHERE id = ?", user.getId());
        assertEquals(1, ((Number) row.get("status")).intValue());
        assertEquals("13812348888", row.get("phone"));
    }

    @Test
    void shouldRejectDisabledMemberTokenWith403() throws Exception {
        User user = createUser(0);
        String token = JwtUtil.generateToken(user.getId());

        mockMvc.perform(get("/api/member/test/ping")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("账号已停用"));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT status, phone FROM users WHERE id = ?", user.getId());
        assertEquals(0, ((Number) row.get("status")).intValue());
    }

    @Test
    void shouldRejectMissingTokenWith401() throws Exception {
        mockMvc.perform(get("/api/member/test/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    private User createUser(int status) {
        String username = "member_" + UUID.randomUUID().toString().replace("-", "");
        User user = new User();
        user.setUsername(username);
        user.setNickname("测试会员");
        user.setEmail(username + "@example.com");
        user.setPhone("13812348888");
        user.setPasswordHash("hash-value");
        User saved = userDao.save(user);
        if (status != 1) {
            jdbcTemplate.update("UPDATE users SET status = ? WHERE id = ?", status, saved.getId());
        }
        return saved;
    }

    @TestConfiguration
    static class PingControllerConfig {
        @Bean
        PingController pingController() {
            return new PingController();
        }
    }

    @RestController
    @RequestMapping("/api/member/test")
    static class PingController {
        @RequireAuth
        @GetMapping("/ping")
        public R<String> ping() {
            return R.ok("pong");
        }
    }
}
