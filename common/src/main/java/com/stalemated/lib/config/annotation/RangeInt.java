package com.stalemated.lib.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines integer numerical boundaries for a config field.
 * <p>
 * During config loading, any value outside the [min, max] range is clamped to the nearest bound.
 * <p>
 * Example: {@code @RangeInt(min = 0, max = 100)}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RangeInt {
    /**
     * Minimum allowable value (inclusive).
     */
    int min();

    /**
     * Maximum allowable value (inclusive).
     */
    int max();
}
