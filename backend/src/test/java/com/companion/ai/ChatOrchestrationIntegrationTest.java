package com.companion.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.companion.chat.ChatMessageLifecycleService;
import com.companion.chat.model.ChatMessageExchange;
import com.companion.entity.AiUsageRecord;
import com.companion.entity.ChatMessage;
import com.companion.entity.Personality;
import com.companion.entity.enums.ChatMessageStatus;
import com.companion.mapper.AiUsageRecordMapper;
import com.companion.mapper.ChatMessageMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "ai.mock.enabled=true",
        "ai.mock.delay=0s"
})
@ActiveProfiles("soft-delete-test")
class ChatOrchestrationIntegrationTest {

    private static final long USER_ID = 61001L;
    private static final long SESSION_ID = 62001L;

    @Autowired
    private ChatMessageLifecycleService lifecycleService;

    @Autowired
    private AiService aiService;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private AiUsageRecordMapper aiUsageRecordMapper;

    @Test
    void completesOrchestrationAndLinksUsageToAssistantMessage() {
        ChatMessageExchange exchange = lifecycleService.createExchange(
                SESSION_ID, "hello", "phase-6-orchestration"
        );

        String response = aiService.chatStream(
                USER_ID, SESSION_ID, "hello", personality(), exchange
        ).collectList().map(parts -> String.join("", parts)).block();

        ChatMessage userMessage = chatMessageMapper.selectById(exchange.userMessageId());
        ChatMessage assistantMessage = chatMessageMapper.selectById(exchange.assistantMessageId());
        AiUsageRecord usage = aiUsageRecordMapper.selectOne(
                new LambdaQueryWrapper<AiUsageRecord>()
                        .eq(AiUsageRecord::getChatMessageId, exchange.assistantMessageId())
        );

        assertThat(response).isNotBlank();
        assertThat(userMessage.getStatus()).isEqualTo(ChatMessageStatus.COMPLETED.getCode());
        assertThat(assistantMessage.getStatus()).isEqualTo(ChatMessageStatus.COMPLETED.getCode());
        assertThat(assistantMessage.getContent()).isEqualTo(response);
        assertThat(usage).isNotNull();
        assertThat(usage.getUserId()).isEqualTo(USER_ID);
        assertThat(usage.getSessionId()).isEqualTo(SESSION_ID);
        assertThat(usage.getChatMessageId()).isEqualTo(exchange.assistantMessageId());
        assertThat(usage.getSuccess()).isEqualTo(1);
    }

    private Personality personality() {
        Personality personality = new Personality();
        personality.setId(63001L);
        personality.setAvatarId(64001L);
        personality.setName("Phase 6 assistant");
        personality.setCorePersonality("helpful");
        personality.setIdentity("virtual companion");
        personality.setLanguageStyle("clear");
        personality.setHobbies("conversation");
        personality.setRelationship("companion");
        return personality;
    }
}
