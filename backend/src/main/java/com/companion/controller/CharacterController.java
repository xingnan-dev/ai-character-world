package com.companion.controller;

import com.companion.common.result.Result;
import com.companion.character.CharacterParser;
import com.companion.character.model.CharacterDraft;
import com.companion.dto.request.CharacterCreateRequest;
import com.companion.dto.request.CharacterParseRequest;
import com.companion.dto.request.CharacterUpdateRequest;
import com.companion.dto.response.CharacterResponse;
import com.companion.security.AuthenticatedUser;
import com.companion.service.CharacterService;
import com.companion.entity.enums.CharacterType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/characters")
public class CharacterController {

    private final CharacterService characterService;
    private final CharacterParser characterParser;

    @PostMapping("/parse")
    public Result<CharacterDraft> parse(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CharacterParseRequest request) {
        log.info("解析角色草稿: userId={}, type={}", user.userId(), request.getCharacterType());
        CharacterType type = CharacterType.fromName(request.getCharacterType());
        return Result.success(characterParser.parse(type, request.getDescription()));
    }

    @PostMapping
    public Result<CharacterResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CharacterCreateRequest request) {
        log.info("创建角色: userId={}, type={}", user.userId(), request.getCharacterType());
        return Result.success(characterService.create(user.userId(), request));
    }

    @GetMapping
    public Result<List<CharacterResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String type) {
        return Result.success(characterService.list(user.userId(), type));
    }

    @GetMapping("/{id}")
    public Result<CharacterResponse> get(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        return Result.success(characterService.get(user.userId(), id));
    }

    @PutMapping("/{id}")
    public Result<CharacterResponse> update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id,
            @Valid @RequestBody CharacterUpdateRequest request) {
        return Result.success(characterService.update(user.userId(), id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id) {
        characterService.delete(user.userId(), id);
        return Result.success();
    }
}
