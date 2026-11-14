package com.stalemated.lib.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Writes a comment above a config field or a {@link Nest} category in the {@code .json5} file,
 * as line comments ({@code // ...}). Use {@code \n} or a text block for several lines.
 * <p>
 * It does not change config screens; their descriptions come from the translations.
 * <p>
 * Example: {@code @Comment("This is a test!")}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Comment {
    /**
     * The comment text written to the config file.
     *
     * @return the comment text
     */
    String value();
}
