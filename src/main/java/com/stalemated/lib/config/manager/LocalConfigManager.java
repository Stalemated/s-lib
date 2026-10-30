package com.stalemated.lib.config.manager;

import com.stalemated.lib.config.io.ConfigProvider;
import com.stalemated.lib.config.model.OptionChange;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.OptionTree;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import com.stalemated.lib.util.reflection.ReflectionUtils;

/**
 * Base config manager for handling local configs.
 * Provides default disk read/write strategies via a {@link ConfigProvider}.
 *
 * @param <T> The config data model class.
 */
public class LocalConfigManager<T> {
    
    protected final ConfigProvider<T> provider;
    protected final Path configPath;
    protected final Logger logger;
    protected final OptionTree optionTree;
    protected final Supplier<T> defaultFactory;
    
    protected final List<Consumer<T>> loadListeners = new ArrayList<>();
    protected final List<Consumer<T>> saveListeners = new ArrayList<>();

    public boolean configLoadFailed = false;

    private boolean registered = false;

    /**
     * Constructs a new LocalConfigManager using reflection to create default instances.
     * 
     * @param provider A config provider.
     * @param configPath The absolute path to the config file.
     * @param logger The mod's logger used for warnings and error reporting.
     * @param optionTree The option tree mapping the configuration model.
     */
    public LocalConfigManager(ConfigProvider<T> provider, Path configPath, Logger logger, OptionTree optionTree) {
        this(provider, configPath, logger, optionTree, null);
    }

    /**
     * Constructs a new LocalConfigManager.
     * 
     * @param provider A config provider.
     * @param configPath The absolute path to the config file.
     * @param logger The mod's logger used for warnings and error reporting.
     * @param optionTree The option tree mapping the config model.
     * @param defaultFactory An optional supplier providing default instances.
     */
    public LocalConfigManager(ConfigProvider<T> provider, Path configPath, Logger logger, OptionTree optionTree, Supplier<T> defaultFactory) {
        this.provider = provider;
        this.configPath = configPath;
        this.logger = logger;
        this.optionTree = optionTree;
        this.defaultFactory = defaultFactory;
    }

    /**
     * Creates a lightweight, in-memory {@link LocalConfigManager} wrapping a single POJO instance
     * so it can participate in transactional {@code ConfigSession} buffers and YACL screens.
     *
     * @param instance The POJO instance to manage.
     * @param onSave   Optional callback executed when changes are committed and saved.
     * @param <T>      The POJO model type.
     * @return An in-memory {@link LocalConfigManager} for the given instance.
     */
    public static <T> LocalConfigManager<T> forInstance(T instance, Consumer<T> onSave) {
        return forInstance(instance, null, onSave);
    }

    /**
     * Creates a lightweight, {@link LocalConfigManager} in memory, wrapping a single POJO instance
     * with an explicit default instance factory.
     *
     * @param instance       The POJO instance to manage.
     * @param defaultFactory Optional supplier providing default instances for cloning and defaults.
     * @param onSave         Optional callback executed when changes are committed and saved.
     * @param <T>            The POJO model type.
     * @return An in-memory {@link LocalConfigManager} for the given instance.
     */
    @SuppressWarnings("unchecked")
    public static <T> LocalConfigManager<T> forInstance(T instance, Supplier<T> defaultFactory, Consumer<T> onSave) {
        Objects.requireNonNull(instance, "instance cannot be null");
        Class<T> clazz = (Class<T>) instance.getClass();
        OptionTree tree = OptionTree.of(clazz, defaultFactory);

        ConfigProvider<T> provider = new ConfigProvider<>() {
            @Override
            public T instance() {
                return instance;
            }

            @Override
            public boolean load() {
                return true;
            }

            @Override
            public void save() {
                if (onSave != null) {
                    onSave.accept(instance);
                }
            }
        };
        LocalConfigManager<T> manager = new LocalConfigManager<>(provider, null, null, tree, defaultFactory);
        manager.registered = true;
        return manager;
    }

    /**
     * Registers a listener to be invoked when the config is successfully loaded or initialized.
     */
    public void onConfigLoaded(Consumer<T> listener) {
        loadListeners.add(listener);
    }

    /**
     * Registers a listener to be invoked when the config is successfully saved to disk.
     */
    public void onConfigSaved(Consumer<T> listener) {
        saveListeners.add(listener);
    }

    /**
     * Registers the config.
     * This handles file verification, loading the config, creating backups if loading fails,
     * saving defaults if the file is new, and invoking lifecycle listeners.
     */
    public final void register() {
        registered = true;
        configLoadFailed = !provider.load();

        for (Consumer<T> listener : loadListeners) {
            listener.accept(getConfig());
        }
    }

    /**
     * Creates a deep clone of the provided config instance based on the {@link OptionTree} schema.
     *
     * @param source The config object to clone.
     * @return A fresh instance with all option values copied, or {@code null} if source is null.
     */
    @SuppressWarnings("unchecked")
    public T cloneConfig(T source) {
        if (source == null) return null;
        T copy = defaultFactory != null
                ? defaultFactory.get()
                : ReflectionUtils.tryInstantiate((Class<T>) optionTree.getRootClass());
        optionTree.copyAllValues(source, copy);
        return copy;
    }

    /**
     * Executes a transactional modification on the config instance.
     * <p>
     * Modifications made within the {@code mutator} are validated, clamped against metadata
     * boundaries (e.g. {@code @RangeInt}), and atomically saved to disk if any value changed.
     * If an uncaught exception occurs during mutator execution, the active configuration
     * remains unmutated (transaction isolation).
     *
     * @param mutator The consumer performing strongly typed property updates on the working copy.
     */
    public void modify(Consumer<T> mutator) {
        requireRegistered();
        T current = getConfig();
        if (current == null) return;

        T workingCopy = cloneConfig(current);
        mutator.accept(workingCopy);

        List<OptionChange> changes = optionTree.findChanges(current, workingCopy);
        if (changes.isEmpty()) return;

        for (OptionChange change : changes) {
            change.option().setValue(current, change.clampedValue());
        }
        save();
    }

    /**
     * Saves the current config instance to disk and invokes the save listeners.
     */
    public final void save() {
        requireRegistered();
        T instance = provider.instance();
        if (instance != null && optionTree != null) {
            for (OptionInfo option : optionTree.all()) {
                option.enforceLimits(instance);
            }
        }
        
        provider.save();
        for (Consumer<T> listener : saveListeners) {
            listener.accept(getConfig());
        }
    }

    protected final void requireRegistered() {
        if (!registered) {
            throw new IllegalStateException("Config '" + getName() + "' was changed before register() loaded it; call register() first");
        }
    }

    /**
     * Retrieves the config provider handling this manager's I/O.
     *
     * @return The config provider.
     */
    public ConfigProvider<T> getProvider() {
        return provider;
    }

    /**
     * Retrieves the current config instance.
     * 
     * @return The config object.
     */
    public T getConfig() {
        return provider.instance();
    }

    /**
     * Retrieves the active config instance. For local configs, this is identical to {@link #getConfig()}.
     *
     * @return The active config object.
     */
    public T getActiveConfig() {
        return getConfig();
    }

    /**
     * Retrieves the option tree schema descriptor.
     *
     * @return The option tree.
     */
    public OptionTree getOptionTree() {
        return optionTree;
    }

    /**
     * Retrieves the path to the config file.
     *
     * @return The config path.
     */
    public Path getConfigPath() {
        return configPath;
    }

    /**
     * Retrieves a human-readable name or identifier for this config manager.
     * Defaults to the filename without extension or the root class simple name.
     *
     * @return The config name.
     */
    public String getName() {
        if (configPath != null && configPath.getFileName() != null) {
            String fileName = configPath.getFileName().toString();
            int dot = fileName.lastIndexOf('.');
            return dot > 0 ? fileName.substring(0, dot) : fileName;
        }
        return optionTree != null ? optionTree.getRootClass().getSimpleName() : "Config";
    }
}
