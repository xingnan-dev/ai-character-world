package com.companion.world;

import com.companion.ai.LlmClient;
import com.companion.ai.exception.LlmErrorType;
import com.companion.ai.exception.LlmProviderException;
import com.companion.ai.model.LlmResponse;
import com.companion.ai.prompt.ComposedChatPrompt;
import com.companion.ai.prompt.ClasspathPromptTemplateLoader;
import com.companion.ai.prompt.PromptTemplateRenderer;
import com.companion.ai.prompt.WorldPromptComposer;
import com.companion.character.snapshot.CharacterSnapshot;
import com.companion.entity.CharacterWorld;
import com.companion.entity.WorldEvent;
import com.companion.entity.WorldRound;
import com.companion.entity.enums.WorldEventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorldRoundOrchestratorTest {

    private final WorldParticipantResolver resolver = mock(WorldParticipantResolver.class);
    private final PromptTemplateRenderer renderer = new PromptTemplateRenderer();
    private final WorldPromptComposer composer = new WorldPromptComposer(
            new ClasspathPromptTemplateLoader(new DefaultResourceLoader(), renderer), renderer);
    private final StubLlmClient llm = new StubLlmClient();
    private final WorldRoundLifecycleService lifecycle = mock(WorldRoundLifecycleService.class);
    private final WorldRoundOrchestrator orchestrator =
            new WorldRoundOrchestrator(resolver, composer, llm, lifecycle);
    private final CharacterWorld world = new CharacterWorld();
    private final WorldRound round = new WorldRound();
    private final List<WorldEvent> stored = new ArrayList<>();

    @BeforeEach
    void setUp() {
        world.setId(5L); world.setName("world");
        round.setId(7L); round.setWorldId(5L); round.setUserInput("hello");
        stored.add(event(1, null, "hello", WorldEventStatus.COMPLETED.name()));
        when(lifecycle.claim(5L, 7L)).thenReturn(new WorldExecutionClaim(true, 3));
        when(lifecycle.renew(7L, 3)).thenReturn(true);
        when(lifecycle.events(7L)).thenAnswer(invocation -> List.copyOf(stored));
        when(lifecycle.saveEvent(eq(5L), eq(7L), eq(3L), anyInt(), anyLong(), anyString(), anyString(), any()))
                .thenAnswer(invocation -> {
                    stored.add(event(invocation.getArgument(3), invocation.getArgument(4),
                            invocation.getArgument(5), invocation.getArgument(6)));
                    return true;
                });
    }

    @Test
    void invokesSeriallyAndPersistsDeterministicSequenceBeforeContinuing() {
        when(resolver.resolveAiParticipants(5L)).thenReturn(List.of(actor(10L, 1), actor(20L, 2)));
        llm.thenReturn(response("first"));
        llm.thenReturn(response("second"));

        orchestrator.execute(9L, world, round);

        ArgumentCaptor<Integer> sequences = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Long> participants = ArgumentCaptor.forClass(Long.class);
        verify(lifecycle, times(2)).saveEvent(eq(5L), eq(7L), eq(3L), sequences.capture(),
                participants.capture(), anyString(), eq("COMPLETED"), eq(null));
        assertThat(sequences.getAllValues()).containsExactly(2, 3);
        assertThat(participants.getAllValues()).containsExactly(10L, 20L);
        verify(lifecycle).finish(7L, 3, 2);
    }

    @Test
    void recordsStableFailureAndContinuesWithLaterActor() {
        when(resolver.resolveAiParticipants(5L)).thenReturn(List.of(actor(10L, 1), actor(20L, 2)));
        llm.thenThrow(new LlmProviderException("provider", LlmErrorType.TIMEOUT, null, true, "secret detail"));
        llm.thenReturn(response("survived"));

        orchestrator.execute(9L, world, round);

        verify(lifecycle).saveEvent(5L, 7L, 3, 2, 10L, "", "FAILED", "LLM_TIMEOUT");
        verify(lifecycle).saveEvent(5L, 7L, 3, 3, 20L, "survived", "COMPLETED", null);
        assertThat(llm.calls).isEqualTo(2);
    }

    @Test
    void skipsRecoveredTerminalEventAndDoesNotDuplicateLlmCall() {
        WorldActorContext first = actor(10L, 1);
        WorldActorContext second = actor(20L, 2);
        stored.add(event(2, 10L, "already", "COMPLETED"));
        when(resolver.resolveAiParticipants(5L)).thenReturn(List.of(first, second));
        llm.thenReturn(response("second"));

        orchestrator.execute(9L, world, round);

        assertThat(llm.calls).isEqualTo(1);
        verify(lifecycle, never()).saveEvent(eq(5L), eq(7L), eq(3L), eq(2), eq(10L), any(), any(), any());
        verify(lifecycle).saveEvent(5L, 7L, 3, 3, 20L, "second", "COMPLETED", null);
    }

    @Test
    void stopsWhenFencingRejectsOldWorker() {
        when(resolver.resolveAiParticipants(5L)).thenReturn(List.of(actor(10L, 1), actor(20L, 2)));
        llm.thenReturn(response("late"));
        when(lifecycle.saveEvent(eq(5L), eq(7L), eq(3L), anyInt(), anyLong(), anyString(), anyString(), any()))
                .thenReturn(false);

        orchestrator.execute(9L, world, round);

        assertThat(llm.calls).isEqualTo(1);
        verify(lifecycle, never()).finish(anyLong(), anyLong(), anyInt());
    }

    @Test
    void noAiFinishesAsFailedWithoutCallingProvider() {
        when(resolver.resolveAiParticipants(5L)).thenReturn(List.of());
        orchestrator.execute(9L, world, round);
        assertThat(llm.calls).isZero();
        verify(lifecycle).finish(7L, 3, 0);
    }

    private WorldActorContext actor(long id, int order) {
        CharacterSnapshot snapshot = new CharacterSnapshot(1, id, "AI", "actor-" + id, null,
                null, null, null, null, null, null, CharacterSnapshot.Profile.empty(),
                "INITIAL", null, null, "#000");
        return new WorldActorContext(id, 5L, order, snapshot, null);
    }

    private WorldEvent event(int sequence, Long participantId, String content, String status) {
        WorldEvent event = new WorldEvent();
        event.setRoundId(7L); event.setSequenceNo(sequence); event.setParticipantId(participantId);
        event.setContent(content); event.setStatus(status);
        return event;
    }

    private LlmResponse response(String content) {
        return new LlmResponse(content, "mock", "model", "stop", null, null);
    }

    private static final class StubLlmClient extends LlmClient {
        private final List<Object> outcomes = new ArrayList<>();
        private int calls;

        private StubLlmClient() {
            super(null, null);
        }

        private void thenReturn(LlmResponse response) {
            outcomes.add(response);
        }

        private void thenThrow(RuntimeException error) {
            outcomes.add(error);
        }

        @Override
        public LlmResponse complete(ComposedChatPrompt prompt, Integer maxTokens) {
            Object outcome = outcomes.get(calls++);
            if (outcome instanceof RuntimeException error) throw error;
            return (LlmResponse) outcome;
        }
    }
}
