package com.stalemated.lib.config.annotation;

import com.stalemated.lib.config.network.SyncMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares how a config option or an entire config class
 * is synced between server and client.
 * <p>
 * Example: {@code @Sync(SyncMode.INFORM_SERVER)}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Sync {
    /**
     * The desired {@code @Sync} value.
     */
    SyncMode value() default SyncMode.OVERRIDE_CLIENT;
}
