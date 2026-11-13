package com.octopus.demo.userservice.controller;

import com.octopus.demo.common.auth.RequireAuth;
import com.octopus.demo.common.auth.UserContext;
import com.octopus.demo.common.bean.R;
import com.octopus.demo.common.util.JwtUtil;
import com.octopus.demo.userservice.config.JwtProperties;
import com.octopus.demo.userservice.dto.LoginRequest;
import com.octopus.demo.userservice.dto.RegisterRequest;
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
@RequestMapping("/api/member/auth")
public class MemberAuthController {

    private static final int STATUS_DISABLED = 0;

    private final UserService userService;
    private final JwtProperties jwtProperties;

    public MemberAuthController(UserService userService, JwtProperties jwtProperties) {
        this.userService = userService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/register")
    public R<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setAge(request.getAge());
        user.setPhone(request.getPhone());
        return R.ok(UserVO.from(userService.createUser(user, request.getPassword())));
    }

    @PostMapping("/login")
    public ResponseEntity<R<LoginVO>> login(@Valid @RequestBody LoginRequest request) {
        Optional<User> authenticated = userService.authenticate(request.getUsername(), request.getPassword());
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(R.fail(401, "用户名或密码错误"));
        }
        User user = authenticated.get();
        if (user.getStatus() != null && user.getStatus() == STATUS_DISABLED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(R.fail(403, "账号已停用"));
        }
        return ResponseEntity.ok(R.ok(buildLoginVO(user)));
    }

    @RequireAuth
    @PostMapping("/logout")
    public R<Void> logout() {
        return R.ok();
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
