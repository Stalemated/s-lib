package com.stalemated.lib.compat.yacl.registry;

import com.stalemated.lib.compat.yacl.controller.builder.AdvancedColorControllerBuilder;
import com.stalemated.lib.compat.yacl.internal.IdController;
import com.stalemated.lib.compat.yacl.internal.ItemOrTagController;
import com.stalemated.lib.compat.yacl.internal.ItemTargetIndex;
import com.stalemated.lib.compat.yacl.internal.TypeLens;
import com.stalemated.lib.config.OptionInfo;
import com.stalemated.lib.config.annotation.Format;
import com.stalemated.lib.config.annotation.ItemDropdown;
import com.stalemated.lib.config.internal.Constraint;
import com.stalemated.lib.util.color.ColorUtils;
import com.stalemated.lib.util.internal.ReflectionUtils;
import com.stalemated.lib.util.internal.RegexText;
import com.stalemated.lib.util.internal.TypeCoercionUtils;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.awt.Color;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.stalemated.lib.SLib.LOGGER;

/**
 * Maps config types to YACL controllers. A type is either edited as it is, with
 * {@link #registerDirect(Class, Object, ControllerFactory)}, or converted to another type for the
 * screen, usually {@code String}, with {@link #registerAdapted}. It also gives the initial value
 * of new list elements and entries. Client only.
 * <p>
 * A new registry already handles {@code boolean}, {@code int}, {@code long}, {@code float} and
 * {@code double} with their wrappers, {@code String}, {@code Color}, {@code TextColor},
 * {@code ResourceLocation}, {@code UUID} and {@code Pattern}. Numbers with a range annotation get
 * a slider. Enums need no registration: they get a dropdown.
 */
public class YaclControllerRegistry {

    private static final Pattern NO_BREAK_SPACES = Pattern.compile("[\\u00a0\\u202f]");

    private final Map<Class<?>, TypeLens<?, ?>> lenses = new LinkedHashMap<>();
    private final Map<Class<?>, Supplier<?>> initialSuppliers = new LinkedHashMap<>();
    private final Map<Boolean, ItemTargetIndex> itemTargetsByTagInclusion = new HashMap<>();
    private final Function<Boolean, ItemTargetIndex> itemTargetSource;

    /**
     * Creates a registry with the default types. The item suggestions are read from the item
     * registry the first time they are needed.
     */
    public YaclControllerRegistry() {
        this(ItemTargetIndex::fromRegistry);
    }

    /** Used by the S-Lib YACL layer. */
    @ApiStatus.Internal
    public YaclControllerRegistry(Function<Boolean, ItemTargetIndex> itemTargetSource) {
        this.itemTargetSource = Objects.requireNonNull(itemTargetSource, "itemTargetSource cannot be null");
        registerDefaultPrimitives();
        registerDefaultAdaptedTypes();
    }

    /**
     * Same as {@link #YaclControllerRegistry()}. Each call returns a new registry.
     *
     * @return a new registry
     */
    public static YaclControllerRegistry createDefault() {
        return new YaclControllerRegistry();
    }

    /**
     * Drops the cached item suggestions, so they are read again from the item registry and the
     * loaded tags. {@code YaclScreenBuilder} calls this every time it builds a screen.
     */
    public void clearItemTargets() {
        itemTargetsByTagInclusion.clear();
    }

    private ItemTargetIndex itemTargets(boolean includeTags) {
        return itemTargetsByTagInclusion.computeIfAbsent(includeTags, itemTargetSource);
    }

    private void registerDefaultPrimitives() {
        registerDirect(boolean.class, Boolean.class, false, (info, opt) -> TickBoxControllerBuilder.create(opt));

        registerDirect(int.class, Integer.class, 0, (info, opt) -> {
            if (constraintOf(info) instanceof Constraint.IntRange range) {
                return formatted(IntegerSliderControllerBuilder.create(opt)
                        .range(range.min(), range.max())
                        .step(range.step()), info, range.min(), null);
            }
            return IntegerFieldControllerBuilder.create(opt);
        });

        registerDirect(long.class, Long.class, 0L, (info, opt) -> {
            if (constraintOf(info) instanceof Constraint.LongRange range) {
                return formatted(LongSliderControllerBuilder.create(opt)
                        .range(range.min(), range.max())
                        .step(range.step()), info, range.min(), null);
            }
            return LongFieldControllerBuilder.create(opt);
        });

        registerDirect(float.class, Float.class, 0.0f, (info, opt) -> {
            if (constraintOf(info) instanceof Constraint.FloatRange range) {
                return formatted(FloatSliderControllerBuilder.create(opt)
                        .range(range.min(), range.max())
                        .step(range.step()), info, range.min(), decimalPattern(Float.toString(range.step())));
            }
            return FloatFieldControllerBuilder.create(opt);
        });

        registerDirect(double.class, Double.class, 0.0, (info, opt) -> {
            if (constraintOf(info) instanceof Constraint.DoubleRange range) {
                return formatted(DoubleSliderControllerBuilder.create(opt)
                        .range(range.min(), range.max())
                        .step(range.step()), info, range.min(), decimalPattern(Double.toString(range.step())));
            }
            return DoubleFieldControllerBuilder.create(opt);
        });

        registerDirect(String.class, "", (info, opt) -> {
            Optional<ItemDropdown> dropdown = itemDropdownOf(info);
            if (dropdown.isPresent()) {
                boolean includeTags = dropdown.get().allowTags();
                return () -> new ItemOrTagController(opt, () -> itemTargets(includeTags));
            }
            return StringControllerBuilder.create(opt);
        });
    }

    private static Constraint constraintOf(OptionInfo info) {
        return info != null ? info.constraint() : Constraint.NONE;
    }

    private static Optional<Format> formatOf(OptionInfo info) {
        return info != null ? info.annotation(Format.class) : Optional.empty();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static <T extends Number, B extends ValueFormattableController<T, B>> B formatted(B slider, OptionInfo info, T min, String defaultPattern) {
        String pattern = formatOf(info).map(Format::value).orElse(defaultPattern);
        if (pattern == null) return slider;
        try {
            String.format(pattern, min);
        } catch (IllegalFormatException e) {
            throw new IllegalArgumentException("@Format on '" + info.getKey() + "' cannot format a " + min.getClass().getSimpleName() + ": '" + pattern + "'", e);
        }
        return slider.formatValue(value -> Component.literal(NO_BREAK_SPACES.matcher(String.format(pattern, value)).replaceAll(" ")));
    }

    private static String decimalPattern(String step) {
        return "%,." + Math.max(0, new BigDecimal(step).stripTrailingZeros().scale()) + "f";
    }

    private static Optional<ItemDropdown> itemDropdownOf(OptionInfo info) {
        return info != null ? info.annotation(ItemDropdown.class) : Optional.empty();
    }

    private void registerDefaultAdaptedTypes() {
        registerAdapted(
                Color.class,
                ColorUtils::toArgbHex,
                str -> parseOrWarn("color", str, ColorUtils::parseArgbColor),
                "#FFFFFF",
                (info, opt) -> AdvancedColorControllerBuilder.create(opt).alpha(true)
        );

        registerAdapted(
                TextColor.class,
                TextColor::serialize,
                str -> parseOrWarn("text color", str, ColorUtils::parseTextColor),
                "white",
                (info, opt) -> AdvancedColorControllerBuilder.create(opt).alpha(false)
        );

        registerAdapted(
                ResourceLocation.class,
                ResourceLocation::toString,
                YaclControllerRegistry::idFromGui,
                "",
                (info, opt) -> {
                    if (itemDropdownOf(info).isPresent()) {
                        return () -> new ItemOrTagController(opt, () -> itemTargets(false));
                    }
                    return () -> new IdController(opt);
                }
        );

        registerAdapted(
                UUID.class,
                UUID::toString,
                str -> parseOrWarn("UUID", str, UUID::fromString),
                "00000000-0000-0000-0000-000000000000",
                (info, opt) -> StringControllerBuilder.create(opt)
        );

        registerAdapted(
                Pattern.class,
                RegexText::format,
                str -> parseOrWarn("regex", str, RegexText::parse),
                ".*",
                (info, opt) -> StringControllerBuilder.create(opt)
        );
    }

    private static ResourceLocation idFromGui(String text) {
        if (text.isEmpty()) return null;
        return parseOrWarn("id", text, ResourceLocation::tryParse);
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

    /**
     * Registers a type that the screen edits as it is. It replaces an earlier registration of the
     * same type.
     *
     * @param type the type to register
     * @param initial value shown when the option has no value, and initial value of new list elements; the same instance is used every time
     * @param factory makes the controller
     * @param <V> the value type
     * @throws NullPointerException if {@code factory} is {@code null}
     */
    public <V> void registerDirect(Class<V> type, V initial, ControllerFactory<V> factory) {
        registerDirect(type, type, initial, factory);
    }

    /**
     * Same as {@link #registerDirect(Class, Object, ControllerFactory)} for a primitive type and
     * its wrapper, with the same controller for both.
     *
     * @param primitiveType the primitive class, for example {@code int.class}
     * @param boxedType the wrapper class, for example {@code Integer.class}
     * @param initial value shown when the option has no value, and initial value of new list elements
     * @param factory makes the controller
     * @param <V> the wrapper type
     * @throws NullPointerException if {@code factory} is {@code null}
     */
    public <V> void registerDirect(Class<?> primitiveType, Class<V> boxedType, V initial, ControllerFactory<V> factory) {
        TypeLens<V, V> lens = TypeLens.identity(initial, factory);
        for (Class<?> type : List.of(primitiveType, boxedType)) {
            lenses.put(type, lens);
            initialSuppliers.put(type, () -> initial);
        }
    }

    /**
     * Registers a type that the screen edits as another type. It replaces an earlier registration
     * of the same type.
     *
     * @param modelType the type of the config field
     * @param toGui converts a config value to the screen value
     * @param fromGui converts a screen value back; returning {@code null} ignores the edit
     * @param fallbackGuiDefault screen value used when the config value is {@code null}; {@code fromGui(fallbackGuiDefault)} is also the initial value of new elements, and a {@code String} here lets the type be edited as text in map pairs, {@code nestedMapLists} and {@code dropdown}
     * @param guiControllerFactory makes the controller of the screen type
     * @param <M> the config type
     * @param <G> the screen type
     * @throws NullPointerException if {@code toGui}, {@code fromGui} or {@code guiControllerFactory} is {@code null}
     */
    public <M, G> void registerAdapted(
            Class<M> modelType,
            Function<M, G> toGui,
            Function<G, M> fromGui,
            G fallbackGuiDefault,
            ControllerFactory<G> guiControllerFactory
    ) {
        lenses.put(modelType, new TypeLens<>(toGui, fromGui, fallbackGuiDefault, guiControllerFactory));
        initialSuppliers.put(modelType, () -> fromGui.apply(fallbackGuiDefault));
    }

    /**
     * Registers the initial value of new elements of {@code type}, for lists and Add Entry. It
     * replaces the initial value of an earlier registration of the same type.
     *
     * @param type the element type; its subtypes use it too unless they have their own
     * @param supplier makes a new element
     * @param <E> the element type
     */
    public <E> void registerInitialSupplier(Class<E> type, Supplier<E> supplier) {
        initialSuppliers.put(type, supplier);
    }

    /**
     * Returns the initial value of a new element of {@code elementType}, for lists and Add Entry.
     * It tries, in order: the initial value registered for the type, or else for the first
     * registered supertype; the first constant of an enum; a new instance from the no-args
     * constructor, even a private one.
     *
     * @param elementType the element type, or {@code null}
     * @param <E> the element type
     * @return the value, or {@code null} if none of those works or {@code elementType} is {@code null}
     * @throws RuntimeException if the no-args constructor throws
     */
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

    /** Used by the S-Lib YACL layer. */
    @ApiStatus.Internal
    @SuppressWarnings("unchecked")
    public <M, G> Optional<TypeLens<M, G>> findLens(Class<?> type) {
        return Optional.ofNullable((TypeLens<M, G>) ReflectionUtils.findAssignable(lenses, type));
    }

    /** Used by the S-Lib YACL layer. */
    @ApiStatus.Internal
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Optional<TypeLens<Object, String>> findTextLens(Class<?> type) {
        Optional<TypeLens<Object, Object>> lens = findLens(type);
        if (lens.isPresent() && lens.get().fallback() instanceof String) {
            return Optional.of((TypeLens) lens.get());
        }
        if (!TypeCoercionUtils.canCoerce(type)) {
            return Optional.empty();
        }
        return Optional.of(new TypeLens<>(
                YaclControllerRegistry::toText,
                text -> TypeCoercionUtils.coerce(text, type),
                "",
                (info, opt) -> StringControllerBuilder.create(opt)
        ));
    }

    private static String toText(Object value) {
        return value instanceof Enum<?> constant ? constant.name() : String.valueOf(value);
    }
}
