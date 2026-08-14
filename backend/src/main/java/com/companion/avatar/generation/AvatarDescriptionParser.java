package com.companion.avatar.generation;

import com.companion.ai.LlmClient;
import com.companion.ai.PromptBuilder;
import com.companion.ai.dto.AvatarGenerateResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class AvatarDescriptionParser {

    private static final Pattern JSON_CODE_BLOCK = Pattern.compile(
            "```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    public enum ParseSource {
        LLM,
        RULE_FALLBACK
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParseResult {
        private String name;
        private AvatarAppearanceConfig appearanceConfig;
        private AvatarGenerateResult.PersonalityConfig personality;
        private List<String> tags;
        private ParseSource parseSource;
    }

    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final AvatarAppearanceNormalizer normalizer;

    public ParseResult parse(String description) {
        try {
            String response = llmClient.chat(
                    promptBuilder.buildAvatarGenerateSystemPrompt(),
                    promptBuilder.buildAvatarGenerateUserPrompt(description)
            );
            AvatarGenerateResult raw = objectMapper.readValue(extractJson(response), AvatarGenerateResult.class);
            ParseResult result = normalize(raw, ParseSource.LLM);
            log.info("Avatar description parsed: source={}, descriptionLength={}",
                    result.getParseSource(), length(description));
            return result;
        } catch (Exception exception) {
            log.warn("Avatar LLM parsing failed; using rule fallback: descriptionLength={}, errorType={}",
                    length(description), exception.getClass().getSimpleName());
            return normalize(buildFallback(description), ParseSource.RULE_FALLBACK);
        }
    }

    private ParseResult normalize(AvatarGenerateResult raw, ParseSource source) {
        AvatarGenerateResult safe = raw == null ? new AvatarGenerateResult() : raw;
        AvatarAppearanceConfig appearance = normalizer.normalize(safe.getAppearanceConfig());
        AvatarGenerateResult.PersonalityConfig personality = normalizePersonality(safe.getPersonality());
        String name = safe.getName() == null || safe.getName().isBlank()
                ? generateName(appearance)
                : safe.getName().trim();
        if (name.length() > 50) {
            name = name.substring(0, 50);
        }
        List<String> tags = safe.getTags() == null
                ? List.of()
                : safe.getTags().stream().filter(value -> value != null && !value.isBlank()).map(String::trim).limit(10).toList();
        return new ParseResult(name, appearance, personality, tags, source);
    }

    private AvatarGenerateResult.PersonalityConfig normalizePersonality(AvatarGenerateResult.PersonalityConfig source) {
        AvatarGenerateResult.PersonalityConfig result = source == null
                ? new AvatarGenerateResult.PersonalityConfig()
                : source;
        if (result.getType() == null || result.getType().isBlank()) {
            result.setType("gentle");
        }
        if (result.getTraits() == null || result.getTraits().isEmpty()) {
            result.setTraits(List.of("温柔", "体贴"));
        } else {
            result.setTraits(result.getTraits().stream().filter(value -> value != null && !value.isBlank()).map(String::trim).limit(5).toList());
        }
        if (result.getSpeakingStyle() == null || result.getSpeakingStyle().isBlank()) {
            result.setSpeakingStyle("温柔细腻");
        }
        if (result.getSlogan() == null || result.getSlogan().isBlank()) {
            result.setSlogan("你好呀，很高兴认识你");
        }
        return result;
    }

    private AvatarGenerateResult buildFallback(String description) {
        String text = description == null ? "" : description.toLowerCase(Locale.ROOT);
        AvatarAppearanceConfig appearance = new AvatarAppearanceConfig();
        appearance.setGender(containsAny(text, "男", "male") ? "male" : "female");
        appearance.setHairColor(firstMatch(text,
                new String[][]{{"银发", "silver"}, {"金发", "blonde"}, {"紫发", "purple"}, {"蓝发", "blue"}, {"粉发", "pink"}, {"红发", "red"}}, "black"));
        appearance.setHairStyle(firstMatch(text,
                new String[][]{{"短发", "short"}, {"双马尾", "twin_tail"}, {"马尾", "ponytail"}, {"波浪", "wavy"}}, "long"));
        appearance.setEyeColor(firstMatch(text,
                new String[][]{{"红眼", "red"}, {"紫眼", "purple"}, {"绿眼", "green"}, {"异瞳", "heterochromia"}}, "blue"));
        appearance.setBodyType("slim");
        appearance.setEarType(firstMatch(text, new String[][]{{"猫耳", "cat"}, {"精灵耳", "elf"}, {"恶魔耳", "demon"}}, "human"));
        appearance.setWingType(firstMatch(text, new String[][]{{"机械翅膀", "mechanical"}, {"天使翅膀", "angel"}, {"恶魔翅膀", "demon"}, {"翅膀", "energy"}}, "none"));
        appearance.setOutfitStyle(firstMatch(text, new String[][]{{"机甲", "armor"}, {"哥特", "gothic"}, {"未来", "tech_future"}, {"和服", "kimono"}}, "casual"));
        appearance.setOutfitColor(firstMatch(text, new String[][]{{"黑色", "black"}, {"红色", "red"}, {"蓝色", "blue"}, {"紫色", "purple"}}, "white"));
        appearance.setAccessories(extractAccessories(text));

        AvatarGenerateResult.PersonalityConfig personality = new AvatarGenerateResult.PersonalityConfig();
        if (containsAny(text, "高冷", "cool")) {
            personality.setType("cool");
            personality.setTraits(List.of("高冷", "沉稳"));
        } else if (containsAny(text, "幽默", "humorous")) {
            personality.setType("humorous");
            personality.setTraits(List.of("幽默", "风趣"));
        } else {
            personality.setType("gentle");
            personality.setTraits(List.of("温柔", "体贴"));
        }
        personality.setSpeakingStyle("温柔细腻");
        personality.setSlogan("你好呀，很高兴认识你");

        AvatarGenerateResult result = new AvatarGenerateResult();
        result.setAppearanceConfig(appearance);
        result.setPersonality(personality);
        result.setName(generateName(normalizer.normalize(appearance)));
        result.setTags(List.of(appearance.getHairColor(), appearance.getEyeColor(), personality.getType()));
        return result;
    }

    private List<String> extractAccessories(String text) {
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        if (containsAny(text, "眼镜", "glasses")) result.add("glasses");
        if (containsAny(text, "耳机", "headset")) result.add("headset");
        if (containsAny(text, "耳环", "earrings")) result.add("earrings");
        if (containsAny(text, "项链", "necklace")) result.add("necklace");
        if (containsAny(text, "发夹", "hairpin")) result.add("hairpin");
        return result;
    }

    private String firstMatch(String text, String[][] mappings, String defaultValue) {
        for (String[] mapping : mappings) {
            if (text.contains(mapping[0]) || text.contains(mapping[1])) {
                return mapping[1];
            }
        }
        return defaultValue;
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) return true;
        }
        return false;
    }

    private String generateName(AvatarAppearanceConfig appearance) {
        return switch (appearance.getHairColor()) {
            case "silver" -> "银月";
            case "purple" -> "紫霞";
            case "blue" -> "蓝霜";
            case "blonde" -> "金璃";
            case "pink" -> "樱雪";
            case "red" -> "赤绫";
            case "white" -> "霜雪";
            default -> "星瑶";
        };
    }

    private String extractJson(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Empty LLM response");
        }

        Matcher codeBlock = JSON_CODE_BLOCK.matcher(text);
        while (codeBlock.find()) {
            String json = extractFirstValidObject(codeBlock.group(1));
            if (json != null) {
                return json;
            }
        }

        String json = extractFirstValidObject(text);
        if (json != null) {
            return json;
        }
        throw new IllegalArgumentException("LLM response does not contain a complete JSON object");
    }

    private String extractFirstValidObject(String text) {
        for (int start = 0; start < text.length(); start++) {
            if (text.charAt(start) != '{') {
                continue;
            }
            int depth = 0;
            boolean inString = false;
            boolean escaped = false;
            for (int index = start; index < text.length(); index++) {
                char current = text.charAt(index);
                if (inString) {
                    if (escaped) {
                        escaped = false;
                    } else if (current == '\\') {
                        escaped = true;
                    } else if (current == '"') {
                        inString = false;
                    }
                    continue;
                }
                if (current == '"') {
                    inString = true;
                } else if (current == '{') {
                    depth++;
                } else if (current == '}') {
                    depth--;
                    if (depth == 0) {
                        String candidate = text.substring(start, index + 1);
                        if (isJsonObject(candidate)) {
                            return candidate;
                        }
                        break;
                    }
                }
            }
        }
        return null;
    }

    private boolean isJsonObject(String candidate) {
        try {
            JsonNode node = objectMapper.readTree(candidate);
            return node != null && node.isObject()
                    && (node.has("appearanceConfig") || node.has("personality"));
        } catch (Exception ignored) {
            return false;
        }
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }
}
