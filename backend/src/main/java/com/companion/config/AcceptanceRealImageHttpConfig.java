package com.companion.config;

import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

/** Real image HTTP boundary for the explicitly opted-in acceptance run. */
@Configuration
@Profile("acceptance")
@ConditionalOnProperty(name = "acceptance.image.fake.enabled", havingValue = "false")
public class AcceptanceRealImageHttpConfig {
    @Bean
    @Primary
    OkHttpClient acceptanceRealImageHttpClient() {
        return new OkHttpClient.Builder()
                .retryOnConnectionFailure(false)
                .followRedirects(false)
                .followSslRedirects(false)
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(120))
                .build();
    }
}
