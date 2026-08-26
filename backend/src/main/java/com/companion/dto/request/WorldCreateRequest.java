package com.companion.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class WorldCreateRequest {
    @NotBlank(message = "世界名称不能为空")
    @Size(max = 100, message = "世界名称不能超过100字符")
    private String name;

    @Size(max = 2000, message = "世界背景不能超过2000字符")
    private String background;

    @Size(max = 2000, message = "世界规则不能超过2000字符")
    private String rules;

    @NotEmpty(message = "至少需要一个参与者")
    @Size(max = 20, message = "一个世界最多20个参与者")
    @Valid
    private List<@NotNull(message = "参与者不能为空") WorldParticipantCreateRequest> participants;
}
