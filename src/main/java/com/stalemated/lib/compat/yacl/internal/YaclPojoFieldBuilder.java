package com.stalemated.lib.compat.yacl.internal;

import com.stalemated.lib.compat.yacl.registry.YaclControllerRegistry;
import com.stalemated.lib.config.internal.CategoryNode;
import com.stalemated.lib.config.internal.ConfigNode;
import com.stalemated.lib.config.OptionInfo;
import com.stalemated.lib.util.internal.StringUtils;
import com.stalemated.lib.util.internal.ReflectionUtils;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
     * Builds a YACL option for one field of a POJO entry inside a map or list option.
     * Resolves the display title and description with the naming strategy of the context.
     *
     * @param context The shared YACL context.
     * @param containerInfo The option metadata for the parent container (Map or List).
     * @param entry The accessor of the entry inside the container.
     * @param defaultItem The default value of the entry, or null if the container defaults do not have it.
     * @param field The reflective field in the POJO.
     * @return The built {@link Option}.
     * @throws UnsupportedOperationException If no controller or text conversion can edit the field type.
     */
    public static Option<?> buildPojoFieldOption(
            YaclContext<?> context,
            OptionInfo containerInfo,
            ValueAccessor<Object> entry,
            Object defaultItem,
            Field field
    ) {
        String optionKey = containerInfo.getKey() + "." + field.getName();
        Object defaultFieldVal = defaultFieldValue(field, defaultItem, entry.get());
        OptionInfo fieldInfo = resolveFieldOptionInfo(containerInfo, field, defaultFieldVal);
        YaclOptionMetadata meta = context.metadata(
                context.namingStrategy().optionName(context.translationPrefix(), optionKey),
                context.optionDescription(optionKey, field),
                containerInfo,
                optionKey
        );
        field.setAccessible(true);
        ValueAccessor<Object> value = entry.property(
                field.getName(),
                item -> ReflectionUtils.readField(field, item),
                (item, model) -> ReflectionUtils.setFieldValue(field, item, fieldInfo.clampValue(model))
        );

        OptionAssembler assembler = context.assembler();
        Optional<TypeLens<Object, Object>> lens = assembler.lensFor(field.getType(), meta);
        if (lens.isPresent()) {
            return assembler.option(meta, fieldInfo, value, lens.get(), defaultFieldVal, null);
        }
        if (Collection.class.isAssignableFrom(field.getType())) {
            TypeLens<Object, String> commaSeparated = commaSeparatedLens(context, fieldInfo.getCollectionElementType(), optionKey);
            return assembler.option(meta, fieldInfo, value, commaSeparated, defaultFieldVal, null);
        }
        throw new UnsupportedOperationException("No YACL controller or adapter registered for type: "
                + field.getType().getName() + " on config option: " + optionKey);
    }

    private static Object defaultFieldValue(Field field, Object defaultItem, Object currentItem) {
        Object fromDefaults = defaultItem != null ? ReflectionUtils.getFieldValue(field, defaultItem) : null;
        return fromDefaults != null ? fromDefaults : ReflectionUtils.getFieldValue(field, currentItem);
    }

    private static OptionInfo resolveFieldOptionInfo(OptionInfo containerInfo, Field field, Object defaultFieldVal) {
        if (containerInfo.getElementTemplate() instanceof CategoryNode categoryNode) {
            ConfigNode child = categoryNode.getChild(field.getName());
            if (child instanceof OptionInfo childInfo) {
                return childInfo;
            }
        }
        return containerInfo.childTemplate(containerInfo.getKey() + "." + field.getName(), field, defaultFieldVal, field.getType(), field.getGenericType());
    }

    private static TypeLens<Object, String> commaSeparatedLens(YaclContext<?> context, Class<?> elementType, String optionKey) {
        TypeLens<Object, String> elementText = context.controllerRegistry().findTextLens(elementType).orElseThrow(() ->
                new UnsupportedOperationException("No text conversion registered for element type: "
                        + elementType.getName() + " on config option: " + optionKey));

        return new TypeLens<>(
                collection -> collection instanceof Collection<?> elements ? joinNonNull(elements, elementText) : "",
                text -> parseTokens(text, elementText),
                "",
                (info, opt) -> StringControllerBuilder.create(opt)
        );
    }

    private static String joinNonNull(Collection<?> elements, TypeLens<Object, String> elementText) {
        List<String> tokens = new ArrayList<>(elements.size());
        for (Object element : elements) {
            if (element != null) {
                tokens.add(elementText.guiOf(element, ""));
            }
        }
        return StringUtils.joinCommaSeparated(tokens);
    }

    private static List<Object> parseTokens(String text, TypeLens<Object, String> elementText) {
        List<String> tokens = StringUtils.parseCommaSeparated(text);
        List<Object> elements = new ArrayList<>(tokens.size());
        for (String token : tokens) {
            Object element = elementText.fromGui().apply(token);
            if (element != null) {
                elements.add(element);
            }
        }
        return elements;
    }
}
