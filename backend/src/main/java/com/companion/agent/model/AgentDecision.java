package com.companion.agent.model;
import com.fasterxml.jackson.databind.JsonNode;
public record AgentDecision(String type,String decisionSummary,String toolName,JsonNode arguments,String finalResult) {}
