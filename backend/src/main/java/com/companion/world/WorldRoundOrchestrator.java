package com.companion.world;

import com.companion.ai.LlmClient;
import com.companion.ai.exception.LlmErrorType;
import com.companion.ai.exception.LlmProviderException;
import com.companion.ai.model.LlmResponse;
import com.companion.ai.prompt.WorldPromptComposer;
import com.companion.ai.prompt.WorldPromptComposer.WorldSpeech;
import com.companion.character.snapshot.CharacterSnapshotJsonMapper;
import com.companion.character.snapshot.CharacterSnapshot;
import com.companion.entity.CharacterWorld;
import com.companion.entity.WorldEvent;
import com.companion.entity.WorldRound;
import com.companion.entity.enums.WorldEventStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WorldRoundOrchestrator {

    private static final int MAX_EVENT_CONTENT_LENGTH = 60_000;

    private final WorldParticipantResolver participantResolver;
    private final WorldPromptComposer promptComposer;
    private final LlmClient llmClient;
    private final WorldRoundLifecycleService lifecycle;
    private final CharacterSnapshotJsonMapper snapshotJsonMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public WorldRoundOrchestrator(WorldParticipantResolver resolver, WorldPromptComposer composer,
                                  LlmClient llm, WorldRoundLifecycleService lifecycle,
                                  CharacterSnapshotJsonMapper mapper) {
        this.participantResolver=resolver; this.promptComposer=composer; this.llmClient=llm;
        this.lifecycle=lifecycle; this.snapshotJsonMapper=mapper;
    }

    public WorldRoundOrchestrator(WorldParticipantResolver resolver, WorldPromptComposer composer,
                                  LlmClient llm, WorldRoundLifecycleService lifecycle) {
        this(resolver, composer, llm, lifecycle, new CharacterSnapshotJsonMapper(new com.fasterxml.jackson.databind.ObjectMapper()));
    }

    public void execute(Long userId, CharacterWorld world, WorldRound round) {
        WorldExecutionClaim claim = lifecycle.claim(world.getId(), round.getId());
        if (!claim.acquired()) {
            return;
        }
        long version = claim.executionVersion();
        List<WorldActorContext> actors = participantResolver.resolveAiParticipants(world.getId());
        Map<Long, WorldEvent> existing = existingByParticipant(round.getId());

        for (int index = 0; index < actors.size(); index++) {
            WorldActorContext actor = actors.get(index);
            int sequenceNo = index + 2;
            if (existing.containsKey(actor.participantId())) {
                continue;
            }
            if (!lifecycle.renew(round.getId(), version)) {
                return;
            }
            EventResult result = actor.isValid()
                    ? invoke(userId, world, round, actor, actors, existing)
                    : EventResult.failed(actor.resolutionErrorCode());
            if (!lifecycle.saveEvent(world.getId(), round.getId(), version, sequenceNo, actor.participantId(),
                    result.content(), result.status(), result.errorCode())) {
                return;
            }
            existing = existingByParticipant(round.getId());
        }
        lifecycle.finish(round.getId(), version, actors.size());
    }

    private EventResult invoke(Long userId, CharacterWorld world, WorldRound round,
                               WorldActorContext actor, List<WorldActorContext> actors,
                               Map<Long, WorldEvent> existing) {
        try {
            CharacterSnapshot userSnapshot = round.getUserCharacterSnapshot() == null
                    ? new CharacterSnapshot(CharacterSnapshot.CURRENT_VERSION, 1L, "USER", "用户", null,
                    null, null, null, null, null, null, CharacterSnapshot.Profile.empty(), "INITIAL", null, null, null)
                    : snapshotJsonMapper.read(round.getUserCharacterSnapshot());
            LlmResponse response = llmClient.complete(promptComposer.compose(
                    userId, world, round.getId(), actor, actors,
                    userSnapshot, round.getUserInput(),
                    successfulSpeeches(actors, existing)), null);
            String content = response == null ? null : response.content();
            if (content == null || content.isBlank()) {
                return EventResult.failed("LLM_INVALID_RESPONSE");
            }
            String normalized = content.trim();
            if (normalized.length() > MAX_EVENT_CONTENT_LENGTH) {
                normalized = normalized.substring(0, MAX_EVENT_CONTENT_LENGTH);
            }
            return new EventResult(normalized, WorldEventStatus.COMPLETED.name(), null);
        } catch (LlmProviderException error) {
            return EventResult.failed("LLM_" + stable(error.getErrorType()));
        } catch (RuntimeException error) {
            return EventResult.failed("WORLD_AI_INTERNAL_ERROR");
        }
    }

    private String stable(LlmErrorType type) {
        return type == null ? "UPSTREAM_ERROR" : type.name();
    }

    private List<WorldSpeech> successfulSpeeches(List<WorldActorContext> actors,
                                                 Map<Long, WorldEvent> existing) {
        List<WorldSpeech> speeches = new ArrayList<>();
        for (WorldActorContext actor : actors) {
            WorldEvent event = existing.get(actor.participantId());
            if (event != null && WorldEventStatus.COMPLETED.name().equals(event.getStatus())) {
                speeches.add(new WorldSpeech(actor.displayName(), event.getContent()));
            }
        }
        return speeches;
    }

    private Map<Long, WorldEvent> existingByParticipant(Long roundId) {
        Map<Long, WorldEvent> result = new HashMap<>();
        for (WorldEvent event : lifecycle.events(roundId)) {
            if (event.getParticipantId() != null) {
                result.put(event.getParticipantId(), event);
            }
        }
        return result;
    }

    private record EventResult(String content, String status, String errorCode) {
        private static EventResult failed(String errorCode) {
            return new EventResult("", WorldEventStatus.FAILED.name(), errorCode);
        }
    }
}
