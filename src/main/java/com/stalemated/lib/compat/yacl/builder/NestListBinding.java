package com.stalemated.lib.compat.yacl.builder;

import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Bidirectional binding (getter and setter) for dynamic list properties contained within nested POJO objects.
 * Encapsulates functional accessors into a cohesive, null-safe record.
 *
 * @param <N> The type of the enclosing POJO.
 * @param getter The function to extract the list from a POJO instance.
 * @param setter The consumer to update the list on a POJO instance.
 * @param keys Keys shown before the keys of the map, even when the map has no entry for them.
 * @param newEntry Creates the entry of a shown key the map does not have yet, or {@code null} to only edit existing entries.
 * @param entryName The name of the list option of each key.
 * @param description The description of every list option, or {@code null} to use the description of the map.
 */
public record NestListBinding<N>(
        Function<N, List<String>> getter,
        BiConsumer<N, List<String>> setter,
        Supplier<? extends Collection<String>> keys,
        Supplier<N> newEntry,
        Function<String, Component> entryName,
        Component description
) {
    public NestListBinding {
        Objects.requireNonNull(getter, "getter cannot be null");
        Objects.requireNonNull(setter, "setter cannot be null");
        Objects.requireNonNull(keys, "keys cannot be null");
        Objects.requireNonNull(entryName, "entryName cannot be null");
    }

    /**
     * Creates a new {@link NestListBinding} from the provided getter and setter.
     *
     * @param getter The list getter.
     * @param setter The list setter.
     * @param <N> The type of the enclosing POJO.
     * @return A new {@link NestListBinding} instance.
     */
    public static <N> NestListBinding<N> of(
            Function<N, List<String>> getter,
            BiConsumer<N, List<String>> setter
    ) {
        return new NestListBinding<>(getter, setter, List::of, null, Component::literal, null);
    }

    public NestListBinding<N> withKeys(Supplier<? extends Collection<String>> keys, Supplier<N> newEntry) {
        Objects.requireNonNull(newEntry, "newEntry cannot be null");
        return new NestListBinding<>(getter, setter, keys, newEntry, entryName, description);
    }

    public NestListBinding<N> withEntryNames(Function<String, Component> entryName) {
        return new NestListBinding<>(getter, setter, keys, newEntry, entryName, description);
    }

    public NestListBinding<N> withDescription(Component description) {
        Objects.requireNonNull(description, "description cannot be null");
        return new NestListBinding<>(getter, setter, keys, newEntry, entryName, description);
    }
}
