package com.companion.ai.context;

import com.companion.ai.model.LlmMessage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Conservative local token estimator used for context-window protection.
 * It intentionally avoids provider-specific tokenizer dependencies.
 */
@Component
public class TokenEstimator {

    private static final int MESSAGE_OVERHEAD_TOKENS = 4;

    public int estimate(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int tokens = 0;
        int latinRunLength = 0;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);

            if (isCjk(codePoint)) {
                tokens += tokensForLatinRun(latinRunLength);
                latinRunLength = 0;
                tokens++;
            } else {
                latinRunLength++;
            }
        }
        return tokens + tokensForLatinRun(latinRunLength);
    }

    public int estimateMessage(String content) {
        return MESSAGE_OVERHEAD_TOKENS + estimate(content);
    }

    public int estimateMessages(List<LlmMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        return messages.stream()
                .filter(message -> message != null)
                .mapToInt(message -> estimateMessage(message.content()))
                .sum();
    }

    public String truncate(String text, int maxTokens) {
        if (text == null || text.isEmpty() || maxTokens <= 0) {
            return "";
        }
        if (estimate(text) <= maxTokens) {
            return text;
        }

        int low = 0;
        int high = text.length();
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (estimate(text.substring(0, middle)) <= maxTokens) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        if (low > 0 && low < text.length() && Character.isHighSurrogate(text.charAt(low - 1))) {
            low--;
        }
        return text.substring(0, low);
    }

    private int tokensForLatinRun(int length) {
        return length == 0 ? 0 : (length + 3) / 4;
    }

    private boolean isCjk(int codePoint) {
        Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
        return script == Character.UnicodeScript.HAN
                || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA
                || script == Character.UnicodeScript.HANGUL;
    }
}
