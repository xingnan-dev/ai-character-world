package com.companion.ai.context;

import com.companion.ai.MemoryEngine;
import com.companion.ai.config.LlmProperties;
import com.companion.ai.model.LlmMessage;
import com.companion.ai.prompt.ChatPromptComposer;
import com.companion.ai.prompt.ClasspathPromptTemplateLoader;
import com.companion.ai.prompt.ComposedChatPrompt;
import com.companion.ai.prompt.PromptTemplateRenderer;
import com.companion.character.snapshot.CharacterSnapshot;
import com.companion.chat.model.ChatMessageExchange;
import com.companion.entity.CharacterRelationship;
import com.companion.mapper.ChatMessageMapper;
import com.companion.service.CharacterRelationshipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatContextRelationshipTest {
    private MemoryEngine memory;
    private ChatMessageMapper messages;
    private CharacterRelationshipService relationships;
    private ChatContextAssembler assembler;

    @BeforeEach void setUp() {
        memory = mock(MemoryEngine.class);
        ConversationContextManager context = mock(ConversationContextManager.class);
        messages = mock(ChatMessageMapper.class);
        relationships = mock(CharacterRelationshipService.class);
        PromptTemplateRenderer renderer = new PromptTemplateRenderer();
        ChatPromptComposer composer = new ChatPromptComposer(
                new ClasspathPromptTemplateLoader(new DefaultResourceLoader(), renderer), renderer);
        when(context.limitMemory(any())).thenAnswer(call -> call.getArgument(0));
        when(context.selectHistory(any(), any())).thenReturn(List.of());
        when(messages.selectList(any())).thenReturn(List.of());
        assembler = new ChatContextAssembler(memory, context, messages, composer, new LlmProperties(), relationships);
    }

    @Test void characterAReadsOnlyCharacterARelationship() {
        CharacterRelationship relationshipA = relationship(71L, "TRUSTED", "A-only relationship");
        when(relationships.getOrCreate(10L, 71L)).thenReturn(relationshipA);

        ComposedChatPrompt prompt = assembler.assembleCharacter(
                10L, 20L, "hello", character(71L), new ChatMessageExchange(1L, 2L, "r", true));

        assertThat(prompt.messages()).extracting(LlmMessage::content)
                .anySatisfy(content -> assertThat(content).contains("A-only relationship"))
                .noneSatisfy(content -> assertThat(content).contains("B-only relationship"));
        verify(relationships).getOrCreate(10L, 71L);
        verify(relationships, never()).getOrCreate(10L, 72L);
    }

    @Test void missingRelationshipDoesNotBreakCharacterPrompt() {
        when(relationships.getOrCreate(10L, 71L)).thenThrow(new RuntimeException("database unavailable"));
        assertThat(assembler.assembleCharacter(
                10L, 20L, "hello", character(71L), new ChatMessageExchange(1L, 2L, "r", true)))
                .isNotNull();
    }

    private CharacterRelationship relationship(Long characterId, String stage, String summary) {
        CharacterRelationship value = new CharacterRelationship();
        value.setCharacterId(characterId); value.setStage(stage); value.setSummary(summary);
        value.setInteractionStyle("warm"); value.setRecentChange("meaningful disclosure");
        return value;
    }

    private CharacterSnapshot character(Long id) {
        return new CharacterSnapshot(1, id, "AI", "Nova", null, "companion", "kind", null,
                null, "friend", "natural", CharacterSnapshot.Profile.empty(), "INITIAL", null, null, "purple");
    }
}
