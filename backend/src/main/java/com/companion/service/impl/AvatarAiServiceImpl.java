package com.companion.service.impl;

import com.companion.avatar.generation.AvatarGenerationCoordinator;
import com.companion.dto.request.AvatarGenerateRequest;
import com.companion.dto.response.AvatarGenerateResponse;
import com.companion.service.AvatarAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AvatarAiServiceImpl implements AvatarAiService {

    private final AvatarGenerationCoordinator coordinator;

    @Override
    public AvatarGenerateResponse generateAvatar(Long userId, AvatarGenerateRequest request) {
        // AI-generated avatars require a bound personality because ChatSession snapshots depend on it.
        // createPersonality remains accepted for API compatibility but does not disable this invariant.
        return coordinator.generate(userId, request);
    }
}
