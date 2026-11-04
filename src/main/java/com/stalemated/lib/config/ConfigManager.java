package com.stalemated.lib.config;

import java.util.Map;
import java.util.function.Consumer;

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

    default String toJson(Object value) {
        return getOptionTree().gson().toJson(value);
    }

    default <V> V fromJson(String json, Class<V> type) {
        return getOptionTree().gson().fromJson(json, type);
    }
}
