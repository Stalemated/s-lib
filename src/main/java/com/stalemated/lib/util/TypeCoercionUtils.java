package com.stalemated.lib.util;

/**
 * Utility for coercing string representations into primitive values, boxed types, and enums.
 */
public final class TypeCoercionUtils {

    private TypeCoercionUtils() {}

    /**
     * Coerces a string value into an instance of the specified target type.
     * Supports primitives, boxed types, enums, strings, and fallback values.
     *
     * @param valStr The string representation to coerce.
     * @param targetType The target class type.
     * @return The coerced object, or the original string if type is unhandled or targetType is null/Object.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object coerce(String valStr, Class<?> targetType) {
        if (targetType == null || targetType == String.class || targetType == Object.class) {
            return valStr;
        }

        String trimmed = valStr != null ? valStr.trim() : "";

        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(trimmed);
        }
        if (targetType == byte.class || targetType == Byte.class) {
            try {
                return Byte.parseByte(trimmed);
            } catch (NumberFormatException e) {
                return (byte) 0;
            }
        }
        if (targetType == short.class || targetType == Short.class) {
            try {
                return Short.parseShort(trimmed);
            } catch (NumberFormatException e) {
                return (short) 0;
            }
        }
        if (targetType == int.class || targetType == Integer.class) {
            try {
                return Integer.parseInt(trimmed);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        if (targetType == long.class || targetType == Long.class) {
            try {
                return Long.parseLong(trimmed);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        if (targetType == float.class || targetType == Float.class) {
            try {
                return Float.parseFloat(trimmed);
            } catch (NumberFormatException e) {
                return 0.0f;
            }
        }
        if (targetType == double.class || targetType == Double.class) {
            try {
                return Double.parseDouble(trimmed);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        if (targetType == char.class || targetType == Character.class) {
            return !trimmed.isEmpty() ? trimmed.charAt(0) : '\0';
        }
        if (targetType.isEnum()) {
            try {
                return Enum.valueOf((Class<Enum>) targetType, trimmed);
            } catch (Exception e) {
                // Try case-insensitive lookup
                Object[] constants = targetType.getEnumConstants();
                if (constants != null) {
                    for (Object constant : constants) {
                        if (((Enum<?>) constant).name().equalsIgnoreCase(trimmed)) {
                            return constant;
                        }
                    }
                    if (constants.length > 0) {
                        return constants[0];
                    }
                }
                return null;
            }
        }

        return valStr;
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
