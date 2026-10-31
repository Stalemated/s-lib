package com.stalemated.lib.util.math;

import java.util.function.LongSupplier;

public final class ScrollClock {

    private static final long RESET_AFTER_MS = 100;

    private final LongSupplier nowMillis;
    private long startTime;
    private long lastRenderTime;
    private int lastIdentity;
    private boolean started = false;

    public ScrollClock() {
        this(() -> System.nanoTime() / 1_000_000L);
    }

    ScrollClock(LongSupplier nowMillis) {
        this.nowMillis = nowMillis;
    }

    public long elapsedMillis(int identity) {
        long now = nowMillis.getAsLong();
        if (!started || now - lastRenderTime > RESET_AFTER_MS || identity != lastIdentity) {
            started = true;
            startTime = now;
            lastIdentity = identity;
        }
        lastRenderTime = now;
        return now - startTime;
    }
}
