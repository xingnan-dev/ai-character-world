package com.companion.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageVO {

    private Long id;

    private Long sessionId;

    private String requestId;

    private Integer role;

    private String content;

    private String emotion;

    private Integer status;

    private String errorCode;

    private String errorMessage;

    private String createTime;
}
