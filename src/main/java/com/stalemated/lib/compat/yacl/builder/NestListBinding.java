package com.stalemated.lib.compat.yacl.builder;

import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Describes how to read and write a list inside each POJO of a map, for
 * {@link YaclCategoryBuilder#nestedMapLists(String, NestListBinding)}. Client only.
 * <p>
 * Make one with an {@code of} method and change it with the {@code with} methods; each of them
 * returns a copy.
 *
 * @param <N> the POJO type of the map values
 * @param elementType class of the list elements; it must convert from text: a type registered with a {@code String} fallback, or a primitive, wrapper, enum or {@code String}
 * @param getter reads the list of a POJO; a {@code null} list counts as empty
 * @param setter writes a new list into a POJO
 * @param keys keys shown before the keys of the map, even when the map has no entry for them; it runs every time the screen is built
 * @param newEntry creates the entry of a shown key the map does not have yet, or {@code null} to only edit existing entries
 * @param entryName the name of the list of each key
 * @param description the description of every list, or {@code null} to use the description of the map
 */
public record NestListBinding<N>(
        Class<?> elementType,
        Function<N, ? extends List<?>> getter,
        BiConsumer<N, List<?>> setter,
        Supplier<? extends Collection<String>> keys,
        Supplier<N> newEntry,
        Function<String, Component> entryName,
        Component description
) {
    /**
     * Checks the components that can't be {@code null}.
     *
     * @param elementType class of the list elements
     * @param getter reads the list of a POJO
     * @param setter writes a new list into a POJO
     * @param keys keys shown before the keys of the map
     * @param newEntry creates a missing entry, or {@code null}
     * @param entryName the name of the list of each key
     * @param description the description of every list, or {@code null}
     * @throws NullPointerException if {@code elementType}, {@code getter}, {@code setter}, {@code keys} or {@code entryName} is {@code null}
     */
    public NestListBinding {
        Objects.requireNonNull(elementType, "elementType cannot be null");
        Objects.requireNonNull(getter, "getter cannot be null");
        Objects.requireNonNull(setter, "setter cannot be null");
        Objects.requireNonNull(keys, "keys cannot be null");
        Objects.requireNonNull(entryName, "entryName cannot be null");
    }

    /**
     * Creates a binding for a {@code List<String>}, with no extra keys and no {@code newEntry}.
     * Each list is named after its key, as plain text, and uses the description of the map.
     *
     * @param getter reads the list of a POJO
     * @param setter writes a new list into a POJO
     * @param <N> the POJO type of the map values
     * @return a new binding
     * @throws NullPointerException if {@code getter} is {@code null}
     */
    public static <N> NestListBinding<N> of(
            Function<N, List<String>> getter,
            BiConsumer<N, List<String>> setter
    ) {
        return of(String.class, getter, setter);
    }

    /**
     * Same as {@link #of(Function, BiConsumer)} for lists of {@code elementType}. The type must
     * convert from text: a type registered with a {@code String} fallback, or a primitive,
     * wrapper, enum or {@code String}. Otherwise building the screen fails.
     *
     * @param elementType class of the list elements
     * @param getter reads the list of a POJO
     * @param setter writes a new list into a POJO
     * @param <N> the POJO type of the map values
     * @param <E> the list element type
     * @return a new binding
     * @throws NullPointerException if {@code elementType} or {@code getter} is {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <N, E> NestListBinding<N> of(
            Class<E> elementType,
            Function<N, List<E>> getter,
            BiConsumer<N, List<E>> setter
    ) {
        return new NestListBinding<>(elementType, getter, (item, list) -> setter.accept(item, (List<E>) list), List::of, null, Component::literal, null);
    }

    /**
     * Returns a copy that also shows the keys from {@code keys}, for example every entry of a
     * registry, and creates the missing entries with {@code newEntry}.
     *
     * @param keys supplies the extra keys; it runs every time the screen is built
     * @param newEntry creates the entry of a shown key the map does not have yet
     * @return the copy
     * @throws NullPointerException if {@code keys} or {@code newEntry} is {@code null}
     */
    public NestListBinding<N> withKeys(Supplier<? extends Collection<String>> keys, Supplier<N> newEntry) {
        Objects.requireNonNull(newEntry, "newEntry cannot be null");
        return new NestListBinding<>(elementType, getter, setter, keys, newEntry, entryName, description);
    }

    /**
     * Returns a copy with another name for the list of each key.
     *
     * @param entryName makes the name of a list from its key
     * @return the copy
     * @throws NullPointerException if {@code entryName} is {@code null}
     */
    public NestListBinding<N> withEntryNames(Function<String, Component> entryName) {
        return new NestListBinding<>(elementType, getter, setter, keys, newEntry, entryName, description);
    }

    /**
     * Returns a copy with one description for every list, instead of the description of the map.
     *
     * @param description text shown as the description of each list
     * @return the copy
     * @throws NullPointerException if {@code description} is {@code null}
     */
    public NestListBinding<N> withDescription(Component description) {
        Objects.requireNonNull(description, "description cannot be null");
        return new NestListBinding<>(elementType, getter, setter, keys, newEntry, entryName, description);
    }
}
