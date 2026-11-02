package com.stalemated.lib.config;

/**
 * Builder for {@link LocalConfigManager}.
 *
 * @param <T> The config data model type.
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
