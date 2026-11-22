package com.companion.agent;

import com.companion.entity.AgentRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Component
public class AgentExecutionDispatcher {
    private static final Logger log = LoggerFactory.getLogger(AgentExecutionDispatcher.class);
    private final Executor executor;
    private final AgentOrchestrator orchestrator;
    private final AgentRunLifecycleService lifecycle;
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();

    public AgentExecutionDispatcher(@Qualifier("agentExecutionExecutor") Executor executor,
                                    AgentOrchestrator orchestrator,
                                    AgentRunLifecycleService lifecycle) {
        this.executor = executor;
        this.orchestrator = orchestrator;
        this.lifecycle = lifecycle;
    }

    public DispatchStatus dispatch(Long userId, AgentRun run) {
        if (!lifecycle.canResume(run) || !inFlight.add(run.getId())) return DispatchStatus.NOT_DISPATCHED;
        try {
            executor.execute(() -> execute(userId, run.getId()));
            return DispatchStatus.ACCEPTED;
        } catch (RejectedExecutionException exception) {
            inFlight.remove(run.getId());
            lifecycle.failDispatch(userId, run, "AGENT_EXECUTION_REJECTED", "Agent execution queue is full");
            return DispatchStatus.REJECTED;
        } catch (RuntimeException exception) {
            inFlight.remove(run.getId());
            lifecycle.failDispatch(userId, run, "AGENT_DISPATCH_ERROR", "Agent execution could not be scheduled");
            log.error("Failed to dispatch Agent run {}", run.getId(), exception);
            return DispatchStatus.REJECTED;
        }
    }

    private void execute(Long userId, Long runId) {
        try {
            orchestrator.execute(userId, runId);
        } catch (RuntimeException exception) {
            log.error("Unexpected Agent worker failure for run {}", runId, exception);
        } finally {
            inFlight.remove(runId);
        }
    }

    public enum DispatchStatus { ACCEPTED, NOT_DISPATCHED, REJECTED }
}
