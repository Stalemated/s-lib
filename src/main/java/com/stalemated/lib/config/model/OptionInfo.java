package com.stalemated.lib.config.model;

import com.stalemated.lib.config.annotation.Comment;
import com.stalemated.lib.config.annotation.ItemDropdown;
import com.stalemated.lib.config.annotation.RangeDouble;
import com.stalemated.lib.config.annotation.RangeFloat;
import com.stalemated.lib.config.annotation.RangeInt;
import com.stalemated.lib.config.annotation.RangeLong;
import com.stalemated.lib.config.annotation.Title;
import com.stalemated.lib.config.network.SyncMode;
import com.stalemated.lib.util.reflection.ReflectionUtils;
import com.stalemated.lib.util.math.MathUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable metadata descriptor for a single config option.
 * <p>
 * Decoupled from any single instance of the config model, allowing the same
 * {@link OptionInfo} to read, write, validate, and clamp values across local configs,
 * server-synced clones, default templates, and UI buffers.
 */
public class OptionInfo implements ConfigNode {

    private final String key;
    private final Field field;
    private final List<Field> fieldPath;
    private final Class<?> type;
    private final Type genericType;
    private final SyncMode syncMode;
    private final RangeInt rangeInt;
    private final RangeLong rangeLong;
    private final RangeFloat rangeFloat;
    private final RangeDouble rangeDouble;
    private final String comment;
    private final String title;
    private final ItemDropdown itemDropdown;
    private final Object defaultValue;
    private final boolean isTemplate;
    private final ConfigNode template;

    public OptionInfo(String key, Field field, List<Field> fieldPath, SyncMode syncMode, Object defaultValue) {
        this(key, field, fieldPath, syncMode, defaultValue, field.getType(), field.getGenericType(), false, null);
    }

    public OptionInfo(String key, Field field, List<Field> fieldPath, SyncMode syncMode, Object defaultValue, Class<?> type, Type genericType, boolean isTemplate, ConfigNode template) {
        this.key = key;
        this.field = field;
        this.fieldPath = fieldPath != null ? Collections.unmodifiableList(fieldPath) : Collections.emptyList();
        this.syncMode = syncMode;
        this.defaultValue = defaultValue;
        this.type = type;
        this.genericType = genericType;
        this.isTemplate = isTemplate;
        this.template = template;

        if (this.field != null && !this.isTemplate) {
            this.field.setAccessible(true);
        }
        for (Field f : this.fieldPath) {
            f.setAccessible(true);
        }

        if (field != null) {
            this.rangeInt = field.getAnnotation(RangeInt.class);
            this.rangeLong = field.getAnnotation(RangeLong.class);
            this.rangeFloat = field.getAnnotation(RangeFloat.class);
            this.rangeDouble = field.getAnnotation(RangeDouble.class);
            Comment commentAnn = field.getAnnotation(Comment.class);
            this.comment = commentAnn != null ? commentAnn.value() : null;
            Title titleAnn = field.getAnnotation(Title.class);
            this.title = titleAnn != null ? titleAnn.value() : null;
            this.itemDropdown = field.getAnnotation(ItemDropdown.class);
        } else {
            this.rangeInt = null;
            this.rangeLong = null;
            this.rangeFloat = null;
            this.rangeDouble = null;
            this.comment = null;
            this.title = null;
            this.itemDropdown = null;
        }
        validateRanges();
    }

    private void validateRanges() {
        if (rangeInt != null && rangeInt.min() > rangeInt.max()) {
            throw invertedRange("@RangeInt", rangeInt.min(), rangeInt.max());
        }
        if (rangeLong != null && rangeLong.min() > rangeLong.max()) {
            throw invertedRange("@RangeLong", rangeLong.min(), rangeLong.max());
        }
        if (rangeFloat != null && rangeFloat.min() > rangeFloat.max()) {
            throw invertedRange("@RangeFloat", rangeFloat.min(), rangeFloat.max());
        }
        if (rangeDouble != null && rangeDouble.min() > rangeDouble.max()) {
            throw invertedRange("@RangeDouble", rangeDouble.min(), rangeDouble.max());
        }
    }

    private IllegalArgumentException invertedRange(String annotation, Object min, Object max) {
        return new IllegalArgumentException(annotation + " on '" + key + "': min (" + min + ") > max (" + max + ")");
    }

    public String getKey() { return key; }

    public Field getField() { return field; }

    public List<Field> getFieldPath() { return fieldPath; }
    
    @Override
    public Type genericType() { return genericType; }

    @Override
    public Class<?> type() { return type; }

    public Class<?> getType() { return type; }

    public Type getGenericType() { return genericType; }

    public SyncMode getSyncMode() { return syncMode; }

    public RangeInt getRangeInt() { return rangeInt; }

    public RangeLong getRangeLong() { return rangeLong; }

    public RangeFloat getRangeFloat() { return rangeFloat; }

    public RangeDouble getRangeDouble() { return rangeDouble; }

    public int getIntStep() {
        if (rangeInt != null) {
            return Math.max(1, rangeInt.step());
        }
        return 1;
    }

    public long getLongStep() {
        if (rangeLong != null) {
            return Math.max(1L, rangeLong.step());
        }
        return 1L;
    }

    public float getFloatStep() {
        if (rangeFloat != null) {
            float s = rangeFloat.step();
            return s > 0 ? s : 0.1f;
        }
        return 0.1f;
    }

    public double getDoubleStep() {
        if (rangeDouble != null) {
            double s = rangeDouble.step();
            return s > 0 ? s : 0.1;
        }
        return 0.1;
    }

    public String getComment() { return comment; }

    public String getTitle() { return title; }

    public boolean hasTitle() {
        return title != null && !title.isBlank();
    }

    public ItemDropdown getItemDropdown() { return itemDropdown; }

    public boolean hasItemDropdown() {
        return itemDropdown != null;
    }

    public boolean allowItemTags() {
        return itemDropdown != null && itemDropdown.allowTags();
    }

    public Object getDefaultValue() { return defaultValue; }
    
    public boolean isTemplate() { return isTemplate; }

    public ConfigNode getTemplate() { return template; }

    public ConfigNode getElementTemplate() { return template; }

    /**
     * Resolves the element type if this option represents a {@link Collection}.
     * Checks the element template first, then inspects the generic type argument.
     *
     * @return The element Class, or {@code Object.class} if not resolvable.
     */
    public Class<?> getCollectionElementType() {
        if (template instanceof OptionInfo tInfo && tInfo.type() != null) {
            return tInfo.type();
        }
        return ReflectionUtils.resolveGenericTypeArgument(genericType, 0, Object.class);
    }

    /**
     * Resolves the value type if this option represents a {@link Map}.
     * Checks the element template first, then the second generic type argument,
     * and falls back to inspecting default map values or {@code String.class}.
     *
     * @return The map value Class.
     */
    public Class<?> getMapValueType() {
        if (template instanceof OptionInfo tInfo && tInfo.type() != null) {
            return tInfo.type();
        }
        Class<?> resolved = ReflectionUtils.resolveGenericTypeArgument(genericType, 1, null);
        if (resolved != null) {
            return resolved;
        }
        if (defaultValue instanceof Map<?, ?> defMap && !defMap.isEmpty()) {
            for (Object val : defMap.values()) {
                if (val != null) return val.getClass();
            }
        }
        return String.class;
    }

    public boolean isCollection() {
        return type != null && Collection.class.isAssignableFrom(type);
    }

    public boolean isMap() {
        return type != null && Map.class.isAssignableFrom(type);
    }

    public boolean isEnum() {
        return type != null && type.isEnum();
    }

    public boolean hasRange() {
        return rangeInt != null || rangeLong != null || rangeFloat != null || rangeDouble != null;
    }

    public boolean isNumeric() {
        return type != null && (Number.class.isAssignableFrom(type) || (type.isPrimitive() && type != boolean.class && type != char.class));
    }

    /**
     * Resolves and extracts the current value of this option from a root config instance.
     *
     * @param rootInstance The root config instance.
     * @return The resolved value.
     */
    public Object getValue(Object rootInstance) {
        if (isTemplate) throw new UnsupportedOperationException("Cannot read value directly from an OptionInfo template.");
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
        if (isTemplate) throw new UnsupportedOperationException("Cannot set value directly on an OptionInfo template.");
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
        if (isTemplate || rootInstance == null) return;
        
        Object current = getValue(rootInstance);
        Object clamped = clampValue(current);
        
        if (!Objects.equals(current, clamped)) {
            setValue(rootInstance, clamped);
        }
    }

    /**
     * Clamps a numeric value according to any {@link RangeInt}, {@link RangeLong}, {@link RangeFloat} or {@link RangeDouble} annotations.
     * Ranged floats and doubles are also rounded to their significant digits to drop slider step noise.
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

        if (rangeLong != null && (type == long.class || type == Long.class) && value instanceof Number num) {
            long current = num.longValue();
            return MathUtils.clamp(current, rangeLong.min(), rangeLong.max());
        }

        if ((type == float.class || type == Float.class) && value instanceof Number num) {
            if (rangeFloat == null) return num.floatValue();
            return MathUtils.roundToSignificantDigits(MathUtils.clamp(num.floatValue(), rangeFloat.min(), rangeFloat.max()));
        }

        if ((type == double.class || type == Double.class) && value instanceof Number num) {
            if (rangeDouble == null) return num.doubleValue();
            return MathUtils.roundToSignificantDigits(MathUtils.clamp(num.doubleValue(), rangeDouble.min(), rangeDouble.max()));
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
        return "OptionInfo[" + key + " (" + type.getSimpleName() + "), sync=" + syncMode + ", template=" + isTemplate + "]";
    }
}
