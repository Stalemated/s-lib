package com.stalemated.lib.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Uses the S-Lib item dropdown, with search, for a {@code String} or {@code ResourceLocation}
 * field, or a collection of them, in YACL screens.
 * <p>
 * In a {@code String} field the suggestions are only hints: any text is accepted.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ItemDropdown {
    /**
     * Whether the suggestions include item tags ({@code #namespace:tag}) and the wildcards
     * {@code *} and {@code namespace:*}. With {@code false}, only items are suggested.
     * <p>
     * Ignored for {@code ResourceLocation} fields, because an id cannot contain {@code '#'} or
     * {@code '*'}.
     *
     * @return {@code true} to suggest tags and wildcards along with item ids
     */
    boolean allowTags() default true;
}
