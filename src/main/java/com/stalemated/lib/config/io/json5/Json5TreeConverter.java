package com.stalemated.lib.config.io.json5;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonNull;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import com.stalemated.lib.config.model.CategoryNode;
import com.stalemated.lib.config.model.ConfigNode;
import com.stalemated.lib.config.model.OptionInfo;

final class Json5TreeConverter {

    private final Json5KeyLayout layout;

    Json5TreeConverter(Json5KeyLayout layout) {
        this.layout = layout;
    }

    JsonObject toJankson(com.google.gson.JsonObject options, CategoryNode root) {
        return category(options, root);
    }

    private JsonElement convert(com.google.gson.JsonElement value, ConfigNode schema) {
        if (value.isJsonNull()) return JsonNull.INSTANCE;
        if (value.isJsonPrimitive()) return primitive(value.getAsJsonPrimitive());
        if (value.isJsonArray()) return elements(value.getAsJsonArray(), elementSchema(schema));
        if (schema instanceof CategoryNode category) return category(value.getAsJsonObject(), category);
        return entries(value.getAsJsonObject(), elementSchema(schema));
    }

    private JsonObject category(com.google.gson.JsonObject fields, CategoryNode category) {
        JsonObject object = new JsonObject();
        for (ConfigNode child : category.children().values()) {
            String key = layout.jsonName(child.field());
            com.google.gson.JsonElement value = fields.get(key);
            if (value != null) {
                object.put(key, convert(value, child), child.comment());
            }
        }
        return object;
    }

    private JsonObject entries(com.google.gson.JsonObject entries, ConfigNode valueSchema) {
        JsonObject object = new JsonObject();
        entries.entrySet().forEach(entry -> object.put(entry.getKey(), convert(entry.getValue(), valueSchema)));
        return object;
    }

    private JsonArray elements(com.google.gson.JsonArray elements, ConfigNode elementSchema) {
        JsonArray array = new JsonArray();
        elements.forEach(element -> array.add(convert(element, elementSchema)));
        return array;
    }

    private static ConfigNode elementSchema(ConfigNode schema) {
        return schema instanceof OptionInfo container ? container.getElementTemplate() : null;
    }

    private static JsonPrimitive primitive(com.google.gson.JsonPrimitive primitive) {
        if (primitive.isBoolean()) return JsonPrimitive.of(primitive.getAsBoolean());
        if (primitive.isString()) return JsonPrimitive.of(primitive.getAsString());
        return number(primitive.getAsNumber().toString());
    }

    private static JsonPrimitive number(String literal) {
        boolean integral = literal.chars().allMatch(c -> c == '-' || Character.isDigit(c));
        return integral ? JsonPrimitive.of(Long.valueOf(literal)) : JsonPrimitive.of(Double.valueOf(literal));
    }
}
