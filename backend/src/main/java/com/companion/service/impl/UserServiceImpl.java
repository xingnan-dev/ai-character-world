package com.companion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.common.exception.BusinessException;
import com.companion.common.result.ResultCode;
import com.companion.common.utils.JwtUtils;
import com.companion.dto.request.LoginRequest;
import com.companion.dto.request.RegisterRequest;
import com.companion.dto.response.LoginVO;
import com.companion.dto.response.UserVO;
import com.companion.entity.User;
import com.companion.mapper.UserMapper;
import com.companion.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    /**
     * 内存Token存储（临时方案，后续可替换为Redis）
     */
    private static final Map<String, Long> TOKEN_CACHE = new ConcurrentHashMap<>();

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

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
        user.setPassword(PASSWORD_ENCODER.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setStatus(1);
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

        if (!PASSWORD_ENCODER.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户名或密码错误");
        }

        String token = JwtUtils.generateToken(user.getId(), user.getUsername());
        // 临时使用内存存储Token
        TOKEN_CACHE.put(token, user.getId());

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUser(convertToVO(user));
        log.info("用户登录成功: {}", request.getUsername());
        return loginVO;
    }

    /**
     * 验证Token是否有效（临时方案）
     */
    public static boolean validateToken(String token) {
        return TOKEN_CACHE.containsKey(token);
    }

    /**
     * 移除Token（退出登录）
     */
    public static void removeToken(String token) {
        TOKEN_CACHE.remove(token);
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
