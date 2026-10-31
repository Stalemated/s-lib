package com.stalemated.lib.compat.yacl.registry;

import java.util.Objects;
import java.util.function.Function;

public record TypeLens<M, G>(Function<M, G> toGui, Function<G, M> fromGui, G fallback, ControllerFactory<G> controller) {

    public TypeLens {
        Objects.requireNonNull(toGui, "toGui cannot be null");
        Objects.requireNonNull(fromGui, "fromGui cannot be null");
        Objects.requireNonNull(controller, "controller cannot be null");
    }

    public static <V> TypeLens<V, V> identity(V fallback, ControllerFactory<V> controller) {
        return new TypeLens<>(Function.identity(), Function.identity(), fallback, controller);
    }

    public G guiOf(M model, G whenAbsent) {
        G gui = model != null ? toGui.apply(model) : null;
        return gui != null ? gui : whenAbsent;
    }
}
