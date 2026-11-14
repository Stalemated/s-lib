package com.stalemated.lib.util.math;

import java.util.function.LongSupplier;

/**
 * Tracks elapsed animation time for a rendered scrolling element. It starts over when the
 * element's identity changes or after more than 100 ms without a render call. Not
 * thread-safe; meant for the render thread.
 */
public final class ScrollClock {

    private static final long RESET_AFTER_MS = 100;

    private final LongSupplier nowMillis;
    private long startTime;
    private long lastRenderTime;
    private int lastIdentity;
    private boolean started = false;

    /**
     * Creates a clock using {@link System#nanoTime()}.
     */
    public ScrollClock() {
        this(() -> System.nanoTime() / 1_000_000L);
    }

    ScrollClock(LongSupplier nowMillis) {
        this.nowMillis = nowMillis;
    }

    /**
     * Returns milliseconds elapsed since the current scroll cycle started, resetting if
     * {@code identity} differs from the previous call or more than 100 ms have passed since
     * that call.
     *
     * @param identity identifies what is being drawn, for example the text's {@code hashCode()}
     * @return elapsed milliseconds for this scroll cycle
     */
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
