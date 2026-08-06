package com.companion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.common.exception.BusinessException;
import com.companion.common.result.ResultCode;
import com.companion.dto.request.ChangePasswordRequest;
import com.companion.entity.enums.UserStatus;
import com.companion.security.JwtTokenService;
import com.companion.security.IssuedAccessToken;
import com.companion.security.TokenSessionService;
import com.companion.dto.request.LoginRequest;
import com.companion.dto.request.RegisterRequest;
import com.companion.dto.response.LoginVO;
import com.companion.dto.response.UserVO;
import com.companion.entity.User;
import com.companion.mapper.UserMapper;
import com.companion.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtTokenService jwtTokenService;
    private final TokenSessionService tokenSessionService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void register(RegisterRequest request) {
        Long count = userMapper.selectCount(
                new QueryWrapper<User>().eq("username", request.getUsername())
        );
        if (count > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setStatus(UserStatus.ACTIVE.getCode());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        log.info("用户注册成功: {}", request.getUsername());
    }

    @Override
    public LoginVO login(LoginRequest request) {
        User user = userMapper.selectOne(
                new QueryWrapper<User>().eq("username", request.getUsername())
        );
        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户名或密码错误");
        }

        if (!UserStatus.isActive(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "Account is disabled or locked");
        }

        IssuedAccessToken token = jwtTokenService.issueAccessToken(user.getId(), user.getUsername());
        tokenSessionService.register(user.getId(), token);

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token.value());
        loginVO.setUser(convertToVO(user));
        log.info("用户登录成功: {}", request.getUsername());
        return loginVO;
    }

    @Override
    public void logout(Long userId, String tokenId) {
        tokenSessionService.revoke(userId, tokenId);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null || !UserStatus.isActive(user.getStatus())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "New password must differ from current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        tokenSessionService.revokeAll(userId);
        log.info("User password changed and all sessions revoked: userId={}", userId);
    }

    @Override
    public UserVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return convertToVO(user);
    }

    @Override
    public UserVO updateUserInfo(Long userId, String nickname, String avatarUrl) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        if (nickname != null) {
            user.setNickname(nickname);
        }
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        return convertToVO(user);
    }

    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setCreateTime(user.getCreateTime() != null ? user.getCreateTime().toString() : null);
        return vo;
    }
}
