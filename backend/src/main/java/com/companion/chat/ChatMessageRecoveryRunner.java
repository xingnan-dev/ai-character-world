package com.companion.chat;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.companion.entity.ChatMessage;
import com.companion.entity.enums.ChatMessageStatus;
import com.companion.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageRecoveryRunner implements ApplicationRunner {

    static final String INTERRUPTION_ERROR_CODE = "SERVICE_RESTART_INTERRUPTED";
    private static final int ASSISTANT_ROLE = 2;

    private final ChatMessageMapper chatMessageMapper;

    @Value("${chat.message-recovery-timeout:5m}")
    private Duration recoveryTimeout;

    @Override
    public void run(ApplicationArguments args) {
        int recovered = recoverStaleMessages(LocalDateTime.now());
        if (recovered > 0) {
            log.warn("Recovered {} stale chat messages as INTERRUPTED", recovered);
        }
    }

    int recoverStaleMessages(LocalDateTime recoveryTime) {
        LocalDateTime cutoff = recoveryTime.minus(recoveryTimeout);
        LambdaUpdateWrapper<ChatMessage> update = new LambdaUpdateWrapper<ChatMessage>()
                .eq(ChatMessage::getRole, ASSISTANT_ROLE)
                .in(ChatMessage::getStatus, List.of(
                        ChatMessageStatus.PENDING.getCode(),
                        ChatMessageStatus.STREAMING.getCode()
                ))
                .le(ChatMessage::getUpdateTime, cutoff)
                .set(ChatMessage::getStatus, ChatMessageStatus.INTERRUPTED.getCode())
                .set(ChatMessage::getErrorCode, INTERRUPTION_ERROR_CODE)
                .set(ChatMessage::getErrorMessage, null)
                .set(ChatMessage::getUpdateTime, recoveryTime)
                .set(ChatMessage::getCompletionTime, recoveryTime);
        return chatMessageMapper.update(null, update);
    }
}
