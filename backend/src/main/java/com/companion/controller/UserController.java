package com.companion.controller;

import com.companion.common.result.Result;
import com.companion.dto.request.ChangePasswordRequest;
import com.companion.dto.response.UserVO;
import com.companion.security.AuthenticatedUser;
import com.companion.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public Result<UserVO> getUserInfo(@AuthenticationPrincipal AuthenticatedUser user) {
        Long userId = user.userId();
        log.info("获取用户信息: userId={}", userId);
        UserVO userVO = userService.getUserInfo(userId);
        return Result.success(userVO);
    }

    @PutMapping("/update")
    public Result<UserVO> updateUserInfo(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        Long userId = user.userId();
        String nickname = body != null ? body.get("nickname") : null;
        String avatarUrl = body != null ? body.get("avatarUrl") : null;
        log.info("更新用户信息: userId={}, nickname={}", userId, nickname);
        UserVO userVO = userService.updateUserInfo(userId, nickname, avatarUrl);
        return Result.success(userVO);
    }

    @PutMapping("/password")
    public Result<Void> changePassword(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(user.userId(), request);
        return Result.success();
    }
}
