package com.stalemated.lib.compat.yacl.registry;

import com.stalemated.lib.compat.yacl.controller.builder.AdvancedColorControllerBuilder;
import com.stalemated.lib.compat.yacl.controller.builder.ItemOrTagControllerBuilder;
import com.stalemated.lib.compat.yacl.controller.builder.SimpleEnumDropdownControllerBuilder;
import com.stalemated.lib.compat.yacl.controller.helper.ItemTargetIndex;
import com.stalemated.lib.compat.yacl.factory.YaclContext;
import com.stalemated.lib.config.annotation.ItemDropdown;
import com.stalemated.lib.config.model.Constraint;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.session.ConfigSession;
import com.stalemated.lib.util.color.ColorUtils;
import com.stalemated.lib.util.reflection.ReflectionUtils;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;

import java.awt.Color;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.stalemated.lib.SLib.LOGGER;

/**
 * Registry of controller builders and type adapters for YACL.
 * Decouples model types from GUI controller implementations, supporting both
 * direct model controllers and intermediate adapted types (Lens pattern).
 */
public class YaclControllerRegistry {

    @FunctionalInterface
    public interface ControllerFactory<V> {
        ControllerBuilder<V> create(OptionInfo info, Option<V> yaclOption);
    }

    public record AdaptedType<M, G>(
            Class<M> modelType,
            Function<M, G> toGui,
            Function<G, M> fromGui,
            G fallbackGuiDefault,
            ControllerFactory<G> guiControllerFactory
    ) {}

    public interface OptionBinder {
        <T> Option<?> bindOption(
                YaclContext<T> context,
                OptionInfo info,
                YaclOptionMetadata meta,
                Consumer<Option.Builder<?>> customizer
        );
    }

    private final Map<Class<?>, OptionBinder> binders = new LinkedHashMap<>();
    private final Map<Class<?>, ControllerFactory<?>> directControllers = new LinkedHashMap<>();
    private final Map<Class<?>, AdaptedType<?, ?>> adaptedTypes = new LinkedHashMap<>();
    private final Map<Class<?>, Supplier<?>> initialSuppliers = new LinkedHashMap<>();
    private final Map<Boolean, ItemTargetIndex> itemTargetsByTagInclusion = new HashMap<>();

    public YaclControllerRegistry() {
        registerDefaultPrimitives();
        registerDefaultAdaptedTypes();
        registerDefaultInitialSuppliers();
    }

    public static YaclControllerRegistry createDefault() {
        return new YaclControllerRegistry();
    }

    private ItemTargetIndex itemTargets(boolean includeTags) {
        return itemTargetsByTagInclusion.computeIfAbsent(includeTags, ItemTargetIndex::fromRegistry);
    }

    private void registerDefaultPrimitives() {
        registerDirect(boolean.class, Boolean.class, (info, opt) -> TickBoxControllerBuilder.create(opt));

        registerDirect(int.class, Integer.class, (info, opt) -> {
            if (constraintOf(info) instanceof Constraint.IntRange range) {
                return IntegerSliderControllerBuilder.create(opt)
                        .range(range.min(), range.max())
                        .step(range.step());
            }
            return IntegerFieldControllerBuilder.create(opt);
        });

        registerDirect(long.class, Long.class, (info, opt) -> {
            if (constraintOf(info) instanceof Constraint.LongRange range) {
                return LongSliderControllerBuilder.create(opt)
                        .range(range.min(), range.max())
                        .step(range.step());
            }
            return LongFieldControllerBuilder.create(opt);
        });

        registerDirect(float.class, Float.class, (info, opt) -> {
            if (constraintOf(info) instanceof Constraint.FloatRange range) {
                return FloatSliderControllerBuilder.create(opt)
                        .range(range.min(), range.max())
                        .step(range.step());
            }
            return FloatFieldControllerBuilder.create(opt);
        });

        registerDirect(double.class, Double.class, (info, opt) -> {
            if (constraintOf(info) instanceof Constraint.DoubleRange range) {
                return DoubleSliderControllerBuilder.create(opt)
                        .range(range.min(), range.max())
                        .step(range.step());
            }
            return DoubleFieldControllerBuilder.create(opt);
        });

        registerDirect(String.class, (info, opt) -> {
            Optional<ItemDropdown> dropdown = itemDropdownOf(info);
            if (dropdown.isPresent()) {
                boolean includeTags = dropdown.get().allowTags();
                return ItemOrTagControllerBuilder.create(opt, () -> itemTargets(includeTags));
            }
            return StringControllerBuilder.create(opt);
        });
    }

    private static Constraint constraintOf(OptionInfo info) {
        return info != null ? info.getConstraint() : Constraint.NONE;
    }

    private static Optional<ItemDropdown> itemDropdownOf(OptionInfo info) {
        return info != null ? info.annotation(ItemDropdown.class) : Optional.empty();
    }

    private void registerDefaultAdaptedTypes() {
        registerAdapted(
                Color.class,
                c -> c != null ? ColorUtils.toArgbHex(c) : "#FFFFFF",
                str -> parseOrWarn("color", str, ColorUtils::parseArgbColor),
                "#FFFFFF",
                (info, opt) -> AdvancedColorControllerBuilder.create(opt).alpha(true)
        );

        registerAdapted(
                TextColor.class,
                c -> c != null ? c.serialize() : "white",
                str -> parseOrWarn("text color", str, ColorUtils::parseTextColor),
                "white",
                (info, opt) -> AdvancedColorControllerBuilder.create(opt).alpha(false)
        );

        registerAdapted(
                ResourceLocation.class,
                loc -> loc != null ? loc.toString() : "minecraft:air",
                str -> parseOrWarn("id", str, ResourceLocation::tryParse),
                "minecraft:air",
                (info, opt) -> {
                    if (itemDropdownOf(info).isPresent()) {
                        return ItemOrTagControllerBuilder.create(opt, () -> itemTargets(false));
                    }
                    return StringControllerBuilder.create(opt);
                }
        );

        registerAdapted(
                UUID.class,
                uuid -> uuid != null ? uuid.toString() : "00000000-0000-0000-0000-000000000000",
                str -> parseOrWarn("UUID", str, UUID::fromString),
                "00000000-0000-0000-0000-000000000000",
                (info, opt) -> StringControllerBuilder.create(opt)
        );

        registerAdapted(
                Pattern.class,
                p -> p != null ? p.pattern() : ".*",
                str -> parseOrWarn("regex", str, Pattern::compile),
                ".*",
                (info, opt) -> StringControllerBuilder.create(opt)
        );
    }

    private static <M> M parseOrWarn(String kind, String input, Function<String, M> parser) {
        M parsed;
        try {
            parsed = parser.apply(input);
        } catch (RuntimeException e) {
            LOGGER.warn("Ignoring invalid {} '{}': {}", kind, input, e.getMessage());
            return null;
        }
        if (parsed == null) {
            LOGGER.warn("Ignoring invalid {} '{}'", kind, input);
        }
        return parsed;
    }

    private void registerDefaultInitialSuppliers() {
        registerInitialSupplier(boolean.class, () -> false);
        registerInitialSupplier(Boolean.class, () -> false);
        registerInitialSupplier(int.class, () -> 0);
        registerInitialSupplier(Integer.class, () -> 0);
        registerInitialSupplier(long.class, () -> 0L);
        registerInitialSupplier(Long.class, () -> 0L);
        registerInitialSupplier(float.class, () -> 0.0f);
        registerInitialSupplier(Float.class, () -> 0.0f);
        registerInitialSupplier(double.class, () -> 0.0);
        registerInitialSupplier(Double.class, () -> 0.0);
        registerInitialSupplier(String.class, () -> "");
        registerInitialSupplier(Color.class, () -> Color.WHITE);
        registerInitialSupplier(TextColor.class, () -> ColorUtils.parseTextColor("white"));
        registerInitialSupplier(ResourceLocation.class, () -> ResourceLocation.tryParse("minecraft:air"));
        registerInitialSupplier(UUID.class, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"));
        registerInitialSupplier(Pattern.class, () -> Pattern.compile(".*"));
    }

    public <V> void registerDirect(Class<V> type, ControllerFactory<V> factory) {
        registerDirect(type, type, factory);
    }

    public <V> void registerDirect(Class<?> primitiveType, Class<V> boxedType, ControllerFactory<V> factory) {
        directControllers.put(primitiveType, factory);
        if (boxedType != null && boxedType != primitiveType) {
            directControllers.put(boxedType, factory);
        }
        OptionBinder binder = new OptionBinder() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Option<?> bindOption(
                    YaclContext<T> context,
                    OptionInfo info,
                    YaclOptionMetadata meta,
                    Consumer<Option.Builder<?>> customizer
            ) {
                Option.Builder<V> builder = Option.createBuilder();
                builder.name(meta.name())
                       .description(meta.description())
                       .available(meta.available());

                V defaultVal = (V) info.defaultCopy();
                ConfigSession<?> session = context.session();
                return context.synchronizer().buildSyncedOption(
                        info,
                        builder,
                        defaultVal,
                        () -> session.get(info),
                        val -> session.set(info, val),
                        b -> {
                            b.controller(opt -> factory.create(info, opt));
                            if (customizer != null) {
                                customizer.accept(b);
                            }
                        }
                );
            }
        };

        binders.put(primitiveType, binder);
        if (boxedType != null && boxedType != primitiveType) {
            binders.put(boxedType, binder);
        }
    }

    public <M, G> void registerAdapted(
            Class<M> modelType,
            Function<M, G> toGui,
            Function<G, M> fromGui,
            G fallbackGuiDefault,
            ControllerFactory<G> guiControllerFactory
    ) {
        AdaptedType<M, G> adapted = new AdaptedType<>(modelType, toGui, fromGui, fallbackGuiDefault, guiControllerFactory);
        adaptedTypes.put(modelType, adapted);
        if (!initialSuppliers.containsKey(modelType)) {
            initialSuppliers.put(modelType, () -> fromGui.apply(fallbackGuiDefault));
        }

        binders.put(modelType, new OptionBinder() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Option<?> bindOption(
                    YaclContext<T> context,
                    OptionInfo info,
                    YaclOptionMetadata meta,
                    Consumer<Option.Builder<?>> customizer
            ) {
                Option.Builder<G> builder = Option.createBuilder();
                builder.name(meta.name())
                       .description(meta.description())
                       .available(meta.available());

                M modelDefault = (M) info.defaultCopy();
                G guiDefault = modelDefault != null ? toGui.apply(modelDefault) : fallbackGuiDefault;

                ConfigSession<?> session = context.session();
                return context.synchronizer().buildSyncedOption(
                        info,
                        builder,
                        guiDefault,
                        () -> {
                            M modelVal = session.get(info);
                            return modelVal != null ? toGui.apply(modelVal) : fallbackGuiDefault;
                        },
                        guiVal -> {
                            M modelVal = fromGui.apply(guiVal);
                            if (modelVal != null) {
                                session.set(info, modelVal);
                            }
                        },
                        b -> {
                            b.controller(opt -> guiControllerFactory.create(info, opt));
                            if (customizer != null) {
                                customizer.accept(b);
                            }
                        }
                );
            }
        });
    }

    public <E> void registerInitialSupplier(Class<E> type, Supplier<E> supplier) {
        initialSuppliers.put(type, supplier);
    }

    @SuppressWarnings("unchecked")
    public <E> E resolveInitialValue(Class<?> elementType) {
        if (elementType == null) return null;
        Supplier<?> supplier = ReflectionUtils.findAssignable(initialSuppliers, elementType);
        if (supplier != null) {
            return (E) supplier.get();
        }
        if (elementType.isEnum()) {
            Object[] constants = elementType.getEnumConstants();
            if (constants != null && constants.length > 0) {
                return (E) constants[0];
            }
        }
        if (ReflectionUtils.hasNoArgsConstructor(elementType)) {
            return (E) ReflectionUtils.tryInstantiate(elementType);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <V> ControllerFactory<V> findDirectController(Class<?> type) {
        ControllerFactory<?> cf = ReflectionUtils.findAssignable(directControllers, type);
        if (cf != null) return (ControllerFactory<V>) cf;
        if (type.isEnum()) {
            return (ControllerFactory<V>) createEnumDropdownFactory();
        }
        return null;
    }

    private static <E extends Enum<E>> ControllerFactory<E> createEnumDropdownFactory() {
        return (info, opt) -> SimpleEnumDropdownControllerBuilder.create(opt);
    }

    @SuppressWarnings("unchecked")
    public <M, G> AdaptedType<M, G> findAdaptedType(Class<?> type) {
        return (AdaptedType<M, G>) ReflectionUtils.findAssignable(adaptedTypes, type);
    }

    public <T> Option<?> createOption(
            YaclContext<T> context,
            OptionInfo info,
            YaclOptionMetadata meta,
            Consumer<Option.Builder<?>> customizer
    ) {
        Class<?> type = info.type();

        OptionBinder binder = findBinder(type);
        if (binder != null) {
            return binder.bindOption(context, info, meta, customizer);
        }

        if (info.isEnum()) {
            return bindEnumOption(context, info, meta, customizer);
        }

        throw new UnsupportedOperationException("No YACL controller or adapter registered for type: "
                + type.getName() + " on config option: " + info.getKey());
    }

    private OptionBinder findBinder(Class<?> type) {
        return ReflectionUtils.findAssignable(binders, type);
    }

    private static Option<?> bindEnumOption(
            YaclContext<?> context,
            OptionInfo info,
            YaclOptionMetadata meta,
            Consumer<Option.Builder<?>> customizer
    ) {
        return bindEnumOptionHelper(context, info, meta, customizer);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> Option<E> bindEnumOptionHelper(
            YaclContext<?> context,
            OptionInfo info,
            YaclOptionMetadata meta,
            Consumer<Option.Builder<?>> customizer
    ) {
        Option.Builder<E> builder = Option.createBuilder();
        builder.name(meta.name())
               .description(meta.description())
               .available(meta.available());
        ConfigSession<?> session = context.session();
        return context.synchronizer().buildSyncedOption(
                info,
                builder,
                (E) info.defaultCopy(),
                () -> session.get(info),
                val -> session.set(info, val),
                b -> {
                    b.controller(opt -> SimpleEnumDropdownControllerBuilder.create(opt).formatValue(meta::formatEnum));
                    if (customizer != null) {
                        customizer.accept(b);
                    }
                }
        );
    }
}
