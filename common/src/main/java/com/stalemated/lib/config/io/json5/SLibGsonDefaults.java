package com.stalemated.lib.config.io.json5;

import com.google.gson.*;
import com.stalemated.lib.util.color.ColorUtils;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;

import java.awt.Color;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Default GSON configurations, type adapters, and custom deserializers for S-Lib configs.
 */
public final class SLibGsonDefaults {

    private SLibGsonDefaults() {}

    /**
     * Applies default GSON adapters and configs.
     *
     * @param builder The GsonBuilder to configure.
     */
    public static void apply(GsonBuilder builder) {
        registerPrimitives(builder);
        registerJavaDefaults(builder);
        registerMinecraftDefaults(builder);
    }

    // Registration

    private static void registerPrimitives(GsonBuilder builder) {
        builder.registerTypeAdapter(String.class, stringDeserializer());
        builder.registerTypeAdapter(int.class, (JsonDeserializer<Integer>) (json, type, ctx) -> (int) json.getAsDouble());
        builder.registerTypeAdapter(Integer.class, (JsonDeserializer<Integer>) (json, type, ctx) -> (int) json.getAsDouble());
        builder.registerTypeAdapter(long.class, (JsonDeserializer<Long>) (json, type, ctx) -> (long) json.getAsDouble());
        builder.registerTypeAdapter(Long.class, (JsonDeserializer<Long>) (json, type, ctx) -> (long) json.getAsDouble());
        builder.registerTypeAdapter(float.class, (JsonDeserializer<Float>) (json, type, ctx) -> (float) json.getAsDouble());
        builder.registerTypeAdapter(Float.class, (JsonDeserializer<Float>) (json, type, ctx) -> (float) json.getAsDouble());
        builder.registerTypeAdapter(double.class, (JsonDeserializer<Double>) (json, type, ctx) -> json.getAsDouble());
        builder.registerTypeAdapter(Double.class, (JsonDeserializer<Double>) (json, type, ctx) -> json.getAsDouble());
    }

    private static void registerJavaDefaults(GsonBuilder builder) {
        builder.registerTypeAdapter(Color.class, colorAdapter());
        builder.registerTypeAdapter(UUID.class, uuidAdapter());
        builder.registerTypeAdapter(Pattern.class, patternAdapter());
    }

    private static void registerMinecraftDefaults(GsonBuilder builder) {
        builder.registerTypeAdapter(Identifier.class, identifierAdapter());
        builder.registerTypeAdapter(TextColor.class, textColorAdapter());
    }

    // Adapter Definitions

    private static JsonDeserializer<String> stringDeserializer() {
        return (json, typeOfT, context) -> {
            if (json.isJsonObject() && json.getAsJsonObject().has("value")) {
                return json.getAsJsonObject().get("value").getAsString();
            }
            return json.getAsString();
        };
    }

    private static ColorAdapter colorAdapter() {
        return new ColorAdapter();
    }

    private static UuidAdapter uuidAdapter() {
        return new UuidAdapter();
    }

    private static PatternAdapter patternAdapter() {
        return new PatternAdapter();
    }

    private static IdentifierAdapter identifierAdapter() {
        return new IdentifierAdapter();
    }

    private static TextColorAdapter textColorAdapter() {
        return new TextColorAdapter();
    }

    // Adapter Implementations

    private static final class ColorAdapter implements JsonSerializer<Color>, JsonDeserializer<Color> {
        @Override
        public JsonElement serialize(Color src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(ColorUtils.toHexString(src));
        }

        @Override
        public Color deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive()) {
                JsonPrimitive prim = json.getAsJsonPrimitive();
                if (prim.isNumber()) {
                    return new Color(prim.getAsInt(), true);
                }
                return ColorUtils.parseToAWT(prim.getAsString());
            }
            return Color.WHITE;
        }
    }

    private static final class TextColorAdapter implements JsonSerializer<TextColor>, JsonDeserializer<TextColor> {
        @Override
        public JsonElement serialize(TextColor src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(ColorUtils.toHexString(src));
        }

        @Override
        public TextColor deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive()) {
                TextColor color = ColorUtils.resolveTextColor(json.getAsString());
                if (color != null) return color;
            }
            return TextColor.fromRgb(ColorUtils.DEFAULT_COLOR);
        }
    }

    private static final class IdentifierAdapter implements JsonSerializer<Identifier>, JsonDeserializer<Identifier> {
        @Override
        public JsonElement serialize(Identifier src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public Identifier deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new Identifier(json.getAsString());
        }
    }

    private static final class UuidAdapter implements JsonSerializer<UUID>, JsonDeserializer<UUID> {
        @Override
        public JsonElement serialize(UUID src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public UUID deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return UUID.fromString(json.getAsString());
        }
    }

    private static final class PatternAdapter implements JsonSerializer<Pattern>, JsonDeserializer<Pattern> {
        @Override
        public JsonElement serialize(Pattern src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.pattern());
        }

        @Override
        public Pattern deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Pattern.compile(json.getAsString());
        }
    }
}