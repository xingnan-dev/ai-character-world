package com.companion.service;
public interface WorldMemoryService { String getRelevantMemory(Long userId,Long worldId,String query); void extractAndStore(Long userId,Long worldId,Long roundId); }
