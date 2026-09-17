package com.stalemated.lib.config.io.json5;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.api.Marshaller;
import net.minecraft.util.Identifier;

import java.util.UUID;
import java.util.function.BiFunction;

public final class SLibJanksonDefaults {

    private SLibJanksonDefaults() {}

    public static void apply(Jankson.Builder builder) {
        BiFunction<Float, Marshaller, JsonElement> floatSerializer =
                (f, m) -> new JsonPrimitive(Double.parseDouble(String.valueOf(f)));
        builder.registerSerializer(Float.class, floatSerializer);
        builder.registerSerializer(float.class, floatSerializer);

        builder.registerSerializer(Identifier.class, (id, m) -> new JsonPrimitive(id.toString()));
        builder.registerSerializer(UUID.class, (uuid, m) -> new JsonPrimitive(uuid.toString()));
    }
}