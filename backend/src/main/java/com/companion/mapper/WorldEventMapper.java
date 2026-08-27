package com.companion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.companion.entity.WorldEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface WorldEventMapper extends BaseMapper<WorldEvent> {

    @Insert("""
            INSERT INTO t_world_event
                (round_id, sequence_no, participant_id, event_type, content, status,
                 error_code, completion_time, create_time)
            SELECT
                #{roundId}, #{sequenceNo}, #{participantId}, #{eventType}, #{content}, #{status},
                #{errorCode}, #{completionTime}, #{createTime}
            FROM t_world_round round_state
            WHERE round_state.id = #{roundId}
              AND round_state.world_id = #{worldId}
              AND round_state.status = 'RUNNING'
              AND round_state.execution_version = #{executionVersion}
              AND round_state.lease_until IS NOT NULL
              AND round_state.lease_until > CURRENT_TIMESTAMP
              AND (SELECT COUNT(*)
                   FROM t_world_event preceding
                   WHERE preceding.round_id = round_state.id
                     AND preceding.sequence_no < #{sequenceNo}) = #{sequenceNo} - 1
              AND NOT EXISTS (
                   SELECT 1
                   FROM t_world_event unfinished
                   WHERE unfinished.round_id = round_state.id
                     AND unfinished.sequence_no < #{sequenceNo}
                     AND unfinished.status NOT IN ('COMPLETED', 'FAILED'))
            """)
    int insertIfExecutionOwned(@Param("worldId") Long worldId,
                               @Param("roundId") Long roundId,
                               @Param("executionVersion") long executionVersion,
                               @Param("sequenceNo") int sequenceNo,
                               @Param("participantId") Long participantId,
                               @Param("eventType") String eventType,
                               @Param("content") String content,
                               @Param("status") String status,
                               @Param("errorCode") String errorCode,
                               @Param("completionTime") LocalDateTime completionTime,
                               @Param("createTime") LocalDateTime createTime);
}
