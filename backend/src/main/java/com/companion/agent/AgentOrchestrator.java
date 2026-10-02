package com.companion.agent;

import com.companion.agent.model.AgentDecision;
import com.companion.agent.tool.AgentToolExecutor;
import com.companion.agent.tool.AgentToolRegistry;
import com.companion.entity.AgentRun;
import com.companion.entity.AgentStep;
import com.companion.entity.enums.AgentRunStatus;
import com.companion.entity.enums.AgentStepStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class AgentOrchestrator {
    private final AgentRunLifecycleService lifecycle;
    private final AgentDecisionClient decisions;
    private final AgentToolRegistry registry;
    private final AgentToolExecutor tools;
    private final ObjectMapper mapper;
    private final AgentToolCallIdentity toolCallIdentity;

    public AgentOrchestrator(AgentRunLifecycleService lifecycle, AgentDecisionClient decisions,
                             AgentToolRegistry registry, AgentToolExecutor tools, ObjectMapper mapper) {
        this.lifecycle = lifecycle; this.decisions = decisions; this.registry = registry; this.tools = tools; this.mapper = mapper;
        this.toolCallIdentity = new AgentToolCallIdentity(mapper);
    }

    public void execute(Long userId, Long runId) {
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Agent loop must run outside a transaction");
        AgentRunLifecycleService.ExecutionClaim claim = lifecycle.claim(userId, runId);
        if (claim == null) return;
        int executionVersion = claim.executionVersion();
        AgentRun run = lifecycle.requireOwned(userId, runId);
        AgentStep step = null;
        try {
            while (run.getCurrentStep() < run.getMaxSteps()) {
                lifecycle.assertExecutionOwner(userId, runId, executionVersion);
                int number = run.getCurrentStep() + 1;
                step = lifecycle.getOrCreateStep(userId, runId, executionVersion, number);
                if (AgentStepStatus.COMPLETED.name().equals(step.getStatus())) {
                    run = lifecycle.requireOwned(userId, runId);
                    continue;
                }
                if (AgentStepStatus.TOOL_RUNNING.name().equals(step.getStatus()))
                    step = lifecycle.recoverStaleTool(userId, runId, executionVersion, step);
                if (AgentStepStatus.FAILED.name().equals(step.getStatus())) return;
                if (AgentStepStatus.PENDING.name().equals(step.getStatus())) {
                    lifecycle.renewExecution(userId, runId, executionVersion);
                    AgentDecision decision = decisions.decide(run, lifecycle.steps(runId));
                    lifecycle.assertExecutionOwner(userId, runId, executionVersion);
                    if ("TOOL_CALL".equals(decision.type())) registry.require(decision.toolName()).validate(decision.arguments());
                    step = lifecycle.persistDecision(userId, runId, executionVersion, step, decision);
                }
                if ("FINAL".equals(step.getDecisionType())) {
                    lifecycle.completeFinal(userId, runId, executionVersion, run, step, step.getToolResult());
                    return;
                }
                AgentStep completedDuplicate = toolCallIdentity.completedDuplicate(step, lifecycle.steps(runId))
                        .orElse(null);
                if (completedDuplicate != null) {
                    lifecycle.completeDuplicateTool(userId, runId, executionVersion, run, step, completedDuplicate);
                    run = lifecycle.requireOwned(userId, runId);
                    continue;
                }
                step = lifecycle.reserveToolAttempt(userId, runId, executionVersion, step);
                if (step == null) return;
                lifecycle.assertExecutionOwner(userId, runId, executionVersion);
                step = lifecycle.getOrCreateStep(userId, runId, executionVersion, number);
                JsonNode arguments = mapper.readTree(step.getToolArguments());
                String result;
                try {
                    result = tools.execute(userId, step.getToolName(), arguments);
                } catch (AgentExecutionException toolFailure) {
                    if (lifecycle.recordToolFailure(userId, runId, executionVersion, step, toolFailure)) {
                        run = lifecycle.requireOwned(userId, runId);
                        continue;
                    }
                    return;
                }
                lifecycle.assertExecutionOwner(userId, runId, executionVersion);
                lifecycle.completeTool(userId, runId, executionVersion, run, step, result);
                run = lifecycle.requireOwned(userId, runId);
            }
            lifecycle.fail(userId, runId, executionVersion, null, "MAX_STEPS_EXCEEDED",
                    "Agent reached maxSteps without FINAL", -1);
        } catch (Exception exception) {
            AgentRun current = lifecycle.requireOwned(userId, runId);
            if (!AgentRunStatus.COMPLETED.name().equals(current.getStatus()) && !AgentRunStatus.FAILED.name().equals(current.getStatus())) {
                String code = exception instanceof AgentExecutionException agent ? agent.getCode() : "AGENT_EXECUTION_ERROR";
                int retryCount = exception instanceof AgentExecutionException agent ? agent.getRetryCount() : -1;
                lifecycle.fail(userId, runId, executionVersion, step, code, exception.getMessage(), retryCount);
            }
        }
    }
}
