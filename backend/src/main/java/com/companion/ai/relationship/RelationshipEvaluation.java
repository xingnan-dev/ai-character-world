package com.companion.ai.relationship;

import com.companion.entity.enums.RelationshipStage;

public record RelationshipEvaluation(
        boolean changed,
        RelationshipStage stage,
        String summary,
        String interactionStyle,
        String recentChange
) {
    public static RelationshipEvaluation unchanged() {
        return new RelationshipEvaluation(false, null, null, null, null);
    }
}
