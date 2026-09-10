package com.companion.image;

import com.companion.config.ImageGenerationProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.Map;

public class OkHttpImageGenerationClient implements ImageGenerationClient {
    private final OkHttpClient http;
    private final ObjectMapper json;
    private final ImageGenerationProperties properties;

    public OkHttpImageGenerationClient(OkHttpClient http, ObjectMapper json, ImageGenerationProperties properties) {
        this.http = http;
        this.json = json;
        this.properties = properties;
    }

    @Override
    public String generate(String prompt) throws IOException {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) throw new IOException("图片生成服务未配置，请设置 ZHIPU_IMAGE_API_KEY");
        byte[] body = json.writeValueAsBytes(Map.of("model", properties.getModel(), "prompt", prompt, "size", "1024x1024"));
        Request request = new Request.Builder().url(properties.getApiUrl())
                .header("Authorization", "Bearer " + properties.getApiKey())
                .post(RequestBody.create(body, MediaType.get("application/json"))).build();
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) throw new IOException("图片生成失败，请稍后重试");
            JsonNode url = json.readTree(response.body().byteStream()).path("data").path(0).path("url");
            if (!url.isTextual() || url.asText().isBlank()) throw new IOException("图片生成返回为空");
            return url.asText();
        }
    }
}
