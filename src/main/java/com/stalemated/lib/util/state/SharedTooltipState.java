package com.stalemated.lib.util.state;

/**
 * A global flag that tooltip mods use to tell each other that a tooltip has its own size.
 * S-Lib only stores it; other mods read it with {@link #isCustomDimensionsRequested()}.
 * Nothing resets it on its own. Meant for the render thread.
 */
public final class SharedTooltipState {

    private static volatile boolean customDimensionsRequested = false;

    private SharedTooltipState() {}

    /**
     * Runs {@code render} with the custom-dimensions flag enabled, restoring its previous state
     * afterward even if {@code render} throws.
     *
     * @param render the tooltip rendering operation to execute
     */
    public static void withCustomDimensions(Runnable render) {
        boolean previous = customDimensionsRequested;
        customDimensionsRequested = true;
        try {
            render.run();
        } finally {
            customDimensionsRequested = previous;
        }
    }

    /**
     * Sets the custom-dimensions flag to {@code true}. It stays set until
     * {@link #clearCustomDimensionsRequest()}; prefer {@link #withCustomDimensions(Runnable)}.
     */
    public static void requestCustomDimensions() {
        customDimensionsRequested = true;
    }

    /**
     * Resets the custom-dimensions flag to {@code false}.
     */
    public static void clearCustomDimensionsRequest() {
        customDimensionsRequested = false;
    }

    /**
     * Returns whether custom tooltip dimensions are currently requested.
     *
     * @return {@code true} if the custom-dimensions flag is set
     */
    public static boolean isCustomDimensionsRequested() {
        return customDimensionsRequested;
    }
}
