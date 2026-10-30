package com.stalemated.lib.config;

import com.stalemated.lib.config.io.ConfigProvider;
import com.stalemated.lib.config.model.OptionTree;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.function.Supplier;

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
    LocalConfigManager<T> create(ConfigProvider<T> provider, Path path, Logger logger, OptionTree tree, Supplier<T> factory) {
        return new LocalConfigManager<>(provider, path, logger, tree, factory);
    }
}
