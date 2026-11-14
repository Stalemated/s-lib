package com.stalemated.lib.util.math;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Small math helpers for config values and animations.
 */
public final class MathUtils {

    private static final MathContext FLOAT_SIGNIFICANT_DIGITS = new MathContext(7);
    private static final MathContext DOUBLE_SIGNIFICANT_DIGITS = new MathContext(15);

    private MathUtils() {}

    /**
     * Clamps {@code value} between {@code min} and {@code max}, inclusive.
     *
     * @param value input value
     * @param min lower bound
     * @param max upper bound
     * @return clamped value
     * @throws IllegalArgumentException if {@code min > max}
     */
    public static int clamp(int value, int min, int max) {
        if (min > max) throw new IllegalArgumentException("min (" + min + ") > max (" + max + ")");

        if (value < min) return min;
        return Math.min(value, max);
    }

    /**
     * Clamps {@code value} between {@code min} and {@code max}, inclusive.
     *
     * @param value input value
     * @param min lower bound
     * @param max upper bound
     * @return clamped value
     * @throws IllegalArgumentException if {@code min > max}
     */
    public static long clamp(long value, long min, long max) {
        if (min > max) throw new IllegalArgumentException("min (" + min + ") > max (" + max + ")");

        if (value < min) return min;
        return Math.min(value, max);
    }

    /**
     * Clamps {@code value} between {@code min} and {@code max}, inclusive.
     *
     * @param value input value
     * @param min lower bound
     * @param max upper bound
     * @return clamped value, or {@code NaN} if {@code value} is {@code NaN}
     * @throws IllegalArgumentException if {@code min} or {@code max} is {@code NaN}, or {@code min > max}
     */
    public static float clamp(float value, float min, float max) {
        if (!(min <= max)) throw new IllegalArgumentException("min (" + min + ") must not be NaN or greater than max (" + max + ")");

        if (value < min) return min;
        return Math.min(value, max);
    }

    /**
     * Clamps {@code value} between {@code min} and {@code max}, inclusive.
     *
     * @param value input value
     * @param min lower bound
     * @param max upper bound
     * @return clamped value, or {@code NaN} if {@code value} is {@code NaN}
     * @throws IllegalArgumentException if {@code min} or {@code max} is {@code NaN}, or {@code min > max}
     */
    public static double clamp(double value, double min, double max) {
        if (!(min <= max)) throw new IllegalArgumentException("min (" + min + ") must not be NaN or greater than max (" + max + ")");

        if (value < min) return min;
        return Math.min(value, max);
    }

    /**
     * Rounds a finite {@code float} to 7 significant digits to eliminate slider step noise.
     *
     * @param value value to round
     * @return rounded float (non-finite values are returned unchanged)
     */
    public static float roundToSignificantDigits(float value) {
        if (!Float.isFinite(value)) return value;
        return new BigDecimal(Float.toString(value)).round(FLOAT_SIGNIFICANT_DIGITS).floatValue();
    }

    /**
     * Rounds a finite {@code double} to 15 significant digits to eliminate slider step noise.
     *
     * @param value value to round
     * @return rounded double (non-finite values are returned unchanged)
     */
    public static double roundToSignificantDigits(double value) {
        if (!Double.isFinite(value)) return value;
        return new BigDecimal(Double.toString(value)).round(DOUBLE_SIGNIFICANT_DIGITS).doubleValue();
    }

    /**
     * Linearly interpolates between {@code start} and {@code end} by {@code t}.
     *
     * @param start start value
     * @param end end value
     * @param t interpolation factor; not clamped, so values outside 0 to 1 extrapolate
     * @return interpolated value
     */
    public static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }

    /**
     * Evaluates a cubic ease-out curve for normalized progress {@code t}.
     *
     * @param t progress, meant for 0 to 1; not clamped
     * @return eased progress
     */
    public static double easeOutCubic(double t) {
        return 1.0 - Math.pow(1.0 - t, 3.0);
    }

    /**
     * Evaluates an exponential ease-out curve for normalized progress {@code t}.
     *
     * @param t progress, meant for 0 to 1; not clamped
     * @return eased progress
     */
    public static double easeOutExpo(double t) {
        return t == 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * t);
    }

    /**
     * Returns whether {@code value.doubleValue()} is finite (neither {@code NaN} nor infinite).
     *
     * @param value the number to test
     * @return {@code true} if finite
     */
    public static boolean isFinite(Number value) {
        return Double.isFinite(value.doubleValue());
    }
}
