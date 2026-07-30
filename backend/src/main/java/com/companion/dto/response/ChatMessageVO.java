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

    private Integer role;

    private String content;

    private String emotion;

    private String createTime;
}