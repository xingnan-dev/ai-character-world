package com.companion.ai.worldmemory;
import com.companion.entity.enums.WorldMemoryType;
public record WorldMemoryCandidate(WorldMemoryType type,String key,String content,int importance) {}
