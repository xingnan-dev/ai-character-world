package com.companion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvatarGenerateRequest {

    @NotBlank(message = "描述不能为空")
    @Size(max = 1000, message = "描述不超过1000字符")
    private String description;

    private Boolean createPersonality = true;

    private Boolean useTemplate = false;

    private Long templateId;
}
