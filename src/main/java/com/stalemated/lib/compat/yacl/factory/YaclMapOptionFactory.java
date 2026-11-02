package com.stalemated.lib.compat.yacl.factory;

import com.stalemated.lib.compat.yacl.binding.OptionAssembler;
import com.stalemated.lib.compat.yacl.binding.ValueAccessor;
import com.stalemated.lib.compat.yacl.builder.NestListBinding;
import com.stalemated.lib.compat.yacl.registry.TypeLens;
import com.stalemated.lib.compat.yacl.registry.YaclOptionMetadata;
import com.stalemated.lib.compat.yacl.util.NestedAccessor;
import com.stalemated.lib.compat.yacl.util.YaclSessionHelper;
import com.stalemated.lib.config.model.OptionInfo;
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
        Class<?> valueType = info.getMapValueType();
        TypeLens<Object, String> valueText = context.controllerRegistry().findTextLens(valueType).orElseThrow(() ->
                new UnsupportedOperationException("No text conversion registered for map value type: "
                        + valueType.getName() + " on config option: " + info.getKey()));

        OptionInfo valueInfo = info.getElementTemplate() instanceof OptionInfo template ? template : info;
        OptionAssembler assembler = context.assembler();
        String initialEntry = new KeyValuePair("key", assembler.initialElement(valueInfo, valueType, valueText)).format();
        Map<String, ?> defaultMap = (Map<String, ?>) info.defaultCopy();

        ValueAccessor<List<String>> lines = ValueAccessor.of(
                info.getKey(),
                () -> toLines(context.session().get(info), valueText),
                guiList -> persistGuiMap(info, guiList, valueText)
        );
        return assembler.list(meta, info, lines, TEXT, toLines(defaultMap, valueText), initialEntry, customizer);
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

    private static List<String> toLines(Map<String, ?> map, TypeLens<Object, String> valueText) {
        return KeyValuePair.toLines(map, value -> valueText.guiOf(value, ""));
    }

    private void persistGuiMap(OptionInfo info, List<String> guiList, TypeLens<Object, String> valueText) {
        Map<String, String> stringMap = KeyValuePair.fromLines(guiList);
        Map<String, Object> convertedMap = new LinkedHashMap<>(stringMap.size());

        for (Map.Entry<String, String> entry : stringMap.entrySet()) {
            Object value = valueText.fromGui().apply(entry.getValue());
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
}
