package com.companion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_personality")
public class Personality {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long avatarId;

    private String name;

    private Integer templateType;

    private String corePersonality;

    private String identity;

    private String languageStyle;

    private String hobbies;

    private String relationship;

    private String systemPrompt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
