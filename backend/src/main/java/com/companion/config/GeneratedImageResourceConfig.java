package com.companion.config;
import org.springframework.context.annotation.*; import org.springframework.web.servlet.config.annotation.*; import java.nio.file.*;
@Configuration public class GeneratedImageResourceConfig implements WebMvcConfigurer {private final ImageGenerationProperties p; public GeneratedImageResourceConfig(ImageGenerationProperties p){this.p=p;} public void addResourceHandlers(ResourceHandlerRegistry r){String path=Paths.get(p.getStorageDirectory()).toAbsolutePath().normalize().toUri().toString();r.addResourceHandler(p.getPublicPath()+"**").addResourceLocations(path);}}
