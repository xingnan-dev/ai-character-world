package com.companion.controller;

import com.companion.common.result.Result;
import com.companion.dto.request.WorldCreateRequest;
import com.companion.dto.response.WorldResponse;
import com.companion.security.AuthenticatedUser;
import com.companion.service.CharacterWorldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/worlds")
public class CharacterWorldController {

    private final CharacterWorldService worldService;

    @PostMapping
    public Result<WorldResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody WorldCreateRequest request) {
        return Result.success(worldService.create(user.userId(), request));
    }

    @GetMapping("/{id}")
    public Result<WorldResponse> get(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        return Result.success(worldService.get(user.userId(), id));
    }
}
