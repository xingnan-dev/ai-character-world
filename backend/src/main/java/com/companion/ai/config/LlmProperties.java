package com.companion.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class LlmProperties {

    private String provider = "openai-compatible";
    private String apiUrl;
    private String apiKey;
    private String modelName;
    private Double temperature;
    private Integer maxTokens;
    private Duration connectTimeout = Duration.ofSeconds(30);
    private Duration readTimeout = Duration.ofSeconds(120);
    private Mock mock = new Mock();

    @Data
    public static class Mock {
        private boolean enabled;
        private Duration delay = Duration.ofMillis(20);
    }
}
