package com.companion.ai;

import com.companion.chat.model.ChatMessageExchange;
import com.companion.entity.Personality;
import com.companion.character.snapshot.CharacterSnapshot;
import reactor.core.publisher.Flux;

public interface AiService {

    Flux<String> chatStream(Long userId, Long sessionId, String userMessage,
                            Personality personality, ChatMessageExchange exchange);

    Flux<String> chatStream(Long userId, Long sessionId, String userMessage,
                            CharacterSnapshot character, ChatMessageExchange exchange);

    default Flux<String> chatStream(Long userId, Long sessionId, String userMessage,
                                    Personality personality, ChatMessageExchange exchange, Long characterId) {
        return chatStream(userId, sessionId, userMessage, personality, exchange);
    }

    default Flux<String> chatStream(Long userId, Long sessionId, String userMessage,
                                    CharacterSnapshot character, ChatMessageExchange exchange, Long characterId) {
        return chatStream(userId, sessionId, userMessage, character, exchange);
    }
}
