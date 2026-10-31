package com.stalemated.lib.compat.yacl.util;

import com.stalemated.lib.config.io.json5.DeepCopy;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.session.ConfigSession;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Utility helper methods for managing nested session buffers and container mutations in YACL.
 */
public final class YaclSessionHelper {

    private YaclSessionHelper() {}

    public static Map<?, ?> defaultMapOf(OptionInfo mapInfo) {
        return mapInfo.defaultCopy() instanceof Map<?, ?> defaults ? defaults : Map.of();
    }

    public static List<?> defaultListOf(OptionInfo listInfo) {
        return listInfo.defaultCopy() instanceof List<?> defaults ? defaults : List.of();
    }

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
        Map<String, N> newMap = new LinkedHashMap<>(currentMap != null ? currentMap : (Map<String, N>) defaultMapOf(mapInfo));
        N item = newMap.get(entryKey);

        if (item != null) {
            N clonedItem = DeepCopy.of(item, item.getClass());
            updater.accept(clonedItem);
            newMap.put(entryKey, clonedItem);
            if (mapInfo.isSortedMap()) {
                session.set(mapInfo, new TreeMap<>(newMap));
            } else {
                session.set(mapInfo, newMap);
            }
        }
    }

    /**
     * Atomically mutates a nested item within a list stored in a {@link ConfigSession}, cloning it deeply
     * to preserve draft isolation before applying the modification and persisting the cloned list back to the session.
     *
     * @param session  The config session holding the draft buffer.
     * @param listInfo The {@link OptionInfo} representing the parent list option.
     * @param index    The zero-based index of the item within the list.
     * @param updater  A consumer that mutates the deeply cloned item.
     * @param <N>      The type of the nested item.
     */
    @SuppressWarnings("unchecked")
    public static <N> void updateNestedList(
            ConfigSession<?> session,
            OptionInfo listInfo,
            int index,
            Consumer<N> updater
    ) {
        List<N> currentList = session.get(listInfo);
        List<N> newList = new ArrayList<>(currentList != null ? currentList : (List<N>) defaultListOf(listInfo));
        if (index >= 0 && index < newList.size()) {
            N item = newList.get(index);
            if (item != null) {
                N clonedItem = DeepCopy.of(item, item.getClass());
                updater.accept(clonedItem);
                newList.set(index, clonedItem);
                session.set(listInfo, newList);
            }
        }
    }
}
