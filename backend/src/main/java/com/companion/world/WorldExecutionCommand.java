package com.companion.world;

public record WorldExecutionCommand(Long userId, Long worldId, Long roundId) {
    public WorldExecutionCommand {
        if (userId == null || worldId == null || roundId == null) {
            throw new IllegalArgumentException("World execution command identifiers are required");
        }
    }
}
