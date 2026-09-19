package com.stalemated.lib.config.manager.builder;

import blue.endless.jankson.Jankson;
import com.google.gson.GsonBuilder;
import com.stalemated.lib.config.io.ConfigProvider;
import com.stalemated.lib.config.manager.LocalConfigManager;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Builder for {@link LocalConfigManager}.
 *
 * @param <T> The config data model type.
 */
public class LocalConfigBuilder<T> {
    private final Class<T> configClass;
    private String modId;
    private Path configPath;
    private Logger logger;
    private Supplier<T> defaultFactory;
    private ConfigProvider<T> provider;
    private Consumer<Jankson.Builder> janksonCustomizer;
    private Consumer<GsonBuilder> gsonCustomizer;

    public LocalConfigBuilder(Class<T> configClass) {
        this.configClass = configClass;
    }

    public LocalConfigBuilder<T> modId(String modId) {
        this.modId = modId;
        return this;
    }

    public LocalConfigBuilder<T> configPath(Path path) {
        this.configPath = path;
        return this;
    }

    public LocalConfigBuilder<T> logger(Logger logger) {
        this.logger = logger;
        return this;
    }

    public LocalConfigBuilder<T> defaultFactory(Supplier<T> defaultFactory) {
        this.defaultFactory = defaultFactory;
        return this;
    }

    public LocalConfigBuilder<T> provider(ConfigProvider<T> provider) {
        this.provider = provider;
        return this;
    }

    public LocalConfigBuilder<T> janksonCustomizer(Consumer<Jankson.Builder> customizer) {
        this.janksonCustomizer = customizer;
        return this;
    }

    public LocalConfigBuilder<T> gsonCustomizer(Consumer<GsonBuilder> customizer) {
        this.gsonCustomizer = customizer;
        return this;
    }

    /**
     * Builds the {@link LocalConfigManager} instance applying conventions for any unspecified properties.
     *
     * @return The configured manager.
     */
    public LocalConfigManager<T> build() {
        ResolvedConfig<T> resolved = ConfigBuilderHelper.resolveDefaults(
                configClass,
                modId,
                configPath,
                logger,
                defaultFactory,
                provider,
                janksonCustomizer,
                gsonCustomizer,
                "LocalConfigBuilder"
        );
        return new LocalConfigManager<>(
                resolved.provider(),
                resolved.path(),
                resolved.logger(),
                resolved.tree()
        );
    }

    /**
     * Builds the manager and invokes {@link LocalConfigManager#register()} to perform initial disk loading/creation.
     *
     * @return The fully initialized and registered manager.
     */
    public LocalConfigManager<T> register() {
        LocalConfigManager<T> manager = build();
        manager.register();
        return manager;
    }
}
