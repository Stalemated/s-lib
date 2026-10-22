package com.stalemated.lib.compat.yacl.registry;

import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.session.ConfigSession;
import dev.isxander.yacl3.api.OptionDescription;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * Immutable value object encapsulating the presentation metadata (name, description, availability)
 * for a configuration option in YACL.
 *
 * @param name The translated option display title.
 * @param description The translated option description or tooltip.
 * @param available Whether the option is editable based on network sync rules.
 */
public record YaclOptionMetadata(
        Component name,
        OptionDescription description,
        boolean available
) {
    public YaclOptionMetadata {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
    }

    /**
     * Resolves presentation metadata for the given option and session.
     *
     * @param session The configuration session.
     * @param info The option metadata.
     * @param modId The mod identifier.
     * @param namingStrategy The naming strategy.
     * @param availabilityRule The availability rule.
     * @return A new {@link YaclOptionMetadata} instance.
     */
    public static YaclOptionMetadata resolve(
            ConfigSession<?> session,
            OptionInfo info,
            String modId,
            YaclNamingStrategy namingStrategy,
            OptionAvailabilityRule availabilityRule
    ) {
        boolean available = availabilityRule.isAvailable(info, session);
        Component name = namingStrategy.resolveName(modId, info);
        OptionDescription description = namingStrategy.resolveDescription(modId, info, available);
        return new YaclOptionMetadata(name, description, available);
    }
}
