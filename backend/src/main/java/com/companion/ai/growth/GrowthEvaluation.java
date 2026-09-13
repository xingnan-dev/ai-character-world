package com.companion.ai.growth;
public record GrowthEvaluation(boolean changed,String growthSummary,String behaviorAdaptation,String userUnderstanding,String growthDirection){ public static GrowthEvaluation unchanged(){return new GrowthEvaluation(false,null,null,null,null);} }
