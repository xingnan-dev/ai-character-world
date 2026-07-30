package com.companion.controller;

import com.companion.common.result.Result;
import com.companion.dto.response.UserVO;
import com.companion.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/info")
    public Result<UserVO> getUserInfo() {
        Long userId = Long.valueOf(
                SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString()
        );
        log.info("获取用户信息: userId={}", userId);
        UserVO userVO = userService.getUserInfo(userId);
        return Result.success(userVO);
    }

    @PutMapping("/update")
    public Result<UserVO> updateUserInfo(@RequestBody(required = false) java.util.Map<String, String> body) {
        Long userId = Long.valueOf(
                SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString()
        );
        String nickname = body != null ? body.get("nickname") : null;
        String avatarUrl = body != null ? body.get("avatarUrl") : null;
        log.info("更新用户信息: userId={}, nickname={}", userId, nickname);
        UserVO userVO = userService.updateUserInfo(userId, nickname, avatarUrl);
        return Result.success(userVO);
    }
}