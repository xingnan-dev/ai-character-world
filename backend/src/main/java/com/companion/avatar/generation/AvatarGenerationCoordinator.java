package com.companion.avatar.generation;

import com.companion.ai.dto.AvatarGenerateResult;
import com.companion.common.exception.BusinessException;
import com.companion.common.result.ResultCode;
import com.companion.dto.request.AvatarGenerateRequest;
import com.companion.dto.response.AvatarGenerateResponse;
import com.companion.dto.response.AvatarVO;
import com.companion.dto.response.PersonalityVO;
import com.companion.entity.Avatar;
import com.companion.entity.AvatarAsset;
import com.companion.entity.AvatarAttribute;
import com.companion.entity.Personality;
import com.companion.mapper.AvatarAttributeMapper;
import com.companion.mapper.AvatarMapper;
import com.companion.mapper.PersonalityMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AvatarGenerationCoordinator {

    private final AvatarDescriptionParser parser;
    private final AvatarAssetSelector assetSelector;
    private final AvatarMapper avatarMapper;
    private final PersonalityMapper personalityMapper;
    private final AvatarAttributeMapper avatarAttributeMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public AvatarGenerateResponse generate(Long userId, AvatarGenerateRequest request) {
        long startedAt = System.nanoTime();
        AvatarDescriptionParser.ParseResult parsed = parser.parse(request.getDescription());
        AvatarAssetSelector.Selection selection = assetSelector.select(parsed.getAppearanceConfig());
        if (selection.matchType() == AvatarAssetSelector.MatchType.UNAVAILABLE) {
            throw new BusinessException(
                    ResultCode.NOT_FOUND.getCode(),
                    "当前资产库没有匹配该外观的3D模型，请调整描述或稍后重试。"
            );
        }
        AvatarAsset asset = selection.asset();
        if (asset == null || asset.getFileUrl() == null || asset.getFileUrl().isBlank()) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "没有可用的形象模型");
        }

        String appearanceJson = writeJson(parsed.getAppearanceConfig());
        String generationJson = writeJson(parsed);
        LocalDateTime now = LocalDateTime.now();

        Avatar avatar = createAvatar(userId, request, parsed, asset, appearanceJson, generationJson, now);
        Personality personality = createPersonality(avatar, parsed, now);
        bindPersonality(avatar, personality, now);
        List<AvatarAttribute> attributes = saveAttributes(avatar.getId(), parsed.getAppearanceConfig(), now);

        long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
        log.info("Avatar generation completed: userId={}, parseSource={}, assetId={}, durationMs={}",
                userId, parsed.getParseSource(), asset.getId(), durationMs);
        return buildResponse(avatar, personality, attributes, parsed, selection);
    }

    private Avatar createAvatar(Long userId, AvatarGenerateRequest request,
                                AvatarDescriptionParser.ParseResult parsed, AvatarAsset asset,
                                String appearanceJson, String generationJson, LocalDateTime now) {
        Avatar avatar = new Avatar();
        avatar.setUserId(userId);
        avatar.setName(parsed.getName());
        avatar.setType(1);
        avatar.setGender(parseGender(parsed.getAppearanceConfig().getGender()));
        avatar.setBaseModel(extractBaseModel(asset.getFileUrl()));
        avatar.setModelUrl(asset.getFileUrl());
        avatar.setThumbnailUrl(asset.getThumbnailUrl());
        avatar.setAppearanceConfig(appearanceJson);
        avatar.setSourceDescription(request.getDescription());
        avatar.setGenerateType(1);
        avatar.setGenerateResult(generationJson);
        avatar.setSlogan(parsed.getPersonality().getSlogan());
        avatar.setStatus(1);
        avatar.setCreateTime(now);
        avatar.setUpdateTime(now);
        if (avatarMapper.insert(avatar) != 1) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "形象创建失败");
        }
        return avatar;
    }

    private Personality createPersonality(Avatar avatar, AvatarDescriptionParser.ParseResult parsed, LocalDateTime now) {
        AvatarGenerateResult.PersonalityConfig config = parsed.getPersonality();
        Personality personality = new Personality();
        personality.setAvatarId(avatar.getId());
        personality.setName(parsed.getName() + "的人格");
        personality.setIsAiGenerated(1);
        personality.setTemplateType(1);
        personality.setCorePersonality(String.join("、", config.getTraits()));
        personality.setIdentity("AI虚拟伴侣");
        personality.setLanguageStyle(config.getSpeakingStyle());
        personality.setHobbies("陪伴、交流");
        personality.setRelationship("朋友");
        personality.setSystemPrompt(buildSystemPrompt(parsed));
        personality.setStatus(1);
        personality.setCreateTime(now);
        personality.setUpdateTime(now);
        if (personalityMapper.insert(personality) != 1) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "人格创建失败");
        }
        return personality;
    }

    private void bindPersonality(Avatar avatar, Personality personality, LocalDateTime now) {
        avatar.setPersonalityId(personality.getId());
        avatar.setUpdateTime(now);
        if (avatarMapper.updateById(avatar) != 1) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "形象人格绑定失败");
        }
    }

    private List<AvatarAttribute> saveAttributes(Long avatarId, AvatarAppearanceConfig config, LocalDateTime now) {
        List<AvatarAttribute> result = new ArrayList<>();
        add(result, avatarId, "hair", "color", config.getHairColor(), now);
        add(result, avatarId, "hair", "style", config.getHairStyle(), now);
        add(result, avatarId, "eye", "color", config.getEyeColor(), now);
        add(result, avatarId, "body", "type", config.getBodyType(), now);
        add(result, avatarId, "ear", "type", config.getEarType(), now);
        add(result, avatarId, "wing", "type", config.getWingType(), now);
        add(result, avatarId, "outfit", "style", config.getOutfitStyle(), now);
        add(result, avatarId, "outfit", "color", config.getOutfitColor(), now);
        if (!config.getAccessories().isEmpty()) {
            add(result, avatarId, "accessory", "types", writeJson(config.getAccessories()), now);
        }
        for (AvatarAttribute attribute : result) {
            if (avatarAttributeMapper.insert(attribute) != 1) {
                throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "形象属性保存失败");
            }
        }
        return result;
    }

    private void add(List<AvatarAttribute> result, Long avatarId, String category, String key,
                     String value, LocalDateTime now) {
        if (value == null || value.isBlank()) return;
        AvatarAttribute attribute = new AvatarAttribute();
        attribute.setAvatarId(avatarId);
        attribute.setCategory(category);
        attribute.setAttrKey(key);
        attribute.setAttrValue(value);
        attribute.setSortOrder(result.size());
        attribute.setStatus(1);
        attribute.setCreateTime(now);
        attribute.setUpdateTime(now);
        result.add(attribute);
    }

    private AvatarGenerateResponse buildResponse(Avatar avatar, Personality personality,
                                                 List<AvatarAttribute> attributes,
                                                 AvatarDescriptionParser.ParseResult parsed,
                                                 AvatarAssetSelector.Selection selection) {
        AvatarGenerateResponse response = new AvatarGenerateResponse();
        AvatarVO avatarVO = new AvatarVO();
        avatarVO.setId(avatar.getId());
        avatarVO.setUserId(avatar.getUserId());
        avatarVO.setName(avatar.getName());
        avatarVO.setType(avatar.getType());
        avatarVO.setGender(avatar.getGender());
        avatarVO.setBaseModel(avatar.getBaseModel());
        avatarVO.setModelUrl(avatar.getModelUrl());
        avatarVO.setThumbnailUrl(avatar.getThumbnailUrl());
        avatarVO.setAppearanceConfig(avatar.getAppearanceConfig());
        avatarVO.setPersonalityId(avatar.getPersonalityId());
        avatarVO.setSlogan(avatar.getSlogan());
        avatarVO.setSourceDescription(avatar.getSourceDescription());
        avatarVO.setGenerateType(avatar.getGenerateType());
        avatarVO.setCreateTime(avatar.getCreateTime().toString());
        response.setAvatar(avatarVO);

        PersonalityVO personalityVO = new PersonalityVO();
        personalityVO.setId(personality.getId());
        personalityVO.setAvatarId(personality.getAvatarId());
        personalityVO.setName(personality.getName());
        personalityVO.setTemplateType(personality.getTemplateType());
        personalityVO.setCorePersonality(personality.getCorePersonality());
        personalityVO.setIdentity(personality.getIdentity());
        personalityVO.setLanguageStyle(personality.getLanguageStyle());
        personalityVO.setHobbies(personality.getHobbies());
        personalityVO.setRelationship(personality.getRelationship());
        personalityVO.setSystemPrompt(personality.getSystemPrompt());
        avatarVO.setPersonality(personalityVO);
        response.setPersonality(personalityVO);

        response.setAttributes(attributes.stream().map(attribute -> new AvatarGenerateResponse.AvatarAttributeVO(
                attribute.getCategory(), attribute.getAttrKey(), attribute.getAttrValue(), attribute.getSortOrder()
        )).toList());
        response.setReasoning("已根据描述解析外观并匹配现有模型资产");
        response.setParsedResult(objectMapper.convertValue(parsed, new TypeReference<Map<String, Object>>() { }));
        response.setParseSource(parsed.getParseSource().name());
        response.setAssetMatchType(selection.matchType().name());
        response.setUnmatchedAttributes(selection.unmatchedAttributes());
        response.setMessage("形象『" + avatar.getName() + "』生成成功！");
        return response;
    }

    private String buildSystemPrompt(AvatarDescriptionParser.ParseResult parsed) {
        AvatarGenerateResult.PersonalityConfig personality = parsed.getPersonality();
        return "你是一个名为" + parsed.getName() + "的AI虚拟角色。"
                + "你的性格：" + String.join("、", personality.getTraits()) + "。"
                + "你的语言风格：" + personality.getSpeakingStyle() + "。"
                + "你的开场白是：『" + personality.getSlogan() + "』。";
    }

    private int parseGender(String gender) {
        return switch (gender) {
            case "male" -> 1;
            case "female" -> 2;
            default -> 3;
        };
    }

    private String extractBaseModel(String modelUrl) {
        String filename = modelUrl.substring(modelUrl.lastIndexOf('/') + 1);
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "形象配置序列化失败");
        }
    }
}
