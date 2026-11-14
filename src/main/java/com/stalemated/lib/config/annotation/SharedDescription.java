package com.stalemated.lib.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Lets several options share one tooltip in YACL screens. By default the option uses
 * {@code <prefix>.description.<value in snake_case>} instead of its own
 * {@code <prefix>.option.<key>.desc}.
 * <p>
 * It does not change the option name or the comment in the file. A missing translation shows an
 * empty description. It also works on the fields of {@code nestedList} and {@code nestedMap}
 * entries.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SharedDescription {
    /**
     * Key of the shared description, without the {@code <prefix>.description.} part. It is
     * converted to snake_case.
     *
     * @return the shared description key
     */
    String value();
}


