package com.stalemated.lib.compat.yacl.registry;

import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.session.ConfigSession;
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
 * @param available Whether the option is editable based on network sync rules.
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
        if (enumFormatter == null) {
            enumFormatter = e -> Component.literal(e != null ? e.toString() : "");
        }
    }

    public YaclOptionMetadata(Component name, OptionDescription description, boolean available) {
        this(name, description, available, e -> Component.literal(e != null ? e.toString() : ""));
    }

    public Component formatEnum(Enum<?> value) {
        return enumFormatter.apply(value);
    }

    /**
     * Resolves presentation metadata for the given option and session.
     *
     * @param session The configuration session.
     * @param info The option metadata.
     * @param translationPrefix The prefix of every translation key on the screen.
     * @param namingStrategy The naming strategy.
     * @param availabilityRule The availability rule.
     * @return A new {@link YaclOptionMetadata} instance.
     */
    public static YaclOptionMetadata resolve(
            ConfigSession<?> session,
            OptionInfo info,
            String translationPrefix,
            YaclNamingStrategy namingStrategy,
            OptionAvailabilityRule availabilityRule
    ) {
        boolean available = availabilityRule.isAvailable(info, session);
        Component name = namingStrategy.resolveName(translationPrefix, info);
        OptionDescription description = namingStrategy.resolveDescription(translationPrefix, info, available);
        Function<Enum<?>, Component> enumFormatter = e -> namingStrategy.resolveEnumValue(translationPrefix, info, e);
        return new YaclOptionMetadata(name, description, available, enumFormatter);
    }
}
