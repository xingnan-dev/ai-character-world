package com.companion.dto.response;

import com.companion.character.model.CharacterProfile;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CharacterResponse {
    private Long id;
    private String characterType;
    private String name;
    private Integer age;
    private String identity;
    private String corePersonality;
    private String currentGoal;
    private String biography;
    private String relationshipToUser;
    private String speakingStyle;
    private CharacterProfile profile;
    private String sourceDescription;
    private Integer generateType;
    private String visualType;
    private Long avatarId;
    private String imageUrl;
    private String avatarColor;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
