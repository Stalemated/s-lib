package com.stalemated.lib.config;

import com.google.gson.GsonBuilder;
import com.stalemated.lib.config.io.ConfigLoadFailure;
import com.stalemated.lib.config.io.ConfigProvider;
import com.stalemated.lib.config.internal.FileConfigProvider;
import com.stalemated.lib.config.internal.Json5Serializer;
import com.stalemated.lib.config.internal.SLibGsonDefaults;
import com.stalemated.lib.util.io.PathUtils;
import com.stalemated.lib.util.internal.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Base fluent builder shared by {@link LocalConfigBuilder} and {@link SyncedConfigBuilder}.
 * <p>
 * Get one from {@link SLibConfig#local(String, Class)} or {@link SLibConfig#synced(String, Class)}
 * and use it from one thread during mod initialization. Each call to {@link #build()} creates a
 * new, separate manager.
 *
 * @param <T> the config model type
 * @param <M> the type of the built {@link ConfigManager}
 * @param <B> the concrete builder type, returned by every setter
 */
public abstract class ConfigBuilder<T, M extends ConfigManager<T>, B extends ConfigBuilder<T, M, B>> {

    final String modId;
    final Class<T> configClass;
    private Path configPath;
    private Logger logger;
    private Supplier<T> defaultFactory;
    private ConfigProvider<T> provider;
    private Consumer<GsonBuilder> gsonCustomizer = builder -> {};
    private final List<Consumer<T>> loadListeners = new ArrayList<>();
    private final List<Consumer<T>> saveListeners = new ArrayList<>();
    private final List<Consumer<T>> changeListeners = new ArrayList<>();
    private final List<Consumer<ConfigLoadFailure>> loadFailedListeners = new ArrayList<>();

    ConfigBuilder(String modId, Class<T> configClass) {
        this.modId = Objects.requireNonNull(modId, "modId cannot be null");
        this.configClass = Objects.requireNonNull(configClass, "configClass cannot be null");
    }

    abstract B self();

    abstract M create(LocalConfigManager<T> localManager);

    abstract void register(M manager);

    /**
     * Sets the path of the config file. Defaults to {@code config/<modId>.json5}.
     * <p>
     * The file name without its extension is also the name of the config in logs and errors.
     * Two registered configs cannot use the same file.
     *
     * @param path the config file
     * @return this builder
     * @throws NullPointerException if {@code path} is {@code null}
     */
    public B configPath(Path path) {
        this.configPath = Objects.requireNonNull(path, "path cannot be null");
        return self();
    }

    /**
     * Sets the SLF4J logger of this config. Defaults to {@code LoggerFactory.getLogger(modId)}.
     * <p>
     * It gets the messages about the file, like read errors, invalid values and backups, and the
     * warnings of {@code modify} in synced configs. Errors thrown by listeners and network errors
     * go to the S-Lib logger instead.
     *
     * @param logger the logger to use
     * @return this builder
     * @throws NullPointerException if {@code logger} is {@code null}
     */
    public B logger(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
        return self();
    }

    /**
     * Sets the factory that creates default instances of the config.
     * <p>
     * The supplier must return a new instance on every call; {@link #build()} rejects {@code null}
     * and the same instance twice. Without a factory, S-Lib calls the no-argument constructor of
     * the config class, even if it is private. The default value of each option and every copy of
     * the config come from this factory.
     *
     * @param defaultFactory returns a new default instance on every call
     * @return this builder
     * @throws NullPointerException if {@code defaultFactory} is {@code null}
     */
    public B defaultFactory(Supplier<T> defaultFactory) {
        this.defaultFactory = Objects.requireNonNull(defaultFactory, "defaultFactory cannot be null");
        return self();
    }

    /**
     * Replaces the default JSON5 file with your own {@link ConfigProvider}. Works for local and
     * synced configs.
     * <p>
     * The provider does not get the path, the logger or the default factory; it has to know them
     * itself. The path of {@link #configPath(Path)} is still the name of the config and the file
     * that two configs cannot share.
     *
     * @param provider loads and saves the config
     * @return this builder
     * @throws NullPointerException if {@code provider} is {@code null}
     */
    public B provider(ConfigProvider<T> provider) {
        this.provider = Objects.requireNonNull(provider, "provider cannot be null");
        return self();
    }

    /**
     * Changes the {@link GsonBuilder} of this config. S-Lib applies its defaults first: nulls are
     * written, {@code @Ignore} fields are skipped, and there are adapters for {@code String},
     * {@code Color}, {@code UUID}, {@code Pattern}, {@code ResourceLocation} and {@code TextColor}.
     * <p>
     * The resulting {@code Gson} is used for the file, deep copies, network packets and
     * {@link ConfigManager#toJson(Object)}. A field naming policy also changes the keys written to
     * the file. Calling this again replaces the previous customizer.
     *
     * @param customizer runs once inside {@link #build()}, after the S-Lib defaults
     * @return this builder
     * @throws NullPointerException if {@code customizer} is {@code null}
     */
    public B gsonCustomizer(Consumer<GsonBuilder> customizer) {
        this.gsonCustomizer = Objects.requireNonNull(customizer, "customizer cannot be null");
        return self();
    }

    /**
     * Adds a listener that runs after {@code register()} loads the config, with the loaded
     * instance. It also runs when the load failed; it then gets the values left in memory.
     *
     * @param listener gets the loaded config
     * @return this builder
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public B onLoad(Consumer<T> listener) {
        loadListeners.add(Objects.requireNonNull(listener, "listener cannot be null"));
        return self();
    }

    /**
     * Adds a listener that runs after each successful save, with the saved instance. It does not
     * run if writing fails. In a synced config it also runs on the server when it saves a change
     * sent by a client.
     *
     * @param listener gets the saved config
     * @return this builder
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public B onSave(Consumer<T> listener) {
        saveListeners.add(Objects.requireNonNull(listener, "listener cannot be null"));
        return self();
    }

    /**
     * Adds a listener that gets the current config after each {@code save()}, after a
     * {@code modify} that changed something and, in synced configs, when the values from the
     * server change. It does not run on load.
     * <p>
     * Same as {@link ConfigManager#onChange(Consumer)} on the built manager, but there is no
     * {@link Subscription} to remove it.
     *
     * @param listener gets the current config
     * @return this builder
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public B onChange(Consumer<T> listener) {
        changeListeners.add(Objects.requireNonNull(listener, "listener cannot be null"));
        return self();
    }

    /**
     * Adds a listener that runs when a load does not go as written: the file could not be read,
     * is not valid JSON5, had invalid values that were replaced by their defaults, or could not be
     * written or backed up. It runs before the {@link #onLoad(Consumer)} listeners.
     *
     * @param listener gets what went wrong
     * @return this builder
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public B onLoadFailed(Consumer<ConfigLoadFailure> listener) {
        loadFailedListeners.add(Objects.requireNonNull(listener, "listener cannot be null"));
        return self();
    }

    /**
     * Builds the manager without loading the file or registering network receivers. Call
     * {@code register()} on it before use, or use {@link #register()} instead.
     * <p>
     * Errors in the config class, like a bad range annotation, show up here.
     *
     * @return the new manager, not registered yet
     * @throws IllegalArgumentException if {@code defaultFactory} returns {@code null} or the same
     *                                  instance twice, or if the config schema is invalid
     * @throws RuntimeException if there is no default factory and the config class has no
     *                          no-argument constructor
     * @throws net.minecraft.ResourceLocationException if {@code modId} is not a valid namespace,
     *                                                 in a synced config without {@code syncChannel}
     */
    public M build() {
        if (defaultFactory != null) {
            LocalConfigManager.requireFreshInstances(modId, configClass, defaultFactory);
        }
        Supplier<T> factory = defaultFactory != null ? defaultFactory : () -> ReflectionUtils.tryInstantiate(configClass);
        Logger resolvedLogger = logger != null ? logger : LoggerFactory.getLogger(modId);
        Path path = configPath != null ? configPath : PathUtils.buildPath(modId + ".json5");
        OptionTree tree = new OptionTree(configClass, factory, SLibGsonDefaults.create(gsonCustomizer));
        ConfigProvider<T> resolvedProvider = provider != null
                ? provider
                : new FileConfigProvider<>(path, factory, resolvedLogger, new Json5Serializer<>(tree));

        LocalConfigManager<T> localManager = new LocalConfigManager<>(resolvedProvider, path, resolvedLogger, tree, factory);
        loadListeners.forEach(localManager::addLoadListener);
        saveListeners.forEach(localManager::addSaveListener);
        loadFailedListeners.forEach(localManager::addLoadFailedListener);

        M manager = create(localManager);
        changeListeners.forEach(manager::onChange);
        return manager;
    }

    /**
     * Builds the manager and registers it: the file is loaded, or created with defaults if it is
     * missing or empty. A synced config also claims its sync channel and registers its network
     * receivers.
     * <p>
     * Call it from your mod initializer, not from a static initializer, so the file loads at a
     * known point.
     *
     * @return the registered manager, ready for {@code get()}
     * @throws IllegalArgumentException if {@code defaultFactory} or the config schema is invalid
     * @throws IllegalStateException if another config already uses the same file or sync channel,
     *                               or another receiver is registered on one of its channels
     */
    public M register() {
        M manager = build();
        register(manager);
        return manager;
    }
}
