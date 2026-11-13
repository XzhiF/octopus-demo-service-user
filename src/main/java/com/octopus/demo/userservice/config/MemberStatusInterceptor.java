package com.octopus.demo.userservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.octopus.demo.common.auth.UserContext;
import com.octopus.demo.userservice.dao.UserDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;

/**
 * Rejects requests whose authenticated user has status=0.
 * Runs after the common AuthInterceptor (which populates UserContext),
 * so it can only observe a userId once the bearer token has been validated.
 */
@Component
public class MemberStatusInterceptor implements HandlerInterceptor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int STATUS_DISABLED = 0;

    private final UserDao userDao;

    public MemberStatusInterceptor(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return true;
        }
        if (userDao.findById(userId)
                .map(user -> user.getStatus() != null && user.getStatus() == STATUS_DISABLED)
                .orElse(false)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json;charset=UTF-8");
            OBJECT_MAPPER.writeValue(response.getWriter(), Map.of("code", 403, "msg", "账号已停用"));
            return false;
        }
        return true;
    }
}
