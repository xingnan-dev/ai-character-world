package com.companion.ai.memory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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

    @Test
    void distinguishesNameStatementsFromNameQuestions() {
        assertThat(extractor.extract("我叫小明"))
                .extracting(ExtractedMemory::value)
                .containsExactly("小明");
        assertThat(extractor.extract("我叫张三"))
                .extracting(ExtractedMemory::value)
                .containsExactly("张三");

        assertThat(extractor.extract("我叫什么")).isEmpty();
        assertThat(extractor.extract("我叫什么名字？")).isEmpty();
        assertThat(extractor.extract("你知道我叫什么吗？")).isEmpty();
    }

    @Test
    void extractsExplicitHobbiesIncludingCompoundStatements() {
        assertThat(extractor.extract("我喜欢摄影"))
                .extracting(ExtractedMemory::memoryKey, ExtractedMemory::value)
                .containsExactly(tuple("preference.hobby", "摄影"));
        assertThat(extractor.extract("我喜欢拍风景"))
                .extracting(ExtractedMemory::memoryKey, ExtractedMemory::value)
                .containsExactly(tuple("preference.hobby", "拍风景"));
        assertThat(extractor.extract("我叫小明，喜欢摄影"))
                .extracting(ExtractedMemory::memoryKey, ExtractedMemory::value)
                .containsExactly(
                        tuple("profile.name", "小明"),
                        tuple("preference.hobby", "摄影")
                );
    }

    @Test
    void doesNotExtractHobbyFromQuestionsOrOrdinaryChat() {
        assertThat(extractor.extract("我喜欢什么？")).isEmpty();
        assertThat(extractor.extract("你喜欢摄影吗？")).isEmpty();
        assertThat(extractor.extract("摄影真有意思")).isEmpty();
    }
}
