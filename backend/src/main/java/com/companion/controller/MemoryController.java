package com.companion.controller;

import com.companion.common.result.Result;
import com.companion.dto.request.MemoryUpdateRequest;
import com.companion.dto.response.MemoryVO;
import com.companion.security.AuthenticatedUser;
import com.companion.service.MemoryService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @GetMapping("/list")
    public Result<List<MemoryVO>> getMemoryList(@AuthenticationPrincipal AuthenticatedUser user) {
        Long userId = user.userId();
        log.info("获取记忆列表: userId={}", userId);
        List<MemoryVO> list = memoryService.getMemoryList(userId);
        return Result.success(list);
    }

    @PutMapping("/update")
    public Result<MemoryVO> updateMemory(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody MemoryUpdateRequest request) {
        Long userId = user.userId();
        log.info("更新记忆: userId={}, id={}", userId, request.getId());
        MemoryVO memoryVO = memoryService.updateMemory(userId, request);
        return Result.success(memoryVO);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteMemory(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        Long userId = user.userId();
        log.info("删除记忆: userId={}, id={}", userId, id);
        memoryService.deleteMemory(userId, id);
        return Result.success();
    }

    @DeleteMapping("/clear")
    public Result<Void> clearMemory(@AuthenticationPrincipal AuthenticatedUser user) {
        Long userId = user.userId();
        log.info("清空记忆: userId={}", userId);
        memoryService.clearMemory(userId);
        return Result.success();
    }
}
