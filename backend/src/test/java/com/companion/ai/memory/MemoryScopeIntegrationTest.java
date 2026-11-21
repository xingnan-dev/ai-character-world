package com.companion.ai.memory;
import com.companion.ai.MemoryEngine;
import com.companion.entity.UserMemory;
import com.companion.mapper.UserMemoryMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest @ActiveProfiles("soft-delete-test") @Transactional
class MemoryScopeIntegrationTest {
    @Autowired MemoryEngine engine;
    @Autowired UserMemoryMapper memories;
    @Test void databaseRetrievalSharesOnlyOwnGlobalFactsAndOwnCharacterFacts() {
        insert(95101L,null,"global-fact"); insert(95101L,71L,"A-only"); insert(95101L,72L,"B-only");
        insert(95102L,null,"other-user");
        assertThat(engine.getMemoryContext(95101L,71L,"facts")).contains("global-fact","A-only").doesNotContain("B-only","other-user");
        assertThat(engine.getMemoryContext(95101L,72L,"facts")).contains("global-fact","B-only").doesNotContain("A-only","other-user");
        assertThat(engine.getMemoryContext(95102L,71L,"facts")).contains("other-user").doesNotContain("global-fact","A-only","B-only");
    }
    private void insert(Long user,Long character,String value){var m=new UserMemory();m.setUserId(user);m.setCharacterId(character);m.setCategory(1);m.setMemoryKey("test.fact");m.setValue(value);m.setImportance(.8f);m.setStatus(1);m.setDeleted(0);m.setCreateTime(LocalDateTime.now());m.setUpdateTime(LocalDateTime.now());m.setLastAccessTime(LocalDateTime.now());memories.insert(m);}
}
