package com.stalemated.lib.compat.yacl.factory;

import com.stalemated.lib.compat.yacl.registry.YaclOptionMetadata;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.util.TypeCoercionUtils;
import com.stalemated.lib.util.codec.KeyValuePair;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;

import java.util.*;
import java.util.function.Consumer;

/**
 * Metadata-driven factory that produces YACL {@link ListOption} widgets for {@code Map<String, ?>} config options.
 * Allows interactive addition (+), modification, and removal (-) of key-value pairs formatted as {@code "key: value"} strings.
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

        builder.binding(
                defaultGui,
                () -> KeyValuePair.toLines(context.session().get(info)),
                guiList -> persistGuiMap(info, defaultMap, guiList, valClass)
        );

        builder.controller(StringControllerBuilder::create);
        builder.initial(resolveInitialEntry(valClass));

        if (customizer != null) {
            customizer.accept(builder);
        }

        return builder.build();
    }

    private void persistGuiMap(OptionInfo info, Map<String, ?> defaultMap, List<String> guiList, Class<?> valClass) {
        Map<String, String> stringMap = KeyValuePair.fromLines(guiList);
        Map<String, Object> convertedMap = new LinkedHashMap<>(stringMap.size());

        for (Map.Entry<String, String> entry : stringMap.entrySet()) {
            convertedMap.put(entry.getKey(), TypeCoercionUtils.coerce(entry.getValue(), valClass));
        }

        if ((info.type() != null && SortedMap.class.isAssignableFrom(info.type()))
                || (defaultMap instanceof SortedMap)) {
            context.session().set(info, new TreeMap<>(convertedMap));
        } else {
            context.session().set(info, new LinkedHashMap<>(convertedMap));
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
