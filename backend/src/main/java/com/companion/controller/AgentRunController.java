package com.companion.controller;
import com.companion.common.result.Result;import com.companion.dto.request.AgentRunCreateRequest;import com.companion.dto.response.AgentRunPageResponse;import com.companion.dto.response.AgentRunResponse;import com.companion.security.AuthenticatedUser;import com.companion.service.AgentRunService;import jakarta.validation.Valid;import lombok.RequiredArgsConstructor;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/agent/runs") public class AgentRunController {private final AgentRunService service;
 @PostMapping public Result<AgentRunResponse> create(@AuthenticationPrincipal AuthenticatedUser u,@Valid @RequestBody AgentRunCreateRequest q){return Result.success(service.create(u.userId(),q));}
 @GetMapping public Result<AgentRunPageResponse> list(@AuthenticationPrincipal AuthenticatedUser u,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int pageSize){return Result.success(service.list(u.userId(),page,pageSize));}
 @GetMapping("/{runId}") public Result<AgentRunResponse> get(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable Long runId){return Result.success(service.get(u.userId(),runId));}
 @PostMapping("/{runId}/resume") public Result<AgentRunResponse> resume(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable Long runId){return Result.success(service.resume(u.userId(),runId));}
}
