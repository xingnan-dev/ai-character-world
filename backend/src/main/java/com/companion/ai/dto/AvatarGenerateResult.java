package com.companion.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvatarGenerateResult {

    private String name;

    private AppearanceConfig appearanceConfig;

    private PersonalityConfig personality;

    private List<String> tags;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppearanceConfig {
        private String gender;
        private String hairColor;
        private String hairStyle;
        private String eyeColor;
        private String bodyType;
        private String earType;
        private Boolean hasWing;
        private String wingType;
        private String outfitStyle;
        private String outfitColor;
        private Boolean hasAccessory;
        private List<String> accessoryType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonalityConfig {
        private String type;
        private List<String> traits;
        private String speakingStyle;
        private String slogan;
    }
}
