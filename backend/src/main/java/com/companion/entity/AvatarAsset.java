package com.companion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_avatar_asset")
public class AvatarAsset {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Integer assetType;

    private String fileUrl;

    private Long fileSize;

    private String thumbnailUrl;

    private Integer gender;

    private String styleTags;

    private String colorTags;

    @TableField("supported_attributes")
    private String supportedAttributes;

    private String description;

    private String license;

    private String source;

    private Integer isOfficial;

    private Integer downloadCount;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
