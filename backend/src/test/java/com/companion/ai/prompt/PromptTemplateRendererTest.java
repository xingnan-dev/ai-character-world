package com.companion.ai.prompt;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromptTemplateRendererTest {

    private final PromptTemplateRenderer renderer = new PromptTemplateRenderer();

    @Test
    void rendersAllVariablesIncludingRegexReplacementCharacters() {
        PromptTemplateDefinition template = template(
                "角色：{{name}}；关系：{{relationship}}",
                Set.of("name", "relationship")
        );

        String result = renderer.render(template, Map.of(
                "name", "$1\\伙伴",
                "relationship", "朋友"
        ));

        assertThat(result).isEqualTo("角色：$1\\伙伴；关系：朋友");
    }

    @Test
    void rejectsMissingVariableAndNamesItInTheError() {
        PromptTemplateDefinition template = template(
                "{{name}} / {{identity}}",
                Set.of("name", "identity")
        );

        assertThatThrownBy(() -> renderer.render(template, Map.of("name", "星瑶")))
                .isInstanceOf(PromptTemplateException.class)
                .hasMessageContaining("identity")
                .hasMessageContaining("CHAT_PERSONALITY");
    }

    @Test
    void rejectsNullVariableValue() {
        PromptTemplateDefinition template = template("{{name}}", Set.of("name"));
        Map<String, Object> values = new java.util.HashMap<>();
        values.put("name", null);

        assertThatThrownBy(() -> renderer.render(template, values))
                .isInstanceOf(PromptTemplateException.class)
                .hasMessageContaining("name");
    }

    @Test
    void performsSinglePassAndDoesNotInterpretPlaceholdersInsideValues() {
        PromptTemplateDefinition template = template("消息：{{content}}", Set.of("content"));

        String result = renderer.render(template, Map.of("content", "保留 {{nested}} 原文"));

        assertThat(result).isEqualTo("消息：保留 {{nested}} 原文");
    }

    @Test
    void discoversDistinctVariablesInDeclarationOrder() {
        assertThat(renderer.findVariables("{{name}} {{identity}} {{name}}"))
                .containsExactly("name", "identity");
    }

    private PromptTemplateDefinition template(String content, Set<String> variables) {
        return new PromptTemplateDefinition(
                PromptTemplateKey.CHAT_PERSONALITY,
                "test",
                content,
                variables
        );
    }
}
