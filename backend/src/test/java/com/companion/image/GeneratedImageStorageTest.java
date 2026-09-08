package com.companion.image;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

class GeneratedImageStorageTest {
    @TempDir Path temp;
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 0};

    @Test void savesToConfiguredDirectoryWithUniqueServerNamesAndNeverTouchesOldImages() throws Exception {
        Path old = temp.resolve("old.png");
        Files.write(old, PNG);
        FakeTransport transport = new FakeTransport(ok(PNG), ok(PNG));
        GeneratedImageStorage storage = new GeneratedImageStorage(transport, temp, 100);
        Path first = storage.download("https://203.0.113.10/a");
        Path second = storage.download("https://203.0.113.10/a");
        assertEquals(temp.toAbsolutePath(), first.getParent());
        assertNotEquals(first, second);
        assertTrue(first.toString().endsWith(".png"));
        assertArrayEquals(PNG, Files.readAllBytes(old));
    }

    @Test void followsOnlyValidatedRedirectTargets() throws Exception {
        FakeTransport transport = new FakeTransport(response(302, 0, "https://203.0.113.11/final", null), ok(PNG));
        Path saved = new GeneratedImageStorage(transport, temp, 100).download("https://203.0.113.10/start");
        assertTrue(Files.exists(saved));
        assertEquals(List.of("https://203.0.113.10/start", "https://203.0.113.11/final"), transport.seen);
        FakeTransport blocked = new FakeTransport(response(302, 0, "https://127.0.0.1/private", null));
        assertThrows(IOException.class, () -> new GeneratedImageStorage(blocked, temp, 100).download("https://203.0.113.10/start"));
        assertEquals(1, blocked.seen.size());
    }

    @Test void rejectsHttpAndPrivateAddressesBeforeTransport() {
        FakeTransport transport = new FakeTransport();
        GeneratedImageStorage storage = new GeneratedImageStorage(transport, temp, 100);
        assertThrows(IOException.class, () -> storage.download("http://203.0.113.10/a"));
        assertThrows(IOException.class, () -> storage.download("https://127.0.0.1/a"));
        assertTrue(transport.seen.isEmpty());
    }

    @Test void enforcesDeclaredAndStreamingSizeAndCleansPartialFiles() {
        assertThrows(IOException.class, () -> new GeneratedImageStorage(new FakeTransport(response(200, 101, null, PNG)), temp, 100).download("https://203.0.113.10/a"));
        byte[] oversized = new byte[101];
        System.arraycopy(PNG, 0, oversized, 0, PNG.length);
        assertThrows(IOException.class, () -> new GeneratedImageStorage(new FakeTransport(response(200, -1, null, oversized)), temp, 100).download("https://203.0.113.10/a"));
        assertNoPartFiles();
    }

    @Test void detectsActualFormatInsteadOfTrustingMetadataAndCleansPartialFiles() {
        assertThrows(IOException.class, () -> new GeneratedImageStorage(new FakeTransport(ok("not-image-data".getBytes())), temp, 100).download("https://203.0.113.10/a"));
        assertNoPartFiles();
    }

    @Test void downloadFailuresLeaveNoFiles() {
        assertThrows(IOException.class, () -> new GeneratedImageStorage(new FakeTransport(response(503, 0, null, null)), temp, 100).download("https://203.0.113.10/a"));
        assertNoPartFiles();
    }

    private void assertNoPartFiles() {
        assertDoesNotThrow(() -> { try (var paths = Files.list(temp)) { assertTrue(paths.noneMatch(path -> path.toString().endsWith(".part"))); } });
    }
    private static ImageDownloadTransport.DownloadResponse ok(byte[] bytes) { return response(200, bytes.length, null, bytes); }
    private static ImageDownloadTransport.DownloadResponse response(int status, long length, String location, byte[] bytes) {
        return new ImageDownloadTransport.DownloadResponse(status, length, location, bytes == null ? null : new ByteArrayInputStream(bytes));
    }
    private static class FakeTransport implements ImageDownloadTransport {
        final Queue<DownloadResponse> responses = new ArrayDeque<>();
        final java.util.ArrayList<String> seen = new java.util.ArrayList<>();
        FakeTransport(DownloadResponse... responses) { this.responses.addAll(List.of(responses)); }
        public DownloadResponse download(URI uri) throws IOException { seen.add(uri.toString()); if (responses.isEmpty()) throw new IOException("no response"); return responses.remove(); }
    }
}
