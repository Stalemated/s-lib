package com.stalemated.lib.compat.yacl.builder;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Bidirectional binding (getter and setter) for dynamic list properties contained within nested POJO objects.
 * Encapsulates functional accessors into a cohesive, null-safe record.
 *
 * @param <N> The type of the enclosing POJO.
 * @param getter The function to extract the list from a POJO instance.
 * @param setter The consumer to update the list on a POJO instance.
 */
public record NestListBinding<N>(
        Function<N, List<String>> getter,
        BiConsumer<N, List<String>> setter
) {
    public NestListBinding {
        Objects.requireNonNull(getter, "getter cannot be null");
        Objects.requireNonNull(setter, "setter cannot be null");
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
        return new NestListBinding<>(getter, setter);
    }
}
