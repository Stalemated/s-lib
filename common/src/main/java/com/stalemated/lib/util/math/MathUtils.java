package com.stalemated.lib.util.math;

public class MathUtils {
    public static int clamp(int value, int min, int max) {
        if (value < min) return min;
        return Math.min(value, max);
    }

    public static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }

    public static double easeOutCubic(double t) {
        return 1.0 - Math.pow(1.0 - t, 3.0);
    }

    public static double easeOutExpo(double t) {
        return t == 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * t);
    }
}
