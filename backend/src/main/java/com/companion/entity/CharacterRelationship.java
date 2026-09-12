package com.companion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_character_relationship")
public class CharacterRelationship {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long characterId;
    private String stage;
    private String summary;
    private String interactionStyle;
    private String recentChange;
    @TableLogic
    private Integer deleted;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
