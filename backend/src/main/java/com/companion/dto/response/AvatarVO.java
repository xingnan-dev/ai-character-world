package com.companion.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvatarVO {

    private Long id;

    private Long userId;

    private String name;

    private Integer type;

    private Integer gender;

    private String baseModel;

    private String modelUrl;

    private String thumbnailUrl;

    private String appearanceConfig;

    private Long personalityId;

    private String slogan;

    private String sourceDescription;

    private Integer generateType;

    private String createTime;

    private PersonalityVO personality;
}