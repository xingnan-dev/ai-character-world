package com.companion.service.impl;

import com.companion.agent.*;
import com.companion.common.exception.BusinessException;
import com.companion.common.result.ResultCode;
import com.companion.dto.request.AgentRunCreateRequest;
import com.companion.dto.response.*;
import com.companion.entity.AgentRun;
import com.companion.entity.AgentStep;
import com.companion.service.AgentRunService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AgentRunServiceImpl implements AgentRunService {
    private static final int MAX_PAGE_SIZE = 50;
    private final AgentRunCreationService creation;
    private final AgentRunLifecycleService lifecycle;
    private final AgentExecutionDispatcher dispatcher;

    public AgentRunServiceImpl(AgentRunCreationService creation, AgentRunLifecycleService lifecycle,
                               AgentExecutionDispatcher dispatcher) {
        this.creation = creation; this.lifecycle = lifecycle; this.dispatcher = dispatcher;
    }

    public AgentRunResponse create(Long userId, AgentRunCreateRequest request) {
        AgentRun run = creation.create(userId, request);
        dispatcher.dispatch(userId, run);
        return get(userId, run.getId());
    }

    public AgentRunResponse get(Long userId, Long runId) {
        AgentRun run = lifecycle.requireOwned(userId, runId);
        return response(run, lifecycle.steps(runId));
    }

    public AgentRunPageResponse list(Long userId, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "page must be >= 1 and pageSize must be between 1 and 50");
        }
        long offset = (long) (page - 1) * pageSize;
        if (offset > Integer.MAX_VALUE) throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "page offset is too large");
        AgentRunPageResponse response = new AgentRunPageResponse();
        response.setItems(lifecycle.listOwned(userId, (int) offset, pageSize).stream().map(this::listItem).toList());
        response.setPage(page); response.setPageSize(pageSize); response.setTotal(lifecycle.countOwned(userId));
        return response;
    }

    public AgentRunResponse resume(Long userId, Long runId) {
        AgentRun run = lifecycle.requireOwned(userId, runId);
        dispatcher.dispatch(userId, run);
        return get(userId, runId);
    }

    private AgentRunResponse response(AgentRun run, List<AgentStep> steps) {
        AgentRunResponse response = new AgentRunResponse();
        response.setId(run.getId()); response.setRequestId(run.getRequestId()); response.setGoal(run.getGoal());
        response.setStatus(run.getStatus()); response.setCurrentStep(run.getCurrentStep()); response.setMaxSteps(run.getMaxSteps());
        response.setFinalResult(run.getFinalResult()); response.setLastErrorCode(run.getLastErrorCode());
        response.setLastErrorMessage(run.getLastErrorMessage()); response.setVersion(run.getVersion());
        response.setExecutionVersion(run.getExecutionVersion()); applyRecovery(response, run);
        response.setCreateTime(run.getCreateTime()); response.setUpdateTime(run.getUpdateTime());
        response.setCompletionTime(run.getCompletionTime()); response.setSteps(steps.stream().map(this::step).toList());
        return response;
    }

    private AgentRunListItemResponse listItem(AgentRun run) {
        AgentRunListItemResponse response = new AgentRunListItemResponse();
        response.setId(run.getId()); response.setRequestId(run.getRequestId()); response.setGoal(run.getGoal());
        response.setStatus(run.getStatus()); response.setCurrentStep(run.getCurrentStep()); response.setMaxSteps(run.getMaxSteps());
        response.setLastErrorCode(run.getLastErrorCode()); response.setLastErrorMessage(run.getLastErrorMessage());
        AgentRecoveryState recoveryState = lifecycle.recoveryState(run);
        response.setRecoveryState(recoveryState.name()); response.setCanResume(canResume(recoveryState));
        response.setCreateTime(run.getCreateTime()); response.setUpdateTime(run.getUpdateTime()); response.setCompletionTime(run.getCompletionTime());
        return response;
    }

    private void applyRecovery(AgentRunResponse response, AgentRun run) {
        AgentRecoveryState recoveryState = lifecycle.recoveryState(run);
        response.setRecoveryState(recoveryState.name()); response.setCanResume(canResume(recoveryState));
    }

    private boolean canResume(AgentRecoveryState state) {
        return state == AgentRecoveryState.PENDING || state == AgentRecoveryState.STALE_RECOVERABLE;
    }

    private AgentStepResponse step(AgentStep step) {
        AgentStepResponse response = new AgentStepResponse();
        response.setId(step.getId()); response.setStepNumber(step.getStepNumber()); response.setDecisionType(step.getDecisionType());
        response.setDecisionSummary(step.getDecisionSummary()); response.setToolCallId(step.getToolCallId()); response.setToolName(step.getToolName());
        response.setToolArguments(step.getToolArguments()); response.setToolResult(step.getToolResult()); response.setStatus(step.getStatus());
        response.setRetryCount(step.getRetryCount()); response.setToolAttemptCount(step.getToolAttemptCount()); response.setErrorCode(step.getErrorCode());
        response.setErrorMessage(step.getErrorMessage()); response.setCreateTime(step.getCreateTime()); response.setUpdateTime(step.getUpdateTime());
        response.setCompletionTime(step.getCompletionTime()); return response;
    }
}
