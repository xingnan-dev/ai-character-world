package com.companion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_world_event")
public class WorldEvent {
    @TableId(type = IdType.AUTO)
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
