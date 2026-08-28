package com.companion.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorldExecutionDispatchResponse {
    private String dispatchStatus;
    private WorldRoundResponse round;
}
