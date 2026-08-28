package com.companion.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "app.world.execution")
public class WorldExecutionProperties {
    @Min(1)
    @Max(32)
    private int corePoolSize = 2;

    @Min(1)
    @Max(32)
    private int maxPoolSize = 2;

    @Min(1)
    @Max(1000)
    private int queueCapacity = 32;

    @Min(1)
    @Max(300)
    private int shutdownAwaitSeconds = 30;
}
