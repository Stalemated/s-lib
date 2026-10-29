package com.stalemated.lib.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code String}, {@code ResourceLocation}, or collection of them
 * to use the visual Minecraft Item dropdown controller in YACL.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ItemDropdown {
    /**
     * Whether item tags ({@code #namespace:tag}) and wildcards ({@code *}, {@code namespace:*})
     * are included in the dropdown suggestions.
     * <p>
     * Note: When applied to a {@code ResourceLocation} field or collection,
     * tags and wildcards are automatically excluded because {@code ResourceLocation} cannot
     * store {@code '#'} or {@code '*'} characters.
     *
     * @return {@code true} if tags and wildcards should be suggested for String fields.
     */
    boolean allowTags() default true;
}
