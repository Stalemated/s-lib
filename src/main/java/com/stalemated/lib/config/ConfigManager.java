package com.stalemated.lib.config;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ConfigManager<T> {

    String getName();

    OptionTree getOptionTree();

    T get();

    T cloneConfig(T source);

    void modify(Consumer<T> mutator);

    default void modifyOptions(Map<OptionInfo, ?> values) {
        modify(copy -> values.forEach((option, value) -> option.setValue(copy, value)));
    }

    Subscription onChange(Consumer<T> listener);

    default <E> ConfigManager<E> editListElement(Function<T, List<E>> list, E element, Supplier<E> defaults) {
        return InMemoryConfigManager.forListElement(this, list, element, defaults);
    }

    default String toJson(Object value) {
        return getOptionTree().gson().toJson(value);
    }

    default <V> V fromJson(String json, Class<V> type) {
        return getOptionTree().gson().fromJson(json, type);
    }
}
