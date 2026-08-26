package com.companion.character.snapshot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class CharacterSnapshotJsonMapper {

    private final ObjectMapper objectMapper;

    public CharacterSnapshotJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public String write(CharacterSnapshot snapshot) {
        if (snapshot == null) {
            throw new CharacterSnapshotException("Character snapshot is required");
        }
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException error) {
            throw new CharacterSnapshotException("Unable to serialize character snapshot", error);
        }
    }

    public CharacterSnapshot read(String json) {
        if (json == null || json.isBlank()) {
            throw new CharacterSnapshotException("Character snapshot JSON is required");
        }
        try {
            return objectMapper.readValue(json, CharacterSnapshot.class);
        } catch (JsonProcessingException | RuntimeException error) {
            throw new CharacterSnapshotException("Invalid character snapshot JSON", error);
        }
    }
}
