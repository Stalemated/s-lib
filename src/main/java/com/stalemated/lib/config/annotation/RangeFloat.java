package com.stalemated.lib.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Limits a {@code float} or {@code Float} option, or the contents of a {@link Nest} collection
 * or map of them, to {@code [min, max]}. In YACL screens the option becomes a slider.
 * <p>
 * Values are clamped when the config loads, changes, saves or arrives from the network, and
 * rounded to 7 significant digits. {@code NaN} is rejected and infinities are clamped.
 * {@code min}, {@code max} and {@code step} must be finite, {@code min} not greater than
 * {@code max} and {@code step} greater than 0, or building the config fails.
 * <p>
 * Example: {@code @RangeFloat(min = 1.0f, max = 12.5f)}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RangeFloat {
    /**
     * Minimum allowed value (inclusive).
     *
     * @return the lower bound
     */
    float min();

    /**
     * Maximum allowed value (inclusive).
     *
     * @return the upper bound
     */
    float max();

    /**
     * Step of the YACL slider. Without {@link Format}, it also sets how many decimals the slider
     * shows. It does not affect clamping.
     *
     * @return the step size
     */
    float step() default 0.1f;
}
