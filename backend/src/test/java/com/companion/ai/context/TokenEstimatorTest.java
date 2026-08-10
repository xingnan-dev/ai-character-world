package com.companion.ai.context;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenEstimatorTest {

    private final TokenEstimator estimator = new TokenEstimator();

    @Test
    void estimatesChineseAndEnglishWithoutProviderTokenizer() {
        assertThat(estimator.estimate("你好世界")).isEqualTo(4);
        assertThat(estimator.estimate("hello world")).isEqualTo(3);
        assertThat(estimator.estimate("你好hello")).isEqualTo(4);
    }

    @Test
    void truncatesTextWithinRequestedBudget() {
        String result = estimator.truncate("你好世界hello world", 5);

        assertThat(result).isNotEmpty();
        assertThat(estimator.estimate(result)).isLessThanOrEqualTo(5);
        assertThat(result.length()).isLessThan("你好世界hello world".length());
    }
}
