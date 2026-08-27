package com.companion.world;

public record WorldExecutionClaim(boolean acquired, long executionVersion) {
    public static WorldExecutionClaim notAcquired() {
        return new WorldExecutionClaim(false, -1);
    }
}
