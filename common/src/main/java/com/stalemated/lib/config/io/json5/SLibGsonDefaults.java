package com.stalemated.lib.config.io.json5;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import net.minecraft.util.Identifier;

public final class SLibGsonDefaults {
    
    private SLibGsonDefaults() {}

    public static void apply(GsonBuilder builder) {
        builder.registerTypeAdapter(String.class, (JsonDeserializer<String>) (json, typeOfT, context) -> {
            if (json.isJsonObject() && json.getAsJsonObject().has("value")) {
                return json.getAsJsonObject().get("value").getAsString();
            }
            return json.getAsString();
        });
        builder.registerTypeAdapter(int.class, (JsonDeserializer<Integer>) (json, type, ctx) -> (int) json.getAsDouble());
        builder.registerTypeAdapter(Integer.class, (JsonDeserializer<Integer>) (json, type, ctx) -> (int) json.getAsDouble());
        builder.registerTypeAdapter(long.class, (JsonDeserializer<Long>) (json, type, ctx) -> (long) json.getAsDouble());
        builder.registerTypeAdapter(Long.class, (JsonDeserializer<Long>) (json, type, ctx) -> (long) json.getAsDouble());
        builder.registerTypeAdapter(float.class, (JsonDeserializer<Float>) (json, type, ctx) -> (float) json.getAsDouble());
        builder.registerTypeAdapter(Float.class, (JsonDeserializer<Float>) (json, type, ctx) -> (float) json.getAsDouble());
        builder.registerTypeAdapter(double.class, (JsonDeserializer<Double>) (json, type, ctx) -> json.getAsDouble());
        builder.registerTypeAdapter(Double.class, (JsonDeserializer<Double>) (json, type, ctx) -> json.getAsDouble());
        builder.registerTypeAdapter(Identifier.class, (JsonDeserializer<Identifier>) (json, type, ctx) -> new Identifier(json.getAsString()));
    }
}