package com.companion.world;

import com.companion.ai.prompt.ClasspathPromptTemplateLoader;
import com.companion.ai.prompt.ComposedChatPrompt;
import com.companion.ai.prompt.PromptTemplateRenderer;
import com.companion.ai.prompt.WorldPromptComposer;
import com.companion.character.snapshot.CharacterSnapshot;
import com.companion.entity.CharacterWorld;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorldPromptComposerTest {

    @Test
    void composesWorldSnapshotAndNamedTranscriptWithoutChatDependencies() {
        PromptTemplateRenderer renderer = new PromptTemplateRenderer();
        WorldPromptComposer composer = new WorldPromptComposer(
                new ClasspathPromptTemplateLoader(new DefaultResourceLoader(), renderer), renderer);
        CharacterWorld world = new CharacterWorld();
        world.setId(3L);
        world.setName("海港");
        world.setBackground("暴雨中的港口");
        world.setRules("不能离开灯塔");
        WorldActorContext actor = actor(10L, 1, "林");
        WorldActorContext other = actor(11L, 2, "周");

        ComposedChatPrompt prompt = composer.compose(7L, world, 8L, other, List.of(actor, other),
                "发生了什么？", List.of(new WorldPromptComposer.WorldSpeech("林", "船回来了。")));

        assertThat(prompt.messages()).hasSize(2);
        assertThat(prompt.messages().get(0).content())
                .contains("暴雨中的港口", "不能离开灯塔", "当前角色的不可变快照", "姓名=周", "- 林", "- 周");
        assertThat(prompt.messages().get(1).content())
                .contains("用户：发生了什么？", "林：船回来了。", "轮到“周”");
        assertThat(prompt.metadata()).containsEntry("worldId", 3L)
                .containsEntry("roundId", 8L).containsEntry("participantId", 11L);
    }

    private WorldActorContext actor(long id, int order, String name) {
        CharacterSnapshot snapshot = new CharacterSnapshot(1, id, "AI", name, null,
                "守望者", "冷静", "守住港口", null, "伙伴", "简洁",
                CharacterSnapshot.Profile.empty(), "INITIAL", null, null, "#000000");
        return new WorldActorContext(id, 3L, order, snapshot, null);
    }
}
