package com.companion.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;import com.companion.entity.AgentRun;import org.apache.ibatis.annotations.Mapper;import org.apache.ibatis.annotations.Param;import org.apache.ibatis.annotations.Select;
@Mapper public interface AgentRunMapper extends BaseMapper<AgentRun> {
 @Select("SELECT * FROM t_agent_run WHERE id=#{id} AND user_id=#{userId} AND status='RUNNING' AND execution_version=#{executionVersion} FOR UPDATE")
 AgentRun lockOwnedExecution(@Param("id") Long id,@Param("userId") Long userId,@Param("executionVersion") int executionVersion);
}
