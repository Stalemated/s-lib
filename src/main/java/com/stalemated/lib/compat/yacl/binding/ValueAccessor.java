package com.stalemated.lib.compat.yacl.binding;

import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.session.ConfigSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ValueAccessor<M> {

    private final String key;
    private final Supplier<M> reader;
    private final Consumer<M> writer;

    private ValueAccessor(String key, Supplier<M> reader, Consumer<M> writer) {
        this.key = Objects.requireNonNull(key, "key cannot be null");
        this.reader = Objects.requireNonNull(reader, "reader cannot be null");
        this.writer = Objects.requireNonNull(writer, "writer cannot be null");
    }

    public static <M> ValueAccessor<M> of(String key, Supplier<M> reader, Consumer<M> writer) {
        return new ValueAccessor<>(key, reader, writer);
    }

    public static <M> ValueAccessor<M> option(ConfigSession<?> session, OptionInfo info) {
        return of(info.getKey(), () -> session.get(info), value -> session.set(info, value));
    }

    public static <E> ValueAccessor<List<E>> collection(ConfigSession<?> session, OptionInfo info) {
        return of(
                info.getKey(),
                () -> {
                    Collection<E> current = session.get(info);
                    return current != null ? new ArrayList<>(current) : null;
                },
                list -> {
                    Objects.requireNonNull(list, "collection option " + info.getKey() + " cannot be set to null");
                    session.set(info, new ArrayList<>(list));
                }
        );
    }

    public static ValueAccessor<Object> mapEntry(ConfigSession<?> session, OptionInfo mapInfo, String entryKey) {
        Objects.requireNonNull(entryKey, "entryKey cannot be null");
        return of(
                mapInfo.getKey(),
                () -> mapOrDefaults(session, mapInfo).get(entryKey),
                item -> {
                    Map<Object, Object> updated = new LinkedHashMap<>(mapOrDefaults(session, mapInfo));
                    updated.put(entryKey, item);
                    session.set(mapInfo, mapInfo.isSortedMap() ? new TreeMap<>(updated) : updated);
                }
        );
    }

    public static ValueAccessor<Object> listEntry(ConfigSession<?> session, OptionInfo listInfo, int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("list entry index must be 0 or greater, got " + index);
        }
        return of(
                listInfo.getKey(),
                () -> {
                    List<?> list = listOrDefaults(session, listInfo);
                    return index < list.size() ? list.get(index) : null;
                },
                item -> {
                    List<Object> updated = new ArrayList<>(listOrDefaults(session, listInfo));
                    updated.set(index, item);
                    session.set(listInfo, updated);
                }
        );
    }

    public static <E> ValueAccessor<E> listElement(ConfigSession<?> session, OptionInfo listInfo, int index, E padding) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("list element index must be 0 or greater, got " + index);
        }
        return of(
                listInfo.getKey(),
                () -> {
                    List<E> list = session.get(listInfo);
                    return list != null && index < list.size() ? list.get(index) : null;
                },
                value -> {
                    List<E> current = session.get(listInfo);
                    List<E> updated = current != null ? new ArrayList<>(current) : new ArrayList<>();
                    while (updated.size() <= index) {
                        updated.add(padding);
                    }
                    updated.set(index, value);
                    session.set(listInfo, updated);
                }
        );
    }

    public static Map<?, ?> defaultMapOf(OptionInfo mapInfo) {
        return mapInfo.defaultCopy() instanceof Map<?, ?> defaults ? defaults : Map.of();
    }

    public static List<?> defaultListOf(OptionInfo listInfo) {
        return listInfo.defaultCopy() instanceof List<?> defaults ? defaults : List.of();
    }

    private static Map<?, ?> mapOrDefaults(ConfigSession<?> session, OptionInfo mapInfo) {
        Map<?, ?> current = session.get(mapInfo);
        return current != null ? current : defaultMapOf(mapInfo);
    }

    private static List<?> listOrDefaults(ConfigSession<?> session, OptionInfo listInfo) {
        List<?> current = session.get(listInfo);
        return current != null ? current : defaultListOf(listInfo);
    }

    public <P> ValueAccessor<P> property(Function<M, P> read, BiConsumer<M, P> write) {
        return of(
                key,
                () -> {
                    M owner = get();
                    return owner != null ? read.apply(owner) : null;
                },
                value -> {
                    M owner = get();
                    if (owner != null) {
                        write.accept(owner, value);
                        set(owner);
                    }
                }
        );
    }

    public String key() {
        return key;
    }

    public M get() {
        return reader.get();
    }

    public void set(M value) {
        writer.accept(value);
    }
}
