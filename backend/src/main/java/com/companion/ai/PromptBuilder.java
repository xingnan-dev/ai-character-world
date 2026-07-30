package com.companion.ai;

import com.companion.entity.ChatMessage;
import com.companion.entity.Personality;
import com.companion.entity.UserMemory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PromptBuilder {

    public String buildSystemPrompt(Personality personality) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个虚拟伴侣。");

        if (personality.getName() != null && !personality.getName().isEmpty()) {
            sb.append("你的名字叫").append(personality.getName()).append("。");
        }

        if (personality.getCorePersonality() != null && !personality.getCorePersonality().isEmpty()) {
            sb.append("你的核心性格：").append(personality.getCorePersonality()).append("。");
        }

        if (personality.getIdentity() != null && !personality.getIdentity().isEmpty()) {
            sb.append("你的身份：").append(personality.getIdentity()).append("。");
        }

        if (personality.getLanguageStyle() != null && !personality.getLanguageStyle().isEmpty()) {
            sb.append("你的语言风格：").append(personality.getLanguageStyle()).append("。");
        }

        if (personality.getHobbies() != null && !personality.getHobbies().isEmpty()) {
            sb.append("你的兴趣爱好：").append(personality.getHobbies()).append("。");
        }

        if (personality.getRelationship() != null && !personality.getRelationship().isEmpty()) {
            sb.append("你和用户的关系：").append(personality.getRelationship()).append("。");
        }

        return sb.toString();
    }

    public String buildUserPrompt(String userMessage, List<ChatMessage> history, UserMemory memory) {
        StringBuilder sb = new StringBuilder();

        String memoryContext = buildMemoryContext(memory);
        if (!memoryContext.isEmpty()) {
            sb.append("【关于用户的记忆】").append(memoryContext).append("\n\n");
        }

        String historyText = buildHistoryText(history);
        if (!historyText.isEmpty()) {
            sb.append("【历史对话】\n").append(historyText).append("\n\n");
        }

        sb.append("用户现在对你说：").append(userMessage);

        return sb.toString();
    }

    public String buildFullPrompt(Personality personality, String userMessage,
                                  List<ChatMessage> history, UserMemory memory) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildSystemPrompt(personality));
        sb.append("\n\n");
        sb.append(buildUserPrompt(userMessage, history, memory));
        return sb.toString();
    }

    private String buildMemoryContext(UserMemory memory) {
        if (memory == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (memory.getMemoryKey() != null && !memory.getMemoryKey().isEmpty()) {
            sb.append(memory.getMemoryKey()).append("：").append(memory.getValue() == null ? "" : memory.getValue());
        }
        return sb.toString();
    }

    private String buildHistoryText(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }
        List<ChatMessage> sorted = history.stream()
                .sorted((a, b) -> {
                    if (a.getId() == null || b.getId() == null) return 0;
                    return a.getId().compareTo(b.getId());
                })
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        int limit = Math.min(sorted.size(), 10);
        List<ChatMessage> recent = sorted.subList(sorted.size() - limit, sorted.size());
        Collections.reverse(recent);

        for (ChatMessage msg : recent) {
            String roleText = msg.getRole() != null && msg.getRole() == 2 ? "你" : "用户";
            sb.append(roleText).append("：").append(msg.getContent() == null ? "" : msg.getContent()).append("\n");
        }
        return sb.toString().trim();
    }
}
