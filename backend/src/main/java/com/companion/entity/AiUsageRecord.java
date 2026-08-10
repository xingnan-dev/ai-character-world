package com.companion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_ai_usage")
public class AiUsageRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String llmRequestId;

    private Long chatMessageId;

    private String provider;

    private String model;

    private Long userId;

    private Long sessionId;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private Long latencyMs;

    private Integer success;

    private String errorCode;

    private LocalDateTime createdTime;
}
