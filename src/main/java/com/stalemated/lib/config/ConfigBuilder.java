package com.stalemated.lib.config;

import com.google.gson.GsonBuilder;
import com.stalemated.lib.config.io.ConfigLoadFailure;
import com.stalemated.lib.config.io.ConfigProvider;
import com.stalemated.lib.config.io.FileConfigProvider;
import com.stalemated.lib.config.io.json5.Json5Serializer;
import com.stalemated.lib.config.model.OptionTree;
import com.stalemated.lib.util.io.PathUtils;
import com.stalemated.lib.util.reflection.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class ConfigBuilder<T, M extends LocalConfigManager<T>, B extends ConfigBuilder<T, M, B>> {

    final String modId;
    final Class<T> configClass;
    private Path configPath;
    private Logger logger;
    private Supplier<T> defaultFactory;
    private ConfigProvider<T> provider;
    private Consumer<GsonBuilder> gsonCustomizer;
    private final List<Consumer<T>> loadListeners = new ArrayList<>();
    private final List<Consumer<T>> saveListeners = new ArrayList<>();
    private final List<Consumer<T>> changeListeners = new ArrayList<>();
    private final List<Consumer<ConfigLoadFailure>> loadFailedListeners = new ArrayList<>();

    ConfigBuilder(String modId, Class<T> configClass) {
        this.modId = Objects.requireNonNull(modId, "modId cannot be null");
        this.configClass = Objects.requireNonNull(configClass, "configClass cannot be null");
    }

    abstract B self();

    abstract M create(ConfigProvider<T> provider, Path path, Logger logger, OptionTree tree, Supplier<T> factory);

    public B configPath(Path path) {
        this.configPath = Objects.requireNonNull(path, "path cannot be null");
        return self();
    }

    public B logger(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
        return self();
    }

    public B defaultFactory(Supplier<T> defaultFactory) {
        this.defaultFactory = Objects.requireNonNull(defaultFactory, "defaultFactory cannot be null");
        return self();
    }

    public B provider(ConfigProvider<T> provider) {
        this.provider = Objects.requireNonNull(provider, "provider cannot be null");
        return self();
    }

    public B gsonCustomizer(Consumer<GsonBuilder> customizer) {
        this.gsonCustomizer = Objects.requireNonNull(customizer, "customizer cannot be null");
        return self();
    }

    public B onLoad(Consumer<T> listener) {
        loadListeners.add(Objects.requireNonNull(listener, "listener cannot be null"));
        return self();
    }

    public B onSave(Consumer<T> listener) {
        saveListeners.add(Objects.requireNonNull(listener, "listener cannot be null"));
        return self();
    }

    public B onChange(Consumer<T> listener) {
        changeListeners.add(Objects.requireNonNull(listener, "listener cannot be null"));
        return self();
    }

    public B onLoadFailed(Consumer<ConfigLoadFailure> listener) {
        loadFailedListeners.add(Objects.requireNonNull(listener, "listener cannot be null"));
        return self();
    }

    /**
     * Builds the manager without loading the file. It must be registered before use.
     *
     * @return The configured manager.
     */
    public M build() {
        Supplier<T> factory = defaultFactory != null ? defaultFactory : () -> ReflectionUtils.tryInstantiate(configClass);
        Logger resolvedLogger = logger != null ? logger : LoggerFactory.getLogger(modId);
        Path path = configPath != null ? configPath : PathUtils.buildPath(modId + ".json5");
        OptionTree tree = new OptionTree(configClass, factory);
        ConfigProvider<T> resolvedProvider = provider != null
                ? provider
                : new FileConfigProvider<>(path, factory, resolvedLogger, new Json5Serializer<>(tree, gsonCustomizer));

        M manager = create(resolvedProvider, path, resolvedLogger, tree, factory);
        loadListeners.forEach(manager::addLoadListener);
        saveListeners.forEach(manager::addSaveListener);
        changeListeners.forEach(manager::onChange);
        loadFailedListeners.forEach(manager::addLoadFailedListener);
        return manager;
    }

    /**
     * Builds the manager and invokes {@link LocalConfigManager#register()} to perform initial disk loading/creation.
     *
     * @return The fully initialized and registered manager.
     */
    public M register() {
        M manager = build();
        manager.register();
        return manager;
    }
}
