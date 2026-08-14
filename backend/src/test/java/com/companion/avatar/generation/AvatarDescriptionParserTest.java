package com.companion.avatar.generation;

import com.companion.ai.LlmClient;
import com.companion.ai.PromptBuilder;
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
        parser = new AvatarDescriptionParser(llmClient, promptBuilder, new ObjectMapper(), new AvatarAppearanceNormalizer());
    }

    @Test
    void parsesLlmResultAndIgnoresModelLocationFields() {
        when(llmClient.chat("system", "user")).thenReturn("""
                {"name":"月璃","modelUrl":"https://evil/model.vrm","baseModel":"evil","thumbnailUrl":"evil.png",
                 "appearanceConfig":{"gender":"female","hairColor":"purple","hairStyle":"long","eyeColor":"blue",
                 "bodyType":"slim","outfitStyle":"gothic","outfitColor":"black","earType":"human","wingType":"none",
                 "accessories":["glasses"]},
                 "personality":{"type":"cool","traits":["沉稳"],"speakingStyle":"简洁","slogan":"你好"},"tags":["gothic"]}
                """);

        AvatarDescriptionParser.ParseResult result = parser.parse("紫发哥特女孩");

        assertThat(result.getParseSource()).isEqualTo(AvatarDescriptionParser.ParseSource.LLM);
        assertThat(result.getAppearanceConfig().getHairColor()).isEqualTo("purple");
        assertThat(new ObjectMapper().convertValue(result, new TypeReference<java.util.Map<String, Object>>() { }))
                .doesNotContainKeys("modelUrl", "baseModel", "thumbnailUrl");
    }

    @Test
    void fallsBackToRulesWhenLlmFails() {
        when(llmClient.chat("system", "user")).thenThrow(new IllegalStateException("provider unavailable"));

        AvatarDescriptionParser.ParseResult result = parser.parse("银发短发女孩，戴眼镜");

        assertThat(result.getParseSource()).isEqualTo(AvatarDescriptionParser.ParseSource.RULE_FALLBACK);
        assertThat(result.getAppearanceConfig().getHairColor()).isEqualTo("silver");
        assertThat(result.getAppearanceConfig().getHairStyle()).isEqualTo("short");
        assertThat(result.getAppearanceConfig().getAccessories()).containsExactly("glasses");
    }

    @Test
    void parsesJsonFromMarkdownCodeBlockWithExplanatoryText() {
        when(llmClient.chat("system", "user")).thenReturn("""
                Here is the structured result:
                ```json
                {"name":"Nova","appearanceConfig":{"gender":"female","hairColor":"silver"},
                 "personality":{"type":"gentle","traits":["calm"],"speakingStyle":"soft","slogan":"hello"}}
                ```
                This character is ready.
                """);

        AvatarDescriptionParser.ParseResult result = parser.parse("silver-haired assistant");

        assertThat(result.getParseSource()).isEqualTo(AvatarDescriptionParser.ParseSource.LLM);
        assertThat(result.getName()).isEqualTo("Nova");
        assertThat(result.getAppearanceConfig().getHairColor()).isEqualTo("silver");
    }

    @Test
    void parsesCompleteJsonObjectSurroundedByPlainText() {
        when(llmClient.chat("system", "user")).thenReturn("""
                result follows {"name":"Nova","appearanceConfig":{"gender":"male"},
                "personality":{"traits":["calm"]}} end of result
                """);

        AvatarDescriptionParser.ParseResult result = parser.parse("male assistant");

        assertThat(result.getParseSource()).isEqualTo(AvatarDescriptionParser.ParseSource.LLM);
        assertThat(result.getAppearanceConfig().getGender()).isEqualTo("male");
    }

    @Test
    void fallsBackWhenJsonIsTruncated() {
        when(llmClient.chat("system", "user")).thenReturn("""
                ```json
                {"name":"Nova","appearanceConfig":{"gender":"male","hairColor":"silver"}
                """);

        AvatarDescriptionParser.ParseResult result = parser.parse("male silver-haired assistant");

        assertThat(result.getParseSource()).isEqualTo(AvatarDescriptionParser.ParseSource.RULE_FALLBACK);
        assertThat(result.getAppearanceConfig().getGender()).isEqualTo("male");
    }

    @Test
    void fallsBackWhenJsonIsInvalid() {
        when(llmClient.chat("system", "user")).thenReturn("```json\n{invalid json}\n```");

        AvatarDescriptionParser.ParseResult result = parser.parse("female assistant");

        assertThat(result.getParseSource()).isEqualTo(AvatarDescriptionParser.ParseSource.RULE_FALLBACK);
    }
}
