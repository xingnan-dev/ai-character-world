package com.companion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user_memory")
public class UserMemory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer category;

    private String memoryKey;

    private String value;

    private Float importance;

    private LocalDateTime lastAccessTime;

    @TableLogic
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
