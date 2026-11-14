package com.stalemated.lib.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Replaces the text of a YACL slider with a {@link String#format(String, Object...)} pattern,
 * for example {@code "%.2f ms"} or {@code "%d%%"}.
 * <p>
 * The field also needs a range annotation, or building the config fails. A pattern that cannot
 * format the slider value fails when the screen is built. Without {@code @Format}, a slider
 * shows as many decimals as its {@code step}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Format {
    /**
     * The pattern passed to {@link String#format(String, Object...)} with the slider value: an
     * {@code Integer}, {@code Long}, {@code Float} or {@code Double}.
     *
     * @return the format pattern
     */
    String value();
}
