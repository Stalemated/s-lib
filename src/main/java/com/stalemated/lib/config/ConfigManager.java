package com.stalemated.lib.config;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Reads and changes a config. {@link LocalConfigManager}, {@link SyncedConfigManager} and
 * {@link InMemoryConfigManager} implement it.
 * <p>
 * Changes are not synchronized: call {@code modify} and {@code save} from the main thread of
 * their side.
 *
 * @param <T> the config model type
 */
public interface ConfigManager<T> {

    /**
     * Returns the name of this config for logs and errors: the file name without its extension
     * or, without a file, the simple name of the config class.
     *
     * @return the config name
     */
    String getName();

    /**
     * Returns the options of this config, scanned from its class.
     *
     * @return the option tree, never {@code null}
     */
    OptionTree getOptionTree();

    /**
     * Returns the current config.
     * <p>
     * This is the object the manager uses, so change it with {@link #modify(Consumer)} rather
     * than by hand. In a {@link SyncedConfigManager} on a server with the mod, it is the server's
     * copy: its {@code OVERRIDE_CLIENT} values and the local values for the rest.
     *
     * @return the current config, never {@code null} after registration
     * @throws IllegalStateException if the manager is not registered yet
     */
    T get();

    /**
     * Creates a deep copy of {@code source}, clamping each value.
     * <p>
     * Only the options are copied. {@code @Ignore}, {@code static} and {@code transient} fields
     * keep the values of the new instance.
     *
     * @param source the config to copy, or {@code null}
     * @return the copy, or {@code null} if {@code source} is {@code null}
     * @throws IllegalArgumentException if {@code source} has a value that can never be valid,
     *                                  like {@code NaN}
     */
    T cloneConfig(T source);

    /**
     * Runs {@code mutator} on a copy of the current config and applies what changed.
     * <p>
     * Values are clamped. If something changed, the config is saved once and the change
     * listeners run; if nothing changed, nothing is saved. If {@code mutator} throws, nothing is
     * applied. A {@link SyncedConfigManager} also sends the changes over the network.
     *
     * @param mutator changes the copy it gets
     * @throws NullPointerException if {@code mutator} is {@code null}
     * @throws IllegalStateException if the manager is not registered yet
     * @throws IllegalArgumentException if the mutator leaves a value that can never be valid,
     *                                  like {@code NaN}
     * @throws java.io.UncheckedIOException if saving fails; the new values stay in memory
     */
    void modify(Consumer<T> mutator);

    /**
     * Sets several options in one change, like {@link #modify(Consumer)}. Each value is clamped
     * first. Config screens save through this method.
     * <p>
     * The default implementation goes through {@code modify}. The S-Lib managers compare the
     * values with the current config directly, and check that every option comes from
     * {@link #getOptionTree()} before they apply anything.
     *
     * @param values new value per option, with the options taken from {@link #getOptionTree()}
     * @throws NullPointerException if {@code values} is {@code null}
     * @throws IllegalArgumentException if a value can never be valid, or an option belongs to
     *                                  another config
     * @throws RuntimeException if a value does not match the type of its field
     */
    default void modifyOptions(Map<OptionInfo, ?> values) {
        modify(copy -> values.forEach((option, value) -> option.setValue(copy, value)));
    }

    /**
     * Adds a listener that gets the current config after each save, after a
     * {@link #modify(Consumer)} that changed something and, in a {@link SyncedConfigManager},
     * when the values from the server change. It does not run on load.
     * <p>
     * If a listener throws, the error is logged and the other listeners still run.
     *
     * @param listener gets the current config
     * @return a {@link Subscription} that removes the listener
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    Subscription onChange(Consumer<T> listener);

    /**
     * Creates a manager to edit one element of a list in this config, for example in its own
     * config screen.
     * <p>
     * The element is found by identity in the list of {@link #get()}. Saving the returned
     * manager copies the element and writes it back at the same index with
     * {@link #modify(Consumer)} on this config, so code that reacts to the save goes in
     * {@link #onChange(Consumer)} of this config. If the list gets shorter than that index, the
     * save fails with an {@code IllegalStateException}.
     *
     * @param list returns the live, mutable list of a config instance
     * @param element an element taken from {@link #get()} after the last save
     * @param defaults returns a new default element on every call
     * @param <E> the list element type
     * @return a manager for that element; it needs no {@code register()}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code element} is not in the list
     */
    default <E> ConfigManager<E> editListElement(Function<T, List<E>> list, E element, Supplier<E> defaults) {
        return InMemoryConfigManager.forListElement(this, list, element, defaults);
    }

    /**
     * Creates a manager to edit a new element for a list in this config. The element starts
     * with the values from {@code defaults}.
     * <p>
     * The first save adds the element at the end of the list. Later saves replace that same
     * element. If the list gets shorter than its index, the save fails with an
     * {@code IllegalStateException}.
     *
     * @param list returns the live, mutable list of a config instance
     * @param defaults returns a new default element on every call
     * @param <E> the list element type
     * @return a manager for the new element; it needs no {@code register()}
     * @throws NullPointerException if any argument is {@code null}
     */
    default <E> ConfigManager<E> addListElement(Function<T, List<E>> list, Supplier<E> defaults) {
        return InMemoryConfigManager.forNewListElement(this, list, defaults);
    }

    /**
     * Serializes a value with the {@code Gson} of this config, with the S-Lib adapters and the
     * {@code gsonCustomizer} of the builder. The result is compact JSON, not JSON5 with comments.
     *
     * @param value any object, or {@code null}
     * @return the JSON text
     */
    default String toJson(Object value) {
        return getOptionTree().gson().toJson(value);
    }

    /**
     * Deserializes JSON with the {@code Gson} of this config.
     *
     * @param json the JSON text
     * @param type the class to read
     * @param <V> the type to read
     * @return the value, or {@code null} if the JSON is {@code null}
     * @throws com.google.gson.JsonSyntaxException if {@code json} is not valid JSON for {@code type}
     */
    default <V> V fromJson(String json, Class<V> type) {
        return getOptionTree().gson().fromJson(json, type);
    }
}
