package com.stalemated.lib.compat.yacl.factory;

import com.stalemated.lib.compat.yacl.binding.OptionAssembler;
import com.stalemated.lib.compat.yacl.binding.ValueAccessor;
import com.stalemated.lib.compat.yacl.builder.NestListBinding;
import com.stalemated.lib.compat.yacl.registry.TypeLens;
import com.stalemated.lib.compat.yacl.registry.YaclOptionMetadata;
import com.stalemated.lib.compat.yacl.util.NestedAccessor;
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

/**
 * Metadata-driven factory that produces YACL {@link ListOption} widgets for {@code Map<String, ?>} config options,
 * including both key-value pair lists and projected inner list properties of nested POJO maps.
 *
 * @param <T> The config model type.
 * @param context The shared YACL UI construction context.
 */
public record YaclMapOptionFactory<T>(YaclContext<T> context) {

    private static final TypeLens<String, String> TEXT = TypeLens.identity("", (info, opt) -> StringControllerBuilder.create(opt));

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
        Map<String, ?> defaultMap = (Map<String, ?>) info.defaultCopy();

        ValueAccessor<List<String>> lines = ValueAccessor.of(
                info.getKey(),
                () -> KeyValuePair.toLines(context.session().get(info)),
                guiList -> persistGuiMap(info, guiList, valClass)
        );
        return context.assembler().list(meta, info, lines, TEXT, KeyValuePair.toLines(defaultMap), resolveInitialEntry(valClass), customizer);
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
        Map<String, N> defaultMap = (Map<String, N>) YaclSessionHelper.defaultMapOf(mapInfo);
        Map<String, N> currentMap = context.session().get(mapInfo);
        if (currentMap == null) {
            currentMap = defaultMap;
        }
        if (currentMap.isEmpty()) {
            return Collections.emptyList();
        }

        OptionAssembler assembler = context.assembler();
        OptionDescription desc = context.namingStrategy().resolveDescription(context.translationPrefix(), mapInfo, available);
        List<ListOption<String>> options = new ArrayList<>(currentMap.size());

        for (String entryKey : currentMap.keySet()) {
            YaclOptionMetadata meta = context.metadata(Component.literal(entryKey), desc, available, mapInfo.getKey());
            ValueAccessor<List<String>> projected = ValueAccessor.entryProperty(
                    context.session(),
                    mapInfo,
                    NestedAccessor.forMap(entryKey, defaultMap),
                    item -> copyOf(binding.getter().apply((N) item)),
                    (item, list) -> binding.setter().accept((N) item, new ArrayList<>(list))
            );
            List<String> defaultList = copyOf(defaultMap.containsKey(entryKey) ? binding.getter().apply(defaultMap.get(entryKey)) : null);
            options.add(assembler.list(meta, mapInfo, projected, TEXT, defaultList, "", null));
        }

        return options;
    }

    private static List<String> copyOf(List<String> list) {
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    private void persistGuiMap(OptionInfo info, List<String> guiList, Class<?> valClass) {
        Map<String, String> stringMap = KeyValuePair.fromLines(guiList);
        Map<String, Object> convertedMap = new LinkedHashMap<>(stringMap.size());

        for (Map.Entry<String, String> entry : stringMap.entrySet()) {
            Object value = TypeCoercionUtils.coerce(entry.getValue(), valClass);
            if (value != null) {
                convertedMap.put(entry.getKey(), value);
            }
        }

        if (info.isSortedMap()) {
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
