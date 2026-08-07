package com.companion.service.impl;

import com.companion.ai.AiService;
import com.companion.chat.SessionPersonalityResolver;
import com.companion.common.exception.BusinessException;
import com.companion.dto.request.ChatSendRequest;
import com.companion.entity.Avatar;
import com.companion.entity.ChatSession;
import com.companion.entity.Personality;
import com.companion.mapper.AvatarMapper;
import com.companion.mapper.ChatMessageMapper;
import com.companion.mapper.ChatSessionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServicePersonalityIsolationTest {

    private static final long USER_A_ID = 10L;
    private static final long USER_B_ID = 11L;
    private static final long AVATAR_B_ID = 20L;
    private static final long SESSION_B_ID = 30L;

    @Mock private ChatSessionMapper chatSessionMapper;
    @Mock private ChatMessageMapper chatMessageMapper;
    @Mock private AvatarMapper avatarMapper;
    @Mock private SessionPersonalityResolver sessionPersonalityResolver;
    @Mock private AiService aiService;

    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatServiceImpl(
                chatSessionMapper, chatMessageMapper, avatarMapper, sessionPersonalityResolver, aiService
        );
    }

    @Test
    void userCannotReadAnotherUsersSessionPersonalitySnapshot() {
        when(chatSessionMapper.selectOne(any())).thenReturn(null);

        ChatSendRequest request = new ChatSendRequest(SESSION_B_ID, "你好");

        assertThatThrownBy(() -> chatService.sendMessage(USER_A_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("资源不存在");

        verifyNoInteractions(avatarMapper, sessionPersonalityResolver, aiService);
    }

    @Test
    void sendMessageUsesOnlyTheBoundSessionSnapshot() {
        ChatSession session = new ChatSession();
        session.setId(SESSION_B_ID);
        session.setUserId(USER_B_ID);
        session.setAvatarId(AVATAR_B_ID);
        session.setStatus(1);

        Avatar avatar = new Avatar();
        avatar.setId(AVATAR_B_ID);
        avatar.setUserId(USER_B_ID);
        avatar.setStatus(1);

        Personality boundPersonality = new Personality();
        boundPersonality.setId(40L);
        boundPersonality.setAvatarId(AVATAR_B_ID);
        boundPersonality.setName("创建会话时的人格");

        when(chatSessionMapper.selectOne(any())).thenReturn(session);
        when(avatarMapper.selectOne(any())).thenReturn(avatar);
        when(sessionPersonalityResolver.resolveFromSession(session)).thenReturn(boundPersonality);
        when(aiService.chatStream(USER_B_ID, SESSION_B_ID, "你好", boundPersonality))
                .thenReturn(Flux.just("你好"));

        chatService.sendMessage(USER_B_ID, new ChatSendRequest(SESSION_B_ID, "你好"));

        verify(sessionPersonalityResolver).resolveFromSession(session);
        verify(aiService).chatStream(USER_B_ID, SESSION_B_ID, "你好", boundPersonality);
    }
}
