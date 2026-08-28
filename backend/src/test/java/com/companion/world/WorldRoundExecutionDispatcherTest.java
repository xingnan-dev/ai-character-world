package com.companion.world;

import com.companion.dto.response.WorldRoundResponse;
import com.companion.service.WorldRoundService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class WorldRoundExecutionDispatcherTest {

    @Test
    void acceptsOnceAndAlwaysReleasesGuardAfterWorkerCompletion() {
        List<Runnable> tasks = new ArrayList<>();
        WorldRoundService service = mock(WorldRoundService.class);
        WorldRoundExecutionDispatcher dispatcher = new WorldRoundExecutionDispatcher(tasks::add, service);
        WorldExecutionCommand command = new WorldExecutionCommand(1L, 2L, 3L);

        assertThat(dispatcher.dispatch(command, pending())).isEqualTo(WorldExecutionDispatchStatus.ACCEPTED);
        assertThat(dispatcher.dispatch(command, pending())).isEqualTo(WorldExecutionDispatchStatus.ALREADY_ACCEPTED);
        assertThat(dispatcher.isInFlight(3L)).isTrue();

        tasks.get(0).run();
        verify(service).execute(1L, 2L, 3L);
        assertThat(dispatcher.isInFlight(3L)).isFalse();
    }

    @Test
    void rejectionReturnsBusyAndReleasesGuardForRetry() {
        Executor rejecting = task -> { throw new RejectedExecutionException("full"); };
        WorldRoundService service = mock(WorldRoundService.class);
        WorldRoundExecutionDispatcher dispatcher = new WorldRoundExecutionDispatcher(rejecting, service);

        assertThat(dispatcher.dispatch(new WorldExecutionCommand(1L, 2L, 3L), pending()))
                .isEqualTo(WorldExecutionDispatchStatus.BUSY);
        assertThat(dispatcher.isInFlight(3L)).isFalse();
        verifyNoInteractions(service);
    }

    @Test
    void workerFailureReleasesGuard() {
        WorldRoundService service = mock(WorldRoundService.class);
        doThrow(new IllegalStateException("fake failure")).when(service).execute(1L, 2L, 3L);
        WorldRoundExecutionDispatcher dispatcher = new WorldRoundExecutionDispatcher(Runnable::run, service);

        assertThat(dispatcher.dispatch(new WorldExecutionCommand(1L, 2L, 3L), pending()))
                .isEqualTo(WorldExecutionDispatchStatus.ACCEPTED);
        assertThat(dispatcher.isInFlight(3L)).isFalse();
    }

    @Test
    void terminalAndLeasedRunningRoundsAreNotEnqueued() {
        List<Runnable> tasks = new ArrayList<>();
        WorldRoundService service = mock(WorldRoundService.class);
        WorldRoundExecutionDispatcher dispatcher = new WorldRoundExecutionDispatcher(tasks::add, service);
        WorldRoundResponse terminal = pending();
        terminal.setStatus("COMPLETED");
        WorldRoundResponse leased = pending();
        leased.setStatus("RUNNING");
        leased.setExecutionRecoverable(false);

        assertThat(dispatcher.dispatch(new WorldExecutionCommand(1L, 2L, 3L), terminal))
                .isEqualTo(WorldExecutionDispatchStatus.TERMINAL);
        assertThat(dispatcher.dispatch(new WorldExecutionCommand(1L, 2L, 4L), leased))
                .isEqualTo(WorldExecutionDispatchStatus.ALREADY_ACCEPTED);
        assertThat(tasks).isEmpty();
        verifyNoInteractions(service);
    }

    private WorldRoundResponse pending() {
        WorldRoundResponse response = new WorldRoundResponse();
        response.setId(3L);
        response.setWorldId(2L);
        response.setStatus("PENDING");
        response.setExecutionRecoverable(true);
        return response;
    }
}
