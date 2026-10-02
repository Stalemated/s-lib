package com.stalemated.lib.config;

import com.stalemated.lib.config.manager.builder.LocalConfigBuilder;
import com.stalemated.lib.config.manager.LocalConfigManager;
import com.stalemated.lib.config.manager.builder.SyncedConfigBuilder;
import com.stalemated.lib.config.manager.SyncedConfigManager;

import java.util.function.Supplier;

/**
 * Primary entry point for registering configs in S-Lib.
 * <p>
 * Provides one-line config registration methods that handle serialization,
 * networking, and lifecycle hooks automatically.
 */
public final class SLibConfig {

    private SLibConfig() {}

    /**
     * Creates a new fluent builder for configuring a synchronized config with custom parameters
     * (e.g., custom permissions, custom channel, custom path, or sync callbacks).
     *
     * @param configClass The runtime class of the config data model.
     * @param <T> The config data model type.
     * @return A new builder instance.
     */
    public static <T> SyncedConfigBuilder<T> syncedBuilder(Class<T> configClass) {
        return new SyncedConfigBuilder<>(configClass);
    }

    /**
     * Creates a new fluent builder for configuring a local-only config with custom parameters
     * (e.g., custom path, custom logger, or GSON type adapter).
     *
     * @param configClass The runtime class of the config data model.
     * @param <T> The config data model type.
     * @return A new builder instance.
     */
    public static <T> LocalConfigBuilder<T> localBuilder(Class<T> configClass) {
        return new LocalConfigBuilder<>(configClass);
    }

    /**
     * Registers a synced config manager using the given default factory.
     * <p>
     * The manager will automatically load/create the JSON5 file from {@code config/{modId}.json5},
     * bind to network sync channel {@code {modId}:sync_config}, and register itself with
     * the centralized lifecycle system so child mods require no platform hook code.
     *
     * @param modId The unique identifier of your mod.
     * @param configClass The runtime class of the config data model.
     * @param defaultFactory A supplier creating a default config instance (e.g. {@code MyConfig::new}).
     * @param <T> The config data model type.
     * @return The fully initialized and registered {@link SyncedConfigManager}.
     */
    public static <T> SyncedConfigManager<T> registerSynced(
            String modId,
            Class<T> configClass,
            Supplier<T> defaultFactory) {
        return syncedBuilder(configClass)
                .modId(modId)
                .defaultFactory(defaultFactory)
                .register();
    }

    /**
     * Registers a synced config manager using the class's default constructor.
     * <p>
     * The manager will automatically load/create the JSON5 file from {@code config/{modId}.json5},
     * bind to network sync channel {@code {modId}:sync_config}, and register itself with
     * the centralized lifecycle system so child mods require no platform hook code.
     *
     * @param modId The unique identifier of your mod.
     * @param configClass The runtime class of the config data model.
     * @param <T> The config data model type.
     * @return The fully initialized and registered {@link SyncedConfigManager}.
     */
    public static <T> SyncedConfigManager<T> registerSynced(
            String modId,
            Class<T> configClass) {
        return syncedBuilder(configClass)
                .modId(modId)
                .register();
    }

    /**
     * Registers a local-only config manager using the given default factory.
     * <p>
     * The manager will automatically load/create the JSON5 file from {@code config/{modId}.json5}.
     * Unlike synced configs, this manager operates entirely client-side (or server-side only) 
     * and does not interact with the network registry.
     *
     * @param modId The unique identifier of your mod.
     * @param configClass The runtime class of the config data model.
     * @param defaultFactory A supplier creating a default config instance (e.g. {@code MyConfig::new}).
     * @param <T> The config data model type.
     * @return The fully initialized and registered {@link LocalConfigManager}.
     */
    public static <T> LocalConfigManager<T> registerLocal(
            String modId,
            Class<T> configClass,
            Supplier<T> defaultFactory) {
        return localBuilder(configClass)
                .modId(modId)
                .defaultFactory(defaultFactory)
                .register();
    }

    /**
     * Registers a local-only config manager using the class's default constructor.
     * <p>
     * The manager will automatically load/create the JSON5 file from {@code config/{modId}.json5}.
     * Unlike synced configs, this manager operates entirely client-side (or server-side only) 
     * and does not interact with the network registry.
     *
     * @param modId The unique identifier of your mod.
     * @param configClass The runtime class of the config data model.
     * @param <T> The config data model type.
     * @return The fully initialized and registered {@link LocalConfigManager}.
     */
    public static <T> LocalConfigManager<T> registerLocal(
            String modId,
            Class<T> configClass) {
        return localBuilder(configClass)
                .modId(modId)
                .register();
    }
}
