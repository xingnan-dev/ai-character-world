package com.companion.ai.memory;

import com.companion.ai.LlmClient;
import com.companion.ai.model.LlmResponse;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MemoryQualityRegressionTest {
    @Test void guessesAndUnexecutedPlansAreNotConfirmedUserFacts() {
        var client=mock(LlmClient.class);
        var extractor=new LlmMemoryExtractor(client);
        for(String source:java.util.List.of("我猜测代号可能是KEY-9。", "我计划明年前往月球。")) {
            when(client.complete(anyString(),anyString(),anyInt())).thenReturn(new LlmResponse(
                    "{\"memories\":[{\"type\":\"EXPERIENCE\",\"key\":\"visit\",\"value\":\"月球\",\"evidence\":\"我计划明年前往月球\",\"confidence\":1}]}","stub","stub","stop",null,null));
            assertThat(extractor.extract(source, "")).isEmpty();
        }
    }
    @Test void confirmedSentenceSurvivesIndependentUncertainSentence() {
        assertThat(MemoryInputPolicy.factualStatements("我叫小明。我猜测代号可能是KEY-9。"))
                .isEqualTo("我叫小明");
    }
    @Test void exactPreferencePhraseIsRetainedButFreeParaphraseIsExplicitlyUnsupported() {
        var client=mock(LlmClient.class);var extractor=new LlmMemoryExtractor(client);
        when(client.complete(anyString(),anyString(),anyInt())).thenReturn(new LlmResponse(
                "{\"memories\":[{\"type\":\"PREFERENCE\",\"key\":\"drink\",\"value\":\"不加糖\",\"evidence\":\"我喝咖啡时不加糖\",\"confidence\":0.9}]}","stub","stub","stop",null,null));
        assertThat(extractor.extract("我喝咖啡时不加糖。", "")).singleElement().satisfies(m->assertThat(m.value()).isEqualTo("不加糖"));
        when(client.complete(anyString(),anyString(),anyInt())).thenReturn(new LlmResponse(
                "{\"memories\":[{\"type\":\"PREFERENCE\",\"key\":\"drink\",\"value\":\"偏好无糖咖啡\",\"evidence\":\"我喝咖啡时不加糖\",\"confidence\":0.9}]}","stub","stub","stop",null,null));
        assertThat(extractor.extract("我喝咖啡时不加糖。", "")).isEmpty();
    }
}
