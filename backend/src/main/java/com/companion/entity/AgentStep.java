package com.companion.entity;
import com.baomidou.mybatisplus.annotation.*;import lombok.Data;import java.time.LocalDateTime;
@Data @TableName("t_agent_step") public class AgentStep {
 @TableId(type=IdType.AUTO) private Long id; private Long runId; private Integer stepNumber; private String decisionType;
 private String decisionSummary; private String toolCallId; private String toolName; private String toolArguments;
 private String toolResult; private String status; private Integer retryCount; private Integer toolAttemptCount; private String errorCode; private String errorMessage;
 private LocalDateTime createTime; private LocalDateTime updateTime; private LocalDateTime completionTime;
}
