package com.companion.entity.enums;

import java.util.Locale;
import java.util.Optional;

public enum RelationshipStage {
    NEW,
    FAMILIAR,
    TRUSTED,
    CLOSE;

    public static Optional<RelationshipStage> parse(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
