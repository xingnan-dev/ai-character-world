package com.companion.ai.worldmemory;

import com.companion.ai.LlmClient;
import com.companion.ai.model.LlmResponse;
import com.companion.ai.prompt.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class WorldMemoryEvidenceTest {
    @Test void inventedFactWithoutSourceIsRejected() {
        var client = mock(LlmClient.class);
        when(client.complete(anyString(), anyString(), anyInt())).thenReturn(new LlmResponse(
                "{\"memories\":[{\"type\":\"WORLD_EVENT\",\"content\":\"The tower exploded\"}]}", "stub", "stub", "stop", null, null));
        var renderer = new PromptTemplateRenderer();
        var extractor = new LlmWorldMemoryExtractor(client, new ObjectMapper(),
                new ClasspathPromptTemplateLoader(new DefaultResourceLoader(), renderer), renderer);
        assertThat(extractor.extract(new WorldMemoryExtractionContext("Observatory", "Archivist", "The box was sealed.", ""))).isEmpty();
    }

    private java.util.List<WorldMemoryCandidate> extract(String source, String type, String stance, String summary, String evidence, long id) throws Exception {
        var client=mock(LlmClient.class);
        var mapper=new ObjectMapper();
        String json=mapper.writeValueAsString(java.util.Map.of("memories",java.util.List.of(java.util.Map.of("type",type,"stance",stance,"content",summary,"sourceEventId",id,"evidence",evidence))));
        when(client.complete(anyString(),anyString(),anyInt())).thenReturn(new LlmResponse(json,"stub","stub","stop",null,null));
        var renderer=new PromptTemplateRenderer();
        var extractor=new LlmWorldMemoryExtractor(client,mapper,new ClasspathPromptTemplateLoader(new DefaultResourceLoader(),renderer),renderer);
        return extractor.extract(new WorldMemoryExtractionContext("world","actor","Event 7 [AI_MESSAGE]: "+source,"",java.util.Map.of(7L,source)));
    }
    @Test void enactedAiStoryAllowsReasonableParaphrase() throws Exception {
        assertThat(extract("I placed the crystal in the box and sealed it.","WORLD_EVENT","OBSERVED","The crystal is secured in a sealed box.","placed the crystal in the box and sealed it",7))
            .singleElement().satisfies(m->{assertThat(m.sourceEventId()).isEqualTo(7);assertThat(m.content()).contains("secured", "来源 Event 7");});
    }
    @Test void unexecutedPlanCannotBecomeCompletedEventEvenWhenModelClaimsObserved() throws Exception {
        assertThat(extract("I plan to open the tower tomorrow.","WORLD_EVENT","OBSERVED","The tower was opened.","open the tower",7)).isEmpty();
    }
    @Test void promiseRetainsNotCompletedMarker() throws Exception {
        assertThat(extract("I plan to open the tower tomorrow.","PROMISE","PLAN","Open the tower tomorrow.","plan to open the tower tomorrow",7)).singleElement().satisfies(m->assertThat(m.content()).contains("尚不代表已完成"));
    }
    @Test void questionAndHypothesisCannotBecomeConfirmedDiscovery() throws Exception {
        for(String source:java.util.List.of("Is there a dragon?","If a dragon lived there, it might fly.","据说北塔有龙。","假设北塔有龙。"))
            assertThat(extract(source,"DISCOVERY","OBSERVED","A dragon lives in the tower.",source,7)).isEmpty();
    }
    @Test void rumorMayBeStoredOnlyAsExplicitUnconfirmedState() throws Exception {
        assertThat(extract("传闻北塔有龙，未经证实。","WORLD_STATE","UNCERTAIN","北塔有龙的传闻尚未证实。","传闻北塔有龙",7)).singleElement().satisfies(m->assertThat(m.content()).contains("未证实的说法，不代表已发生"));
    }
    @Test void inventedQuoteOrForeignEventCannotSupportNewFact() throws Exception {
        assertThat(extract("The box was sealed.","WORLD_EVENT","OBSERVED","The tower exploded.","The tower exploded.",7)).isEmpty();
        assertThat(extract("The box was sealed.","WORLD_EVENT","OBSERVED","The box is sealed.","The box was sealed.",99)).isEmpty();
    }
    @Test void arbitraryModelConfidenceDoesNotReplaceEvidence() throws Exception {
        assertThat(extract("The box was sealed.","WORLD_EVENT","OBSERVED","The tower exploded.","",7)).isEmpty();
    }
    @Test void uncertaintyPrefixCannotTurnEmptyContentIntoValidMemory() throws Exception {
        assertThat(extract("Maybe a dragon lives there.","WORLD_STATE","UNCERTAIN","","Maybe a dragon lives there.",7)).isEmpty();
    }
}
