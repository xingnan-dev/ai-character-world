package com.companion.ai.memory;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.ai.MemoryEngine;
import com.companion.common.exception.BusinessException;
import com.companion.dto.request.MemoryUpdateRequest;
import com.companion.entity.UserMemory;
import com.companion.mapper.UserMemoryMapper;
import com.companion.service.MemoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("soft-delete-test")
@Transactional
class MemoryReliabilityIntegrationTest {

    @Autowired private MemoryEngine memoryEngine;
    @Autowired private MemoryService memoryService;
    @Autowired private UserMemoryMapper memoryMapper;

    @Test
    void createsThenUpdatesSingleNameWithoutDuplicates() {
        memoryEngine.extractMemory("我叫小明", "ignored", 101L);
        memoryEngine.extractMemory("我叫小王", "ignored", 101L);

        List<UserMemory> names = activeMemories(101L, "profile.name");
        assertThat(names).singleElement().satisfies(memory -> {
            assertThat(memory.getValue()).isEqualTo("小王");
            assertThat(memory.getCategory()).isEqualTo(1);
        });
    }

    @Test
    void isolatesUsersAndKeepsDistinctHobbiesWithoutDuplicatingSameValue() {
        memoryEngine.extractMemory("我叫小明", null, 201L);
        memoryEngine.extractMemory("我叫小王", null, 202L);
        memoryEngine.extractMemory("我喜欢音乐", null, 201L);
        memoryEngine.extractMemory("我喜欢阅读", null, 201L);
        memoryEngine.extractMemory("我喜欢音乐", null, 201L);

        assertThat(activeMemories(201L, "profile.name")).singleElement()
                .extracting(UserMemory::getValue).isEqualTo("小明");
        assertThat(activeMemories(202L, "profile.name")).singleElement()
                .extracting(UserMemory::getValue).isEqualTo("小王");
        assertThat(activeMemories(201L, "preference.hobby"))
                .extracting(UserMemory::getValue)
                .containsExactlyInAnyOrder("音乐", "阅读");
    }

    @Test
    void allowsImportanceOnlyUpdateAndMapsAvatarId() {
        UserMemory memory = insertMemory(301L, "profile.name", "小明");
        memory.setAvatarId(901L);
        memoryMapper.updateById(memory);

        memoryService.updateMemory(301L, new MemoryUpdateRequest(memory.getId(), null, 0.95f));

        UserMemory updated = memoryMapper.selectById(memory.getId());
        assertThat(updated.getValue()).isEqualTo("小明");
        assertThat(updated.getImportance()).isEqualTo(0.95f);
        assertThat(updated.getAvatarId()).isEqualTo(901L);
    }

    @Test
    void rejectsBlankOversizedAndOutOfRangeUpdates() {
        UserMemory memory = insertMemory(401L, "profile.name", "小明");

        assertInvalid(memory.getId(), "   ", null);
        assertInvalid(memory.getId(), "x".repeat(2001), null);
        assertInvalid(memory.getId(), null, -0.01f);
        assertInvalid(memory.getId(), null, 1.01f);
    }

    private void assertInvalid(Long id, String value, Float importance) {
        assertThatThrownBy(() -> memoryService.updateMemory(
                401L, new MemoryUpdateRequest(id, value, importance)
        )).isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(400);
    }

    private UserMemory insertMemory(Long userId, String key, String value) {
        UserMemory memory = new UserMemory();
        memory.setUserId(userId);
        memory.setMemoryKey(key);
        memory.setValue(value);
        memory.setImportance(0.5f);
        memory.setStatus(1);
        memoryMapper.insert(memory);
        return memory;
    }

    private List<UserMemory> activeMemories(Long userId, String key) {
        return memoryMapper.selectList(new QueryWrapper<UserMemory>()
                .eq("user_id", userId)
                .eq("memory_key", key)
                .eq("status", 1));
    }
}
