package com.stalemated.lib.config;

import com.stalemated.lib.config.annotation.Comment;
import com.stalemated.lib.config.annotation.Title;
import com.stalemated.lib.config.internal.ConfigNode;
import com.stalemated.lib.config.internal.Constraint;
import com.stalemated.lib.config.internal.Constraints;
import com.stalemated.lib.config.internal.DeepCopy;
import com.stalemated.lib.config.internal.InvalidConfigValueException;
import com.stalemated.lib.config.network.SyncMode;
import com.stalemated.lib.util.internal.ReflectionUtils;
import org.jetbrains.annotations.ApiStatus;

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
 * One option of a config: its key, field, type, sync mode, limits, comment, title and default
 * value. Get them from {@link ConfigManager#getOptionTree()}.
 * <p>
 * It is not tied to an instance: {@link #getValue(Object)} and {@link #setValue(Object, Object)}
 * work on any instance of the root class. Templates describe the elements of a collection, with
 * keys ending in {@code .*}, or are made with {@link #childTemplate}. They cannot be read or
 * written.
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
    private final DeepCopy deepCopy;

    OptionInfo(String key, Field field, List<Field> fieldPath, SyncMode syncMode, Object defaultValue, Class<?> type, Type genericType, boolean isTemplate, ConfigNode template, DeepCopy deepCopy) {
        this.key = key;
        this.field = field;
        this.fieldPath = fieldPath != null ? Collections.unmodifiableList(fieldPath) : Collections.emptyList();
        this.syncMode = syncMode;
        this.defaultValue = defaultValue;
        this.type = type;
        this.genericType = genericType;
        this.isTemplate = isTemplate;
        this.template = template;
        this.deepCopy = deepCopy;

        if (this.field != null && !this.isTemplate) {
            this.field.setAccessible(true);
        }
        for (Field f : this.fieldPath) {
            f.setAccessible(true);
        }

        this.constraint = Constraints.of(key, field, type, genericType, template, deepCopy);
        this.comment = annotation(Comment.class).map(Comment::value).orElse(null);
        this.title = annotation(Title.class).map(Title::value).orElse(null);
    }

    /**
     * Returns the key of this option, with dots for {@code @Nest} fields, for example
     * {@code visuals.scale}. Element templates of collections end in {@code .*}.
     *
     * @return the key, never {@code null}
     */
    public String getKey() { return key; }

    /**
     * Returns the Java field of this option.
     *
     * @return the field; for a template it can be the field of the collection, or {@code null}
     *         if {@link #childTemplate} got no field
     */
    @Override
    public Field field() { return field; }

    /**
     * Returns the {@code @Nest} fields from the root class down to the object that declares this
     * option.
     *
     * @return an unmodifiable list, empty for options of the root class and for templates
     */
    public List<Field> getFieldPath() { return fieldPath; }

    /**
     * Returns the generic {@link Type} of this option, for example {@code List<String>}.
     *
     * @return the generic field type
     */
    @Override
    public Type genericType() { return genericType; }

    /**
     * Returns the raw {@link Class} of this option.
     *
     * @return the raw field class
     */
    @Override
    public Class<?> type() { return type; }

    /**
     * Returns the sync mode of this option. At each level the order is: {@code @Sync} on the
     * field, {@code @Sync} on the class of a {@code @Nest} field, then the mode of the parent. The
     * root uses the {@code @Sync} of the config class, or {@link SyncMode#OVERRIDE_CLIENT}.
     *
     * @return the sync mode, never {@code null}
     */
    public SyncMode getSyncMode() { return syncMode; }

    /**
     * Used by the S-Lib YACL layer to read ranges and steps.
     */
    @ApiStatus.Internal
    @Override
    public Constraint constraint() { return constraint; }

    /**
     * Returns an annotation of the field of this option. A custom {@code ControllerFactory} can
     * use it to read its own annotations.
     *
     * @param annotationType the annotation class
     * @param <A> the annotation type
     * @return the annotation, or empty if the field does not have it or there is no field
     */
    public <A extends Annotation> Optional<A> annotation(Class<A> annotationType) {
        return field == null ? Optional.empty() : Optional.ofNullable(field.getAnnotation(annotationType));
    }

    /**
     * Returns the {@code @Comment} text declared on this option, or {@code null} if none.
     *
     * @return the comment, or {@code null}
     */
    @Override
    public String comment() { return comment; }

    /**
     * Returns the {@code @Title} key declared on this option, or {@code null} if none.
     *
     * @return the title key, or {@code null}
     */
    public String getTitle() { return title; }

    /**
     * Returns whether this option declares a non-blank {@code @Title}.
     *
     * @return {@code true} if a non-blank title is present
     */
    public boolean hasTitle() {
        return title != null && !title.isBlank();
    }

    /**
     * Returns a deep copy of the default value of this option, taken from the default instance
     * when the schema was built. Known immutable types, like {@code String} and enums, are
     * returned as they are.
     *
     * @return the copy, or {@code null} if the default is {@code null}
     */
    public Object defaultCopy() {
        return deepCopy.of(defaultValue, genericType);
    }

    /**
     * Used by the S-Lib YACL layer to build the entries of collections and maps.
     */
    @ApiStatus.Internal
    public ConfigNode getElementTemplate() { return template; }

    /**
     * Creates a template for one element of a collection or for a field of such an element. It
     * keeps the sync mode of this option and reads its limits from the annotations of
     * {@code childField}. {@link #getValue(Object)} and {@link #setValue(Object, Object)} throw on
     * it. Used by the S-Lib YACL layer.
     *
     * @param childKey the key of the template
     * @param childField the field whose annotations are read, or {@code null} for none
     * @param childDefault the default value
     * @param childType the raw type of the value
     * @param childGenericType the generic type of the value
     * @return the new template
     * @throws IllegalArgumentException if the range or {@code @Format} annotations of
     *                                  {@code childField} are invalid
     */
    public OptionInfo childTemplate(String childKey, Field childField, Object childDefault, Class<?> childType, Type childGenericType) {
        return new OptionInfo(childKey, childField, Collections.emptyList(), syncMode, childDefault, childType, childGenericType, true, null, deepCopy);
    }

    /**
     * Returns the element class if this option is a {@link Collection}: the type of the element
     * template first, else the first generic type argument.
     *
     * @return the element class, or {@code Object.class} if it cannot be found
     */
    public Class<?> getCollectionElementType() {
        if (template instanceof OptionInfo tInfo && tInfo.type() != null) {
            return tInfo.type();
        }
        return ReflectionUtils.resolveGenericTypeArgument(genericType, 0, Object.class);
    }

    /**
     * Returns the value class if this option is a {@link Map}: the type of the element template
     * first, then the second generic type argument, then the class of the first non-null value
     * of the default map.
     *
     * @return the value class, or {@code String.class} if none of those works
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

    /**
     * Returns whether the raw type of this option is a {@link Collection}.
     *
     * @return {@code true} for collection options
     */
    public boolean isCollection() {
        return type != null && Collection.class.isAssignableFrom(type);
    }

    /**
     * Returns whether the raw type of this option is a {@link Map}.
     *
     * @return {@code true} for map options
     */
    public boolean isMap() {
        return type != null && Map.class.isAssignableFrom(type);
    }

    /**
     * Returns whether the raw type or the default value of this option is a {@link SortedMap}.
     * Config screens then save the map as a {@code TreeMap}.
     *
     * @return {@code true} for sorted map options
     */
    public boolean isSortedMap() {
        return (type != null && SortedMap.class.isAssignableFrom(type)) || defaultValue instanceof SortedMap;
    }

    /**
     * Returns whether the raw type of this option is an {@code enum}.
     *
     * @return {@code true} for enum options
     */
    public boolean isEnum() {
        return type != null && type.isEnum();
    }

    /**
     * Reads the value of this option from {@code rootInstance}, following the {@code @Nest}
     * fields. It does not create missing objects.
     * <p>
     * The value is the live object: changing a returned list changes the config.
     *
     * @param rootInstance an instance of the root class, or {@code null}
     * @return the value, or a copy of the default if {@code rootInstance} or an enclosing
     *         {@code @Nest} object is {@code null}
     * @throws UnsupportedOperationException if this option is a template
     * @throws RuntimeException if the field cannot be read, for example when {@code rootInstance}
     *                          is not of the root class
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
     * Clamps {@code value} and writes it to {@code rootInstance}. Missing {@code @Nest} objects on
     * the way are created with their no-argument constructor.
     * <p>
     * It does not save or run listeners; use the manager for that.
     *
     * @param rootInstance an instance of the root class; if {@code null}, nothing happens
     * @param value the new value, of the type of the field
     * @throws UnsupportedOperationException if this option is a template
     * @throws IllegalArgumentException if {@code value} can never be valid here, like {@code NaN},
     *                                  or its collection type cannot be created
     * @throws RuntimeException if the field cannot be written, for example when {@code value} has
     *                          the wrong type
     */
    public void setValue(Object rootInstance, Object value) {
        if (isTemplate) throw new UnsupportedOperationException("Cannot set value directly on an OptionInfo template.");
        if (rootInstance == null) return;

        Object clamped = clampValue(value);
        try {
            Object target = resolveTargetObject(rootInstance, true);
            field.set(target, clamped);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set config option: " + key, e);
        }
    }

    /**
     * Reads the value from {@code rootInstance}, clamps it and writes it back if clamping changed
     * it. {@code save()} does this for every option.
     *
     * @param rootInstance an instance of the root class; if {@code null} or if this option is a
     *                     template, nothing happens
     * @throws IllegalArgumentException if the stored value can never be valid, like {@code NaN}
     *                                  in a float without a range
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
     * Applies the limits of this option to a value. Ranged floats are also rounded to 7
     * significant digits and ranged doubles to 15, to drop slider step noise. Collections and
     * maps are converted to the type of the field.
     * <p>
     * With a range, an infinite number is clamped to the bound. Without one, floats and doubles
     * must be finite. The type of the value is not checked here; a wrong type usually fails later,
     * in {@link #setValue(Object, Object)}.
     *
     * @param value the value to check, or {@code null}
     * @return the clamped value, or {@code value} itself if nothing changed
     * @throws IllegalArgumentException if {@code value} can never be valid here: {@code NaN}, an
     *                                  infinite number without a range, or a value Gson cannot
     *                                  write
     */
    public Object clampValue(Object value) {
        try {
            return constraint.apply(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidConfigValueException("Invalid value for config option '" + key + "': " + e.getMessage(), e);
        }
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

    /**
     * Returns a debug text like
     * {@code OptionInfo[visuals.scale (float), sync=OVERRIDE_CLIENT, template=false]}.
     *
     * @return the debug text
     */
    @Override
    public String toString() {
        return "OptionInfo[" + key + " (" + type.getSimpleName() + "), sync=" + syncMode + ", template=" + isTemplate + "]";
    }
}
