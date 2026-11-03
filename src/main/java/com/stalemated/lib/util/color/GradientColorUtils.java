package com.stalemated.lib.util.color;

public final class GradientColorUtils {

    private GradientColorUtils() {}

    // Static Gradient
    public static int interpolate(int color1, int color2, float ratio) {
        int a = channel(getAlpha(color1), getAlpha(color2), ratio);
        int r = channel(getRed(color1), getRed(color2), ratio);
        int g = channel(getGreen(color1), getGreen(color2), ratio);
        int b = channel(getBlue(color1), getBlue(color2), ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // Breathing Gradient
    public static int interpolateAnimation(int color1, int color2, float ratio, float animationFactor) {
        int baseColor = interpolate(color1, color2, ratio);

        int r = channel(getRed(baseColor), 255, animationFactor);
        int g = channel(getGreen(baseColor), 255, animationFactor);
        int b = channel(getBlue(baseColor), 255, animationFactor);

        return (getAlpha(baseColor) << 24) | (r << 16) | (g << 8) | b;
    }

    public static int getAlpha(int color) { return (color >>> 24) & 0xFF; }
    public static int getRed(int color) { return (color >> 16) & 0xFF; }
    public static int getGreen(int color) { return (color >> 8) & 0xFF; }
    public static int getBlue(int color) { return color & 0xFF; }

    public static float bounceBack(float input, float range) {
        float doubleRange = range * 2.0f;
        float position = Math.abs(input) % doubleRange;
        return position >= range ? doubleRange - position : position;
    }

    public static int gradientSlide(float progression, int start, int end) {
        float ratio = bounceBack(progression, 1.0f);
        return interpolate(start, end, ratio);
    }

    private static int channel(int from, int to, float ratio) {
        return (int) (from + (to - from) * ratio);
    }
}
