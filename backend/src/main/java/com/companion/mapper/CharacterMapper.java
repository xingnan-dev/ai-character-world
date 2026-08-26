package com.companion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.companion.entity.AiCharacter;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CharacterMapper extends BaseMapper<AiCharacter> {
}
