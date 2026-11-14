package com.stalemated.lib.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Limits a {@code long} or {@code Long} option, or the contents of a {@link Nest} collection or
 * map of them, to {@code [min, max]}. In YACL screens the option becomes a slider.
 * <p>
 * Values are clamped when the config loads, changes, saves or arrives from the network.
 * {@code min} must not be greater than {@code max} and {@code step} must be greater than 0, or
 * building the config fails.
 * <p>
 * Example: {@code @RangeLong(min = 0L, max = 100000L)}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RangeLong {
    /**
     * Minimum allowed value (inclusive).
     *
     * @return the lower bound
     */
    long min();

    /**
     * Maximum allowed value (inclusive).
     *
     * @return the upper bound
     */
    long max();

    /**
     * Step of the YACL slider. It does not affect clamping.
     *
     * @return the step size
     */
    long step() default 1L;
}
