package com.stalemated.lib.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * On an object field, makes it a category: its fields become options with keys like
 * {@code field.subfield}. On a {@code Collection} or {@code Map}, scans the element type, so
 * range annotations apply to the contents and POJO elements can be edited in screens.
 * <p>
 * A {@code null} nested object is created with its no-argument constructor. A {@link Sync} on
 * the class of the field applies to its options, unless the field has its own. A
 * {@link Comment} on the field is written above the category, and a {@link Title} names its
 * group in screens.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Nest {
}
