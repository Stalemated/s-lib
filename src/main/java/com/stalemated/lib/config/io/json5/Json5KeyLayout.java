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
import java.util.List;
import java.util.Map;

final class Json5KeyLayout {

    private final FieldNamingStrategy namingStrategy;
    private final Map<OptionInfo, List<Key>> optionPaths = new HashMap<>();
    private final Key root = new Key(List.of());

    Json5KeyLayout(OptionTree optionTree, FieldNamingStrategy namingStrategy) {
        this.namingStrategy = namingStrategy;

        for (OptionInfo option : optionTree.all()) {
            List<Key> path = new ArrayList<>();
            Key parent = root;
            for (Field category : option.getFieldPath()) {
                parent = parent.child(namesOf(category));
                path.add(parent);
            }
            path.add(parent.child(namesOf(option.getField())));
            optionPaths.put(option, List.copyOf(path));
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

    List<String> namesOf(Field field) {
        List<String> names = new ArrayList<>();
        names.add(jsonName(field));
        SerializedName serializedName = field.getAnnotation(SerializedName.class);
        if (serializedName != null) {
            names.addAll(List.of(serializedName.alternate()));
        }
        return List.copyOf(names);
    }

    String pathOf(OptionInfo option) {
        return String.join(".", optionPaths.get(option).stream().map(Key::name).toList());
    }

    void put(com.google.gson.JsonObject tree, OptionInfo option, com.google.gson.JsonElement value) {
        List<Key> path = optionPaths.get(option);
        com.google.gson.JsonObject parent = tree;
        for (Key category : path.subList(0, path.size() - 1)) {
            if (!parent.has(category.name())) {
                parent.add(category.name(), new com.google.gson.JsonObject());
            }
            parent = parent.getAsJsonObject(category.name());
        }
        parent.add(path.get(path.size() - 1).name(), value);
    }

    JsonElement find(JsonObject json, OptionInfo option, LoadReport report) {
        JsonElement current = json;
        for (Key key : optionPaths.get(option)) {
            if (!(current instanceof JsonObject object)) return null;
            String present = key.firstNameIn(object);
            if (present == null) return null;
            if (!present.equals(key.name())) {
                report.requireMigration();
            }
            current = object.get(present);
        }
        return current;
    }

    void reportUnexpectedKeys(JsonObject json, LoadReport report) {
        reportUnexpectedKeys("", json, root, report);
    }

    private void reportUnexpectedKeys(String prefix, JsonObject object, Key parent, LoadReport report) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String path = childPath(prefix, entry.getKey());
            Key key = parent.children.get(entry.getKey());

            if (key == null || !entry.getKey().equals(key.firstNameIn(object))) {
                report.unknownKey(path, entry.getValue());
            } else if (key.isCategory()) {
                if (entry.getValue() instanceof JsonObject category) {
                    reportUnexpectedKeys(path, category, key, report);
                } else {
                    report.reject(path, entry.getValue(), "expected an object with the options of this category");
                }
            }
        }
    }

    private static final class Key {
        private final List<String> names;
        private final Map<String, Key> children = new HashMap<>();

        private Key(List<String> names) {
            this.names = names;
        }

        private String name() {
            return names.get(0);
        }

        private boolean isCategory() {
            return !children.isEmpty();
        }

        private Key child(List<String> childNames) {
            Key existing = children.get(childNames.get(0));
            if (existing != null) return existing;

            Key created = new Key(childNames);
            childNames.forEach(name -> children.put(name, created));
            return created;
        }

        private String firstNameIn(JsonObject object) {
            for (String name : names) {
                if (object.containsKey(name)) return name;
            }
            return null;
        }
    }
}
