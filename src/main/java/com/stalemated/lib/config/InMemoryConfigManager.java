package com.stalemated.lib.config;

import com.stalemated.lib.config.io.ConfigLoadFailure;
import com.stalemated.lib.config.io.ConfigProvider;
import com.stalemated.lib.config.model.OptionTree;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Config manager for a single POJO instance kept in memory, so it can be edited through
 * a {@code ConfigSession} and YACL screens without a config file.
 *
 * @param <T> The POJO model type.
 */
public final class InMemoryConfigManager<T> implements ConfigManager<T> {

    private final LocalConfigManager<T> manager;

    /**
     * @param instance The POJO instance to manage.
     * @param onSave   Optional callback executed when changes are committed.
     */
    public InMemoryConfigManager(T instance, Consumer<T> onSave) {
        this(instance, null, onSave);
    }

    /**
     * @param instance       The POJO instance to manage.
     * @param defaultFactory Optional supplier of new default instances, used for clones and defaults.
     * @param onSave         Optional callback executed when changes are committed.
     */
    @SuppressWarnings("unchecked")
    public InMemoryConfigManager(T instance, Supplier<T> defaultFactory, Consumer<T> onSave) {
        Objects.requireNonNull(instance, "instance cannot be null");
        Class<T> type = (Class<T>) instance.getClass();
        if (defaultFactory != null) {
            LocalConfigManager.requireFreshInstances(type.getSimpleName(), type, defaultFactory);
        }
        this.manager = LocalConfigManager.inMemory(new InstanceProvider<>(instance, onSave), OptionTree.of(type, defaultFactory), defaultFactory);
    }

    @Override
    public String getName() {
        return manager.getName();
    }

    @Override
    public OptionTree getOptionTree() {
        return manager.getOptionTree();
    }

    @Override
    public T get() {
        return manager.get();
    }

    @Override
    public T cloneConfig(T source) {
        return manager.cloneConfig(source);
    }

    @Override
    public void modify(Consumer<T> mutator) {
        manager.modify(mutator);
    }

    @Override
    public Subscription onChange(Consumer<T> listener) {
        return manager.onChange(listener);
    }

    private record InstanceProvider<T>(T instance, Consumer<T> onSave) implements ConfigProvider<T> {
        @Override
        public Optional<ConfigLoadFailure> load() {
            return Optional.empty();
        }

        @Override
        public void save() {
            if (onSave != null) onSave.accept(instance);
        }
    }
}
