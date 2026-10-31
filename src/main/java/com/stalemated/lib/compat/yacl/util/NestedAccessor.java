package com.stalemated.lib.compat.yacl.util;

import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.session.ConfigSession;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Accessor strategy decoupling nested field option bindings from the container data structure (e.g., Map vs. List).
 */
public interface NestedAccessor {

    /**
     * Retrieves the target nested item from the session's active draft buffer.
     *
     * @param session The configuration session holding the draft buffer.
     * @param containerInfo The metadata representing the parent container (Map or List).
     * @return The item instance, or null if absent.
     */
    Object getItem(ConfigSession<?> session, OptionInfo containerInfo);

    /**
     * Atomically mutates the target nested item inside the session's active draft buffer.
     *
     * @param session The configuration session holding the draft buffer.
     * @param containerInfo The metadata representing the parent container (Map or List).
     * @param updater A consumer that modifies the isolated cloned item.
     */
    void updateItem(ConfigSession<?> session, OptionInfo containerInfo, Consumer<Object> updater);

    /**
     * Returns the default value of the nested item, taken from the container's default structure.
     *
     * @return The default item instance, or null if absent.
     */
    Object getDefaultItem();

    /**
     * Returns a string identifier for this entry (e.g., map key or list index string).
     *
     * @return The entry identifier.
     */
    String identifier();

    /**
     * Creates an accessor targeting an entry in a Map container by key.
     *
     * @param entryKey The map key.
     * @param defaults A copy of the container's default map.
     * @return A map entry accessor.
     */
    static NestedAccessor forMap(String entryKey, Map<?, ?> defaults) {
        Objects.requireNonNull(entryKey, "entryKey cannot be null");
        return new MapNestedAccessor(entryKey, defaults.get(entryKey));
    }

    /**
     * Creates an accessor targeting an entry in a List container by zero-based index.
     *
     * @param index The element index.
     * @param defaults A copy of the container's default list.
     * @return A list entry accessor.
     */
    static NestedAccessor forList(int index, List<?> defaults) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("index cannot be negative: " + index);
        }
        return new ListNestedAccessor(index, index < defaults.size() ? defaults.get(index) : null);
    }

    record MapNestedAccessor(String entryKey, Object defaultItem) implements NestedAccessor {
        @Override
        public Object getItem(ConfigSession<?> session, OptionInfo containerInfo) {
            Map<?, ?> map = session.get(containerInfo);
            return map != null ? map.get(entryKey) : null;
        }

        @Override
        public void updateItem(ConfigSession<?> session, OptionInfo containerInfo, Consumer<Object> updater) {
            YaclSessionHelper.updateNestedMap(session, containerInfo, entryKey, updater);
        }

        @Override
        public Object getDefaultItem() {
            return defaultItem;
        }

        @Override
        public String identifier() {
            return entryKey;
        }
    }

    record ListNestedAccessor(int index, Object defaultItem) implements NestedAccessor {
        @Override
        public Object getItem(ConfigSession<?> session, OptionInfo containerInfo) {
            List<?> list = session.get(containerInfo);
            return list != null && index >= 0 && index < list.size() ? list.get(index) : null;
        }

        @Override
        public void updateItem(ConfigSession<?> session, OptionInfo containerInfo, Consumer<Object> updater) {
            YaclSessionHelper.updateNestedList(session, containerInfo, index, updater);
        }

        @Override
        public Object getDefaultItem() {
            return defaultItem;
        }

        @Override
        public String identifier() {
            return String.valueOf(index);
        }
    }
}
