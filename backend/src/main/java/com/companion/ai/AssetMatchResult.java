package com.companion.ai;

import com.companion.entity.AvatarAsset;

/** Highest-scoring persisted asset candidate and its unmodified score. */
public record AssetMatchResult(AvatarAsset asset, int score) {

    public static AssetMatchResult empty() {
        return new AssetMatchResult(null, Integer.MIN_VALUE);
    }

    public boolean isEmpty() {
        return asset == null;
    }
}
