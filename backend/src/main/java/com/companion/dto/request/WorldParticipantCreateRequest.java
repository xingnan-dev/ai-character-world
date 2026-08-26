package com.companion.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorldParticipantCreateRequest {
    @NotNull(message = "characterId不能为空")
    private Long characterId;

    @NotBlank(message = "participantType不能为空")
    private String participantType;

    @NotNull(message = "displayOrder不能为空")
    @Min(value = 0, message = "displayOrder不能小于0")
    @Max(value = 9999, message = "displayOrder不能大于9999")
    private Integer displayOrder;
}
