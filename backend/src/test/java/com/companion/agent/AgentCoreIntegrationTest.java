package com.companion.agent;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.companion.agent.model.AgentDecision;
import com.companion.agent.tool.AgentToolExecutor;
import com.companion.common.exception.BusinessException;
import com.companion.dto.request.AgentRunCreateRequest;
import com.companion.entity.AgentRun;
import com.companion.entity.AgentStep;
import com.companion.mapper.AgentRunMapper;
import com.companion.mapper.AgentStepMapper;
import com.companion.service.AgentRunService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("soft-delete-test")
class AgentCoreIntegrationTest {
    @Autowired AgentRunService service;
    @Autowired AgentRunCreationService creation;
    @Autowired AgentRunLifecycleService lifecycle;
    @Autowired AgentRunMapper runs;
    @Autowired AgentStepMapper steps;
    @Autowired ObjectMapper json;
    @MockBean AgentDecisionClient decisions;
    @MockBean AgentToolExecutor tools;
    ExecutorService workers;

    @BeforeEach void setup() { reset(decisions, tools); workers = Executors.newFixedThreadPool(2); }
    @AfterEach void close() { workers.shutdownNow(); }

    @Test
    void persistsRealMultiStepLoopAndIdempotentlyReusesCompletedRun() {
        AgentRunCreateRequest request = request("answer using world context", 6);
        when(decisions.decide(any(), any())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return new AgentDecision("TOOL_CALL", "need world", "get_world_context",
                    json.createObjectNode().put("worldId", 1), null);
        }).thenReturn(new AgentDecision("FINAL", "done", null, null, "final answer"));
        when(tools.execute(anyLong(), eq("get_world_context"), any())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return "world result";
        });
        var result = service.create(70001L, request);
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getSteps()).extracting("status").containsExactly("COMPLETED", "COMPLETED");
        var same = service.create(70001L, request);
        assertThat(same.getId()).isEqualTo(result.getId());
        verify(tools, times(1)).execute(anyLong(), anyString(), any());
    }

    @Test
    void ownershipInputAndRetryFailureCheckpointAreHandled() {
        AgentRunCreateRequest invalid = request(" ", 6);
        assertThatThrownBy(() -> service.create(70002L, invalid)).isInstanceOf(BusinessException.class);
        AgentRun run = run(70002L, "PENDING", 0, 6, LocalDateTime.now());
        when(decisions.decide(any(), any())).thenReturn(new AgentDecision("TOOL_CALL", "need world",
                "get_world_context", json.createObjectNode().put("worldId", 1), null));
        when(tools.execute(anyLong(), anyString(), any()))
                .thenThrow(new AgentExecutionException("TOOL_TIMEOUT", "timed out", true));
        var failed = service.resume(70002L, run.getId());
        assertThat(failed.getStatus()).isEqualTo("FAILED");
        assertThat(failed.getSteps().get(0).getRetryCount()).isEqualTo(1);
        assertThat(failed.getSteps().get(0).getToolAttemptCount()).isEqualTo(2);
        String toolCallId = failed.getSteps().get(0).getToolCallId();
        var resumed = service.resume(70002L, run.getId());
        assertThat(resumed.getSteps().get(0).getToolCallId()).isEqualTo(toolCallId);
        verify(tools, times(2)).execute(anyLong(), anyString(), any());
        assertThatThrownBy(() -> service.get(99999L, run.getId())).isInstanceOf(BusinessException.class);
    }

    @Test
    void timeoutThenRetrySuccessConsumesExactlyTwoAttempts() {
        AgentRun run = run(70010L, "PENDING", 0, 6, LocalDateTime.now());
        when(decisions.decide(any(), any())).thenReturn(
                new AgentDecision("TOOL_CALL", "need world", "get_world_context", json.createObjectNode().put("worldId", 1), null),
                new AgentDecision("FINAL", "done", null, null, "ok"));
        when(tools.execute(anyLong(), anyString(), any()))
                .thenThrow(new AgentExecutionException("TOOL_TIMEOUT", "timed out", true))
                .thenReturn("world");
        var result = service.resume(70010L, run.getId());
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getSteps().get(0).getToolAttemptCount()).isEqualTo(2);
        assertThat(result.getSteps().get(0).getRetryCount()).isEqualTo(1);
        verify(tools, times(2)).execute(anyLong(), anyString(), any());
    }

    @Test
    void retryableNonTimeoutFailureIsNotMistakenForCompletedDuplicate() {
        AgentRun run = run(70019L, "PENDING", 0, 6, LocalDateTime.now());
        when(decisions.decide(any(), any())).thenReturn(
                new AgentDecision("TOOL_CALL", "need world", "get_world_context",
                        json.createObjectNode().put("worldId", 1), null),
                new AgentDecision("FINAL", "done", null, null, "ok"));
        when(tools.execute(anyLong(), anyString(), any()))
                .thenThrow(new AgentExecutionException("TOOL_TRANSIENT", "temporary", true))
                .thenReturn("world");

        var result = service.resume(70019L, run.getId());

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getSteps().get(0).getDecisionType()).isEqualTo("TOOL_CALL");
        assertThat(result.getSteps().get(0).getToolAttemptCount()).isEqualTo(2);
        assertThat(result.getSteps().get(0).getRetryCount()).isEqualTo(1);
        verify(tools, times(2)).execute(anyLong(), eq("get_world_context"), any());
    }

    @Test
    void completedExactDuplicateReusesResultWithoutExecutingToolAgain() {
        AgentRun run = run(70020L, "PENDING", 0, 6, LocalDateTime.now());
        AgentDecision call = characterDecision(3);
        when(decisions.decide(any(), any())).thenReturn(
                call, call, new AgentDecision("FINAL", "done", null, null, "ok"));
        when(tools.execute(anyLong(), eq("get_character_context"), any())).thenReturn("character-result");

        var result = service.resume(70020L, run.getId());

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getSteps()).extracting("decisionType")
                .containsExactly("TOOL_CALL", "DUPLICATE_TOOL_CALL", "FINAL");
        assertThat(result.getSteps().get(1).getToolResult()).isEqualTo("character-result");
        assertThat(result.getSteps().get(1).getToolAttemptCount()).isZero();
        assertThat(result.getSteps().get(1).getRetryCount()).isZero();
        assertThat(result.getSteps().get(1).getDecisionSummary()).contains("already completed at step 1");
        org.mockito.ArgumentCaptor<java.util.List<AgentStep>> histories =
                org.mockito.ArgumentCaptor.forClass(java.util.List.class);
        verify(decisions, times(3)).decide(any(), histories.capture());
        assertThat(histories.getAllValues().get(2))
                .anyMatch(checkpoint -> "DUPLICATE_TOOL_CALL".equals(checkpoint.getDecisionType())
                        && "character-result".equals(checkpoint.getToolResult()));
        verify(tools, times(1)).execute(anyLong(), eq("get_character_context"), any());
    }

    @Test
    void canonicalJsonFieldOrderIsDuplicate() {
        AgentRun run = run(70021L, "PENDING", 0, 6, LocalDateTime.now());
        var first = json.createObjectNode().put("characterId", 3).put("query", "history");
        var reordered = json.createObjectNode().put("query", "history").put("characterId", 3);
        when(decisions.decide(any(), any())).thenReturn(
                new AgentDecision("TOOL_CALL", "search", "search_character_memory", first, null),
                new AgentDecision("TOOL_CALL", "search again", "search_character_memory", reordered, null),
                new AgentDecision("FINAL", "done", null, null, "ok"));
        when(tools.execute(anyLong(), eq("search_character_memory"), any())).thenReturn("memory-result");

        var result = service.resume(70021L, run.getId());

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getSteps().get(1).getDecisionType()).isEqualTo("DUPLICATE_TOOL_CALL");
        assertThat(result.getSteps().get(1).getToolResult()).isEqualTo("memory-result");
        verify(tools, times(1)).execute(anyLong(), eq("search_character_memory"), any());
    }

    @Test
    void sameToolWithDifferentArgumentsExecutesTwice() {
        AgentRun run = run(70022L, "PENDING", 0, 6, LocalDateTime.now());
        when(decisions.decide(any(), any())).thenReturn(
                characterDecision(3), characterDecision(4),
                new AgentDecision("FINAL", "done", null, null, "ok"));
        when(tools.execute(anyLong(), eq("get_character_context"), any())).thenReturn("first", "second");

        var result = service.resume(70022L, run.getId());

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getSteps()).extracting("decisionType")
                .containsExactly("TOOL_CALL", "TOOL_CALL", "FINAL");
        verify(tools, times(2)).execute(anyLong(), eq("get_character_context"), any());
    }

    @Test
    void differentToolsExecuteIndependently() {
        AgentRun run = run(70023L, "PENDING", 0, 6, LocalDateTime.now());
        when(decisions.decide(any(), any())).thenReturn(
                characterDecision(3),
                new AgentDecision("TOOL_CALL", "world", "get_world_context",
                        json.createObjectNode().put("worldId", 3), null),
                new AgentDecision("FINAL", "done", null, null, "ok"));
        when(tools.execute(anyLong(), anyString(), any())).thenReturn("character", "world");

        var result = service.resume(70023L, run.getId());

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        verify(tools, times(1)).execute(anyLong(), eq("get_character_context"), any());
        verify(tools, times(1)).execute(anyLong(), eq("get_world_context"), any());
    }

    @Test
    void repeatedDuplicateDecisionsStayBoundedAndNeverReexecute() {
        AgentRun run = run(70024L, "PENDING", 0, 6, LocalDateTime.now());
        AgentDecision call = characterDecision(3);
        when(decisions.decide(any(), any())).thenReturn(
                call, call, call, new AgentDecision("FINAL", "done", null, null, "ok"));
        when(tools.execute(anyLong(), anyString(), any())).thenReturn("character-result");

        var result = service.resume(70024L, run.getId());

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getSteps()).extracting("decisionType").containsExactly(
                "TOOL_CALL", "DUPLICATE_TOOL_CALL", "DUPLICATE_TOOL_CALL", "FINAL");
        assertThat(result.getSteps().subList(1, 3)).allMatch(step ->
                step.getToolAttemptCount() == 0 && "character-result".equals(step.getToolResult()));
        verify(tools, times(1)).execute(anyLong(), eq("get_character_context"), any());
    }

    @Test
    void sameCompletedCallInDifferentRunsExecutesOncePerRun() {
        AgentRun first = run(70025L, "PENDING", 0, 6, LocalDateTime.now());
        AgentRun second = run(70025L, "PENDING", 0, 6, LocalDateTime.now());
        when(decisions.decide(any(), any())).thenReturn(
                characterDecision(3), new AgentDecision("FINAL", "done", null, null, "first"),
                characterDecision(3), new AgentDecision("FINAL", "done", null, null, "second"));
        when(tools.execute(anyLong(), anyString(), any())).thenReturn("result-1", "result-2");

        assertThat(service.resume(70025L, first.getId()).getStatus()).isEqualTo("COMPLETED");
        assertThat(service.resume(70025L, second.getId()).getStatus()).isEqualTo("COMPLETED");

        verify(tools, times(2)).execute(anyLong(), eq("get_character_context"), any());
    }

    @Test
    void deterministicToolFailureDoesNotConsumeRetryAttempt() {
        AgentRun run = run(70011L, "PENDING", 0, 6, LocalDateTime.now());
        when(decisions.decide(any(), any())).thenReturn(
                new AgentDecision("TOOL_CALL", "need world", "get_world_context", json.createObjectNode().put("worldId", 1), null));
        when(tools.execute(anyLong(), anyString(), any()))
                .thenThrow(new AgentExecutionException("DENIED", "denied", false));
        var result = service.resume(70011L, run.getId());
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getSteps().get(0).getToolAttemptCount()).isEqualTo(1);
        assertThat(result.getSteps().get(0).getRetryCount()).isZero();
        verify(tools, times(1)).execute(anyLong(), anyString(), any());
    }

    @Test
    void concurrentResumeLetsOnlyOneWorkerReachLlm() throws Exception {
        AgentRun run = run(70003L, "PENDING", 0, 6, LocalDateTime.now());
        CountDownLatch enteredLlm = new CountDownLatch(1);
        CountDownLatch releaseLlm = new CountDownLatch(1);
        when(decisions.decide(any(), any())).thenAnswer(invocation -> {
            enteredLlm.countDown();
            assertThat(releaseLlm.await(5, TimeUnit.SECONDS)).isTrue();
            return new AgentDecision("FINAL", "done", null, null, "ok");
        });
        var first = workers.submit(() -> service.resume(70003L, run.getId()));
        assertThat(enteredLlm.await(5, TimeUnit.SECONDS)).isTrue();
        var second = workers.submit(() -> service.resume(70003L, run.getId()));
        assertThat(second.get(5, TimeUnit.SECONDS).getStatus()).isEqualTo("RUNNING");
        releaseLlm.countDown();
        assertThat(first.get(5, TimeUnit.SECONDS).getStatus()).isEqualTo("COMPLETED");
        verify(decisions, times(1)).decide(any(), any());
        verifyNoInteractions(tools);
    }

    @Test
    void staleTakeoverFencesOldWorkerAndPreservesToolCall() throws Exception {
        AgentRun run = run(70004L, "PENDING", 0, 6, LocalDateTime.now());
        CountDownLatch initialToolStarted = new CountDownLatch(1);
        CountDownLatch releaseInitialTool = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        when(decisions.decide(any(), any())).thenReturn(
                new AgentDecision("TOOL_CALL", "stored", "get_world_context", json.createObjectNode().put("worldId", 1), null),
                new AgentDecision("FINAL", "done", null, null, "done"));
        when(tools.execute(anyLong(), anyString(), any())).thenAnswer(invocation -> {
            if (calls.incrementAndGet() == 1) {
                initialToolStarted.countDown();
                assertThat(releaseInitialTool.await(5, TimeUnit.SECONDS)).isTrue();
                return "old result";
            }
            return "replacement result";
        });
        var oldWorker = workers.submit(() -> service.resume(70004L, run.getId()));
        assertThat(initialToolStarted.await(5, TimeUnit.SECONDS)).isTrue();
        runs.update(null, new UpdateWrapper<AgentRun>().eq("id", run.getId())
                .set("update_time", LocalDateTime.now().minusMinutes(10)));
        var replacement = workers.submit(() -> service.resume(70004L, run.getId()));
        assertThat(replacement.get(5, TimeUnit.SECONDS).getStatus()).isEqualTo("COMPLETED");
        releaseInitialTool.countDown();
        assertThat(oldWorker.get(5, TimeUnit.SECONDS).getStatus()).isEqualTo("COMPLETED");
        verify(tools, times(2)).execute(anyLong(), anyString(), any());
        AgentStep completed = steps.selectOne(new QueryWrapper<AgentStep>().eq("run_id", run.getId()).eq("step_number", 1));
        assertThat(completed.getStatus()).isEqualTo("COMPLETED");
        assertThat(completed.getToolAttemptCount()).isEqualTo(2);
        assertThat(completed.getToolResult()).isEqualTo("replacement result");
    }

    @Test
    void terminalRunCannotBeReclaimedOrFailedByOldExecution() throws Exception {
        AgentRun run = run(70005L, "PENDING", 0, 6, LocalDateTime.now());
        CountDownLatch oldLlmStarted = new CountDownLatch(1);
        CountDownLatch releaseOldLlm = new CountDownLatch(1);
        AtomicInteger decisionsMade = new AtomicInteger();
        when(decisions.decide(any(), any())).thenAnswer(invocation -> {
            if (decisionsMade.incrementAndGet() == 1) {
                oldLlmStarted.countDown();
                assertThat(releaseOldLlm.await(5, TimeUnit.SECONDS)).isTrue();
            }
            return new AgentDecision("FINAL", "done", null, null, "ok");
        });
        var oldWorker = workers.submit(() -> service.resume(70005L, run.getId()));
        assertThat(oldLlmStarted.await(5, TimeUnit.SECONDS)).isTrue();
        runs.update(null, new UpdateWrapper<AgentRun>().eq("id", run.getId())
                .set("update_time", LocalDateTime.now().minusMinutes(10)));
        var replacement = workers.submit(() -> service.resume(70005L, run.getId()));
        assertThat(replacement.get(5, TimeUnit.SECONDS).getStatus()).isEqualTo("COMPLETED");
        releaseOldLlm.countDown();
        assertThat(oldWorker.get(5, TimeUnit.SECONDS).getStatus()).isEqualTo("COMPLETED");
        assertThat(lifecycle.claim(70005L, run.getId())).isNull();
        assertThat(lifecycle.fail(70005L, run.getId(), 1, null, "OLD", "old", -1)).isFalse();
        assertThat(lifecycle.requireOwned(70005L, run.getId()).getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void concurrentRequestIdCreatesOneRunAndReturnsSameId() throws Exception {
        AgentRunCreateRequest request = request("same goal", 6);
        CountDownLatch start = new CountDownLatch(1);
        var first = workers.submit(() -> { start.await(); return creation.create(70006L, request); });
        var second = workers.submit(() -> { start.await(); return creation.create(70006L, request); });
        start.countDown();
        AgentRun a = first.get(5, TimeUnit.SECONDS);
        AgentRun b = second.get(5, TimeUnit.SECONDS);
        assertThat(a.getId()).isEqualTo(b.getId());
        assertThat(runs.selectCount(new QueryWrapper<AgentRun>().eq("user_id", 70006L)
                .eq("request_id", request.getRequestId()))).isEqualTo(1);
    }

    @Test
    void toolCompletionAndCurrentStepAdvanceRollbackTogether() {
        AgentRun run = run(70007L, "PENDING", 0, 6, LocalDateTime.now());
        int version = lifecycle.claim(70007L, run.getId()).executionVersion();
        AgentStep step = lifecycle.getOrCreateStep(70007L, run.getId(), version, 1);
        step = lifecycle.persistDecision(70007L, run.getId(), version, step,
                new AgentDecision("TOOL_CALL", "stored", "get_world_context",
                        json.createObjectNode().put("worldId", 1), null));
        step = lifecycle.reserveToolAttempt(70007L, run.getId(), version, step);
        assertThat(step).isNotNull();
        AgentRun wrongExpectation = new AgentRun();
        BeanUtils.copyProperties(lifecycle.requireOwned(70007L, run.getId()), wrongExpectation);
        wrongExpectation.setCurrentStep(99);
        AgentStep running = steps.selectById(step.getId());
        assertThatThrownBy(() -> lifecycle.completeTool(70007L, run.getId(), version,
                wrongExpectation, running, "result")).isInstanceOf(AgentExecutionException.class);
        assertThat(steps.selectById(step.getId()).getStatus()).isEqualTo("TOOL_RUNNING");
        assertThat(lifecycle.requireOwned(70007L, run.getId()).getCurrentStep()).isZero();
    }

    @Test
    void resumesPersistedDecidedCheckpointWithoutReplanning() {
        AgentRun run = run(70012L, "RUNNING", 0, 1, LocalDateTime.now().minusMinutes(10));
        AgentStep step = new AgentStep();
        step.setRunId(run.getId()); step.setStepNumber(1); step.setDecisionType("TOOL_CALL");
        step.setDecisionSummary("persisted"); step.setToolCallId("fixed-decided-tool-call");
        step.setToolName("get_world_context"); step.setToolArguments("{\"worldId\":42}");
        step.setStatus("DECIDED"); step.setRetryCount(0); step.setToolAttemptCount(0);
        step.setCreateTime(LocalDateTime.now()); step.setUpdateTime(LocalDateTime.now()); steps.insert(step);
        when(tools.execute(anyLong(), anyString(), any())).thenReturn("persisted world result");

        var result = service.resume(70012L, run.getId());

        verifyNoInteractions(decisions);
        var name = org.mockito.ArgumentCaptor.forClass(String.class);
        var arguments = org.mockito.ArgumentCaptor.forClass(com.fasterxml.jackson.databind.JsonNode.class);
        verify(tools, times(1)).execute(eq(70012L), name.capture(), arguments.capture());
        assertThat(name.getValue()).isEqualTo("get_world_context");
        com.fasterxml.jackson.databind.JsonNode persistedArguments = arguments.getValue();
        if (persistedArguments.isTextual()) {
            try { persistedArguments = json.readTree(persistedArguments.asText()); }
            catch (com.fasterxml.jackson.core.JsonProcessingException exception) { throw new AssertionError(exception); }
        }
        assertThat(persistedArguments.path("worldId").asLong()).isEqualTo(42L);
        assertThat(result.getSteps()).hasSize(1);
        assertThat(result.getSteps().get(0).getToolCallId()).isEqualTo("fixed-decided-tool-call");
        assertThat(result.getSteps().get(0).getToolAttemptCount()).isEqualTo(1);
        assertThat(result.getSteps().get(0).getToolResult()).isEqualTo("persisted world result");
        assertThat(result.getSteps().get(0).getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void maxStepsIsConfigurableAndNeverCreatesAnExtraStep() {
        assertMaxStepsBounded(70013L, 6);
        reset(decisions, tools);
        assertMaxStepsBounded(70014L, 2);
    }

    @Test
    void firstAttemptCrashLeavesExactlyOneReservableAttempt() {
        AgentRun run = run(70008L, "PENDING", 0, 6, LocalDateTime.now());
        int firstVersion = lifecycle.claim(70008L, run.getId()).executionVersion();
        AgentStep step = lifecycle.getOrCreateStep(70008L, run.getId(), firstVersion, 1);
        step = lifecycle.persistDecision(70008L, run.getId(), firstVersion, step,
                new AgentDecision("TOOL_CALL", "stored", "get_world_context", json.createObjectNode().put("worldId", 1), null));
        step = lifecycle.reserveToolAttempt(70008L, run.getId(), firstVersion, step);
        assertThat(step.getToolAttemptCount()).isEqualTo(1);
        runs.update(null, new UpdateWrapper<AgentRun>().eq("id", run.getId()).set("update_time", LocalDateTime.now().minusMinutes(10)));
        int replacementVersion = lifecycle.claim(70008L, run.getId()).executionVersion();
        step = lifecycle.recoverStaleTool(70008L, run.getId(), replacementVersion, steps.selectById(step.getId()));
        assertThat(step.getStatus()).isEqualTo("DECIDED");
        step = lifecycle.reserveToolAttempt(70008L, run.getId(), replacementVersion, step);
        assertThat(step.getToolAttemptCount()).isEqualTo(2);
        assertThat(step.getRetryCount()).isEqualTo(1);
        assertThat(lifecycle.reserveToolAttempt(70008L, run.getId(), replacementVersion, step)).isNull();
    }

    @Test
    void secondAttemptCrashFailsWithoutThirdExecution() {
        AgentRun run = run(70009L, "PENDING", 0, 6, LocalDateTime.now());
        int firstVersion = lifecycle.claim(70009L, run.getId()).executionVersion();
        AgentStep step = lifecycle.getOrCreateStep(70009L, run.getId(), firstVersion, 1);
        step = lifecycle.persistDecision(70009L, run.getId(), firstVersion, step,
                new AgentDecision("TOOL_CALL", "stored", "get_world_context", json.createObjectNode().put("worldId", 1), null));
        step = lifecycle.reserveToolAttempt(70009L, run.getId(), firstVersion, step);
        assertThat(lifecycle.recordToolFailure(70009L, run.getId(), firstVersion, step,
                new AgentExecutionException("TRANSIENT", "retry", true))).isTrue();
        step = lifecycle.reserveToolAttempt(70009L, run.getId(), firstVersion, steps.selectById(step.getId()));
        assertThat(step.getToolAttemptCount()).isEqualTo(2);
        String toolCallId = step.getToolCallId();
        runs.update(null, new UpdateWrapper<AgentRun>().eq("id", run.getId()).set("update_time", LocalDateTime.now().minusMinutes(10)));
        int replacementVersion = lifecycle.claim(70009L, run.getId()).executionVersion();
        AgentStep exhausted = lifecycle.recoverStaleTool(70009L, run.getId(), replacementVersion, steps.selectById(step.getId()));
        assertThat(exhausted.getStatus()).isEqualTo("FAILED");
        assertThat(exhausted.getErrorCode()).isEqualTo("TOOL_RETRY_EXHAUSTED");
        assertThat(exhausted.getToolCallId()).isEqualTo(toolCallId);
        AgentRun failed = lifecycle.requireOwned(70009L, run.getId());
        assertThat(failed.getStatus()).isEqualTo("FAILED");
        assertThat(failed.getLastErrorCode()).isEqualTo("TOOL_RETRY_EXHAUSTED");
        verifyNoInteractions(tools);
    }

    private AgentRunCreateRequest request(String goal, int max) {
        AgentRunCreateRequest request = new AgentRunCreateRequest();
        request.setRequestId(UUID.randomUUID().toString()); request.setGoal(goal); request.setMaxSteps(max); return request;
    }

    private void assertMaxStepsBounded(long userId, int maxSteps) {
        AgentRun run = run(userId, "PENDING", 0, maxSteps, LocalDateTime.now());
        when(decisions.decide(any(), any())).thenReturn(new AgentDecision("TOOL_CALL", "continue",
                "get_world_context", json.createObjectNode().put("worldId", 1), null));
        when(tools.execute(anyLong(), anyString(), any())).thenReturn("context");

        var result = service.resume(userId, run.getId());

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getLastErrorCode()).isEqualTo("MAX_STEPS_EXCEEDED");
        assertThat(result.getFinalResult()).isNull();
        assertThat(result.getSteps()).hasSize(maxSteps);
        assertThat(result.getSteps()).extracting("stepNumber")
                .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, maxSteps).boxed().toList());
        assertThat(result.getSteps()).noneMatch(existing -> existing.getStepNumber() == maxSteps + 1);
        verify(decisions, times(maxSteps)).decide(any(), any());
        verify(tools, times(1)).execute(anyLong(), anyString(), any());
    }

    private AgentDecision characterDecision(long characterId) {
        return new AgentDecision("TOOL_CALL", "need character", "get_character_context",
                json.createObjectNode().put("characterId", characterId), null);
    }

    private AgentRun run(long user, String status, int current, int max, LocalDateTime updated) {
        AgentRun run = new AgentRun(); run.setUserId(user); run.setRequestId(UUID.randomUUID().toString());
        run.setGoal("goal"); run.setStatus(status); run.setCurrentStep(current); run.setMaxSteps(max);
        run.setVersion(0); run.setExecutionVersion(0); run.setCreateTime(updated); run.setUpdateTime(updated); runs.insert(run); return run;
    }
}
