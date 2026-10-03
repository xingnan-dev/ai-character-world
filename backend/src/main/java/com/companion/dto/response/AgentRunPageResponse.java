package com.companion.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class AgentRunPageResponse {
    private List<AgentRunListItemResponse> items;
    private int page;
    private int pageSize;
    private long total;
}
