package com.companion.image;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.URI;

public class OkHttpImageDownloadTransport implements ImageDownloadTransport {
    private final OkHttpClient http;

    public OkHttpImageDownloadTransport(OkHttpClient http) {
        this.http = http;
    }

    @Override
    public DownloadResponse download(URI uri) throws IOException {
        Response response = http.newCall(new Request.Builder().url(uri.toString()).get().build()).execute();
        if (response.body() == null) {
            response.close();
            return new DownloadResponse(response.code(), 0, response.header("Location"), null);
        }
        return new DownloadResponse(response.code(), response.body().contentLength(), response.header("Location"),
                new java.io.FilterInputStream(response.body().byteStream()) {
                    @Override public void close() throws IOException { response.close(); }
                });
    }
}
