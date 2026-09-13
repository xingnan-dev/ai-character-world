package com.companion.service;
import com.companion.ai.growth.GrowthEvaluationContext;
import com.companion.entity.CharacterGrowth;
import java.util.Optional;
public interface CharacterGrowthService { CharacterGrowth getOrCreate(Long userId,Long characterId); Optional<CharacterGrowth> get(Long userId, Long characterId); CharacterGrowth evaluateAndUpdate(Long userId,Long characterId,GrowthEvaluationContext context); }
