package com.companion.ai.memory;

import com.companion.entity.UserMemory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryRetrievalScorerTest {

    private final MemoryRetrievalScorer scorer = new MemoryRetrievalScorer();
    private final LocalDateTime now = LocalDateTime.of(2026, 8, 10, 12, 0);

    @Test
    void prioritizesSupportedProfileIntents() {
        assertIntent("profile.name", "你还记得我叫什么名字吗", "profile.job");
        assertIntent("profile.age", "你知道我多大了吗", "profile.name");
        assertIntent("profile.location", "我住在哪里", "profile.age");
        assertIntent("profile.job", "我的职业是什么", "profile.location");
    }

    @Test
    void matchesHobbyIntentAndMemoryValue() {
        UserMemory hobby = memory("preference.hobby", "音乐", 0.5f, now.minusDays(10));
        UserMemory unrelatedHobby = memory("preference.hobby", "阅读", 0.5f, now.minusDays(10));

        double valueMatched = scorer.score(hobby, "我喜欢什么音乐", now).totalScore();
        double onlyIntentMatched = scorer.score(unrelatedHobby, "我喜欢什么音乐", now).totalScore();

        assertThat(valueMatched).isGreaterThan(onlyIntentMatched);
    }

    @Test
    void importanceAndFreshnessContributeToScore() {
        UserMemory important = memory("custom.fact", "alpha", 0.9f, now.minusDays(100));
        UserMemory lessImportant = memory("custom.fact", "alpha", 0.2f, now.minusDays(100));
        assertThat(scorer.score(important, "unrelated", now).totalScore())
                .isGreaterThan(scorer.score(lessImportant, "unrelated", now).totalScore());

        UserMemory fresh = memory("custom.fact", "alpha", 0.5f, now.minusDays(1));
        UserMemory stale = memory("custom.fact", "alpha", 0.5f, now.minusDays(100));
        assertThat(scorer.score(fresh, "unrelated", now).totalScore())
                .isGreaterThan(scorer.score(stale, "unrelated", now).totalScore());
    }

    @Test
    void isNullSafeBoundedAndStable() {
        assertThat(scorer.score(null, null, now).totalScore()).isZero();

        UserMemory incomplete = new UserMemory();
        double first = scorer.score(incomplete, null, now).totalScore();
        double second = scorer.score(incomplete, null, now).totalScore();
        assertThat(first).isBetween(0.0, 1.0).isEqualTo(second);

        UserMemory excessive = memory("profile.name", "Amy", 10.0f, now.plusDays(1));
        assertThat(scorer.score(excessive, "我的名字是Amy", now).totalScore())
                .isBetween(0.0, 1.0);
    }

    private void assertIntent(String expectedKey, String question, String unrelatedKey) {
        UserMemory expected = memory(expectedKey, "expected", 0.5f, now.minusDays(10));
        UserMemory unrelated = memory(unrelatedKey, "unrelated", 0.5f, now.minusDays(10));

        assertThat(scorer.score(expected, question, now).totalScore())
                .isGreaterThan(scorer.score(unrelated, question, now).totalScore());
    }

    private UserMemory memory(String key, String value, Float importance, LocalDateTime lastAccessTime) {
        UserMemory memory = new UserMemory();
        memory.setMemoryKey(key);
        memory.setValue(value);
        memory.setImportance(importance);
        memory.setLastAccessTime(lastAccessTime);
        return memory;
    }
}
