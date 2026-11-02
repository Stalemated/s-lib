package com.stalemated.lib.config.model;

import com.stalemated.lib.util.reflection.ReflectionUtils;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Supplier;

final class ContainerFactory {

    private static final Map<Class<?>, Supplier<Collection<Object>>> COLLECTIONS = Map.of(
            Collection.class, ArrayList::new,
            List.class, ArrayList::new,
            Set.class, LinkedHashSet::new,
            SortedSet.class, TreeSet::new,
            NavigableSet.class, TreeSet::new,
            Queue.class, ArrayDeque::new,
            Deque.class, ArrayDeque::new
    );

    private static final Map<Class<?>, Supplier<Map<Object, Object>>> MAPS = Map.of(
            Map.class, LinkedHashMap::new,
            SortedMap.class, TreeMap::new,
            NavigableMap.class, TreeMap::new,
            ConcurrentMap.class, ConcurrentHashMap::new,
            ConcurrentNavigableMap.class, ConcurrentSkipListMap::new
    );

    private ContainerFactory() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    static Collection<Object> newCollection(Class<?> type, Class<?> elementType) {
        if (EnumSet.class.isAssignableFrom(type)) {
            return EnumSet.noneOf((Class) requireEnum(type, elementType));
        }
        Supplier<Collection<Object>> standard = COLLECTIONS.get(type);
        return standard != null ? standard.get() : instantiate(type);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static Map<Object, Object> newMap(Class<?> type, Class<?> keyType) {
        if (EnumMap.class.isAssignableFrom(type)) {
            return new EnumMap(requireEnum(type, keyType));
        }
        Supplier<Map<Object, Object>> standard = MAPS.get(type);
        return standard != null ? standard.get() : instantiate(type);
    }

    private static Class<?> requireEnum(Class<?> containerType, Class<?> typeArgument) {
        if (!typeArgument.isEnum()) {
            throw new IllegalArgumentException(containerType.getSimpleName() + " needs an enum type argument, got " + typeArgument.getName());
        }
        return typeArgument;
    }

    @SuppressWarnings("unchecked")
    private static <C> C instantiate(Class<?> type) {
        if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            throw new IllegalArgumentException("Cannot create a " + type.getName() + "; declare the field as a concrete class or a standard collection interface");
        }
        return (C) ReflectionUtils.tryInstantiate(type);
    }
}
