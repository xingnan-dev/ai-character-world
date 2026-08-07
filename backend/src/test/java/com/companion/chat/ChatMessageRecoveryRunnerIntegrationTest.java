package com.companion.chat;

import com.companion.entity.ChatMessage;
import com.companion.entity.enums.ChatMessageStatus;
import com.companion.mapper.ChatMessageMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@ActiveProfiles("soft-delete-test")
@Transactional
class ChatMessageRecoveryRunnerIntegrationTest {

    private static final long SESSION_ID = 91001L;

    @Autowired
    private ChatMessageRecoveryRunner recoveryRunner;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Test
    void stalePendingAndStreamingMessagesBecomeInterrupted() {
        LocalDateTime recoveryTime = LocalDateTime.now();
        ChatMessage pending = insertMessage(ChatMessageStatus.PENDING, recoveryTime.minusMinutes(10));
        ChatMessage streaming = insertMessage(ChatMessageStatus.STREAMING, recoveryTime.minusMinutes(10));
        ReflectionTestUtils.setField(recoveryRunner, "recoveryTimeout", Duration.ofMinutes(5));

        int recovered = recoveryRunner.recoverStaleMessages(recoveryTime);

        assertThat(recovered).isEqualTo(2);
        assertInterrupted(chatMessageMapper.selectById(pending.getId()), recoveryTime);
        assertInterrupted(chatMessageMapper.selectById(streaming.getId()), recoveryTime);
    }

    @Test
    void recentAndTerminalMessagesRemainUnchanged() {
        LocalDateTime recoveryTime = LocalDateTime.now();
        ChatMessage recent = insertMessage(ChatMessageStatus.STREAMING, recoveryTime.minusMinutes(1));
        ChatMessage completed = insertMessage(ChatMessageStatus.COMPLETED, recoveryTime.minusMinutes(10));
        ChatMessage failed = insertMessage(ChatMessageStatus.FAILED, recoveryTime.minusMinutes(10));
        ReflectionTestUtils.setField(recoveryRunner, "recoveryTimeout", Duration.ofMinutes(5));

        int recovered = recoveryRunner.recoverStaleMessages(recoveryTime);

        assertThat(recovered).isZero();
        assertThat(chatMessageMapper.selectById(recent.getId()).getStatus())
                .isEqualTo(ChatMessageStatus.STREAMING.getCode());
        assertThat(chatMessageMapper.selectById(completed.getId()).getStatus())
                .isEqualTo(ChatMessageStatus.COMPLETED.getCode());
        assertThat(chatMessageMapper.selectById(failed.getId()).getStatus())
                .isEqualTo(ChatMessageStatus.FAILED.getCode());
    }

    private ChatMessage insertMessage(ChatMessageStatus status, LocalDateTime updateTime) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(SESSION_ID);
        message.setRequestId(java.util.UUID.randomUUID().toString());
        message.setRole(2);
        message.setContent("");
        message.setStatus(status.getCode());
        message.setCreateTime(updateTime);
        message.setUpdateTime(updateTime);
        if (status == ChatMessageStatus.COMPLETED || status == ChatMessageStatus.FAILED) {
            message.setCompletionTime(updateTime);
        }
        chatMessageMapper.insert(message);
        return message;
    }

    private void assertInterrupted(ChatMessage message, LocalDateTime recoveryTime) {
        assertThat(message.getStatus()).isEqualTo(ChatMessageStatus.INTERRUPTED.getCode());
        assertThat(message.getErrorCode()).isEqualTo(ChatMessageRecoveryRunner.INTERRUPTION_ERROR_CODE);
        assertThat(message.getErrorMessage()).isNull();
        assertThat(message.getCompletionTime()).isCloseTo(recoveryTime, within(1, ChronoUnit.MICROS));
        assertThat(message.getUpdateTime()).isCloseTo(recoveryTime, within(1, ChronoUnit.MICROS));
    }
}
