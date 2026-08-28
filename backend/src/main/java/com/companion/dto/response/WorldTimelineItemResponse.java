package com.companion.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class WorldTimelineItemResponse {
    private WorldRoundResponse round;
    private List<WorldEventResponse> events;
}
