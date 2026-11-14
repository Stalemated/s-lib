package com.stalemated.lib.config;

import com.stalemated.lib.config.internal.ConfigRegistry;
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
 * Config manager that loads and saves a config through a {@link ConfigProvider}, by default a
 * JSON5 file. It never uses the network.
 * <p>
 * Get one from {@link SLibConfig#local(String, Class)} with {@code build()} or
 * {@code register()}. Call {@link #register()} before reading or changing the config.
 *
 * @param <T> the config model type
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
     * Registers the config: claims the file, loads it, runs the load failure listeners if the
     * load failed, and then the load listeners.
     * <p>
     * With the default JSON5 file, a missing or empty file is created with defaults. The file is
     * rewritten when options are missing, clamped or stored under an old key. Before it drops
     * unknown keys or invalid values, S-Lib saves a backup named
     * {@code <name>_backup_<yyyyMMdd-HHmmss>.json5}. A file that is not valid JSON5 is backed up
     * and left as it is, and the config keeps its defaults.
     *
     * @throws IllegalStateException if this manager is already registered, or if another
     *                               registered config already uses the same file
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
     * Creates a deep copy of {@code source}, clamping each value. The copy comes from the default
     * factory or, without one, from the no-argument constructor of the config class.
     *
     * @param source the config to copy, or {@code null}
     * @return the copy, or {@code null} if {@code source} is {@code null}
     * @throws IllegalArgumentException if {@code source} has a value that can never be valid,
     *                                  like {@code NaN}
     * @throws RuntimeException if there is no default factory and the config class has no
     *                          no-argument constructor
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
     * Runs {@code mutator} on a copy of the config and applies what changed. Values are clamped,
     * and if something changed the config is saved once and the change listeners run. If
     * {@code mutator} throws, the config stays as it was. {@code @Sync} modes are ignored.
     *
     * @param mutator changes the copy it gets
     * @throws IllegalStateException if called before {@link #register()}
     * @throws IllegalArgumentException if the mutator leaves a value that can never be valid,
     *                                  like {@code NaN}
     * @throws UncheckedIOException if writing to disk fails; the new values stay in memory
     */
    @Override
    public void modify(Consumer<T> mutator) {
        applyChanges(changesMadeBy(this, get(), mutator));
    }

    /**
     * Sets several options in one change. Every option must come from {@link #getOptionTree()};
     * this is checked before anything is applied. Each value is clamped, values that do not
     * change are skipped, and if anything changed the config is saved once.
     *
     * @param values new value per option
     * @throws IllegalStateException if called before {@link #register()}
     * @throws IllegalArgumentException if an option belongs to another config, or a value can
     *                                  never be valid
     * @throws RuntimeException if a value does not match the type of its field
     * @throws UncheckedIOException if writing to disk fails
     */
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
     * Clamps the stored config, writes it with the {@link ConfigProvider} and runs the save and
     * change listeners. The default JSON5 file is written to a {@code .tmp} file first and then
     * moved over the old one.
     * <p>
     * Use it after changing fields of {@link #get()} by hand; they are only clamped here. From
     * code, {@link #modify(Consumer)} is better: it only saves and runs the listeners when
     * something changed.
     *
     * @throws IllegalStateException if called before {@link #register()}
     * @throws UncheckedIOException if writing fails; change listeners still run, save listeners
     *                              do not
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
     * Returns the current config: the live instance of the provider.
     *
     * @return the current config, never {@code null}
     * @throws IllegalStateException if called before {@link #register()}
     */
    @Override
    public T get() {
        requireRegistered();
        return provider.instance();
    }

    Logger logger() {
        return logger;
    }

    /**
     * Returns the options of this config, scanned from its class.
     *
     * @return the option tree
     */
    @Override
    public OptionTree getOptionTree() {
        return optionTree;
    }

    /**
     * Returns the path of the config file: the one set with {@code configPath} or the default
     * {@code config/<modId>.json5}, as it was given.
     *
     * @return the config file path
     */
    public Path getConfigPath() {
        return configPath;
    }

    /**
     * Returns the file name without its last extension or, without a path, the simple name of the
     * config class.
     *
     * @return the config name
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
