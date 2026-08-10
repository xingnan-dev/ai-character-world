package com.companion.ai.memory;

import com.companion.entity.UserMemory;

public record ScoredMemory(UserMemory memory, double totalScore) {
}
