package com.stalemated.lib.util.color;

/**
 * Color interpolation and wave helpers for gradients. Every color here is an ARGB int.
 */
public final class GradientColorUtils {

    private GradientColorUtils() {}

    /**
     * Linearly interpolates all four ARGB channels between {@code color1} and {@code color2},
     * truncating each channel.
     *
     * @param color1 the start color
     * @param color2 the end color
     * @param ratio 0 gives {@code color1}, 1 gives {@code color2}; not clamped, and values
     *              outside 0 to 1 can push a channel out of 0 to 255 and into the next one
     * @return the interpolated color
     */
    public static int interpolate(int color1, int color2, float ratio) {
        int a = channel(getAlpha(color1), getAlpha(color2), ratio);
        int r = channel(getRed(color1), getRed(color2), ratio);
        int g = channel(getGreen(color1), getGreen(color2), ratio);
        int b = channel(getBlue(color1), getBlue(color2), ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Interpolates between {@code color1} and {@code color2} at {@code ratio}, then brightens
     * the red, green, and blue channels toward white (255) by {@code animationFactor}. Alpha
     * stays as interpolated.
     *
     * @param color1 the start color
     * @param color2 the end color
     * @param ratio as in {@link #interpolate(int, int, float)}
     * @param animationFactor 0 keeps the color, 1 gives white; not clamped
     * @return the brightened color
     */
    public static int interpolateAnimation(int color1, int color2, float ratio, float animationFactor) {
        int baseColor = interpolate(color1, color2, ratio);

        int r = channel(getRed(baseColor), 255, animationFactor);
        int g = channel(getGreen(baseColor), 255, animationFactor);
        int b = channel(getBlue(baseColor), 255, animationFactor);

        return (getAlpha(baseColor) << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Extracts the alpha channel (bits 24 to 31) from {@code color}.
     *
     * @param color the color to read
     * @return the alpha, 0 to 255
     */
    public static int getAlpha(int color) { return (color >>> 24) & 0xFF; }

    /**
     * Extracts the red channel (bits 16 to 23) from {@code color}.
     *
     * @param color the color to read
     * @return the red value, 0 to 255
     */
    public static int getRed(int color) { return (color >> 16) & 0xFF; }

    /**
     * Extracts the green channel (bits 8 to 15) from {@code color}.
     *
     * @param color the color to read
     * @return the green value, 0 to 255
     */
    public static int getGreen(int color) { return (color >> 8) & 0xFF; }

    /**
     * Extracts the blue channel (bits 0 to 7) from {@code color}.
     *
     * @param color the color to read
     * @return the blue value, 0 to 255
     */
    public static int getBlue(int color) { return color & 0xFF; }

    /**
     * Maps {@code input} to a triangle wave: it goes from 0 up to {@code range} and back to 0
     * every {@code 2 * range}. A negative {@code input} gives the same result as its absolute
     * value.
     *
     * @param input the position on the wave
     * @param range the peak; must be greater than 0 (0 gives NaN)
     * @return a value from 0 to {@code range}
     */
    public static float bounceBack(float input, float range) {
        float doubleRange = range * 2.0f;
        float position = Math.abs(input) % doubleRange;
        return position >= range ? doubleRange - position : position;
    }

    /**
     * Computes a color that slides from {@code start} to {@code end} and back as
     * {@code progression} grows, using {@link #bounceBack(float, float)} with a range of 1.
     *
     * @param progression the position in the animation; one full back-and-forth takes 2 units
     * @param start the color at 0
     * @param end the color at 1
     * @return the interpolated color
     */
    public static int gradientSlide(float progression, int start, int end) {
        float ratio = bounceBack(progression, 1.0f);
        return interpolate(start, end, ratio);
    }

    private static int channel(int from, int to, float ratio) {
        return (int) (from + (to - from) * ratio);
    }
}
