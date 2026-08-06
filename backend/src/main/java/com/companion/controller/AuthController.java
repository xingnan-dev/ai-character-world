package com.companion.controller;

import com.companion.common.result.Result;
import com.companion.dto.request.LoginRequest;
import com.companion.dto.request.RegisterRequest;
import com.companion.dto.response.LoginVO;
import com.companion.security.AuthenticatedUser;
import com.companion.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        log.info("用户注册: {}", request.getUsername());
        userService.register(request);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        log.info("用户登录: {}", request.getUsername());
        LoginVO loginVO = userService.login(request);
        return Result.success(loginVO);
    }

    @PostMapping("/logout")
    public Result<Void> logout(@AuthenticationPrincipal AuthenticatedUser user) {
        log.info("用户登出");
        if (user != null) {
            userService.logout(user.userId(), user.tokenId());
        }
        return Result.success();
    }
}
