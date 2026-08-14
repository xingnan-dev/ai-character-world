package com.companion.ai.dto;

import com.companion.avatar.generation.AvatarAppearanceConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AvatarGenerateResult {

    private String name;

    private AvatarAppearanceConfig appearanceConfig;

    private PersonalityConfig personality;

    private List<String> tags;

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
