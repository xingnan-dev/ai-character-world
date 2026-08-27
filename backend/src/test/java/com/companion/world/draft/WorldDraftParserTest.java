package com.companion.world.draft;

import com.companion.ai.LlmClient;
import com.companion.ai.exception.LlmErrorType;
import com.companion.ai.exception.LlmProviderException;
import com.companion.ai.model.LlmResponse;
import com.companion.ai.prompt.ClasspathPromptTemplateLoader;
import com.companion.ai.prompt.PromptTemplateRenderer;
import com.companion.common.exception.BusinessException;
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
class WorldDraftParserTest {

    @Mock private LlmClient llmClient;
    private WorldDraftParser parser;

    @BeforeEach
    void setUp() {
        PromptTemplateRenderer renderer = new PromptTemplateRenderer();
        parser = new WorldDraftParser(llmClient,
                new ClasspathPromptTemplateLoader(new DefaultResourceLoader(), renderer),
                renderer, new ObjectMapper());
    }

    @Test
    void parsesFencedJsonAndKeepsSourceDescription() {
        when(llmClient.complete(anyString(), anyString(), eq(1200)))
                .thenReturn(response("```json\n" + validJson() + "\n```", "stop"));

        WorldDraft draft = parser.parse("  雨夜侦探城  ");

        assertThat(draft.getName()).isEqualTo("雨夜城");
        assertThat(draft.getSourceDescription()).isEqualTo("雨夜侦探城");
    }

    @Test
    void promptIsWorldSpecificAndDoesNotMentionChatPersonalityOrMemory() {
        org.mockito.ArgumentCaptor<String> system = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<String> user = org.mockito.ArgumentCaptor.forClass(String.class);
        when(llmClient.complete(system.capture(), user.capture(), eq(1200)))
                .thenReturn(response(validJson(), "stop"));

        parser.parse("雨夜世界");

        String prompt = system.getValue() + user.getValue();
        assertThat(prompt).contains("World", "雨夜世界", "不生成主题配置");
        assertThat(prompt).doesNotContain("ChatSession", "ChatMessage", "Personality", "Memory");
    }

    @Test
    void rejectsUnknownWrongTypeBlankOverlongAndTrailingContent() {
        assertInvalid(validJson().replace("}", ",\"theme\":\"dark\"}"));
        assertInvalid(validJson().replace("\"神秘\"", "123"));
        assertInvalid(validJson().replace("\"雨夜城\"", "\"   \""));
        assertInvalid(validJson().replace("\"雨夜城\"", "\"" + "城".repeat(101) + "\""));
        assertInvalid(validJson() + " {}");
    }

    @Test
    void mapsProviderAndTruncationFailuresToStableErrors() {
        when(llmClient.complete(anyString(), anyString(), eq(1200))).thenThrow(
                new LlmProviderException("provider", LlmErrorType.TIMEOUT, null, true, "secret"));
        assertThatThrownBy(() -> parser.parse("雨夜世界"))
                .isInstanceOf(BusinessException.class).hasMessage(WorldDraftParser.PROVIDER_ERROR);

        when(llmClient.complete(anyString(), anyString(), eq(1200)))
                .thenReturn(response(validJson(), "length"));
        assertThatThrownBy(() -> parser.parse("雨夜世界"))
                .isInstanceOf(BusinessException.class).hasMessage(WorldDraftParser.INVALID_OUTPUT);
    }

    private void assertInvalid(String json) {
        assertThatThrownBy(() -> parser.parseJson(json))
                .isInstanceOf(BusinessException.class).hasMessage(WorldDraftParser.INVALID_OUTPUT);
    }

    private LlmResponse response(String content, String finishReason) {
        return new LlmResponse(content, "mock", "model", finishReason, null, null);
    }

    private String validJson() {
        return "{\"name\":\"雨夜城\",\"background\":\"永不停歇的雨\",\"rules\":\"遵守城市法律\","
                + "\"atmosphere\":\"神秘\",\"scene\":\"午夜侦探事务所\"}";
    }
}
