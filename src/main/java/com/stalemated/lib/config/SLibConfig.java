package com.stalemated.lib.config;

/**
 * Primary entry point for registering configs in S-Lib.
 * <p>
 * Call {@code register()} on the builder from your mod initializer, not from a static field initializer,
 * so the config loads at a known point in the mod lifecycle.
 */
public final class SLibConfig {

    private SLibConfig() {}

    /**
     * Creates a builder for a config that syncs between server and clients.
     * By default, the file is {@code config/<modId>.json5} and the network channel {@code <modId>:sync_config}.
     *
     * @param modId The unique identifier of your mod.
     * @param configClass The runtime class of the config data model.
     * @param <T> The config data model type.
     * @return A new {@link SyncedConfigBuilder} instance.
     */
    public static <T> SyncedConfigBuilder<T> synced(String modId, Class<T> configClass) {
        return new SyncedConfigBuilder<>(modId, configClass);
    }

    /**
     * Creates a builder for a config that only lives locally.
     * By default, the file is {@code config/<modId>.json5}.
     *
     * @param modId The unique identifier of your mod.
     * @param configClass The runtime class of the config data model.
     * @param <T> The config data model type.
     * @return A new {@link LocalConfigBuilder} instance.
     */
    public static <T> LocalConfigBuilder<T> local(String modId, Class<T> configClass) {
        return new LocalConfigBuilder<>(modId, configClass);
    }
}
