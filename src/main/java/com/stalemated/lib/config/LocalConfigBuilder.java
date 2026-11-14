package com.stalemated.lib.config;

/**
 * Fluent builder for {@link LocalConfigManager}.
 * <p>
 * Get one from {@link SLibConfig#local(String, Class)}.
 *
 * @param <T> the config model type
 */
public final class LocalConfigBuilder<T> extends ConfigBuilder<T, LocalConfigManager<T>, LocalConfigBuilder<T>> {

    LocalConfigBuilder(String modId, Class<T> configClass) {
        super(modId, configClass);
    }

    @Override
    LocalConfigBuilder<T> self() {
        return this;
    }

    @Override
    LocalConfigManager<T> create(LocalConfigManager<T> localManager) {
        return localManager;
    }

    @Override
    void register(LocalConfigManager<T> manager) {
        manager.register();
    }
}
