package com.companion.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class WorldTimelinePageResponse {
    private List<WorldTimelineItemResponse> items;
    private Long nextBeforeRoundId;
    private boolean hasMore;
}
