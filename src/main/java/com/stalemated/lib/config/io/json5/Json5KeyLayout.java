package com.stalemated.lib.config.io.json5;

import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.annotations.SerializedName;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.OptionTree;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Json5KeyLayout {

    private final FieldNamingStrategy namingStrategy;
    private final Map<OptionInfo, List<String>> optionPaths = new HashMap<>();
    private final Set<String> optionKeys = new HashSet<>();
    private final Set<String> categoryKeys = new HashSet<>();

    Json5KeyLayout(OptionTree optionTree, FieldNamingStrategy namingStrategy) {
        this.namingStrategy = namingStrategy;

        for (OptionInfo option : optionTree.all()) {
            List<String> path = new ArrayList<>();
            for (Field category : option.getFieldPath()) {
                path.add(jsonName(category));
                categoryKeys.add(String.join(".", path));
            }
            path.add(jsonName(option.getField()));
            optionPaths.put(option, List.copyOf(path));
            optionKeys.add(String.join(".", path));
        }
    }

    static String childPath(String parentPath, String key) {
        return parentPath.isEmpty() ? key : parentPath + "." + key;
    }

    static String elementPath(String parentPath, int index) {
        return parentPath + "[" + index + "]";
    }

    String jsonName(Field field) {
        SerializedName serializedName = field.getAnnotation(SerializedName.class);
        return serializedName != null ? serializedName.value() : namingStrategy.translateName(field);
    }

    String pathOf(OptionInfo option) {
        return String.join(".", optionPaths.get(option));
    }

    void put(com.google.gson.JsonObject root, OptionInfo option, com.google.gson.JsonElement value) {
        List<String> path = optionPaths.get(option);
        com.google.gson.JsonObject parent = root;
        for (String category : path.subList(0, path.size() - 1)) {
            if (!parent.has(category)) {
                parent.add(category, new com.google.gson.JsonObject());
            }
            parent = parent.getAsJsonObject(category);
        }
        parent.add(path.get(path.size() - 1), value);
    }

    JsonElement find(JsonObject root, OptionInfo option) {
        JsonElement current = root;
        for (String key : optionPaths.get(option)) {
            if (!(current instanceof JsonObject jsonObject)) return null;
            current = jsonObject.get(key);
        }
        return current;
    }

    void reportUnexpectedKeys(JsonObject root, LoadReport report) {
        reportUnexpectedKeys("", root, report);
    }

    private void reportUnexpectedKeys(String prefix, JsonObject jsonObject, LoadReport report) {
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String path = childPath(prefix, entry.getKey());
            if (optionKeys.contains(path)) continue;

            if (!categoryKeys.contains(path)) {
                report.unknownKey(path, entry.getValue());
            } else if (entry.getValue() instanceof JsonObject category) {
                reportUnexpectedKeys(path, category, report);
            } else {
                report.reject(path, entry.getValue(), "expected an object with the options of this category");
            }
        }
    }
}
