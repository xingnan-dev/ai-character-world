package com.companion.controller;

import com.companion.common.result.Result;
import com.companion.dto.request.PersonalityCreateRequest;
import com.companion.dto.response.PersonalityVO;
import com.companion.service.PersonalityService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
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
            @Valid @RequestBody PersonalityCreateRequest request) {
        log.info("创建人格: avatarId={}, name={}", request.getAvatarId(), request.getName());
        PersonalityVO personalityVO = personalityService.createPersonality(
                request.getAvatarId(), request);
        return Result.success(personalityVO);
    }

    @GetMapping("/avatar/{avatarId}")
    public Result<PersonalityVO> getPersonalityByAvatarId(@PathVariable Long avatarId) {
        log.info("获取形象人格: avatarId={}", avatarId);
        PersonalityVO personalityVO = personalityService.getPersonalityByAvatarId(avatarId);
        return Result.success(personalityVO);
    }

    @PutMapping("/update")
    public Result<PersonalityVO> updatePersonality(
            @Valid @RequestBody PersonalityCreateRequest request) {
        log.info("更新人格: avatarId={}", request.getAvatarId());
        PersonalityVO personalityVO = personalityService.updatePersonality(
                request.getAvatarId(), request);
        return Result.success(personalityVO);
    }

    @GetMapping("/templates")
    public Result<List<PersonalityVO>> getTemplateList() {
        log.info("获取人格模板列表");
        List<PersonalityVO> list = personalityService.getTemplateList();
        return Result.success(list);
    }
}