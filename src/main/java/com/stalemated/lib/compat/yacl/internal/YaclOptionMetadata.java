package com.stalemated.lib.compat.yacl.internal;

import dev.isxander.yacl3.api.OptionDescription;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.function.Function;

/**
 * Immutable value object encapsulating the presentation metadata (name, description, availability,
 * and enum value formatting) for a configuration option in YACL.
 *
 * @param name The translated option display title.
 * @param description The translated option description or tooltip.
 * @param available Whether the availability rule lets the user edit the option.
 * @param enumFormatter Function resolving localized display names for enum constants.
 */
public record YaclOptionMetadata(
        Component name,
        OptionDescription description,
        boolean available,
        Function<Enum<?>, Component> enumFormatter
) {
    public YaclOptionMetadata {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
        Objects.requireNonNull(enumFormatter, "enumFormatter cannot be null");
    }

    public Component formatEnum(Enum<?> value) {
        return enumFormatter.apply(value);
    }

    public static YaclOptionMetadata editable(Component name, Component description, Function<Enum<?>, Component> enumFormatter) {
        return new YaclOptionMetadata(name, OptionDescription.of(description), true, enumFormatter);
    }

    public static YaclOptionMetadata locked(Component name, Component description, Component reason, Function<Enum<?>, Component> enumFormatter) {
        return new YaclOptionMetadata(name, OptionDescription.of(description, reason), false, enumFormatter);
    }
}
