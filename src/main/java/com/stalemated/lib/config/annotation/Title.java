package com.stalemated.lib.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a section header label to be rendered above the annotated config field in GUI screens.
 * <p>
 * The {@link #value()} is resolved via the naming strategy, which prepends {@code modId + ".title."}
 * by default (e.g. {@code @Title("primitives")} resolves to {@code modId + ".title.primitives"}),
 * or preserves fully qualified keys if they already start with {@code modId + "."}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Title {
    /**
     * The title key suffix (e.g. {@code "primitives"}), or a fully qualified translation key.
     *
     * @return The title key or suffix.
     */
    String value();
}
