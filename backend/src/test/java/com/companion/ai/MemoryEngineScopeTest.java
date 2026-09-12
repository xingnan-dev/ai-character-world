package com.companion.ai;

import com.companion.ai.config.LlmProperties;
import com.companion.ai.memory.MemoryRetrievalScorer;
import com.companion.ai.memory.RuleBasedMemoryExtractor;
import com.companion.entity.UserMemory;
import com.companion.mapper.UserMemoryMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MemoryEngineScopeTest {
    private final UserMemoryMapper mapper = Mockito.mock(UserMemoryMapper.class);
    private final MemoryEngine engine = new MemoryEngine(mapper, new RuleBasedMemoryExtractor(), new MemoryRetrievalScorer(), new LlmProperties());

    private UserMemory memory(Long character, String value, float importance) {
        UserMemory m = new UserMemory(); m.setUserId(1L); m.setCharacterId(character); m.setMemoryKey("profile.name"); m.setValue(value); m.setImportance(importance); m.setStatus(1); m.setLastAccessTime(LocalDateTime.now()); return m;
    }

    @Test void characterScopeIncludesGlobalAndOwnButNotOther() {
        Mockito.when(mapper.selectList(Mockito.any())).thenReturn(List.of(memory(null, "global", .5f), memory(10L, "a", .5f)));
        String context = engine.getMemoryContext(1L, 10L, "我的名字");
        assertTrue(context.contains("global") && context.contains("a"));
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserMemory>> query = ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class);
        Mockito.verify(mapper).selectList(query.capture());
        assertTrue(query.getValue().getTargetSql().contains("character_id"));
    }

    @Test void nullCharacterScopeIsSafe() {
        Mockito.when(mapper.selectList(Mockito.any())).thenReturn(List.of());
        assertDoesNotThrow(() -> engine.getMemoryContext(1L, null, "hello"));
    }

    @Test void consolidationQueryKeepsCharacterScope() throws Exception {
        var method = MemoryEngine.class.getDeclaredMethod("saveExtractedMemory", Long.class, Long.class, com.companion.ai.memory.ExtractedMemory.class);
        method.setAccessible(true);
        Mockito.when(mapper.selectOne(Mockito.any())).thenReturn(null);
        method.invoke(engine, 1L, 10L, new com.companion.ai.memory.ExtractedMemory("profile.name", "a", 1, .8f, true));
        ArgumentCaptor<UserMemory> captor = ArgumentCaptor.forClass(UserMemory.class);
        Mockito.verify(mapper).insert(captor.capture());
        assertEquals(10L, captor.getValue().getCharacterId());
    }

    private void save(Long characterId, String key, String value, boolean single) throws Exception {
        var method = MemoryEngine.class.getDeclaredMethod("saveExtractedMemory", Long.class, Long.class, com.companion.ai.memory.ExtractedMemory.class);
        method.setAccessible(true);
        method.invoke(engine, 1L, characterId, new com.companion.ai.memory.ExtractedMemory(key, value, single ? 1 : 2, .8f, single));
    }

    @Test void exactDuplicateUpdatesExistingInsteadOfInserting() throws Exception {
        UserMemory existing = memory(10L, "a", .5f); Mockito.when(mapper.selectOne(Mockito.any())).thenReturn(existing);
        save(10L, "profile.name", "a", true);
        Mockito.verify(mapper).updateById(existing); Mockito.verify(mapper, Mockito.never()).insert(Mockito.any());
    }

    @Test void changedValueUpdatesOnlySameScope() throws Exception {
        UserMemory existing = memory(10L, "old", .5f); Mockito.when(mapper.selectOne(Mockito.any())).thenReturn(existing);
        save(10L, "profile.name", "new", true);
        assertEquals("new", existing.getValue()); Mockito.verify(mapper).updateById(existing);
    }

    @Test void preferenceDuplicateUsesValueScopedQuery() throws Exception {
        UserMemory existing = memory(10L, "music", .5f); Mockito.when(mapper.selectOne(Mockito.any())).thenReturn(existing);
        save(10L, "preference.hobby", "music", false);
        Mockito.verify(mapper).updateById(existing); Mockito.verify(mapper, Mockito.never()).insert(Mockito.any());
    }

    @Test void globalAndCharacterScopesDoNotOverwriteEachOther() throws Exception {
        Mockito.when(mapper.selectOne(Mockito.any())).thenReturn(null);
        save(null, "profile.name", "global", true); save(10L, "profile.name", "specific", true);
        var captor = ArgumentCaptor.forClass(UserMemory.class); Mockito.verify(mapper, Mockito.times(2)).insert(captor.capture());
        assertNull(captor.getAllValues().get(0).getCharacterId()); assertEquals(10L, captor.getAllValues().get(1).getCharacterId());
    }
}
