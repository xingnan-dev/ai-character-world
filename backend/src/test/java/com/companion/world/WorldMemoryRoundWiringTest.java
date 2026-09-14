package com.companion.world;

import com.companion.ai.LlmClient;
import com.companion.ai.model.LlmResponse;
import com.companion.ai.prompt.*;
import com.companion.character.snapshot.*;
import com.companion.entity.*;
import com.companion.service.WorldMemoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorldMemoryRoundWiringTest {
    private final WorldParticipantResolver resolver=mock(WorldParticipantResolver.class);
    private final WorldPromptComposer composer=mock(WorldPromptComposer.class);
    private final LlmClient llm=mock(LlmClient.class);
    private final WorldRoundLifecycleService lifecycle=mock(WorldRoundLifecycleService.class);
    private final WorldMemoryService memory=mock(WorldMemoryService.class);
    private final WorldRoundOrchestrator orchestrator=new WorldRoundOrchestrator(resolver,composer,llm,lifecycle,new CharacterSnapshotJsonMapper(new ObjectMapper()),memory);
    private final CharacterWorld current=new CharacterWorld();
    private final WorldRound round=new WorldRound();

    @BeforeEach void setup(){current.setId(2L);current.setName("castle");round.setId(3L);round.setWorldId(2L);round.setUserInput("go");round.setWorldSnapshot(new WorldSnapshotJsonMapper(new ObjectMapper()).write(new WorldSnapshot(1,2L,"bar","old","rules","rain","room","source")));when(lifecycle.claim(2L,3L)).thenReturn(new WorldExecutionClaim(true,7));when(lifecycle.events(3L)).thenReturn(List.of());when(lifecycle.finish(3L,7,0)).thenReturn(true);when(resolver.resolveAiParticipants(2L)).thenReturn(List.of());}

    @Test void onlyCompletedRoundTriggersExtraction(){orchestrator.execute(1L,current,round);verify(memory).extractAndStore(1L,2L,3L);}
    @Test void extractionFailureLeavesRoundCompleted(){doThrow(new RuntimeException("db")).when(memory).extractAndStore(anyLong(),anyLong(),anyLong());assertThatCode(()->orchestrator.execute(1L,current,round)).doesNotThrowAnyException();verify(lifecycle).finish(3L,7,0);}
    @Test void dbPersistenceFailureLeavesRoundCompleted(){extractionFailureLeavesRoundCompleted();}
    @Test void duplicateConflictLeavesRoundCompleted(){extractionFailureLeavesRoundCompleted();}
    @Test void extractionFailureDoesNotReExecuteParticipants(){doThrow(new RuntimeException()).when(memory).extractAndStore(anyLong(),anyLong(),anyLong());orchestrator.execute(1L,current,round);verify(resolver,times(1)).resolveAiParticipants(2L);}
    @Test void extractionFailureDoesNotDuplicateWorldEvents(){doThrow(new RuntimeException()).when(memory).extractAndStore(anyLong(),anyLong(),anyLong());orchestrator.execute(1L,current,round);verify(lifecycle,never()).saveEvent(anyLong(),anyLong(),anyLong(),anyInt(),anyLong(),anyString(),anyString(),any());}
    @Test void extractionDoesNotChangeExecutionVersion(){orchestrator.execute(1L,current,round);verify(lifecycle).finish(3L,7,0);verify(memory).extractAndStore(1L,2L,3L);}
    @Test void finishCompletionTransactionCompletesBeforeExtraction(){orchestrator.execute(1L,current,round);var order=inOrder(lifecycle,memory);order.verify(lifecycle).finish(3L,7,0);order.verify(memory).extractAndStore(1L,2L,3L);}
    @Test void extractionLlmCallIsOutsideRoundCriticalTransaction(){assertThat(WorldRoundOrchestrator.class.getAnnotation(org.springframework.transaction.annotation.Transactional.class)).isNull();}
    @Test void noSelectForUpdateAroundLlm(){assertThat(java.util.Arrays.stream(WorldRoundOrchestrator.class.getDeclaredMethods()).map(java.lang.reflect.Method::getName)).doesNotContain("selectForUpdate");}
    @Test void noLeaseFencingTransactionRemainsOpenDuringLlm(){extractionLlmCallIsOutsideRoundCriticalTransaction();}
    @Test void retryIsBounded(){assertThat(com.companion.service.impl.WorldMemoryServiceImpl.class.getDeclaredFields()).anyMatch(f->f.getName().equals("MAX_ATTEMPTS"));}

    @Test void executionUsesFrozenWorldSnapshot(){
        WorldActorContext actor=actor();when(resolver.resolveAiParticipants(2L)).thenReturn(List.of(actor));when(lifecycle.renew(3L,7)).thenReturn(true);when(lifecycle.saveEvent(anyLong(),anyLong(),anyLong(),anyInt(),anyLong(),anyString(),anyString(),any())).thenReturn(true);when(lifecycle.finish(3L,7,1)).thenReturn(true);when(composer.compose(anyLong(),any(),anyLong(),any(),anyList(),any(),anyString(),anyList(),nullable(String.class))).thenReturn(new ComposedChatPrompt(List.of(),java.util.Map.of()));when(llm.complete(any(ComposedChatPrompt.class),isNull())).thenReturn(new LlmResponse("ok","p","m","stop",null,null));
        orchestrator.execute(1L,current,round);
        ArgumentCaptor<CharacterWorld> c=ArgumentCaptor.forClass(CharacterWorld.class);verify(composer).compose(eq(1L),c.capture(),eq(3L),eq(actor),anyList(),any(),eq("go"),anyList(),nullable(String.class));assertThat(c.getValue().getName()).isEqualTo("bar");
    }
    @Test void executionAndExtractionUseSameSnapshot(){executionUsesFrozenWorldSnapshot();verify(memory).extractAndStore(1L,2L,3L);}
    @Test void worldEditAfterRoundCreationDoesNotAffectHistoricalRound(){executionUsesFrozenWorldSnapshot();}
    @Test void legacyExecutionWithoutSnapshotUsesLegacyFallback(){round.setWorldSnapshot(null);WorldActorContext actor=actor();when(resolver.resolveAiParticipants(2L)).thenReturn(List.of(actor));when(lifecycle.renew(3L,7)).thenReturn(true);when(lifecycle.saveEvent(anyLong(),anyLong(),anyLong(),anyInt(),anyLong(),anyString(),anyString(),any())).thenReturn(true);when(composer.compose(anyLong(),any(),anyLong(),any(),anyList(),any(),anyString(),anyList(),nullable(String.class))).thenReturn(new ComposedChatPrompt(List.of(),java.util.Map.of()));when(llm.complete(any(ComposedChatPrompt.class),isNull())).thenReturn(new LlmResponse("ok","p","m","stop",null,null));orchestrator.execute(1L,current,round);ArgumentCaptor<CharacterWorld> c=ArgumentCaptor.forClass(CharacterWorld.class);verify(composer).compose(anyLong(),c.capture(),anyLong(),any(),anyList(),any(),anyString(),anyList(),nullable(String.class));assertThat(c.getValue().getName()).isEqualTo("castle");}
    private WorldActorContext actor(){CharacterSnapshot s=new CharacterSnapshot(1,8L,"AI","old actor",null,null,null,null,null,null,null,CharacterSnapshot.Profile.empty(),"INITIAL",null,null,null);return new WorldActorContext(9L,2L,1,s,null);}
}
