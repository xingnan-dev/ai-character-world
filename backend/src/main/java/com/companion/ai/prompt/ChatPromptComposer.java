package com.companion.ai.prompt;

import com.companion.ai.model.LlmMessage;
import com.companion.ai.model.LlmRole;
import com.companion.entity.ChatMessage;
import com.companion.entity.Personality;
import com.companion.entity.CharacterRelationship;
import com.companion.character.snapshot.CharacterSnapshot;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ChatPromptComposer {

    private final PromptTemplateLoader templateLoader;
    private final PromptTemplateRenderer templateRenderer;

    public ChatPromptComposer(PromptTemplateLoader templateLoader, PromptTemplateRenderer templateRenderer) {
        this.templateLoader = templateLoader;
        this.templateRenderer = templateRenderer;
    }

    public ComposedChatPrompt compose(Long userId,
                                      Long sessionId,
                                      Long avatarId,
                                      Personality personality,
                                      String memoryContext,
                                      List<ChatMessage> history,
                                      String userMessage) {
        if (personality == null) {
            throw new PromptTemplateException("Personality is required to compose a chat prompt");
        }
        if (userMessage == null || userMessage.isBlank()) {
            throw new PromptTemplateException("User message is required to compose a chat prompt");
        }

        List<LlmMessage> messages = new ArrayList<>();
        messages.add(systemMessage(PromptTemplateKey.CHAT_SAFETY, Map.of()));
        messages.add(systemMessage(PromptTemplateKey.CHAT_PERSONALITY, personalityVariables(personality)));

        if (memoryContext != null && !memoryContext.isBlank()) {
            messages.add(systemMessage(
                    PromptTemplateKey.CHAT_MEMORY,
                    Map.of("memoryContext", memoryContext.trim())
            ));
        }

        normalizedHistory(history).stream()
                .map(this::toLlmMessage)
                .filter(message -> message.content() != null && !message.content().isBlank())
                .forEach(messages::add);

        messages.add(new LlmMessage(LlmRole.USER, userMessage));

        Map<String, Object> metadata = new LinkedHashMap<>();
        putIfPresent(metadata, "userId", userId);
        putIfPresent(metadata, "sessionId", sessionId);
        putIfPresent(metadata, "avatarId", avatarId);
        putIfPresent(metadata, "personalityId", personality.getId());
        metadata.put("safetyPromptVersion", PromptTemplateKey.CHAT_SAFETY.version());
        metadata.put("personalityPromptVersion", PromptTemplateKey.CHAT_PERSONALITY.version());
        if (memoryContext != null && !memoryContext.isBlank()) {
            metadata.put("memoryPromptVersion", PromptTemplateKey.CHAT_MEMORY.version());
        }

        return new ComposedChatPrompt(messages, metadata);
    }

    public ComposedChatPrompt composeCharacter(Long userId, Long sessionId,
                                                CharacterSnapshot character,
                                                String memoryContext,
                                                List<ChatMessage> history,
                                                String userMessage) {
        return composeCharacter(userId, sessionId, character, null, memoryContext, history, userMessage);
    }

    public ComposedChatPrompt composeCharacter(Long userId, Long sessionId,
                                                CharacterSnapshot character,
                                                CharacterRelationship relationshipState,
                                                String memoryContext,
                                                List<ChatMessage> history,
                                                String userMessage) {
        if (character == null || !"AI".equals(character.characterType())) {
            throw new PromptTemplateException("AI Character is required to compose a chat prompt");
        }
        if (userMessage == null || userMessage.isBlank()) {
            throw new PromptTemplateException("User message is required to compose a chat prompt");
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("name", character.name());
        variables.put("corePersonality", valueOrDefault(character.corePersonality(), "友善、尊重用户"));
        variables.put("identity", valueOrDefault(character.identity(), "AI虚拟伴侣"));
        variables.put("languageStyle", valueOrDefault(character.speakingStyle(), "自然、清晰"));
        variables.put("hobbies", character.profile().interests().isEmpty()
                ? "与用户交流" : String.join("、", character.profile().interests()));
        variables.put("relationship", valueOrDefault(character.relationshipToUser(), "虚拟伙伴"));

        List<LlmMessage> messages = new ArrayList<>();
        messages.add(systemMessage(PromptTemplateKey.CHAT_SAFETY, Map.of()));
        messages.add(systemMessage(PromptTemplateKey.CHAT_PERSONALITY, variables));
        if (memoryContext != null && !memoryContext.isBlank()) {
            messages.add(systemMessage(PromptTemplateKey.CHAT_MEMORY,
                    Map.of("memoryContext", memoryContext.trim())));
        }
        if (relationshipState != null) {
            messages.add(systemMessage(PromptTemplateKey.CHAT_RELATIONSHIP, Map.of(
                    "stage", valueOrDefault(relationshipState.getStage(), "NEW"),
                    "summary", valueOrDefault(relationshipState.getSummary(), "No relationship summary yet."),
                    "interactionStyle", valueOrDefault(relationshipState.getInteractionStyle(), "Polite and friendly."),
                    "recentChange", valueOrDefault(relationshipState.getRecentChange(), "No meaningful recent change.")
            )));
        }
        normalizedHistory(history).stream().map(this::toLlmMessage)
                .filter(message -> message.content() != null && !message.content().isBlank())
                .forEach(messages::add);
        messages.add(new LlmMessage(LlmRole.USER, userMessage));

        Map<String, Object> metadata = new LinkedHashMap<>();
        putIfPresent(metadata, "userId", userId);
        putIfPresent(metadata, "sessionId", sessionId);
        putIfPresent(metadata, "characterId", character.sourceCharacterId());
        metadata.put("characterSnapshotVersion", character.snapshotVersion());
        metadata.put("safetyPromptVersion", PromptTemplateKey.CHAT_SAFETY.version());
        metadata.put("personalityPromptVersion", PromptTemplateKey.CHAT_PERSONALITY.version());
        if (relationshipState != null) {
            metadata.put("relationshipPromptVersion", PromptTemplateKey.CHAT_RELATIONSHIP.version());
        }
        return new ComposedChatPrompt(messages, metadata);
    }

    private LlmMessage systemMessage(PromptTemplateKey key, Map<String, ?> values) {
        PromptTemplateDefinition template = templateLoader.load(key);
        return new LlmMessage(LlmRole.SYSTEM, templateRenderer.render(template, values));
    }

    private Map<String, Object> personalityVariables(Personality personality) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("name", valueOrDefault(personality.getName(), "未命名角色"));
        variables.put("corePersonality", valueOrDefault(personality.getCorePersonality(), "友善、尊重用户"));
        variables.put("identity", valueOrDefault(personality.getIdentity(), "AI虚拟伴侣"));
        variables.put("languageStyle", valueOrDefault(personality.getLanguageStyle(), "自然、清晰"));
        variables.put("hobbies", valueOrDefault(personality.getHobbies(), "与用户交流"));
        variables.put("relationship", valueOrDefault(personality.getRelationship(), "虚拟伙伴"));
        return variables;
    }

    private List<ChatMessage> normalizedHistory(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        return history.stream()
                .filter(message -> message != null && message.getRole() != null)
                .sorted(Comparator.comparing(
                        ChatMessage::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .toList();
    }

    private LlmMessage toLlmMessage(ChatMessage message) {
        LlmRole role = Integer.valueOf(2).equals(message.getRole()) ? LlmRole.ASSISTANT : LlmRole.USER;
        return new LlmMessage(role, message.getContent());
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private void putIfPresent(Map<String, Object> metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }
}
