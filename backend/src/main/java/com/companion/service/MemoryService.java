package com.companion.service;

import com.companion.dto.request.MemoryUpdateRequest;
import com.companion.dto.response.MemoryVO;

import java.util.List;

public interface MemoryService {

    List<MemoryVO> getMemoryList(Long userId);

    MemoryVO updateMemory(Long userId, MemoryUpdateRequest request);

    void deleteMemory(Long userId, Long memoryId);

    void clearMemory(Long userId);
}