package com.companion.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class GeneratedImageResourceConfig implements WebMvcConfigurer {
    private final ImageGenerationProperties properties;

    public GeneratedImageResourceConfig(ImageGenerationProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String configuredDirectory = properties.getStorageDirectory();
        if (configuredDirectory == null || configuredDirectory.isBlank()) {
            throw new IllegalStateException("image-generation.storage-directory must not be blank");
        }
        Path directory = Path.of(configuredDirectory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(directory);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot create image storage directory: " + directory, error);
        }
        registry.addResourceHandler(properties.getPublicPath() + "**")
                .addResourceLocations(directory.toUri().toString());
    }
}
