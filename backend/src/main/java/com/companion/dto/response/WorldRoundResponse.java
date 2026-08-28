package com.companion.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorldRoundResponse {
    private Long id;
    private Long worldId;
    private String requestId;
    private String userInput;
    private String status;
    private String errorCode;
    private boolean executionRecoverable;
    private LocalDateTime startedTime;
    private LocalDateTime completionTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
