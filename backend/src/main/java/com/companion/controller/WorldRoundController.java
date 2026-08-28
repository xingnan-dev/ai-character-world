package com.companion.controller;

import com.companion.common.result.Result;
import com.companion.dto.request.WorldRoundCreateRequest;
import com.companion.dto.response.WorldEventResponse;
import com.companion.dto.response.WorldExecutionDispatchResponse;
import com.companion.dto.response.WorldRoundResponse;
import com.companion.security.AuthenticatedUser;
import com.companion.service.WorldRoundService;
import com.companion.world.WorldExecutionCommand;
import com.companion.world.WorldExecutionDispatchStatus;
import com.companion.world.WorldRoundExecutionDispatcher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final WorldRoundExecutionDispatcher executionDispatcher;

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

    @GetMapping("/active")
    public Result<WorldRoundResponse> getActive(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long worldId) {
        return Result.success(roundService.getActive(user.userId(), worldId));
    }

    @PostMapping("/{roundId}/execute")
    public ResponseEntity<Result<WorldExecutionDispatchResponse>> execute(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long worldId,
            @PathVariable Long roundId) {
        WorldRoundResponse round = roundService.get(user.userId(), worldId, roundId);
        WorldExecutionDispatchStatus dispatchStatus = executionDispatcher.dispatch(
                new WorldExecutionCommand(user.userId(), worldId, roundId), round);
        if (dispatchStatus == WorldExecutionDispatchStatus.BUSY) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Result.error(503, "WORLD_EXECUTION_BUSY"));
        }
        return ResponseEntity.accepted().body(Result.success(
                new WorldExecutionDispatchResponse(dispatchStatus.name(), round)));
    }

    @GetMapping("/{roundId}/events")
    public Result<List<WorldEventResponse>> getEvents(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long worldId,
            @PathVariable Long roundId) {
        return Result.success(roundService.getEvents(user.userId(), worldId, roundId));
    }
}
