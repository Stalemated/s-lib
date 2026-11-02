package com.stalemated.lib.config;

import com.stalemated.lib.config.model.OptionTree;

import java.util.function.Consumer;

public interface ConfigManager<T> {

    String getName();

    OptionTree getOptionTree();

    T get();

    T cloneConfig(T source);

    void modify(Consumer<T> mutator);

    Subscription onChange(Consumer<T> listener);

    default String toJson(Object value) {
        return getOptionTree().gson().toJson(value);
    }

    default <V> V fromJson(String json, Class<V> type) {
        return getOptionTree().gson().fromJson(json, type);
    }
}
