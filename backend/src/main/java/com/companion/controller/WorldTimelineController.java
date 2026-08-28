package com.companion.controller;

import com.companion.common.result.Result;
import com.companion.dto.response.WorldTimelinePageResponse;
import com.companion.security.AuthenticatedUser;
import com.companion.service.WorldRoundService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/worlds/{worldId}/timeline")
public class WorldTimelineController {

    private final WorldRoundService roundService;

    @GetMapping
    public Result<WorldTimelinePageResponse> getTimeline(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long worldId,
            @RequestParam(required = false) Long beforeRoundId,
            @RequestParam(required = false) Integer limit) {
        return Result.success(roundService.getTimeline(user.userId(), worldId, beforeRoundId, limit));
    }
}
