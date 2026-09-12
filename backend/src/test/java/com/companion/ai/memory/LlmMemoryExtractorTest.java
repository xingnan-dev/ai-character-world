package com.companion.ai.memory;

import com.companion.ai.LlmClient;
import com.companion.ai.model.LlmResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LlmMemoryExtractorTest {
    private final LlmClient client = Mockito.mock(LlmClient.class);
    private final LlmMemoryExtractor extractor = new LlmMemoryExtractor(client);

    @Test void parsesValidJson() {
        Mockito.when(client.complete(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(new LlmResponse("{\"memories\":[{\"type\":\"USER_FACT\",\"key\":\"profile.name\",\"value\":\"小明\",\"importance\":0.8}]}", "mock", "m", "stop", null, null));
        List<ExtractedMemory> result = extractor.extract("我叫小明", "");
        assertEquals(1, result.size());
        assertEquals("profile.name", result.get(0).memoryKey());
        assertEquals("小明", result.get(0).value());
    }

    @Test void malformedJsonIsSafe() {
        Mockito.when(client.complete(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(new LlmResponse("not-json", "mock", "m", "stop", null, null));
        assertTrue(extractor.extract("hello", "").isEmpty());
    }

    @Test void emptyResultIsEmpty() {
        Mockito.when(client.complete(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(new LlmResponse("{\"memories\":[]}", "mock", "m", "stop", null, null));
        assertTrue(extractor.extract("hello", "").isEmpty());
    }

    @Test void providerFailureIsSafe() {
        Mockito.when(client.complete(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt()))
                .thenThrow(new RuntimeException("provider down"));
        assertDoesNotThrow(() -> assertTrue(extractor.extract("hello", "").isEmpty()));
    }
}
