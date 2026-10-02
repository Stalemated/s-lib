package com.stalemated.lib.util.math;

public class ScrollMathUtil {
    private static long globalStartTime = -1;
    private static long globalLastRenderTime = -1;
    private static int globalLastIdentity = -1;

    /**
     * Calculates the scroll progress based on time and parameters.
     * 
     * @param overflowWidth   The amount of pixels that overflow the allowed space.
     * @param speedPixelsPerSecond The speed of scrolling in pixels per second.
     * @param pauseMs         The time to pause at the start and end of the scroll in milliseconds.
     * @param elapsedTime     The time elapsed since the animation started.
     * @return The offset in pixels to translate the text.
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

    /**
     * Determines the elapsed time for a tooltip that is recreated every frame.
     * Resets the timer if more than 100ms have passed since the last render, or if the identity changes.
     */
    public static long getTooltipElapsedTime(int identity) {
        long currentTime = System.currentTimeMillis();
        if (globalStartTime == -1 || (currentTime - globalLastRenderTime) > 100 || identity != globalLastIdentity) {
            globalStartTime = currentTime;
            globalLastIdentity = identity;
        }
        globalLastRenderTime = currentTime;
        return currentTime - globalStartTime;
    }
}
