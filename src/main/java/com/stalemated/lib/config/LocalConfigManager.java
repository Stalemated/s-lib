package com.stalemated.lib.config;

import com.stalemated.lib.config.io.ConfigLoadFailure;
import com.stalemated.lib.config.io.ConfigProvider;
import com.stalemated.lib.config.io.ConfigSerializer;
import com.stalemated.lib.config.model.OptionChange;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.OptionTree;
import com.stalemated.lib.util.reflection.ReflectionUtils;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
            public Optional<ConfigLoadFailure> load() {
                return Optional.empty();
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
     * and invokes the load, load failure, and change listeners.
     *
     * @throws IllegalStateException If another registered config already uses the same file or sync channel.
     */
    public final void register() {
        onRegister();
        registered = true;

        Optional<ConfigLoadFailure> failure = provider.load();
        failure.ifPresent(f -> loadFailedListeners.fire(listener -> listener.accept(f)));

        T loaded = provider.instance();
        loadListeners.fire(listener -> listener.accept(loaded));
        fireChanged();
    }

    void onRegister() {
        ConfigRegistry.claimFile(this);
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
        T current = get();

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
     * Clamps the stored config, writes it to disk, and invokes the save and change listeners.
     * Fields mutated directly are only clamped here, and synced configs never send them over the network;
     * use {@link #modify(Consumer)} for both.
     */
    public final void save() {
        requireRegistered();
        T stored = provider.instance();
        for (OptionInfo option : optionTree.all()) {
            option.enforceLimits(stored);
        }

        provider.save();
        saveListeners.fire(listener -> listener.accept(stored));
        fireChanged();
    }

    final void fireChanged() {
        T current = get();
        changeListeners.fire(listener -> listener.accept(current));
    }

    final void requireRegistered() {
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
    public T get() {
        requireRegistered();
        return provider.instance();
    }

    public String toJson(Object value) {
        return serializer().serializeOption(value);
    }

    public <V> V fromJson(String json, Class<V> type) {
        return type.cast(serializer().deserializeOption(json, type));
    }

    ConfigSerializer<T> serializer() {
        ConfigSerializer<T> serializer = provider.getSerializer();
        if (serializer == null) {
            throw new IllegalStateException("Config '" + getName() + "' uses a provider without a serializer, so it cannot convert values to JSON");
        }
        return serializer;
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
     * @return The config path, or null for managers created with {@code forInstance}.
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
        return optionTree.getRootClass().getSimpleName();
    }
}
