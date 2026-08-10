package com.companion.ai.usage;

import com.companion.entity.AiUsageRecord;
import com.companion.mapper.AiUsageRecordMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("soft-delete-test")
@Transactional
class AiUsagePersistenceIntegrationTest {

    @Autowired
    private AiUsageService aiUsageService;

    @Autowired
    private AiUsageRecordMapper aiUsageRecordMapper;

    @Test
    void savesUsageRecordWithNullableTokenAndOwnershipFields() {
        AiUsageRecord record = new AiUsageRecord();
        record.setLlmRequestId("integration-usage-1");
        record.setProvider("mock");
        record.setModel("test-model");
        record.setLatencyMs(12L);
        record.setSuccess(1);
        record.setCreatedTime(LocalDateTime.now());

        aiUsageService.save(record);

        AiUsageRecord saved = aiUsageRecordMapper.selectById(record.getId());
        assertThat(saved).isNotNull();
        assertThat(saved.getPromptTokens()).isNull();
        assertThat(saved.getCompletionTokens()).isNull();
        assertThat(saved.getTotalTokens()).isNull();
        assertThat(saved.getUserId()).isNull();
        assertThat(saved.getSessionId()).isNull();
    }
}
