package com.companion.chat.model;

import com.companion.entity.Personality;

public record PersonalitySnapshot(
        Long personalityId,
        Long avatarId,
        String name,
        String corePersonality,
        String identity,
        String languageStyle,
        String hobbies,
        String relationship,
        Integer snapshotVersion
) {
    public static final int CURRENT_VERSION = 1;

    public static PersonalitySnapshot from(Personality personality) {
        if (personality == null) {
            throw new IllegalArgumentException("Personality is required");
        }
        return new PersonalitySnapshot(
                personality.getId(),
                personality.getAvatarId(),
                personality.getName(),
                personality.getCorePersonality(),
                personality.getIdentity(),
                personality.getLanguageStyle(),
                personality.getHobbies(),
                personality.getRelationship(),
                CURRENT_VERSION
        );
    }

    public Personality toPersonality() {
        Personality personality = new Personality();
        personality.setId(personalityId);
        personality.setAvatarId(avatarId);
        personality.setName(name);
        personality.setCorePersonality(corePersonality);
        personality.setIdentity(identity);
        personality.setLanguageStyle(languageStyle);
        personality.setHobbies(hobbies);
        personality.setRelationship(relationship);
        personality.setStatus(1);
        return personality;
    }
}
