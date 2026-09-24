package com.stalemated.lib.config.validation;

import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.CategoryNode;
import com.stalemated.lib.config.model.ConfigNode;
import com.stalemated.lib.util.reflection.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public class ConfigValidator {

    /**
     * Validates a configuration instance against its OptionTree schema, enforcing all mathematical limits.
     *
     * @param instance The configuration instance to validate.
     * @param schema The OptionTree schema to validate against.
     */
    public static boolean validate(Object instance, ConfigNode schema) {
        if (instance == null || schema == null) return false;

        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        return validateNode(instance, schema, visited);
    }

    private static boolean validateNode(Object obj, ConfigNode schema, Set<Object> visited) {
        if (obj == null || schema == null || !visited.add(obj)) return false;

        if (schema instanceof OptionInfo optionInfo) {
            if (optionInfo.getTemplate() != null) {
                return validateCollectionNode(obj, optionInfo.getTemplate(), visited);
            }
            return false;
        }

        if (schema instanceof CategoryNode categoryNode) {
            return validateCategoryNode(obj, categoryNode, visited);
        }
        return false;
    }

    private static boolean validateCollectionNode(Object obj, ConfigNode template, Set<Object> visited) {
        boolean mutated = false;
        if (obj instanceof Iterable<?> iterable) {
            for (Object element : iterable) {
                mutated |= validateNode(element, template, visited);
            }
        } else if (obj instanceof Map<?, ?> map) {
            for (Object element : map.values()) {
                mutated |= validateNode(element, template, visited);
            }
        }
        return mutated;
    }

    private static boolean validateCategoryNode(Object obj, CategoryNode categoryNode, Set<Object> visited) {
        Class<?> currentClass = obj.getClass();
        boolean mutated = false;

        for (Map.Entry<String, ConfigNode> entry : categoryNode.children().entrySet()) {
            String fieldName = entry.getKey();
            ConfigNode childSchema = entry.getValue();

            try {
                Field field = ReflectionUtils.findField(currentClass, fieldName);
                if (field == null) continue;

                Object fieldValue = ReflectionUtils.getFieldValueSafe(field, obj);

                if (childSchema instanceof OptionInfo optionInfo && optionInfo.getTemplate() == null) {
                    Object clamped = optionInfo.clampValue(fieldValue);
                    if (clamped != null && !clamped.equals(fieldValue)) {
                        ReflectionUtils.setFieldValueSafe(field, obj, clamped);
                        mutated = true;
                    }
                } else {
                    mutated |= validateNode(fieldValue, childSchema, visited);
                }
            } catch (Exception ignored) { }
        }
        return mutated;
    }
}
