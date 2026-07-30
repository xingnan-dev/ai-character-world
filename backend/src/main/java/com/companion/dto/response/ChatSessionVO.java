package com.companion.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionVO {

    private Long id;

    private Long userId;

    private Long avatarId;

    private String title;

    private String avatarName;

    private String createTime;

    private List<ChatMessageVO> messages;
}