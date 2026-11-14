package com.stalemated.lib.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Puts a header above the option in YACL screens. By default its key is
 * {@code <prefix>.group.<value in snake_case>}, the same key that {@code group(...)} uses, so
 * {@code @Title("primitives")} shows {@code <prefix>.group.primitives}.
 * <p>
 * On a {@link Nest} field it names that group instead. In {@code autoGenerate}, a title on a
 * simple option starts a new group. On the first option of a group without a name, it names the
 * group. Blank values are ignored.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Title {
    /**
     * Key of the title, without the {@code <prefix>.group.} part, for example
     * {@code "primitives"}. A custom {@code YaclNamingStrategy} can change the full key.
     *
     * @return the title key
     */
    String value();
}
