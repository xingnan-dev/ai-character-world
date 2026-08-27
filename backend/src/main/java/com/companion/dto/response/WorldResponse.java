package com.companion.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorldResponse {
    private Long id;
    private String name;
    private String background;
    private String rules;
    private String atmosphere;
    private String scene;
    private String sourceDescription;
    private Integer status;
    private boolean participantsLocked;
    private List<WorldParticipantResponse> participants;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
