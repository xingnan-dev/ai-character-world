package com.companion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSendRequest {

    @NotNull
    private Long sessionId;

    @NotBlank
    private String content;

    private String requestId;

    public ChatSendRequest(Long sessionId, String content) {
        this.sessionId = sessionId;
        this.content = content;
    }
}
