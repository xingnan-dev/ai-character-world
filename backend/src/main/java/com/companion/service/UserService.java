package com.companion.service;

import com.companion.dto.request.ChangePasswordRequest;
import com.companion.dto.request.LoginRequest;
import com.companion.dto.request.RegisterRequest;
import com.companion.dto.response.LoginVO;
import com.companion.dto.response.UserVO;

public interface UserService {

    void register(RegisterRequest request);

    LoginVO login(LoginRequest request);

    void logout(Long userId, String tokenId);

    void changePassword(Long userId, ChangePasswordRequest request);

    UserVO getUserInfo(Long userId);

    UserVO updateUserInfo(Long userId, String nickname, String avatarUrl);

    UserVO setCurrentUserCharacter(Long userId, Long characterId);
}
