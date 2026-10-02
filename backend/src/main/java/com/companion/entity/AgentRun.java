package com.companion.entity;
import com.baomidou.mybatisplus.annotation.*;import lombok.Data;import java.time.LocalDateTime;
@Data @TableName("t_agent_run") public class AgentRun {
 @TableId(type=IdType.AUTO) private Long id; private Long userId; private String requestId; private String goal;
 private String status; private Integer currentStep; private Integer maxSteps; private String finalResult;
 private String lastErrorCode; private String lastErrorMessage; private Integer version; private Integer executionVersion;
 private LocalDateTime createTime; private LocalDateTime updateTime; private LocalDateTime completionTime;
}
