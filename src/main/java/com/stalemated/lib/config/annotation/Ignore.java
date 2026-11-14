package com.stalemated.lib.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Leaves a field out of the config: it is not saved, loaded, synced or shown in screens, and
 * comparisons of option values skip it.
 * <p>
 * {@code static} and {@code transient} fields are left out even without this annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Ignore {
}
