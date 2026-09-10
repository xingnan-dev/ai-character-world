package com.companion.image;

import com.sun.net.httpserver.HttpServer;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GeneratedImageStorageSsrfTest {
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 0};
    private static final InetAddress PUBLIC = address("8.8.8.8");
    private static final InetAddress SECOND_PUBLIC = address("1.1.1.1");
    private static final InetAddress PRIVATE = address("10.0.0.1");

    @TempDir Path temp;

    @Test
    void dnsRebindingCannotChangeTheAddressesPinnedIntoTransport() throws Exception {
        ScriptedResolver resolver = new ScriptedResolver();
        resolver.answer("images.example", List.of(PUBLIC), List.of(PRIVATE));
        RecordingTransport transport = new RecordingTransport(ok(PNG));
        transport.onDownload = () -> assertEquals(List.of(PRIVATE), resolver.resolve("images.example"));

        Path saved = storage(transport, resolver).download("https://images.example/picture.png");

        assertTrue(Files.exists(saved));
        assertEquals(List.of(PUBLIC), transport.calls.get(0).addresses());
        assertEquals("images.example", transport.calls.get(0).uri().getHost());
    }

    @Test
    void rejectsIpv4MappedIpv6NonPublicRangesBeforeTransport() throws Exception {
        for (String ipv4 : List.of("127.0.0.1", "10.0.0.1", "169.254.1.1",
                "100.64.0.1", "100.127.255.254", "192.0.2.1", "198.51.100.1", "203.0.113.1")) {
            ScriptedResolver resolver = new ScriptedResolver();
            resolver.answer("images.example", List.of(mapped(ipv4)));
            RecordingTransport transport = new RecordingTransport();

            assertThrows(IOException.class,
                    () -> storage(transport, resolver).download("https://images.example/picture.png"), ipv4);
            assertTrue(transport.calls.isEmpty(), ipv4);
        }
    }

    @Test
    void rejectsSpecialUseIpv4AndIpv6RangesBeforeTransport() {
        List<String> forbidden = List.of(
                "0.0.0.1", "10.0.0.1", "100.64.0.1", "100.127.255.254", "127.0.0.1",
                "169.254.1.1", "172.16.0.1", "192.0.0.1", "192.0.2.1", "192.168.0.1",
                "198.18.0.1", "198.19.255.254", "198.51.100.1", "203.0.113.1",
                "224.0.0.1", "240.0.0.1", "::", "::1", "fc00::1", "fd12:3456:789a::1",
                "fe80::1", "ff00::1", "2001:db8::1");
        for (String literal : forbidden) {
            ScriptedResolver resolver = new ScriptedResolver();
            resolver.answer("images.example", List.of(address(literal)));
            RecordingTransport transport = new RecordingTransport();

            assertThrows(IOException.class,
                    () -> storage(transport, resolver).download("https://images.example/picture.png"), literal);
            assertTrue(transport.calls.isEmpty(), literal);
        }
    }

    @Test
    void allowsPublicIpv4AndIpv6WithoutChangingOriginalHostnames() throws Exception {
        InetAddress publicIpv6 = address("2606:4700:4700::1111");
        ScriptedResolver resolver = new ScriptedResolver();
        resolver.answer("ipv4.example", List.of(PUBLIC));
        resolver.answer("ipv6.example", List.of(publicIpv6));
        RecordingTransport transport = new RecordingTransport(
                redirect("https://ipv6.example/final"), ok(PNG));

        Path saved = storage(transport, resolver).download("https://ipv4.example/start");

        assertArrayEquals(PNG, Files.readAllBytes(saved));
        assertEquals(List.of(PUBLIC), transport.calls.get(0).addresses());
        assertEquals(List.of(publicIpv6), transport.calls.get(1).addresses());
        assertEquals("ipv4.example", transport.calls.get(0).uri().getHost());
        assertEquals("ipv6.example", transport.calls.get(1).uri().getHost());
    }

    @Test
    void rejectsMixedPublicPrivateAnswerAndEmptyAnswerAsAWhole() {
        ScriptedResolver mixed = new ScriptedResolver();
        mixed.answer("images.example", List.of(PUBLIC, PRIVATE));
        RecordingTransport mixedTransport = new RecordingTransport();
        assertThrows(IOException.class,
                () -> storage(mixedTransport, mixed).download("https://images.example/picture.png"));
        assertTrue(mixedTransport.calls.isEmpty());

        ScriptedResolver empty = new ScriptedResolver();
        empty.answer("images.example", List.of());
        RecordingTransport emptyTransport = new RecordingTransport();
        assertThrows(IOException.class,
                () -> storage(emptyTransport, empty).download("https://images.example/picture.png"));
        assertTrue(emptyTransport.calls.isEmpty());
    }

    @Test
    void redirectToPrivateHostIsRejectedBeforeSecondTransportCall() {
        ScriptedResolver resolver = new ScriptedResolver();
        resolver.answer("public.example", List.of(PUBLIC));
        resolver.answer("private.example", List.of(PRIVATE));
        RecordingTransport transport = new RecordingTransport(redirect("https://private.example/final"));

        assertThrows(IOException.class,
                () -> storage(transport, resolver).download("https://public.example/start"));

        assertEquals(1, transport.calls.size());
    }

    @Test
    void eachRedirectTargetIsResolvedOnceAndPinnedWithItsOriginalHostname() throws Exception {
        ScriptedResolver resolver = new ScriptedResolver();
        resolver.answer("first.example", List.of(PUBLIC));
        resolver.answer("second.example", List.of(SECOND_PUBLIC));
        RecordingTransport transport = new RecordingTransport(
                redirect("https://second.example/final"), ok(PNG));

        storage(transport, resolver).download("https://first.example/start");

        assertEquals(List.of("first.example", "second.example"), resolver.seen);
        assertEquals(List.of(PUBLIC), transport.calls.get(0).addresses());
        assertEquals(List.of(SECOND_PUBLIC), transport.calls.get(1).addresses());
        assertEquals("first.example", transport.calls.get(0).uri().getHost());
        assertEquals("second.example", transport.calls.get(1).uri().getHost());
    }

    @Test
    void normalPublicResolutionDownloadsAndClosesRedirectAndBodyResponses() throws Exception {
        ScriptedResolver resolver = new ScriptedResolver();
        resolver.answer("images.example", List.of(PUBLIC), List.of(PUBLIC));
        CloseTrackingInputStream redirectBody = new CloseTrackingInputStream(new byte[]{1});
        CloseTrackingInputStream imageBody = new CloseTrackingInputStream(PNG);
        RecordingTransport transport = new RecordingTransport(
                response(302, 1, "/final", redirectBody), response(200, PNG.length, null, imageBody));

        Path saved = storage(transport, resolver).download("https://images.example/start");

        assertArrayEquals(PNG, Files.readAllBytes(saved));
        assertTrue(redirectBody.closed);
        assertTrue(imageBody.closed);
    }

    @Test
    void okHttpUsesOnlyPinnedAddressesWhileKeepingOriginalHostHeader() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        AtomicReference<String> hostHeader = new AtomicReference<>();
        server.createContext("/image", exchange -> {
            hostHeader.set(exchange.getRequestHeaders().getFirst("Host"));
            exchange.sendResponseHeaders(200, PNG.length);
            exchange.getResponseBody().write(PNG);
            exchange.close();
        });
        server.start();
        try {
            Dns forbiddenFallbackDns = hostname -> {
                throw new UnknownHostException("base DNS must not be used: " + hostname);
            };
            OkHttpClient client = new OkHttpClient.Builder().dns(forbiddenFallbackDns).build();
            OkHttpImageDownloadTransport transport = new OkHttpImageDownloadTransport(client);
            URI originalHostname = URI.create("http://original-host.invalid:" + server.getAddress().getPort() + "/image");

            try (ImageDownloadTransport.DownloadResponse response = transport.download(
                    originalHostname, List.of(InetAddress.getLoopbackAddress()))) {
                assertEquals(200, response.status());
                assertArrayEquals(PNG, response.body().readAllBytes());
            }

            assertEquals("original-host.invalid:" + server.getAddress().getPort(), hostHeader.get());
        } finally {
            server.stop(0);
        }
    }

    private GeneratedImageStorage storage(ImageDownloadTransport transport,
                                          GeneratedImageStorage.HostResolver resolver) {
        return new GeneratedImageStorage(transport, temp, 1024, resolver);
    }

    private static ImageDownloadTransport.DownloadResponse ok(byte[] bytes) {
        return response(200, bytes.length, null, new ByteArrayInputStream(bytes));
    }

    private static ImageDownloadTransport.DownloadResponse redirect(String location) {
        return response(302, 0, location, null);
    }

    private static ImageDownloadTransport.DownloadResponse response(
            int status, long length, String location, InputStream body) {
        return new ImageDownloadTransport.DownloadResponse(status, length, location, body);
    }

    private static InetAddress address(String value) {
        try {
            return InetAddress.getByName(value);
        } catch (UnknownHostException error) {
            throw new AssertionError(error);
        }
    }

    private static InetAddress mapped(String ipv4) throws Exception {
        byte[] bytes = new byte[16];
        bytes[10] = (byte) 0xff;
        bytes[11] = (byte) 0xff;
        byte[] ipv4Bytes = InetAddress.getByName(ipv4).getAddress();
        System.arraycopy(ipv4Bytes, 0, bytes, 12, 4);
        return Inet6Address.getByAddress(null, bytes, -1);
    }

    private static final class ScriptedResolver implements GeneratedImageStorage.HostResolver {
        private final Map<String, Queue<List<InetAddress>>> answers = new HashMap<>();
        private final List<String> seen = new ArrayList<>();

        @SafeVarargs
        final void answer(String host, List<InetAddress>... values) {
            answers.put(host, new ArrayDeque<>(Arrays.asList(values)));
        }

        @Override
        public List<InetAddress> resolve(String host) throws IOException {
            seen.add(host);
            Queue<List<InetAddress>> queued = answers.get(host);
            if (queued == null || queued.isEmpty()) throw new IOException("no DNS answer for " + host);
            return queued.remove();
        }
    }

    private static final class RecordingTransport implements ImageDownloadTransport {
        private final Queue<DownloadResponse> responses = new ArrayDeque<>();
        private final List<Call> calls = new ArrayList<>();
        private DownloadHook onDownload = () -> { };

        RecordingTransport(DownloadResponse... responses) {
            this.responses.addAll(Arrays.asList(responses));
        }

        @Override
        public DownloadResponse download(URI uri, List<InetAddress> resolvedAddresses) throws IOException {
            calls.add(new Call(uri, List.copyOf(resolvedAddresses)));
            onDownload.run();
            if (responses.isEmpty()) throw new IOException("no response");
            return responses.remove();
        }
    }

    @FunctionalInterface
    private interface DownloadHook {
        void run() throws IOException;
    }

    private record Call(URI uri, List<InetAddress> addresses) { }

    private static final class CloseTrackingInputStream extends ByteArrayInputStream {
        private boolean closed;

        CloseTrackingInputStream(byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
