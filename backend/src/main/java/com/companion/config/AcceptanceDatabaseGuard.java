package com.companion.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/** Fail closed before datasource creation for the acceptance profile. */
public final class AcceptanceDatabaseGuard implements EnvironmentPostProcessor, Ordered {
    @Override public void postProcessEnvironment(ConfigurableEnvironment e, SpringApplication app) {
        if (!e.acceptsProfiles("acceptance")) return;
        String url = e.getProperty("spring.datasource.url", "");
        boolean flyway = e.getProperty("spring.flyway.enabled", Boolean.class, true);
        if (!url.startsWith("jdbc:h2:mem:") || flyway)
            throw new IllegalStateException("Unsafe acceptance database: require jdbc:h2:mem: and Flyway=false");
    }
    @Override public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }
}
