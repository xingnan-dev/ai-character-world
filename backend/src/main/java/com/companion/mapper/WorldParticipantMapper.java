package com.companion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.companion.entity.WorldParticipant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WorldParticipantMapper extends BaseMapper<WorldParticipant> {
    @Delete("DELETE FROM t_world_participant WHERE world_id = #{worldId}")
    int deletePhysicallyByWorldId(@Param("worldId") Long worldId);
}
