package com.companion.controller;

import com.companion.common.result.Result;
import com.companion.dto.request.PersonalityCreateRequest;
import com.companion.dto.response.PersonalityVO;
import com.companion.security.AuthenticatedUser;
import com.companion.service.PersonalityService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/personality")
public class PersonalityController {

    private final PersonalityService personalityService;

    public PersonalityController(PersonalityService personalityService) {
        this.personalityService = personalityService;
    }

    @PostMapping("/create")
    public Result<PersonalityVO> createPersonality(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody PersonalityCreateRequest request) {
        Long userId = user.userId();
        log.info("创建人格: userId={}, avatarId={}, name={}", userId, request.getAvatarId(), request.getName());
        PersonalityVO personalityVO = personalityService.createPersonality(
                userId, request.getAvatarId(), request);
        return Result.success(personalityVO);
    }

    @GetMapping("/avatar/{avatarId}")
    public Result<PersonalityVO> getPersonalityByAvatarId(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long avatarId) {
        Long userId = user.userId();
        log.info("获取形象人格: userId={}, avatarId={}", userId, avatarId);
        PersonalityVO personalityVO = personalityService.getPersonalityByAvatarId(userId, avatarId);
        return Result.success(personalityVO);
    }

    @PutMapping("/update")
    public Result<PersonalityVO> updatePersonality(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody PersonalityCreateRequest request) {
        Long userId = user.userId();
        log.info("更新人格: userId={}, avatarId={}", userId, request.getAvatarId());
        PersonalityVO personalityVO = personalityService.updatePersonality(
                userId, request.getAvatarId(), request);
        return Result.success(personalityVO);
    }

    @GetMapping("/templates")
    public Result<List<PersonalityVO>> getTemplateList() {
        log.info("获取人格模板列表");
        List<PersonalityVO> list = personalityService.getTemplateList();
        return Result.success(list);
    }
}
