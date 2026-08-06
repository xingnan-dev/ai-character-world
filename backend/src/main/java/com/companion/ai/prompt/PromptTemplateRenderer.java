package com.companion.ai.prompt;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PromptTemplateRenderer {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([A-Za-z][A-Za-z0-9_.-]*)}}");

    public String render(PromptTemplateDefinition template, Map<String, ?> values) {
        if (template == null) {
            throw new PromptTemplateException("Prompt template is required");
        }
        Map<String, ?> safeValues = values == null ? Map.of() : values;
        Set<String> missing = new LinkedHashSet<>();
        for (String variable : template.variables()) {
            if (!safeValues.containsKey(variable) || safeValues.get(variable) == null) {
                missing.add(variable);
            }
        }
        if (!missing.isEmpty()) {
            throw new PromptTemplateException(
                    "Missing prompt variables for " + template.key().name() + ": " + missing
            );
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template.content());
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            Object value = safeValues.get(matcher.group(1));
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(String.valueOf(value)));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    public Set<String> findVariables(String content) {
        if (content == null || content.isEmpty()) {
            return Set.of();
        }
        Set<String> variables = new LinkedHashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return Collections.unmodifiableSet(variables);
    }
}
