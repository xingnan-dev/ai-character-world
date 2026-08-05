package com.companion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_avatar_template")
public class AvatarTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String category;

    private String description;

    @TableField("avatar_config")
    private String avatarConfig;

    @TableField("attributes_config")
    private String attributesConfig;

    @TableField("personality_config")
    private String personalityConfig;

    private Long assetId;

    private String thumbnailUrl;

    private String previewPrompt;

    private Integer isOfficial;

    private Long creatorId;

    private Integer useCount;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
