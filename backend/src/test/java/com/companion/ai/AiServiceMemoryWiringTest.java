package com.companion.ai;

import com.companion.ai.context.ChatContextAssembler;
import com.companion.ai.prompt.ComposedChatPrompt;
import com.companion.chat.ChatMessageLifecycleService;
import com.companion.chat.model.ChatMessageExchange;
import com.companion.character.snapshot.CharacterSnapshot;
import com.companion.entity.Personality;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiServiceMemoryWiringTest {
    @Test void characterChatPassesCharacterIdToMemoryExtraction() {
        LlmClient llm = mock(LlmClient.class); ChatContextAssembler assembler = mock(ChatContextAssembler.class);
        MemoryEngine memory = mock(MemoryEngine.class); ChatMessageLifecycleService lifecycle = mock(ChatMessageLifecycleService.class);
        when(assembler.assembleCharacter(anyLong(), anyLong(), anyString(), any(), any())).thenReturn(new ComposedChatPrompt(java.util.List.of(), java.util.Map.of()));
        when(llm.streamChat(any(ComposedChatPrompt.class))).thenReturn(Flux.just("ok")); when(lifecycle.markStreaming(anyLong())).thenReturn(true); when(lifecycle.complete(anyLong(), anyString())).thenReturn(true);
        CharacterSnapshot character = mock(CharacterSnapshot.class); when(character.sourceCharacterId()).thenReturn(123L);
        new AiServiceImpl(llm, assembler, memory, lifecycle).chatStream(1L, 2L, "hi", character, new ChatMessageExchange(3L, 4L, "r", true), 123L).blockLast();
        verify(memory).extractMemory("hi", "ok", 1L, 123L);
    }

    @Test void legacyAvatarChatPassesNullCharacterId() {
        LlmClient llm = mock(LlmClient.class); ChatContextAssembler assembler = mock(ChatContextAssembler.class);
        MemoryEngine memory = mock(MemoryEngine.class); ChatMessageLifecycleService lifecycle = mock(ChatMessageLifecycleService.class);
        when(assembler.assemble(anyLong(), anyLong(), anyString(), any(), any())).thenReturn(new ComposedChatPrompt(java.util.List.of(), java.util.Map.of()));
        when(llm.streamChat(any(ComposedChatPrompt.class))).thenReturn(Flux.just("ok")); when(lifecycle.markStreaming(anyLong())).thenReturn(true); when(lifecycle.complete(anyLong(), anyString())).thenReturn(true);
        new AiServiceImpl(llm, assembler, memory, lifecycle).chatStream(1L, 2L, "hi", new Personality(), new ChatMessageExchange(3L, 4L, "r", true)).blockLast();
        verify(memory).extractMemory("hi", "ok", 1L, null);
    }
}
