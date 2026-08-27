package com.companion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.companion.entity.CharacterWorld;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CharacterWorldMapper extends BaseMapper<CharacterWorld> {
    @Select("""
            SELECT id, owner_user_id, name, background, rules, atmosphere, scene,
                   source_description, status, deleted, create_time, update_time
            FROM t_world
            WHERE id = #{worldId} AND owner_user_id = #{userId} AND status = 1 AND deleted = 0
            FOR UPDATE
            """)
    CharacterWorld selectOwnedForUpdate(@Param("userId") Long userId, @Param("worldId") Long worldId);
}
