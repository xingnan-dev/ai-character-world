package com.companion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.ai.AiService;
import com.companion.common.exception.BusinessException;
import com.companion.dto.request.ChatSendRequest;
import com.companion.entity.Avatar;
import com.companion.entity.ChatSession;
import com.companion.entity.Personality;
import com.companion.mapper.AvatarMapper;
import com.companion.mapper.ChatMessageMapper;
import com.companion.mapper.ChatSessionMapper;
import com.companion.mapper.PersonalityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServicePersonalityIsolationTest {

    private static final long USER_A_ID = 10L;
    private static final long AVATAR_A_ID = 20L;
    private static final long SESSION_A_ID = 30L;

    @Mock private ChatSessionMapper chatSessionMapper;
    @Mock private ChatMessageMapper chatMessageMapper;
    @Mock private AvatarMapper avatarMapper;
    @Mock private PersonalityMapper personalityMapper;
    @Mock private AiService aiService;

    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatServiceImpl(
                chatSessionMapper, chatMessageMapper, avatarMapper, personalityMapper, aiService
        );

        ChatSession session = new ChatSession();
        session.setId(SESSION_A_ID);
        session.setUserId(USER_A_ID);
        session.setAvatarId(AVATAR_A_ID);
        session.setStatus(1);

        Avatar avatar = new Avatar();
        avatar.setId(AVATAR_A_ID);
        avatar.setUserId(USER_A_ID);
        avatar.setStatus(1);

        when(chatSessionMapper.selectOne(any())).thenReturn(session);
        when(avatarMapper.selectOne(any())).thenReturn(avatar);
    }

    @Test
    void doesNotFallBackToAnotherUsersLatestPersonality() {
        when(personalityMapper.selectOne(any())).thenAnswer(invocation -> {
            QueryWrapper<Personality> wrapper = invocation.getArgument(0);
            assertThatQueryContains(wrapper, AVATAR_A_ID);
            return null;
        });

        ChatSendRequest request = new ChatSendRequest();
        request.setSessionId(SESSION_A_ID);
        request.setContent("你好");

        assertThatThrownBy(() -> chatService.sendMessage(USER_A_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("该头像尚未设置人格");

        verify(personalityMapper).selectOne(any());
        verifyNoInteractions(aiService);
    }

    private void assertThatQueryContains(QueryWrapper<?> wrapper, Object expected) {
        wrapper.getSqlSegment();
        Map<String, Object> parameters = wrapper.getParamNameValuePairs();
        org.assertj.core.api.Assertions.assertThat(parameters.values()).contains(expected);
    }
}
