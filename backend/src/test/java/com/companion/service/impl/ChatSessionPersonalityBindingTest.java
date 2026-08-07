package com.companion.service.impl;

import com.companion.ai.AiService;
import com.companion.chat.SessionPersonalityResolver;
import com.companion.chat.model.PersonalitySnapshot;
import com.companion.dto.request.ChatSessionCreateRequest;
import com.companion.entity.Avatar;
import com.companion.entity.ChatSession;
import com.companion.mapper.AvatarMapper;
import com.companion.mapper.ChatMessageMapper;
import com.companion.mapper.ChatSessionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatSessionPersonalityBindingTest {

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
    void createSessionPersistsPersonalityIdAndSnapshot() {
        Avatar avatar = new Avatar();
        avatar.setId(20L);
        avatar.setUserId(10L);
        avatar.setName("星瑶");
        avatar.setStatus(1);

        PersonalitySnapshot snapshot = new PersonalitySnapshot(
                30L, 20L, "温柔人格", "温柔", "虚拟伴侣", "自然", "音乐", "朋友", 1
        );
        String snapshotJson = "{\"personalityId\":30}";

        when(avatarMapper.selectOne(any())).thenReturn(avatar);
        when(sessionPersonalityResolver.resolveForNewSession(10L, avatar)).thenReturn(snapshot);
        when(sessionPersonalityResolver.encode(snapshot)).thenReturn(snapshotJson);
        when(chatSessionMapper.insert(any())).thenAnswer(invocation -> {
            ChatSession session = invocation.getArgument(0);
            session.setId(40L);
            return 1;
        });

        chatService.createSession(10L, new ChatSessionCreateRequest(20L, null));

        ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
        verify(chatSessionMapper).insert(captor.capture());
        ChatSession saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(10L);
        assertThat(saved.getAvatarId()).isEqualTo(20L);
        assertThat(saved.getPersonalityId()).isEqualTo(30L);
        assertThat(saved.getPersonalitySnapshot()).isEqualTo(snapshotJson);
        assertThat(saved.getPersonalitySnapshotVersion()).isEqualTo(1);
    }
}
