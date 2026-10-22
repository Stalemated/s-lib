package com.stalemated.lib.compat.yacl.factory;

import com.stalemated.lib.compat.yacl.controller.builder.SimpleEnumDropdownControllerBuilder;
import com.stalemated.lib.compat.yacl.util.YaclSessionHelper;
import com.stalemated.lib.config.annotation.RangeDouble;
import com.stalemated.lib.config.annotation.RangeFloat;
import com.stalemated.lib.config.annotation.RangeInt;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.session.ConfigSession;
import com.stalemated.lib.util.StringUtils;
import com.stalemated.lib.util.TypeCoercionUtils;
import com.stalemated.lib.util.math.MathUtils;
import com.stalemated.lib.util.reflection.ReflectionUtils;
import dev.isxander.yacl3.api.Binding;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Internal factory collaborator responsible for reflectively building YACL options
 * corresponding to fields within nested POJO objects stored inside {@code Map<String, ?>} configurations.
 */
public final class YaclPojoFieldBuilder {

    private YaclPojoFieldBuilder() {}

    /**
     * Builds a YACL option for a specific field of a POJO entry within a map option.
     * Automatically resolves the localized display title and description using the naming strategy in the context.
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
        Component name = context.namingStrategy().resolvePojoFieldName(context.modId(), mapInfo, field);
        OptionDescription desc = context.namingStrategy().resolvePojoFieldDescription(context.modId(), mapInfo, field, available);
        return buildPojoFieldOption(context, mapInfo, entryKey, field, sampleItem, name, desc, available);
    }

    /**
     * Builds a YACL option for a specific field of a POJO entry with an explicitly provided name and description.
     *
     * @param context The shared YACL context.
     * @param mapInfo The option metadata for the parent map.
     * @param entryKey The key of the entry within the map.
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
            OptionInfo mapInfo,
            String entryKey,
            Field field,
            Object sampleItem,
            Component name,
            OptionDescription desc,
            boolean available
    ) {
        Class<?> fieldType = field.getType();
        Object defaultFieldVal = resolveDefaultFieldValue(mapInfo, entryKey, field, sampleItem);

        if (fieldType == boolean.class || fieldType == Boolean.class) {
            return createBooleanOption(context.session(), mapInfo, entryKey, field, defaultFieldVal, name, desc, available);
        }
        if (fieldType == int.class || fieldType == Integer.class) {
            return createIntegerOption(context.session(), mapInfo, entryKey, field, defaultFieldVal, name, desc, available);
        }
        if (fieldType == long.class || fieldType == Long.class) {
            return createLongOption(context.session(), mapInfo, entryKey, field, defaultFieldVal, name, desc, available);
        }
        if (fieldType == float.class || fieldType == Float.class) {
            return createFloatOption(context.session(), mapInfo, entryKey, field, defaultFieldVal, name, desc, available);
        }
        if (fieldType == double.class || fieldType == Double.class) {
            return createDoubleOption(context.session(), mapInfo, entryKey, field, defaultFieldVal, name, desc, available);
        }
        if (fieldType == String.class) {
            return createStringOption(context.session(), mapInfo, entryKey, field, defaultFieldVal, name, desc, available);
        }
        if (fieldType.isEnum()) {
            return createEnumOption(context.session(), mapInfo, entryKey, field, defaultFieldVal, name, desc, available);
        }
        if (fieldType == Color.class) {
            return createColorOption(context.session(), mapInfo, entryKey, field, defaultFieldVal, name, desc, available);
        }
        if (List.class.isAssignableFrom(fieldType)) {
            return createInlineCommaSeparatedListOption(context.session(), mapInfo, entryKey, field, defaultFieldVal, name, desc, available);
        }
        return null;
    }

    private static Object resolveDefaultFieldValue(OptionInfo mapInfo, String entryKey, Field field, Object sampleItem) {
        Object initialFieldVal = ReflectionUtils.getFieldValueSafe(field, sampleItem);

        if (mapInfo.getDefaultValue() instanceof Map<?, ?> defMap && defMap.containsKey(entryKey)) {
            Object defItem = defMap.get(entryKey);
            if (defItem != null) {
                Object defVal = ReflectionUtils.getFieldValueSafe(field, defItem);
                if (defVal != null) return defVal;
            }
        }
        return initialFieldVal;
    }

    private static <T, V> Binding<V> createFieldBinding(
            ConfigSession<T> session,
            OptionInfo mapInfo,
            String entryKey,
            Field field,
            V defaultValue,
            Function<Object, V> valueExtractor,
            UnaryOperator<V> valueTransformer
    ) {
        return Binding.generic(
                defaultValue,
                () -> {
                    Map<String, ?> m = session.get(mapInfo);
                    if (m != null && m.get(entryKey) != null) {
                        Object val = ReflectionUtils.getFieldValueSafe(field, m.get(entryKey));
                        V extracted = valueExtractor.apply(val);
                        if (extracted != null) return extracted;
                    }
                    return defaultValue;
                },
                val -> {
                    V finalVal = valueTransformer != null ? valueTransformer.apply(val) : val;
                    YaclSessionHelper.updateNestedMap(session, mapInfo, entryKey, item ->
                            ReflectionUtils.setFieldValueSafe(field, item, finalVal)
                    );
                }
        );
    }

    private static <T> Option<Boolean> createBooleanOption(
            ConfigSession<T> session,
            OptionInfo mapInfo,
            String entryKey,
            Field field,
            Object defaultVal,
            Component name,
            OptionDescription desc,
            boolean available
    ) {
        boolean def = defaultVal instanceof Boolean b ? b : false;
        return Option.<Boolean>createBuilder()
                .name(name)
                .description(desc)
                .available(available)
                .binding(createFieldBinding(session, mapInfo, entryKey, field, def, val -> val instanceof Boolean b ? b : null, null))
                .controller(TickBoxControllerBuilder::create)
                .build();
    }

    private static <T> Option<Integer> createIntegerOption(
            ConfigSession<T> session,
            OptionInfo mapInfo,
            String entryKey,
            Field field,
            Object defaultVal,
            Component name,
            OptionDescription desc,
            boolean available
    ) {
        int def = defaultVal instanceof Integer i ? i : 0;
        RangeInt range = field.getAnnotation(RangeInt.class);
        UnaryOperator<Integer> clamp = range != null ? val -> MathUtils.clamp(val, range.min(), range.max()) : null;

        Option.Builder<Integer> builder = Option.<Integer>createBuilder()
                .name(name)
                .description(desc)
                .available(available)
                .binding(createFieldBinding(session, mapInfo, entryKey, field, def, val -> val instanceof Integer i ? i : null, clamp));

        if (range != null) {
            builder.controller(opt -> IntegerSliderControllerBuilder.create(opt)
                    .range(range.min(), range.max())
                    .step(Math.max(1, range.step())));
        } else {
            builder.controller(IntegerFieldControllerBuilder::create);
        }
        return builder.build();
    }

    private static <T> Option<Long> createLongOption(
            ConfigSession<T> session,
            OptionInfo mapInfo,
            String entryKey,
            Field field,
            Object defaultVal,
            Component name,
            OptionDescription desc,
            boolean available
    ) {
        long def = defaultVal instanceof Number num ? num.longValue() : 0L;
        return Option.<Long>createBuilder()
                .name(name)
                .description(desc)
                .available(available)
                .binding(createFieldBinding(session, mapInfo, entryKey, field, def, val -> val instanceof Number num ? num.longValue() : null, null))
                .controller(LongFieldControllerBuilder::create)
                .build();
    }

    private static <T> Option<Float> createFloatOption(
            ConfigSession<T> session,
            OptionInfo mapInfo,
            String entryKey,
            Field field,
            Object defaultVal,
            Component name,
            OptionDescription desc,
            boolean available
    ) {
        float def = defaultVal instanceof Number num ? num.floatValue() : 0.0f;
        RangeFloat range = field.getAnnotation(RangeFloat.class);
        UnaryOperator<Float> clamp = range != null
                ? val -> MathUtils.roundFloat(MathUtils.clamp(val, range.min(), range.max()))
                : null;

        Option.Builder<Float> builder = Option.<Float>createBuilder()
                .name(name)
                .description(desc)
                .available(available)
                .binding(createFieldBinding(session, mapInfo, entryKey, field, def, val -> val instanceof Float f ? f : null, clamp));

        if (range != null) {
            builder.controller(opt -> FloatSliderControllerBuilder.create(opt)
                    .range(range.min(), range.max())
                    .step(range.step() > 0 ? range.step() : 0.1f));
        } else {
            builder.controller(FloatFieldControllerBuilder::create);
        }
        return builder.build();
    }

    private static <T> Option<Double> createDoubleOption(
            ConfigSession<T> session,
            OptionInfo mapInfo,
            String entryKey,
            Field field,
            Object defaultVal,
            Component name,
            OptionDescription desc,
            boolean available
    ) {
        double def = defaultVal instanceof Number num ? num.doubleValue() : 0.0;
        RangeDouble range = field.getAnnotation(RangeDouble.class);
        UnaryOperator<Double> clamp = range != null
                ? val -> MathUtils.roundDouble(MathUtils.clamp(val, range.min(), range.max()))
                : null;

        Option.Builder<Double> builder = Option.<Double>createBuilder()
                .name(name)
                .description(desc)
                .available(available)
                .binding(createFieldBinding(session, mapInfo, entryKey, field, def, val -> val instanceof Double d ? d : null, clamp));

        if (range != null) {
            builder.controller(opt -> DoubleSliderControllerBuilder.create(opt)
                    .range(range.min(), range.max())
                    .step(range.step() > 0 ? range.step() : 0.1));
        } else {
            builder.controller(DoubleFieldControllerBuilder::create);
        }
        return builder.build();
    }

    private static <T> Option<String> createStringOption(
            ConfigSession<T> session,
            OptionInfo mapInfo,
            String entryKey,
            Field field,
            Object defaultVal,
            Component name,
            OptionDescription desc,
            boolean available
    ) {
        String def = defaultVal instanceof String s ? s : "";
        return Option.<String>createBuilder()
                .name(name)
                .description(desc)
                .available(available)
                .binding(createFieldBinding(session, mapInfo, entryKey, field, def, val -> val instanceof String s ? s : null, null))
                .controller(StringControllerBuilder::create)
                .build();
    }

    private static <T> Option<?> createEnumOption(
            ConfigSession<T> session,
            OptionInfo mapInfo,
            String entryKey,
            Field field,
            Object defaultVal,
            Component name,
            OptionDescription desc,
            boolean available
    ) {
        return createEnumOptionHelper(session, mapInfo, entryKey, field, defaultVal, name, desc, available);
    }

    @SuppressWarnings("unchecked")
    private static <T, E extends Enum<E>> Option<E> createEnumOptionHelper(
            ConfigSession<T> session,
            OptionInfo mapInfo,
            String entryKey,
            Field field,
            Object defaultVal,
            Component name,
            OptionDescription desc,
            boolean available
    ) {
        Class<?> rawType = field.getType();
        E resolvedDefault = defaultVal != null
                ? (E) defaultVal
                : (E) TypeCoercionUtils.resolveDefaultInitial(rawType);

        Option.Builder<E> b = Option.createBuilder();
        b.name(name).description(desc).available(available);
        b.binding(
                resolvedDefault,
                () -> {
                    Map<String, ?> m = session.get(mapInfo);
                    if (m != null && m.get(entryKey) != null) {
                        Object val = ReflectionUtils.getFieldValueSafe(field, m.get(entryKey));
                        if (rawType.isInstance(val)) return (E) val;
                    }
                    return resolvedDefault;
                },
                val -> YaclSessionHelper.updateNestedMap(session, mapInfo, entryKey, item ->
                        ReflectionUtils.setFieldValueSafe(field, item, val)
                )
        );
        b.controller(SimpleEnumDropdownControllerBuilder::create);
        return b.build();
    }

    private static <T> Option<Color> createColorOption(
            ConfigSession<T> session,
            OptionInfo mapInfo,
            String entryKey,
            Field field,
            Object defaultVal,
            Component name,
            OptionDescription desc,
            boolean available
    ) {
        Color def = defaultVal instanceof Color c ? c : Color.WHITE;
        return Option.<Color>createBuilder()
                .name(name)
                .description(desc)
                .available(available)
                .binding(createFieldBinding(session, mapInfo, entryKey, field, def, val -> val instanceof Color c ? c : null, null))
                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true))
                .build();
    }

    private static <T> Option<String> createInlineCommaSeparatedListOption(
            ConfigSession<T> session,
            OptionInfo mapInfo,
            String entryKey,
            Field field,
            Object defaultVal,
            Component name,
            OptionDescription desc,
            boolean available
    ) {
        List<?> defVal = defaultVal instanceof List<?> l ? l : Collections.emptyList();
        String defStr = StringUtils.joinCommaSeparated(defVal);

        return Option.<String>createBuilder()
                .name(name)
                .description(desc)
                .available(available)
                .binding(
                        defStr,
                        () -> {
                            Map<String, ?> m = session.get(mapInfo);
                            if (m != null && m.get(entryKey) != null) {
                                Object val = ReflectionUtils.getFieldValueSafe(field, m.get(entryKey));
                                if (val instanceof List<?> l) {
                                    return StringUtils.joinCommaSeparated(l);
                                }
                            }
                            return defStr;
                        },
                        val -> {
                            List<String> parsedList = StringUtils.parseCommaSeparated(val);
                            YaclSessionHelper.updateNestedMap(session, mapInfo, entryKey, item ->
                                    ReflectionUtils.setFieldValueSafe(field, item, parsedList)
                            );
                        }
                )
                .controller(StringControllerBuilder::create)
                .build();
    }
}
