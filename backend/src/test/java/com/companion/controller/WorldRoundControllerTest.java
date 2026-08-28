package com.companion.controller;

import com.companion.dto.response.WorldExecutionDispatchResponse;
import com.companion.dto.response.WorldRoundResponse;
import com.companion.security.AuthenticatedUser;
import com.companion.service.WorldRoundService;
import com.companion.world.WorldExecutionCommand;
import com.companion.world.WorldExecutionDispatchStatus;
import com.companion.world.WorldRoundExecutionDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorldRoundControllerTest {

    @Test
    void mapsAcceptedDispatchToHttp202() {
        WorldRoundService service = mock(WorldRoundService.class);
        WorldRoundExecutionDispatcher dispatcher = mock(WorldRoundExecutionDispatcher.class);
        WorldRoundResponse round = pendingRound();
        when(service.get(1L, 2L, 3L)).thenReturn(round);
        when(dispatcher.dispatch(any(WorldExecutionCommand.class), any(WorldRoundResponse.class)))
                .thenReturn(WorldExecutionDispatchStatus.ACCEPTED);
        WorldRoundController controller = new WorldRoundController(service, dispatcher);

        ResponseEntity<?> response = controller.execute(user(), 2L, 3L);

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        WorldExecutionDispatchResponse data = (WorldExecutionDispatchResponse)
                ((com.companion.common.result.Result<?>) response.getBody()).getData();
        assertThat(data.getDispatchStatus()).isEqualTo("ACCEPTED");
        assertThat(data.getRound()).isSameAs(round);
    }

    @Test
    void mapsQueueRejectionToStableBusyResponse() {
        WorldRoundService service = mock(WorldRoundService.class);
        WorldRoundExecutionDispatcher dispatcher = mock(WorldRoundExecutionDispatcher.class);
        when(service.get(1L, 2L, 3L)).thenReturn(pendingRound());
        when(dispatcher.dispatch(any(WorldExecutionCommand.class), any(WorldRoundResponse.class)))
                .thenReturn(WorldExecutionDispatchStatus.BUSY);
        WorldRoundController controller = new WorldRoundController(service, dispatcher);

        ResponseEntity<?> response = controller.execute(user(), 2L, 3L);

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        com.companion.common.result.Result<?> body = (com.companion.common.result.Result<?>) response.getBody();
        assertThat(body.getCode()).isEqualTo(503);
        assertThat(body.getMsg()).isEqualTo("WORLD_EXECUTION_BUSY");
        assertThat(body.getData()).isNull();
    }

    private AuthenticatedUser user() {
        return new AuthenticatedUser(1L, "owner", "USER", "token");
    }

    private WorldRoundResponse pendingRound() {
        WorldRoundResponse round = new WorldRoundResponse();
        round.setId(3L);
        round.setWorldId(2L);
        round.setStatus("PENDING");
        round.setExecutionRecoverable(true);
        return round;
    }
}
