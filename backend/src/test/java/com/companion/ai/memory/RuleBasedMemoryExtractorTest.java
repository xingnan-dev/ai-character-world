package com.companion.ai.memory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedMemoryExtractorTest {

    private final RuleBasedMemoryExtractor extractor = new RuleBasedMemoryExtractor();

    @Test
    void extractsStableProfileKeys() {
        assertThat(extractor.extract("我叫小明")).singleElement().satisfies(memory -> {
            assertThat(memory.memoryKey()).isEqualTo("profile.name");
            assertThat(memory.value()).isEqualTo("小明");
            assertThat(memory.singleValued()).isTrue();
        });
        assertThat(extractor.extract("我今年25岁")).singleElement()
                .extracting(ExtractedMemory::memoryKey).isEqualTo("profile.age");
        assertThat(extractor.extract("我来自杭州")).singleElement()
                .extracting(ExtractedMemory::memoryKey).isEqualTo("profile.location");
        assertThat(extractor.extract("我的职业是程序员")).singleElement()
                .extracting(ExtractedMemory::memoryKey).isEqualTo("profile.job");
    }

    @Test
    void extractsHobbyAsMultiValuedMemoryAndRejectsBlankInput() {
        assertThat(extractor.extract("我喜欢音乐")).singleElement().satisfies(memory -> {
            assertThat(memory.memoryKey()).isEqualTo("preference.hobby");
            assertThat(memory.value()).isEqualTo("音乐");
            assertThat(memory.singleValued()).isFalse();
        });
        assertThat(extractor.extract(" ")).isEmpty();
        assertThat(extractor.extract(null)).isEmpty();
    }
}
