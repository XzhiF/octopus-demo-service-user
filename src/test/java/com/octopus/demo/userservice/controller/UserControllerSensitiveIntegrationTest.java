package com.octopus.demo.userservice.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.octopus.demo.userservice.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerSensitiveIntegrationTest {

    private static final String REJECT_MESSAGE = "昵称包含违规内容，请修改";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Logger serviceLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void attachLogAppender() {
        serviceLogger = (Logger) LoggerFactory.getLogger(UserServiceImpl.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        serviceLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        serviceLogger.detachAppender(logAppender);
    }

    @Test
    void shouldRejectAbuseNicknameOnCreate() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("sensitive_abuse", "蠢货")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(REJECT_MESSAGE))
                .andReturn();

        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode json = objectMapper.readTree(response);
        assertTrue(json.path("data").isNull());
        assertFalse(response.contains("蠢货"), "response must not echo the hit word");
        assertFalse(response.contains("ABUSE"), "response must not expose dictionary details");

        String allUsers = mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertFalse(allUsers.contains("蠢货"), "rejected nickname must not be persisted");
    }

    @ParameterizedTest
    @ValueSource(strings = {"蠢 货", "ＢＡＳＴＡＲＤ", "BaStArD", "色情", "porn"})
    void shouldRejectNicknameVariantsOnCreate(String nickname) throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("variant_" + nickname.hashCode(), nickname)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(REJECT_MESSAGE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"class", "assignment", "classic", "passenger"})
    void shouldAcceptNormalNicknames(String nickname) throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("guard_" + nickname, nickname)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nickname").value(nickname));
    }

    @Test
    void shouldCreateUserWithLegalNickname() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("legal_e2e", "E2E_小明")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("legal_e2e"))
                .andExpect(jsonPath("$.data.nickname").value("E2E_小明"));
    }

    @Test
    void shouldRejectAbuseNicknameOnUpdateAndKeepOriginal() throws Exception {
        long id = createLegalUser("upd_abuse", "E2E_原昵称");

        mockMvc.perform(put("/api/users/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("upd_abuse", "bastard")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(REJECT_MESSAGE));

        mockMvc.perform(get("/api/users/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("E2E_原昵称"));
    }

    @Test
    void shouldAllowLegalNicknameOnUpdate() throws Exception {
        long id = createLegalUser("upd_legal", "E2E_Before");

        mockMvc.perform(put("/api/users/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("upd_legal", "E2E_Alice_2026")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nickname").value("E2E_Alice_2026"));

        mockMvc.perform(get("/api/users/" + id))
                .andExpect(jsonPath("$.data.nickname").value("E2E_Alice_2026"));
    }

    @Test
    void shouldLogOnlyCategoryAndFieldNameOnHit() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("log_abuse", "蠢货")))
                .andExpect(status().isBadRequest());

        assertEquals(1, logAppender.list.size(), "exactly one rejection log expected");
        ILoggingEvent event = logAppender.list.get(0);
        assertEquals(Level.WARN, event.getLevel());
        String message = event.getFormattedMessage();
        assertTrue(message.contains("nickname"), "log must mention the field name");
        assertTrue(message.contains("ABUSE"), "log must mention the category");
        assertFalse(message.contains("蠢货"), "log must not contain the original nickname");
    }

    private long createLegalUser(String username, String nickname) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(username, nickname)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value(nickname))
                .andReturn();
        JsonNode json = objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return json.path("data").path("id").asLong();
    }

    private String body(String username, String nickname) {
        return """
                {"username":"%s","nickname":"%s","email":"%s@example.com","age":25}
                """.formatted(username, nickname, username);
    }
}
