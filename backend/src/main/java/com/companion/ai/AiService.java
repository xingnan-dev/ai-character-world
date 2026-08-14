package com.companion.ai;

import com.companion.chat.model.ChatMessageExchange;
import com.companion.entity.Personality;
import reactor.core.publisher.Flux;

public interface AiService {

    Flux<String> chatStream(Long userId, Long sessionId, String userMessage,
                            Personality personality, ChatMessageExchange exchange);
}
