package com.companion.image;

import com.companion.character.snapshot.CharacterSnapshot;
import com.companion.character.snapshot.CharacterSnapshotJsonMapper;
import com.companion.common.exception.BusinessException;
import com.companion.chat.SessionPersonalityResolver;
import com.companion.dto.request.CharacterCreateRequest;
import com.companion.dto.request.CharacterImageConfirmRequest;
import com.companion.dto.request.ChatSessionCreateRequest;
import com.companion.dto.request.WorldCreateRequest;
import com.companion.dto.request.WorldParticipantCreateRequest;
import com.companion.dto.request.WorldRoundCreateRequest;
import com.companion.dto.response.CharacterResponse;
import com.companion.dto.response.ChatSessionVO;
import com.companion.dto.response.WorldResponse;
import com.companion.dto.response.WorldRoundResponse;
import com.companion.entity.Avatar;
import com.companion.entity.CharacterImageGeneration;
import com.companion.entity.ChatSession;
import com.companion.entity.Personality;
import com.companion.entity.WorldParticipant;
import com.companion.entity.WorldRound;
import com.companion.mapper.AvatarMapper;
import com.companion.mapper.CharacterImageGenerationMapper;
import com.companion.mapper.ChatSessionMapper;
import com.companion.mapper.PersonalityMapper;
import com.companion.mapper.WorldParticipantMapper;
import com.companion.mapper.WorldRoundMapper;
import com.companion.service.CharacterImageGenerationService;
import com.companion.service.CharacterService;
import com.companion.service.CharacterWorldService;
import com.companion.service.ChatService;
import com.companion.service.WorldRoundService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("soft-delete-test")
@Transactional
class CharacterImageSnapshotCompatibilityIntegrationTest {
    private static final long USER_ID = 92001L;
    private static final String OLD_URL = "/generated-images/old.png";
    private static final String NEW_URL = "/generated-images/new.png";

    @Autowired private CharacterService characterService;
    @Autowired private ChatService chatService;
    @Autowired private CharacterWorldService worldService;
    @Autowired private WorldRoundService roundService;
    @Autowired private CharacterImageGenerationService imageGenerationService;
    @Autowired private CharacterImageGenerationMapper generationMapper;
    @Autowired private ChatSessionMapper chatSessionMapper;
    @Autowired private WorldParticipantMapper participantMapper;
    @Autowired private WorldRoundMapper roundMapper;
    @Autowired private AvatarMapper avatarMapper;
    @Autowired private PersonalityMapper personalityMapper;
    @Autowired private SessionPersonalityResolver personalityResolver;
    @Autowired private CharacterSnapshotJsonMapper snapshotMapper;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void createLegacyPersonalityTableMissingFromTheTestSchema() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS t_personality (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    avatar_id BIGINT,
                    source_avatar_id BIGINT,
                    is_ai_generated TINYINT DEFAULT 0,
                    name VARCHAR(50) NOT NULL,
                    template_type TINYINT DEFAULT 1,
                    core_personality VARCHAR(500),
                    identity VARCHAR(100),
                    language_style VARCHAR(200),
                    hobbies VARCHAR(500),
                    relationship VARCHAR(100),
                    system_prompt VARCHAR(4000),
                    status TINYINT DEFAULT 1,
                    deleted TINYINT DEFAULT 0,
                    create_time DATETIME,
                    update_time DATETIME
                )
                """);
    }

    @Test
    void replacingCharacterImagesPreservesOldSnapshotsAndNewSnapshotsUseNewImage() {
        CharacterResponse aiCharacter = createCharacter("AI", "snapshot-ai", OLD_URL);
        CharacterResponse userCharacter = createCharacter("USER", "snapshot-user", OLD_URL);
        CharacterResponse supportingAi = createCharacter("AI", "supporting-ai", OLD_URL);

        ChatSessionVO oldChat = chatService.createSession(
                USER_ID, new ChatSessionCreateRequest(null, aiCharacter.getId(), "old-character-chat"));
        WorldResponse oldWorld = worldService.create(
                USER_ID, worldRequest("old-world", aiCharacter.getId(), supportingAi.getId()));
        worldService.setUserCharacter(USER_ID, oldWorld.getId(), userCharacter.getId());
        WorldRoundResponse oldRound = roundService.create(
                USER_ID, oldWorld.getId(), roundRequest("old-round", "old input"));
        ChatSessionVO legacySession = createLegacyAvatarPersonalitySession();
        ChatSession legacyBefore = chatSessionMapper.selectById(legacySession.getId());
        String legacyPersonalitySnapshot = legacyBefore.getPersonalitySnapshot();
        Personality legacyPersonality = personalityResolver.resolveFromSession(legacyBefore);

        confirmNewImage(aiCharacter.getId(), "ai-image-confirmation");
        confirmNewImage(userCharacter.getId(), "user-image-confirmation");

        ChatSessionVO queriedOldChat = session(oldChat.getId());
        WorldResponse queriedOldWorld = worldService.get(USER_ID, oldWorld.getId());
        WorldRoundResponse queriedOldRound = roundService.get(USER_ID, oldWorld.getId(), oldRound.getId());
        ChatSessionVO queriedLegacy = session(legacySession.getId());
        ChatSession legacyAfter = chatSessionMapper.selectById(legacySession.getId());
        Personality resolvedLegacyAfter = personalityResolver.resolveFromSession(legacyAfter);

        assertEquals(OLD_URL, queriedOldChat.getImageUrl());
        assertEquals("snapshot identity", queriedOldChat.getIdentity());
        assertEquals("stable", queriedOldChat.getCorePersonality());
        assertEquals(OLD_URL, participant(queriedOldWorld, aiCharacter.getId()).character().imageUrl());
        assertEquals(OLD_URL, queriedOldRound.getUserCharacter().imageUrl());
        assertEquals(legacySession.getAvatarId(), queriedLegacy.getAvatarId());
        assertEquals(legacySession.getTitle(), queriedLegacy.getTitle());
        assertEquals("legacy-personality", queriedLegacy.getAvatarName());
        assertEquals("unchanged personality", queriedLegacy.getCorePersonality());
        assertEquals(legacyPersonalitySnapshot, legacyAfter.getPersonalitySnapshot());
        assertEquals(legacyPersonality.getName(), resolvedLegacyAfter.getName());
        assertEquals(legacyPersonality.getCorePersonality(), resolvedLegacyAfter.getCorePersonality());

        ChatSession rawOldChat = chatSessionMapper.selectById(oldChat.getId());
        WorldParticipant rawOldParticipant = participantMapper.selectById(
                participant(queriedOldWorld, aiCharacter.getId()).id());
        WorldRound rawOldRound = roundMapper.selectById(oldRound.getId());
        assertSnapshotUrl(rawOldChat.getCharacterSnapshot(), OLD_URL);
        assertSnapshotUrl(rawOldParticipant.getCharacterSnapshot(), OLD_URL);
        assertSnapshotUrl(rawOldRound.getUserCharacterSnapshot(), OLD_URL);

        ChatSessionVO newChat = chatService.createSession(
                USER_ID, new ChatSessionCreateRequest(null, aiCharacter.getId(), "new-character-chat"));
        WorldResponse newWorld = worldService.create(
                USER_ID, worldRequest("new-world", aiCharacter.getId(), supportingAi.getId()));
        worldService.setUserCharacter(USER_ID, newWorld.getId(), userCharacter.getId());
        WorldRoundResponse newRound = roundService.create(
                USER_ID, newWorld.getId(), roundRequest("new-round", "new input"));

        assertEquals(NEW_URL, session(newChat.getId()).getImageUrl());
        assertEquals(NEW_URL,
                participant(worldService.get(USER_ID, newWorld.getId()), aiCharacter.getId()).character().imageUrl());
        assertEquals(NEW_URL,
                roundService.get(USER_ID, newWorld.getId(), newRound.getId()).getUserCharacter().imageUrl());
        assertSnapshotUrl(chatSessionMapper.selectById(newChat.getId()).getCharacterSnapshot(), NEW_URL);
        assertSnapshotUrl(participantMapper.selectById(
                participant(newWorld, aiCharacter.getId()).id()).getCharacterSnapshot(), NEW_URL);
        assertSnapshotUrl(roundMapper.selectById(newRound.getId()).getUserCharacterSnapshot(), NEW_URL);
    }

    @Test
    void chatSessionFreezesCurrentUserCharacterSnapshot() {
        CharacterResponse ai = createCharacter("AI", "snapshot-ai-user", OLD_URL);
        CharacterResponse user = createCharacter("USER", "snapshot-me", OLD_URL);
        bindCurrentUserCharacter(user.getId());

        ChatSessionVO created = chatService.createSession(
                USER_ID, new ChatSessionCreateRequest(null, ai.getId(), "chat-user-snapshot"));

        CharacterSnapshot frozen = created.getUserCharacter();
        assertNotNull(frozen);
        assertEquals(user.getId(), frozen.sourceCharacterId());
        assertEquals("USER", frozen.characterType());
        assertEquals("snapshot-me", frozen.name());
        assertEquals(OLD_URL, frozen.imageUrl());
        assertEquals("#667eea", frozen.avatarColor());

        confirmNewImage(user.getId(), "user-snapshot-confirmation");

        ChatSessionVO queried = session(created.getId());
        assertNotNull(queried.getUserCharacter());
        assertEquals(OLD_URL, queried.getUserCharacter().imageUrl());
        assertEquals("snapshot-me", queried.getUserCharacter().name());
    }

    @Test
    void chatSessionWithoutBoundUserCharacterHasNullUserSnapshot() {
        CharacterResponse ai = createCharacter("AI", "snapshot-ai-no-user", OLD_URL);

        ChatSessionVO created = chatService.createSession(
                USER_ID, new ChatSessionCreateRequest(null, ai.getId(), "chat-no-user"));

        assertNull(created.getUserCharacter());
    }

    @Test
    void bindingAnAiCharacterAsCurrentUserCharacterIsRejected() {
        CharacterResponse ai = createCharacter("AI", "snapshot-ai-invalid-user", OLD_URL);
        bindCurrentUserCharacter(ai.getId());

        BusinessException exception = assertThrows(BusinessException.class, () -> chatService.createSession(
                USER_ID, new ChatSessionCreateRequest(null, ai.getId(), "chat-invalid-user-snapshot")));

        assertEquals("当前用户身份必须是USER Character", exception.getMessage());
    }

    @Test
    void legacyAvatarSessionIncludesBoundUserCharacterSnapshot() {
        CharacterResponse user = createCharacter("USER", "snapshot-legacy-user", OLD_URL);
        bindCurrentUserCharacter(user.getId());

        ChatSessionVO created = createLegacyAvatarPersonalitySession();

        assertNotNull(created.getUserCharacter());
        assertEquals(user.getId(), created.getUserCharacter().sourceCharacterId());
        assertEquals(OLD_URL, created.getUserCharacter().imageUrl());
        assertEquals("snapshot-legacy-user", created.getUserCharacter().name());
    }

    @Test
    void chatSessionCreationSurvivesDeletedBoundUserCharacter() {
        CharacterResponse ai = createCharacter("AI", "snapshot-ai-deleted-user", OLD_URL);
        CharacterResponse user = createCharacter("USER", "snapshot-me-deleted", OLD_URL);
        bindCurrentUserCharacter(user.getId());
        characterService.delete(USER_ID, user.getId());

        ChatSessionVO created = chatService.createSession(
                USER_ID, new ChatSessionCreateRequest(null, ai.getId(), "chat-after-user-deleted"));

        assertNotNull(created);
        assertNull(created.getUserCharacter());
    }

    private void bindCurrentUserCharacter(long characterId) {
        jdbc.update(
                "INSERT INTO t_user (id, username, password, nickname, current_user_character_id, status, deleted, create_time, update_time) VALUES (?, ?, ?, ?, ?, 1, 0, NOW(), NOW())",
                USER_ID, "snapshot-user", "pwd", "当前用户", characterId
        );
    }

    private CharacterResponse createCharacter(String type, String name, String imageUrl) {
        CharacterCreateRequest request = new CharacterCreateRequest();
        request.setCharacterType(type);
        request.setName(name);
        request.setIdentity("snapshot identity");
        request.setCorePersonality("stable");
        request.setVisualType("IMAGE");
        request.setImageUrl(imageUrl);
        request.setAvatarColor("#667eea");
        return characterService.create(USER_ID, request);
    }

    private WorldCreateRequest worldRequest(String name, long firstCharacterId, long secondCharacterId) {
        WorldCreateRequest request = new WorldCreateRequest();
        request.setName(name);
        request.setParticipants(List.of(
                participantRequest(firstCharacterId, 0), participantRequest(secondCharacterId, 1)));
        return request;
    }

    private WorldParticipantCreateRequest participantRequest(long characterId, int order) {
        WorldParticipantCreateRequest request = new WorldParticipantCreateRequest();
        request.setCharacterId(characterId);
        request.setParticipantType("AI");
        request.setDisplayOrder(order);
        return request;
    }

    private WorldRoundCreateRequest roundRequest(String requestId, String input) {
        WorldRoundCreateRequest request = new WorldRoundCreateRequest();
        request.setRequestId(requestId);
        request.setUserInput(input);
        return request;
    }

    private void confirmNewImage(long characterId, String requestId) {
        LocalDateTime now = LocalDateTime.now();
        CharacterImageGeneration generation = new CharacterImageGeneration();
        generation.setUserId(USER_ID);
        generation.setRequestId(requestId);
        generation.setRequestHash("0".repeat(64));
        generation.setPrompt("snapshot compatibility");
        generation.setStatus("SUCCEEDED");
        generation.setImagePath("new.png");
        generation.setImageUrl(NEW_URL);
        generation.setCreatedAt(now);
        generation.setUpdatedAt(now);
        assertEquals(1, generationMapper.insert(generation));

        CharacterImageConfirmRequest request = new CharacterImageConfirmRequest();
        request.setGenerationId(generation.getId());
        request.setCharacterId(characterId);
        imageGenerationService.confirm(USER_ID, request);
    }

    private ChatSessionVO createLegacyAvatarPersonalitySession() {
        LocalDateTime now = LocalDateTime.now();
        Avatar avatar = new Avatar();
        avatar.setUserId(USER_ID);
        avatar.setName("legacy-avatar");
        avatar.setStatus(1);
        avatar.setDeleted(0);
        avatar.setCreateTime(now);
        avatar.setUpdateTime(now);
        assertEquals(1, avatarMapper.insert(avatar));

        Personality personality = new Personality();
        personality.setAvatarId(avatar.getId());
        personality.setName("legacy-personality");
        personality.setCorePersonality("unchanged personality");
        personality.setStatus(1);
        personality.setDeleted(0);
        personality.setCreateTime(now);
        personality.setUpdateTime(now);
        assertEquals(1, personalityMapper.insert(personality));
        avatar.setPersonalityId(personality.getId());
        assertEquals(1, avatarMapper.updateById(avatar));

        return chatService.createSession(USER_ID, new ChatSessionCreateRequest(avatar.getId(), "legacy-chat"));
    }

    private ChatSessionVO session(long sessionId) {
        return chatService.getSessionList(USER_ID).stream()
                .filter(session -> session.getId().equals(sessionId))
                .findFirst().orElseThrow();
    }

    private ParticipantView participant(WorldResponse world, long characterId) {
        return world.getParticipants().stream()
                .filter(value -> value.getSourceCharacterId().equals(characterId))
                .map(value -> new ParticipantView(value.getId(), value.getCharacter()))
                .findFirst().orElseThrow();
    }

    private void assertSnapshotUrl(String json, String expectedUrl) {
        CharacterSnapshot snapshot = snapshotMapper.read(json);
        assertEquals(expectedUrl, snapshot.imageUrl());
    }

    private record ParticipantView(Long id, CharacterSnapshot character) { }
}
