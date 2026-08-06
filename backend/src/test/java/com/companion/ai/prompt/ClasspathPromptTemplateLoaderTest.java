package com.companion.ai.prompt;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClasspathPromptTemplateLoaderTest {

    private final PromptTemplateRenderer renderer = new PromptTemplateRenderer();

    @Test
    void loadsEveryRegisteredTemplateAndValidatesDeclaredVariables() {
        ClasspathPromptTemplateLoader loader = new ClasspathPromptTemplateLoader(
                new DefaultResourceLoader(), renderer
        );

        for (PromptTemplateKey key : PromptTemplateKey.values()) {
            PromptTemplateDefinition template = loader.load(key);
            assertThat(template.key()).isEqualTo(key);
            assertThat(template.version()).isEqualTo(key.version());
            assertThat(template.variables()).isEqualTo(key.variables());
            assertThat(template.content()).isNotBlank();
        }
    }

    @Test
    void cachesLoadedTemplateByKey() {
        ClasspathPromptTemplateLoader loader = new ClasspathPromptTemplateLoader(
                new DefaultResourceLoader(), renderer
        );

        PromptTemplateDefinition first = loader.load(PromptTemplateKey.CHAT_SAFETY);
        PromptTemplateDefinition second = loader.load(PromptTemplateKey.CHAT_SAFETY);

        assertThat(second).isSameAs(first);
    }

    @Test
    void reportsUnavailableResourceWithoutLeakingFilesystemDetails() {
        ClasspathPromptTemplateLoader loader = new ClasspathPromptTemplateLoader(
                new ResourceLoader() {
                    @Override
                    public Resource getResource(String location) {
                        return new ClassPathResource("missing-prompt-template.md");
                    }

                    @Override
                    public ClassLoader getClassLoader() {
                        return getClass().getClassLoader();
                    }
                },
                renderer
        );

        assertThatThrownBy(() -> loader.load(PromptTemplateKey.CHAT_SAFETY))
                .isInstanceOf(PromptTemplateException.class)
                .hasMessageContaining("CHAT_SAFETY")
                .hasMessageNotContaining("E:\\");
    }
}
