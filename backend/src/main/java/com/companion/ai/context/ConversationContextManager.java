package com.companion.ai.context;

import com.companion.ai.config.LlmProperties;
import com.companion.ai.model.LlmMessage;
import com.companion.entity.ChatMessage;
import com.companion.entity.enums.ChatMessageStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class ConversationContextManager {

    private static final int MEMORY_BUDGET_DIVISOR = 4;

    private final TokenEstimator tokenEstimator;
    private final LlmProperties properties;

    public ConversationContextManager(TokenEstimator tokenEstimator, LlmProperties properties) {
        this.tokenEstimator = tokenEstimator;
        this.properties = properties;
    }

    /** Limits optional memory to at most one quarter of the available input budget. */
    public String limitMemory(String memoryContext) {
        if (memoryContext == null || memoryContext.isBlank()) {
            return "";
        }
        int memoryBudget = Math.max(0, availableInputTokens() / MEMORY_BUDGET_DIVISOR);
        return tokenEstimator.truncate(memoryContext.trim(), memoryBudget);
    }

    /**
     * Selects the newest completed history that fits beside the required prompt messages.
     * Required messages include system/personality/current-user messages and are never removed.
     */
    public List<ChatMessage> selectHistory(List<ChatMessage> candidates,
                                           List<LlmMessage> requiredMessages) {
        int availableInputTokens = availableInputTokens();
        int requiredTokens = tokenEstimator.estimateMessages(requiredMessages);
        if (requiredTokens > availableInputTokens) {
            throw new ContextWindowExceededException(
                    "Current input and required prompts exceed the configured context window"
            );
        }
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        int remainingTokens = availableInputTokens - requiredTokens;
        List<ChatMessage> newestFirst = candidates.stream()
                .filter(this::isCompletedContent)
                .sorted(Comparator.comparing(
                        ChatMessage::getId,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();

        List<ChatMessage> selected = new ArrayList<>();
        for (ChatMessage message : newestFirst) {
            int messageTokens = tokenEstimator.estimateMessage(message.getContent());
            if (messageTokens > remainingTokens) {
                break;
            }
            selected.add(message);
            remainingTokens -= messageTokens;
        }

        selected.sort(Comparator.comparing(
                ChatMessage::getId,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));
        return List.copyOf(selected);
    }

    private boolean isCompletedContent(ChatMessage message) {
        return message != null
                && Integer.valueOf(ChatMessageStatus.COMPLETED.getCode()).equals(message.getStatus())
                && message.getContent() != null
                && !message.getContent().isBlank();
    }

    private int availableInputTokens() {
        LlmProperties.Context context = properties.getContext();
        int windowTokens = context.getWindowTokens();
        int safetyMarginTokens = context.getSafetyMarginTokens();
        int outputTokens = properties.getMaxTokens() == null ? 0 : properties.getMaxTokens();
        int available = windowTokens - safetyMarginTokens - outputTokens;
        if (available <= 0) {
            throw new IllegalStateException("AI context token configuration leaves no input budget");
        }
        return available;
    }

    public static class ContextWindowExceededException extends IllegalArgumentException {
        public ContextWindowExceededException(String message) {
            super(message);
        }
    }
}
