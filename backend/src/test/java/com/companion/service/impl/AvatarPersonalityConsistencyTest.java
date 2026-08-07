package com.companion.service.impl;

import com.companion.ai.AiService;
import com.companion.chat.ChatMessageLifecycleService;
import com.companion.chat.DefaultSessionPersonalityResolver;
import com.companion.chat.PersonalitySnapshotCodec;
import com.companion.dto.request.AvatarCreateRequest;
import com.companion.dto.request.AvatarPersonalityRequest;
import com.companion.dto.request.ChatSessionCreateRequest;
import com.companion.dto.request.PersonalityCreateRequest;
import com.companion.dto.response.AvatarVO;
import com.companion.entity.Avatar;
import com.companion.entity.ChatSession;
import com.companion.entity.Personality;
import com.companion.mapper.AvatarMapper;
import com.companion.mapper.ChatMessageMapper;
import com.companion.mapper.ChatSessionMapper;
import com.companion.mapper.PersonalityMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvatarPersonalityConsistencyTest {

    @Mock private AvatarMapper avatarMapper;
    @Mock private PersonalityMapper personalityMapper;
    @Mock private ChatSessionMapper chatSessionMapper;
    @Mock private ChatMessageMapper chatMessageMapper;
    @Mock private ChatMessageLifecycleService chatMessageLifecycleService;
    @Mock private AiService aiService;

    private AvatarServiceImpl avatarService;

    @BeforeEach
    void setUp() {
        avatarService = new AvatarServiceImpl(avatarMapper, personalityMapper);
    }

    @Test
    void manualAvatarCreationPersistsBothSidesAndCanCreateChatSession() {
        AtomicReference<Avatar> storedAvatar = new AtomicReference<>();
        AtomicReference<Personality> storedPersonality = new AtomicReference<>();

        when(avatarMapper.insert(any())).thenAnswer(invocation -> {
            Avatar avatar = invocation.getArgument(0);
            avatar.setId(53L);
            storedAvatar.set(avatar);
            return 1;
        });
        when(personalityMapper.insert(any())).thenAnswer(invocation -> {
            Personality personality = invocation.getArgument(0);
            personality.setId(63L);
            storedPersonality.set(personality);
            return 1;
        });
        when(avatarMapper.updateById(any())).thenReturn(1);

        AvatarCreateRequest request = new AvatarCreateRequest();
        request.setName("小雪");
        request.setType(1);
        request.setPersonality(new AvatarPersonalityRequest(
                "小雪的人格", 1, "温柔体贴", "AI虚拟伴侣", "自然亲切", "音乐", "朋友"
        ));

        AvatarVO created = avatarService.createAvatar(3L, request);

        assertThat(storedAvatar.get().getPersonalityId()).isEqualTo(63L);
        assertThat(storedPersonality.get().getAvatarId()).isEqualTo(53L);
        assertThat(created.getPersonalityId()).isEqualTo(63L);
        assertThat(created.getPersonality().getCorePersonality()).isEqualTo("温柔体贴");

        when(avatarMapper.selectOne(any())).thenReturn(storedAvatar.get());
        when(personalityMapper.selectOne(any())).thenReturn(storedPersonality.get());
        when(chatSessionMapper.insert(any())).thenAnswer(invocation -> {
            ChatSession session = invocation.getArgument(0);
            session.setId(73L);
            return 1;
        });

        DefaultSessionPersonalityResolver resolver = new DefaultSessionPersonalityResolver(
                personalityMapper, new PersonalitySnapshotCodec(new ObjectMapper())
        );
        ChatServiceImpl chatService = new ChatServiceImpl(
                chatSessionMapper, chatMessageMapper, avatarMapper, resolver,
                chatMessageLifecycleService, aiService
        );

        chatService.createSession(3L, new ChatSessionCreateRequest(53L, null));

        ArgumentCaptor<ChatSession> sessionCaptor = ArgumentCaptor.forClass(ChatSession.class);
        verify(chatSessionMapper).insert(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getPersonalityId()).isEqualTo(63L);
        assertThat(sessionCaptor.getValue().getPersonalitySnapshot()).contains("温柔体贴");
    }

    @Test
    void personalityServiceCreationUpdatesAvatarBinding() {
        Avatar avatar = new Avatar();
        avatar.setId(54L);
        avatar.setUserId(3L);
        avatar.setStatus(1);
        when(avatarMapper.selectOne(any())).thenReturn(avatar);
        when(personalityMapper.insert(any())).thenAnswer(invocation -> {
            Personality personality = invocation.getArgument(0);
            personality.setId(64L);
            return 1;
        });
        when(avatarMapper.updateById(any())).thenReturn(1);

        PersonalityServiceImpl personalityService = new PersonalityServiceImpl(personalityMapper, avatarMapper);
        PersonalityCreateRequest request = new PersonalityCreateRequest(
                54L, "小学的人格", 1, "幽默", "虚拟人", "自然", "交流", "朋友"
        );

        personalityService.createPersonality(3L, 54L, request);

        assertThat(avatar.getPersonalityId()).isEqualTo(64L);
        verify(avatarMapper).updateById(avatar);
    }
}
