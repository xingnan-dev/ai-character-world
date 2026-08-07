package com.companion.chat;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class SseSubscriptionLifecycleTest {

    @Test
    void normalCompletionDoesNotCancelUpstream() {
        SseSubscriptionLifecycle lifecycle = new SseSubscriptionLifecycle();
        AtomicBoolean cancelled = new AtomicBoolean(false);

        lifecycle.attach(Flux.empty()
                .doOnCancel(() -> cancelled.set(true))
                .subscribe(ignored -> { }, ignored -> { }, lifecycle::markUpstreamTerminated));

        assertThat(lifecycle.isUpstreamTerminated()).isTrue();
        assertThat(lifecycle.cancelUpstream()).isFalse();
        assertThat(cancelled).isFalse();
    }

    @Test
    void clientTerminationCancelsAttachedUpstream() {
        SseSubscriptionLifecycle lifecycle = new SseSubscriptionLifecycle();
        AtomicBoolean cancelled = new AtomicBoolean(false);

        lifecycle.attach(Flux.never()
                .doOnCancel(() -> cancelled.set(true))
                .subscribe());

        assertThat(lifecycle.cancelUpstream()).isTrue();
        assertThat(cancelled).isTrue();
        assertThat(lifecycle.isUpstreamTerminated()).isFalse();
    }
}
