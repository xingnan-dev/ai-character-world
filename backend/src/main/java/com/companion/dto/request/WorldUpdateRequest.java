package com.companion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WorldUpdateRequest {
    @NotBlank(message = "世界名称不能为空")
    @Size(max = 100, message = "世界名称不能超过100字符")
    private String name;
    @Size(max = 2000, message = "世界背景不能超过2000字符")
    private String background;
    @Size(max = 2000, message = "世界规则不能超过2000字符")
    private String rules;
    @Size(max = 500, message = "世界氛围不能超过500字符")
    private String atmosphere;
    @Size(max = 1000, message = "世界场景不能超过1000字符")
    private String scene;
    @Size(max = 2000, message = "原始描述不能超过2000字符")
    private String sourceDescription;
}
