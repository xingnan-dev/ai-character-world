package com.companion.agent;

import com.companion.entity.AgentStep;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

final class AgentToolCallIdentity {

    private final ObjectMapper mapper;

    AgentToolCallIdentity(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    Optional<AgentStep> completedDuplicate(AgentStep candidate, List<AgentStep> runSteps) {
        if (candidate == null || !"TOOL_CALL".equals(candidate.getDecisionType())) {
            return Optional.empty();
        }
        String candidateKey = key(candidate.getToolName(), candidate.getToolArguments());
        return runSteps.stream()
                .filter(step -> !step.getId().equals(candidate.getId()))
                .filter(step -> "COMPLETED".equals(step.getStatus()))
                .filter(step -> "TOOL_CALL".equals(step.getDecisionType()))
                .filter(step -> candidateKey.equals(key(step.getToolName(), step.getToolArguments())))
                .findFirst();
    }

    String key(String toolName, String arguments) {
        try {
            JsonNode parsed = mapper.readTree(arguments);
            if (parsed != null && parsed.isTextual()) {
                parsed = mapper.readTree(parsed.asText());
            }
            return toolName + "\n" + mapper.writeValueAsString(canonical(parsed));
        } catch (Exception exception) {
            throw new AgentExecutionException(
                    "INVALID_TOOL_ARGUMENTS", "Persisted tool arguments are invalid", false, exception);
        }
    }

    private JsonNode canonical(JsonNode node) {
        if (node == null || node.isNull() || node.isValueNode()) {
            return node;
        }
        if (node.isArray()) {
            ArrayNode result = mapper.createArrayNode();
            node.forEach(value -> result.add(canonical(value)));
            return result;
        }
        ObjectNode result = mapper.createObjectNode();
        StreamSupport.stream(((Iterable<String>) node::fieldNames).spliterator(), false)
                .sorted(Comparator.naturalOrder())
                .forEach(name -> result.set(name, canonical(node.get(name))));
        return result;
    }
}
