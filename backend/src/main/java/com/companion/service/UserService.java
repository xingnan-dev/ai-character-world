package com.companion.service;

import com.companion.dto.request.LoginRequest;
import com.companion.dto.request.RegisterRequest;
import com.companion.dto.response.LoginVO;
import com.companion.dto.response.UserVO;

public interface UserService {

    void register(RegisterRequest request);

    LoginVO login(LoginRequest request);

    UserVO getUserInfo(Long userId);

    UserVO updateUserInfo(Long userId, String nickname, String avatarUrl);
}