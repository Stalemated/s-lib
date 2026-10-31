package com.stalemated.lib.compat.yacl.binding;

import com.stalemated.lib.compat.yacl.util.NestedAccessor;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.session.ConfigSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
        boolean storedAsSet = Set.class.isAssignableFrom(info.type());
        return of(
                info.getKey(),
                () -> {
                    Collection<E> current = session.get(info);
                    return current != null ? new ArrayList<>(current) : null;
                },
                list -> {
                    Objects.requireNonNull(list, "collection option " + info.getKey() + " cannot be set to null");
                    session.set(info, storedAsSet ? new LinkedHashSet<>(list) : new ArrayList<>(list));
                }
        );
    }

    public static <M> ValueAccessor<M> entryProperty(
            ConfigSession<?> session,
            OptionInfo containerInfo,
            NestedAccessor entry,
            Function<Object, M> read,
            BiConsumer<Object, M> write
    ) {
        return of(
                containerInfo.getKey(),
                () -> {
                    Object item = entry.getItem(session, containerInfo);
                    return item != null ? read.apply(item) : null;
                },
                value -> entry.updateItem(session, containerInfo, item -> write.accept(item, value))
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
