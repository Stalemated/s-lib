package com.stalemated.lib.util.math;

import java.math.BigDecimal;
import java.math.MathContext;

public class MathUtils {

    private static final MathContext FLOAT_SIGNIFICANT_DIGITS = new MathContext(7);
    private static final MathContext DOUBLE_SIGNIFICANT_DIGITS = new MathContext(15);

    public static int clamp(int value, int min, int max) {
        if (min > max) throw new IllegalArgumentException();

        if (value < min) return min;
        return Math.min(value, max);
    }

    public static long clamp(long value, long min, long max) {
        if (min > max) throw new IllegalArgumentException();

        if (value < min) return min;
        return Math.min(value, max);
    }

    public static float clamp(float value, float min, float max) {
        if (min > max) throw new IllegalArgumentException();

        if (value < min) return min;
        return Math.min(value, max);
    }

    public static double clamp(double value, double min, double max) {
        if (min > max) throw new IllegalArgumentException();

        if (value < min) return min;
        return Math.min(value, max);
    }

    public static float roundToSignificantDigits(float value) {
        if (!Float.isFinite(value)) return value;
        return new BigDecimal(Float.toString(value)).round(FLOAT_SIGNIFICANT_DIGITS).floatValue();
    }

    public static double roundToSignificantDigits(double value) {
        if (!Double.isFinite(value)) return value;
        return new BigDecimal(Double.toString(value)).round(DOUBLE_SIGNIFICANT_DIGITS).doubleValue();
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
