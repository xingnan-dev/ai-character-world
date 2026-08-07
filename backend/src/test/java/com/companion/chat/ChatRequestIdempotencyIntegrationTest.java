package com.companion.chat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.companion.ai.AiService;
import com.companion.chat.model.ChatMessageExchange;
import com.companion.dto.request.ChatSendRequest;
import com.companion.entity.Avatar;
import com.companion.entity.ChatMessage;
import com.companion.entity.ChatSession;
import com.companion.entity.Personality;
import com.companion.mapper.AvatarMapper;
import com.companion.mapper.ChatMessageMapper;
import com.companion.mapper.ChatSessionMapper;
import com.companion.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("soft-delete-test")
@Transactional
class ChatRequestIdempotencyIntegrationTest {

    private static final long USER_ID = 1001L;
    private static final long SESSION_ID = 2001L;
    private static final long AVATAR_ID = 3001L;

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @MockBean
    private ChatSessionMapper chatSessionMapper;

    @MockBean
    private AvatarMapper avatarMapper;

    @MockBean
    private SessionPersonalityResolver sessionPersonalityResolver;

    @MockBean
    private AiService aiService;

    private Personality personality;

    @BeforeEach
    void setUp() {
        ChatSession session = new ChatSession();
        session.setId(SESSION_ID);
        session.setUserId(USER_ID);
        session.setAvatarId(AVATAR_ID);
        session.setStatus(1);

        Avatar avatar = new Avatar();
        avatar.setId(AVATAR_ID);
        avatar.setUserId(USER_ID);
        avatar.setStatus(1);

        personality = new Personality();
        personality.setId(4001L);
        personality.setAvatarId(AVATAR_ID);

        when(chatSessionMapper.selectOne(any())).thenReturn(session);
        when(avatarMapper.selectOne(any())).thenReturn(avatar);
        when(sessionPersonalityResolver.resolveFromSession(session)).thenReturn(personality);
        when(aiService.chatStream(
                eq(USER_ID), eq(SESSION_ID), eq("hello"), eq(personality), any(ChatMessageExchange.class)
        )).thenReturn(Flux.never());
    }

    @Test
    void sameRequestIdCreatesOneExchangeAndInvokesAiOnce() {
        ChatSendRequest request = new ChatSendRequest(SESSION_ID, "hello", "request-1");

        chatService.sendMessage(USER_ID, request);
        chatService.sendMessage(USER_ID, request);

        assertThat(messageCount()).isEqualTo(2L);
        verify(aiService, times(1)).chatStream(
                eq(USER_ID), eq(SESSION_ID), eq("hello"), eq(personality), any(ChatMessageExchange.class)
        );
    }

    @Test
    void differentRequestIdsCreateIndependentExchanges() {
        chatService.sendMessage(USER_ID, new ChatSendRequest(SESSION_ID, "hello", "request-1"));
        chatService.sendMessage(USER_ID, new ChatSendRequest(SESSION_ID, "hello", "request-2"));

        assertThat(messageCount()).isEqualTo(4L);
        verify(aiService, times(2)).chatStream(
                eq(USER_ID), eq(SESSION_ID), eq("hello"), eq(personality), any(ChatMessageExchange.class)
        );
    }

    private Long messageCount() {
        return chatMessageMapper.selectCount(
                new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getSessionId, SESSION_ID)
        );
    }
}
