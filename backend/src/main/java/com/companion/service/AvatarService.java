package com.companion.service;

import com.companion.dto.request.AvatarCreateRequest;
import com.companion.dto.request.AvatarUpdateRequest;
import com.companion.dto.response.AvatarVO;

import java.util.List;

public interface AvatarService {

    AvatarVO createAvatar(Long userId, AvatarCreateRequest request);

    AvatarVO getAvatarById(Long userId, Long id);

    List<AvatarVO> getAvatarList(Long userId);

    AvatarVO updateAvatar(Long userId, AvatarUpdateRequest request);

    void deleteAvatar(Long userId, Long id);
}
