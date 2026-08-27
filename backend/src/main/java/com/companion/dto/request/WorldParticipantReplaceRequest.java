package com.companion.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class WorldParticipantReplaceRequest {
    @NotNull(message = "participants不能为空")
    @Size(min = 2, max = 4, message = "一个世界必须选择2到4个AI角色")
    @Valid
    private List<@NotNull(message = "参与者不能为空") WorldParticipantCreateRequest> participants;
}
