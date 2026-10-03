package com.companion.agent;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.companion.config.AgentProperties;
import com.companion.entity.AgentRun;
import com.companion.entity.AgentStep;
import com.companion.entity.enums.AgentRunStatus;
import com.companion.entity.enums.AgentStepStatus;
import com.companion.mapper.AgentRunMapper;
import com.companion.mapper.AgentStepMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AgentRunLifecycleService {
    private final AgentRunMapper runs;
    private final AgentStepMapper steps;
    private final AgentProperties properties;
    private final ObjectMapper mapper;

    public AgentRunLifecycleService(AgentRunMapper runs, AgentStepMapper steps, AgentProperties properties, ObjectMapper mapper) {
        this.runs = runs; this.steps = steps; this.properties = properties; this.mapper = mapper;
    }

    public AgentRun requireOwned(Long userId, Long id) {
        AgentRun run = runs.selectOne(new QueryWrapper<AgentRun>().eq("id", id).eq("user_id", userId));
        if (run == null) throw new com.companion.common.exception.BusinessException(com.companion.common.result.ResultCode.NOT_FOUND);
        return run;
    }

    public List<AgentStep> steps(Long runId) {
        return steps.selectList(new QueryWrapper<AgentStep>().eq("run_id", runId).orderByAsc("step_number"));
    }

    public List<AgentRun> listOwned(Long userId, int offset, int pageSize) {
        return runs.selectList(new QueryWrapper<AgentRun>().eq("user_id", userId)
                .orderByDesc("create_time").orderByDesc("id")
                .last("LIMIT " + offset + "," + pageSize));
    }

    public long countOwned(Long userId) {
        return runs.selectCount(new QueryWrapper<AgentRun>().eq("user_id", userId));
    }

    public AgentRecoveryState recoveryState(AgentRun run) {
        if (terminal(run)) return AgentRecoveryState.TERMINAL;
        if (AgentRunStatus.PENDING.name().equals(run.getStatus())) return AgentRecoveryState.PENDING;
        if (AgentRunStatus.RUNNING.name().equals(run.getStatus())) {
            LocalDateTime cutoff = LocalDateTime.now().minus(properties.getStaleTimeout());
            return run.getUpdateTime().isBefore(cutoff)
                    ? AgentRecoveryState.STALE_RECOVERABLE : AgentRecoveryState.ACTIVE;
        }
        return AgentRecoveryState.TERMINAL;
    }

    public boolean canResume(AgentRun run) {
        AgentRecoveryState state = recoveryState(run);
        return state == AgentRecoveryState.PENDING || state == AgentRecoveryState.STALE_RECOVERABLE;
    }

    @Transactional
    public boolean failDispatch(Long userId, AgentRun expected, String code, String message) {
        if (!canResume(expected)) return false;
        LocalDateTime now = LocalDateTime.now();
        UpdateWrapper<AgentRun> update = new UpdateWrapper<AgentRun>()
                .eq("id", expected.getId()).eq("user_id", userId).eq("status", expected.getStatus())
                .eq("version", expected.getVersion()).eq("execution_version", expected.getExecutionVersion())
                .set("status", AgentRunStatus.FAILED.name()).set("last_error_code", code)
                .set("last_error_message", limit(message, 1000)).set("completion_time", now)
                .set("update_time", now);
        if (AgentRunStatus.RUNNING.name().equals(expected.getStatus())) {
            update.lt("update_time", now.minus(properties.getStaleTimeout()));
        }
        return runs.update(null, update) == 1;
    }

    @Transactional
    public ExecutionClaim claim(Long userId, Long id) {
        AgentRun run = requireOwned(userId, id);
        if (terminal(run)) return null;
        LocalDateTime now = LocalDateTime.now();
        UpdateWrapper<AgentRun> update = new UpdateWrapper<AgentRun>()
                .eq("id", id).eq("user_id", userId).eq("version", run.getVersion())
                .eq("execution_version", run.getExecutionVersion()).eq("status", run.getStatus());
        if (AgentRunStatus.RUNNING.name().equals(run.getStatus())) {
            LocalDateTime cutoff = now.minus(properties.getStaleTimeout());
            if (!run.getUpdateTime().isBefore(cutoff)) return null;
            update.lt("update_time", cutoff);
        } else if (!AgentRunStatus.PENDING.name().equals(run.getStatus())) return null;
        int executionVersion = run.getExecutionVersion() + 1;
        update.set("status", AgentRunStatus.RUNNING.name()).set("version", run.getVersion() + 1)
                .set("execution_version", executionVersion).set("update_time", now);
        return runs.update(null, update) == 1 ? new ExecutionClaim(id, userId, executionVersion) : null;
    }

    public boolean isExecutionOwner(Long userId, Long runId, int executionVersion) {
        return runs.selectCount(new QueryWrapper<AgentRun>().eq("id", runId).eq("user_id", userId)
                .eq("status", AgentRunStatus.RUNNING.name()).eq("execution_version", executionVersion)) == 1;
    }

    public void assertExecutionOwner(Long userId, Long runId, int executionVersion) {
        if (!isExecutionOwner(userId, runId, executionVersion)) throw lostExecution();
    }

    @Transactional
    public void renewExecution(Long userId, Long runId, int executionVersion) {
        if (runs.update(null, runningUpdate(userId, runId, executionVersion)
                .set("update_time", LocalDateTime.now())) != 1) throw lostExecution();
    }

    @Transactional
    public AgentStep getOrCreateStep(Long userId, Long runId, int executionVersion, int number) {
        lockExecution(userId, runId, executionVersion);
        AgentStep existing = find(runId, number);
        if (existing != null) return existing;
        AgentStep created = new AgentStep(); created.setRunId(runId); created.setStepNumber(number);
        created.setStatus(AgentStepStatus.PENDING.name()); created.setRetryCount(0); created.setToolAttemptCount(0);
        created.setCreateTime(LocalDateTime.now()); created.setUpdateTime(created.getCreateTime());
        try { steps.insert(created); return created; }
        catch (DuplicateKeyException exception) { AgentStep winner = find(runId, number); if (winner == null) throw exception; return winner; }
    }

    @Transactional
    public AgentStep persistDecision(Long userId, Long runId, int executionVersion, AgentStep step,
                                     com.companion.agent.model.AgentDecision decision) {
        lockExecution(userId, runId, executionVersion);
        UpdateWrapper<AgentStep> update = new UpdateWrapper<AgentStep>().eq("id", step.getId()).eq("run_id", runId)
                .eq("status", AgentStepStatus.PENDING.name()).set("decision_type", decision.type())
                .set("decision_summary", decision.decisionSummary().trim()).set("tool_name", decision.toolName())
                .set("tool_arguments", json(decision.arguments()))
                .set("tool_result", "FINAL".equals(decision.type()) ? decision.finalResult() : null)
                .set("tool_call_id", "TOOL_CALL".equals(decision.type()) ? UUID.randomUUID().toString() : null)
                .set("status", AgentStepStatus.DECIDED.name()).set("update_time", LocalDateTime.now());
        if (steps.update(null, update) != 1) throw conflict();
        return steps.selectById(step.getId());
    }

    @Transactional
    public AgentStep reserveToolAttempt(Long userId, Long runId, int executionVersion, AgentStep step) {
        lockExecution(userId, runId, executionVersion);
        int attempts = step.getToolAttemptCount() == null ? 0 : step.getToolAttemptCount();
        if (attempts >= 2) return null;
        if (runs.update(null, runningUpdate(userId, runId, executionVersion)
                .set("update_time", LocalDateTime.now())) != 1) throw lostExecution();
        int affected = steps.update(null, new UpdateWrapper<AgentStep>().eq("id", step.getId()).eq("run_id", runId)
                .eq("tool_call_id", step.getToolCallId()).eq("status", AgentStepStatus.DECIDED.name())
                .eq("tool_attempt_count", attempts).lt("tool_attempt_count", 2)
                .set("tool_attempt_count", attempts + 1).set("retry_count", Math.max(0, attempts))
                .set("status", AgentStepStatus.TOOL_RUNNING.name()).set("update_time", LocalDateTime.now()));
        return affected == 1 ? steps.selectById(step.getId()) : null;
    }

    @Transactional
    public AgentStep recoverStaleTool(Long userId, Long runId, int executionVersion, AgentStep step) {
        lockExecution(userId, runId, executionVersion);
        if (!AgentStepStatus.TOOL_RUNNING.name().equals(step.getStatus())) return step;
        int attempts = step.getToolAttemptCount() == null ? 0 : step.getToolAttemptCount();
        if (attempts >= 2) {
            failExhaustedTool(userId, runId, executionVersion, step, "TOOL_RETRY_EXHAUSTED", "Stale tool retry exhausted");
            return steps.selectById(step.getId());
        }
        if (steps.update(null, new UpdateWrapper<AgentStep>().eq("id", step.getId()).eq("run_id", runId)
                .eq("tool_call_id", step.getToolCallId()).eq("status", AgentStepStatus.TOOL_RUNNING.name())
                .eq("tool_attempt_count", attempts).set("status", AgentStepStatus.DECIDED.name())
                .set("update_time", LocalDateTime.now())) != 1) throw conflict();
        return steps.selectById(step.getId());
    }

    @Transactional
    public boolean recordToolFailure(Long userId, Long runId, int executionVersion, AgentStep step,
                                     AgentExecutionException failure) {
        lockExecution(userId, runId, executionVersion);
        AgentStep current = steps.selectById(step.getId());
        int attempts = current.getToolAttemptCount() == null ? 0 : current.getToolAttemptCount();
        if (failure.isRetryable() && attempts < 2) {
            if (steps.update(null, new UpdateWrapper<AgentStep>().eq("id", current.getId()).eq("run_id", runId)
                    .eq("tool_call_id", current.getToolCallId()).eq("status", AgentStepStatus.TOOL_RUNNING.name())
                    .eq("tool_attempt_count", attempts).set("status", AgentStepStatus.DECIDED.name())
                    .set("error_code", failure.getCode()).set("error_message", limit(failure.getMessage(), 1000))
                    .set("update_time", LocalDateTime.now())) != 1) throw conflict();
            return true;
        }
        failExhaustedTool(userId, runId, executionVersion, current, failure.getCode(), failure.getMessage());
        return false;
    }

    @Transactional
    public void completeTool(Long userId, Long runId, int executionVersion, AgentRun expectedRun,
                             AgentStep step, String result) {
        lockExecution(userId, runId, executionVersion);
        LocalDateTime now = LocalDateTime.now();
        if (steps.update(null, new UpdateWrapper<AgentStep>().eq("id", step.getId()).eq("run_id", runId)
                .eq("tool_call_id", step.getToolCallId()).eq("status", AgentStepStatus.TOOL_RUNNING.name())
                .eq("tool_attempt_count", step.getToolAttemptCount())
                .set("tool_result", result).set("status", AgentStepStatus.COMPLETED.name())
                .set("completion_time", now).set("update_time", now)) != 1) throw conflict();
        if (runs.update(null, runningUpdate(userId, runId, executionVersion)
                .eq("current_step", expectedRun.getCurrentStep()).set("current_step", step.getStepNumber())
                .set("update_time", now)) != 1) throw conflict();
    }

    @Transactional
    public void completeDuplicateTool(Long userId, Long runId, int executionVersion, AgentRun expectedRun,
                                      AgentStep duplicate, AgentStep completed) {
        lockExecution(userId, runId, executionVersion);
        LocalDateTime now = LocalDateTime.now();
        String summary = "This exact tool call already completed at step " + completed.getStepNumber()
                + ". Reuse its persisted result; choose a different tool only if needed, or return FINAL.";
        if (steps.update(null, new UpdateWrapper<AgentStep>().eq("id", duplicate.getId()).eq("run_id", runId)
                .eq("tool_call_id", duplicate.getToolCallId()).eq("status", AgentStepStatus.DECIDED.name())
                .eq("tool_attempt_count", 0)
                .set("decision_type", "DUPLICATE_TOOL_CALL")
                .set("decision_summary", summary).set("tool_result", completed.getToolResult())
                .set("status", AgentStepStatus.COMPLETED.name())
                .set("completion_time", now).set("update_time", now)) != 1) throw conflict();
        if (runs.update(null, runningUpdate(userId, runId, executionVersion)
                .eq("current_step", expectedRun.getCurrentStep()).set("current_step", duplicate.getStepNumber())
                .set("update_time", now)) != 1) throw conflict();
    }

    @Transactional
    public void completeFinal(Long userId, Long runId, int executionVersion, AgentRun expectedRun,
                              AgentStep step, String result) {
        lockExecution(userId, runId, executionVersion);
        LocalDateTime now = LocalDateTime.now();
        if (steps.update(null, new UpdateWrapper<AgentStep>().eq("id", step.getId()).eq("run_id", runId)
                .eq("status", AgentStepStatus.DECIDED.name()).eq("decision_type", "FINAL")
                .set("tool_result", result).set("status", AgentStepStatus.COMPLETED.name())
                .set("completion_time", now).set("update_time", now)) != 1) throw conflict();
        if (runs.update(null, runningUpdate(userId, runId, executionVersion)
                .eq("current_step", expectedRun.getCurrentStep()).set("status", AgentRunStatus.COMPLETED.name())
                .set("current_step", step.getStepNumber()).set("final_result", result)
                .set("completion_time", now).set("update_time", now)) != 1) throw conflict();
    }

    @Transactional
    public boolean fail(Long userId, Long runId, int executionVersion, AgentStep step,
                        String code, String message, int retryCount) {
        if (runs.lockOwnedExecution(runId, userId, executionVersion) == null) return false;
        LocalDateTime now = LocalDateTime.now();
        if (step != null) {
            UpdateWrapper<AgentStep> update = new UpdateWrapper<AgentStep>().eq("id", step.getId()).eq("run_id", runId)
                    .ne("status", AgentStepStatus.COMPLETED.name()).set("status", AgentStepStatus.FAILED.name())
                    .set("error_code", code).set("error_message", limit(message, 1000))
                    .set("completion_time", now).set("update_time", now);
            if (retryCount >= 0) update.set("retry_count", retryCount);
            steps.update(null, update);
        }
        return runs.update(null, runningUpdate(userId, runId, executionVersion)
                .set("status", AgentRunStatus.FAILED.name()).set("last_error_code", code)
                .set("last_error_message", limit(message, 1000)).set("completion_time", now)
                .set("update_time", now)) == 1;
    }

    private UpdateWrapper<AgentRun> runningUpdate(Long userId, Long runId, int executionVersion) {
        return new UpdateWrapper<AgentRun>().eq("id", runId).eq("user_id", userId)
                .eq("status", AgentRunStatus.RUNNING.name()).eq("execution_version", executionVersion);
    }

    private void failExhaustedTool(Long userId, Long runId, int executionVersion, AgentStep step,
                                   String code, String message) {
        LocalDateTime now = LocalDateTime.now();
        if (steps.update(null, new UpdateWrapper<AgentStep>().eq("id", step.getId()).eq("run_id", runId)
                .eq("tool_call_id", step.getToolCallId()).eq("status", AgentStepStatus.TOOL_RUNNING.name())
                .eq("tool_attempt_count", step.getToolAttemptCount()).set("status", AgentStepStatus.FAILED.name())
                .set("error_code", code).set("error_message", limit(message, 1000))
                .set("completion_time", now).set("update_time", now)) != 1) throw conflict();
        if (runs.update(null, runningUpdate(userId, runId, executionVersion)
                .set("status", AgentRunStatus.FAILED.name()).set("last_error_code", code)
                .set("last_error_message", limit(message, 1000)).set("completion_time", now)
                .set("update_time", now)) != 1) throw conflict();
    }

    private AgentRun lockExecution(Long userId, Long runId, int executionVersion) {
        AgentRun run = runs.lockOwnedExecution(runId, userId, executionVersion);
        if (run == null) throw lostExecution();
        return run;
    }

    private AgentStep find(Long runId, int number) { return steps.selectOne(new QueryWrapper<AgentStep>().eq("run_id", runId).eq("step_number", number)); }
    private boolean terminal(AgentRun run) { return AgentRunStatus.COMPLETED.name().equals(run.getStatus()) || AgentRunStatus.FAILED.name().equals(run.getStatus()); }
    private String json(JsonNode node) { try { return node == null ? null : mapper.writeValueAsString(node); } catch (Exception e) { throw new IllegalArgumentException(e); } }
    private AgentExecutionException conflict() { return new AgentExecutionException("AGENT_CAS_CONFLICT", "Agent state changed concurrently", false); }
    private AgentExecutionException lostExecution() { return new AgentExecutionException("AGENT_EXECUTION_LOST", "Agent execution ownership was lost", false); }
    private String limit(String value, int length) { return value == null ? null : value.substring(0, Math.min(length, value.length())); }

    public record ExecutionClaim(Long runId, Long userId, int executionVersion) {}
}
