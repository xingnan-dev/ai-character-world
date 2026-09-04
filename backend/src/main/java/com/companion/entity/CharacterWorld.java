package com.companion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_world")
public class CharacterWorld {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ownerUserId;
    private Long userCharacterId;
    private String name;
    private String background;
    private String rules;
    private String atmosphere;
    private String scene;
    private String sourceDescription;
    private Integer status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
