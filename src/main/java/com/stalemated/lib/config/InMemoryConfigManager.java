package com.stalemated.lib.config;

import com.stalemated.lib.config.io.ConfigLoadFailure;
import com.stalemated.lib.config.io.ConfigProvider;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
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
        this.manager = LocalConfigManager.inMemory(new InstanceProvider<>(instance, onSave), new OptionTree(type, defaultFactory), defaultFactory);
    }

    static <P, E> InMemoryConfigManager<E> forListElement(ConfigManager<P> owner, Function<P, List<E>> list, E element, Supplier<E> defaults) {
        Objects.requireNonNull(owner, "owner cannot be null");
        Objects.requireNonNull(list, "list cannot be null");
        Objects.requireNonNull(element, "element cannot be null");
        Objects.requireNonNull(defaults, "defaults cannot be null");

        int index = indexOf(list.apply(owner.get()), element);
        if (index == ListElementWriter.NOT_IN_LIST) {
            throw new IllegalArgumentException("The element to edit is not in the list of '" + owner.getName()
                    + "'; take it from get() after the last save, or use addListElement for a new one");
        }
        return listElementManager(owner, list, index, element, defaults);
    }

    static <P, E> InMemoryConfigManager<E> forNewListElement(ConfigManager<P> owner, Function<P, List<E>> list, Supplier<E> defaults) {
        Objects.requireNonNull(owner, "owner cannot be null");
        Objects.requireNonNull(list, "list cannot be null");
        Objects.requireNonNull(defaults, "defaults cannot be null");
        return listElementManager(owner, list, ListElementWriter.NOT_IN_LIST, defaults.get(), defaults);
    }

    private static <P, E> InMemoryConfigManager<E> listElementManager(ConfigManager<P> owner, Function<P, List<E>> list, int index, E initial, Supplier<E> defaults) {
        ListElementWriter<P, E> writer = new ListElementWriter<>(owner, list, index);
        InMemoryConfigManager<E> elementManager = new InMemoryConfigManager<>(defaults.get(), defaults, writer);
        elementManager.getOptionTree().copyAllValues(initial, elementManager.get());
        writer.elementManager = elementManager;
        return elementManager;
    }

    private static <E> int indexOf(List<E> elements, E element) {
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i) == element) return i;
        }
        return ListElementWriter.NOT_IN_LIST;
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
    public void modifyOptions(Map<OptionInfo, ?> values) {
        manager.modifyOptions(values);
    }

    @Override
    public Subscription onChange(Consumer<T> listener) {
        return manager.onChange(listener);
    }

    private static final class ListElementWriter<P, E> implements Consumer<E> {
        private static final int NOT_IN_LIST = -1;

        private final ConfigManager<P> owner;
        private final Function<P, List<E>> list;
        private int index;
        private InMemoryConfigManager<E> elementManager;

        private ListElementWriter(ConfigManager<P> owner, Function<P, List<E>> list, int index) {
            this.owner = owner;
            this.list = list;
            this.index = index;
        }

        @Override
        public void accept(E saved) {
            E copy = elementManager.cloneConfig(saved);
            owner.modify(config -> {
                List<E> elements = list.apply(config);
                if (index == NOT_IN_LIST) {
                    index = elements.size();
                    elements.add(copy);
                } else if (index < elements.size()) {
                    elements.set(index, copy);
                } else {
                    throw new IllegalStateException("The list of '" + owner.getName() + "' has " + elements.size() + " elements, cannot replace element " + index);
                }
            });
        }
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
