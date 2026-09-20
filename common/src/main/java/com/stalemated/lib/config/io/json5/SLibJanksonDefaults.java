package com.stalemated.lib.config.io.json5;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonNull;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.api.DeserializerFunction;
import blue.endless.jankson.api.Marshaller;
import com.stalemated.lib.util.color.ColorUtils;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;

import java.awt.Color;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

/**
 * Default Jankson configurations, serializers, and deserializers for S-Lib configs.
 */
public final class SLibJanksonDefaults {

    private SLibJanksonDefaults() {}

    /**
     * Applies default Jankson serializers, deserializers, and type configurations.
     *
     * @param builder The Jankson.Builder to configure.
     */
    public static void apply(Jankson.Builder builder) {
        registerPrimitives(builder);
        registerJavaDefaults(builder);
        registerMinecraftDefaults(builder);
    }

    // Registration

    private static void registerPrimitives(Jankson.Builder builder) {
        BiFunction<Float, Marshaller, JsonElement> floatSerializer = floatSerializer();
        builder.registerSerializer(Float.class, floatSerializer);
        builder.registerSerializer(float.class, floatSerializer);

        BiFunction<Double, Marshaller, JsonElement> doubleSerializer = doubleSerializer();
        builder.registerSerializer(Double.class, doubleSerializer);
        builder.registerSerializer(double.class, doubleSerializer);
    }

    private static void registerJavaDefaults(Jankson.Builder builder) {
        builder.registerSerializer(Color.class, colorSerializer());
        builder.registerDeserializer(JsonPrimitive.class, Color.class, colorDeserializer());

        builder.registerSerializer(UUID.class, uuidSerializer());
        builder.registerDeserializer(JsonPrimitive.class, UUID.class, uuidDeserializer());

        builder.registerSerializer(Pattern.class, patternSerializer());
        builder.registerDeserializer(JsonPrimitive.class, Pattern.class, patternDeserializer());
    }

    private static void registerMinecraftDefaults(Jankson.Builder builder) {
        builder.registerSerializer(Identifier.class, identifierSerializer());
        builder.registerDeserializer(JsonPrimitive.class, Identifier.class, identifierDeserializer());

        builder.registerSerializer(TextColor.class, textColorSerializer());
        builder.registerDeserializer(JsonPrimitive.class, TextColor.class, textColorDeserializer());

        builder.registerSerializer(Item.class, itemSerializer());
        builder.registerDeserializer(JsonPrimitive.class, Item.class, itemDeserializer());
    }

    // Serializer / Deserializer Definitions

    private static BiFunction<Float, Marshaller, JsonElement> floatSerializer() {
        return (f, m) -> {
            if (f == null) return JsonNull.INSTANCE;
            if (Float.isNaN(f) || Float.isInfinite(f)) return new JsonPrimitive(f.toString());
            return new JsonPrimitive(Double.parseDouble(Float.toString(f)));
        };
    }

    private static BiFunction<Double, Marshaller, JsonElement> doubleSerializer() {
        return (d, m) -> {
            if (d == null) return JsonNull.INSTANCE;
            if (Double.isNaN(d) || Double.isInfinite(d)) return new JsonPrimitive(d.toString());
            return new JsonPrimitive(Double.parseDouble(Double.toString(d)));
        };
    }

    private static BiFunction<Color, Marshaller, JsonElement> colorSerializer() {
        return (color, m) -> new JsonPrimitive(ColorUtils.toHexString(color));
    }

    private static DeserializerFunction<JsonPrimitive, Color> colorDeserializer() {
        return (prim, m) -> {
            if (prim.getValue() instanceof Number num) {
                return new Color(num.intValue(), true);
            }
            return ColorUtils.parseToAWT(prim.asString());
        };
    }

    private static BiFunction<UUID, Marshaller, JsonElement> uuidSerializer() {
        return (uuid, m) -> new JsonPrimitive(uuid.toString());
    }

    private static DeserializerFunction<JsonPrimitive, UUID> uuidDeserializer() {
        return (prim, m) -> UUID.fromString(prim.asString());
    }

    private static BiFunction<Pattern, Marshaller, JsonElement> patternSerializer() {
        return (pattern, m) -> new JsonPrimitive(pattern.pattern());
    }

    private static DeserializerFunction<JsonPrimitive, Pattern> patternDeserializer() {
        return (prim, m) -> Pattern.compile(prim.asString());
    }

    private static BiFunction<Identifier, Marshaller, JsonElement> identifierSerializer() {
        return (id, m) -> new JsonPrimitive(id.toString());
    }

    private static DeserializerFunction<JsonPrimitive, Identifier> identifierDeserializer() {
        return (prim, m) -> new Identifier(prim.asString());
    }

    private static BiFunction<TextColor, Marshaller, JsonElement> textColorSerializer() {
        return (color, m) -> new JsonPrimitive(ColorUtils.toHexString(color));
    }

    private static DeserializerFunction<JsonPrimitive, TextColor> textColorDeserializer() {
        return (prim, m) -> {
            TextColor color = ColorUtils.resolveTextColor(prim.asString());
            return color != null ? color : TextColor.fromRgb(ColorUtils.DEFAULT_COLOR);
        };
    }

    private static BiFunction<Item, Marshaller, JsonElement> itemSerializer() {
        return (item, m) -> new JsonPrimitive(Registries.ITEM.getId(item).toString());
    }

    private static DeserializerFunction<JsonPrimitive, Item> itemDeserializer() {
        return (prim, m) -> Registries.ITEM.get(new Identifier(prim.asString()));
    }
}