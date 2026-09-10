package com.companion.image;

import java.io.IOException;

public interface ImageGenerationClient {
    String generate(String prompt) throws IOException;
}
