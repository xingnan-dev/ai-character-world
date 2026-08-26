package com.companion.character;

import com.companion.ai.LlmClient;
import com.companion.ai.exception.LlmProviderException;
import com.companion.ai.model.LlmResponse;
import com.companion.ai.prompt.PromptTemplateKey;
import com.companion.ai.prompt.PromptTemplateLoader;
import com.companion.ai.prompt.PromptTemplateRenderer;
import com.companion.character.model.CharacterDraft;
import com.companion.character.model.CharacterProfile;
import com.companion.common.exception.BusinessException;
import com.companion.common.result.ResultCode;
import com.companion.entity.enums.CharacterType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CharacterParser {

    private static final int MAX_TOKENS = 1536;
    private static final Set<String> ROOT_FIELDS = Set.of(
            "characterType", "name", "age", "identity", "corePersonality", "currentGoal",
            "biography", "relationshipToUser", "speakingStyle", "profile"
    );
    private static final Set<String> PROFILE_FIELDS = Set.of(
            "values", "likes", "dislikes", "interests", "fears", "secrets", "behaviorTendencies"
    );

    private final LlmClient llmClient;
    private final PromptTemplateLoader templateLoader;
    private final PromptTemplateRenderer templateRenderer;
    private final ObjectMapper objectMapper;

    public CharacterDraft parse(CharacterType requestedType, String description) {
        String normalizedDescription = description == null ? null : description.trim();
        if (normalizedDescription == null || normalizedDescription.isEmpty() || normalizedDescription.length() > 2000) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "description长度必须为1到2000字符");
        }

        String systemPrompt = templateRenderer.render(
                templateLoader.load(PromptTemplateKey.CHARACTER_PARSE_SYSTEM), Map.of()
        );
        String userPrompt = templateRenderer.render(
                templateLoader.load(PromptTemplateKey.CHARACTER_PARSE_USER),
                Map.of("characterType", requestedType.name(), "description", normalizedDescription)
        );

        LlmResponse response;
        try {
            response = llmClient.complete(systemPrompt, userPrompt, MAX_TOKENS);
        } catch (LlmProviderException error) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "角色AI解析失败，请稍后重试");
        } catch (RuntimeException error) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "角色AI解析失败，请稍后重试");
        }
        if (response == null || isTruncated(response.finishReason())) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "角色AI输出被截断，请重试");
        }

        CharacterDraft draft = parseJson(response.content());
        CharacterType parsedType;
        try {
            parsedType = CharacterType.fromName(draft.getCharacterType());
        } catch (BusinessException error) {
            throw invalidOutput("characterType只能是AI或USER");
        }
        if (parsedType != requestedType) {
            throw invalidOutput("characterType与请求不一致");
        }
        validateDraft(draft);
        draft.setCharacterType(requestedType.name());
        draft.setSourceDescription(normalizedDescription);
        return draft;
    }

    CharacterDraft parseJson(String content) {
        if (content == null || content.isBlank()) {
            throw invalidOutput("LLM返回为空");
        }
        try {
            ObjectMapper strictMapper = objectMapper.copy()
                    .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
            JsonNode root = strictMapper.readTree(content);
            if (root == null || !root.isObject()) {
                throw invalidOutput("根节点必须是JSON对象");
            }
            rejectUnknownFields(root, ROOT_FIELDS, "角色");
            JsonNode profile = root.get("profile");
            if (profile == null || !profile.isObject()) {
                throw invalidOutput("profile必须是JSON对象");
            }
            rejectUnknownFields(profile, PROFILE_FIELDS, "profile");
            return strictMapper.treeToValue(root, CharacterDraft.class);
        } catch (BusinessException error) {
            throw error;
        } catch (JsonProcessingException | IllegalArgumentException error) {
            throw invalidOutput("JSON格式非法");
        }
    }

    private void rejectUnknownFields(JsonNode node, Set<String> whitelist, String scope) {
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (!whitelist.contains(name)) {
                throw invalidOutput(scope + "包含未知字段: " + name);
            }
        }
    }

    private void validateDraft(CharacterDraft draft) {
        draft.setName(required(draft.getName(), 80, "name"));
        if (draft.getAge() != null && (draft.getAge() < 0 || draft.getAge() > 150)) {
            throw invalidOutput("age必须在0到150之间");
        }
        draft.setIdentity(optional(draft.getIdentity(), 200, "identity"));
        draft.setCorePersonality(optional(draft.getCorePersonality(), 1000, "corePersonality"));
        draft.setCurrentGoal(optional(draft.getCurrentGoal(), 500, "currentGoal"));
        draft.setBiography(optional(draft.getBiography(), 5000, "biography"));
        draft.setRelationshipToUser(optional(draft.getRelationshipToUser(), 300, "relationshipToUser"));
        draft.setSpeakingStyle(optional(draft.getSpeakingStyle(), 500, "speakingStyle"));
        validateProfile(draft.getProfile());
    }

    private void validateProfile(CharacterProfile profile) {
        if (profile == null) throw invalidOutput("profile不能为空");
        validateItems(profile.getValues(), "values");
        validateItems(profile.getLikes(), "likes");
        validateItems(profile.getDislikes(), "dislikes");
        validateItems(profile.getInterests(), "interests");
        validateItems(profile.getFears(), "fears");
        validateItems(profile.getSecrets(), "secrets");
        validateItems(profile.getBehaviorTendencies(), "behaviorTendencies");
    }

    private void validateItems(java.util.List<String> items, String field) {
        if (items == null) throw invalidOutput(field + "必须是数组");
        if (items.size() > 10) throw invalidOutput(field + "最多10项");
        for (int index = 0; index < items.size(); index++) {
            String normalized = required(items.get(index), 200, field);
            items.set(index, normalized);
        }
    }

    private String required(String value, int max, String field) {
        if (value == null || value.isBlank()) throw invalidOutput(field + "不能为空");
        String normalized = value.trim();
        if (normalized.length() > max) throw invalidOutput(field + "超过最大长度" + max);
        return normalized;
    }

    private String optional(String value, int max, String field) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.isEmpty()) return null;
        if (normalized.length() > max) throw invalidOutput(field + "超过最大长度" + max);
        return normalized;
    }

    private boolean isTruncated(String finishReason) {
        return finishReason != null && "length".equalsIgnoreCase(finishReason.trim());
    }

    private BusinessException invalidOutput(String detail) {
        return new BusinessException(ResultCode.SERVER_ERROR.getCode(), "角色AI输出无效：" + detail);
    }
}
