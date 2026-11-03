package com.octopus.demo.userservice.controller;

import com.octopus.demo.common.auth.RequireAuth;
import com.octopus.demo.common.auth.UserContext;
import com.octopus.demo.common.bean.R;
import com.octopus.demo.common.util.JwtUtil;
import com.octopus.demo.userservice.config.JwtProperties;
import com.octopus.demo.userservice.dto.LoginRequest;
import com.octopus.demo.userservice.model.User;
import com.octopus.demo.userservice.service.UserService;
import com.octopus.demo.userservice.vo.LoginVO;
import com.octopus.demo.userservice.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtProperties jwtProperties;

    public AuthController(UserService userService, JwtProperties jwtProperties) {
        this.userService = userService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/login")
    public ResponseEntity<R<LoginVO>> login(@Valid @RequestBody LoginRequest request) {
        Optional<User> authenticated = userService.authenticate(request.getUsername(), request.getPassword());
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(R.fail(401, "用户名或密码错误"));
        }
        return ResponseEntity.ok(R.ok(buildLoginVO(authenticated.get())));
    }

    @RequireAuth
    @GetMapping("/me")
    public R<UserVO> me() {
        return userService.getUserById(UserContext.getUserId())
                .map(UserVO::from)
                .map(R::ok)
                .orElseGet(() -> R.fail(404, "用户不存在"));
    }

    private LoginVO buildLoginVO(User user) {
        String token = JwtUtil.generateToken(user.getId());
        long expiresInSeconds = jwtProperties.getExpirationDays() * 86400L;
        return new LoginVO(token, "Bearer", expiresInSeconds, UserVO.from(user));
    }
}
