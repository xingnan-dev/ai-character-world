package com.companion.controller;

import com.companion.common.result.Result;
import com.companion.dto.request.LoginRequest;
import com.companion.dto.request.RegisterRequest;
import com.companion.dto.response.LoginVO;
import com.companion.service.UserService;
import com.companion.service.impl.UserServiceImpl;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    @Autowired(required = false)
    private com.companion.common.utils.RedisUtils redisUtils;

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
        // Redis可选，如果可用则缓存Token
        if (redisUtils != null) {
            redisUtils.setWithExpire(
                    "auth:token:" + loginVO.getToken(),
                    loginVO.getUser().getId(),
                    7L * 24 * 60 * 60
            );
        }
        return Result.success(loginVO);
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("用户登出");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            UserServiceImpl.removeToken(token);
            if (redisUtils != null) {
                redisUtils.delete("auth:token:" + token);
            }
        }
        return Result.success();
    }
}
