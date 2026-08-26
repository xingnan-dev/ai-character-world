package com.companion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.character.model.CharacterProfile;
import com.companion.common.exception.BusinessException;
import com.companion.common.result.ResultCode;
import com.companion.dto.request.CharacterCreateRequest;
import com.companion.dto.request.CharacterUpdateRequest;
import com.companion.dto.response.CharacterResponse;
import com.companion.entity.AiCharacter;
import com.companion.entity.Avatar;
import com.companion.entity.enums.CharacterType;
import com.companion.entity.enums.CharacterVisualType;
import com.companion.mapper.AvatarMapper;
import com.companion.mapper.CharacterMapper;
import com.companion.service.CharacterService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CharacterServiceImpl implements CharacterService {

    private static final Pattern AVATAR_COLOR = Pattern.compile(
            "^(#[0-9a-fA-F]{6}|blue|purple|cyan|green|orange|pink|red|gray|indigo|teal)$"
    );

    private final CharacterMapper characterMapper;
    private final AvatarMapper avatarMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public CharacterResponse create(Long userId, CharacterCreateRequest request) {
        CharacterType characterType = CharacterType.fromName(request.getCharacterType());
        CharacterVisualType visualType = CharacterVisualType.fromName(request.getVisualType());
        validateProfile(request.getProfile());
        validateVisual(userId, visualType, request.getAvatarId(), request.getImageUrl());
        String avatarColor = validateColor(request.getAvatarColor());

        LocalDateTime now = LocalDateTime.now();
        AiCharacter character = new AiCharacter();
        character.setUserId(userId);
        character.setCharacterType(characterType.getCode());
        character.setName(requiredTrimmed(request.getName(), "name不能为空"));
        character.setAge(request.getAge());
        character.setIdentity(normalize(request.getIdentity()));
        character.setCorePersonality(normalize(request.getCorePersonality()));
        character.setCurrentGoal(normalize(request.getCurrentGoal()));
        character.setBiography(normalize(request.getBiography()));
        character.setRelationshipToUser(normalize(request.getRelationshipToUser()));
        character.setSpeakingStyle(normalize(request.getSpeakingStyle()));
        character.setProfileConfig(writeProfile(request.getProfile()));
        character.setSourceDescription(normalize(request.getSourceDescription()));
        character.setGenerateType(0);
        character.setVisualType(visualType.getCode());
        character.setAvatarId(request.getAvatarId());
        character.setImageUrl(normalize(request.getImageUrl()));
        character.setAvatarColor(avatarColor == null ? "purple" : avatarColor);
        character.setStatus(1);
        character.setCreateTime(now);
        character.setUpdateTime(now);
        characterMapper.insert(character);
        return toResponse(character);
    }

    @Override
    public List<CharacterResponse> list(Long userId, String type) {
        QueryWrapper<AiCharacter> query = new QueryWrapper<AiCharacter>()
                .eq("user_id", userId)
                .eq("status", 1)
                .orderByDesc("create_time")
                .orderByDesc("id");
        if (type != null && !type.isBlank()) {
            query.eq("character_type", CharacterType.fromName(type).getCode());
        }
        return characterMapper.selectList(query).stream().map(this::toResponse).toList();
    }

    @Override
    public CharacterResponse get(Long userId, Long characterId) {
        return toResponse(findOwnedActiveCharacter(userId, characterId));
    }

    @Override
    @Transactional
    public CharacterResponse update(Long userId, Long characterId, CharacterUpdateRequest request) {
        AiCharacter character = findOwnedActiveCharacter(userId, characterId);
        validateProfile(request.getProfile());

        if (request.getCharacterType() != null) {
            character.setCharacterType(CharacterType.fromName(request.getCharacterType()).getCode());
        }
        setIfPresent(request.getName(), value -> character.setName(requiredTrimmed(value, "name不能为空")));
        if (request.getAge() != null) character.setAge(request.getAge());
        setIfPresent(request.getIdentity(), value -> character.setIdentity(normalize(value)));
        setIfPresent(request.getCorePersonality(), value -> character.setCorePersonality(normalize(value)));
        setIfPresent(request.getCurrentGoal(), value -> character.setCurrentGoal(normalize(value)));
        setIfPresent(request.getBiography(), value -> character.setBiography(normalize(value)));
        setIfPresent(request.getRelationshipToUser(), value -> character.setRelationshipToUser(normalize(value)));
        setIfPresent(request.getSpeakingStyle(), value -> character.setSpeakingStyle(normalize(value)));
        if (request.getProfile() != null) character.setProfileConfig(writeProfile(request.getProfile()));
        setIfPresent(request.getSourceDescription(), value -> character.setSourceDescription(normalize(value)));
        setIfPresent(request.getImageUrl(), value -> character.setImageUrl(normalize(value)));
        if (request.getAvatarColor() != null) character.setAvatarColor(validateColor(request.getAvatarColor()));
        if (request.getAvatarId() != null) character.setAvatarId(request.getAvatarId());

        CharacterVisualType visualType = request.getVisualType() == null
                ? CharacterVisualType.fromCode(character.getVisualType())
                : CharacterVisualType.fromName(request.getVisualType());
        character.setVisualType(visualType.getCode());
        validateVisual(userId, visualType, character.getAvatarId(), character.getImageUrl());

        character.setUpdateTime(LocalDateTime.now());
        characterMapper.updateById(character);
        return toResponse(character);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long characterId) {
        AiCharacter character = findOwnedActiveCharacter(userId, characterId);
        characterMapper.deleteById(character.getId());
    }

    private AiCharacter findOwnedActiveCharacter(Long userId, Long characterId) {
        AiCharacter character = characterMapper.selectOne(
                new QueryWrapper<AiCharacter>()
                        .eq("id", characterId)
                        .eq("user_id", userId)
                        .eq("status", 1)
        );
        if (character == null) throw new BusinessException(ResultCode.NOT_FOUND);
        return character;
    }

    private void validateVisual(Long userId, CharacterVisualType visualType, Long avatarId, String imageUrl) {
        if (visualType == CharacterVisualType.VRM && avatarId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "VRM视觉必须绑定Avatar");
        }
        if (visualType == CharacterVisualType.IMAGE && (imageUrl == null || imageUrl.isBlank())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "IMAGE视觉必须提供imageUrl");
        }
        if (avatarId != null) {
            Avatar avatar = avatarMapper.selectOne(
                    new QueryWrapper<Avatar>()
                            .eq("id", avatarId)
                            .eq("user_id", userId)
                            .eq("status", 1)
            );
            if (avatar == null) throw new BusinessException(ResultCode.NOT_FOUND);
        }
    }

    private void validateProfile(CharacterProfile profile) {
        if (profile == null) return;
        validateItems(profile.getValues());
        validateItems(profile.getLikes());
        validateItems(profile.getDislikes());
        validateItems(profile.getInterests());
        validateItems(profile.getFears());
        validateItems(profile.getSecrets());
        validateItems(profile.getBehaviorTendencies());
    }

    private void validateItems(List<String> items) {
        if (items == null) return;
        if (items.size() > 10) throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "profile每类最多10项");
        for (String item : items) {
            if (item == null || item.isBlank() || item.trim().length() > 200) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "profile单项必须为1到200字符");
            }
        }
    }

    private String validateColor(String value) {
        String normalized = normalize(value);
        if (normalized == null) return null;
        if (!AVATAR_COLOR.matcher(normalized).matches()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "avatarColor不是合法颜色");
        }
        return normalized;
    }

    private String writeProfile(CharacterProfile profile) {
        if (profile == null) return null;
        try {
            return objectMapper.writeValueAsString(profile);
        } catch (JsonProcessingException error) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "角色档案序列化失败");
        }
    }

    private CharacterProfile readProfile(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, CharacterProfile.class);
        } catch (JsonProcessingException error) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "角色档案读取失败");
        }
    }

    private CharacterResponse toResponse(AiCharacter character) {
        CharacterResponse response = new CharacterResponse();
        response.setId(character.getId());
        response.setCharacterType(CharacterType.fromCode(character.getCharacterType()).name());
        response.setName(character.getName());
        response.setAge(character.getAge());
        response.setIdentity(character.getIdentity());
        response.setCorePersonality(character.getCorePersonality());
        response.setCurrentGoal(character.getCurrentGoal());
        response.setBiography(character.getBiography());
        response.setRelationshipToUser(character.getRelationshipToUser());
        response.setSpeakingStyle(character.getSpeakingStyle());
        response.setProfile(readProfile(character.getProfileConfig()));
        response.setSourceDescription(character.getSourceDescription());
        response.setGenerateType(character.getGenerateType());
        response.setVisualType(CharacterVisualType.fromCode(character.getVisualType()).name());
        response.setAvatarId(character.getAvatarId());
        response.setImageUrl(character.getImageUrl());
        response.setAvatarColor(character.getAvatarColor());
        response.setCreateTime(character.getCreateTime());
        response.setUpdateTime(character.getUpdateTime());
        return response;
    }

    private String requiredTrimmed(String value, String message) {
        if (value == null || value.isBlank()) throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
        return value.trim();
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void setIfPresent(String value, Consumer<String> consumer) {
        if (value != null) consumer.accept(value);
    }
}
