package com.companion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_world_participant")
public class WorldParticipant {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long worldId;
    private Integer participantType;
    private Long sourceCharacterId;
    private String characterSnapshot;
    private Integer displayOrder;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
}
