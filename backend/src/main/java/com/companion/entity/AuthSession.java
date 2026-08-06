package com.companion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_auth_session")
public class AuthSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String jti;

    private Long userId;

    private LocalDateTime expiresAt;

    private LocalDateTime revokedAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
