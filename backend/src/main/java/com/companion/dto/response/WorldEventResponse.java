package com.companion.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorldEventResponse {
    private Long id;
    private Long roundId;
    private Integer sequenceNo;
    private Long participantId;
    private String eventType;
    private String content;
    private String status;
    private String errorCode;
    private LocalDateTime completionTime;
    private LocalDateTime createTime;
}
