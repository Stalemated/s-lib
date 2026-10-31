package com.stalemated.lib.util.state;

public final class SharedTooltipState {

    private static boolean customDimensionsRequested = false;

    private SharedTooltipState() {}

    /**
     * Executes the given render operation while setting a custom dimensions flag.
     * The flag is temporarily set to true during the execution of the render operation
     * and is reset to false afterward, even if the operation throws an exception.
     *
     * @param render A {@link Runnable} representing the operation to execute with custom dimensions enabled.
     */
    public static void withCustomDimensions(Runnable render) {
        customDimensionsRequested = true;
        try {
            render.run();
        } finally {
            customDimensionsRequested = false;
        }
    }

    public static void requestCustomDimensions() {
        customDimensionsRequested = true;
    }

    public static void clearCustomDimensionsRequest() {
        customDimensionsRequested = false;
    }

    public static boolean isCustomDimensionsRequested() {
        return customDimensionsRequested;
    }
}
