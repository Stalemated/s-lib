package com.stalemated.lib.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Limits an {@code int} or {@code Integer} option, or the contents of a {@link Nest} collection
 * or map of them, to {@code [min, max]}. In YACL screens the option becomes a slider.
 * <p>
 * Values are clamped when the config loads, changes, saves or arrives from the network.
 * {@code min} must not be greater than {@code max} and {@code step} must be greater than 0, or
 * building the config fails. It fails too on a field of another type, like {@code long}.
 * <p>
 * Example: {@code @RangeInt(min = 0, max = 100)}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RangeInt {
    /**
     * Minimum allowed value (inclusive).
     *
     * @return the lower bound
     */
    int min();

    /**
     * Maximum allowed value (inclusive).
     *
     * @return the upper bound
     */
    int max();

    /**
     * Step of the YACL slider. It does not affect clamping.
     *
     * @return the step size
     */
    int step() default 1;
}
