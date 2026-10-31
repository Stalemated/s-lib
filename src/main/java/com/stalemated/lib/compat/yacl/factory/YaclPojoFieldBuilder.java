package com.stalemated.lib.compat.yacl.factory;

import com.stalemated.lib.compat.yacl.binding.OptionAssembler;
import com.stalemated.lib.compat.yacl.binding.ValueAccessor;
import com.stalemated.lib.compat.yacl.registry.TypeLens;
import com.stalemated.lib.compat.yacl.registry.YaclControllerRegistry;
import com.stalemated.lib.compat.yacl.registry.YaclOptionMetadata;
import com.stalemated.lib.compat.yacl.util.NestedAccessor;
import com.stalemated.lib.compat.yacl.util.YaclSessionHelper;
import com.stalemated.lib.config.model.CategoryNode;
import com.stalemated.lib.config.model.ConfigNode;
import com.stalemated.lib.config.model.OptionInfo;
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
import java.util.Optional;
import java.util.function.Function;

/**
 * Internal factory collaborator responsible for reflectively building YACL options
 * corresponding to fields within nested POJO objects stored inside Map or List configurations.
 * <p>
 * Delegates controller construction and type adaptation to {@link YaclControllerRegistry}
 * and numeric constraint enforcement to {@link OptionInfo}.
 */
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
        return buildPojoFieldOption(context, mapInfo, NestedAccessor.forMap(entryKey, YaclSessionHelper.defaultMapOf(mapInfo)), field, sampleItem, available);
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
        return buildPojoFieldOption(context, listInfo, NestedAccessor.forList(index, YaclSessionHelper.defaultListOf(listInfo)), field, sampleItem, available);
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
        Component name = context.namingStrategy().resolvePojoFieldName(context.translationPrefix(), containerInfo, field);
        OptionDescription desc = context.namingStrategy().resolvePojoFieldDescription(context.translationPrefix(), containerInfo, field, available);
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
        Object defaultFieldVal = resolveDefaultFieldValue(accessor, field, sampleItem);
        OptionInfo fieldInfo = resolveFieldOptionInfo(containerInfo, field, defaultFieldVal);
        YaclOptionMetadata meta = context.metadata(name, desc, available, containerInfo.getKey() + "." + field.getName());
        ValueAccessor<Object> value = ValueAccessor.entryProperty(
                context.session(),
                containerInfo,
                accessor,
                item -> ReflectionUtils.getFieldValue(field, item),
                (item, model) -> ReflectionUtils.setFieldValue(field, item, fieldInfo.clampValue(model))
        );

        OptionAssembler assembler = context.assembler();
        Optional<TypeLens<Object, Object>> lens = assembler.lensFor(field.getType(), meta);
        if (lens.isPresent()) {
            return assembler.option(meta, fieldInfo, value, lens.get(), defaultFieldVal, null);
        }
        if (List.class.isAssignableFrom(field.getType())) {
            TypeLens<Object, String> commaSeparated = commaSeparatedLens(context, fieldInfo.getCollectionElementType());
            return assembler.option(meta, fieldInfo, value, commaSeparated, defaultFieldVal, null);
        }
        return null;
    }

    private static Object resolveDefaultFieldValue(NestedAccessor accessor, Field field, Object sampleItem) {
        Object initialFieldVal = ReflectionUtils.getFieldValue(field, sampleItem);
        Object defItem = accessor.getDefaultItem();
        if (defItem != null) {
            Object defVal = ReflectionUtils.getFieldValue(field, defItem);
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

    private static TypeLens<Object, String> commaSeparatedLens(YaclContext<?> context, Class<?> elemType) {
        Optional<TypeLens<Object, Object>> elementLens = context.controllerRegistry().findLens(elemType);
        Function<Object, String> elementToText = element -> String.valueOf(elementLens.map(lens -> lens.toGui().apply(element)).orElse(element));
        Function<String, Object> textToElement = elementLens
                .filter(lens -> lens.fallback() instanceof String)
                .<Function<String, Object>>map(lens -> token -> lens.fromGui().apply(token))
                .orElse(token -> TypeCoercionUtils.coerce(token, elemType));

        return new TypeLens<>(
                list -> list instanceof List<?> elements ? joinNonNull(elements, elementToText) : "",
                text -> parseTokens(text, textToElement),
                "",
                (info, opt) -> StringControllerBuilder.create(opt)
        );
    }

    private static String joinNonNull(List<?> elements, Function<Object, String> elementToText) {
        List<String> tokens = new ArrayList<>(elements.size());
        for (Object element : elements) {
            if (element != null) {
                tokens.add(elementToText.apply(element));
            }
        }
        return StringUtils.joinCommaSeparated(tokens);
    }

    private static List<Object> parseTokens(String text, Function<String, Object> textToElement) {
        List<String> tokens = StringUtils.parseCommaSeparated(text);
        List<Object> elements = new ArrayList<>(tokens.size());
        for (String token : tokens) {
            Object element = textToElement.apply(token);
            if (element != null) {
                elements.add(element);
            }
        }
        return elements;
    }
}
