package com.stalemated.lib.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines long integer numerical boundaries for a config field.
 * <p>
 * During config loading, any value outside the [min, max] range is clamped to the nearest bound.
 * <p>
 * Example: {@code @RangeLong(min = 0L, max = 100000L)}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RangeLong {
    /**
     * Minimum allowable value (inclusive).
     */
    long min();

    /**
     * Maximum allowable value (inclusive).
     */
    long max();

    /**
     * Slider step increment.
     */
    long step() default 1L;
}
