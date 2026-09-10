package com.companion.config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.companion.image.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import java.nio.file.Path;
import java.time.Duration;
@Configuration
@EnableConfigurationProperties(ImageGenerationProperties.class)
public class ImageGenerationConfig {
    @Bean
    OkHttpClient imageHttpClient() {
        return new OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
                .connectTimeout(Duration.ofSeconds(30)).readTimeout(Duration.ofSeconds(120)).build();
    }

    @Bean ImageGenerationClient imageGenerationClient(OkHttpClient imageHttpClient, ObjectMapper json, ImageGenerationProperties properties) {
        return new OkHttpImageGenerationClient(imageHttpClient, json, properties);
    }

    @Bean ImageDownloadTransport imageDownloadTransport(OkHttpClient imageHttpClient) {
        return new OkHttpImageDownloadTransport(imageHttpClient);
    }

    @Bean GeneratedImageStorage generatedImageStorage(ImageDownloadTransport transport, ImageGenerationProperties properties) {
        return new GeneratedImageStorage(transport, Path.of(properties.getStorageDirectory()), properties.getMaxBytes());
    }
}
