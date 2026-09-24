package com.stalemated.lib.util.math;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathUtils {
    public static int clamp(int value, int min, int max) {
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

    public static float roundFloat(float value, int decimalPlaces) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return value;

        return new BigDecimal(Float.toString(value))
                .setScale(decimalPlaces, RoundingMode.HALF_UP)
                .floatValue();
    }

    public static float roundFloat(float value) {
        return roundFloat(value, 6);
    }

    public static double roundDouble(double value, int decimalPlaces) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return value;

        return new BigDecimal(Double.toString(value))
                .setScale(decimalPlaces, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static double roundDouble(double value) {
        return roundDouble(value, 14);
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
