package com.companion.ai.provider;

import com.companion.ai.config.LlmProperties;
import com.companion.ai.model.LlmChunk;
import com.companion.ai.model.LlmMessage;
import com.companion.ai.model.LlmRequest;
import com.companion.ai.model.LlmResponse;
import com.companion.ai.model.LlmRole;
import com.companion.ai.model.LlmUsage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.stream.IntStream;

@Component
public class MockLlmProvider implements LlmProvider {

    public static final String NAME = "mock";

    private final LlmProperties properties;

    public MockLlmProvider(LlmProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        return new LlmResponse(
                generateResponse(request), NAME, request.model(), "stop", request.requestId(), LlmUsage.empty()
        );
    }

    @Override
    public Flux<LlmChunk> stream(LlmRequest request) {
        String response = generateResponse(request);
        if (response.isEmpty()) {
            return Flux.just(new LlmChunk("", 0, true, "stop", request.requestId(), LlmUsage.empty()));
        }

        Flux<LlmChunk> chunks = Flux.fromStream(IntStream.range(0, response.length())
                .mapToObj(index -> new LlmChunk(
                        String.valueOf(response.charAt(index)),
                        index,
                        index == response.length() - 1,
                        index == response.length() - 1 ? "stop" : null,
                        request.requestId(),
                        index == response.length() - 1 ? LlmUsage.empty() : null
                )));

        Duration delay = properties.getMock().getDelay();
        return delay == null || delay.isZero() || delay.isNegative() ? chunks : chunks.delayElements(delay);
    }

    private String generateResponse(LlmRequest request) {
        String systemPrompt = message(request, LlmRole.SYSTEM);
        String userPrompt = message(request, LlmRole.USER);
        if (isAvatarGeneration(systemPrompt)) {
            return avatarJson(userPrompt);
        }
        if (isCharacterParsing(systemPrompt)) {
            return characterJson(userPrompt);
        }

        String lower = userPrompt.toLowerCase();
        if (lower.contains("你好") || lower.contains("hello") || lower.contains("hi")) {
            return "你好呀，很高兴见到你。今天想聊些什么？";
        }
        if (lower.contains("难过") || lower.contains("伤心") || lower.contains("压力")) {
            return "我在这里听你说。慢慢来，你不需要独自承担这些感受。";
        }
        if (lower.contains("名字") || lower.contains("介绍")) {
            return "我是你的虚拟伙伴，会尽量按照设定的性格陪你聊天。";
        }
        return "我明白了。关于这件事，你愿意再多告诉我一些吗？";
    }

    private boolean isAvatarGeneration(String systemPrompt) {
        return systemPrompt.contains("appearanceConfig")
                || systemPrompt.contains("虚拟角色创造")
                || systemPrompt.contains("角色属性");
    }

    private boolean isCharacterParsing(String systemPrompt) {
        return systemPrompt.contains("CHARACTER_DRAFT_JSON");
    }

    private String characterJson(String userPrompt) {
        String type = userPrompt.contains("角色类型：USER") ? "USER" : "AI";
        return """
                {"characterType":"%s","name":"林澈","age":28,"identity":"独立研究员","corePersonality":"冷静、好奇且富有同理心","currentGoal":"探索未知并帮助同伴成长","biography":"长期研究人工智能与人类协作。","relationshipToUser":"可信赖的伙伴","speakingStyle":"简洁、自然、理性","profile":{"values":["诚实","成长"],"likes":["阅读"],"dislikes":["欺骗"],"interests":["人工智能","宇宙"],"fears":[],"secrets":[],"behaviorTendencies":["先分析再行动"]}}
                """.formatted(type).trim();
    }

    private String avatarJson(String userPrompt) {
        String lower = userPrompt.toLowerCase();
        String gender = lower.contains("男性") || lower.contains("男生") ? "male" : "female";
        String hairColor = lower.contains("银") ? "silver"
                : lower.contains("紫") ? "purple"
                : lower.contains("蓝") ? "blue" : "black";
        String personality = lower.contains("高冷") ? "cool"
                : lower.contains("幽默") ? "humorous" : "gentle";

        return """
                {"name":"星瑶","appearanceConfig":{"gender":"%s","hairColor":"%s","hairStyle":"long","eyeColor":"blue","bodyType":"slim","earType":"human","hasWing":false,"wingType":"none","outfitStyle":"casual","outfitColor":"black","hasAccessory":false,"accessoryType":[]},"personality":{"type":"%s","traits":["温柔","体贴"],"speakingStyle":"自然、亲切","slogan":"你好呀，很高兴认识你"},"tags":["%s","blue","%s"]}
                """.formatted(gender, hairColor, personality, hairColor, personality).trim();
    }

    private String message(LlmRequest request, LlmRole role) {
        return request.messages().stream()
                .filter(message -> message.role() == role)
                .map(LlmMessage::content)
                .filter(content -> content != null && !content.isBlank())
                .reduce((first, second) -> second)
                .orElse("");
    }
}
