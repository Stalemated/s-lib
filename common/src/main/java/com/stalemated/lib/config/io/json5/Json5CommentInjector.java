package com.stalemated.lib.config.io.json5;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.CategoryNode;
import com.stalemated.lib.config.model.ConfigNode;

import java.util.Map;

public class Json5CommentInjector {

    /**
     * Recursively traverses a Jankson JSON AST and injects comments based on the OptionTree schema.
     *
     * @param json   The serialized Jankson JsonElement.
     * @param schema The OptionTree schema node.
     */
    public static void inject(JsonElement json, ConfigNode schema) {
        if (json == null || schema == null) return;

        if (schema instanceof OptionInfo optionInfo) {
            if (optionInfo.getTemplate() != null) {
                injectInCollection(json, optionInfo.getTemplate());
            }
            return;
        }

        if (schema instanceof CategoryNode categoryNode && json instanceof JsonObject jsonObj) {
            injectInCategory(jsonObj, categoryNode);
        }
    }

    private static void injectInCollection(JsonElement json, ConfigNode template) {
        if (json instanceof JsonArray jsonArray) {
            for (JsonElement element : jsonArray) {
                inject(element, template);
            }
        } else if (json instanceof JsonObject jsonObj) {
            for (Map.Entry<String, JsonElement> entry : jsonObj.entrySet()) {
                inject(entry.getValue(), template);
            }
        }
    }

    private static void injectInCategory(JsonObject jsonObj, CategoryNode categoryNode) {
        for (Map.Entry<String, ConfigNode> entry : categoryNode.children().entrySet()) {
            String key = entry.getKey();
            ConfigNode childSchema = entry.getValue();

            JsonElement childJson = jsonObj.get(key);
            if (childJson == null) continue;

            if (childSchema instanceof OptionInfo optionInfo) {
                String comment = optionInfo.getComment();
                if (comment != null && !comment.isEmpty()) {
                    jsonObj.setComment(key, comment);
                }
            }
            // Recurse into child
            inject(childJson, childSchema);
        }
    }
}
