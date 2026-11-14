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
 * Config manager for one object kept in memory, with no file and no network. Use it to edit any
 * POJO in a config screen or in tests. The managers of {@link ConfigManager#editListElement} and
 * {@link ConfigManager#addListElement} are in-memory managers too.
 * <p>
 * It is ready as soon as it is created and has no {@code register()}. Saving changes the object
 * in place and calls the {@code onSave} callback; nothing is written to disk.
 *
 * @param <T> the config model type
 */
public final class InMemoryConfigManager<T> implements ConfigManager<T> {

    private final LocalConfigManager<T> manager;

    /**
     * Creates a manager for {@code instance}. Defaults and copies come from the no-argument
     * constructor of its class.
     *
     * @param instance the object to edit; it is changed in place
     * @param onSave called with {@code instance} after each save, or {@code null}
     * @throws NullPointerException if {@code instance} is {@code null}
     * @throws IllegalArgumentException if the class of {@code instance} is not a valid config
     *                                  schema
     * @throws RuntimeException if the class of {@code instance} has no no-argument constructor
     */
    public InMemoryConfigManager(T instance, Consumer<T> onSave) {
        this(instance, null, onSave);
    }

    /**
     * Creates a manager for {@code instance}. The schema is scanned from the class of
     * {@code instance}, not from {@code T}.
     * <p>
     * The defaults shown in screens come from {@code defaultFactory}, or from a new instance of
     * the class, not from {@code instance}. The manager uses the default S-Lib {@code Gson},
     * without a customizer. An exception thrown by {@code onSave} reaches the caller of
     * {@code modify}.
     *
     * @param instance the object to edit; it is changed in place
     * @param defaultFactory returns a new default instance on every call, or {@code null} to use
     *                       the no-argument constructor
     * @param onSave called with {@code instance} after each save, or {@code null}
     * @throws NullPointerException if {@code instance} is {@code null}
     * @throws IllegalArgumentException if {@code defaultFactory} returns {@code null} or the same
     *                                  instance twice, or if the config schema is invalid
     * @throws RuntimeException if {@code defaultFactory} is {@code null} and the class has no
     *                          no-argument constructor
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

    /**
     * Returns the object this manager edits. There is no {@code register()}, so it never throws.
     *
     * @return the edited object, never {@code null}
     */
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
