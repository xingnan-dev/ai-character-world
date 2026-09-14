package com.companion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_world_round")
public class WorldRound {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long worldId;
    private String requestId;
    private String userInput;
    private String worldSnapshot;
    private Integer worldSnapshotVersion;
    private Long userCharacterId;
    private String userCharacterSnapshot;
    private Integer userCharacterSnapshotVersion;
    private String status;
    private String errorCode;
    private LocalDateTime startedTime;
    private LocalDateTime completionTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long executionVersion;
    private LocalDateTime leaseUntil;
}
