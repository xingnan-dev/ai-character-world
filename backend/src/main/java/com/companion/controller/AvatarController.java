package com.companion.controller;

import com.companion.common.result.Result;
import com.companion.dto.request.AvatarCreateRequest;
import com.companion.dto.request.AvatarGenerateRequest;
import com.companion.dto.request.AvatarUpdateRequest;
import com.companion.dto.response.AvatarGenerateResponse;
import com.companion.dto.response.AvatarVO;
import com.companion.security.AuthenticatedUser;
import com.companion.service.AvatarAiService;
import com.companion.service.AvatarService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/avatar")
public class AvatarController {

    private final AvatarService avatarService;
    private final AvatarAiService avatarAiService;

    public AvatarController(AvatarService avatarService, AvatarAiService avatarAiService) {
        this.avatarService = avatarService;
        this.avatarAiService = avatarAiService;
    }

    @PostMapping("/generate")
    public Result<AvatarGenerateResponse> generateAvatar(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AvatarGenerateRequest request) {
        Long userId = user.userId();
        log.info("AI生成形象: userId={}, description={}", userId, request.getDescription());
        AvatarGenerateResponse response = avatarAiService.generateAvatar(userId, request);
        return Result.success(response);
    }

    @PostMapping("/create")
    public Result<AvatarVO> createAvatar(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AvatarCreateRequest request) {
        Long userId = user.userId();
        log.info("创建形象: userId={}, name={}", userId, request.getName());
        AvatarVO avatarVO = avatarService.createAvatar(userId, request);
        return Result.success(avatarVO);
    }

    @GetMapping("/list")
    public Result<List<AvatarVO>> getAvatarList(@AuthenticationPrincipal AuthenticatedUser user) {
        Long userId = user.userId();
        log.info("获取形象列表: userId={}", userId);
        List<AvatarVO> list = avatarService.getAvatarList(userId);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<AvatarVO> getAvatarById(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        Long userId = user.userId();
        log.info("获取形象详情: userId={}, id={}", userId, id);
        AvatarVO avatarVO = avatarService.getAvatarById(userId, id);
        return Result.success(avatarVO);
    }

    @PutMapping("/update")
    public Result<AvatarVO> updateAvatar(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AvatarUpdateRequest request) {
        Long userId = user.userId();
        log.info("更新形象: userId={}, id={}", userId, request.getId());
        AvatarVO avatarVO = avatarService.updateAvatar(userId, request);
        return Result.success(avatarVO);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteAvatar(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        Long userId = user.userId();
        log.info("删除形象: userId={}, id={}", userId, id);
        avatarService.deleteAvatar(userId, id);
        return Result.success();
    }
}
