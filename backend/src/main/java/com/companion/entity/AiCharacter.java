package com.companion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_character")
public class AiCharacter {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer characterType;
    private String name;
    private Integer age;
    private String identity;
    private String corePersonality;
    private String currentGoal;
    private String biography;
    private String relationshipToUser;
    private String speakingStyle;
    private String profileConfig;
    private String sourceDescription;
    private Integer generateType;
    private Integer visualType;
    private Long avatarId;
    private String imageUrl;
    private String avatarColor;
    private Integer status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
