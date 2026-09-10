package com.companion.testsupport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/** Test-only fail-closed guard. It runs before datasource and Flyway auto-configuration. */
public final class TestDatabaseSafetyEnvironmentPostProcessor
        implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String url = environment.getProperty("spring.datasource.url", "");
        boolean flywayEnabled = environment.getProperty("spring.flyway.enabled", Boolean.class, true);
        if (!url.startsWith("jdbc:h2:mem:") || flywayEnabled) {
            throw new IllegalStateException(
                    "Unsafe test database configuration: tests require jdbc:h2:mem: and spring.flyway.enabled=false");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
