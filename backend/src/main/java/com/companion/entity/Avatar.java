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

    private Integer gender;

    private String baseModel;

    private String modelUrl;

    @TableField("thumbnail_url")
    private String thumbnailUrl;

    @TableField("appearance_config")
    private String appearanceConfig;

    @TableField("personality_id")
    private Long personalityId;

    private String slogan;

    @TableField("source_description")
    private String sourceDescription;

    @TableField("generate_type")
    private Integer generateType;

    @TableField("generate_result")
    private String generateResult;

    @TableField("template_id")
    private Long templateId;

    private Integer status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
