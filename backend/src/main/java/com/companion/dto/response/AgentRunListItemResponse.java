package com.companion.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AgentRunListItemResponse {
    private Long id;
    private String requestId;
    private String goal;
    private String status;
    private Integer currentStep;
    private Integer maxSteps;
    private String lastErrorCode;
    private String lastErrorMessage;
    private Boolean canResume;
    private String recoveryState;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime completionTime;
}
