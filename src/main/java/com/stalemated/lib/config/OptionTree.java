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
 * Scans and caches all options, nested structures ({@link Nest}),
 * sync modes ({@link Sync}) and generic
 * schema templates once during initialization.
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

    public Class<?> getRootClass() { return rootClass; }

    public Gson gson() { return gson; }

    @ApiStatus.Internal
    public DeepCopy deepCopy() { return deepCopy; }

    @ApiStatus.Internal
    public CategoryNode getSchemaRoot() { return schemaRoot; }

    public OptionInfo get(String key) { return options.get(key); }

    public Collection<OptionInfo> all() { return Collections.unmodifiableCollection(options.values()); }

    public List<OptionInfo> getSyncedOptions() { return Collections.unmodifiableList(syncedOptions); }

    public boolean hasOptionsSyncedAs(SyncMode mode) {
        return syncedOptions.stream().anyMatch(option -> option.getSyncMode() == mode);
    }

    /**
     * Copies all values from source to target.
     *
     * @param source The source config object.
     * @param target The target config object.
     */
    public void copyAllValues(Object source, Object target) {
        if (source == null || target == null) return;

        for (OptionInfo option : options.values()) {
            Object val = option.getValue(source);
            option.setValue(target, deepCopy.of(val, option.genericType()));
        }
    }

    /**
     * Compares two config instances, validating and clamping candidate values
     * against schema boundaries, and returns the list of detected effective changes.
     *
     * @param current The baseline config instance.
     * @param candidate The candidate or modified config instance.
     * @return An unmodifiable list of detected {@link OptionChange} descriptors.
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

    public boolean sameValue(Object a, Object b) {
        return ReflectionUtils.deepEquals(a, b, OptionTree::isIgnored);
    }
}
