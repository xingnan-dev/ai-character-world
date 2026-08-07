package com.companion.chat;

import com.companion.chat.model.PersonalitySnapshot;
import com.companion.entity.Avatar;
import com.companion.entity.ChatSession;
import com.companion.entity.Personality;

public interface SessionPersonalityResolver {

    PersonalitySnapshot resolveForNewSession(Long userId, Avatar avatar);

    String encode(PersonalitySnapshot snapshot);

    Personality resolveFromSession(ChatSession session);
}
