package com.companion.controller;

import com.companion.common.result.Result;
import com.companion.dto.request.AvatarCreateRequest;
import com.companion.dto.request.AvatarUpdateRequest;
import com.companion.dto.response.AvatarVO;
import com.companion.service.AvatarService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/avatar")
public class AvatarController {

    private final AvatarService avatarService;

    public AvatarController(AvatarService avatarService) {
        this.avatarService = avatarService;
    }

    @PostMapping("/create")
    public Result<AvatarVO> createAvatar(@Valid @RequestBody AvatarCreateRequest request) {
        Long userId = Long.valueOf(
                SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString()
        );
        log.info("创建形象: userId={}, name={}", userId, request.getName());
        AvatarVO avatarVO = avatarService.createAvatar(userId, request);
        return Result.success(avatarVO);
    }

    @GetMapping("/list")
    public Result<List<AvatarVO>> getAvatarList() {
        Long userId = Long.valueOf(
                SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString()
        );
        log.info("获取形象列表: userId={}", userId);
        List<AvatarVO> list = avatarService.getAvatarList(userId);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<AvatarVO> getAvatarById(@PathVariable Long id) {
        log.info("获取形象详情: id={}", id);
        AvatarVO avatarVO = avatarService.getAvatarById(id);
        return Result.success(avatarVO);
    }

    @PutMapping("/update")
    public Result<AvatarVO> updateAvatar(@Valid @RequestBody AvatarUpdateRequest request) {
        Long userId = Long.valueOf(
                SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString()
        );
        log.info("更新形象: userId={}, id={}", userId, request.getId());
        AvatarVO avatarVO = avatarService.updateAvatar(userId, request);
        return Result.success(avatarVO);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteAvatar(@PathVariable Long id) {
        Long userId = Long.valueOf(
                SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString()
        );
        log.info("删除形象: userId={}, id={}", userId, id);
        avatarService.deleteAvatar(userId, id);
        return Result.success();
    }
}