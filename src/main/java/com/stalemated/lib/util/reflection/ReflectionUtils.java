package com.stalemated.lib.util.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.*;

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
     * Checks if a class has a public no-argument constructor.
     *
     * @param clazz The class to check.
     * @return True if it has a public no-argument constructor, false otherwise.
     */
    public static boolean hasPublicNoArgsConstructor(Class<?> clazz) {
        try {
            var constructor = clazz.getConstructor();
            return Modifier.isPublic(constructor.getModifiers());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Checks if a class has any declared no-argument constructor regardless of visibility.
     *
     * @param clazz The class to check.
     * @return True if a no-argument constructor exists, false otherwise.
     */
    public static boolean hasNoArgsConstructor(Class<?> clazz) {
        if (clazz == null || clazz.isPrimitive() || clazz.isArray() || clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
            return false;
        }
        try {
            clazz.getDeclaredConstructor();
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Checks whether a class belongs to standard Java/JVM system packages that should
     * not be reflectively mutated or inspected across encapsulated boundaries.
     *
     * @param clazz The class to check.
     * @return True if system/JDK class, false otherwise.
     */
    public static boolean isSystemClass(Class<?> clazz) {
        if (clazz == null) return true;
        String name = clazz.getName();
        return name.startsWith("java.") || name.startsWith("javax.") ||
                name.startsWith("jdk.") || name.startsWith("sun.");
    }

    /**
     * Retrieves the raw class of a type.
     *
     * @param type The type to check.
     * @return The raw class of the type.
     */
    public static Class<?> getRawClass(Type type) {
        if (type instanceof Class<?> c) return c;
        if (type instanceof ParameterizedType pType) return (Class<?>) pType.getRawType();
        return Object.class;
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

    /**
     * Performs a deep equality comparison supporting primitives, strings,
     * arrays, collections, maps, sets, and POJOs without explicit {@code equals} implementations.
     *
     * @param a The first object.
     * @param b The second object.
     * @return True if both objects are deeply equal, false otherwise.
     */
    public static boolean deepEquals(Object a, Object b) {
        if (Objects.deepEquals(a, b)) return true;
        if (a == null || b == null) return false;

        if (a.getClass().isArray() && b.getClass().isArray()) {
            if (a instanceof Object[] arrA && b instanceof Object[] arrB) {
                if (arrA.length != arrB.length) return false;
                for (int i = 0; i < arrA.length; i++) {
                    if (!deepEquals(arrA[i], arrB[i])) return false;
                }
                return true;
            }
            return Objects.deepEquals(a, b);
        }

        if (a.getClass().isArray() || b.getClass().isArray()) {
            return false;
        }

        if (a instanceof List<?> || b instanceof List<?>) {
            if (!(a instanceof List<?> listA && b instanceof List<?> listB)) return false;
            if (listA.size() != listB.size()) return false;

            for (int i = 0; i < listA.size(); i++) {
                if (!deepEquals(listA.get(i), listB.get(i))) return false;
            }
            return true;
        }

        if (a instanceof Set<?> || b instanceof Set<?>) {
            if (!(a instanceof Set<?> setA && b instanceof Set<?> setB)) return false;
            if (setA.size() != setB.size()) return false;
            List<Object> remainingB = new ArrayList<>(setB);

            for (Object itemA : setA) {
                boolean found = false;
                Iterator<Object> itB = remainingB.iterator();
                while (itB.hasNext()) {
                    if (deepEquals(itemA, itB.next())) {
                        itB.remove();
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
            return remainingB.isEmpty();
        }

        if (a instanceof Map<?, ?> || b instanceof Map<?, ?>) {
            if (!(a instanceof Map<?, ?> mapA && b instanceof Map<?, ?> mapB)) return false;
            if (mapA.size() != mapB.size()) return false;

            for (Map.Entry<?, ?> entryA : mapA.entrySet()) {
                if (!mapB.containsKey(entryA.getKey())) return false;
                if (!deepEquals(entryA.getValue(), mapB.get(entryA.getKey()))) return false;
            }
            return true;
        }

        if (a instanceof Collection<?> || b instanceof Collection<?>) {
            if (!(a instanceof Collection<?> collA && b instanceof Collection<?> collB)) return false;
            if (collA.size() != collB.size()) return false;
            Iterator<?> itA = collA.iterator();
            Iterator<?> itB = collB.iterator();

            while (itA.hasNext() && itB.hasNext()) {
                if (!deepEquals(itA.next(), itB.next())) return false;
            }
            return !itA.hasNext() && !itB.hasNext();
        }

        if (a.getClass() == b.getClass() && !hasOverriddenEquals(a.getClass())) {
            return compareFields(a, b);
        }

        return false;
    }

    /**
     * Checks if a class has overridden the default {@link Object#equals(Object)} method.
     *
     * @param clazz The class to inspect.
     * @return True if the class has a custom equals method, false otherwise.
     */
    public static boolean hasOverriddenEquals(Class<?> clazz) {
        if (clazz == null || clazz.isPrimitive() || clazz.isArray()) return false;
        try {
            return clazz.getMethod("equals", Object.class).getDeclaringClass() != Object.class;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Recursively compares all non-static, non-transient fields of two objects of the same class.
     *
     * @param a The first object.
     * @param b The second object.
     * @return True if all non-static, non-transient fields match deeply, false otherwise.
     */
    public static boolean compareFields(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.getClass() != b.getClass() || a.getClass().isArray()) return false;

        Class<?> clazz = a.getClass();
        if (isSystemClass(clazz)) return false;

        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            if (isSystemClass(current)) break;

            for (Field f : current.getDeclaredFields()) {
                if (isStaticOrTransient(f)) continue;
                try {
                    f.setAccessible(true);
                    Object valA = f.get(a);
                    Object valB = f.get(b);
                    if (!deepEquals(valA, valB)) return false;
                } catch (Throwable t) {
                    return false;
                }
            }
            current = current.getSuperclass();
        }
        return true;
    }

    /**
     * Resolves the actual Class of a type argument at the specified index from a {@link ParameterizedType},
     * unwrapping wildcards if present.
     *
     * @param genericType The generic type to inspect.
     * @param argumentIndex The 0-based argument index.
     * @param fallback The fallback class if resolution is not possible.
     * @return The resolved Class or fallback.
     */
    public static Class<?> resolveGenericTypeArgument(Type genericType, int argumentIndex, Class<?> fallback) {
        if (genericType instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length > argumentIndex) {
                Type target = args[argumentIndex];
                if (target instanceof Class<?> clazz) {
                    return clazz;
                }
                if (target instanceof WildcardType wt && wt.getUpperBounds().length > 0 && wt.getUpperBounds()[0] instanceof Class<?> clazz) {
                    return clazz;
                }
            }
        }
        return fallback;
    }

    /**
     * Searches a map keyed by {@link Class} for a value whose key matches the target type.
     * Checks for exact match first (O(1)), then iterates through the map entries checking
     * {@link Class#isAssignableFrom(Class)}.
     *
     * @param map The map to search in.
     * @param targetType The class type to match.
     * @param <V> The value type.
     * @return The value matching the target type or an assignable supertype/interface, or null if not found.
     */
    public static <V> V findAssignable(Map<Class<?>, V> map, Class<?> targetType) {
        if (map == null || targetType == null) return null;
        V exact = map.get(targetType);
        if (exact != null) return exact;
        for (Map.Entry<Class<?>, V> entry : map.entrySet()) {
            if (entry.getKey().isAssignableFrom(targetType)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
