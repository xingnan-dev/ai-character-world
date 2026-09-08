package com.companion.image;

import com.companion.common.exception.BusinessException;
import com.companion.config.ImageGenerationProperties;
import com.companion.dto.request.CharacterImageConfirmRequest;
import com.companion.dto.request.CharacterImageGenerateRequest;
import com.companion.entity.AiCharacter;
import com.companion.entity.CharacterImageGeneration;
import com.companion.entity.enums.CharacterVisualType;
import com.companion.mapper.CharacterImageGenerationMapper;
import com.companion.mapper.CharacterMapper;
import com.companion.service.impl.CharacterImageGenerationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CharacterImageGenerationServiceTest {
    @TempDir Path temp;
    CharacterImageGenerationMapper generations;
    CharacterMapper characters;
    ImageGenerationProperties properties;
    ImageGenerationClient client;
    GeneratedImageStorage storage;

    @BeforeEach void setUp() throws Exception {
        generations = mock(CharacterImageGenerationMapper.class);
        characters = mock(CharacterMapper.class);
        properties = new ImageGenerationProperties();
        properties.setDailyLimit(2);
        properties.setPerUserConcurrency(1);
        client = mock(ImageGenerationClient.class);
        storage = mock(GeneratedImageStorage.class);
        when(generations.selectCount(any())).thenReturn(0L);
        when(generations.insert(any())).thenAnswer(invocation -> { ((CharacterImageGeneration) invocation.getArgument(0)).setId(9L); return 1; });
        doReturn("https://203.0.113.10/image").when(client).generate(any());
        when(storage.download(any())).thenReturn(temp.resolve("saved.png"));
    }

    @Test void successPersistsSucceededRecord() throws Exception {
        var response = service().generate(1L, request("r1", " portrait "));
        assertEquals("SUCCEEDED", response.getStatus());
        assertEquals("/generated-images/saved.png", response.getImageUrl());
        verify(client).generate("portrait");
        verify(generations).updateById(any());
    }

    @Test void upstreamTimeoutAndDownloadFailurePersistFailedAndConsumeQuota() throws Exception {
        when(client.generate(any())).thenThrow(new IOException("timeout"));
        assertEquals("FAILED", service().generate(1L, request("r1", "p")).getStatus());
        verify(generations).insert(any());
        reset(client); clearInvocations(generations);
        when(client.generate(any())).thenReturn("https://203.0.113.10/image");
        when(storage.download(any())).thenThrow(new IOException("download failed"));
        assertEquals("FAILED", service().generate(1L, request("r2", "p")).getStatus());
        verify(generations).insert(any());
    }

    @Test void sameRequestReturnsStoredResultAndDifferentContentConflicts() {
        CharacterImageGeneration old = record(8L, "r1", "p", "SUCCEEDED", "/old.png");
        when(generations.selectOne(any())).thenReturn(old);
        assertEquals(8L, service().generate(1L, request("r1", "p")).getId());
        assertThrows(BusinessException.class, () -> service().generate(1L, request("r1", "different")));
        verifyNoInteractions(client, storage);
    }

    @Test void dailyLimitCountsFailuresAndTimeouts() {
        when(generations.selectCount(any())).thenReturn(2L);
        assertThrows(BusinessException.class, () -> service().generate(1L, request("r", "p")));
        verifyNoInteractions(client, storage);
    }

    @Test void concurrentRequestIsRejectedAndFailureReleasesPermit() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        when(client.generate(any())).thenAnswer(invocation -> { entered.countDown(); release.await(2, TimeUnit.SECONDS); throw new IOException("timeout"); });
        var service = service();
        var executor = Executors.newSingleThreadExecutor();
        var first = executor.submit(() -> service.generate(1L, request("r1", "p")));
        assertTrue(entered.await(2, TimeUnit.SECONDS));
        assertThrows(BusinessException.class, () -> service.generate(1L, request("r2", "p")));
        release.countDown();
        assertEquals("FAILED", first.get(2, TimeUnit.SECONDS).getStatus());
        doReturn("https://203.0.113.10/image").when(client).generate(any());
        assertEquals("SUCCEEDED", service.generate(1L, request("r3", "p")).getStatus());
        executor.shutdownNow();
    }

    @Test void crossUserGenerationLookupAndConfirmCannotReadOrBindAnotherUsersData() throws Exception {
        when(generations.selectOne(any())).thenReturn(null);
        service().generate(2L, request("same-id", "p"));
        verify(client).generate("p");
        when(generations.selectOne(any())).thenReturn(null);
        assertThrows(BusinessException.class, () -> service().confirm(2L, confirm(9L, 10L)));
        verifyNoInteractions(characters);
    }

    @Test void confirmBindsImageTypeAndUrlWhileRepeatedConfirmIsIdempotent() {
        CharacterImageGeneration generation = record(9L, "r", "p", "SUCCEEDED", "/generated-images/x.png");
        AiCharacter character = new AiCharacter(); character.setId(10L); character.setUserId(1L); character.setStatus(1);
        when(generations.selectOne(any())).thenReturn(generation);
        when(characters.selectOne(any())).thenReturn(character);
        var service = service();
        service.confirm(1L, confirm(9L, 10L));
        assertEquals(CharacterVisualType.IMAGE.getCode(), character.getVisualType());
        assertEquals("/generated-images/x.png", character.getImageUrl());
        service.confirm(1L, confirm(9L, 10L));
        verify(characters, times(1)).updateById(character);
    }

    @Test void confirmedGenerationCannotBeReboundToAnotherCharacter() {
        CharacterImageGeneration generation = record(9L, "r", "p", "SUCCEEDED", "/x.png");
        generation.setCharacterId(10L); generation.setConfirmedAt(java.time.LocalDateTime.now());
        when(generations.selectOne(any())).thenReturn(generation);
        assertThrows(BusinessException.class, () -> service().confirm(1L, confirm(9L, 11L)));
        verifyNoInteractions(characters);
    }

    private CharacterImageGenerationServiceImpl service() { return new CharacterImageGenerationServiceImpl(generations, characters, properties, client, storage); }
    private CharacterImageGenerateRequest request(String id, String prompt) { var r = new CharacterImageGenerateRequest(); r.setRequestId(id); r.setPrompt(prompt); return r; }
    private CharacterImageConfirmRequest confirm(Long generationId, Long characterId) { var r = new CharacterImageConfirmRequest(); r.setGenerationId(generationId); r.setCharacterId(characterId); return r; }
    private CharacterImageGeneration record(Long id, String requestId, String prompt, String status, String url) {
        var r = new CharacterImageGeneration(); r.setId(id); r.setRequestId(requestId); r.setRequestHash(hash(prompt)); r.setPrompt(prompt); r.setStatus(status); r.setImageUrl(url); return r;
    }
    private String hash(String value) { try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); } catch (Exception e) { throw new AssertionError(e); } }
}
