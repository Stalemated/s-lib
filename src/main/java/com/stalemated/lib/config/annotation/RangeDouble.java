package com.stalemated.lib.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Limits a {@code double} or {@code Double} option, or the contents of a {@link Nest} collection
 * or map of them, to {@code [min, max]}. In YACL screens the option becomes a slider.
 * <p>
 * Values are clamped when the config loads, changes, saves or arrives from the network, and
 * rounded to 15 significant digits. {@code NaN} is rejected and infinities are clamped.
 * {@code min}, {@code max} and {@code step} must be finite, {@code min} not greater than
 * {@code max} and {@code step} greater than 0, or building the config fails.
 * <p>
 * Example: {@code @RangeDouble(min = 1.0, max = 12.5)}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RangeDouble {
    /**
     * Minimum allowed value (inclusive).
     *
     * @return the lower bound
     */
    double min();

    /**
     * Maximum allowed value (inclusive).
     *
     * @return the upper bound
     */
    double max();

    /**
     * Step of the YACL slider. Without {@link Format}, it also sets how many decimals the slider
     * shows. It does not affect clamping.
     *
     * @return the step size
     */
    double step() default 0.1;
}
