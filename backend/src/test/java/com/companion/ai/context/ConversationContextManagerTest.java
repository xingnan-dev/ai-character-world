package com.companion.ai.context;

import com.companion.ai.config.LlmProperties;
import com.companion.ai.model.LlmMessage;
import com.companion.ai.model.LlmRole;
import com.companion.entity.ChatMessage;
import com.companion.entity.enums.ChatMessageStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversationContextManagerTest {

    private LlmProperties properties;
    private TokenEstimator estimator;
    private ConversationContextManager manager;

    @BeforeEach
    void setUp() {
        properties = new LlmProperties();
        properties.setMaxTokens(10);
        properties.getContext().setWindowTokens(50);
        properties.getContext().setSafetyMarginTokens(5);
        estimator = new TokenEstimator();
        manager = new ConversationContextManager(estimator, properties);
    }

    @Test
    void keepsAllShortCompletedHistoryInChronologicalOrder() {
        ChatMessage older = message(1L, ChatMessageStatus.COMPLETED, "hello");
        ChatMessage newer = message(2L, ChatMessageStatus.COMPLETED, "world");

        List<ChatMessage> selected = manager.selectHistory(
                List.of(newer, older),
                requiredMessages("personality", "hi")
        );

        assertThat(selected).extracting(ChatMessage::getId).containsExactly(1L, 2L);
    }

    @Test
    void removesOldMessagesWhenHistoryExceedsBudget() {
        ChatMessage oldest = message(1L, ChatMessageStatus.COMPLETED, "1234567890123456");
        ChatMessage middle = message(2L, ChatMessageStatus.COMPLETED, "1234567890123456");
        ChatMessage newest = message(3L, ChatMessageStatus.COMPLETED, "1234567890123456");

        List<ChatMessage> selected = manager.selectHistory(
                List.of(oldest, middle, newest),
                requiredMessages("personality", "current")
        );

        assertThat(selected).extracting(ChatMessage::getId)
                .contains(3L)
                .doesNotContain(1L);
    }

    @Test
    void filtersFailedAndCancelledMessagesDefensively() {
        ChatMessage completed = message(1L, ChatMessageStatus.COMPLETED, "valid");
        ChatMessage failed = message(2L, ChatMessageStatus.FAILED, "partial failure");
        ChatMessage cancelled = message(3L, ChatMessageStatus.CANCELLED, "partial cancellation");

        List<ChatMessage> selected = manager.selectHistory(
                List.of(completed, failed, cancelled),
                requiredMessages("personality", "current")
        );

        assertThat(selected).extracting(ChatMessage::getId).containsExactly(1L);
    }

    @Test
    void requiredPersonalityAndCurrentUserMessagesAreNeverRemoved() {
        List<LlmMessage> required = requiredMessages("stable personality prompt", "current user message");

        manager.selectHistory(
                List.of(message(1L, ChatMessageStatus.COMPLETED, "very old history")),
                required
        );

        assertThat(required).extracting(LlmMessage::content)
                .containsExactly("stable personality prompt", "current user message");
    }

    @Test
    void rejectsCurrentInputWhenRequiredPromptAloneExceedsWindow() {
        String oversizedInput = "中".repeat(40);

        assertThatThrownBy(() -> manager.selectHistory(
                List.of(), requiredMessages("personality", oversizedInput)
        )).isInstanceOf(ConversationContextManager.ContextWindowExceededException.class)
                .hasMessageContaining("exceed the configured context window");
    }

    @Test
    void limitsMemoryLengthToItsBudget() {
        String limited = manager.limitMemory("记".repeat(100));

        int availableInput = 50 - 10 - 5;
        assertThat(estimator.estimate(limited)).isLessThanOrEqualTo(availableInput / 4);
    }

    private List<LlmMessage> requiredMessages(String personalityPrompt, String currentMessage) {
        return List.of(
                new LlmMessage(LlmRole.SYSTEM, personalityPrompt),
                new LlmMessage(LlmRole.USER, currentMessage)
        );
    }

    private ChatMessage message(Long id, ChatMessageStatus status, String content) {
        ChatMessage message = new ChatMessage();
        message.setId(id);
        message.setRole(1);
        message.setStatus(status.getCode());
        message.setContent(content);
        return message;
    }
}
