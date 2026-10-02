package com.stalemated.lib.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines float number boundaries for a config field.
 * <p>
 * During config loading, any value outside the [min, max] range is clamped to the nearest bound.
 * <p>
 * Example: {@code @RangeFloat(min = 1.0f, max = 12.5f)}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RangeFloat {
    /**
     * Minimum allowable value (inclusive).
     */
    float min();

    /**
     * Maximum allowable value (inclusive).
     */
    float max();
}
