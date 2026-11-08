package com.stalemated.lib.compat.yacl.internal;

import com.stalemated.lib.compat.yacl.builder.NestListBinding;
import com.stalemated.lib.config.OptionInfo;
import com.stalemated.lib.util.internal.KeyValuePair;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Consumer;

import static com.stalemated.lib.SLib.LOGGER;

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
        Class<?> valueType = info.getMapValueType();
        TypeLens<Object, String> valueText = context.controllerRegistry().findTextLens(valueType).orElseThrow(() ->
                new UnsupportedOperationException("No text conversion registered for map value type: "
                        + valueType.getName() + " on config option: " + info.getKey()));

        OptionInfo valueInfo = info.getElementTemplate() instanceof OptionInfo template ? template : info;
        OptionAssembler assembler = context.assembler();
        String initialEntry = new KeyValuePair("key", assembler.initialElement(valueInfo, valueType, valueText)).format();
        Map<String, ?> defaultMap = (Map<String, ?>) info.defaultCopy();

        ValueAccessor<List<String>> lines = ValueAccessor.of(
                SyncSlot.of(info.getKey()),
                () -> toLines(context.session().get(info), valueText),
                guiList -> persistGuiMap(info, guiList, valueText)
        );
        ListOption<String> pairs = assembler.list(meta, info, lines, TEXT, toLines(defaultMap, valueText), initialEntry, customizer);
        assembler.applyEnabledIf(info, pairs);
        return pairs;
    }

    /**
     * Builds a list of YACL {@link ListOption} widgets projecting a list property
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

        Map<String, N> defaultMap = (Map<String, N>) ValueAccessor.defaultMapOf(mapInfo);
        Map<String, N> currentMap = context.session().get(mapInfo);
        if (currentMap == null) {
            currentMap = defaultMap;
        }
        Set<String> keys = new LinkedHashSet<>(binding.keys().get());
        keys.addAll(currentMap.keySet());
        if (keys.isEmpty()) {
            return Collections.emptyList();
        }

        OptionAssembler assembler = context.assembler();
        Component desc = binding.description() != null ? binding.description() : context.optionDescription(mapInfo.getKey(), mapInfo.field());
        List<ListOption<String>> options = new ArrayList<>(keys.size());
        TypeLens<Object, String> elementText = context.controllerRegistry().findTextLens(binding.elementType()).orElseThrow(() ->
                new UnsupportedOperationException("No text conversion registered for list element type: "
                        + binding.elementType().getName() + " on config option: " + mapInfo.getKey()));
        String initialElement = elementText.guiOf(context.controllerRegistry().resolveInitialValue(binding.elementType()), elementText.fallback());

        for (String entryKey : keys) {
            YaclOptionMetadata meta = context.metadata(binding.entryName().apply(entryKey), desc, mapInfo, mapInfo.getKey());
            ValueAccessor<List<Object>> projected = ValueAccessor.mapEntry(context.session(), mapInfo, entryKey).property(
                    null,
                    item -> copyOf(binding.getter().apply((N) item)),
                    (item, list) -> binding.setter().accept((N) item, new ArrayList<>(list)),
                    binding.newEntry()
            );
            List<Object> defaultList = copyOf(defaultMap.containsKey(entryKey) ? binding.getter().apply(defaultMap.get(entryKey)) : null);
            options.add(assembler.list(meta, mapInfo, projected, elementText, defaultList, initialElement, null));
        }

        return options;
    }

    private static List<Object> copyOf(List<?> list) {
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    private static KeyValuePair parseOrWarn(String line) {
        try {
            return KeyValuePair.parse(line);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Ignoring map entry: {}", e.getMessage());
            return null;
        }
    }

    private static List<String> toLines(Map<String, ?> map, TypeLens<Object, String> valueText) {
        return KeyValuePair.toLines(map, value -> valueText.guiOf(value, ""));
    }

    private void persistGuiMap(OptionInfo info, List<String> guiList, TypeLens<Object, String> valueText) {
        Map<String, Object> convertedMap = new LinkedHashMap<>(guiList.size());

        for (String line : guiList) {
            KeyValuePair pair = parseOrWarn(line);
            if (pair == null) continue;

            Object value = valueText.fromGui().apply(pair.value());
            if (value != null) {
                convertedMap.put(pair.key(), value);
            }
        }

        if (info.isSortedMap()) {
            context.session().set(info, new TreeMap<>(convertedMap));
        } else {
            context.session().set(info, convertedMap);
        }
    }
}
