package com.octopus.demo.userservice.controller;

import com.octopus.demo.common.audit.AuditEvent;
import com.octopus.demo.common.audit.AuditLogger;
import com.octopus.demo.common.audit.AuditQuery;
import com.octopus.demo.common.auth.AuthAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditController.class)
@Import(AuthAutoConfiguration.class)
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogger auditLogger;

    @Test
    @DisplayName("GET /api/users/audit returns audit events when authenticated")
    void getAuditLogs_returnsEvents() throws Exception {
        var event = new AuditEvent(Instant.now(), 1L, "CREATE", "USER", "1", Map.of());
        when(auditLogger.query(any(AuditQuery.class))).thenReturn(List.of(event));

        mockMvc.perform(get("/api/users/audit").header("X-User-Id", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].action").value("CREATE"));
    }

    @Test
    @DisplayName("GET /api/users/audit with filters passes query params")
    void getAuditLogs_withFilters() throws Exception {
        when(auditLogger.query(any(AuditQuery.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/users/audit")
                .header("X-User-Id", "1")
                .param("action", "DELETE")
                .param("limit", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/users/audit returns 401 when X-User-Id header is missing")
    void getAuditLogs_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/users/audit"))
            .andExpect(status().isUnauthorized());
    }
}
