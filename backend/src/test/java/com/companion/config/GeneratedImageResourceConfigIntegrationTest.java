package com.companion.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("acceptance")
class GeneratedImageResourceConfigIntegrationTest {
    private static final String ORIGINAL_DATASOURCE_URL = System.getProperty("spring.datasource.url");
    private static final String ORIGINAL_FLYWAY_ENABLED = System.getProperty("spring.flyway.enabled");
    private static final String ORIGINAL_STORAGE_DIRECTORY = System.getProperty("image-generation.storage-directory");
    private static final String ORIGINAL_ACCEPTANCE_FAKE = System.getProperty("acceptance.image.fake.enabled");
    private static Path storage;
    private static Path outside;

    static {
        ensurePaths();
        System.setProperty("spring.datasource.url", "jdbc:h2:mem:resource_mapping;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
        System.setProperty("spring.flyway.enabled", "false");
        System.setProperty("image-generation.storage-directory", storage.toString());
        System.setProperty("acceptance.image.fake.enabled", "true");
    }

    @LocalServerPort
    int port;

    @BeforeAll
    static void setUp() throws IOException {
        ensurePaths();
    }

    private static void ensurePaths() {
        if (storage != null) return;
        try {
            Path root = Files.createTempDirectory("image-resource-test-");
            storage = root.resolve("images");
            outside = root.resolve("outside.jpg");
            Files.write(outside, new byte[]{1, 2, 3, 4});
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    @AfterAll
    static void tearDown() throws IOException {
        restoreProperty("spring.datasource.url", ORIGINAL_DATASOURCE_URL);
        restoreProperty("spring.flyway.enabled", ORIGINAL_FLYWAY_ENABLED);
        restoreProperty("image-generation.storage-directory", ORIGINAL_STORAGE_DIRECTORY);
        restoreProperty("acceptance.image.fake.enabled", ORIGINAL_ACCEPTANCE_FAKE);
        if (storage != null && storage.getParent() != null) {
            Files.walk(storage.getParent()).sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        }
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    @Test
    void createsMissingDirectoryAndServesImagesWithoutTraversal() throws Exception {
        assertThat(Files.isDirectory(storage)).isTrue();
        byte[] jpeg = imageBytes("jpg");
        Path existing = storage.resolve("existing.jpg");
        Files.write(existing, jpeg);
        String beforeHash = sha256(existing);

        TestRestTemplate client = new TestRestTemplate();
        ResponseEntity<byte[]> response = client.getForEntity(url("/generated-images/existing.jpg"), byte[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().getType()).isEqualTo("image");
        assertThat(response.getHeaders().getContentType().getSubtype()).isEqualTo("jpeg");
        assertThat(response.getBody()).isEqualTo(jpeg);

        Path png = storage.resolve("new.png");
        byte[] pngBytes = imageBytes("png");
        Files.write(png, pngBytes);
        ResponseEntity<byte[]> pngResponse = client.getForEntity(url("/generated-images/new.png"), byte[].class);
        assertThat(pngResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pngResponse.getHeaders().getContentType()).isNotNull();
        assertThat(pngResponse.getHeaders().getContentType().getType()).isEqualTo("image");
        assertThat(pngResponse.getHeaders().getContentType().getSubtype()).isEqualTo("png");
        assertThat(pngResponse.getBody()).isEqualTo(pngBytes);

        ResponseEntity<byte[]> traversal = client.getForEntity(url("/generated-images/%2e%2e/outside.jpg"), byte[].class);
        assertThat(traversal.getStatusCode()).isNotEqualTo(HttpStatus.OK);
        assertThat(sha256(existing)).isEqualTo(beforeHash);
        assertThat(Files.exists(outside)).isTrue();
    }

    private String url(String path) {
        return "http://127.0.0.1:" + port + path;
    }

    private byte[] imageBytes(String format) throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertThat(ImageIO.write(image, format, output)).isTrue();
        return output.toByteArray();
    }

    private String sha256(Path path) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}
