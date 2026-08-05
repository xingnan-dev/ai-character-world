package com.companion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.common.exception.BusinessException;
import com.companion.common.result.ResultCode;
import com.companion.dto.request.AvatarCreateRequest;
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

        if (request.getPersonalityId() != null) {
            Personality personality = personalityMapper.selectById(request.getPersonalityId());
            if (personality == null) {
                throw new BusinessException(ResultCode.NOT_FOUND);
            }
            if (personality.getAvatarId() != null) {
                Avatar personalityAvatar = findOwnedActiveAvatar(userId, personality.getAvatarId());
                if (personalityAvatar == null) {
                    throw new BusinessException(ResultCode.NOT_FOUND);
                }
            }
        }

        avatarMapper.insert(avatar);

        if (request.getPersonalityId() != null) {
            Personality personality = personalityMapper.selectById(request.getPersonalityId());
            personality.setAvatarId(avatar.getId());
            personalityMapper.updateById(personality);
        }

        return convertToVO(avatar);
    }

    @Override
    public AvatarVO getAvatarById(Long userId, Long id) {
        Avatar avatar = findOwnedActiveAvatar(userId, id);
        if (avatar == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        AvatarVO vo = convertToVO(avatar);
        Personality personality = personalityMapper.selectOne(
                new QueryWrapper<Personality>().eq("avatar_id", id)
        );
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
                .map(this::convertToVO)
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
        avatar.setStatus(0);
        avatar.setUpdateTime(LocalDateTime.now());
        avatarMapper.updateById(avatar);
    }

    private Avatar findOwnedActiveAvatar(Long userId, Long avatarId) {
        return avatarMapper.selectOne(
                new QueryWrapper<Avatar>()
                        .eq("id", avatarId)
                        .eq("user_id", userId)
                        .eq("status", 1)
        );
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
