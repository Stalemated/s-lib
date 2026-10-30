package com.stalemated.lib.util;

import java.util.Map;
import java.util.function.Function;

import static com.stalemated.lib.SLib.LOGGER;
import static java.util.Map.entry;

/**
 * Utility for coercing string representations into primitive values, boxed types, and enums.
 */
public final class TypeCoercionUtils {

    private static final Map<Class<?>, Function<String, Object>> PARSERS = Map.ofEntries(
            entry(boolean.class, TypeCoercionUtils::parseBoolean),
            entry(Boolean.class, TypeCoercionUtils::parseBoolean),
            entry(byte.class, Byte::valueOf),
            entry(Byte.class, Byte::valueOf),
            entry(short.class, Short::valueOf),
            entry(Short.class, Short::valueOf),
            entry(int.class, Integer::valueOf),
            entry(Integer.class, Integer::valueOf),
            entry(long.class, Long::valueOf),
            entry(Long.class, Long::valueOf),
            entry(float.class, Float::valueOf),
            entry(Float.class, Float::valueOf),
            entry(double.class, Double::valueOf),
            entry(Double.class, Double::valueOf),
            entry(char.class, TypeCoercionUtils::parseChar),
            entry(Character.class, TypeCoercionUtils::parseChar)
    );

    private TypeCoercionUtils() {}

    /**
     * Coerces a string value into an instance of the specified target type.
     * Supports primitives, boxed types, enums and strings.
     *
     * @param valStr The string representation to coerce.
     * @param targetType The target class type.
     * @return The coerced object, {@code null} (after logging a warning) if the value is not valid for the type,
     *         or the original string if the type is unhandled or targetType is null/Object.
     */
    public static Object coerce(String valStr, Class<?> targetType) {
        if (targetType == null || targetType == String.class || targetType == Object.class) {
            return valStr;
        }

        Function<String, Object> parser = targetType.isEnum()
                ? value -> parseEnum(targetType, value)
                : PARSERS.get(targetType);
        if (parser == null) return valStr;

        String trimmed = valStr != null ? valStr.trim() : "";
        try {
            return parser.apply(trimmed);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Ignoring '{}': not a valid {}", trimmed, targetType.getSimpleName());
            return null;
        }
    }

    private static Object parseBoolean(String value) {
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        throw new IllegalArgumentException(value);
    }

    private static Object parseChar(String value) {
        if (value.isEmpty()) throw new IllegalArgumentException("empty");
        return value.charAt(0);
    }

    private static Object parseEnum(Class<?> enumType, String value) {
        for (Object constant : enumType.getEnumConstants()) {
            if (((Enum<?>) constant).name().equalsIgnoreCase(value)) {
                return constant;
            }
        }
        throw new IllegalArgumentException(value);
    }

    /**
     * Resolves a sensible default initial value for the given type.
     *
     * @param targetType The class type.
     * @return A default value for primitives/enums, or null if unknown.
     */
    public static Object resolveDefaultInitial(Class<?> targetType) {
        if (targetType == null) return null;
        if (targetType == boolean.class || targetType == Boolean.class) return false;
        if (targetType == byte.class || targetType == Byte.class) return (byte) 0;
        if (targetType == short.class || targetType == Short.class) return (short) 0;
        if (targetType == int.class || targetType == Integer.class) return 0;
        if (targetType == long.class || targetType == Long.class) return 0L;
        if (targetType == float.class || targetType == Float.class) return 0.0f;
        if (targetType == double.class || targetType == Double.class) return 0.0;
        if (targetType == char.class || targetType == Character.class) return '\0';
        if (targetType == String.class) return "";
        if (targetType.isEnum()) {
            Object[] constants = targetType.getEnumConstants();
            return (constants != null && constants.length > 0) ? constants[0] : null;
        }
        return null;
    }
}
