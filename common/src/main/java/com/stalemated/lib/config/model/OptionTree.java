package com.stalemated.lib.config.model;

import com.stalemated.lib.config.annotation.Ignore;
import com.stalemated.lib.config.annotation.Nest;
import com.stalemated.lib.config.annotation.Sync;
import com.stalemated.lib.config.network.SyncMode;
import com.stalemated.lib.util.reflection.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Scans and caches all options, nested structures ({@link Nest}), and sync modes ({@link Sync})
 * once during initialization.
 */
public class OptionTree {

    private final Class<?> rootClass;
    private final Map<String, OptionInfo> options = new LinkedHashMap<>();
    private final List<OptionInfo> syncedOptions = new ArrayList<>();
    private final List<OptionInfo> localOnlyOptions = new ArrayList<>();

    public OptionTree(Class<?> rootClass, Supplier<?> defaultFactory) {
        this.rootClass = rootClass;
        Object defaultInstance = defaultFactory != null ? defaultFactory.get() : ReflectionUtils.tryInstantiate(rootClass);

        SyncMode rootMode = rootClass.isAnnotationPresent(Sync.class)
                ? rootClass.getAnnotation(Sync.class).value()
                : SyncMode.OVERRIDE_CLIENT;

        scanClass("", rootClass, new ArrayList<>(), defaultInstance, rootMode);

        for (OptionInfo option : options.values()) {
            if (option.getSyncMode().isSynced()) {
                syncedOptions.add(option);
            } else {
                localOnlyOptions.add(option);
            }
        }
    }

    private void scanClass(String prefix, Class<?> clazz, List<Field> path, Object instance, SyncMode parentSyncMode) {
        Class<?> current = clazz;

        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (isIgnored(field)) continue;

                field.setAccessible(true);
                String key = prefix.isEmpty() ? field.getName() : prefix + "." + field.getName();
                SyncMode resolvedSyncMode = resolveSyncMode(field, parentSyncMode);

                if (field.isAnnotationPresent(Nest.class)) {
                    Object nestedInstance = getOrCreateNestedInstance(field, instance);
                    
                    List<Field> nextPath = new ArrayList<>(path);
                    nextPath.add(field);
                    scanClass(key, field.getType(), nextPath, nestedInstance, resolvedSyncMode);
                } else {
                    Object defaultValue = ReflectionUtils.getFieldValueSafe(field, instance);
                    OptionInfo info = new OptionInfo(key, field, path, resolvedSyncMode, defaultValue);
                    options.put(key, info);
                }
            }
            current = current.getSuperclass();
        }
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
        if (parentInstance == null) {
            return null;
        }
        
        Object nested = ReflectionUtils.getFieldValueSafe(field, parentInstance);
        if (nested == null) {
            nested = ReflectionUtils.tryInstantiate(field.getType());
            ReflectionUtils.setFieldValueSafe(field, parentInstance, nested);
        }
        return nested;
    }

    public Class<?> getRootClass() {
        return rootClass;
    }

    public OptionInfo get(String key) {
        return options.get(key);
    }

    public Collection<OptionInfo> all() {
        return Collections.unmodifiableCollection(options.values());
    }

    public List<OptionInfo> getSyncedOptions() {
        return Collections.unmodifiableList(syncedOptions);
    }

    public List<OptionInfo> getLocalOnlyOptions() {
        return Collections.unmodifiableList(localOnlyOptions);
    }

    /**
     * Copies only options marked with {@link SyncMode#OVERRIDE_CLIENT} from source to target.
     *
     * @param source The source config object (e.g. parsed from server).
     * @param target The target config object (e.g. active config).
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
