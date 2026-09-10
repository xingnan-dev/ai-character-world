package com.companion.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "image-generation")
public class ImageGenerationProperties {
    private String apiKey;
    private String apiUrl = "https://open.bigmodel.cn/api/paas/v4/images/generations";
    private String model = "cogview-3-flash";
    private String storageDirectory = "./generated-images";
    private String publicPath = "/generated-images/";
    private int dailyLimit = 20;
    private int perUserConcurrency = 1;
    private long maxBytes = 20L * 1024 * 1024;
}
