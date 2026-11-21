package com.companion.ai.memory;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Conservative input boundary: uncertain or quoted fiction is not a user fact. */
final class MemoryInputPolicy {
    private static final Pattern NON_FACT = Pattern.compile(
            "如果|假如|假设|要是|倘若|小说|台词|角色说|角色的台词|\\b(?:if|suppose|imagine|hypothetical|fictional dialogue)\\b",
            Pattern.CASE_INSENSITIVE);

    private MemoryInputPolicy() { }

    static String factualStatements(String input) {
        if (input == null || input.isBlank() || NON_FACT.matcher(input).find()) return "";
        return Arrays.stream(input.split("[，,。.!！；;\\r\\n]+"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .filter(value -> !value.matches(".*[?？].*")
                        && !value.matches(".*(?:什么|是否|吗|呢).*"))
                .filter(value -> !value.toLowerCase(Locale.ROOT).matches(
                        "^(?:what|who|where|when|why|how|do you|can you|could you)\\b.*"))
                .collect(Collectors.joining("，"));
    }
}
