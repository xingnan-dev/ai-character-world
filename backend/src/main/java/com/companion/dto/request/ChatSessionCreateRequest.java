package com.companion.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionCreateRequest {

    public ChatSessionCreateRequest(Long avatarId, String title) {
        this.avatarId = avatarId;
        this.title = title;
    }

    private Long avatarId;

    private Long characterId;

    private String title;
}
