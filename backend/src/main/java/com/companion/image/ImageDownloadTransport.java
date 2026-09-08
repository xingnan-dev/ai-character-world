package com.companion.image;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public interface ImageDownloadTransport {
    DownloadResponse download(URI uri) throws IOException;

    record DownloadResponse(int status, long contentLength, String location, InputStream body)
            implements AutoCloseable {
        @Override
        public void close() throws IOException {
            if (body != null) body.close();
        }
    }
}
