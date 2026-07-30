package com.companion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_avatar")
public class Avatar {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private Integer type;

    private String baseModel;

    private String modelUrl;

    @TableField("appearance_config")
    private String appearanceConfig;

    @TableField("personality_id")
    private Long personalityId;

    private String slogan;

    @TableLogic
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
