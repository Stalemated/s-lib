package com.stalemated.lib.compat.yacl.factory;

import com.stalemated.lib.compat.yacl.registry.YaclControllerRegistry;
import com.stalemated.lib.compat.yacl.util.NestedAccessor;
import com.stalemated.lib.config.model.CategoryNode;
import com.stalemated.lib.config.model.ConfigNode;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.session.ConfigSession;
import com.stalemated.lib.util.StringUtils;
import com.stalemated.lib.util.TypeCoercionUtils;
import com.stalemated.lib.util.reflection.ReflectionUtils;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Internal factory collaborator responsible for reflectively building YACL options
 * corresponding to fields within nested POJO objects stored inside Map or List configurations.
 * <p>
 * Delegates controller construction and type adaptation to {@link YaclControllerRegistry}
 * and numeric constraint enforcement to {@link OptionInfo}.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class YaclPojoFieldBuilder {

    private YaclPojoFieldBuilder() {}

    /**
     * Builds a YACL option for a specific field of a POJO entry within a map option using a key.
     *
     * @param context The shared YACL context.
     * @param mapInfo The option metadata for the parent map.
     * @param entryKey The key of the entry within the map.
     * @param field The reflective field in the POJO.
     * @param sampleItem A sample or current instance of the POJO.
     * @param available Whether the option should be interactively editable.
     * @param <T> The configuration model type.
     * @return The built {@link Option}, or null if the field type is unsupported.
     */
    public static <T> Option<?> buildPojoFieldOption(
            YaclContext<T> context,
            OptionInfo mapInfo,
            String entryKey,
            Field field,
            Object sampleItem,
            boolean available
    ) {
        return buildPojoFieldOption(context, mapInfo, NestedAccessor.forMap(entryKey), field, sampleItem, available);
    }

    /**
     * Builds a YACL option for a specific field of a POJO entry within a list option using an index.
     *
     * @param context The shared YACL context.
     * @param listInfo The option metadata for the parent list.
     * @param index The zero-based index of the entry within the list.
     * @param field The reflective field in the POJO.
     * @param sampleItem A sample or current instance of the POJO.
     * @param available Whether the option should be interactively editable.
     * @param <T> The configuration model type.
     * @return The built {@link Option}, or null if the field type is unsupported.
     */
    public static <T> Option<?> buildPojoFieldOption(
            YaclContext<T> context,
            OptionInfo listInfo,
            int index,
            Field field,
            Object sampleItem,
            boolean available
    ) {
        return buildPojoFieldOption(context, listInfo, NestedAccessor.forList(index), field, sampleItem, available);
    }

    /**
     * Builds a YACL option for a specific field of a POJO entry with an explicit {@link NestedAccessor}.
     * Automatically resolves the localized display title and description using the naming strategy in the context.
     *
     * @param context The shared YACL context.
     * @param containerInfo The option metadata for the parent container (Map or List).
     * @param accessor The nested entry accessor strategy.
     * @param field The reflective field in the POJO.
     * @param sampleItem A sample or current instance of the POJO.
     * @param available Whether the option should be interactively editable.
     * @param <T> The configuration model type.
     * @return The built {@link Option}, or null if the field type is unsupported.
     */
    public static <T> Option<?> buildPojoFieldOption(
            YaclContext<T> context,
            OptionInfo containerInfo,
            NestedAccessor accessor,
            Field field,
            Object sampleItem,
            boolean available
    ) {
        Component name = context.namingStrategy().resolvePojoFieldName(context.modId(), containerInfo, field);
        OptionDescription desc = context.namingStrategy().resolvePojoFieldDescription(context.modId(), containerInfo, field, available);
        return buildPojoFieldOption(context, containerInfo, accessor, field, sampleItem, name, desc, available);
    }

    /**
     * Builds a YACL option for a specific field of a POJO entry with an explicit name, description, and {@link NestedAccessor}.
     *
     * @param context The shared YACL context.
     * @param containerInfo The option metadata for the parent container (Map or List).
     * @param accessor The nested entry accessor strategy.
     * @param field The reflective field in the POJO.
     * @param sampleItem A sample or current instance of the POJO.
     * @param name The display title component.
     * @param desc The description or tooltip.
     * @param available Whether the option should be interactively editable.
     * @param <T> The configuration model type.
     * @return The built {@link Option}, or null if the field type is unsupported.
     */
    public static <T> Option<?> buildPojoFieldOption(
            YaclContext<T> context,
            OptionInfo containerInfo,
            NestedAccessor accessor,
            Field field,
            Object sampleItem,
            Component name,
            OptionDescription desc,
            boolean available
    ) {
        Class<?> fieldType = field.getType();
        Object defaultFieldVal = resolveDefaultFieldValue(containerInfo, accessor, field, sampleItem);
        OptionInfo fieldInfo = resolveFieldOptionInfo(containerInfo, field, defaultFieldVal);

        // Direct controllers registered in YaclControllerRegistry (primitives, String, Color, Enum, custom)
        YaclControllerRegistry.ControllerFactory directFactory = context.controllerRegistry().findDirectController(fieldType);
        if (directFactory != null) {
            return createDirectFieldOption(context, containerInfo, fieldInfo, accessor, field, defaultFieldVal, name, desc, available, directFactory);
        }

        // Adapted types registered in YaclControllerRegistry (TextColor, ResourceLocation, UUID, Pattern, custom)
        YaclControllerRegistry.AdaptedType adapted = context.controllerRegistry().findAdaptedType(fieldType);
        if (adapted != null) {
            return createAdaptedFieldOption(context, containerInfo, fieldInfo, accessor, field, defaultFieldVal, name, desc, available, adapted);
        }

        // Inline comma-separated list options for List<E> fields inside nested POJOs
        if (List.class.isAssignableFrom(fieldType)) {
            return createInlineCommaSeparatedListOption(context, containerInfo, fieldInfo, accessor, field, defaultFieldVal, name, desc, available);
        }

        return null;
    }

    private static Object resolveDefaultFieldValue(OptionInfo containerInfo, NestedAccessor accessor, Field field, Object sampleItem) {
        Object initialFieldVal = ReflectionUtils.getFieldValueSafe(field, sampleItem);
        Object defItem = accessor.getDefaultItem(containerInfo);
        if (defItem != null) {
            Object defVal = ReflectionUtils.getFieldValueSafe(field, defItem);
            if (defVal != null) return defVal;
        }
        return initialFieldVal;
    }

    private static OptionInfo resolveFieldOptionInfo(OptionInfo containerInfo, Field field, Object defaultFieldVal) {
        if (containerInfo.getElementTemplate() instanceof CategoryNode categoryNode) {
            ConfigNode child = categoryNode.getChild(field.getName());
            if (child instanceof OptionInfo childInfo) {
                return childInfo;
            }
        }
        return new OptionInfo(
                containerInfo.getKey() + "." + field.getName(),
                field,
                Collections.emptyList(),
                containerInfo.getSyncMode(),
                defaultFieldVal,
                field.getType(),
                field.getGenericType(),
                true,
                null
        );
    }

    private static <T, V> Option<V> createDirectFieldOption(
            YaclContext<T> context,
            OptionInfo containerInfo,
            OptionInfo fieldInfo,
            NestedAccessor accessor,
            Field field,
            Object defaultVal,
            Component name,
            OptionDescription desc,
            boolean available,
            YaclControllerRegistry.ControllerFactory<V> controllerFactory
    ) {
        V resolvedDefault = defaultVal != null
                ? (V) defaultVal
                : context.controllerRegistry().resolveInitialValue(field.getType());

        Option.Builder<V> builder = Option.<V>createBuilder()
                .name(name)
                .description(desc)
                .available(available);

        return context.synchronizer().buildSyncedOption(
                containerInfo,
                builder,
                resolvedDefault,
                () -> {
                    Object item = accessor.getItem(context.session(), containerInfo);
                    if (item != null) {
                        Object val = ReflectionUtils.getFieldValueSafe(field, item);
                        if (val != null) return (V) val;
                    }
                    return resolvedDefault;
                },
                val -> {
                    Object clamped = fieldInfo.clampValue(val);
                    accessor.updateItem(context.session(), containerInfo, item ->
                            ReflectionUtils.setFieldValueSafe(field, item, clamped)
                    );
                },
                b -> b.controller(opt -> controllerFactory.create(fieldInfo, opt))
        );
    }

    private static <T, M, G> Option<G> createAdaptedFieldOption(
            YaclContext<T> context,
            OptionInfo containerInfo,
            OptionInfo fieldInfo,
            NestedAccessor accessor,
            Field field,
            Object defaultVal,
            Component name,
            OptionDescription desc,
            boolean available,
            YaclControllerRegistry.AdaptedType<M, G> adapted
    ) {
        M modelDefault = defaultVal != null ? (M) defaultVal : null;
        G guiDefault = modelDefault != null ? adapted.toGui().apply(modelDefault) : adapted.fallbackGuiDefault();

        Option.Builder<G> builder = Option.<G>createBuilder()
                .name(name)
                .description(desc)
                .available(available);

        ConfigSession<T> session = context.session();
        return context.synchronizer().buildSyncedOption(
                containerInfo,
                builder,
                guiDefault,
                () -> {
                    Object item = accessor.getItem(session, containerInfo);
                    if (item != null) {
                        Object val = ReflectionUtils.getFieldValueSafe(field, item);
                        if (val != null) {
                            return adapted.toGui().apply((M) val);
                        }
                    }
                    return guiDefault;
                },
                guiVal -> {
                    M modelVal = adapted.fromGui().apply(guiVal);
                    if (modelVal != null) {
                        accessor.updateItem(session, containerInfo, item ->
                                ReflectionUtils.setFieldValueSafe(field, item, modelVal)
                        );
                    }
                },
                b -> b.controller(opt -> adapted.guiControllerFactory().create(fieldInfo, opt))
        );
    }

    private static <T> Option<String> createInlineCommaSeparatedListOption(
            YaclContext<T> context,
            OptionInfo containerInfo,
            OptionInfo fieldInfo,
            NestedAccessor accessor,
            Field field,
            Object defaultVal,
            Component name,
            OptionDescription desc,
            boolean available
    ) {
        Class<?> elemType = fieldInfo.getCollectionElementType();
        YaclControllerRegistry.AdaptedType adapted = elemType != null
                ? context.controllerRegistry().findAdaptedType(elemType)
                : null;

        List<?> defVal = defaultVal instanceof List<?> l ? l : Collections.emptyList();
        String defStr = formatListToString(defVal, adapted);

        Option.Builder<String> builder = Option.<String>createBuilder()
                .name(name)
                .description(desc)
                .available(available);

        return context.synchronizer().buildSyncedOption(
                containerInfo,
                builder,
                defStr,
                () -> {
                    Object item = accessor.getItem(context.session(), containerInfo);
                    if (item != null) {
                        Object val = ReflectionUtils.getFieldValueSafe(field, item);
                        if (val instanceof List<?> l) {
                            return formatListToString(l, adapted);
                        }
                    }
                    return defStr;
                },
                val -> {
                    List<?> parsedList = parseStringToList(val, elemType, adapted);
                    accessor.updateItem(context.session(), containerInfo, item ->
                            ReflectionUtils.setFieldValueSafe(field, item, parsedList)
                    );
                },
                b -> b.controller(StringControllerBuilder::create)
        );
    }

    private static String formatListToString(List<?> list, YaclControllerRegistry.AdaptedType adapted) {
        if (list == null || list.isEmpty()) return "";
        if (adapted != null) {
            List<String> guiList = new ArrayList<>(list.size());
            for (Object obj : list) {
                if (obj != null) {
                    guiList.add(String.valueOf(adapted.toGui().apply(obj)));
                }
            }
            return StringUtils.joinCommaSeparated(guiList);
        }
        return StringUtils.joinCommaSeparated(list);
    }

    private static List<?> parseStringToList(String val, Class<?> elemType, YaclControllerRegistry.AdaptedType adapted) {
        List<String> rawTokens = StringUtils.parseCommaSeparated(val);
        if (adapted != null) {
            List<Object> adaptedList = new ArrayList<>(rawTokens.size());
            Object fallback = adapted.fallbackGuiDefault();
            boolean isStringGui = fallback == null || fallback instanceof String;

            for (String token : rawTokens) {
                Object guiVal = isStringGui ? token : TypeCoercionUtils.coerce(token, fallback.getClass());
                if (guiVal != null) {
                    Object modelObj = adapted.fromGui().apply(guiVal);
                    if (modelObj != null) {
                        adaptedList.add(modelObj);
                    }
                }
            }
            return adaptedList;
        }

        if (elemType == null || elemType == String.class) {
            return rawTokens;
        }

        List<Object> converted = new ArrayList<>(rawTokens.size());
        for (String token : rawTokens) {
            Object obj = TypeCoercionUtils.coerce(token, elemType);
            if (obj != null) {
                converted.add(obj);
            }
        }
        return converted;
    }
}
