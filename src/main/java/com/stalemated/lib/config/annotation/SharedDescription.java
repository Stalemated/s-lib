package com.stalemated.lib.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Redirects the YACL tooltip translation key from {@code <prefix>.option.<path>.desc}
 * to a shared {@code <prefix>.description.<value>} key (normalized to snake_case).
 * <p>
 * Replaces the option's individual description entirely, allowing multiple config
 * fields to share a single language entry. Does not affect visible names or comments.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SharedDescription {
    /**
     * The shared description key identifier, resolved under {@code <prefix>.description.<value>}.
     */
    String value();
}


