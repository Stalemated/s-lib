package com.stalemated.lib.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables an option in YACL screens only while another option of the same config, the toggle,
 * holds one of the given values. The option follows the value shown on the screen before it is
 * saved.
 * <p>
 * The toggle is a boolean or enum option. If it has its own {@code @EnabledIf}, the whole chain
 * must match. It works on options, lists and {@code mapPairsOption}, not on the entries of
 * {@code nestedList}, {@code nestedMap}, {@code nestedMapLists} or {@code listElement}. A
 * misspelled key, a value the toggle cannot hold, or a cycle fails when the screen is built.
 * <p>
 * Example: {@code @EnabledIf(value = "mode", equals = {"FADE", "SCROLL"})}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EnabledIf {
    /**
     * Full key of the toggle in this config, for example {@code "visuals.enabled"} for a field
     * of a {@link Nest} field named {@code visuals}.
     *
     * @return the key of the toggle
     */
    String value();

    /**
     * Values of the toggle that enable this option: {@code "true"} or {@code "false"} for a
     * boolean, the exact {@code name()} of each constant for an enum. Defaults to
     * {@code {"true"}}.
     *
     * @return the values that enable this option
     */
    String[] equals() default {"true"};
}
