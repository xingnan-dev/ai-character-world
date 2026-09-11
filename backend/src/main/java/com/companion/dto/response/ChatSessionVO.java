package com.companion.dto.response;

import com.companion.character.snapshot.CharacterSnapshot;
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

    private Long characterId;

    private String characterType;

    private String characterName;

    private String imageUrl;

    private String avatarColor;

    private String visualType;

    private String identity;

    private String corePersonality;

    private String currentGoal;

    private String biography;

    private String relationshipToUser;

    private String speakingStyle;

    private CharacterSnapshot.Profile profile;

    private CharacterSnapshot userCharacter;

    private String title;

    private String avatarName;

    private String createTime;

    private List<ChatMessageVO> messages;
}
