package com.companion.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalityVO {

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
}