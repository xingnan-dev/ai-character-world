package com.companion.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Clock;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableConfigurationProperties(WorldExecutionProperties.class)
public class WorldExecutionConfig {

    @Bean
    public Clock worldClock() {
        return Clock.systemDefaultZone();
    }

    @Bean(name = "worldExecutionExecutor")
    public Executor worldExecutionExecutor(WorldExecutionProperties properties) {
        if (properties.getMaxPoolSize() < properties.getCorePoolSize()) {
            throw new IllegalStateException("app.world.execution.max-pool-size must be >= core-pool-size");
        }
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setThreadNamePrefix("world-exec-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(properties.getShutdownAwaitSeconds());
        return executor;
    }
}
