package com.companion.config;
import jakarta.validation.constraints.*;import lombok.Data;import org.springframework.boot.context.properties.ConfigurationProperties;import org.springframework.validation.annotation.Validated;import java.time.Duration;
@Data @Validated @ConfigurationProperties(prefix="app.agent") public class AgentProperties {
 @Min(1) @Max(10) private int defaultMaxSteps=6; @Min(1) @Max(10) private int maxStepsLimit=10;
 @NotNull private Duration staleTimeout=Duration.ofMinutes(5); @NotNull private Duration toolTimeout=Duration.ofSeconds(3);
 @Min(1) @Max(8) private int toolThreads=2; @Min(1) @Max(100) private int toolQueueCapacity=16;
}
