package com.stalemated.lib.client.internal;

import net.minecraft.client.gui.screens.Screen;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class ConfigScreenFactories {

    private static final Map<String, Function<Screen, Screen>> FACTORIES = new ConcurrentHashMap<>();
    private static final Map<String, Function<Screen, Screen>> VIEW = Collections.unmodifiableMap(FACTORIES);

    private ConfigScreenFactories() {}

    public static void add(String modId, Function<Screen, Screen> factory) {
        if (FACTORIES.putIfAbsent(modId, factory) != null) {
            throw new IllegalStateException("A config screen for '" + modId + "' is already registered");
        }
    }

    public static Map<String, Function<Screen, Screen>> view() {
        return VIEW;
    }
}
