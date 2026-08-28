package com.companion.world;

import com.companion.dto.response.WorldRoundResponse;
import com.companion.service.WorldRoundService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Component
public class WorldRoundExecutionDispatcher {

    private final Executor executor;
    private final WorldRoundService roundService;
    private final Set<Long> inFlightRoundIds = ConcurrentHashMap.newKeySet();

    public WorldRoundExecutionDispatcher(
            @Qualifier("worldExecutionExecutor") Executor executor,
            WorldRoundService roundService) {
        this.executor = executor;
        this.roundService = roundService;
    }

    public WorldExecutionDispatchStatus dispatch(WorldExecutionCommand command, WorldRoundResponse round) {
        if (isTerminal(round.getStatus())) {
            return WorldExecutionDispatchStatus.TERMINAL;
        }
        if ("RUNNING".equals(round.getStatus()) && !round.isExecutionRecoverable()) {
            return WorldExecutionDispatchStatus.ALREADY_ACCEPTED;
        }
        if (!inFlightRoundIds.add(command.roundId())) {
            return WorldExecutionDispatchStatus.ALREADY_ACCEPTED;
        }
        try {
            executor.execute(() -> execute(command));
            return WorldExecutionDispatchStatus.ACCEPTED;
        } catch (RejectedExecutionException error) {
            inFlightRoundIds.remove(command.roundId());
            return WorldExecutionDispatchStatus.BUSY;
        } catch (RuntimeException error) {
            inFlightRoundIds.remove(command.roundId());
            throw error;
        }
    }

    boolean isInFlight(Long roundId) {
        return inFlightRoundIds.contains(roundId);
    }

    private void execute(WorldExecutionCommand command) {
        try {
            roundService.execute(command.userId(), command.worldId(), command.roundId());
        } catch (RuntimeException error) {
            log.error("World round background execution failed: worldId={}, roundId={}",
                    command.worldId(), command.roundId(), error);
        } finally {
            inFlightRoundIds.remove(command.roundId());
        }
    }

    private boolean isTerminal(String status) {
        return "COMPLETED".equals(status) || "PARTIAL_FAILED".equals(status) || "FAILED".equals(status);
    }
}
