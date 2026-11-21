package com.companion.ai;

import com.companion.ai.memory.LlmMemoryExtractor;
import com.companion.ai.relationship.LlmRelationshipEvaluator;
import com.companion.ai.growth.LlmGrowthEvaluator;
import com.companion.ai.LlmClient;
import com.companion.ai.prompt.ComposedChatPrompt;
import com.companion.chat.ChatMessageLifecycleService;
import com.companion.character.snapshot.CharacterSnapshot;
import com.companion.dto.response.CharacterResponse;
import com.companion.entity.AiCharacter;
import com.companion.mapper.CharacterMapper;
import com.companion.mapper.ChatMessageMapper;
import com.companion.service.CharacterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("soft-delete-test")
@Transactional
class ChatPostProcessingFailureIntegrationTest {
    @Autowired AiService ai;
    @Autowired ChatMessageLifecycleService lifecycle;
    @Autowired ChatMessageMapper messages;
    @Autowired CharacterMapper characters;
    @Autowired CharacterService characterService;
    @MockBean LlmClient llm;
    @MockBean LlmMemoryExtractor memory;
    @MockBean LlmRelationshipEvaluator relationships;
    @MockBean LlmGrowthEvaluator growth;

    @Test void allPostProcessingFailuresLeaveCompletedReplyPersisted() {
        var character = new AiCharacter();
        character.setUserId(94101L); character.setCharacterType(1); character.setName("Failure isolation guide");
        character.setGenerateType(0); character.setVisualType(0); character.setStatus(1); character.setDeleted(0);
        character.setCreateTime(LocalDateTime.now()); character.setUpdateTime(LocalDateTime.now());
        characters.insert(character);
        var snapshot = CharacterSnapshot.from(characterService.get(94101L, character.getId()));
        var exchange = lifecycle.createExchange(94201L, "我叫验收青杉", "post-processing-failures");
        when(llm.streamChat(any(ComposedChatPrompt.class))).thenReturn(Flux.just("Persisted reply"));
        when(memory.extract(anyString(), anyString())).thenThrow(new IllegalStateException("memory unavailable"));
        when(relationships.evaluate(any(), anyString(), anyString())).thenThrow(new IllegalStateException("relationship unavailable"));
        when(growth.evaluate(any(), any())).thenThrow(new IllegalStateException("growth unavailable"));
        assertThat(ai.chatStream(94101L, 94201L, "我叫验收青杉", snapshot, exchange, character.getId()).collectList().block())
                .containsExactly("Persisted reply");
        var saved = messages.selectById(exchange.assistantMessageId());
        assertThat(saved.getStatus()).isEqualTo(2);
        assertThat(saved.getContent()).isEqualTo("Persisted reply");
        assertThat(saved.getErrorCode()).isNull();
        assertThat(saved.getCompletionTime()).isNotNull();
        verify(memory).extract(anyString(), anyString());
        verify(relationships).evaluate(any(), anyString(), anyString());
        verify(growth).evaluate(any(), any());
    }
}
