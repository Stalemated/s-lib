package com.stalemated.lib.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a section header label to be rendered above the annotated config field in GUI screens.
 * <p>
 * The {@link #value()} is resolved via the naming strategy, which builds
 * {@code <translationPrefix>.group.<value in snake_case>} by default
 * (e.g. {@code @Title("primitives")} resolves to {@code <translationPrefix>.group.primitives}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Title {
    /**
     * The title key, appended to the group translation prefix (e.g. {@code "primitives"}).
     *
     * @return The title key.
     */
    String value();
}
