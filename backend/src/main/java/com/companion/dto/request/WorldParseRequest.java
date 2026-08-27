package com.companion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WorldParseRequest {
    @NotBlank(message = "description不能为空")
    @Size(max = 2000, message = "description不能超过2000字符")
    private String description;
}
