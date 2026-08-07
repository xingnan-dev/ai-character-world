package com.companion.chat;

import reactor.core.Disposable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SseSubscriptionLifecycle {

    private final AtomicReference<Disposable> subscription = new AtomicReference<>();
    private final AtomicBoolean upstreamTerminated = new AtomicBoolean(false);
    private final AtomicBoolean cancellationRequested = new AtomicBoolean(false);

    public void attach(Disposable disposable) {
        if (!subscription.compareAndSet(null, disposable)) {
            disposable.dispose();
            throw new IllegalStateException("SSE subscription is already attached");
        }
        if (cancellationRequested.get() && !upstreamTerminated.get()) {
            disposable.dispose();
        }
    }

    public void markUpstreamTerminated() {
        upstreamTerminated.set(true);
    }

    public boolean cancelUpstream() {
        if (upstreamTerminated.get()) {
            return false;
        }
        cancellationRequested.set(true);
        Disposable disposable = subscription.get();
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
        return true;
    }

    public boolean isUpstreamTerminated() {
        return upstreamTerminated.get();
    }
}
