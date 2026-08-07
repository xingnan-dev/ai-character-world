package com.companion.persistence;

import com.companion.entity.ChatMessage;
import com.companion.entity.enums.ChatMessageStatus;
import com.companion.mapper.ChatMessageMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("soft-delete-test")
@Transactional
class ChatMessageLifecycleIntegrationTest {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Test
    void mapsLifecycleFieldsToDatabaseColumns() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        ChatMessage message = new ChatMessage();
        message.setSessionId(3001L);
        message.setRole(2);
        message.setContent("partial response");
        message.setStatus(ChatMessageStatus.FAILED.getCode());
        message.setErrorCode("TIMEOUT");
        message.setErrorMessage("Provider timed out");
        message.setCreateTime(now.minusSeconds(2));
        message.setUpdateTime(now.minusSeconds(1));
        message.setCompletionTime(now);

        chatMessageMapper.insert(message);
        ChatMessage stored = chatMessageMapper.selectById(message.getId());

        assertThat(stored).isNotNull();
        assertThat(stored.getStatus()).isEqualTo(ChatMessageStatus.FAILED.getCode());
        assertThat(stored.getErrorCode()).isEqualTo("TIMEOUT");
        assertThat(stored.getErrorMessage()).isEqualTo("Provider timed out");
        assertThat(stored.getUpdateTime()).isEqualTo(now.minusSeconds(1));
        assertThat(stored.getCompletionTime()).isEqualTo(now);
    }
}
