package com.companion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.common.exception.BusinessException;
import com.companion.common.result.ResultCode;
import com.companion.dto.request.AvatarCreateRequest;
import com.companion.dto.request.AvatarPersonalityRequest;
import com.companion.dto.request.AvatarUpdateRequest;
import com.companion.dto.response.AvatarVO;
import com.companion.dto.response.PersonalityVO;
import com.companion.entity.Avatar;
import com.companion.entity.Personality;
import com.companion.mapper.AvatarMapper;
import com.companion.mapper.PersonalityMapper;
import com.companion.service.AvatarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvatarServiceImpl implements AvatarService {

    private final AvatarMapper avatarMapper;
    private final PersonalityMapper personalityMapper;

    @Override
    @Transactional
    public AvatarVO createAvatar(Long userId, AvatarCreateRequest request) {
        Avatar avatar = new Avatar();
        avatar.setUserId(userId);
        avatar.setName(request.getName());
        avatar.setType(request.getType());
        avatar.setBaseModel(request.getBaseModel());
        avatar.setModelUrl(request.getModelUrl());
        avatar.setAppearanceConfig(request.getAppearanceConfig());
        avatar.setSlogan(request.getSlogan());
        avatar.setPersonalityId(request.getPersonalityId());
        avatar.setStatus(1);
        avatar.setCreateTime(LocalDateTime.now());
        avatar.setUpdateTime(LocalDateTime.now());

        Personality personality = request.getPersonalityId() == null
                ? null
                : findUnboundPersonality(request.getPersonalityId());

        avatarMapper.insert(avatar);

        if (personality == null) {
            personality = createPersonalityForAvatar(avatar, request.getPersonality());
            avatar.setPersonalityId(personality.getId());
            avatar.setUpdateTime(LocalDateTime.now());
            if (avatarMapper.updateById(avatar) != 1) {
                throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "形象人格绑定失败");
            }
        } else {
            personality.setAvatarId(avatar.getId());
            personality.setUpdateTime(LocalDateTime.now());
            if (personalityMapper.updateById(personality) != 1) {
                throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "人格形象绑定失败");
            }
        }

        AvatarVO vo = convertToVO(avatar);
        vo.setPersonality(convertPersonalityToVO(personality));
        return vo;
    }

    @Override
    public AvatarVO getAvatarById(Long userId, Long id) {
        Avatar avatar = findOwnedActiveAvatar(userId, id);
        if (avatar == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        AvatarVO vo = convertToVO(avatar);
        Personality personality = findBoundPersonality(avatar);
        if (personality != null) {
            vo.setPersonality(convertPersonalityToVO(personality));
        }
        return vo;
    }

    @Override
    public List<AvatarVO> getAvatarList(Long userId) {
        List<Avatar> avatars = avatarMapper.selectList(
                new QueryWrapper<Avatar>()
                        .eq("user_id", userId)
                        .eq("status", 1)
        );
        return avatars.stream()
                .map(avatar -> {
                    AvatarVO vo = convertToVO(avatar);
                    Personality personality = findBoundPersonality(avatar);
                    if (personality != null) {
                        vo.setPersonality(convertPersonalityToVO(personality));
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public AvatarVO updateAvatar(Long userId, AvatarUpdateRequest request) {
        Avatar avatar = findOwnedActiveAvatar(userId, request.getId());
        if (avatar == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        if (request.getName() != null) {
            avatar.setName(request.getName());
        }
        if (request.getAppearanceConfig() != null) {
            avatar.setAppearanceConfig(request.getAppearanceConfig());
        }
        avatar.setUpdateTime(LocalDateTime.now());
        avatarMapper.updateById(avatar);

        return convertToVO(avatar);
    }

    @Override
    public void deleteAvatar(Long userId, Long id) {
        Avatar avatar = findOwnedActiveAvatar(userId, id);
        if (avatar == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        avatarMapper.deleteById(id);
    }

    private Avatar findOwnedActiveAvatar(Long userId, Long avatarId) {
        return avatarMapper.selectOne(
                new QueryWrapper<Avatar>()
                        .eq("id", avatarId)
                        .eq("user_id", userId)
                        .eq("status", 1)
        );
    }

    private Personality findUnboundPersonality(Long personalityId) {
        Personality personality = personalityMapper.selectOne(
                new QueryWrapper<Personality>()
                        .eq("id", personalityId)
                        .eq("status", 1)
        );
        if (personality == null || personality.getAvatarId() != null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return personality;
    }

    private Personality createPersonalityForAvatar(Avatar avatar, AvatarPersonalityRequest request) {
        AvatarPersonalityRequest source = request == null ? defaultPersonality(avatar.getName()) : request;
        Personality personality = new Personality();
        personality.setAvatarId(avatar.getId());
        personality.setName(source.getName());
        personality.setTemplateType(source.getTemplateType() == null ? 1 : source.getTemplateType());
        personality.setCorePersonality(source.getCorePersonality());
        personality.setIdentity(source.getIdentity());
        personality.setLanguageStyle(source.getLanguageStyle());
        personality.setHobbies(source.getHobbies());
        personality.setRelationship(source.getRelationship());
        personality.setSystemPrompt(buildSystemPrompt(personality));
        personality.setStatus(1);
        personality.setCreateTime(LocalDateTime.now());
        personality.setUpdateTime(LocalDateTime.now());
        personalityMapper.insert(personality);
        return personality;
    }

    private AvatarPersonalityRequest defaultPersonality(String avatarName) {
        return new AvatarPersonalityRequest(
                avatarName + "的人格",
                1,
                "友善、耐心",
                "AI虚拟伴侣",
                "自然、亲切",
                "陪伴、交流",
                "朋友"
        );
    }

    private Personality findBoundPersonality(Avatar avatar) {
        if (avatar == null || avatar.getPersonalityId() == null) {
            return null;
        }
        return personalityMapper.selectOne(
                new QueryWrapper<Personality>()
                        .eq("id", avatar.getPersonalityId())
                        .eq("avatar_id", avatar.getId())
                        .eq("status", 1)
        );
    }

    private String buildSystemPrompt(Personality personality) {
        return "你是一个" + personality.getCorePersonality() + "的角色。"
                + personality.getIdentity() + "。你的说话风格是" + personality.getLanguageStyle()
                + "。你喜欢" + (personality.getHobbies() == null ? "各种有趣的事情" : personality.getHobbies())
                + "。你们之间是" + (personality.getRelationship() == null ? "朋友" : personality.getRelationship())
                + "。请始终保持你的角色设定，用自然的方式交流。";
    }

    private AvatarVO convertToVO(Avatar avatar) {
        AvatarVO vo = new AvatarVO();
        vo.setId(avatar.getId());
        vo.setUserId(avatar.getUserId());
        vo.setName(avatar.getName());
        vo.setType(avatar.getType());
        vo.setGender(avatar.getGender());
        vo.setBaseModel(avatar.getBaseModel());
        vo.setModelUrl(avatar.getModelUrl());
        vo.setThumbnailUrl(avatar.getThumbnailUrl());
        vo.setAppearanceConfig(avatar.getAppearanceConfig());
        vo.setPersonalityId(avatar.getPersonalityId());
        vo.setSlogan(avatar.getSlogan());
        vo.setSourceDescription(avatar.getSourceDescription());
        vo.setGenerateType(avatar.getGenerateType());
        vo.setCreateTime(avatar.getCreateTime() != null ? avatar.getCreateTime().toString() : null);
        return vo;
    }

    private PersonalityVO convertPersonalityToVO(Personality p) {
        PersonalityVO vo = new PersonalityVO();
        vo.setId(p.getId());
        vo.setAvatarId(p.getAvatarId());
        vo.setName(p.getName());
        vo.setTemplateType(p.getTemplateType());
        vo.setCorePersonality(p.getCorePersonality());
        vo.setIdentity(p.getIdentity());
        vo.setLanguageStyle(p.getLanguageStyle());
        vo.setHobbies(p.getHobbies());
        vo.setRelationship(p.getRelationship());
        vo.setSystemPrompt(p.getSystemPrompt());
        return vo;
    }
}
