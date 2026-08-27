package com.companion.controller;

import com.companion.common.result.Result;
import com.companion.dto.request.WorldRoundCreateRequest;
import com.companion.dto.response.WorldEventResponse;
import com.companion.dto.response.WorldRoundResponse;
import com.companion.security.AuthenticatedUser;
import com.companion.service.WorldRoundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/worlds/{worldId}/rounds")
public class WorldRoundController {

    private final WorldRoundService roundService;

    @PostMapping
    public Result<WorldRoundResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long worldId,
            @Valid @RequestBody WorldRoundCreateRequest request) {
        return Result.success(roundService.create(user.userId(), worldId, request));
    }

    @GetMapping("/{roundId}")
    public Result<WorldRoundResponse> get(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long worldId,
            @PathVariable Long roundId) {
        return Result.success(roundService.get(user.userId(), worldId, roundId));
    }

    @PostMapping("/{roundId}/execute")
    public Result<WorldRoundResponse> execute(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long worldId,
            @PathVariable Long roundId) {
        return Result.success(roundService.execute(user.userId(), worldId, roundId));
    }

    @GetMapping("/{roundId}/events")
    public Result<List<WorldEventResponse>> getEvents(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long worldId,
            @PathVariable Long roundId) {
        return Result.success(roundService.getEvents(user.userId(), worldId, roundId));
    }
}
