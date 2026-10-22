package com.stalemated.lib.compat.yacl.util;

import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.session.ConfigSession;
import com.stalemated.lib.util.reflection.ReflectionUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Utility helper methods for managing nested session buffers and map mutations in YACL.
 */
public final class YaclSessionHelper {

    private YaclSessionHelper() {}

    /**
     * Atomically mutates a nested item within a map stored in a {@link ConfigSession}, cloning it deeply
     * to preserve draft isolation before applying the modification and persisting the cloned map back to the session.
     *
     * @param session  The configuration session holding the draft buffer.
     * @param mapInfo  The {@link OptionInfo} representing the parent map option.
     * @param entryKey The key of the item within the map.
     * @param updater  A consumer that mutates the deeply cloned item.
     * @param <N>      The type of the nested item.
     */
    @SuppressWarnings("unchecked")
    public static <N> void updateNestedMap(
            ConfigSession<?> session,
            OptionInfo mapInfo,
            String entryKey,
            Consumer<N> updater
    ) {
        Map<String, N> currentMap = session.get(mapInfo);
        if (currentMap == null && mapInfo.getDefaultValue() instanceof Map<?, ?> defMap) {
            currentMap = (Map<String, N>) defMap;
        }

        Map<String, N> newMap = currentMap != null ? new LinkedHashMap<>(currentMap) : new LinkedHashMap<>();
        N item = newMap.get(entryKey);

        if (item != null) {
            N clonedItem = (N) ReflectionUtils.cloneDeep(item);
            updater.accept(clonedItem);
            newMap.put(entryKey, clonedItem);
            if ((mapInfo.type() != null && SortedMap.class.isAssignableFrom(mapInfo.type()))
                    || (mapInfo.getDefaultValue() instanceof SortedMap)) {
                session.set(mapInfo, new TreeMap<>(newMap));
            } else {
                session.set(mapInfo, new LinkedHashMap<>(newMap));
            }
        }
    }
}
