package com.companion.dto.response;
import lombok.Data;import java.time.LocalDateTime;import java.util.List;
@Data public class AgentRunResponse {private Long id;private String requestId;private String goal;private String status;private Integer currentStep;private Integer maxSteps;private String finalResult;private String lastErrorCode;private String lastErrorMessage;private Integer version;private Integer executionVersion;private LocalDateTime createTime;private LocalDateTime updateTime;private LocalDateTime completionTime;private List<AgentStepResponse> steps;}
