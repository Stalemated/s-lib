package com.stalemated.lib.config;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.stalemated.lib.SLib.LOGGER;

final class Listeners<L> {

    private final List<L> listeners = new CopyOnWriteArrayList<>();
    private final Supplier<String> configName;

    Listeners(Supplier<String> configName) {
        this.configName = configName;
    }

    Subscription add(L listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    void fire(Consumer<L> call) {
        for (L listener : listeners) {
            try {
                call.accept(listener);
            } catch (RuntimeException e) {
                LOGGER.error("A listener of config '{}' failed; the remaining listeners still run", configName.get(), e);
            }
        }
    }
}
