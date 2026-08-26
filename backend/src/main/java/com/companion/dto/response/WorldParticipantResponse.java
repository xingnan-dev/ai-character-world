package com.companion.dto.response;

import com.companion.character.snapshot.CharacterSnapshot;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorldParticipantResponse {
    private Long id;
    private String participantType;
    private Long sourceCharacterId;
    private Integer displayOrder;
    private CharacterSnapshot character;
    private LocalDateTime createTime;
}
