package com.stalemated.lib.config;

/**
 * Entry point for S-Lib configs.
 * <p>
 * You can build the manager in a static field, but call {@code register()} from your mod
 * initializer, not from a static initializer, so the file loads at a known point.
 */
public final class SLibConfig {

    private SLibConfig() {}

    /**
     * Creates a builder for a config that syncs between the server and its clients.
     * <p>
     * By default, the file is {@code config/<modId>.json5} and the sync channel is
     * {@code <modId>:sync_config}. Nothing is read or written until {@code register()}.
     *
     * @param modId the id of your mod; it names the file, the logger and the sync channel
     * @param configClass the config class; it needs a no-argument constructor unless you set
     *                    {@code defaultFactory}
     * @param <T> the config model type
     * @return a new {@link SyncedConfigBuilder}
     * @throws NullPointerException if {@code modId} or {@code configClass} is {@code null}
     */
    public static <T> SyncedConfigBuilder<T> synced(String modId, Class<T> configClass) {
        return new SyncedConfigBuilder<>(modId, configClass);
    }

    /**
     * Creates a builder for a config that only lives locally and never uses the network. The
     * {@code @Sync} annotations of the class are ignored.
     * <p>
     * By default, the file is {@code config/<modId>.json5}. Nothing is read or written until
     * {@code register()}.
     *
     * @param modId the id of your mod; it names the file and the logger
     * @param configClass the config class; it needs a no-argument constructor unless you set
     *                    {@code defaultFactory}
     * @param <T> the config model type
     * @return a new {@link LocalConfigBuilder}
     * @throws NullPointerException if {@code modId} or {@code configClass} is {@code null}
     */
    public static <T> LocalConfigBuilder<T> local(String modId, Class<T> configClass) {
        return new LocalConfigBuilder<>(modId, configClass);
    }
}
