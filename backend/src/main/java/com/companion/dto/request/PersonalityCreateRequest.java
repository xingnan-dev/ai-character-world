package com.companion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalityCreateRequest {

    @NotNull
    private Long avatarId;

    @NotBlank
    private String name;

    private Integer templateType;

    @NotBlank
    private String corePersonality;

    @NotBlank
    private String identity;

    @NotBlank
    private String languageStyle;

    private String hobbies;

    private String relationship;
}