package com.stalemated.lib.util.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class ReflectionUtils {

    private ReflectionUtils() {}

    /**
     * Instantiates a class using its zero-argument constructor.
     *
     * @param clazz The class to instantiate.
     * @param <T> The type of the class.
     * @return A new instance of the class.
     * @throws RuntimeException if the class cannot be instantiated.
     */
    public static <T> T tryInstantiate(Class<T> clazz) {
        try {
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate class using zero-argument constructor: " + clazz.getName(), e);
        }
    }

    /**
     * Safely retrieves the value of a field from an instance without throwing checked exceptions.
     *
     * @param field The field to retrieve.
     * @param instance The instance to retrieve the field from.
     * @return The field value, or null if retrieval fails or instance is null.
     */
    public static Object getFieldValueSafe(Field field, Object instance) {
        if (instance == null) return null;

        try {
            field.setAccessible(true);
            return field.get(instance);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Safely sets the value of a field on an instance without throwing checked exceptions.
     *
     * @param field The field to set.
     * @param instance The instance to modify.
     * @param value The value to set.
     */
    public static void setFieldValueSafe(Field field, Object instance, Object value) {
        if (instance == null) return;

        try {
            field.setAccessible(true);
            field.set(instance, value);
        } catch (Exception ignored) {}
    }

    /**
     * Checks if a field has the static or transient modifiers.
     *
     * @param field The field to check.
     * @return True if static or transient, false otherwise.
     */
    public static boolean isStaticOrTransient(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers);
    }

    /**
     * Finds a field in the class hierarchy.
     *
     * @param clazz The class to start searching from.
     * @param fieldName The name of the field to find.
     * @return The field if found, or null if not found.
     */
    public static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;

        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
