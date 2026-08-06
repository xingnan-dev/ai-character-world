package com.companion.ai.provider;

import com.companion.ai.model.LlmChunk;
import com.companion.ai.model.LlmRequest;
import com.companion.ai.model.LlmResponse;
import reactor.core.publisher.Flux;

public interface LlmProvider {

    String name();

    LlmResponse complete(LlmRequest request);

    Flux<LlmChunk> stream(LlmRequest request);
}
