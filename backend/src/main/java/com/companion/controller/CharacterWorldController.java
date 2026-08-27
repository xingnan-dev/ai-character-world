package com.companion.controller;

import com.companion.common.result.Result;
import com.companion.dto.request.WorldCreateRequest;
import com.companion.dto.request.WorldParseRequest;
import com.companion.dto.request.WorldParticipantReplaceRequest;
import com.companion.dto.request.WorldUpdateRequest;
import com.companion.dto.response.WorldResponse;
import com.companion.security.AuthenticatedUser;
import com.companion.service.CharacterWorldService;
import com.companion.world.draft.WorldDraft;
import com.companion.world.draft.WorldDraftParser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/worlds")
public class CharacterWorldController {

    private final CharacterWorldService worldService;
    private final WorldDraftParser worldDraftParser;

    @PostMapping("/parse")
    public Result<WorldDraft> parse(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody WorldParseRequest request) {
        return Result.success(worldDraftParser.parse(request.getDescription()));
    }

    @GetMapping
    public Result<java.util.List<WorldResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return Result.success(worldService.list(user.userId()));
    }

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

    @PutMapping("/{id}")
    public Result<WorldResponse> update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id,
            @Valid @RequestBody WorldUpdateRequest request) {
        return Result.success(worldService.update(user.userId(), id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        worldService.delete(user.userId(), id);
        return Result.success();
    }

    @PutMapping("/{id}/participants")
    public Result<WorldResponse> replaceParticipants(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id,
            @Valid @RequestBody WorldParticipantReplaceRequest request) {
        return Result.success(worldService.replaceParticipants(user.userId(), id, request));
    }
}
