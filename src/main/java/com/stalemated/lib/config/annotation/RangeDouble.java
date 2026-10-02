package com.stalemated.lib.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines double number boundaries for a config field.
 * <p>
 * During config loading, any value outside the [min, max] range is clamped to the nearest bound.
 * <p>
 * Example: {@code @RangeDouble(min = 1.0, max = 12.5)}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RangeDouble {
    /**
     * Minimum allowable value (inclusive).
     */
    double min();

    /**
     * Maximum allowable value (inclusive).
     */
    double max();
}
