package com.stalemated.lib.util.color;

public class GradientColorUtils {

    // Static Gradient
    public static int interpolate(int color1, int color2, float ratio) {
        int r1 = getRed(color1);
        int g1 = getGreen(color1);
        int b1 = getBlue(color1);

        int r2 = getRed(color2);
        int g2 = getGreen(color2);
        int b2 = getBlue(color2);

        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);

        return (r << 16) | (g << 8) | b;
    }

    // Breathing Gradient
    public static int interpolateAnimation(int color1, int color2, float ratio, float animationFactor) {
        int baseColor = interpolate(color1, color2, ratio);

        int baseR = getRed(baseColor);
        int baseG = getGreen(baseColor);
        int baseB = getBlue(baseColor);

        int r = (int) (baseR + (255 - baseR) * animationFactor);
        int g = (int) (baseG + (255 - baseG) * animationFactor);
        int b = (int) (baseB + (255 - baseB) * animationFactor);

        return (r << 16) | (g << 8) | b;
    }

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
}
