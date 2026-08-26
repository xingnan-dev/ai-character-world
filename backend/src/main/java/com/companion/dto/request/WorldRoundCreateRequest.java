package com.companion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WorldRoundCreateRequest {
    @NotBlank(message = "requestId不能为空")
    @Size(max = 64, message = "requestId不能超过64字符")
    private String requestId;

    @NotBlank(message = "用户输入不能为空")
    @Size(max = 4000, message = "用户输入不能超过4000字符")
    private String userInput;
}
