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

public class GeneratedImageStorage {
    private static final int MAX_REDIRECTS = 3;
    private final ImageDownloadTransport transport;
    private final Path directory;
    private final long maxBytes;

    public GeneratedImageStorage(ImageDownloadTransport transport, Path directory, long maxBytes) {
        this.transport = transport;
        this.directory = directory.toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
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
            validatePublicHttps(uri);
            try (ImageDownloadTransport.DownloadResponse response = transport.download(uri)) {
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

    private void validatePublicHttps(URI uri) throws IOException {
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) throw new IOException("图片地址不安全");
        for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress()) throw new IOException("图片地址不安全");
        }
    }
}
