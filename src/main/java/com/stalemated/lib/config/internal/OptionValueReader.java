package com.stalemated.lib.config.internal;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import com.google.gson.Gson;
import com.stalemated.lib.config.io.InvalidEntry;
import com.stalemated.lib.config.OptionInfo;
import com.stalemated.lib.config.OptionTree;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class OptionValueReader {

    private final Gson gson;
    private final Json5KeyLayout layout;
    private final Map<Class<?>, Map<String, Field>> fieldsByClass = new ConcurrentHashMap<>();

    OptionValueReader(Gson gson, Json5KeyLayout layout) {
        this.gson = gson;
        this.layout = layout;
    }

    Object read(OptionInfo option, String path, JsonElement raw, LoadReport report) {
        Type type = option.genericType();
        if (type instanceof ParameterizedType parameterized) {
            Type[] typeArguments = parameterized.getActualTypeArguments();
            if (option.isCollection() && raw instanceof JsonArray elements) {
                return readCollection(option.getElementTemplate(), type, typeArguments[0], path, elements, report);
            }
            if (option.isMap() && raw instanceof JsonObject entries) {
                return readMap(option.getElementTemplate(), type, typeArguments[0], typeArguments[1], path, entries, report);
            }
        }
        return parse(raw, type);
    }

    private Collection<Object> readCollection(ConfigNode template, Type collectionType, Type elementType, String path, JsonArray elements, LoadReport report) {
        Collection<Object> values = gson.fromJson("[]", collectionType);
        for (int i = 0; i < elements.size(); i++) {
            String elementPath = Json5KeyLayout.elementPath(path, i);
            try {
                values.add(readElement(template, elementType, elementPath, elements.get(i), report));
            } catch (RuntimeException e) {
                report.reject(elementPath, elements.get(i), e);
            }
        }
        return values;
    }

    private Map<Object, Object> readMap(ConfigNode template, Type mapType, Type keyType, Type valueType, String path, JsonObject entries, LoadReport report) {
        Map<Object, Object> values = gson.fromJson("{}", mapType);
        for (Map.Entry<String, JsonElement> entry : entries.entrySet()) {
            String entryPath = Json5KeyLayout.childPath(path, entry.getKey());
            try {
                Object key = gson.fromJson(gson.toJson(entry.getKey()), keyType);
                values.put(key, readElement(template, valueType, entryPath, entry.getValue(), report));
            } catch (RuntimeException e) {
                report.reject(entryPath, entry.getValue(), e);
            }
        }
        return values;
    }

    private Object readElement(ConfigNode template, Type elementType, String path, JsonElement raw, LoadReport report) {
        Object element = parseOrSalvage(template, elementType, path, raw, report);
        reportUnknownKeys(template, path, raw, report);
        return element;
    }

    private Object parseOrSalvage(ConfigNode template, Type elementType, String path, JsonElement raw, LoadReport report) {
        try {
            return parse(raw, elementType);
        } catch (RuntimeException elementFailure) {
            if (template instanceof CategoryNode && raw instanceof JsonObject fields) {
                return salvageFields(elementType, path, fields, elementFailure, report);
            }
            throw elementFailure;
        }
    }

    private void reportUnknownKeys(ConfigNode template, String path, JsonElement raw, LoadReport report) {
        if (template instanceof CategoryNode category && raw instanceof JsonObject fields) {
            Map<String, Field> fieldsByJsonName = fieldsByClass.computeIfAbsent(category.type(), this::indexSerializedFields);
            for (Map.Entry<String, JsonElement> entry : fields.entrySet()) {
                String fieldPath = Json5KeyLayout.childPath(path, entry.getKey());
                Field field = fieldsByJsonName.get(entry.getKey());
                if (field == null) {
                    report.unknownKey(fieldPath, entry.getValue());
                    continue;
                }
                if (!entry.getKey().equals(layout.jsonName(field))) {
                    report.requireMigration();
                }
                reportUnknownKeys(category.getChild(field.getName()), fieldPath, entry.getValue(), report);
            }
        } else if (template instanceof OptionInfo container && container.getElementTemplate() != null) {
            if (raw instanceof JsonArray elements) {
                for (int i = 0; i < elements.size(); i++) {
                    reportUnknownKeys(container.getElementTemplate(), Json5KeyLayout.elementPath(path, i), elements.get(i), report);
                }
            } else if (raw instanceof JsonObject entries) {
                for (Map.Entry<String, JsonElement> entry : entries.entrySet()) {
                    reportUnknownKeys(container.getElementTemplate(), Json5KeyLayout.childPath(path, entry.getKey()), entry.getValue(), report);
                }
            }
        }
    }

    private Object salvageFields(Type elementType, String path, JsonObject fields, RuntimeException elementFailure, LoadReport report) {
        Object element = gson.fromJson("{}", elementType);
        Map<String, Field> fieldsByJsonName = fieldsByClass.computeIfAbsent(element.getClass(), this::indexSerializedFields);

        List<InvalidEntry> fieldRejections = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : fields.entrySet()) {
            Field field = fieldsByJsonName.get(entry.getKey());
            if (field == null) continue;

            try {
                field.set(element, parse(entry.getValue(), field.getGenericType()));
            } catch (RuntimeException | IllegalAccessException e) {
                fieldRejections.add(LoadReport.rejection(Json5KeyLayout.childPath(path, entry.getKey()), entry.getValue(), e));
            }
        }

        if (fieldRejections.isEmpty()) throw elementFailure;
        report.rejectAll(fieldRejections);
        return element;
    }

    private Map<String, Field> indexSerializedFields(Class<?> elementClass) {
        Map<String, Field> fieldsByJsonName = new HashMap<>();
        for (Class<?> current = elementClass; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (OptionTree.isIgnored(field) || field.isSynthetic()) continue;
                field.setAccessible(true);
                layout.namesOf(field).forEach(name -> fieldsByJsonName.putIfAbsent(name, field));
            }
        }
        return fieldsByJsonName;
    }

    private Object parse(JsonElement raw, Type type) {
        return gson.fromJson(raw.toJson(false, false), type);
    }
}
