package com.companion.ai;

import com.companion.ai.context.ChatContextAssembler;
import com.companion.ai.prompt.ComposedChatPrompt;
import com.companion.chat.ChatMessageLifecycleService;
import com.companion.chat.model.ChatMessageExchange;
import com.companion.character.snapshot.CharacterSnapshot;
import com.companion.entity.Personality;
import com.companion.service.CharacterRelationshipService;
import com.companion.service.CharacterGrowthService;
import com.companion.ai.growth.GrowthEvaluationContext;
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
        CharacterRelationshipService relationships = mock(CharacterRelationshipService.class);
        CharacterGrowthService growth = mock(CharacterGrowthService.class);
        when(assembler.assembleCharacter(anyLong(), anyLong(), anyString(), any(), any())).thenReturn(new ComposedChatPrompt(java.util.List.of(), java.util.Map.of()));
        when(llm.streamChat(any(ComposedChatPrompt.class))).thenReturn(Flux.just("ok")); when(lifecycle.markStreaming(anyLong())).thenReturn(true); when(lifecycle.complete(anyLong(), anyString())).thenReturn(true);
        CharacterSnapshot character = mock(CharacterSnapshot.class); when(character.sourceCharacterId()).thenReturn(123L);
        GrowthEvaluationContext context = new GrowthEvaluationContext("i","r","m","hi","ok"); when(assembler.growthEvaluationContext(1L,character,"hi","ok")).thenReturn(context);
        new AiServiceImpl(llm, assembler, memory, lifecycle, relationships, growth).chatStream(1L, 2L, "hi", character, new ChatMessageExchange(3L, 4L, "r", true), 123L).blockLast();
        verify(memory).extractMemory("hi", "ok", 1L, 123L);
        verify(relationships).evaluateAndUpdate(1L, 123L, "hi", "ok");
        verify(growth).evaluateAndUpdate(1L,123L,context);
    }

    @Test void legacyAvatarChatPassesNullCharacterId() {
        LlmClient llm = mock(LlmClient.class); ChatContextAssembler assembler = mock(ChatContextAssembler.class);
        MemoryEngine memory = mock(MemoryEngine.class); ChatMessageLifecycleService lifecycle = mock(ChatMessageLifecycleService.class);
        CharacterRelationshipService relationships = mock(CharacterRelationshipService.class);
        when(assembler.assemble(anyLong(), anyLong(), anyString(), any(), any())).thenReturn(new ComposedChatPrompt(java.util.List.of(), java.util.Map.of()));
        when(llm.streamChat(any(ComposedChatPrompt.class))).thenReturn(Flux.just("ok")); when(lifecycle.markStreaming(anyLong())).thenReturn(true); when(lifecycle.complete(anyLong(), anyString())).thenReturn(true);
        new AiServiceImpl(llm, assembler, memory, lifecycle, relationships).chatStream(1L, 2L, "hi", new Personality(), new ChatMessageExchange(3L, 4L, "r", true)).blockLast();
        verify(memory).extractMemory("hi", "ok", 1L, null);
        verifyNoInteractions(relationships);
    }

    @Test void relationshipFailureDoesNotBlockCharacterChatOrMemory() {
        LlmClient llm = mock(LlmClient.class); ChatContextAssembler assembler = mock(ChatContextAssembler.class);
        MemoryEngine memory = mock(MemoryEngine.class); ChatMessageLifecycleService lifecycle = mock(ChatMessageLifecycleService.class);
        CharacterRelationshipService relationships = mock(CharacterRelationshipService.class);
        when(assembler.assembleCharacter(anyLong(), anyLong(), anyString(), any(), any())).thenReturn(new ComposedChatPrompt(java.util.List.of(), java.util.Map.of()));
        when(llm.streamChat(any(ComposedChatPrompt.class))).thenReturn(Flux.just("ok")); when(lifecycle.markStreaming(anyLong())).thenReturn(true); when(lifecycle.complete(anyLong(), anyString())).thenReturn(true);
        when(relationships.evaluateAndUpdate(anyLong(), anyLong(), anyString(), anyString())).thenThrow(new RuntimeException("down"));
        CharacterSnapshot character = mock(CharacterSnapshot.class); when(character.sourceCharacterId()).thenReturn(123L);

        assertEquals("ok", new AiServiceImpl(llm, assembler, memory, lifecycle, relationships)
                .chatStream(1L, 2L, "meaningful", character, new ChatMessageExchange(3L, 4L, "r", true), 123L)
                .blockLast());
        verify(memory).extractMemory("meaningful", "ok", 1L, 123L);
        verify(lifecycle).complete(4L, "ok");
    }

    @Test void memoryRelationshipAndGrowthFailuresAreIndependentAndCompletionStaysDone() {
        LlmClient llm=mock(LlmClient.class);ChatContextAssembler assembler=mock(ChatContextAssembler.class);MemoryEngine memory=mock(MemoryEngine.class);ChatMessageLifecycleService lifecycle=mock(ChatMessageLifecycleService.class);CharacterRelationshipService relationships=mock(CharacterRelationshipService.class);CharacterGrowthService growth=mock(CharacterGrowthService.class);CharacterSnapshot character=mock(CharacterSnapshot.class);when(character.sourceCharacterId()).thenReturn(123L);when(assembler.assembleCharacter(anyLong(),anyLong(),anyString(),any(),any())).thenReturn(new ComposedChatPrompt(java.util.List.of(),java.util.Map.of()));when(llm.streamChat(any(ComposedChatPrompt.class))).thenReturn(Flux.just("ok"));when(lifecycle.markStreaming(anyLong())).thenReturn(true);when(lifecycle.complete(anyLong(),anyString())).thenReturn(true);doThrow(new RuntimeException("memory")).when(memory).extractMemory(anyString(),anyString(),anyLong(),anyLong());when(relationships.evaluateAndUpdate(anyLong(),anyLong(),anyString(),anyString())).thenThrow(new RuntimeException("relationship"));when(growth.evaluateAndUpdate(anyLong(),anyLong(),any())).thenThrow(new RuntimeException("growth"));assertEquals("ok",new AiServiceImpl(llm,assembler,memory,lifecycle,relationships,growth).chatStream(1L,2L,"meaningful",character,new ChatMessageExchange(3L,4L,"r",true),123L).blockLast());verify(lifecycle).complete(4L,"ok");verify(memory).extractMemory("meaningful","ok",1L,123L);verify(relationships).evaluateAndUpdate(1L,123L,"meaningful","ok");verify(growth).evaluateAndUpdate(eq(1L),eq(123L),isNull());
    }
}
