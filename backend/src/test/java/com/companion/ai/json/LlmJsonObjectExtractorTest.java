package com.companion.ai.json;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmJsonObjectExtractorTest {

    private static final String JSON = "{\"value\":1}";

    @Test
    void scansStringsEscapesAndNestedObjectsWithoutRepairingJson() {
        String json = "{\"text\":\"brace { } and quote \\\"ok\\\"\",\"nested\":{\"value\":1}}";

        assertThat(LlmJsonObjectExtractor.extract("说明\n" + json + "\n完成")).isEqualTo(json);
        assertThatThrownBy(() -> LlmJsonObjectExtractor.extract("说明 {\"broken\":true"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMultipleFencesUnknownLanguagesAndArrays() {
        assertThatThrownBy(() -> LlmJsonObjectExtractor.extract("```json\n{}\n```\n```json\n{}\n```"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LlmJsonObjectExtractor.extract("```javascript\n{}\n```"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LlmJsonObjectExtractor.extract("[{\"value\":1}]"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LlmJsonObjectExtractor.extract("结果是 [{\"value\":1}]，请确认"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsStrictObjectsOutsideTheOnlyFencedBlock() {
        String fenced = "```json\n" + JSON + "\n```";

        assertRejected("{\"outside\":1}\n" + fenced);
        assertRejected(fenced + "\n{\"outside\":1}");
        assertRejected("说明 [{\"outside\":1}]\n" + fenced);
        assertRejected("{\"before\":1}\n" + fenced + "\n{\"after\":1}");
    }

    @Test
    void acceptsOrdinaryExplanationAndInvalidBracesOutsideOneFence() {
        String fenced = "```json\n" + JSON + "\n```";

        assertThat(LlmJsonObjectExtractor.extract("说明文字。\n" + fenced + "\n请确认！"))
                .isEqualTo(JSON);
        assertThat(LlmJsonObjectExtractor.extract("模板 {名称} 尚未填写。\n" + fenced))
                .isEqualTo(JSON);
        assertThat(LlmJsonObjectExtractor.extract("未完成 {\"broken\":\n" + fenced))
                .isEqualTo(JSON);
    }

    @Test
    void doesNotTreatFenceCharactersInsideAJsonStringAsAnotherBlock() {
        String json = "{\"text\":\"引用 ```json 和 ``` 符号\"}";

        assertThat(LlmJsonObjectExtractor.extract("```json\n" + json + "\n```"))
                .isEqualTo(json);
    }

    @Test
    void errorsNeverContainTheRawModelOutput() {
        String secret = "MODEL_SECRET_84729";

        assertThatThrownBy(() -> LlmJsonObjectExtractor.extract(
                "{\"secret\":\"" + secret + "\"}\n```json\n" + JSON + "\n```"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining(secret);
    }

    private void assertRejected(String content) {
        assertThatThrownBy(() -> LlmJsonObjectExtractor.extract(content))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
