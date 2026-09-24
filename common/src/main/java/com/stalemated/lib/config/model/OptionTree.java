package com.stalemated.lib.config.model;

import com.stalemated.lib.config.annotation.Ignore;
import com.stalemated.lib.config.annotation.Nest;
import com.stalemated.lib.config.annotation.Sync;
import com.stalemated.lib.config.network.SyncMode;
import com.stalemated.lib.util.reflection.ReflectionUtils;

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
    private final Map<String, OptionInfo> options = new LinkedHashMap<>();
    private final List<OptionInfo> syncedOptions = new ArrayList<>();
    private final List<OptionInfo> localOnlyOptions = new ArrayList<>();
    private final ConfigNode schemaRoot;

    public OptionTree(Class<?> rootClass, Supplier<?> defaultFactory) {
        this.rootClass = rootClass;
        Object defaultInstance = defaultFactory != null ? defaultFactory.get() : ReflectionUtils.tryInstantiate(rootClass);

        SyncMode rootMode = rootClass.isAnnotationPresent(Sync.class)
                ? rootClass.getAnnotation(Sync.class).value()
                : SyncMode.OVERRIDE_CLIENT;

        this.schemaRoot = buildNode("", null, rootClass, new ArrayList<>(), rootMode, defaultInstance, false, false);

        for (OptionInfo option : options.values()) {
            if (option.getSyncMode().isSynced()) {
                syncedOptions.add(option);
            } else {
                localOnlyOptions.add(option);
            }
        }
    }

    private ConfigNode buildNode(String key, Field field, Type genericType, List<Field> path, SyncMode syncMode, Object parentInstance, boolean isTemplate, boolean isTemplateRoot) {
        Class<?> type = getRawClass(genericType);

        if (field == null) {
            return buildCategoryNode(key, null, genericType, type, path, syncMode, parentInstance, isTemplate);
        }

        if (isTemplateRoot) {
            if (isNativeLeaf(type)) {
                return buildOptionLeaf(key, field, genericType, type, path, syncMode, parentInstance, true);
            }
            if (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) {
                return buildCollectionNode(key, field, genericType, type, path, syncMode, parentInstance, true);
            }
            return buildCategoryNode(key, field, genericType, type, path, syncMode, parentInstance, true);
        }

        if (field.isAnnotationPresent(Nest.class)) {
            if (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) {
                return buildCollectionNode(key, field, genericType, type, path, syncMode, parentInstance, isTemplate);
            }
            return buildCategoryNode(key, field, genericType, type, path, syncMode, parentInstance, isTemplate);
        }

        return buildOptionLeaf(key, field, genericType, type, path, syncMode, parentInstance, isTemplate);
    }

    private OptionInfo buildOptionLeaf(String key, Field field, Type genericType, Class<?> type, List<Field> path, SyncMode syncMode, Object parentInstance, boolean isTemplate) {
        Object defaultValue = resolveDefaultValue(field, parentInstance, isTemplate);
        OptionInfo info = new OptionInfo(key, field, path, syncMode, defaultValue, type, genericType, isTemplate, null);

        if (!isTemplate) this.options.put(key, info);

        return info;
    }

    private Object resolveDefaultValue(Field field, Object parentInstance, boolean isTemplate) {
        if (!isTemplate && parentInstance != null && field != null) {
            return ReflectionUtils.getFieldValueSafe(field, parentInstance);
        }
        return null;
    }

    private OptionInfo buildCollectionNode(String key, Field field, Type genericType, Class<?> type, List<Field> path, SyncMode syncMode, Object parentInstance, boolean isTemplate) {
        Type innerGeneric = extractInnerType(genericType);
        ConfigNode template = buildNode(key + ".*", field, innerGeneric, null, syncMode, null, true, true);
        Object defaultValue = resolveDefaultValue(field, parentInstance, isTemplate);
        OptionInfo info = new OptionInfo(key, field, path, syncMode, defaultValue, type, genericType, isTemplate, template);

        if (!isTemplate) this.options.put(key, info);

        return info;
    }

    private CategoryNode buildCategoryNode(String key, Field field, Type genericType, Class<?> type, List<Field> path, SyncMode syncMode, Object parentInstance, boolean isTemplate) {
        Object currentInstance = resolveCategoryInstance(field, parentInstance, isTemplate);
        List<Field> childrenPath = resolveCategoryPath(path, field, isTemplate);
        Map<String, ConfigNode> children = scanCategoryChildren(key, type, currentInstance, childrenPath, syncMode, isTemplate);
        return new CategoryNode(type, genericType, children);
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

    private Map<String, ConfigNode> scanCategoryChildren(String key, Class<?> type, Object currentInstance, List<Field> path, SyncMode syncMode, boolean isTemplate) {
        Map<String, ConfigNode> children = new LinkedHashMap<>();
        Class<?> current = type;

        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                if (isIgnored(f)) continue;

                String childKey = key.isEmpty() ? f.getName() : key + "." + f.getName();
                SyncMode childSyncMode = resolveSyncMode(f, syncMode);

                ConfigNode childNode = buildNode(childKey, f, f.getGenericType(), path, childSyncMode, currentInstance, isTemplate, false);
                children.put(f.getName(), childNode);
            }
            current = current.getSuperclass();
        }

        return children;
    }

    private boolean isIgnored(Field field) {
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

        Object nested = ReflectionUtils.getFieldValueSafe(field, parentInstance);
        if (nested == null) {
            nested = ReflectionUtils.tryInstantiate(field.getType());
            ReflectionUtils.setFieldValueSafe(field, parentInstance, nested);
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

    private Class<?> getRawClass(Type type) {
        if (type instanceof Class<?> c) return c;
        if (type instanceof ParameterizedType pType) return (Class<?>) pType.getRawType();
        return Object.class;
    }

    private boolean isNativeLeaf(Class<?> type) {
        if (type.isPrimitive() || Number.class.isAssignableFrom(type) ||
            type == String.class || type == Boolean.class ||
            type == Character.class || type.isEnum()) {
            return true;
        }
        return !ReflectionUtils.hasPublicNoArgsConstructor(type);
    }

    public Class<?> getRootClass() { return rootClass; }

    public ConfigNode getSchemaRoot() { return schemaRoot; }

    public OptionInfo get(String key) { return options.get(key); }

    public Collection<OptionInfo> all() { return Collections.unmodifiableCollection(options.values()); }

    public List<OptionInfo> getSyncedOptions() { return Collections.unmodifiableList(syncedOptions); }

    public List<OptionInfo> getLocalOnlyOptions() { return Collections.unmodifiableList(localOnlyOptions); }

    /**
     * Copies only options marked with {@link SyncMode#OVERRIDE_CLIENT} from source to target.
     *
     * @param source The source config object (e.g., parsed from server).
     * @param target The target config object (e.g., active config).
     */
    public void copySyncedValues(Object source, Object target) {
        if (source == null || target == null) return;

        for (OptionInfo option : syncedOptions) {
            if (option.getSyncMode() == SyncMode.OVERRIDE_CLIENT) {
                Object val = option.getValue(source);
                option.setValue(target, val);
            }
        }
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
            option.setValue(target, val);
        }
    }
}
