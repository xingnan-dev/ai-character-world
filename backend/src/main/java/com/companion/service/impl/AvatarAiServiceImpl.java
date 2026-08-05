package com.companion.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.companion.ai.LlmClient;
import com.companion.ai.ModelMatcher;
import com.companion.ai.PromptBuilder;
import com.companion.ai.dto.AvatarGenerateResult;
import com.companion.common.exception.BusinessException;
import com.companion.common.result.ResultCode;
import com.companion.dto.request.AvatarGenerateRequest;
import com.companion.dto.response.AvatarGenerateResponse;
import com.companion.entity.Avatar;
import com.companion.entity.AvatarAttribute;
import com.companion.entity.Personality;
import com.companion.mapper.AvatarAttributeMapper;
import com.companion.mapper.AvatarMapper;
import com.companion.mapper.PersonalityMapper;
import com.companion.service.AvatarAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvatarAiServiceImpl implements AvatarAiService {

    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final ModelMatcher modelMatcher;
    private final AvatarMapper avatarMapper;
    private final AvatarAttributeMapper avatarAttributeMapper;
    private final PersonalityMapper personalityMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AvatarGenerateResponse generateAvatar(Long userId, AvatarGenerateRequest request) {
        log.info("开始AI生成形象: userId={}, description={}", userId, request.getDescription());

        // Step 1: Call LLM to parse user description
        AvatarGenerateResult parsedResult = parseDescription(request.getDescription());
        log.info("LLM解析完成: name={}, gender={}, personalityType={}", 
                parsedResult.getName(), 
                parsedResult.getAppearanceConfig().getGender(),
                parsedResult.getPersonality().getType());

        // Step 2: Create Personality
        Personality personality = createPersonality(userId, parsedResult);

        // Step 3: Create Avatar
        Avatar avatar = createAvatar(userId, request, parsedResult, personality);

        // Step 4: Create Avatar Attributes
        createAvatarAttributes(avatar.getId(), parsedResult);

        // Step 5: Build Response
        return buildResponse(avatar, personality, parsedResult, request.getDescription());
    }

    private AvatarGenerateResult parseDescription(String description) {
        try {
            String systemPrompt = promptBuilder.buildAvatarGenerateSystemPrompt();
            String userPrompt = promptBuilder.buildAvatarGenerateUserPrompt(description);
            
            String llmResponse = llmClient.chat(systemPrompt, userPrompt);
            log.info("==== LLM原始响应 ====");
            log.info(llmResponse);
            log.info("====================");

            // Clean the response - extract JSON if there's extra text
            String cleanJson = extractJson(llmResponse);
            log.info("提取的JSON: {}", cleanJson);
            AvatarGenerateResult parsedResult = objectMapper.readValue(cleanJson, AvatarGenerateResult.class);
            
            // 防御性检查：确保关键字段不为 null
            if (parsedResult.getAppearanceConfig() == null) {
                parsedResult.setAppearanceConfig(new AvatarGenerateResult.AppearanceConfig());
            }
            if (parsedResult.getPersonality() == null) {
                parsedResult.setPersonality(new AvatarGenerateResult.PersonalityConfig());
            }
            
            AvatarGenerateResult.AppearanceConfig app = parsedResult.getAppearanceConfig();
            AvatarGenerateResult.PersonalityConfig per = parsedResult.getPersonality();
            log.info("解析结果: name={}, hairColor={}, eyeColor={}, outfitStyle={}, personalityType={}",
                    parsedResult.getName(),
                    app.getHairColor(),
                    app.getEyeColor(),
                    app.getOutfitStyle(),
                    per.getType());
            return parsedResult;
        } catch (Exception e) {
            log.error("LLM解析失败，使用降级方案", e);
            return buildFallbackResult(description);
        }
    }

    private String extractJson(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "AI返回内容为空");
        }

        // Try to find JSON object
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        
        throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "AI返回内容格式错误");
    }

    private AvatarGenerateResult buildFallbackResult(String description) {
        log.info("使用降级方案，从关键词提取属性");
        AvatarGenerateResult result = new AvatarGenerateResult();
        AvatarGenerateResult.AppearanceConfig appearance = new AvatarGenerateResult.AppearanceConfig();
        AvatarGenerateResult.PersonalityConfig personality = new AvatarGenerateResult.PersonalityConfig();

        // Default values
        appearance.setGender("female");
        appearance.setHairColor("black");
        appearance.setHairStyle("long");
        appearance.setEyeColor("blue");
        appearance.setBodyType("slim");
        appearance.setEarType("human");
        appearance.setHasWing(false);
        appearance.setOutfitStyle("casual");
        appearance.setOutfitColor("white");

        // Simple keyword matching
        String lowerDesc = description.toLowerCase();
        if (lowerDesc.contains("女") || lowerDesc.contains("female")) appearance.setGender("female");
        else if (lowerDesc.contains("男") || lowerDesc.contains("male")) appearance.setGender("male");
        
        if (lowerDesc.contains("银发") || lowerDesc.contains("silver")) appearance.setHairColor("silver");
        else if (lowerDesc.contains("金发") || lowerDesc.contains("blonde")) appearance.setHairColor("blonde");
        else if (lowerDesc.contains("紫发") || lowerDesc.contains("purple")) appearance.setHairColor("purple");
        else if (lowerDesc.contains("蓝发") || lowerDesc.contains("blue")) appearance.setHairColor("blue");
        
        if (lowerDesc.contains("短发") || lowerDesc.contains("short")) appearance.setHairStyle("short");
        else if (lowerDesc.contains("双马尾") || lowerDesc.contains("twin")) appearance.setHairStyle("twin_tail");
        
        if (lowerDesc.contains("红眼") || lowerDesc.contains("red")) appearance.setEyeColor("red");
        else if (lowerDesc.contains("紫瞳") || lowerDesc.contains("purple")) appearance.setEyeColor("purple");
        else if (lowerDesc.contains("异瞳") || lowerDesc.contains("heterochromia")) appearance.setEyeColor("heterochromia");
        
        if (lowerDesc.contains("猫耳") || lowerDesc.contains("cat")) appearance.setEarType("cat");
        else if (lowerDesc.contains("精灵耳") || lowerDesc.contains("elf")) appearance.setEarType("elf");
        
        if (lowerDesc.contains("翅膀") || lowerDesc.contains("wing")) {
            appearance.setHasWing(true);
            if (lowerDesc.contains("机械") || lowerDesc.contains("mechanical")) appearance.setWingType("mechanical");
            else if (lowerDesc.contains("天使") || lowerDesc.contains("angel")) appearance.setWingType("angel");
            else appearance.setWingType("energy");
        }
        
        if (lowerDesc.contains("机甲") || lowerDesc.contains("armor")) appearance.setOutfitStyle("armor");
        else if (lowerDesc.contains("哥特") || lowerDesc.contains("gothic")) appearance.setOutfitStyle("gothic");
        else if (lowerDesc.contains("未来") || lowerDesc.contains("tech")) appearance.setOutfitStyle("tech_future");
        
        if (lowerDesc.contains("黑") || lowerDesc.contains("black")) appearance.setOutfitColor("black");
        else if (lowerDesc.contains("白") || lowerDesc.contains("white")) appearance.setOutfitColor("white");

        // Personality
        if (lowerDesc.contains("温柔")) {
            personality.setType("gentle");
            personality.setTraits(List.of("温柔", "体贴"));
        } else if (lowerDesc.contains("高冷") || lowerDesc.contains("cool")) {
            personality.setType("cool");
            personality.setTraits(List.of("高冷", "神秘"));
        } else if (lowerDesc.contains("幽默")) {
            personality.setType("humorous");
            personality.setTraits(List.of("幽默", "风趣"));
        } else if (lowerDesc.contains("傲娇") || lowerDesc.contains("tsundere")) {
            personality.setType("tsundere");
            personality.setTraits(List.of("傲娇", "可爱"));
        } else {
            personality.setType("gentle");
            personality.setTraits(List.of("温柔"));
        }
        personality.setSpeakingStyle("温柔细腻");
        personality.setSlogan("你好呀，很高兴认识你~");

        // Name generation
        result.setName(generateName(appearance));
        result.setAppearanceConfig(appearance);
        result.setPersonality(personality);
        result.setTags(List.of(
                appearance.getHairColor(), 
                appearance.getEyeColor(),
                personality.getType()
        ));

        return result;
    }

    private String generateName(AvatarGenerateResult.AppearanceConfig appearance) {
        String color = appearance.getHairColor();
        switch (color) {
            case "silver": return "银月";
            case "purple": return "紫霞";
            case "blue": return "蓝霜";
            case "blonde": return "金璃";
            case "pink": return "樱雪";
            case "red": return "赤绫";
            case "white": return "霜雪";
            default: return "星瑶";
        }
    }

    private Personality createPersonality(Long userId, AvatarGenerateResult result) {
        Personality personality = new Personality();
        personality.setName(result.getName() + "的人格");
        personality.setIsAiGenerated(1);
        personality.setTemplateType(1);
        
        // 防止 traits 为 null 导致 NPE
        List<String> traits = result.getPersonality().getTraits();
        if (traits == null || traits.isEmpty()) {
            traits = List.of("温柔", "体贴");
        }
        personality.setCorePersonality(String.join("、", traits));
        personality.setIdentity("AI虚拟伴侣");
        personality.setLanguageStyle(result.getPersonality().getSpeakingStyle());
        personality.setHobbies("陪伴、交流");
        personality.setRelationship("朋友");
        personality.setSystemPrompt(buildSystemPrompt(result));
        personality.setCreateTime(LocalDateTime.now());
        personality.setUpdateTime(LocalDateTime.now());
        personalityMapper.insert(personality);
        return personality;
    }

    private String buildSystemPrompt(AvatarGenerateResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个名为").append(result.getName() != null ? result.getName() : "AI角色").append("的AI虚拟角色.");
        
        // 防御性获取 personality 信息
        AvatarGenerateResult.PersonalityConfig per = result.getPersonality();
        if (per != null) {
            List<String> traits = per.getTraits();
            String traitsStr = (traits != null && !traits.isEmpty()) ? String.join("、", traits) : "温柔、体贴";
            sb.append("你的性格：").append(traitsStr).append("。");
            sb.append("你的语言风格：").append(per.getSpeakingStyle() != null ? per.getSpeakingStyle() : "温柔细腻").append("。");
            sb.append("你的开场白是：「").append(per.getSlogan() != null ? per.getSlogan() : "你好呀~").append("」。");
        } else {
            sb.append("你的性格：温柔、体贴。");
            sb.append("你的语言风格：温柔细腻。");
            sb.append("你的开场白是：「你好呀~」。");
        }
        return sb.toString();
    }

    private Avatar createAvatar(Long userId, AvatarGenerateRequest request, 
                                 AvatarGenerateResult parsedResult, Personality personality) {
        Avatar avatar = new Avatar();
        avatar.setUserId(userId);
        avatar.setName(parsedResult.getName() != null ? parsedResult.getName() : "AI角色");
        avatar.setType(1);
        
        // 防御性获取 gender
        String gender = "female";
        if (parsedResult.getAppearanceConfig() != null && parsedResult.getAppearanceConfig().getGender() != null) {
            gender = parsedResult.getAppearanceConfig().getGender();
        }
        avatar.setGender(parseGender(gender));
        
        // Use ModelMatcher to find the best VRM model
        String modelUrl = modelMatcher.match(parsedResult);
        avatar.setBaseModel(extractBaseModel(modelUrl));
        avatar.setModelUrl(modelUrl);
        
        avatar.setPersonalityId(personality.getId());
        avatar.setSourceDescription(request.getDescription());
        avatar.setGenerateType(1);
        
        // 防御性获取 slogan
        String slogan = "你好呀，很高兴认识你~";
        if (parsedResult.getPersonality() != null && parsedResult.getPersonality().getSlogan() != null) {
            slogan = parsedResult.getPersonality().getSlogan();
        }
        avatar.setSlogan(slogan);
        
        // Store the parsed result as JSON
        try {
            avatar.setGenerateResult(objectMapper.writeValueAsString(parsedResult));
        } catch (Exception e) {
            log.warn("序列化生成结果失败", e);
        }
        
        avatar.setStatus(1);
        avatar.setCreateTime(LocalDateTime.now());
        avatar.setUpdateTime(LocalDateTime.now());
        avatarMapper.insert(avatar);
        return avatar;
    }

    private String extractBaseModel(String modelUrl) {
        if (modelUrl == null || modelUrl.isEmpty()) {
            return "default";
        }
        // Extract filename without extension
        String filename = modelUrl.substring(modelUrl.lastIndexOf('/') + 1);
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }

    private Integer parseGender(String gender) {
        if (gender == null) return 2;
        switch (gender.toLowerCase()) {
            case "male": return 1;
            case "female": return 2;
            default: return 3;
        }
    }

    private void createAvatarAttributes(Long avatarId, AvatarGenerateResult result) {
        AvatarGenerateResult.AppearanceConfig app = result.getAppearanceConfig();
        int sortOrder = 0;

        // Hair attributes
        if (app.getHairColor() != null) {
            insertAttribute(avatarId, "hair", "color", app.getHairColor(), sortOrder++);
        }
        if (app.getHairStyle() != null) {
            insertAttribute(avatarId, "hair", "style", app.getHairStyle(), sortOrder++);
        }

        // Eye attributes
        if (app.getEyeColor() != null) {
            insertAttribute(avatarId, "eye", "color", app.getEyeColor(), sortOrder++);
        }

        // Body attributes
        if (app.getBodyType() != null) {
            insertAttribute(avatarId, "body", "type", app.getBodyType(), sortOrder++);
        }

        // Ear attributes
        if (app.getEarType() != null) {
            insertAttribute(avatarId, "ear", "type", app.getEarType(), sortOrder++);
        }

        // Wing attributes
        if (Boolean.TRUE.equals(app.getHasWing()) && app.getWingType() != null) {
            insertAttribute(avatarId, "wing", "type", app.getWingType(), sortOrder++);
        }

        // Outfit attributes
        if (app.getOutfitStyle() != null) {
            insertAttribute(avatarId, "outfit", "style", app.getOutfitStyle(), sortOrder++);
        }
        if (app.getOutfitColor() != null) {
            insertAttribute(avatarId, "outfit", "color", app.getOutfitColor(), sortOrder++);
        }

        // Accessory attributes
        if (Boolean.TRUE.equals(app.getHasAccessory()) && app.getAccessoryType() != null) {
            for (String accessory : app.getAccessoryType()) {
                insertAttribute(avatarId, "accessory", "type", accessory, sortOrder++);
            }
        }
    }

    private void insertAttribute(Long avatarId, String category, String attrKey, 
                                  String attrValue, int sortOrder) {
        AvatarAttribute attr = new AvatarAttribute();
        attr.setAvatarId(avatarId);
        attr.setCategory(category);
        attr.setAttrKey(attrKey);
        attr.setAttrValue(attrValue);
        attr.setSortOrder(sortOrder);
        attr.setStatus(1);
        attr.setCreateTime(LocalDateTime.now());
        attr.setUpdateTime(LocalDateTime.now());
        avatarAttributeMapper.insert(attr);
    }

    private AvatarGenerateResponse buildResponse(Avatar avatar, Personality personality,
                                                   AvatarGenerateResult parsedResult, String description) {
        AvatarGenerateResponse response = new AvatarGenerateResponse();
        
        // Build AvatarVO
        com.companion.dto.response.AvatarVO avatarVO = new com.companion.dto.response.AvatarVO();
        avatarVO.setId(avatar.getId());
        avatarVO.setUserId(avatar.getUserId());
        avatarVO.setName(avatar.getName());
        avatarVO.setType(avatar.getType());
        avatarVO.setGender(avatar.getGender());
        avatarVO.setBaseModel(avatar.getBaseModel());
        avatarVO.setModelUrl(avatar.getModelUrl());
        avatarVO.setSourceDescription(avatar.getSourceDescription());
        avatarVO.setGenerateType(avatar.getGenerateType());
        avatarVO.setSlogan(avatar.getSlogan());
        avatarVO.setCreateTime(avatar.getCreateTime() != null ? avatar.getCreateTime().toString() : null);
        response.setAvatar(avatarVO);

        // Build PersonalityVO
        com.companion.dto.response.PersonalityVO personalityVO = new com.companion.dto.response.PersonalityVO();
        personalityVO.setId(personality.getId());
        personalityVO.setName(personality.getName());
        personalityVO.setTemplateType(personality.getTemplateType());
        personalityVO.setCorePersonality(personality.getCorePersonality());
        personalityVO.setLanguageStyle(personality.getLanguageStyle());
        personalityVO.setSystemPrompt(personality.getSystemPrompt());
        response.setPersonality(personalityVO);

        // Build Attributes VO list
        java.util.List<AvatarGenerateResponse.AvatarAttributeVO> attributes = new java.util.ArrayList<>();
        java.util.List<AvatarAttribute> dbAttributes = avatarAttributeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AvatarAttribute>()
                        .eq("avatar_id", avatar.getId())
                        .orderByAsc("sort_order")
        );
        for (AvatarAttribute attr : dbAttributes) {
            AvatarGenerateResponse.AvatarAttributeVO vo = new AvatarGenerateResponse.AvatarAttributeVO();
            vo.setCategory(attr.getCategory());
            vo.setAttrKey(attr.getAttrKey());
            vo.setAttrValue(attr.getAttrValue());
            vo.setSortOrder(attr.getSortOrder());
            attributes.add(vo);
        }
        response.setAttributes(attributes);

        // Set reasoning and parsed result
        response.setReasoning("AI已根据描述生成角色，包含" + 
                parsedResult.getAppearanceConfig().getHairColor() + "发色、" +
                parsedResult.getPersonality().getType() + "性格");
        response.setParsedResult(objectMapper.convertValue(parsedResult, Map.class));
        response.setMessage("形象「" + avatar.getName() + "」生成成功！");

        return response;
    }
}
