package com.companion.avatar.generation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AvatarAppearanceConfig {

    private String gender;
    private String hairColor;
    private String hairStyle;
    private String eyeColor;
    private String bodyType;
    private String outfitStyle;
    private String outfitColor;
    private String earType;
    private String wingType;

    @JsonAlias("accessoryType")
    private List<String> accessories;
}
