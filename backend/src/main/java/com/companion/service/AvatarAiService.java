package com.companion.service;

import com.companion.dto.request.AvatarGenerateRequest;
import com.companion.dto.response.AvatarGenerateResponse;

public interface AvatarAiService {

    AvatarGenerateResponse generateAvatar(Long userId, AvatarGenerateRequest request);
}
