package com.companion.ai.prompt;

import com.companion.ai.model.LlmMessage;
import com.companion.ai.model.LlmRole;
import com.companion.entity.ChatMessage;
import com.companion.entity.Personality;
import com.companion.entity.CharacterRelationship;
import com.companion.entity.CharacterGrowth;
import com.companion.character.snapshot.CharacterSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatPromptComposerTest {

    private ChatPromptComposer composer;
    private Personality personality;

    @BeforeEach
    void setUp() {
        PromptTemplateRenderer renderer = new PromptTemplateRenderer();
        PromptTemplateLoader loader = new ClasspathPromptTemplateLoader(new DefaultResourceLoader(), renderer);
        composer = new ChatPromptComposer(loader, renderer);

        personality = new Personality();
        personality.setId(31L);
        personality.setAvatarId(21L);
        personality.setName("星瑶");
        personality.setCorePersonality("温柔体贴");
        personality.setIdentity("虚拟伴侣");
        personality.setLanguageStyle("自然亲切");
        personality.setHobbies("音乐");
        personality.setRelationship("朋友");
    }

    @Test
    void composesTemplatesHistoryAndCurrentMessageInStructuredOrder() {
        ChatMessage newer = message(12L, 2, "第二条回复");
        ChatMessage older = message(11L, 1, "第一条消息");

        ComposedChatPrompt result = composer.compose(
                10L, 20L, 21L, personality, "- 喜好：音乐", List.of(newer, older), "当前问题"
        );

        assertThat(result.messages()).extracting(LlmMessage::role)
                .containsExactly(
                        LlmRole.SYSTEM,
                        LlmRole.SYSTEM,
                        LlmRole.SYSTEM,
                        LlmRole.USER,
                        LlmRole.ASSISTANT,
                        LlmRole.USER
                );
        assertThat(result.messages()).extracting(LlmMessage::content)
                .element(3).isEqualTo("第一条消息");
        assertThat(result.messages()).extracting(LlmMessage::content)
                .element(4).isEqualTo("第二条回复");
        assertThat(result.messages().get(5).content()).isEqualTo("当前问题");
        assertThat(result.messages().get(1).content()).contains("星瑶", "温柔体贴");
        assertThat(result.messages().get(2).content()).contains("喜好：音乐");
        assertThat(result.metadata()).containsEntry("userId", 10L)
                .containsEntry("sessionId", 20L)
                .containsEntry("avatarId", 21L)
                .containsEntry("personalityId", 31L);
    }

    @Test
    void omitsMemoryMessageWhenMemoryIsBlank() {
        ComposedChatPrompt result = composer.compose(
                10L, 20L, 21L, personality, " ", List.of(), "你好"
        );

        assertThat(result.messages()).hasSize(3);
        assertThat(result.messages()).extracting(LlmMessage::role)
                .containsExactly(LlmRole.SYSTEM, LlmRole.SYSTEM, LlmRole.USER);
        assertThat(result.metadata()).doesNotContainKey("memoryPromptVersion");
    }

    @Test
    void suppliesSafeDefaultsForMissingOptionalPersonalityFields() {
        Personality incomplete = new Personality();
        incomplete.setAvatarId(21L);

        ComposedChatPrompt result = composer.compose(
                10L, 20L, 21L, incomplete, null, null, "你好"
        );

        assertThat(result.messages().get(1).content())
                .contains("未命名角色", "友善、尊重用户", "AI虚拟伴侣");
    }

    @Test
    void rejectsMissingPersonalityOrUserMessage() {
        assertThatThrownBy(() -> composer.compose(10L, 20L, 21L, null, null, List.of(), "你好"))
                .isInstanceOf(PromptTemplateException.class)
                .hasMessageContaining("Personality");
        assertThatThrownBy(() -> composer.compose(10L, 20L, 21L, personality, null, List.of(), " "))
                .isInstanceOf(PromptTemplateException.class)
                .hasMessageContaining("User message");
    }

    @Test
    void characterPromptContainsOnlyTheProvidedCharactersRelationshipState() {
        CharacterRelationship relationship = new CharacterRelationship();
        relationship.setCharacterId(71L);
        relationship.setStage("TRUSTED");
        relationship.setSummary("They communicate with earned trust.");
        relationship.setInteractionStyle("Warm and candid");
        relationship.setRecentChange("The user shared a difficult concern.");

        ComposedChatPrompt result = composer.composeCharacter(
                10L, 20L, character(71L), relationship, null, List.of(), "Can we talk?"
        );

        assertThat(result.messages()).extracting(LlmMessage::content)
                .anySatisfy(content -> assertThat(content).contains(
                        "Relationship State", "TRUSTED", "earned trust", "Warm and candid",
                        "must come from Memory or Chat History"));
        assertThat(result.metadata()).containsEntry("characterId", 71L)
                .containsEntry("relationshipPromptVersion", "1");
        assertThat(result.messages()).extracting(LlmMessage::content)
                .noneSatisfy(content -> assertThat(content).contains("Character B secret"));
    }

    @Test void characterGrowthFollowsRelationshipAndPreservesIdentityAuthority() {
        CharacterRelationship relationship = new CharacterRelationship(); relationship.setStage("TRUSTED");
        relationship.setSummary("A relationship"); relationship.setInteractionStyle("warm"); relationship.setRecentChange("none");
        CharacterGrowth growth = new CharacterGrowth(); growth.setGrowthSummary("A growth"); growth.setBehaviorAdaptation("concise");
        growth.setUserUnderstanding("direct"); growth.setGrowthDirection("continuity");
        ComposedChatPrompt result = composer.composeCharacter(10L,20L,character(71L),relationship,growth,"A memory",List.of(),"Question");
        String all=result.messages().stream().map(LlmMessage::content).reduce("",(a,b)->a+"\n"+b);
        assertThat(all.indexOf("角色设定")).isLessThan(all.indexOf("A growth"));
        assertThat(all.indexOf("A relationship")).isLessThan(all.indexOf("A growth"));
        assertThat(all).contains("最高优先级","不得根据成长状态编造记忆或关系事件","Memory 或聊天历史");
        assertThat(result.metadata()).containsEntry("growthPromptVersion","1");
    }

    private CharacterSnapshot character(Long id) {
        return new CharacterSnapshot(1, id, "AI", "Nova", null, "companion", "kind", null,
                null, "friend", "natural", CharacterSnapshot.Profile.empty(), "INITIAL", null, null, "purple");
    }

    private ChatMessage message(Long id, int role, String content) {
        ChatMessage message = new ChatMessage();
        message.setId(id);
        message.setRole(role);
        message.setContent(content);
        return message;
    }
}
