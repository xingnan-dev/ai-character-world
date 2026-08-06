package com.companion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.companion.entity.AuthSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuthSessionMapper extends BaseMapper<AuthSession> {

    @Select("""
            SELECT COUNT(1)
            FROM t_auth_session session
            INNER JOIN t_user user_account ON user_account.id = session.user_id
            WHERE session.jti = #{tokenId}
              AND session.user_id = #{userId}
              AND session.revoked_at IS NULL
              AND session.expires_at > CURRENT_TIMESTAMP
              AND user_account.status = 1
              AND user_account.deleted = 0
            """)
    long countActiveForEnabledUser(@Param("userId") Long userId,
                                   @Param("tokenId") String tokenId);
}
