package com.companion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvatarCreateRequest {

    @NotBlank
    private String name;

    @NotNull
    private Integer type;

    private String baseModel;

    private String modelUrl;

    private String appearanceConfig;

    private String slogan;

    private Long personalityId;
}