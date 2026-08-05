package com.companion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_avatar_attribute")
public class AvatarAttribute {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long avatarId;

    private String category;

    private String attrKey;

    private String attrValue;

    @TableField("attr_metadata")
    private String attrMetadata;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
