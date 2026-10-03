package com.companion.agent;

import com.companion.agent.model.AgentDecision;
import com.companion.agent.tool.AgentToolExecutor;
import com.companion.common.exception.BusinessException;
import com.companion.dto.request.AgentRunCreateRequest;
import com.companion.dto.response.AgentRunResponse;
import com.companion.entity.AgentRun;
import com.companion.mapper.AgentRunMapper;
import com.companion.service.AgentRunService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("soft-delete-test")
class AgentUiContractIntegrationTest {
    @Autowired AgentRunService service;
    @Autowired AgentRunLifecycleService lifecycle;
    @Autowired AgentOrchestrator orchestrator;
    @Autowired AgentRunMapper runs;
    @Autowired ObjectMapper json;
    @MockBean AgentDecisionClient decisions;
    @MockBean AgentToolExecutor tools;

    @BeforeEach
    void resetMocks() {
        reset(decisions, tools);
    }

    @Test
    void createReturnsBeforeExecutionCompletesAndPollingObservesFinal() throws Exception {
        long userId = 71001L;
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(decisions.decide(any(), any())).thenAnswer(invocation -> {
            entered.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            return new AgentDecision("FINAL", "done", null, null, "async result");
        });

        long started = System.nanoTime();
        AgentRunResponse accepted = service.create(userId, request("async create"));
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(2));
        assertThat(accepted.getId()).isNotNull();
        assertThat(accepted.getStatus()).isIn("PENDING", "RUNNING");
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
        AgentRunResponse visible = service.get(userId, accepted.getId());
        assertThat(visible.getStatus()).isEqualTo("RUNNING");
        assertThat(visible.getRecoveryState()).isEqualTo("ACTIVE");
        assertThat(visible.getCanResume()).isFalse();

        release.countDown();
        AgentRunResponse completed = awaitStatus(userId, accepted.getId(), "COMPLETED");
        assertThat(completed.getFinalResult()).isEqualTo("async result");
        assertThat(completed.getSteps()).hasSize(1);
    }

    @Test
    void pollingSeesPersistedToolProgressBeforeFinal() throws Exception {
        long userId = 71002L;
        CountDownLatch toolEntered = new CountDownLatch(1);
        CountDownLatch releaseTool = new CountDownLatch(1);
        when(decisions.decide(any(), any())).thenReturn(
                new AgentDecision("TOOL_CALL", "need world", "get_world_context",
                        json.createObjectNode().put("worldId", 1), null),
                new AgentDecision("FINAL", "done", null, null, "world answer"));
        when(tools.execute(anyLong(), eq("get_world_context"), any())).thenAnswer(invocation -> {
            toolEntered.countDown();
            assertThat(releaseTool.await(2, TimeUnit.SECONDS)).isTrue();
            return "persisted world";
        });

        AgentRunResponse accepted = service.create(userId, request("poll tool"));
        assertThat(toolEntered.await(5, TimeUnit.SECONDS)).isTrue();
        AgentRunResponse progress = service.get(userId, accepted.getId());
        assertThat(progress.getSteps()).hasSize(1);
        assertThat(progress.getSteps().get(0).getStatus()).isEqualTo("TOOL_RUNNING");
        assertThat(progress.getSteps().get(0).getToolAttemptCount()).isEqualTo(1);

        releaseTool.countDown();
        AgentRunResponse completed = awaitStatus(userId, accepted.getId(), "COMPLETED");
        assertThat(completed.getSteps()).extracting("status").containsExactly("COMPLETED", "COMPLETED");
        assertThat(completed.getSteps().get(0).getToolResult()).isEqualTo("persisted world");
    }

    @Test
    void concurrentRequestIdIsOneRunAndOneLogicalExecution() throws Exception {
        long userId = 71003L;
        AgentRunCreateRequest request = request("idempotent async create");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger decisionCalls = new AtomicInteger();
        when(decisions.decide(any(), any())).thenAnswer(invocation -> {
            decisionCalls.incrementAndGet(); entered.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            return new AgentDecision("FINAL", "done", null, null, "one result");
        });
        var callers = Executors.newFixedThreadPool(2);
        try {
            var first = callers.submit(() -> service.create(userId, request));
            var second = callers.submit(() -> service.create(userId, request));
            AgentRunResponse a = first.get(5, TimeUnit.SECONDS);
            AgentRunResponse b = second.get(5, TimeUnit.SECONDS);
            assertThat(a.getId()).isEqualTo(b.getId());
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            release.countDown();
            awaitStatus(userId, a.getId(), "COMPLETED");
            assertThat(decisionCalls).hasValue(1);
        } finally {
            release.countDown(); callers.shutdownNow();
        }
    }

    @Test
    void listIsOwnedBoundedNewestFirstAndExposesRecoveryState() {
        long userId = 71004L;
        AgentRun pending = run(userId, "PENDING", LocalDateTime.now().minusMinutes(3));
        AgentRun active = run(userId, "RUNNING", LocalDateTime.now());
        AgentRun stale = run(userId, "RUNNING", LocalDateTime.now().minusMinutes(10));
        AgentRun failed = run(userId, "FAILED", LocalDateTime.now().minusMinutes(2));
        AgentRun completed = run(userId, "COMPLETED", LocalDateTime.now().plusSeconds(1));
        run(71999L, "PENDING", LocalDateTime.now().plusMinutes(1));

        var page = service.list(userId, 1, 10);
        assertThat(page.getTotal()).isEqualTo(5);
        assertThat(page.getItems()).extracting("id")
                .containsExactly(completed.getId(), active.getId(), failed.getId(), pending.getId(), stale.getId());
        assertThat(page.getItems()).filteredOn(item -> item.getId().equals(pending.getId())).singleElement()
                .satisfies(item -> { assertThat(item.getCanResume()).isTrue(); assertThat(item.getRecoveryState()).isEqualTo("PENDING"); });
        assertThat(page.getItems()).filteredOn(item -> item.getId().equals(active.getId())).singleElement()
                .satisfies(item -> { assertThat(item.getCanResume()).isFalse(); assertThat(item.getRecoveryState()).isEqualTo("ACTIVE"); });
        assertThat(page.getItems()).filteredOn(item -> item.getId().equals(stale.getId())).singleElement()
                .satisfies(item -> { assertThat(item.getCanResume()).isTrue(); assertThat(item.getRecoveryState()).isEqualTo("STALE_RECOVERABLE"); });
        assertThat(page.getItems()).filteredOn(item -> item.getId().equals(completed.getId())).singleElement()
                .satisfies(item -> { assertThat(item.getCanResume()).isFalse(); assertThat(item.getRecoveryState()).isEqualTo("TERMINAL"); });
        assertThat(page.getItems()).filteredOn(item -> item.getId().equals(failed.getId())).singleElement()
                .satisfies(item -> { assertThat(item.getCanResume()).isFalse(); assertThat(item.getRecoveryState()).isEqualTo("TERMINAL"); });
        assertThatThrownBy(() -> service.list(userId, 1, 51)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.get(71999L, pending.getId())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.resume(71999L, pending.getId())).isInstanceOf(BusinessException.class);
    }

    @Test
    void executorRejectionPersistsDiagnosableFailure() {
        long userId = 71006L;
        AgentRun pending = run(userId, "PENDING", LocalDateTime.now());
        AgentExecutionDispatcher rejecting = new AgentExecutionDispatcher(
                command -> { throw new java.util.concurrent.RejectedExecutionException("full"); }, orchestrator, lifecycle);

        assertThat(rejecting.dispatch(userId, pending)).isEqualTo(AgentExecutionDispatcher.DispatchStatus.REJECTED);
        AgentRunResponse failed = service.get(userId, pending.getId());
        assertThat(failed.getStatus()).isEqualTo("FAILED");
        assertThat(failed.getLastErrorCode()).isEqualTo("AGENT_EXECUTION_REJECTED");
        assertThat(failed.getLastErrorMessage()).isEqualTo("Agent execution queue is full");
        assertThat(failed.getCanResume()).isFalse();
    }

    @Test
    void staleResumeReturnsQuicklyAndTerminalResumeIsNoOp() throws Exception {
        long userId = 71005L;
        AgentRun stale = run(userId, "RUNNING", LocalDateTime.now().minusMinutes(10));
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(decisions.decide(any(), any())).thenAnswer(invocation -> {
            entered.countDown(); assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            return new AgentDecision("FINAL", "done", null, null, "resumed");
        });

        long started = System.nanoTime();
        AgentRunResponse response = service.resume(userId, stale.getId());
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(2));
        assertThat(response.getStatus()).isEqualTo("RUNNING");
        assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
        release.countDown();
        awaitStatus(userId, stale.getId(), "COMPLETED");
        service.resume(userId, stale.getId());
        verify(decisions, times(1)).decide(any(), any());
    }

    private AgentRunResponse awaitStatus(long userId, long runId, String expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        AgentRunResponse response;
        do {
            response = service.get(userId, runId);
            if (expected.equals(response.getStatus())) return response;
            Thread.sleep(20);
        } while (System.nanoTime() < deadline);
        throw new AssertionError("Run did not reach " + expected + "; last status=" + response.getStatus());
    }

    private AgentRunCreateRequest request(String goal) {
        AgentRunCreateRequest request = new AgentRunCreateRequest();
        request.setRequestId(UUID.randomUUID().toString()); request.setGoal(goal); request.setMaxSteps(6); return request;
    }

    private AgentRun run(long userId, String status, LocalDateTime time) {
        AgentRun run = new AgentRun(); run.setUserId(userId); run.setRequestId(UUID.randomUUID().toString());
        run.setGoal("goal"); run.setStatus(status); run.setCurrentStep(0); run.setMaxSteps(6);
        run.setVersion(0); run.setExecutionVersion(0); run.setCreateTime(time); run.setUpdateTime(time);
        if ("COMPLETED".equals(status) || "FAILED".equals(status)) run.setCompletionTime(time);
        runs.insert(run); return run;
    }
}
