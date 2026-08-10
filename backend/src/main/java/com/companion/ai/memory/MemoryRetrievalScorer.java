package com.companion.ai.memory;

import com.companion.entity.UserMemory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class MemoryRetrievalScorer {

    private static final double IMPORTANCE_WEIGHT = 0.45;
    private static final double KEY_INTENT_WEIGHT = 0.35;
    private static final double VALUE_MATCH_WEIGHT = 0.15;
    private static final double FRESHNESS_WEIGHT = 0.05;

    private static final Map<String, List<String>> KEY_INTENTS = Map.of(
            "profile.name", List.of("名字", "姓名", "叫什么", "称呼", "我是谁"),
            "profile.age", List.of("年龄", "几岁", "多大"),
            "profile.location", List.of("哪里", "家乡", "来自", "住在哪", "住哪里"),
            "profile.job", List.of("工作", "职业", "上班", "做什么"),
            "preference.hobby", List.of("喜欢", "爱好", "兴趣", "平时做什么")
    );

    public ScoredMemory score(UserMemory memory, String currentMessage) {
        return score(memory, currentMessage, LocalDateTime.now());
    }

    ScoredMemory score(UserMemory memory, String currentMessage, LocalDateTime now) {
        if (memory == null) {
            return new ScoredMemory(null, 0.0);
        }

        String normalizedMessage = normalize(currentMessage);
        double totalScore = importance(memory.getImportance()) * IMPORTANCE_WEIGHT
                + keyIntent(memory.getMemoryKey(), normalizedMessage) * KEY_INTENT_WEIGHT
                + valueMatch(memory.getValue(), normalizedMessage) * VALUE_MATCH_WEIGHT
                + freshness(memory.getLastAccessTime(), now) * FRESHNESS_WEIGHT;
        return new ScoredMemory(memory, clamp(totalScore));
    }

    private double importance(Float importance) {
        return importance == null || !Float.isFinite(importance) ? 0.0 : clamp(importance);
    }

    private double keyIntent(String memoryKey, String normalizedMessage) {
        if (memoryKey == null || normalizedMessage.isEmpty()) {
            return 0.0;
        }
        List<String> intents = KEY_INTENTS.get(memoryKey.trim().toLowerCase(Locale.ROOT));
        if (intents == null) {
            return 0.0;
        }
        return intents.stream().anyMatch(normalizedMessage::contains) ? 1.0 : 0.0;
    }

    private double valueMatch(String value, String normalizedMessage) {
        String normalizedValue = normalize(value);
        if (normalizedValue.isEmpty() || normalizedMessage.isEmpty()) {
            return 0.0;
        }
        if (normalizedMessage.contains(normalizedValue)) {
            return 1.0;
        }
        if (normalizedMessage.length() >= 2 && normalizedValue.contains(normalizedMessage)) {
            return 0.8;
        }
        return 0.0;
    }

    private double freshness(LocalDateTime lastAccessTime, LocalDateTime now) {
        if (lastAccessTime == null || now == null) {
            return 0.0;
        }
        long days = Math.max(0, Duration.between(lastAccessTime, now).toDays());
        if (days <= 7) {
            return 1.0;
        }
        if (days <= 30) {
            return 0.7;
        }
        if (days <= 90) {
            return 0.4;
        }
        return 0.1;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s，。！？、,.!?;；:：]+", "")
                .trim();
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
