package com.stalemated.lib.config.model;

import com.stalemated.lib.config.annotation.Comment;
import com.stalemated.lib.config.annotation.Title;
import com.stalemated.lib.config.io.json5.DeepCopy;
import com.stalemated.lib.config.network.SyncMode;
import com.stalemated.lib.util.reflection.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;

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
    private final Constraint constraint;
    private final String comment;
    private final String title;
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

        this.constraint = RangeConstraints.of(key, field, type);
        this.comment = annotation(Comment.class).map(Comment::value).orElse(null);
        this.title = annotation(Title.class).map(Title::value).orElse(null);
    }

    public String getKey() { return key; }

    public Field getField() { return field; }

    public List<Field> getFieldPath() { return fieldPath; }
    
    @Override
    public Type genericType() { return genericType; }

    @Override
    public Class<?> type() { return type; }

    public SyncMode getSyncMode() { return syncMode; }

    public Constraint getConstraint() { return constraint; }

    public <A extends Annotation> Optional<A> annotation(Class<A> annotationType) {
        return field == null ? Optional.empty() : Optional.ofNullable(field.getAnnotation(annotationType));
    }

    public String getComment() { return comment; }

    public String getTitle() { return title; }

    public boolean hasTitle() {
        return title != null && !title.isBlank();
    }

    public Object defaultCopy() {
        return DeepCopy.of(defaultValue, genericType);
    }

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

    public boolean isSortedMap() {
        return (type != null && SortedMap.class.isAssignableFrom(type)) || defaultValue instanceof SortedMap;
    }

    public boolean isEnum() {
        return type != null && type.isEnum();
    }

    /**
     * Resolves and extracts the current value of this option from a root config instance.
     *
     * @param rootInstance The root config instance.
     * @return The resolved value, or a copy of the default when the root or an enclosing object is null.
     */
    public Object getValue(Object rootInstance) {
        if (isTemplate) throw new UnsupportedOperationException("Cannot read value directly from an OptionInfo template.");
        if (rootInstance == null) return defaultCopy();

        try {
            Object target = resolveTargetObject(rootInstance, false);
            if (target == null) return defaultCopy();

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
     * Applies this option's {@link Constraint} to a candidate value.
     * Ranged floats and doubles are also rounded to their significant digits to drop slider step noise.
     *
     * @param value The candidate value.
     * @return The clamped value if applicable, or the original value.
     */
    public Object clampValue(Object value) {
        return constraint.apply(value);
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
