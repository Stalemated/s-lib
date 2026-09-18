package com.stalemated.lib.config.model;

import com.stalemated.lib.config.annotation.Comment;
import com.stalemated.lib.config.annotation.RangeDouble;
import com.stalemated.lib.config.annotation.RangeFloat;
import com.stalemated.lib.config.annotation.RangeInt;
import com.stalemated.lib.config.network.SyncMode;
import com.stalemated.lib.util.reflection.ReflectionUtils;
import com.stalemated.lib.util.math.MathUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

/**
 * Immutable metadata descriptor for a single config option.
 * <p>
 * Decoupled from any single instance of the config model, allowing the same
 * {@link OptionInfo} to read, write, validate, and clamp values across local configs,
 * server-synced clones, default templates, and UI buffers.
 */
public class OptionInfo {

    private final String key;
    private final Field field;
    private final List<Field> fieldPath;
    private final Class<?> type;
    private final SyncMode syncMode;
    private final RangeInt rangeInt;
    private final RangeFloat rangeFloat;
    private final RangeDouble rangeDouble;
    private final String comment;
    private final Object defaultValue;

    public OptionInfo(String key, Field field, List<Field> fieldPath, SyncMode syncMode, Object defaultValue) {
        this.key = key;
        this.field = field;
        this.fieldPath = Collections.unmodifiableList(fieldPath);
        this.type = field.getType();
        this.syncMode = syncMode;
        this.defaultValue = defaultValue;

        this.field.setAccessible(true);
        for (Field f : this.fieldPath) {
            f.setAccessible(true);
        }

        this.rangeInt = field.getAnnotation(RangeInt.class);
        this.rangeFloat = field.getAnnotation(RangeFloat.class);
        this.rangeDouble = field.getAnnotation(RangeDouble.class);
        Comment commentAnn = field.getAnnotation(Comment.class);
        this.comment = commentAnn != null ? commentAnn.value() : null;
    }

    public String getKey() { return key; }

    public Field getField() { return field; }

    public List<Field> getFieldPath() { return fieldPath; }
    
    public Type getGenericType() { return field.getGenericType(); }

    public Class<?> getType() { return type; }

    public SyncMode getSyncMode() { return syncMode; }

    public RangeInt getRangeInt() { return rangeInt; }

    public RangeFloat getRangeFloat() { return rangeFloat; }

    public RangeDouble getRangeDouble() { return rangeDouble; }

    public String getComment() { return comment; }

    public Object getDefaultValue() { return defaultValue; }

    /**
     * Resolves and extracts the current value of this option from a root config instance.
     *
     * @param rootInstance The root config instance.
     * @return The resolved value.
     */
    public Object getValue(Object rootInstance) {
        if (rootInstance == null) return defaultValue;

        try {
            Object target = resolveTargetObject(rootInstance, false);
            if (target == null) return defaultValue;

            return field.get(target);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read config option: " + key, e);
        }
    }

    /**
     * Sets and clamps the value of this option on a root config instance.
     *
     * @param rootInstance The root config instance.
     * @param value The new value to set.
     */
    public void setValue(Object rootInstance, Object value) {
        if (rootInstance == null) return;

        try {
            Object target = resolveTargetObject(rootInstance, true);
            Object clamped = clampValue(value);
            field.set(target, clamped);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set config option: " + key, e);
        }
    }

    /**
     * Reads the current value from the root instance, applies clamping rules,
     * and sets it back if the clamped value differs from the original.
     * 
     * @param rootInstance The root config instance.
     */
    public void enforceLimits(Object rootInstance) {
        if (rootInstance == null) return;
        
        Object current = getValue(rootInstance);
        Object clamped = clampValue(current);
        
        if (current != null && !current.equals(clamped)) {
            setValue(rootInstance, clamped);
        }
    }

    /**
     * Clamps a numeric value according to any {@link RangeInt} or {@link RangeFloat} annotations.
     *
     * @param value The candidate value.
     * @return The clamped value if applicable, or the original value.
     */
    public Object clampValue(Object value) {
        if (value == null) return null;

        if (rangeInt != null && (type == int.class || type == Integer.class) && value instanceof Number num) {
            int current = num.intValue();
            return MathUtils.clamp(current, rangeInt.min(), rangeInt.max());
        }

        if ((type == float.class || type == Float.class) && value instanceof Number num) {
            float current = num.floatValue();
            if (rangeFloat != null) {
                current = MathUtils.clamp(current, rangeFloat.min(), rangeFloat.max());
            }
            return MathUtils.roundFloat(current);
        }

        if ((type == double.class || type == Double.class) && value instanceof Number num) {
            double current = num.doubleValue();
            if (rangeDouble != null) {
                current = MathUtils.clamp(current, rangeDouble.min(), rangeDouble.max());
            }
            return MathUtils.roundDouble(current);
        }

        return value;
    }

    private Object resolveTargetObject(Object rootInstance, boolean createIfNull) throws Exception {
        Object current = rootInstance;

        for (Field pathField : fieldPath) {
            Object child = pathField.get(current);

            if (child == null) {
                if (!createIfNull) return null;

                child = ReflectionUtils.tryInstantiate(pathField.getType());
                pathField.set(current, child);
            }
            current = child;
        }
        return current;
    }

    @Override
    public String toString() {
        return "OptionInfo[" + key + " (" + type.getSimpleName() + "), sync=" + syncMode + "]";
    }
}
