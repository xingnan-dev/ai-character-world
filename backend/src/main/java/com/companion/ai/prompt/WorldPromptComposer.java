package com.companion.ai.prompt;

import com.companion.ai.model.LlmMessage;
import com.companion.ai.model.LlmRole;
import com.companion.character.snapshot.CharacterSnapshot;
import com.companion.entity.CharacterWorld;
import com.companion.world.WorldActorContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class WorldPromptComposer {

    private final PromptTemplateLoader templateLoader;
    private final PromptTemplateRenderer templateRenderer;

    public WorldPromptComposer(PromptTemplateLoader templateLoader, PromptTemplateRenderer templateRenderer) {
        this.templateLoader = templateLoader;
        this.templateRenderer = templateRenderer;
    }

    public ComposedChatPrompt compose(Long userId, CharacterWorld world, Long roundId,
                                      WorldActorContext actor, List<WorldActorContext> roster,
                                      String userInput, List<WorldSpeech> previousSpeeches) {
        if (world == null || actor == null || !actor.isValid()) {
            throw new PromptTemplateException("A valid world actor is required");
        }
        Map<String, Object> systemValues = new LinkedHashMap<>();
        systemValues.put("worldName", value(world.getName(), "未命名世界"));
        systemValues.put("worldBackground", value(world.getBackground(), "未设置"));
        systemValues.put("worldRules", value(world.getRules(), "遵守角色设定并自然回应"));
        systemValues.put("participantRoster", roster.stream()
                .map(item -> "- " + item.displayName())
                .collect(Collectors.joining("\n")));
        systemValues.put("actorName", actor.displayName());
        systemValues.put("actorSnapshot", describe(actor.snapshot()));

        String transcript = "用户：" + value(userInput, "");
        if (previousSpeeches != null && !previousSpeeches.isEmpty()) {
            transcript += "\n" + previousSpeeches.stream()
                    .map(speech -> speech.speakerName() + "：" + speech.content())
                    .collect(Collectors.joining("\n"));
        }
        Map<String, Object> turnValues = Map.of(
                "transcript", transcript,
                "actorName", actor.displayName()
        );

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("userId", userId);
        metadata.put("worldId", world.getId());
        metadata.put("roundId", roundId);
        metadata.put("participantId", actor.participantId());
        metadata.put("worldSystemPromptVersion", PromptTemplateKey.WORLD_SYSTEM.version());
        metadata.put("worldTurnPromptVersion", PromptTemplateKey.WORLD_TURN.version());
        return new ComposedChatPrompt(List.of(
                message(LlmRole.SYSTEM, PromptTemplateKey.WORLD_SYSTEM, systemValues),
                message(LlmRole.USER, PromptTemplateKey.WORLD_TURN, turnValues)
        ), metadata);
    }

    private LlmMessage message(LlmRole role, PromptTemplateKey key, Map<String, ?> values) {
        return new LlmMessage(role, templateRenderer.render(templateLoader.load(key), values));
    }

    private String describe(CharacterSnapshot snapshot) {
        return "姓名=" + snapshot.name()
                + "\n身份=" + value(snapshot.identity(), "未设置")
                + "\n核心性格=" + value(snapshot.corePersonality(), "未设置")
                + "\n当前目标=" + value(snapshot.currentGoal(), "未设置")
                + "\n经历=" + value(snapshot.biography(), "未设置")
                + "\n与用户关系=" + value(snapshot.relationshipToUser(), "未设置")
                + "\n说话风格=" + value(snapshot.speakingStyle(), "自然")
                + "\n价值观=" + snapshot.profile().values()
                + "\n喜好=" + snapshot.profile().likes()
                + "\n厌恶=" + snapshot.profile().dislikes()
                + "\n兴趣=" + snapshot.profile().interests()
                + "\n恐惧=" + snapshot.profile().fears()
                + "\n秘密=" + snapshot.profile().secrets()
                + "\n行为倾向=" + snapshot.profile().behaviorTendencies();
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public record WorldSpeech(String speakerName, String content) {}
}
