package com.companion.agent;

import com.companion.entity.AgentRun;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AgentExecutionDispatcherTest {
    @Test
    void rejectedDispatchTransitionsRecoverableRunToExplicitFailure() {
        Executor rejecting = command -> { throw new RejectedExecutionException("full"); };
        AgentOrchestrator orchestrator = mock(AgentOrchestrator.class);
        AgentRunLifecycleService lifecycle = mock(AgentRunLifecycleService.class);
        AgentRun run = new AgentRun(); run.setId(42L); run.setStatus("PENDING");
        when(lifecycle.canResume(run)).thenReturn(true);
        AgentExecutionDispatcher dispatcher = new AgentExecutionDispatcher(rejecting, orchestrator, lifecycle);

        assertThat(dispatcher.dispatch(7L, run)).isEqualTo(AgentExecutionDispatcher.DispatchStatus.REJECTED);
        verify(lifecycle).failDispatch(7L, run, "AGENT_EXECUTION_REJECTED", "Agent execution queue is full");
        verifyNoInteractions(orchestrator);
    }

    @Test
    void activeOrTerminalRunIsNotDispatched() {
        Executor executor = mock(Executor.class);
        AgentOrchestrator orchestrator = mock(AgentOrchestrator.class);
        AgentRunLifecycleService lifecycle = mock(AgentRunLifecycleService.class);
        AgentRun run = new AgentRun(); run.setId(43L); run.setStatus("COMPLETED");
        when(lifecycle.canResume(run)).thenReturn(false);

        var dispatcher = new AgentExecutionDispatcher(executor, orchestrator, lifecycle);
        assertThat(dispatcher.dispatch(7L, run)).isEqualTo(AgentExecutionDispatcher.DispatchStatus.NOT_DISPATCHED);
        verifyNoInteractions(executor, orchestrator);
    }
}
