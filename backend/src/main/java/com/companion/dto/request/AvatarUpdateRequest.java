package com.companion.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvatarUpdateRequest {

    @NotNull
    private Long id;

    private String name;

    private String appearanceConfig;
}