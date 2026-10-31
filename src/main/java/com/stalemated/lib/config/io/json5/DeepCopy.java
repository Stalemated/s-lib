package com.stalemated.lib.config.io.json5;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;

import java.awt.Color;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class DeepCopy {

    private static final Gson GSON = createGson();

    private static final Set<Class<?>> IMMUTABLE_TYPES = Set.of(
            String.class, Boolean.class, Character.class,
            Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class,
            BigInteger.class, BigDecimal.class,
            UUID.class, Pattern.class, Color.class, TextColor.class, ResourceLocation.class
    );

    private DeepCopy() {}

    public static <V> V of(V value, Type type) {
        if (isImmutable(value)) return value;
        return GSON.fromJson(GSON.toJsonTree(value, type), type);
    }

    private static boolean isImmutable(Object value) {
        return value == null || value instanceof Enum<?> || IMMUTABLE_TYPES.contains(value.getClass());
    }

    private static Gson createGson() {
        GsonBuilder builder = new GsonBuilder();
        SLibGsonDefaults.apply(builder);
        return builder.create();
    }
}
