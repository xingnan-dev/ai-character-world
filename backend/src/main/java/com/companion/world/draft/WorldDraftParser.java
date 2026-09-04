package com.companion.world.draft;

import com.companion.ai.LlmClient;
import com.companion.ai.json.LlmJsonObjectExtractor;
import com.companion.ai.exception.LlmProviderException;
import com.companion.ai.model.LlmResponse;
import com.companion.ai.prompt.PromptTemplateKey;
import com.companion.ai.prompt.PromptTemplateLoader;
import com.companion.ai.prompt.PromptTemplateRenderer;
import com.companion.common.exception.BusinessException;
import com.companion.common.result.ResultCode;
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
public class WorldDraftParser {

    public static final String PROVIDER_ERROR = "WORLD_DRAFT_PROVIDER_ERROR";
    public static final String INVALID_OUTPUT = "WORLD_DRAFT_INVALID_OUTPUT";
    private static final int MAX_TOKENS = 1200;
    private static final Set<String> FIELDS = Set.of("name", "background", "rules", "atmosphere", "scene");
    private final LlmClient llmClient;
    private final PromptTemplateLoader templateLoader;
    private final PromptTemplateRenderer templateRenderer;
    private final ObjectMapper objectMapper;

    public WorldDraft parse(String description) {
        String normalized = required(description, 2000, "description");
        String systemPrompt = templateRenderer.render(
                templateLoader.load(PromptTemplateKey.WORLD_DRAFT_SYSTEM), Map.of());
        String userPrompt = templateRenderer.render(
                templateLoader.load(PromptTemplateKey.WORLD_DRAFT_USER), Map.of("description", normalized));
        LlmResponse response;
        try {
            response = llmClient.complete(systemPrompt, userPrompt, MAX_TOKENS);
        } catch (LlmProviderException error) {
            throw stable(PROVIDER_ERROR);
        } catch (RuntimeException error) {
            throw stable(PROVIDER_ERROR);
        }
        if (response == null || "length".equalsIgnoreCase(response.finishReason())) {
            throw stable(INVALID_OUTPUT);
        }
        WorldDraft draft = parseJson(response.content());
        draft.setSourceDescription(normalized);
        return draft;
    }

    WorldDraft parseJson(String content) {
        if (content == null || content.isBlank()) throw stable(INVALID_OUTPUT);
        try {
            ObjectMapper strict = objectMapper.copy().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
            JsonNode root = strict.readTree(LlmJsonObjectExtractor.extract(content));
            if (root == null || !root.isObject()) throw stable(INVALID_OUTPUT);
            Iterator<String> names = root.fieldNames();
            while (names.hasNext()) {
                if (!FIELDS.contains(names.next())) throw stable(INVALID_OUTPUT);
            }
            for (String field : FIELDS) {
                JsonNode value = root.get(field);
                if (value == null || !value.isTextual()) throw stable(INVALID_OUTPUT);
            }
            WorldDraft draft = strict.treeToValue(root, WorldDraft.class);
            draft.setName(required(draft.getName(), 100, "name"));
            draft.setBackground(required(draft.getBackground(), 2000, "background"));
            draft.setRules(required(draft.getRules(), 2000, "rules"));
            draft.setAtmosphere(required(draft.getAtmosphere(), 500, "atmosphere"));
            draft.setScene(required(draft.getScene(), 1000, "scene"));
            return draft;
        } catch (BusinessException error) {
            throw error;
        } catch (JsonProcessingException | IllegalArgumentException error) {
            throw stable(INVALID_OUTPUT);
        }
    }

    private String required(String value, int maxLength, String field) {
        if (value == null || value.isBlank()) {
            if ("description".equals(field)) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "description长度必须为1到2000字符");
            }
            throw stable(INVALID_OUTPUT);
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            if ("description".equals(field)) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "description长度必须为1到2000字符");
            }
            throw stable(INVALID_OUTPUT);
        }
        return normalized;
    }

    private BusinessException stable(String code) {
        return new BusinessException(ResultCode.SERVER_ERROR.getCode(), code);
    }
}
