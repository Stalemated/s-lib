package com.stalemated.lib.config.io.json5;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonNull;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.api.Marshaller;
import net.minecraft.util.Identifier;

import java.util.UUID;
import java.util.function.BiFunction;

public final class SLibJanksonDefaults {

    private SLibJanksonDefaults() {}

    public static void apply(Jankson.Builder builder) {
        BiFunction<Float, Marshaller, JsonElement> floatSerializer = (f, m) -> {
            if (f == null) return JsonNull.INSTANCE;
            if (Float.isNaN(f) || Float.isInfinite(f)) return new JsonPrimitive(f.toString());
            return new JsonPrimitive(Double.parseDouble(Float.toString(f)));
        };
        builder.registerSerializer(Float.class, floatSerializer);
        builder.registerSerializer(float.class, floatSerializer);

        BiFunction<Double, Marshaller, JsonElement> doubleSerializer = (d, m) -> {
            if (d == null) return JsonNull.INSTANCE;
            if (Double.isNaN(d) || Double.isInfinite(d)) return new JsonPrimitive(d.toString());
            return new JsonPrimitive(Double.parseDouble(Double.toString(d)));
        };
        builder.registerSerializer(Double.class, doubleSerializer);
        builder.registerSerializer(double.class, doubleSerializer);

        builder.registerSerializer(Identifier.class, (id, m) -> new JsonPrimitive(id.toString()));
        builder.registerSerializer(UUID.class, (uuid, m) -> new JsonPrimitive(uuid.toString()));
    }
}