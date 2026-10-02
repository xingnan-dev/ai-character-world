package com.companion.agent.tool;
import com.fasterxml.jackson.databind.JsonNode;
public interface AgentTool { String name();String description();String inputSchema();void validate(JsonNode arguments);String execute(Long userId,JsonNode arguments); }
