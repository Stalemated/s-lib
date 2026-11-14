package com.stalemated.lib.mixin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a mixin class that needs one or more mods. It only has an effect when the mixin config
 * names {@link ConditionalMixinPlugin} as its {@code "plugin"}, which reads it while Mixin loads
 * the config.
 * <p>
 * It has CLASS retention, so reflection can't see it at runtime.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface RequiresMod {
    /**
     * The mod ids that must all be loaded for this mixin to apply. With an empty array, the mixin
     * always applies.
     *
     * @return the mod ids
     */
    String[] value();
}
