package com.stalemated.lib.config.io.json5;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.CategoryNode;
import com.stalemated.lib.config.model.ConfigNode;
import com.stalemated.lib.util.reflection.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Function;

public class Json5CommentInjector {

    /**
     * Recursively traverses a Jankson JSON AST and injects comments based on the OptionTree schema.
     *
     * @param json     The serialized Jankson JsonElement.
     * @param schema   The OptionTree schema node.
     * @param jsonName Resolves the JSON key GSON writes for a field.
     */
    public static void inject(JsonElement json, ConfigNode schema, Function<Field, String> jsonName) {
        if (json == null || schema == null) return;

        if (schema instanceof OptionInfo optionInfo) {
            if (optionInfo.getElementTemplate() != null) {
                injectInCollection(json, optionInfo.getElementTemplate(), jsonName);
            }
            return;
        }

        if (schema instanceof CategoryNode categoryNode && json instanceof JsonObject jsonObj) {
            injectInCategory(jsonObj, categoryNode, jsonName);
        }
    }

    private static void injectInCollection(JsonElement json, ConfigNode template, Function<Field, String> jsonName) {
        if (json instanceof JsonArray jsonArray) {
            for (JsonElement element : jsonArray) {
                inject(element, template, jsonName);
            }
        } else if (json instanceof JsonObject jsonObj) {
            for (Map.Entry<String, JsonElement> entry : jsonObj.entrySet()) {
                inject(entry.getValue(), template, jsonName);
            }
        }
    }

    private static void injectInCategory(JsonObject jsonObj, CategoryNode categoryNode, Function<Field, String> jsonName) {
        for (Map.Entry<String, ConfigNode> entry : categoryNode.children().entrySet()) {
            ConfigNode childSchema = entry.getValue();
            Field field = childSchema instanceof OptionInfo optionInfo
                    ? optionInfo.getField()
                    : ReflectionUtils.findField(categoryNode.type(), entry.getKey());
            String key = jsonName.apply(field);

            JsonElement childJson = jsonObj.get(key);
            if (childJson == null) continue;

            if (childSchema instanceof OptionInfo optionInfo) {
                String comment = optionInfo.getComment();
                if (comment != null && !comment.isEmpty()) {
                    jsonObj.setComment(key, comment);
                }
            }
            // Recurse into child
            inject(childJson, childSchema, jsonName);
        }
    }
}
