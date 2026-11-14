package com.stalemated.lib.util.math;

/**
 * Computes the scroll offset of text that is wider than its space.
 */
public final class ScrollMathUtil {

    private ScrollMathUtil() {}

    /**
     * Calculates the horizontal pixel offset for a ping-pong scroll cycle with pauses at
     * both ends.
     *
     * @param overflowWidth number of pixels the text is wider than the visible area; 0 or less
     *                      returns 0
     * @param speedPixelsPerSecond scroll speed in pixels per second; must be greater than 0
     * @param pauseMs pause duration in milliseconds at the start and end of each sweep
     * @param elapsedTime elapsed milliseconds since the scroll cycle began
     * @return horizontal offset in pixels, from 0 to {@code overflowWidth}
     */
    public static int calculateScrollOffset(int overflowWidth, double speedPixelsPerSecond, long pauseMs, long elapsedTime) {
        if (overflowWidth <= 0) return 0;

        double speed = speedPixelsPerSecond / 1000.0;
        long travelTime = Math.max(1, (long) (overflowWidth / speed));
        long halfCycle = travelTime + pauseMs;
        long totalCycle = 2 * halfCycle;
        long cycleTime = elapsedTime % totalCycle;

        double progress;
        if (cycleTime < pauseMs) {
            progress = 0.0; // Start pause
        } else if (cycleTime < halfCycle) {
            progress = (double) (cycleTime - pauseMs) / travelTime; // Moving right
        } else if (cycleTime < halfCycle + pauseMs) {
            progress = 1.0; // End pause
        } else {
            progress = 1.0 - ((double) (cycleTime - (halfCycle + pauseMs)) / travelTime); // Moving left
        }

        return (int) (progress * overflowWidth);
    }
}
