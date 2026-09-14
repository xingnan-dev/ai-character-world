package com.companion.ai.worldmemory;

import com.companion.ai.LlmClient;
import com.companion.ai.model.LlmResponse;
import com.companion.ai.prompt.ClasspathPromptTemplateLoader;
import com.companion.ai.prompt.PromptTemplateRenderer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LlmWorldMemoryExtractorTest {
    private final LlmClient client = mock(LlmClient.class);
    private final PromptTemplateRenderer renderer = new PromptTemplateRenderer();
    private final LlmWorldMemoryExtractor extractor = new LlmWorldMemoryExtractor(client, new ObjectMapper(),
            new ClasspathPromptTemplateLoader(new DefaultResourceLoader(), renderer), renderer);
    private final WorldMemoryExtractionContext context = new WorldMemoryExtractionContext("bar", "old actor", "event", "memory");

    private void reply(String json) {
        when(client.complete(anyString(), anyString(), anyInt()))
                .thenReturn(new LlmResponse(json, "mock", "m", "stop", null, null));
    }

    @Test void validExtraction() {
        reply("{\"memories\":[{\"type\":\"WORLD_EVENT\",\"key\":\"door\",\"content\":\"A door opened\",\"importance\":70}]}");
        assertThat(extractor.extract(context)).containsExactly(new WorldMemoryCandidate(com.companion.entity.enums.WorldMemoryType.WORLD_EVENT, "door", "A door opened", 70));
    }
    @Test void multipleMemoriesExtracted() {
        reply("{\"memories\":[{\"type\":\"WORLD_FACT\",\"content\":\"one\"},{\"type\":\"PROMISE\",\"content\":\"two\"}]}");
        assertThat(extractor.extract(context)).hasSize(2);
    }
    @Test void malformedJsonReturnsEmpty() { reply("not-json"); assertThat(extractor.extract(context)).isEmpty(); }
    @Test void emptyResponseReturnsEmpty() { reply("{\"memories\":[]}"); assertThat(extractor.extract(context)).isEmpty(); }
    @Test void providerExceptionReturnsEmpty() { when(client.complete(anyString(),anyString(),anyInt())).thenThrow(new RuntimeException("down")); assertThat(extractor.extract(context)).isEmpty(); }
    @Test void unknownTypeRejected() { reply("{\"memories\":[{\"type\":\"NOPE\",\"content\":\"x\"}]}"); assertThat(extractor.extract(context)).isEmpty(); }
    @Test void overlongContentRejected() { reply("{\"memories\":[{\"type\":\"WORLD_FACT\",\"content\":\"" + "x".repeat(2001) + "\"}]}"); assertThat(extractor.extract(context)).isEmpty(); }
    @Test void candidateCountBounded() {
        String item="{\"type\":\"WORLD_FACT\",\"content\":\"x\"}";
        reply("{\"memories\":[" + String.join(",", java.util.Collections.nCopies(8,item)) + "]}");
        assertThat(extractor.extract(context)).hasSize(5);
    }
}
