package com.companion.chat;

import com.companion.chat.model.PersonalitySnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class PersonalitySnapshotCodec {

    private final ObjectMapper objectMapper;

    public PersonalitySnapshotCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String encode(PersonalitySnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("Personality snapshot is required");
        }
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to encode personality snapshot", e);
        }
    }

    public PersonalitySnapshot decode(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Personality snapshot JSON is required");
        }
        try {
            return objectMapper.readValue(json, PersonalitySnapshot.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid personality snapshot JSON", e);
        }
    }
}
