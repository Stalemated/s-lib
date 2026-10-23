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
     * Resolves the default value of the nested item from the container's default structure.
     *
     * @param containerInfo The metadata representing the parent container.
     * @return The default item instance, or null if absent.
     */
    Object getDefaultItem(OptionInfo containerInfo);

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
     * @return A map entry accessor.
     */
    static NestedAccessor forMap(String entryKey) {
        return new MapNestedAccessor(Objects.requireNonNull(entryKey, "entryKey cannot be null"));
    }

    /**
     * Creates an accessor targeting an entry in a List container by zero-based index.
     *
     * @param index The element index.
     * @return A list entry accessor.
     */
    static NestedAccessor forList(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("index cannot be negative: " + index);
        }
        return new ListNestedAccessor(index);
    }

    record MapNestedAccessor(String entryKey) implements NestedAccessor {
        @Override
        public Object getItem(ConfigSession<?> session, OptionInfo containerInfo) {
            Map<?, ?> map = session.get(containerInfo);
            if (map != null) {
                return map.get(entryKey);
            }
            return getDefaultItem(containerInfo);
        }

        @Override
        public void updateItem(ConfigSession<?> session, OptionInfo containerInfo, Consumer<Object> updater) {
            YaclSessionHelper.updateNestedMap(session, containerInfo, entryKey, updater);
        }

        @Override
        public Object getDefaultItem(OptionInfo containerInfo) {
            if (containerInfo.getDefaultValue() instanceof Map<?, ?> defMap) {
                return defMap.get(entryKey);
            }
            return null;
        }

        @Override
        public String identifier() {
            return entryKey;
        }
    }

    record ListNestedAccessor(int index) implements NestedAccessor {
        @Override
        public Object getItem(ConfigSession<?> session, OptionInfo containerInfo) {
            List<?> list = session.get(containerInfo);
            if (list != null) {
                return (index >= 0 && index < list.size()) ? list.get(index) : null;
            }
            return getDefaultItem(containerInfo);
        }

        @Override
        public void updateItem(ConfigSession<?> session, OptionInfo containerInfo, Consumer<Object> updater) {
            YaclSessionHelper.updateNestedList(session, containerInfo, index, updater);
        }

        @Override
        public Object getDefaultItem(OptionInfo containerInfo) {
            if (containerInfo.getDefaultValue() instanceof List<?> defList && index >= 0 && index < defList.size()) {
                return defList.get(index);
            }
            return null;
        }

        @Override
        public String identifier() {
            return String.valueOf(index);
        }
    }
}
