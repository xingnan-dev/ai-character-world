package com.companion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.companion.entity.CharacterRelationship;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CharacterRelationshipMapper extends BaseMapper<CharacterRelationship> {

    @Update("UPDATE t_character_relationship SET stage=#{relationship.stage}, "
            + "summary=#{relationship.summary}, interaction_style=#{relationship.interactionStyle}, "
            + "recent_change=#{relationship.recentChange}, update_time=#{relationship.updateTime}, "
            + "version=version+1 WHERE id=#{relationship.id} AND deleted=0 AND version=#{expectedVersion}")
    int updateIfVersionMatches(@Param("relationship") CharacterRelationship relationship,
                               @Param("expectedVersion") Integer expectedVersion);
}
