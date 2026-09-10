package com.companion.image;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.List;

public class GeneratedImageStorage {
    private static final int MAX_REDIRECTS = 3;
    private final ImageDownloadTransport transport;
    private final Path directory;
    private final long maxBytes;
    private final HostResolver resolver;

    public GeneratedImageStorage(ImageDownloadTransport transport, Path directory, long maxBytes) {
        this(transport, directory, maxBytes, host -> List.of(InetAddress.getAllByName(host)));
    }

    GeneratedImageStorage(ImageDownloadTransport transport, Path directory, long maxBytes, HostResolver resolver) {
        this.transport = transport;
        this.directory = directory.toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
        this.resolver = resolver;
    }

    public Path download(String value) throws IOException {
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException error) {
            throw new IOException("图片地址无效", error);
        }
        Files.createDirectories(directory);
        for (int redirects = 0; ; redirects++) {
            var resolvedAddresses = validatePublicHttps(uri);
            try (ImageDownloadTransport.DownloadResponse response = transport.download(uri, resolvedAddresses)) {
                if (response.status() >= 300 && response.status() < 400) {
                    if (redirects >= MAX_REDIRECTS || response.location() == null) throw new IOException("图片重定向无效");
                    uri = uri.resolve(response.location());
                    continue;
                }
                if (response.status() < 200 || response.status() >= 300 || response.body() == null) throw new IOException("图片下载失败");
                if (response.contentLength() > maxBytes) throw new IOException("图片过大");
                return save(response.body());
            }
        }
    }

    private Path save(java.io.InputStream input) throws IOException {
        Path partial = Files.createTempFile(directory, "image-", ".part");
        try {
            byte[] header = new byte[12];
            int headerSize = 0;
            try (var output = Files.newOutputStream(partial)) {
                byte[] buffer = new byte[8192];
                long total = 0;
                int read;
                while ((read = input.read(buffer)) != -1) {
                    total += read;
                    if (total > maxBytes) throw new IOException("图片过大");
                    int copy = Math.min(read, header.length - headerSize);
                    if (copy > 0) {
                        System.arraycopy(buffer, 0, header, headerSize, copy);
                        headerSize += copy;
                    }
                    output.write(buffer, 0, read);
                }
            }
            String extension = extension(header, headerSize);
            Path target = directory.resolve(UUID.randomUUID() + extension);
            try {
                Files.move(partial, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(partial, target);
            }
            return target;
        } finally {
            Files.deleteIfExists(partial);
        }
    }

    private String extension(byte[] bytes, int size) throws IOException {
        if (size >= 3 && (bytes[0] & 255) == 0xff && (bytes[1] & 255) == 0xd8 && (bytes[2] & 255) == 0xff) return ".jpg";
        if (size >= 8 && (bytes[0] & 255) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G' && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10) return ".png";
        if (size >= 12 && "RIFF".equals(new String(bytes, 0, 4, StandardCharsets.US_ASCII)) && "WEBP".equals(new String(bytes, 8, 4, StandardCharsets.US_ASCII))) return ".webp";
        throw new IOException("返回内容不是有效图片");
    }

    private java.util.List<InetAddress> validatePublicHttps(URI uri) throws IOException {
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) throw new IOException("图片地址不安全");
        var addresses = List.copyOf(resolver.resolve(uri.getHost()));
        if (addresses.isEmpty()) throw new IOException("图片地址无解析结果");
        for (InetAddress address : addresses) {
            if (!isPubliclyRoutable(address.getAddress())) throw new IOException("图片地址不安全");
        }
        return addresses;
    }

    private boolean isPubliclyRoutable(byte[] address) {
        if (address.length == 4) return isPublicIpv4(address, 0);
        if (address.length != 16) return false;
        if (isIpv4Mapped(address)) return isPublicIpv4(address, 12);

        int first = address[0] & 255;
        int second = address[1] & 255;
        if ((first & 0xfe) == 0xfc) return false; // fc00::/7 unique local
        if (first == 0xfe && (second & 0xc0) == 0x80) return false; // fe80::/10 link local
        if (first == 0xff) return false; // ff00::/8 multicast
        if (first == 0x20 && second == 0x01
                && (address[2] & 255) == 0x0d && (address[3] & 255) == 0xb8) return false; // documentation

        boolean unspecifiedOrLoopback = true;
        for (int index = 0; index < 15; index++) unspecifiedOrLoopback &= address[index] == 0;
        if (unspecifiedOrLoopback && ((address[15] & 255) == 0 || (address[15] & 255) == 1)) return false;
        return true;
    }

    private boolean isIpv4Mapped(byte[] address) {
        for (int index = 0; index < 10; index++) {
            if (address[index] != 0) return false;
        }
        return address[10] == (byte) 0xff && address[11] == (byte) 0xff;
    }

    private boolean isPublicIpv4(byte[] address, int offset) {
        int first = address[offset] & 255;
        int second = address[offset + 1] & 255;
        int third = address[offset + 2] & 255;
        if (first == 0 || first == 10 || first == 127 || first >= 224) return false;
        if (first == 100 && second >= 64 && second <= 127) return false;
        if (first == 169 && second == 254) return false;
        if (first == 172 && second >= 16 && second <= 31) return false;
        if (first == 192 && second == 0 && third == 0) return false;
        if (first == 192 && second == 0 && third == 2) return false;
        if (first == 192 && second == 168) return false;
        if (first == 198 && (second == 18 || second == 19)) return false;
        if (first == 198 && second == 51 && third == 100) return false;
        return !(first == 203 && second == 0 && third == 113);
    }

    @FunctionalInterface
    interface HostResolver {
        List<InetAddress> resolve(String host) throws IOException;
    }
}
