package com.companion.character.snapshot;

import com.companion.character.model.CharacterProfile;
import com.companion.dto.response.CharacterResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public record CharacterSnapshot(
        int snapshotVersion,
        Long sourceCharacterId,
        String characterType,
        String name,
        Integer age,
        String identity,
        String corePersonality,
        String currentGoal,
        String biography,
        String relationshipToUser,
        String speakingStyle,
        Profile profile,
        String visualType,
        Long avatarId,
        String imageUrl,
        String avatarColor
) {
    public static final int CURRENT_VERSION = 1;
    private static final Set<String> CHARACTER_TYPES = Set.of("AI", "USER");
    private static final Set<String> VISUAL_TYPES = Set.of("INITIAL", "VRM", "IMAGE");

    public CharacterSnapshot {
        if (snapshotVersion != CURRENT_VERSION) {
            throw new CharacterSnapshotException("Unsupported character snapshot version: " + snapshotVersion);
        }
        if (sourceCharacterId == null || sourceCharacterId <= 0) {
            throw new CharacterSnapshotException("Character snapshot source is required");
        }
        characterType = allowed(characterType, "Character snapshot type is required", CHARACTER_TYPES);
        name = required(name, "Character snapshot name is required");
        visualType = allowed(visualType, "Character snapshot visual type is required", VISUAL_TYPES);
        profile = profile == null ? Profile.empty() : profile.copy();
    }

    public static CharacterSnapshot from(CharacterResponse character) {
        if (character == null) {
            throw new CharacterSnapshotException("Character is required");
        }
        return new CharacterSnapshot(
                CURRENT_VERSION,
                character.getId(),
                character.getCharacterType(),
                character.getName(),
                character.getAge(),
                character.getIdentity(),
                character.getCorePersonality(),
                character.getCurrentGoal(),
                character.getBiography(),
                character.getRelationshipToUser(),
                character.getSpeakingStyle(),
                Profile.from(character.getProfile()),
                character.getVisualType(),
                character.getAvatarId(),
                character.getImageUrl(),
                character.getAvatarColor()
        );
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new CharacterSnapshotException(message);
        }
        return value.trim();
    }

    private static String allowed(String value, String message, Set<String> allowedValues) {
        String requiredValue = required(value, message);
        if (!allowedValues.contains(requiredValue)) {
            throw new CharacterSnapshotException("Invalid character snapshot value");
        }
        return requiredValue;
    }

    public record Profile(
            List<String> values,
            List<String> likes,
            List<String> dislikes,
            List<String> interests,
            List<String> fears,
            List<String> secrets,
            List<String> behaviorTendencies
    ) {
        public Profile {
            values = immutable(values);
            likes = immutable(likes);
            dislikes = immutable(dislikes);
            interests = immutable(interests);
            fears = immutable(fears);
            secrets = immutable(secrets);
            behaviorTendencies = immutable(behaviorTendencies);
        }

        public static Profile from(CharacterProfile source) {
            if (source == null) return empty();
            return new Profile(
                    source.getValues(), source.getLikes(), source.getDislikes(), source.getInterests(),
                    source.getFears(), source.getSecrets(), source.getBehaviorTendencies()
            );
        }

        public static Profile empty() {
            return new Profile(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        }

        private Profile copy() {
            return new Profile(values, likes, dislikes, interests, fears, secrets, behaviorTendencies);
        }

        private static List<String> immutable(List<String> source) {
            if (source == null) {
                return List.of();
            }
            if (source.size() > 10) {
                throw new CharacterSnapshotException("Invalid character snapshot profile");
            }
            List<String> copy = new ArrayList<>(source.size());
            for (String value : source) {
                if (value == null || value.isBlank() || value.length() > 200) {
                    throw new CharacterSnapshotException("Invalid character snapshot profile");
                }
                copy.add(value);
            }
            return List.copyOf(copy);
        }
    }
}
