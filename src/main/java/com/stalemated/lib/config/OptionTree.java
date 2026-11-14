package com.stalemated.lib.config;

import com.google.gson.Gson;
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory;
import com.stalemated.lib.config.annotation.Comment;
import com.stalemated.lib.config.annotation.Ignore;
import com.stalemated.lib.config.annotation.Nest;
import com.stalemated.lib.config.annotation.Sync;
import com.stalemated.lib.config.annotation.Title;
import com.stalemated.lib.config.internal.CategoryNode;
import com.stalemated.lib.config.internal.ConfigNode;
import com.stalemated.lib.config.internal.Constraint;
import com.stalemated.lib.config.internal.Constraints;
import com.stalemated.lib.config.internal.DeepCopy;
import com.stalemated.lib.config.internal.OptionChange;
import com.stalemated.lib.config.internal.SLibGsonDefaults;
import com.stalemated.lib.config.network.SyncMode;
import com.stalemated.lib.util.internal.ReflectionUtils;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Supplier;

/**
 * The options of a config class, scanned once when the manager is built. Get it from
 * {@link ConfigManager#getOptionTree()}.
 * <p>
 * It holds every option, {@link Nest} category and {@link Sync} mode, with superclass fields
 * first. Invalid annotations fail here with an {@code IllegalArgumentException}. Nested objects
 * that are {@code null} in the default instance are created here.
 */
public class OptionTree {

    private final Class<?> rootClass;
    private final Gson gson;
    private final DeepCopy deepCopy;
    private final Map<String, OptionInfo> options = new LinkedHashMap<>();
    private final List<OptionInfo> syncedOptions = new ArrayList<>();
    private final CategoryNode schemaRoot;

    OptionTree(Class<?> rootClass, Supplier<?> defaultFactory) {
        this(rootClass, defaultFactory, SLibGsonDefaults.create(builder -> {}));
    }

    OptionTree(Class<?> rootClass, Supplier<?> defaultFactory, Gson gson) {
        this.rootClass = rootClass;
        this.gson = gson;
        this.deepCopy = new DeepCopy(gson);
        Object defaultInstance = defaultFactory != null ? defaultFactory.get() : ReflectionUtils.tryInstantiate(rootClass);

        SyncMode rootMode = rootClass.isAnnotationPresent(Sync.class)
                ? rootClass.getAnnotation(Sync.class).value()
                : SyncMode.OVERRIDE_CLIENT;

        this.schemaRoot = buildCategoryNode("", null, rootClass, rootClass, new ArrayList<>(), rootMode, defaultInstance, false);

        for (OptionInfo option : options.values()) {
            if (option.getSyncMode().isSynced()) {
                syncedOptions.add(option);
            }
        }
    }

    private ConfigNode buildNode(String key, Field field, Type genericType, List<Field> path, SyncMode syncMode, Object parentInstance, boolean isTemplate, boolean isTemplateRoot) {
        Class<?> type = ReflectionUtils.getRawClass(genericType);

        if (isTemplateRoot) {
            return buildTemplateRoot(key, field, genericType, type, syncMode);
        }

        if (field.isAnnotationPresent(Nest.class)) {
            if (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) {
                return buildCollectionNode(key, field, genericType, type, path, syncMode, parentInstance, isTemplate);
            }
            return buildCategoryNode(key, field, genericType, type, path, syncMode, parentInstance, isTemplate);
        }

        return buildOptionLeaf(key, field, genericType, type, path, syncMode, parentInstance, isTemplate);
    }

    private ConfigNode buildTemplateRoot(String key, Field field, Type genericType, Class<?> type, SyncMode syncMode) {
        if (!ReflectionUtils.hasPublicNoArgsConstructor(type)) {
            requireNoRangedFields(key, type);
            return buildOptionLeaf(key, field, genericType, type, null, syncMode, null, true);
        }
        if (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) {
            return buildCollectionNode(key, field, genericType, type, null, syncMode, null, true);
        }
        if (gson.getAdapter(type) instanceof ReflectiveTypeAdapterFactory.Adapter<?, ?>) {
            return buildCategoryNode(key, field, genericType, type, null, syncMode, null, true);
        }
        return buildOptionLeaf(key, field, genericType, type, null, syncMode, null, true);
    }

    private OptionInfo buildOptionLeaf(String key, Field field, Type genericType, Class<?> type, List<Field> path, SyncMode syncMode, Object parentInstance, boolean isTemplate) {
        Object defaultValue = resolveDefaultValue(field, parentInstance, isTemplate);
        OptionInfo info = new OptionInfo(key, field, path, syncMode, defaultValue, type, genericType, isTemplate, null, deepCopy);

        if (!isTemplate) this.options.put(key, info);

        return info;
    }

    private Object resolveDefaultValue(Field field, Object parentInstance, boolean isTemplate) {
        if (!isTemplate && parentInstance != null && field != null) {
            return ReflectionUtils.getFieldValue(field, parentInstance);
        }
        return null;
    }

    private OptionInfo buildCollectionNode(String key, Field field, Type genericType, Class<?> type, List<Field> path, SyncMode syncMode, Object parentInstance, boolean isTemplate) {
        Type innerGeneric = extractInnerType(genericType);
        ConfigNode template = buildNode(key + ".*", field, innerGeneric, null, syncMode, null, true, true);
        Object defaultValue = resolveDefaultValue(field, parentInstance, isTemplate);
        OptionInfo info = new OptionInfo(key, field, path, syncMode, defaultValue, type, genericType, isTemplate, template, deepCopy);

        if (!isTemplate) this.options.put(key, info);

        return info;
    }

    private CategoryNode buildCategoryNode(String key, Field field, Type genericType, Class<?> type, List<Field> path, SyncMode syncMode, Object parentInstance, boolean isTemplate) {
        Object currentInstance = resolveCategoryInstance(field, parentInstance, isTemplate);
        List<Field> childrenPath = resolveCategoryPath(path, field, isTemplate);
        Map<Field, ConfigNode> fields = scanCategoryFields(key, type, currentInstance, childrenPath, syncMode, isTemplate);
        Map<String, ConfigNode> children = new LinkedHashMap<>();
        fields.forEach((child, node) -> children.put(child.getName(), node));
        String title = field != null && field.isAnnotationPresent(Title.class)
                ? field.getAnnotation(Title.class).value()
                : null;
        String comment = field != null && field.isAnnotationPresent(Comment.class)
                ? field.getAnnotation(Comment.class).value()
                : null;
        Constraint constraint = isTemplate ? Constraints.ofFields(key, type, fields) : Constraint.NONE;
        return new CategoryNode(field, type, genericType, children, title, comment, constraint);
    }

    private List<Field> resolveCategoryPath(List<Field> path, Field field, boolean isTemplate) {
        if (isTemplate) return null;
        if (field == null) return path == null ? new ArrayList<>() : path;

        List<Field> categoryPath = path == null ? new ArrayList<>() : new ArrayList<>(path);
        categoryPath.add(field);
        return categoryPath;
    }

    private Object resolveCategoryInstance(Field field, Object parentInstance, boolean isTemplate) {
        if (!isTemplate && field != null && parentInstance != null) {
            return getOrCreateNestedInstance(field, parentInstance);
        }
        return parentInstance;
    }

    private Map<Field, ConfigNode> scanCategoryFields(String key, Class<?> type, Object currentInstance, List<Field> path, SyncMode syncMode, boolean isTemplate) {
        Map<Field, ConfigNode> fields = new LinkedHashMap<>();
        for (Field f : ReflectionUtils.declaredFieldsSuperclassFirst(type)) {
            if (isIgnored(f)) continue;

            String childKey = key.isEmpty() ? f.getName() : key + "." + f.getName();
            SyncMode childSyncMode = resolveSyncMode(f, syncMode);

            ConfigNode childNode = buildNode(childKey, f, f.getGenericType(), path, childSyncMode, currentInstance, isTemplate, false);
            fields.put(f, childNode);
        }
        return fields;
    }

    /**
     * Returns whether a field is left out of the config: {@code static}, {@code transient} or
     * marked with {@link Ignore}.
     *
     * @param field the field to check
     * @return {@code true} if the field is ignored
     * @throws NullPointerException if {@code field} is {@code null}
     */
    public static boolean isIgnored(Field field) {
        return ReflectionUtils.isStaticOrTransient(field) || field.isAnnotationPresent(Ignore.class);
    }

    private SyncMode resolveSyncMode(Field field, SyncMode parentMode) {
        if (field.isAnnotationPresent(Sync.class)) {
            return field.getAnnotation(Sync.class).value();
        }

        if (field.isAnnotationPresent(Nest.class) && field.getType().isAnnotationPresent(Sync.class)) {
            return field.getType().getAnnotation(Sync.class).value();
        }
        return parentMode;
    }

    private Object getOrCreateNestedInstance(Field field, Object parentInstance) {
        if (parentInstance == null) return null;

        Object nested = ReflectionUtils.getFieldValue(field, parentInstance);
        if (nested == null) {
            nested = ReflectionUtils.tryInstantiate(field.getType());
            ReflectionUtils.setFieldValue(field, parentInstance, nested);
        }
        return nested;
    }

    private Type extractInnerType(Type genericType) {
        if (genericType instanceof ParameterizedType pType) {
            Type[] typeArgs = pType.getActualTypeArguments();
            if (typeArgs.length == 1) return typeArgs[0];
            if (typeArgs.length >= 2) return typeArgs[1];
        }
        return Object.class;
    }

    private void requireNoRangedFields(String key, Class<?> type) {
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field f : current.getDeclaredFields()) {
                if (!isIgnored(f) && Constraints.isRanged(f)) {
                    throw new IllegalArgumentException(type.getSimpleName() + " in '" + key + "' needs a public no-argument constructor to clamp its ranged fields");
                }
            }
        }
    }

    /**
     * Returns the config class scanned by this tree.
     *
     * @return the root class
     */
    public Class<?> getRootClass() { return rootClass; }

    /**
     * Returns the {@link Gson} of this config: the S-Lib defaults plus the {@code gsonCustomizer}
     * of the builder.
     *
     * @return the {@link Gson} instance
     */
    public Gson gson() { return gson; }

    /**
     * Used by the edit session of the S-Lib config screens to copy values.
     */
    @ApiStatus.Internal
    public DeepCopy deepCopy() { return deepCopy; }

    /**
     * Used by the S-Lib YACL layer to walk the options in declaration order.
     */
    @ApiStatus.Internal
    public CategoryNode getSchemaRoot() { return schemaRoot; }

    /**
     * Returns the option with the given key, with dots for {@link Nest} fields.
     * <p>
     * A {@link Nest} object field is a category, not an option; a {@link Nest} collection or map is
     * an option. Element templates are not included.
     *
     * @param key the option key, for example {@code visuals.scale}
     * @return the option, or {@code null} if there is none with that key
     */
    public OptionInfo get(String key) { return options.get(key); }

    /**
     * Returns all options in scan order: the fields of the highest superclass first, then those
     * of each subclass, each class in declaration order. Templates are not included.
     *
     * @return an unmodifiable collection of the options
     */
    public Collection<OptionInfo> all() { return Collections.unmodifiableCollection(options.values()); }

    /**
     * Returns the options whose sync mode is {@link SyncMode#OVERRIDE_CLIENT} or
     * {@link SyncMode#INFORM_SERVER}.
     *
     * @return an unmodifiable list of the synced options
     */
    public List<OptionInfo> getSyncedOptions() { return Collections.unmodifiableList(syncedOptions); }

    /**
     * Returns whether any synced option uses {@code mode}. Always {@code false} for
     * {@link SyncMode#NONE}, since those options are not synced.
     *
     * @param mode the sync mode to look for
     * @return {@code true} if a synced option uses {@code mode}
     */
    public boolean hasOptionsSyncedAs(SyncMode mode) {
        return syncedOptions.stream().anyMatch(option -> option.getSyncMode() == mode);
    }

    /**
     * Deep-copies every option from {@code source} into {@code target}, clamping each value.
     *
     * @param source the instance to copy from; if {@code null}, nothing happens
     * @param target the instance to copy into; if {@code null}, nothing happens
     * @throws IllegalArgumentException if {@code source} has a value that can never be valid,
     *                                  like {@code NaN}
     */
    public void copyAllValues(Object source, Object target) {
        if (source == null || target == null) return;

        for (OptionInfo option : options.values()) {
            Object val = option.getValue(source);
            option.setValue(target, deepCopy.of(val, option.genericType()));
        }
    }

    /**
     * Compares two instances option by option, clamping the values of {@code candidate}, and
     * returns the options that differ. Used by the S-Lib managers and config screens.
     *
     * @param current the instance to compare against
     * @param candidate the changed instance
     * @return an unmodifiable list of the changes; empty if either instance is {@code null}
     * @throws IllegalArgumentException if {@code candidate} has a value that can never be valid
     */
    @ApiStatus.Internal
    public List<OptionChange> findChanges(Object current, Object candidate) {
        if (current == null || candidate == null) {
            return Collections.emptyList();
        }

        List<OptionChange> changes = new ArrayList<>();
        for (OptionInfo option : options.values()) {
            Object currentVal = option.getValue(current);
            Object candidateVal = option.getValue(candidate);
            Object clampedVal = option.clampValue(candidateVal);

            if (!sameValue(currentVal, clampedVal)) {
                changes.add(new OptionChange(option, clampedVal));
            }
        }
        return Collections.unmodifiableList(changes);
    }

    /**
     * Compares two option values deeply, skipping {@code static}, {@code transient} and
     * {@link Ignore} fields.
     *
     * @param a a value, or {@code null}
     * @param b another value, or {@code null}
     * @return {@code true} if both values are equal
     */
    public boolean sameValue(Object a, Object b) {
        return ReflectionUtils.deepEquals(a, b, OptionTree::isIgnored);
    }
}
