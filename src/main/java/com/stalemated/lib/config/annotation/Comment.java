package com.stalemated.lib.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Attaches comments to a config field.
 * <p>
 * Comments are written directly above the field in the {@code .json5} file as line comments ({@code // comment}).
 * Multiline comments can be written using newline characters ({@code \n}) or text blocks.
 * <p>
 * Example: {@code @Comment("This is a test!")}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Comment {
    /**
     * The comment text describing the purpose or behavior of the config field.
     */
    String value();
}
