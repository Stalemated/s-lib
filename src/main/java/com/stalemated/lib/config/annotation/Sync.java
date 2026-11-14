package com.stalemated.lib.config.annotation;

import com.stalemated.lib.config.network.SyncMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets how a field, or every option of a class, is synced between the server and its clients.
 * Put it on a field, on the config class or on the class of a {@link Nest} field.
 * <p>
 * At each level the order is: {@code @Sync} on the field, {@code @Sync} on the class of a
 * {@link Nest} field, then the mode of the parent. Options with no {@code @Sync} anywhere use
 * {@link SyncMode#OVERRIDE_CLIENT}. A config made with {@code SLibConfig.local} ignores it.
 * <p>
 * Example: {@code @Sync(SyncMode.INFORM_SERVER)}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Sync {
    /**
     * The sync mode. Defaults to {@link SyncMode#OVERRIDE_CLIENT}.
     *
     * @return the sync mode
     */
    SyncMode value() default SyncMode.OVERRIDE_CLIENT;
}
