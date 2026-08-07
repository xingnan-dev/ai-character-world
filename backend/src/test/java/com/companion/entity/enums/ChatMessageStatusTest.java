package com.companion.entity.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ChatMessageStatusTest {

    @Test
    void convertsEverySupportedCodeToItsStatus() {
        assertThat(ChatMessageStatus.fromCode(0)).isEqualTo(ChatMessageStatus.PENDING);
        assertThat(ChatMessageStatus.fromCode(1)).isEqualTo(ChatMessageStatus.STREAMING);
        assertThat(ChatMessageStatus.fromCode(2)).isEqualTo(ChatMessageStatus.COMPLETED);
        assertThat(ChatMessageStatus.fromCode(3)).isEqualTo(ChatMessageStatus.FAILED);
        assertThat(ChatMessageStatus.fromCode(4)).isEqualTo(ChatMessageStatus.CANCELLED);
        assertThat(ChatMessageStatus.fromCode(5)).isEqualTo(ChatMessageStatus.INTERRUPTED);
    }

    @Test
    void rejectsNullAndUnknownCodes() {
        assertThatIllegalArgumentException().isThrownBy(() -> ChatMessageStatus.fromCode(null));
        assertThatIllegalArgumentException().isThrownBy(() -> ChatMessageStatus.fromCode(6));
    }
}
