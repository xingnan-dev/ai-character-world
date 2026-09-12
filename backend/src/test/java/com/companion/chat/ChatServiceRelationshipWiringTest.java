package com.companion.chat;

import com.companion.ai.AiService;
import com.companion.character.snapshot.CharacterSnapshot;
import com.companion.character.snapshot.CharacterSnapshotJsonMapper;
import com.companion.chat.model.ChatMessageExchange;
import com.companion.dto.request.ChatSendRequest;
import com.companion.entity.ChatSession;
import com.companion.mapper.AvatarMapper;
import com.companion.mapper.ChatMessageMapper;
import com.companion.mapper.ChatSessionMapper;
import com.companion.mapper.UserMapper;
import com.companion.service.CharacterService;
import com.companion.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatServiceRelationshipWiringTest {
    @Test void characterChatUsesThePersistedSessionCharacterId() {
        ChatSessionMapper sessions = mock(ChatSessionMapper.class);
        ChatMessageLifecycleService lifecycle = mock(ChatMessageLifecycleService.class);
        AiService ai = mock(AiService.class);
        CharacterSnapshotJsonMapper snapshots = mock(CharacterSnapshotJsonMapper.class);
        ChatSession session = new ChatSession();
        session.setId(20L); session.setUserId(10L); session.setCharacterId(71L);
        session.setCharacterSnapshot("snapshot"); session.setCharacterSnapshotVersion(1); session.setStatus(1);
        CharacterSnapshot snapshot = character(71L);
        when(sessions.selectOne(any())).thenReturn(session);
        when(snapshots.read("snapshot")).thenReturn(snapshot);
        when(lifecycle.createExchange(20L, "hello", "request-1"))
                .thenReturn(new ChatMessageExchange(1L, 2L, "request-1", true));
        when(ai.chatStream(eq(10L), eq(20L), eq("hello"), same(snapshot), any(), eq(71L)))
                .thenReturn(Flux.just("ok"));

        String result = service(sessions, lifecycle, ai, snapshots).sendMessage(10L, request()).blockLast();

        assertThat(result).isEqualTo("ok");
        verify(ai).chatStream(eq(10L), eq(20L), eq("hello"), same(snapshot), any(), eq(71L));
    }

    private ChatServiceImpl service(ChatSessionMapper sessions, ChatMessageLifecycleService lifecycle,
                                    AiService ai, CharacterSnapshotJsonMapper snapshots) {
        return new ChatServiceImpl(sessions, mock(ChatMessageMapper.class), mock(AvatarMapper.class),
                mock(SessionPersonalityResolver.class), lifecycle, ai, mock(CharacterService.class),
                snapshots, mock(UserMapper.class));
    }

    private ChatSendRequest request() {
        ChatSendRequest request = new ChatSendRequest();
        request.setSessionId(20L); request.setContent("hello"); request.setRequestId("request-1");
        return request;
    }

    private CharacterSnapshot character(Long id) {
        return new CharacterSnapshot(1, id, "AI", "Nova", null, "companion", "kind", null,
                null, "friend", "natural", CharacterSnapshot.Profile.empty(), "INITIAL", null, null, "purple");
    }
}
