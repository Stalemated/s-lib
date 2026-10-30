package com.stalemated.lib.compat.yacl.factory;

import com.stalemated.lib.compat.yacl.builder.NestListBinding;
import com.stalemated.lib.compat.yacl.registry.YaclOptionMetadata;
import com.stalemated.lib.compat.yacl.util.YaclSessionHelper;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.util.TypeCoercionUtils;
import com.stalemated.lib.util.codec.KeyValuePair;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Metadata-driven factory that produces YACL {@link ListOption} widgets for {@code Map<String, ?>} config options,
 * including both key-value pair lists and projected inner list properties of nested POJO maps.
 *
 * @param <T> The config model type.
 * @param context The shared YACL UI construction context.
 */
public record YaclMapOptionFactory<T>(YaclContext<T> context) {

    public YaclMapOptionFactory {
        Objects.requireNonNull(context, "context cannot be null");
    }

    public ListOption<String> buildMapPairsOption(OptionInfo info) {
        return buildMapPairsOption(info, null);
    }

    @SuppressWarnings("unchecked")
    public ListOption<String> buildMapPairsOption(
            OptionInfo info,
            Consumer<ListOption.Builder<String>> customizer
    ) {
        YaclOptionMetadata meta = context.resolveMetadata(info);
        Class<?> valClass = info.getMapValueType();

        ListOption.Builder<String> builder = ListOption.createBuilder();
        builder.name(meta.name())
                .description(meta.description())
                .available(meta.available());

        Map<String, ?> defaultMap = (Map<String, ?>) info.getDefaultValue();
        List<String> defaultGui = KeyValuePair.toLines(defaultMap);

        return context.synchronizer().buildSyncedListOption(
                info,
                builder,
                defaultGui,
                () -> KeyValuePair.toLines(context.session().get(info)),
                guiList -> persistGuiMap(info, defaultMap, guiList, valClass),
                b -> {
                    b.controller(StringControllerBuilder::create);
                    b.initial(resolveInitialEntry(valClass));
                    if (customizer != null) {
                        customizer.accept(b);
                    }
                }
        );
    }

    /**
     * Builds a list of YACL {@link ListOption} widgets projecting a dynamic {@code List<String>} property
     * for each entry inside a nested POJO map using a {@link NestListBinding}.
     *
     * @param mapInfo The option metadata for the parent map.
     * @param binding The functional binding for reading and writing the list property on each POJO item.
     * @param <N> The type of the nested POJO item.
     * @return A list of {@link ListOption} groups, one per map entry.
     */
    @SuppressWarnings("unchecked")
    public <N> List<ListOption<String>> buildProjectedMapListOptions(
            OptionInfo mapInfo,
            NestListBinding<N> binding
    ) {
        if (mapInfo == null) {
            return Collections.emptyList();
        }

        boolean available = context.availabilityRule().isAvailable(mapInfo, context.session());
        Map<String, N> currentMap = context.session().get(mapInfo);
        if (currentMap == null && mapInfo.getDefaultValue() instanceof Map<?, ?> defMap) {
            currentMap = (Map<String, N>) defMap;
        }
        if (currentMap == null || currentMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<ListOption<String>> options = new ArrayList<>(currentMap.size());
        OptionDescription desc = context.namingStrategy().resolveDescription(context.modId(), mapInfo, available);

        for (String entryKey : currentMap.keySet()) {
            Component name = Component.literal(entryKey);
            List<String> defaultList = resolveDefaultProjectedList(mapInfo, entryKey, binding.getter());

            ListOption.Builder<String> builder = ListOption.<String>createBuilder()
                    .name(name)
                    .description(desc)
                    .available(available);

            ListOption<String> listOpt = context.synchronizer().buildSyncedListOption(
                    mapInfo,
                    builder,
                    defaultList,
                    () -> getProjectedList(mapInfo, entryKey, binding.getter()),
                    val -> YaclSessionHelper.<N>updateNestedMap(
                            context.session(),
                            mapInfo,
                            entryKey,
                            item -> binding.setter().accept(item, new ArrayList<>(val))
                    ),
                    b -> {
                        b.controller(StringControllerBuilder::create);
                        b.initial("");
                    }
            );

            options.add(listOpt);
        }

        return options;
    }

    @SuppressWarnings("unchecked")
    private <N> List<String> resolveDefaultProjectedList(
            OptionInfo mapInfo,
            String entryKey,
            Function<N, List<String>> listGetter
    ) {
        if (mapInfo.getDefaultValue() instanceof Map<?, ?> defMap && defMap.containsKey(entryKey)) {
            N defItem = (N) defMap.get(entryKey);
            if (defItem != null) {
                List<String> l = listGetter.apply(defItem);
                if (l != null) return new ArrayList<>(l);
            }
        }
        return new ArrayList<>();
    }

    private <N> List<String> getProjectedList(
            OptionInfo mapInfo,
            String entryKey,
            Function<N, List<String>> listGetter
    ) {
        Map<String, N> m = context.session().get(mapInfo);
        if (m != null && m.containsKey(entryKey)) {
            List<String> l = listGetter.apply(m.get(entryKey));
            return l != null ? new ArrayList<>(l) : new ArrayList<>();
        }
        return new ArrayList<>();
    }

    private void persistGuiMap(OptionInfo info, Map<String, ?> defaultMap, List<String> guiList, Class<?> valClass) {
        Map<String, String> stringMap = KeyValuePair.fromLines(guiList);
        Map<String, Object> convertedMap = new LinkedHashMap<>(stringMap.size());

        for (Map.Entry<String, String> entry : stringMap.entrySet()) {
            Object value = TypeCoercionUtils.coerce(entry.getValue(), valClass);
            if (value != null) {
                convertedMap.put(entry.getKey(), value);
            }
        }

        if ((info.type() != null && SortedMap.class.isAssignableFrom(info.type()))
                || (defaultMap instanceof SortedMap)) {
            context.session().set(info, new TreeMap<>(convertedMap));
        } else {
            context.session().set(info, convertedMap);
        }
    }

    private static String resolveInitialEntry(Class<?> valClass) {
        if (valClass == int.class || valClass == Integer.class || valClass == long.class || valClass == Long.class
                || valClass == byte.class || valClass == Byte.class || valClass == short.class || valClass == Short.class) {
            return "key: 0";
        }
        if (valClass == float.class || valClass == Float.class || valClass == double.class || valClass == Double.class) {
            return "key: 0.0";
        }
        if (valClass == boolean.class || valClass == Boolean.class) {
            return "key: true";
        }
        return "key: value";
    }
}
