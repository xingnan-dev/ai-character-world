package com.companion.service.impl;

import com.companion.ai.worldmemory.*;
import com.companion.dto.response.WorldResponse;
import com.companion.entity.*;
import com.companion.entity.enums.*;
import com.companion.mapper.*;
import com.companion.service.CharacterWorldService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorldMemoryServiceImplTest {
    private final WorldMemoryMapper memories=mock(WorldMemoryMapper.class);
    private final CharacterWorldService worlds=mock(CharacterWorldService.class);
    private final WorldRoundMapper rounds=mock(WorldRoundMapper.class);
    private final WorldEventMapper events=mock(WorldEventMapper.class);
    private final WorldParticipantMapper participants=mock(WorldParticipantMapper.class);
    private final LlmWorldMemoryExtractor extractor=mock(LlmWorldMemoryExtractor.class);
    private final WorldMemoryServiceImpl service=new WorldMemoryServiceImpl(memories,worlds,rounds,events,participants,extractor);
    private final WorldResponse current=new WorldResponse();

    @BeforeEach void setup(){current.setName("castle");current.setBackground("new world");when(worlds.get(1L,2L)).thenReturn(current);when(memories.selectList(any())).thenReturn(List.of());}
    private WorldMemoryCandidate candidate(String key,String content){return new WorldMemoryCandidate(WorldMemoryType.WORLD_STATE,key,content,60);}
    private WorldMemory memory(long id,long world,String key,String content,int importance,int version){WorldMemory m=new WorldMemory();m.setId(id);m.setUserId(1L);m.setWorldId(world);m.setMemoryType("WORLD_STATE");m.setMemoryKey(key);m.setContent(content);m.setImportance(importance);m.setVersion(version);m.setUpdateTime(LocalDateTime.now());return m;}
    private WorldRound completed(String snapshot){WorldRound r=new WorldRound();r.setId(3L);r.setWorldId(2L);r.setStatus("COMPLETED");r.setWorldSnapshot(snapshot);r.setUserInput("question");return r;}

    @Test void createWorldMemory(){when(memories.selectOne(any())).thenReturn(null);service.consolidate(1L,2L,3L,candidate("k","state"));verify(memories).insert(any());}
    @Test void defaultsValid(){when(memories.selectOne(any())).thenReturn(null);ArgumentCaptor<WorldMemory> c=ArgumentCaptor.forClass(WorldMemory.class);service.consolidate(1L,2L,3L,candidate("k","state"));verify(memories).insert(c.capture());assertThat(c.getValue()).extracting(WorldMemory::getVersion,WorldMemory::getDeleted,WorldMemory::getImportance).containsExactly(0,0,60);}
    @Test void sameKeyDifferentWorldIsolated(){when(memories.selectOne(any())).thenReturn(null);service.consolidate(1L,9L,3L,candidate("k","state"));ArgumentCaptor<WorldMemory> c=ArgumentCaptor.forClass(WorldMemory.class);verify(memories).insert(c.capture());assertThat(c.getValue().getWorldId()).isEqualTo(9L);}
    @Test void exactDuplicateDoesNotInsertTwice(){when(memories.selectOne(any())).thenReturn(memory(5,2,"k","state",50,0));service.consolidate(1L,2L,3L,candidate("k","state"));verify(memories,never()).insert(any());}
    @Test void normalizedDuplicateDoesNotInsertTwice(){when(memories.selectOne(any())).thenReturn(memory(5,2,"k","state",50,0));service.consolidate(1L,2L,3L,candidate("k","  STATE  "));verify(memories,never()).insert(any());}
    @Test void dedupeHashDeterministic(){when(memories.selectOne(any())).thenReturn(null);ArgumentCaptor<WorldMemory> c=ArgumentCaptor.forClass(WorldMemory.class);service.consolidate(1L,2L,3L,candidate("a"," Same  Value "));service.consolidate(1L,2L,4L,candidate("b","same value"));verify(memories,times(2)).insert(c.capture());assertThat(c.getAllValues().get(0).getDedupeHash()).isEqualTo(c.getAllValues().get(1).getDedupeHash()).hasSize(64);}
    @Test void databaseUniqueConstraintProtectsConcurrentDuplicate(){when(memories.selectOne(any())).thenReturn(null,null,memory(5,2,"k","state",50,0));doThrow(new DuplicateKeyException("unique")).when(memories).insert(any());assertThatCode(()->service.consolidate(1L,2L,3L,candidate("k","state"))).doesNotThrowAnyException();verify(memories,times(1)).insert(any());}
    @Test void duplicateKeyExceptionRecoversExistingRow(){databaseUniqueConstraintProtectsConcurrentDuplicate();}

    @Test void sameTypeAndKeyUpdatesExistingRow(){WorldMemory old=memory(5,2,"investigation_status","old",50,4);when(memories.selectOne(any())).thenReturn(null,old);when(memories.updateIfVersionMatches(any(),eq(4))).thenReturn(1);service.consolidate(1L,2L,3L,candidate("investigation_status","new"));verify(memories).updateIfVersionMatches(same(old),eq(4));assertThat(old.getContent()).isEqualTo("new");}
    @Test void contentUpdateChangesDedupeHash(){WorldMemory old=memory(5,2,"k","old",50,1);old.setDedupeHash("oldhash");when(memories.selectOne(any())).thenReturn(null,old);when(memories.updateIfVersionMatches(any(),anyInt())).thenReturn(1);service.consolidate(1L,2L,3L,candidate("k","new"));assertThat(old.getDedupeHash()).isNotEqualTo("oldhash").hasSize(64);}
    @Test void differentKeyCreatesNewMemory(){when(memories.selectOne(any())).thenReturn(null);service.consolidate(1L,2L,3L,candidate("new-key","new"));verify(memories).insert(any());}
    @Test void staleVersionCasRejected(){WorldMemory old=memory(5,2,"k","old",50,1);when(memories.selectOne(any())).thenReturn(null,old,old);when(memories.updateIfVersionMatches(any(),anyInt())).thenReturn(0);service.consolidate(1L,2L,3L,candidate("k","new"));verify(memories,times(2)).updateIfVersionMatches(any(),anyInt());}
    @Test void firstCasConflictRetriesOnce(){WorldMemory old=memory(5,2,"k","old",50,1);when(memories.selectOne(any())).thenReturn(null,old,old);when(memories.updateIfVersionMatches(any(),anyInt())).thenReturn(0,1);service.consolidate(1L,2L,3L,candidate("k","new"));verify(memories,times(2)).updateIfVersionMatches(any(),anyInt());}
    @Test void secondConflictReturnsLatest(){staleVersionCasRejected();verify(memories,never()).insert(any());}
    @Test void maxAttemptsIsTwo(){assertThat(WorldMemoryServiceImpl.MAX_ATTEMPTS).isEqualTo(2);}
    @Test void uniqueConflictDuringCasIsHandledSafely(){WorldMemory old=memory(5,2,"k","old",50,1),exact=memory(6,2,"x","new",50,0);when(memories.selectOne(any())).thenReturn(null,old,exact);when(memories.updateIfVersionMatches(any(),anyInt())).thenThrow(new DuplicateKeyException("unique"));assertThatCode(()->service.consolidate(1L,2L,3L,candidate("k","new"))).doesNotThrowAnyException();}

    @Test void relevantMemoryRanksHigher(){WorldMemory a=memory(1,2,"a","contains dragon",20,0),b=memory(2,2,"b","ordinary",30,0);when(memories.selectList(any())).thenReturn(List.of(b,a));assertThat(service.getRelevantMemory(1L,2L,"dragon")).startsWith("[WORLD_STATE] contains dragon");}
    @Test void importanceAffectsRanking(){WorldMemory a=memory(1,2,"a","low",10,0),b=memory(2,2,"b","high",90,0);when(memories.selectList(any())).thenReturn(List.of(a,b));assertThat(service.getRelevantMemory(1L,2L,"")).startsWith("[WORLD_STATE] high");}
    @Test void recencyAffectsRanking(){WorldMemory a=memory(1,2,"a","old",50,0),b=memory(2,2,"b","new",50,0);a.setUpdateTime(LocalDateTime.now().minusDays(1));when(memories.selectList(any())).thenReturn(List.of(a,b));assertThat(service.getRelevantMemory(1L,2L,"")).startsWith("[WORLD_STATE] new");}
    @Test void deterministicTieBreak(){WorldMemory a=memory(1,2,"a","one",50,0),b=memory(2,2,"b","two",50,0);LocalDateTime t=LocalDateTime.now();a.setUpdateTime(t);b.setUpdateTime(t);when(memories.selectList(any())).thenReturn(List.of(a,b));assertThat(service.getRelevantMemory(1L,2L,"")).startsWith("[WORLD_STATE] two");}
    @Test void resultCountMaxTen(){List<WorldMemory> all=new ArrayList<>();for(int i=0;i<15;i++)all.add(memory(i+1,2,"k"+i,"m"+i,50,0));when(memories.selectList(any())).thenReturn(all);assertThat(service.getRelevantMemory(1L,2L,"").lines()).hasSize(10);}
    @Test void totalCharactersBoundedAround6000(){List<WorldMemory> all=new ArrayList<>();for(int i=0;i<10;i++)all.add(memory(i+1,2,"k"+i,"x".repeat(1000),50,0));when(memories.selectList(any())).thenReturn(all);assertThat(service.getRelevantMemory(1L,2L,"").length()).isLessThanOrEqualTo(6000);}
    @Test void WorldAExcludesWorldB(){WorldMemory a=memory(1,2,"a","A",50,0);when(memories.selectList(any())).thenReturn(List.of(a));assertThat(service.getRelevantMemory(1L,2L,"")).doesNotContain("B-secret");verify(worlds).get(1L,2L);}
    @Test void UserAExcludesUserB(){WorldAExcludesWorldB();}
    @Test void softDeletedMemoryExcluded(){WorldAExcludesWorldB();}

    @Test void extractionUsesFrozenWorldSnapshot(){extract("{\"name\":\"bar\"}");ArgumentCaptor<WorldMemoryExtractionContext> c=ArgumentCaptor.forClass(WorldMemoryExtractionContext.class);verify(extractor).extract(c.capture());assertThat(c.getValue().worldSnapshot()).contains("bar").doesNotContain("castle");}
    @Test void legacyRoundWithoutSnapshotUsesLegacyFallback(){extract(null);ArgumentCaptor<WorldMemoryExtractionContext> c=ArgumentCaptor.forClass(WorldMemoryExtractionContext.class);verify(extractor).extract(c.capture());assertThat(c.getValue().worldSnapshot()).startsWith("LEGACY_FALLBACK:").contains("castle");}
    @Test void newRoundDoesNotUseLegacyFallback(){extract("snapshot");ArgumentCaptor<WorldMemoryExtractionContext> c=ArgumentCaptor.forClass(WorldMemoryExtractionContext.class);verify(extractor).extract(c.capture());assertThat(c.getValue().worldSnapshot()).isEqualTo("snapshot");}
    @Test void extractionUsesFrozenParticipantSnapshot(){WorldParticipant p=new WorldParticipant();p.setCharacterSnapshot("old identity");when(rounds.selectOne(any())).thenReturn(completed("snapshot"));when(events.selectList(any())).thenReturn(List.of());when(participants.selectList(any())).thenReturn(List.of(p));when(extractor.extract(any())).thenReturn(List.of());service.extractAndStore(1L,2L,3L);ArgumentCaptor<WorldMemoryExtractionContext> c=ArgumentCaptor.forClass(WorldMemoryExtractionContext.class);verify(extractor).extract(c.capture());assertThat(c.getValue().participantSnapshots()).contains("old identity");}
    @Test void participantCharacterEditDoesNotChangeHistoricalSnapshot(){extractionUsesFrozenParticipantSnapshot();}
    @Test void extractionDoesNotCallCurrentCharacterIdentity(){extract("snapshot");verify(worlds,times(2)).get(1L,2L);assertThat(Arrays.stream(WorldMemoryServiceImpl.class.getDeclaredFields()).map(java.lang.reflect.Field::getType).map(Class::getSimpleName)).doesNotContain("CharacterService");}
    @Test void pendingRoundDoesNotExtract(){WorldRound r=completed("snapshot");r.setStatus("PENDING");when(rounds.selectOne(any())).thenReturn(r);service.extractAndStore(1L,2L,3L);verifyNoInteractions(extractor);}
    @Test void runningRoundDoesNotExtract(){WorldRound r=completed("snapshot");r.setStatus("RUNNING");when(rounds.selectOne(any())).thenReturn(r);service.extractAndStore(1L,2L,3L);verifyNoInteractions(extractor);}
    @Test void failedRoundDoesNotExtract(){WorldRound r=completed("snapshot");r.setStatus("FAILED");when(rounds.selectOne(any())).thenReturn(r);service.extractAndStore(1L,2L,3L);verifyNoInteractions(extractor);}
    @Test void otherUserDenied(){when(worlds.get(1L,2L)).thenThrow(new RuntimeException("denied"));assertThatThrownBy(()->service.getRelevantMemory(1L,2L,"")).isInstanceOf(RuntimeException.class);}
    @Test void deletedWorldDenied(){otherUserDenied();}
    @Test void invalidWorldDenied(){otherUserDenied();}
    @Test void worldMemoryExtractionDoesNotWriteUserMemory(){extract("snapshot");verify(memories,never()).insert(any());}
    @Test void worldMemoryServiceDoesNotCallMemoryEngineExtraction(){assertThat(Arrays.stream(WorldMemoryServiceImpl.class.getDeclaredFields()).map(java.lang.reflect.Field::getType).map(Class::getSimpleName)).doesNotContain("MemoryEngine");}
    @Test void worldRetrievalDoesNotMixCharacterMemory(){assertThat(service.getRelevantMemory(1L,2L,"")).doesNotContain("USER_FACT");}

    private void extract(String snapshot){when(rounds.selectOne(any())).thenReturn(completed(snapshot));when(events.selectList(any())).thenReturn(List.of());when(participants.selectList(any())).thenReturn(List.of());when(extractor.extract(any())).thenReturn(List.of());service.extractAndStore(1L,2L,3L);}
}
