package com.companion.ai.usage;

import com.companion.entity.AiUsageRecord;
import com.companion.mapper.AiUsageRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultAiUsageService implements AiUsageService {

    private final AiUsageRecordMapper aiUsageRecordMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(AiUsageRecord record) {
        aiUsageRecordMapper.insert(record);
    }
}
