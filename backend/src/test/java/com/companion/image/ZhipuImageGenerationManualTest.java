package com.companion.image;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Explicitly gated, real Zhipu image generation smoke test.
 *
 * Run manually from backend/ with:
 * mvn -Dtest=ZhipuImageGenerationManualTest -Dzhipu.image.smoke=true test
 *
 * Required environment variable: ZHIPU_API_KEY
 */
class ZhipuImageGenerationManualTest {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String DEFAULT_API_URL =
            "https://open.bigmodel.cn/api/paas/v4/images/generations";
    private static final String DEFAULT_MODEL = "cogview-3-flash";
    private static final String DEFAULT_SIZE = "864x1152";
    private static final Path DEFAULT_OUTPUT_DIRECTORY =
            Path.of("E:\\3Dchat-data\\generated-images");
    private static final long MAX_RESPONSE_BYTES = 1L * 1024 * 1024;
    private static final long MAX_IMAGE_BYTES = 20L * 1024 * 1024;
    private static final String PROMPT = "二维动漫角色头像，成年女性，银色短发、蓝色眼睛、"
            + "黑色外套，冷静温和的表情，简洁背景，清晰半身构图，无文字。";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .retryOnConnectionFailure(false)
            .connectTimeout(Duration.ofSeconds(10))
            .writeTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofSeconds(90))
            .callTimeout(Duration.ofSeconds(120))
            .build();

    @Test
    void generateOneRealCharacterImage() throws Exception {
        assumeTrue(Boolean.getBoolean("zhipu.image.smoke"),
                "Real image call is disabled; set -Dzhipu.image.smoke=true explicitly");

        String apiKey = requireEnvironment("ZHIPU_API_KEY");
        String apiUrl = environmentOrDefault("ZHIPU_IMAGE_API_URL", DEFAULT_API_URL);
        String model = environmentOrDefault("ZHIPU_IMAGE_MODEL", DEFAULT_MODEL);
        String size = environmentOrDefault("ZHIPU_IMAGE_SIZE", DEFAULT_SIZE);
        Path outputDirectory = Path.of(environmentOrDefault(
                "ZHIPU_IMAGE_OUTPUT_DIR", DEFAULT_OUTPUT_DIRECTORY.toString()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("prompt", PROMPT);
        payload.put("quality", "standard");
        payload.put("size", size);
        payload.put("watermark_enabled", true);

        Request request = new Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(objectMapper.writeValueAsBytes(payload), JSON))
                .build();

        String imageUrl;
        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (body == null) {
                fail("Zhipu image API returned an empty response");
            }
            byte[] responseBytes = readLimited(body.byteStream(), MAX_RESPONSE_BYTES,
                    "Zhipu image API response");
            JsonNode root = objectMapper.readTree(responseBytes);
            if (!response.isSuccessful()) {
                String code = root.path("error").path("code").asText("unknown");
                String message = root.path("error").path("message").asText("no message");
                fail("Zhipu image API failed: HTTP " + response.code()
                        + ", code=" + code + ", message=" + message);
            }
            imageUrl = root.path("data").path(0).path("url").asText();
            if (imageUrl.isBlank()) {
                fail("Zhipu image API response did not contain data[0].url");
            }
        }

        Path saved = downloadImage(imageUrl, outputDirectory);
        System.out.println("REAL_ZHIPU_IMAGE_SAVED=" + saved.toAbsolutePath().normalize());
    }

    private Path downloadImage(String imageUrl, Path outputDirectory) throws IOException {
        URI uri = URI.create(imageUrl);
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IOException("Zhipu returned a non-HTTPS image URL");
        }

        Request download = new Request.Builder().url(imageUrl).get().build();
        try (Response response = httpClient.newCall(download).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Generated image download failed with HTTP " + response.code());
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Generated image download returned an empty response");
            }
            String contentType = response.header("Content-Type", "");
            if (!contentType.toLowerCase().startsWith("image/")) {
                throw new IOException("Generated image download returned non-image content: " + contentType);
            }
            long contentLength = body.contentLength();
            if (contentLength > MAX_IMAGE_BYTES) {
                throw new IOException("Generated image exceeds the 20 MiB download limit");
            }

            Files.createDirectories(outputDirectory);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String baseName = "cogview-character-" + timestamp + "-" + UUID.randomUUID();
            Path partial = outputDirectory.resolve(baseName + ".part");
            Path target = outputDirectory.resolve(baseName + imageExtension(contentType));
            try {
                try (InputStream input = body.byteStream();
                     OutputStream output = Files.newOutputStream(partial, StandardOpenOption.CREATE_NEW)) {
                    copyLimited(input, output, MAX_IMAGE_BYTES);
                }
                moveCompletedFile(partial, target);
                return target;
            } catch (Exception error) {
                Files.deleteIfExists(partial);
                throw error;
            }
        }
    }

    private byte[] readLimited(InputStream input, long maximum, String label) throws IOException {
        var output = new java.io.ByteArrayOutputStream();
        copyLimited(input, output, maximum, label);
        return output.toByteArray();
    }

    private void copyLimited(InputStream input, OutputStream output, long maximum) throws IOException {
        copyLimited(input, output, maximum, "Generated image");
    }

    private void copyLimited(InputStream input, OutputStream output, long maximum, String label)
            throws IOException {
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > maximum) {
                throw new IOException(label + " exceeds the configured size limit");
            }
            output.write(buffer, 0, read);
        }
    }

    private void moveCompletedFile(Path partial, Path target) throws IOException {
        try {
            Files.move(partial, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(partial, target);
        }
    }

    private String imageExtension(String contentType) {
        String normalized = contentType.toLowerCase();
        if (normalized.contains("jpeg") || normalized.contains("jpg")) {
            return ".jpg";
        }
        if (normalized.contains("webp")) {
            return ".webp";
        }
        return ".png";
    }

    private String requireEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is not configured in this process");
        }
        return value.trim();
    }

    private String environmentOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
