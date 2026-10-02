package com.companion.dto.response;
import lombok.Data;import java.time.LocalDateTime;
@Data public class AgentStepResponse {private Long id;private Integer stepNumber;private String decisionType;private String decisionSummary;private String toolCallId;private String toolName;private String toolArguments;private String toolResult;private String status;private Integer retryCount;private Integer toolAttemptCount;private String errorCode;private String errorMessage;private LocalDateTime createTime;private LocalDateTime updateTime;private LocalDateTime completionTime;}
