package com.octopus.demo.userservice.controller;

import com.octopus.demo.common.audit.AuditContext;
import com.octopus.demo.common.audit.AuditEvent;
import com.octopus.demo.common.audit.AuditLogger;
import com.octopus.demo.common.audit.AuditQuery;
import com.octopus.demo.common.auth.RequireAuth;
import com.octopus.demo.common.bean.R;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * 审计日志查询端点。
 * 需要 X-User-Id 请求头（由 @RequireAuth 强制执行），防止未授权访问审计数据。
 */
@RequireAuth
@RestController
@RequestMapping("/api/users/audit")
public class AuditController {
    private final AuditLogger auditLogger;

    public AuditController(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @GetMapping
    public R<List<AuditEvent>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "100") int limit) {
        int clampedLimit = Math.max(1, Math.min(limit, 500));
        // Force data isolation: only return audit events for the current authenticated user
        Long currentUserId = AuditContext.getCurrentUserId();
        AuditQuery query = new AuditQuery(currentUserId, action, entityType, from, to, clampedLimit);
        return R.ok(auditLogger.query(query));
    }
}
