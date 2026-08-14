package com.companion.ai.memory;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RuleBasedMemoryExtractor {

    static final String PROFILE_NAME = "profile.name";
    static final String PROFILE_AGE = "profile.age";
    static final String PROFILE_LOCATION = "profile.location";
    static final String PROFILE_JOB = "profile.job";
    static final String PREFERENCE_HOBBY = "preference.hobby";

    private static final int CATEGORY_PROFILE = 1;
    private static final int CATEGORY_PREFERENCE = 2;

    private static final Pattern NAME_PATTERN = Pattern.compile(
            "(?:我叫(?!什么(?:名字)?|谁)|我的名字是(?!什么|谁))\\s*([^，。！？,!?\\s]{1,50})"
    );
    private static final Pattern AGE_PATTERN = Pattern.compile(
            "我(?:今年)?\\s*(\\d{1,3})\\s*岁"
    );
    private static final Pattern LOCATION_PATTERN = Pattern.compile(
            "(?:我来自|我住在|我在)\\s*([^，。！？,!?\\s]{1,100})"
    );
    private static final Pattern JOB_PATTERN = Pattern.compile(
            "(?:我是(?:一名|一个)?|我的职业是)\\s*([^，。！？,!?\\s]{1,100}?)(?:工作|上班|$)"
    );
    private static final Pattern HOBBY_PATTERN = Pattern.compile(
            "(?:我(?:平时|通常|经常)?(?:喜欢|爱)|我的爱好是|(?:[，,；;]|\\s+)喜欢)"
                    + "\\s*([^，,。；;！？!?\\s]{1,200})"
    );

    public List<ExtractedMemory> extract(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return List.of();
        }

        List<ExtractedMemory> extracted = new ArrayList<>();
        addFirst(extracted, userMessage, NAME_PATTERN,
                PROFILE_NAME, CATEGORY_PROFILE, 0.9f, true);
        addFirst(extracted, userMessage, AGE_PATTERN,
                PROFILE_AGE, CATEGORY_PROFILE, 0.8f, true);
        addFirst(extracted, userMessage, LOCATION_PATTERN,
                PROFILE_LOCATION, CATEGORY_PROFILE, 0.6f, true);
        addFirst(extracted, userMessage, JOB_PATTERN,
                PROFILE_JOB, CATEGORY_PROFILE, 0.6f, true);
        addAll(extracted, userMessage, HOBBY_PATTERN,
                PREFERENCE_HOBBY, CATEGORY_PREFERENCE, 0.7f, false);

        Map<String, ExtractedMemory> distinct = new LinkedHashMap<>();
        for (ExtractedMemory memory : extracted) {
            String identity = memory.singleValued()
                    ? memory.memoryKey()
                    : memory.memoryKey() + "\u0000" + memory.value();
            distinct.put(identity, memory);
        }
        return List.copyOf(distinct.values());
    }

    private void addFirst(List<ExtractedMemory> result, String text, Pattern pattern,
                          String key, int category, float importance, boolean singleValued) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            addMatch(result, matcher.group(1), key, category, importance, singleValued);
        }
    }

    private void addAll(List<ExtractedMemory> result, String text, Pattern pattern,
                        String key, int category, float importance, boolean singleValued) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            addMatch(result, matcher.group(1), key, category, importance, singleValued);
        }
    }

    private void addMatch(List<ExtractedMemory> result, String rawValue,
                          String key, int category, float importance, boolean singleValued) {
        String value = normalize(rawValue);
        if (!value.isEmpty() && isValidValue(key, value)) {
            result.add(new ExtractedMemory(key, value, category, importance, singleValued));
        }
    }

    private boolean isValidValue(String key, String value) {
        if (PROFILE_NAME.equals(key)) {
            return !value.matches("(?:什么(?:名字)?|谁)(?:吗|呢)?")
                    && !value.endsWith("吗")
                    && !value.endsWith("呢");
        }
        if (PREFERENCE_HOBBY.equals(key)) {
            return !value.matches("(?:什么|啥|哪些|哪种|哪一个)(?:爱好)?(?:吗|呢)?")
                    && !value.endsWith("吗")
                    && !value.endsWith("呢");
        }
        return true;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n]+", " ").trim();
    }
}
