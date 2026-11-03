package com.stalemated.lib.config.internal;

import com.google.gson.Gson;
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

    private static final Set<Class<?>> IMMUTABLE_TYPES = Set.of(
            String.class, Boolean.class, Character.class,
            Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class,
            BigInteger.class, BigDecimal.class,
            UUID.class, Pattern.class, Color.class, TextColor.class, ResourceLocation.class
    );

    private final Gson gson;

    public DeepCopy(Gson gson) {
        this.gson = gson;
    }

    public <V> V of(V value, Type type) {
        if (isImmutable(value)) return value;
        return gson.fromJson(gson.toJsonTree(value, type), type);
    }

    private static boolean isImmutable(Object value) {
        return value == null || value instanceof Enum<?> || IMMUTABLE_TYPES.contains(value.getClass());
    }
}
