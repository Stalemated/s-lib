package com.stalemated.lib.config.io.json5;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import com.google.gson.Gson;
import com.stalemated.lib.config.io.InvalidEntry;
import com.stalemated.lib.config.model.CategoryNode;
import com.stalemated.lib.config.model.ConfigNode;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.util.reflection.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

final class OptionValueReader {

    private final Gson gson;
    private final Function<Field, String> jsonName;
    private final Map<Class<?>, Map<String, Field>> fieldsByClass = new ConcurrentHashMap<>();

    OptionValueReader(Gson gson, Function<Field, String> jsonName) {
        this.gson = gson;
        this.jsonName = jsonName;
    }

    Object read(OptionInfo option, String path, JsonElement raw, LoadReport report) {
        Type type = option.getGenericType();
        if (type instanceof ParameterizedType parameterized) {
            Type[] typeArguments = parameterized.getActualTypeArguments();
            if (option.isCollection() && raw instanceof JsonArray elements) {
                return readCollection(option.getTemplate(), type, typeArguments[0], path, elements, report);
            }
            if (option.isMap() && raw instanceof JsonObject entries) {
                return readMap(option.getTemplate(), type, typeArguments[0], typeArguments[1], path, entries, report);
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
        try {
            return parse(raw, elementType);
        } catch (RuntimeException elementFailure) {
            if (template instanceof CategoryNode && raw instanceof JsonObject fields) {
                return salvageFields(elementType, path, fields, elementFailure, report);
            }
            throw elementFailure;
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
                if (ReflectionUtils.isStaticOrTransient(field) || field.isSynthetic()) continue;
                field.setAccessible(true);
                fieldsByJsonName.putIfAbsent(jsonName.apply(field), field);
            }
        }
        return fieldsByJsonName;
    }

    private Object parse(JsonElement raw, Type type) {
        return gson.fromJson(raw.toJson(false, false), type);
    }
}
