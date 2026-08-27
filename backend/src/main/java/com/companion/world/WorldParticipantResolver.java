package com.companion.world;

import java.util.List;

public interface WorldParticipantResolver {
    List<WorldActorContext> resolveAiParticipants(Long worldId);
}
