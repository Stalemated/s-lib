package com.stalemated.lib.config.io.json5;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;

public final class DeepCopy {

    private static final Gson GSON = createGson();

    private DeepCopy() {}

    public static <V> V of(V value, Type type) {
        if (isImmutableScalar(value)) return value;
        return GSON.fromJson(GSON.toJsonTree(value, type), type);
    }

    private static boolean isImmutableScalar(Object value) {
        return value == null
                || value instanceof Number
                || value instanceof String
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum<?>;
    }

    private static Gson createGson() {
        GsonBuilder builder = new GsonBuilder();
        SLibGsonDefaults.apply(builder);
        return builder.create();
    }
}
