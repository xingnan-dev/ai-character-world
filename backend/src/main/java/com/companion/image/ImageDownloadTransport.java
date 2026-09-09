package com.companion.image;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.InetAddress;
import java.util.List;

public interface ImageDownloadTransport {
    DownloadResponse download(URI uri, List<InetAddress> resolvedAddresses) throws IOException;

    record DownloadResponse(int status, long contentLength, String location, InputStream body)
            implements AutoCloseable {
        @Override
        public void close() throws IOException {
            if (body != null) body.close();
        }
    }
}
