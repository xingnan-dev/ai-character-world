package com.companion.character.model;

import lombok.Data;

@Data
public class CharacterDraft {

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
}
