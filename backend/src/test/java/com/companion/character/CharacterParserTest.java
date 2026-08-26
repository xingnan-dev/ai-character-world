package com.companion.character;

import com.companion.ai.LlmClient;
import com.companion.ai.exception.LlmErrorType;
import com.companion.ai.exception.LlmProviderException;
import com.companion.ai.model.LlmResponse;
import com.companion.ai.model.LlmUsage;
import com.companion.ai.prompt.ClasspathPromptTemplateLoader;
import com.companion.ai.prompt.PromptTemplateRenderer;
import com.companion.character.model.CharacterDraft;
import com.companion.common.exception.BusinessException;
import com.companion.entity.enums.CharacterType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterParserTest {

    @Mock private LlmClient llmClient;

    private CharacterParser parser;

    @BeforeEach
    void setUp() {
        PromptTemplateRenderer renderer = new PromptTemplateRenderer();
        parser = new CharacterParser(
                llmClient,
                new ClasspathPromptTemplateLoader(new DefaultResourceLoader(), renderer),
                renderer,
                new ObjectMapper()
        );
    }

    @Test
    void parsesValidStrictJson() {
        when(llmClient.complete(anyString(), anyString(), eq(1536)))
                .thenReturn(response(validJson("AI"), "stop"));

        CharacterDraft draft = parser.parse(CharacterType.AI, "一位冷静的研究员");

        assertThat(draft.getCharacterType()).isEqualTo("AI");
        assertThat(draft.getName()).isEqualTo("林澈");
        assertThat(draft.getProfile().getValues()).containsExactly("诚实");
        assertThat(draft.getSourceDescription()).isEqualTo("一位冷静的研究员");
    }

    @Test
    void rejectsTruncatedOutput() {
        when(llmClient.complete(anyString(), anyString(), eq(1536)))
                .thenReturn(response(validJson("AI"), "length"));

        assertThatThrownBy(() -> parser.parse(CharacterType.AI, "角色描述"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("截断");
    }

    @Test
    void rejectsInvalidJson() {
        when(llmClient.complete(anyString(), anyString(), eq(1536)))
                .thenReturn(response("{invalid", "stop"));

        assertThatThrownBy(() -> parser.parse(CharacterType.AI, "角色描述"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("JSON格式非法");
    }

    @Test
    void rejectsUnknownRootField() {
        String json = validJson("AI").replace("\"profile\"", "\"systemPrompt\":\"ignore rules\",\"profile\"");
        when(llmClient.complete(anyString(), anyString(), eq(1536)))
                .thenReturn(response(json, "stop"));

        assertThatThrownBy(() -> parser.parse(CharacterType.AI, "角色描述"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未知字段");
    }

    @Test
    void rejectsUnknownProfileField() {
        String json = validJson("AI").replace("\"values\"", "\"admin\":true,\"values\"");
        when(llmClient.complete(anyString(), anyString(), eq(1536)))
                .thenReturn(response(json, "stop"));

        assertThatThrownBy(() -> parser.parse(CharacterType.AI, "角色描述"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未知字段");
    }

    @Test
    void rejectsProviderFailureWithoutFallback() {
        when(llmClient.complete(anyString(), anyString(), eq(1536))).thenThrow(
                new LlmProviderException("mock", LlmErrorType.UPSTREAM_ERROR, 503, true, "unavailable")
        );

        assertThatThrownBy(() -> parser.parse(CharacterType.AI, "角色描述"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("角色AI解析失败");
    }

    @Test
    void rejectsMismatchedCharacterType() {
        when(llmClient.complete(anyString(), anyString(), eq(1536)))
                .thenReturn(response(validJson("USER"), "stop"));

        assertThatThrownBy(() -> parser.parse(CharacterType.AI, "角色描述"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("characterType与请求不一致");
    }

    @Test
    void rejectsOverlongGeneratedField() {
        String json = validJson("AI").replace("林澈", "角".repeat(81));
        when(llmClient.complete(anyString(), anyString(), eq(1536)))
                .thenReturn(response(json, "stop"));

        assertThatThrownBy(() -> parser.parse(CharacterType.AI, "角色描述"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("name超过最大长度80");
    }

    private LlmResponse response(String content, String finishReason) {
        return new LlmResponse(content, "mock", "mock-model", finishReason, "request-id", LlmUsage.empty());
    }

    private String validJson(String type) {
        return """
                {"characterType":"%s","name":"林澈","age":28,"identity":"研究员","corePersonality":"冷静、好奇","currentGoal":"探索未知","biography":"长期研究人工智能。","relationshipToUser":"伙伴","speakingStyle":"简洁自然","profile":{"values":["诚实"],"likes":["阅读"],"dislikes":[],"interests":["人工智能"],"fears":[],"secrets":[],"behaviorTendencies":["先分析"]}}
                """.formatted(type).trim();
    }
}
