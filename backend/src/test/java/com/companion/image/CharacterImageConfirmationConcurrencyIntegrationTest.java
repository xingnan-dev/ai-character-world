package com.companion.image;

import com.companion.common.exception.BusinessException;
import com.companion.dto.request.CharacterImageConfirmRequest;
import com.companion.entity.AiCharacter;
import com.companion.entity.CharacterImageGeneration;
import com.companion.entity.enums.CharacterVisualType;
import com.companion.mapper.CharacterImageGenerationMapper;
import com.companion.mapper.CharacterMapper;
import com.companion.service.CharacterImageGenerationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("soft-delete-test")
class CharacterImageConfirmationConcurrencyIntegrationTest {
    private static final long USER_ID = 91001L;
    private static final String IMAGE_URL = "/generated-images/concurrent.png";

    @Autowired private CharacterImageGenerationService service;
    @Autowired private CharacterImageGenerationMapper generations;
    @Autowired private CharacterMapper characters;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbc;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        jdbc.update("DELETE FROM t_character_image_generation");
        jdbc.update("DELETE FROM t_character");
    }

    @AfterEach
    void tearDown() throws Exception {
        jdbc.execute("ALTER TABLE t_character DROP CONSTRAINT IF EXISTS ck_confirmation_character_update");
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void sameGenerationConfirmedConcurrentlyToSameCharacterIsIdempotent() throws Exception {
        AiCharacter character = insertCharacter("same-target");
        CharacterImageGeneration generation = insertGeneration(IMAGE_URL);

        List<Attempt> attempts = confirmConcurrently(generation.getId(), character.getId(), character.getId());

        assertEquals(2, attempts.stream().filter(Attempt::succeeded).count());
        assertIndependentConnections(attempts);
        assertConfirmedState(generation.getId(), character.getId(), IMAGE_URL);
    }

    @Test
    void sameGenerationConfirmedConcurrentlyToDifferentCharactersAllowsOnlyOneWinner() throws Exception {
        AiCharacter first = insertCharacter("first-target");
        AiCharacter second = insertCharacter("second-target");
        CharacterImageGeneration generation = insertGeneration(IMAGE_URL);

        List<Attempt> attempts = confirmConcurrently(generation.getId(), first.getId(), second.getId());

        assertEquals(1, attempts.stream().filter(Attempt::succeeded).count());
        assertEquals(1, attempts.stream().filter(a -> a.error() instanceof BusinessException).count());
        assertIndependentConnections(attempts);

        CharacterImageGeneration confirmed = generations.selectById(generation.getId());
        assertNotNull(confirmed.getConfirmedAt());
        assertTrue(confirmed.getCharacterId().equals(first.getId()) || confirmed.getCharacterId().equals(second.getId()));
        assertConfirmedState(generation.getId(), confirmed.getCharacterId(), IMAGE_URL);

        Long losingId = confirmed.getCharacterId().equals(first.getId()) ? second.getId() : first.getId();
        AiCharacter loser = characters.selectById(losingId);
        assertEquals(0, loser.getVisualType());
        assertNull(loser.getImageUrl());
    }

    @Test
    void characterUpdateFailureRollsBackGenerationConfirmation() {
        AiCharacter character = insertCharacter("rollback-target");
        String rejectedUrl = "/generated-images/rollback.png";
        CharacterImageGeneration generation = insertGeneration(rejectedUrl);
        jdbc.execute("ALTER TABLE t_character ADD CONSTRAINT ck_confirmation_character_update " +
                "CHECK (image_url IS NULL OR image_url <> '/generated-images/rollback.png')");

        assertThrows(RuntimeException.class,
                () -> service.confirm(USER_ID, confirmation(generation.getId(), character.getId())));

        CharacterImageGeneration unchangedGeneration = generations.selectById(generation.getId());
        assertNull(unchangedGeneration.getCharacterId());
        assertNull(unchangedGeneration.getConfirmedAt());
        AiCharacter unchangedCharacter = characters.selectById(character.getId());
        assertEquals(0, unchangedCharacter.getVisualType());
        assertNull(unchangedCharacter.getImageUrl());
    }

    private List<Attempt> confirmConcurrently(long generationId, long firstCharacterId, long secondCharacterId)
            throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        Future<Attempt> first = executor.submit(() -> confirmInIndependentTransaction(
                generationId, firstCharacterId, barrier));
        Future<Attempt> second = executor.submit(() -> confirmInIndependentTransaction(
                generationId, secondCharacterId, barrier));
        return List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
    }

    private Attempt confirmInIndependentTransaction(long generationId, long characterId, CyclicBarrier barrier) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction.execute(status -> {
            Connection connection = DataSourceUtils.getConnection(dataSource);
            try {
                barrier.await(5, TimeUnit.SECONDS);
                service.confirm(USER_ID, confirmation(generationId, characterId));
                return new Attempt(connection, null);
            } catch (Exception error) {
                status.setRollbackOnly();
                return new Attempt(connection, error);
            }
        });
    }

    private void assertIndependentConnections(List<Attempt> attempts) {
        assertEquals(2, attempts.size());
        assertNotSame(attempts.get(0).connection(), attempts.get(1).connection());
    }

    private void assertConfirmedState(long generationId, long characterId, String expectedImageUrl) {
        CharacterImageGeneration confirmed = generations.selectById(generationId);
        assertEquals(characterId, confirmed.getCharacterId());
        assertNotNull(confirmed.getConfirmedAt());
        AiCharacter winner = characters.selectById(characterId);
        assertEquals(CharacterVisualType.IMAGE.getCode(), winner.getVisualType());
        assertEquals(expectedImageUrl, winner.getImageUrl());
    }

    private AiCharacter insertCharacter(String name) {
        AiCharacter character = new AiCharacter();
        character.setUserId(USER_ID);
        character.setCharacterType(1);
        character.setName(name);
        character.setGenerateType(0);
        character.setVisualType(0);
        character.setStatus(1);
        character.setDeleted(0);
        character.setCreateTime(LocalDateTime.now());
        character.setUpdateTime(LocalDateTime.now());
        assertEquals(1, characters.insert(character));
        return character;
    }

    private CharacterImageGeneration insertGeneration(String imageUrl) {
        LocalDateTime now = LocalDateTime.now();
        CharacterImageGeneration generation = new CharacterImageGeneration();
        generation.setUserId(USER_ID);
        generation.setRequestId("request-" + System.nanoTime());
        generation.setRequestHash("0".repeat(64));
        generation.setPrompt("portrait");
        generation.setStatus("SUCCEEDED");
        generation.setImagePath("generated.png");
        generation.setImageUrl(imageUrl);
        generation.setCreatedAt(now);
        generation.setUpdatedAt(now);
        assertEquals(1, generations.insert(generation));
        return generation;
    }

    private CharacterImageConfirmRequest confirmation(long generationId, long characterId) {
        CharacterImageConfirmRequest request = new CharacterImageConfirmRequest();
        request.setGenerationId(generationId);
        request.setCharacterId(characterId);
        return request;
    }

    private record Attempt(Connection connection, Exception error) {
        boolean succeeded() {
            return error == null;
        }
    }
}
