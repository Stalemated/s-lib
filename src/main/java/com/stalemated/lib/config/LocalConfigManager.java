package com.stalemated.lib.config;

import com.stalemated.lib.config.internal.ConfigRegistry;
import com.stalemated.lib.config.internal.ConfigSerializer;
import com.stalemated.lib.config.internal.OptionChange;
import com.stalemated.lib.config.io.ConfigLoadFailure;
import com.stalemated.lib.config.io.ConfigProvider;
import com.stalemated.lib.util.internal.ReflectionUtils;
import org.slf4j.Logger;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Config manager that loads and saves a config through a {@link ConfigProvider}.
 *
 * @param <T> The config data model class.
 */
public final class LocalConfigManager<T> implements ConfigManager<T> {

    private final ConfigProvider<T> provider;
    private final Path configPath;
    private final Logger logger;
    private final OptionTree optionTree;
    private final Supplier<T> defaultFactory;

    private final Listeners<Consumer<T>> loadListeners = new Listeners<>(this::getName);
    private final Listeners<Consumer<T>> saveListeners = new Listeners<>(this::getName);
    private final Listeners<Consumer<T>> changeListeners = new Listeners<>(this::getName);
    private final Listeners<Consumer<ConfigLoadFailure>> loadFailedListeners = new Listeners<>(this::getName);

    private boolean registered = false;

    LocalConfigManager(ConfigProvider<T> provider, Path configPath, Logger logger, OptionTree optionTree, Supplier<T> defaultFactory) {
        this.provider = provider;
        this.configPath = configPath;
        this.logger = logger;
        this.optionTree = optionTree;
        this.defaultFactory = defaultFactory;
    }

    static <T> LocalConfigManager<T> inMemory(ConfigProvider<T> provider, OptionTree optionTree, Supplier<T> defaultFactory) {
        LocalConfigManager<T> manager = new LocalConfigManager<>(provider, null, null, optionTree, defaultFactory);
        manager.registered = true;
        return manager;
    }

    static void requireFreshInstances(String name, Class<?> type, Supplier<?> defaultFactory) {
        Object first = defaultFactory.get();
        if (first == null) {
            throw new IllegalArgumentException("defaultFactory of '" + name + "' returned null instead of a " + type.getSimpleName());
        }
        if (first == defaultFactory.get()) {
            throw new IllegalArgumentException("defaultFactory of '" + name + "' must create a new " + type.getSimpleName() + " on every call, got the same instance twice");
        }
    }

    static <T> List<OptionChange> changesMadeBy(ConfigManager<T> manager, T current, Consumer<T> mutator) {
        T workingCopy = manager.cloneConfig(current);
        mutator.accept(workingCopy);
        return manager.getOptionTree().findChanges(current, workingCopy);
    }

    static <T> List<OptionChange> changesIn(ConfigManager<T> manager, T current, Map<OptionInfo, ?> values) {
        OptionTree tree = manager.getOptionTree();
        for (OptionInfo option : values.keySet()) {
            if (tree.get(option.getKey()) != option) {
                throw new IllegalArgumentException("Option '" + option.getKey() + "' does not belong to config '" + manager.getName() + "'");
            }
        }
        List<OptionChange> changes = new ArrayList<>();
        values.forEach((option, value) -> {
            Object clamped = option.clampValue(value);
            if (!tree.sameValue(option.getValue(current), clamped)) {
                changes.add(new OptionChange(option, clamped));
            }
        });
        return changes;
    }

    @Override
    public Subscription onChange(Consumer<T> listener) {
        return changeListeners.add(listener);
    }

    void addLoadListener(Consumer<T> listener) {
        loadListeners.add(listener);
    }

    void addSaveListener(Consumer<T> listener) {
        saveListeners.add(listener);
    }

    void addLoadFailedListener(Consumer<ConfigLoadFailure> listener) {
        loadFailedListeners.add(listener);
    }

    /**
     * Registers the config.
     * This claims the config file, loads it (creating it with defaults when it is new),
     * and invokes the load and load failure listeners.
     *
     * @throws IllegalStateException If another registered config already uses the same file.
     */
    public void register() {
        registerClaimedBy(() -> ConfigRegistry.claimFile(this, getConfigPath()));
    }

    void registerClaimedBy(Runnable claim) {
        if (registered) {
            throw new IllegalStateException("Config '" + getName() + "' is already registered");
        }
        claim.run();
        registered = true;

        Optional<ConfigLoadFailure> failure = provider.load();
        failure.ifPresent(f -> loadFailedListeners.fire(listener -> listener.accept(f)));

        T loaded = provider.instance();
        loadListeners.fire(listener -> listener.accept(loaded));
    }

    /**
     * Creates a deep clone of the provided config instance based on the {@link OptionTree} schema.
     *
     * @param source The config object to clone.
     * @return A fresh instance with all option values copied, or {@code null} if source is null.
     */
    @Override
    @SuppressWarnings("unchecked")
    public T cloneConfig(T source) {
        if (source == null) return null;
        T copy = defaultFactory != null
                ? defaultFactory.get()
                : ReflectionUtils.tryInstantiate((Class<T>) optionTree.getRootClass());
        optionTree.copyAllValues(source, copy);
        return copy;
    }

    T createDefaultOrClone() {
        return defaultFactory != null ? defaultFactory.get() : cloneConfig(get());
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
     * @throws UncheckedIOException If the changed config could not be written, the new values stay in memory.
     */
    @Override
    public void modify(Consumer<T> mutator) {
        applyChanges(changesMadeBy(this, get(), mutator));
    }

    @Override
    public void modifyOptions(Map<OptionInfo, ?> values) {
        applyChanges(changesIn(this, get(), values));
    }

    private void applyChanges(List<OptionChange> changes) {
        if (changes.isEmpty()) return;

        T current = get();
        for (OptionChange change : changes) {
            change.option().setValue(current, change.clampedValue());
        }
        save();
    }

    /**
     * Clamps the stored config, writes it to disk, and invokes the save and change listeners.
     * Fields mutated directly are only clamped here, and synced configs never send them over the network;
     * use {@link #modify(Consumer)} for both.
     *
     * @throws UncheckedIOException If the file could not be written. Change listeners still run, save listeners do not.
     */
    public void save() {
        requireRegistered();
        T stored = provider.instance();
        for (OptionInfo option : optionTree.all()) {
            option.enforceLimits(stored);
        }

        try {
            provider.save();
            saveListeners.fire(listener -> listener.accept(stored));
        } finally {
            fireChanged();
        }
    }

    void fireChanged() {
        T current = get();
        changeListeners.fire(listener -> listener.accept(current));
    }

    private void requireRegistered() {
        if (!registered) {
            throw new IllegalStateException("Config '" + getName() + "' was used before register() loaded it; call register() first");
        }
    }

    /**
     * Retrieves the config values in effect. Never null after {@link #register()}.
     *
     * @return The config object.
     * @throws IllegalStateException If called before {@link #register()}.
     */
    @Override
    public T get() {
        requireRegistered();
        return provider.instance();
    }

    boolean hasSerializer() {
        return provider.getSerializer() != null;
    }

    ConfigSerializer<T> serializer() {
        ConfigSerializer<T> serializer = provider.getSerializer();
        if (serializer == null) {
            throw new IllegalStateException("Config '" + getName() + "' uses a provider without a serializer, so it cannot convert values to JSON");
        }
        return serializer;
    }

    Logger logger() {
        return logger;
    }

    /**
     * Retrieves the option tree schema descriptor.
     *
     * @return The option tree.
     */
    @Override
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
    @Override
    public String getName() {
        if (configPath != null && configPath.getFileName() != null) {
            String fileName = configPath.getFileName().toString();
            int dot = fileName.lastIndexOf('.');
            return dot > 0 ? fileName.substring(0, dot) : fileName;
        }
        return optionTree.getRootClass().getSimpleName();
    }
}
