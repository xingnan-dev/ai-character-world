package com.companion.world;

import com.companion.character.snapshot.CharacterSnapshot;

public record WorldActorContext(
        Long participantId,
        Long worldId,
        int displayOrder,
        CharacterSnapshot snapshot,
        String resolutionErrorCode
) {
    public boolean isValid() {
        return snapshot != null && resolutionErrorCode == null;
    }

    public String displayName() {
        return snapshot == null ? "participant-" + participantId : snapshot.name();
    }
}
