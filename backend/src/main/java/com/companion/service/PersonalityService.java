package com.companion.service;

import com.companion.dto.request.PersonalityCreateRequest;
import com.companion.dto.response.PersonalityVO;

import java.util.List;

public interface PersonalityService {

    PersonalityVO createPersonality(Long userId, Long avatarId, PersonalityCreateRequest request);

    PersonalityVO getPersonalityByAvatarId(Long userId, Long avatarId);

    PersonalityVO updatePersonality(Long userId, Long avatarId, PersonalityCreateRequest request);

    List<PersonalityVO> getTemplateList();
}
