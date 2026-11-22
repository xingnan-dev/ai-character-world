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
    private final WorldMemoryExtractionContext context = new WorldMemoryExtractionContext("bar", "old actor", "Event 1 [AI_MESSAGE]: A door opened", "memory", java.util.Map.of(1L,"A door opened"));

    private void reply(String json) {
        json=json.replace("\"type\":", "\"sourceEventId\":1,\"evidence\":\"A door opened\",\"stance\":\"OBSERVED\",\"type\":");
        when(client.complete(anyString(), anyString(), anyInt()))
                .thenReturn(new LlmResponse(json, "mock", "m", "stop", null, null));
    }

    @Test void validExtraction() {
        reply("{\"memories\":[{\"type\":\"WORLD_EVENT\",\"key\":\"door\",\"content\":\"A door opened\",\"importance\":70}]}");
        assertThat(extractor.extract(context)).containsExactly(new WorldMemoryCandidate(com.companion.entity.enums.WorldMemoryType.WORLD_EVENT, "door", "A door opened\n[\u6765\u6e90 Event 1 \u539f\u6587] A door opened", 70, 1L));
    }
    @Test void extractionPromptDeclaresEverySupportedType() {
        reply("{\"memories\":[]}");
        extractor.extract(context);
        var system = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(client).complete(system.capture(), anyString(), anyInt());
        for (var type : com.companion.entity.enums.WorldMemoryType.values()) {
            assertThat(system.getValue()).contains(type.name());
        }
    }
    @Test void multipleMemoriesExtracted() {
        reply("{\"memories\":[{\"type\":\"WORLD_FACT\",\"content\":\"one\"},{\"type\":\"PROMISE\",\"content\":\"two\"}]}");
        assertThat(extractor.extract(context)).hasSize(2);
    }
    @Test void malformedJsonReportsFailure() { reply("not-json"); org.assertj.core.api.Assertions.assertThatThrownBy(()->extractor.extract(context)).isInstanceOf(IllegalStateException.class); }
    @Test void emptyResponseReturnsEmpty() { reply("{\"memories\":[]}"); assertThat(extractor.extract(context)).isEmpty(); }
    @Test void providerExceptionReportsFailure() { when(client.complete(anyString(),anyString(),anyInt())).thenThrow(new RuntimeException("down")); org.assertj.core.api.Assertions.assertThatThrownBy(()->extractor.extract(context)).isInstanceOf(IllegalStateException.class); }
    @Test void unknownTypeRejected() { reply("{\"memories\":[{\"type\":\"NOPE\",\"content\":\"x\"}]}"); assertThat(extractor.extract(context)).isEmpty(); }
    @Test void overlongContentRejected() { reply("{\"memories\":[{\"type\":\"WORLD_FACT\",\"content\":\"" + "x".repeat(2001) + "\"}]}"); assertThat(extractor.extract(context)).isEmpty(); }
    @Test void candidateCountBounded() {
        String item="{\"type\":\"WORLD_FACT\",\"content\":\"x\"}";
        reply("{\"memories\":[" + String.join(",", java.util.Collections.nCopies(8,item)) + "]}");
        assertThat(extractor.extract(context)).hasSize(5);
    }
}
