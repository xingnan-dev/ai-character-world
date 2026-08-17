package com.companion.avatar.generation;

import com.companion.ai.LlmClient;
import com.companion.ai.PromptBuilder;
import com.companion.ai.config.LlmProperties;
import com.companion.ai.model.LlmResponse;
import com.companion.ai.model.LlmUsage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvatarDescriptionParserTest {

    @Mock private LlmClient llmClient;
    @Mock private PromptBuilder promptBuilder;

    private AvatarDescriptionParser parser;

    @BeforeEach
    void setUp() {
        when(promptBuilder.buildAvatarGenerateSystemPrompt()).thenReturn("system");
        when(promptBuilder.buildAvatarGenerateUserPrompt(anyString())).thenReturn("user");
        LlmProperties properties = new LlmProperties();
        properties.getAvatar().setMaxTokens(1536);
        parser = new AvatarDescriptionParser(
                llmClient, promptBuilder, new ObjectMapper(), new AvatarAppearanceNormalizer(), properties);
    }

    @Test
    void parsesLlmResultAndIgnoresModelLocationFields() {
        when(llmClient.complete("system", "user", 1536)).thenReturn(response("""
                {"name":"Nova","modelUrl":"https://evil/model.vrm","baseModel":"evil","thumbnailUrl":"evil.png",
                 "appearanceConfig":{"gender":"female","hairColor":"purple","hairStyle":"long","eyeColor":"blue",
                 "bodyType":"slim","outfitStyle":"gothic","outfitColor":"black","earType":"human","wingType":"none",
                 "accessories":["glasses"]},
                 "personality":{"type":"cool","traits":["calm"],"speakingStyle":"brief","slogan":"hello"},
                 "tags":["gothic"]}
                """, "stop"));

        AvatarDescriptionParser.ParseResult result = parser.parse("purple-haired gothic assistant");

        assertThat(result.getParseSource()).isEqualTo(AvatarDescriptionParser.ParseSource.LLM);
        assertThat(result.getAppearanceConfig().getHairColor()).isEqualTo("purple");
        assertThat(new ObjectMapper().convertValue(result, new TypeReference<java.util.Map<String, Object>>() { }))
                .doesNotContainKeys("modelUrl", "baseModel", "thumbnailUrl");
    }

    @Test
    void fallsBackToRulesWhenLlmFails() {
        when(llmClient.complete("system", "user", 1536))
                .thenThrow(new IllegalStateException("provider unavailable"));

        AvatarDescriptionParser.ParseResult result = parser.parse("female silver short hair with glasses");

        assertThat(result.getParseSource()).isEqualTo(AvatarDescriptionParser.ParseSource.RULE_FALLBACK);
        assertThat(result.getAppearanceConfig().getHairColor()).isEqualTo("silver");
        assertThat(result.getAppearanceConfig().getHairStyle()).isEqualTo("short");
        assertThat(result.getAppearanceConfig().getAccessories()).containsExactly("glasses");
    }

    @Test
    void parsesJsonFromMarkdownCodeBlockWithExplanatoryText() {
        when(llmClient.complete("system", "user", 1536)).thenReturn(response("""
                Here is the structured result:
                ```json
                {"name":"Nova","appearanceConfig":{"gender":"female","hairColor":"silver"},
                 "personality":{"type":"gentle","traits":["calm"],"speakingStyle":"soft","slogan":"hello"}}
                ```
                This character is ready.
                """, "stop"));

        AvatarDescriptionParser.ParseResult result = parser.parse("silver-haired assistant");

        assertThat(result.getParseSource()).isEqualTo(AvatarDescriptionParser.ParseSource.LLM);
        assertThat(result.getName()).isEqualTo("Nova");
        assertThat(result.getAppearanceConfig().getHairColor()).isEqualTo("silver");
    }

    @Test
    void parsesCompleteJsonObjectSurroundedByPlainText() {
        when(llmClient.complete("system", "user", 1536)).thenReturn(response("""
                result follows {"name":"Nova","appearanceConfig":{"gender":"male"},
                "personality":{"traits":["calm"]}} end of result
                """, "stop"));

        AvatarDescriptionParser.ParseResult result = parser.parse("male assistant");

        assertThat(result.getParseSource()).isEqualTo(AvatarDescriptionParser.ParseSource.LLM);
        assertThat(result.getAppearanceConfig().getGender()).isEqualTo("male");
    }

    @Test
    void fallsBackWhenJsonIsTruncated() {
        when(llmClient.complete("system", "user", 1536)).thenReturn(response("""
                ```json
                {"name":"Nova","appearanceConfig":{"gender":"male","hairColor":"silver"}
                """, "stop"));

        AvatarDescriptionParser.ParseResult result = parser.parse("male silver-haired assistant");

        assertThat(result.getParseSource()).isEqualTo(AvatarDescriptionParser.ParseSource.RULE_FALLBACK);
        assertThat(result.getAppearanceConfig().getGender()).isEqualTo("male");
    }

    @Test
    void fallsBackWhenJsonIsInvalid() {
        when(llmClient.complete("system", "user", 1536))
                .thenReturn(response("```json\n{invalid json}\n```", "stop"));

        AvatarDescriptionParser.ParseResult result = parser.parse("female assistant");

        assertThat(result.getParseSource()).isEqualTo(AvatarDescriptionParser.ParseSource.RULE_FALLBACK);
    }

    @Test
    void finishReasonLengthUsesRuleFallbackWithoutChangingParseSourceSemantics() {
        when(llmClient.complete("system", "user", 1536)).thenReturn(response(
                "{\"appearanceConfig\":{\"gender\":\"male\"}}", "length"));

        AvatarDescriptionParser.ParseResult result = parser.parse("male assistant");

        assertThat(result.getParseSource()).isEqualTo(AvatarDescriptionParser.ParseSource.RULE_FALLBACK);
        assertThat(result.getAppearanceConfig().getGender()).isEqualTo("male");
    }

    private LlmResponse response(String content, String finishReason) {
        return new LlmResponse(content, "test", "test-model", finishReason, null, LlmUsage.empty());
    }
}
