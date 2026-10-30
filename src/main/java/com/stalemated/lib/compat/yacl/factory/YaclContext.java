package com.stalemated.lib.compat.yacl.factory;

import com.stalemated.lib.compat.yacl.registry.OptionAvailabilityRule;
import com.stalemated.lib.compat.yacl.registry.YaclControllerRegistry;
import com.stalemated.lib.compat.yacl.registry.YaclNamingStrategy;
import com.stalemated.lib.compat.yacl.registry.YaclOptionMetadata;
import com.stalemated.lib.compat.yacl.util.YaclBindingSynchronizer;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.session.ConfigSession;

import java.util.Objects;

/**
 * Immutable context object grouping the shared state required across YACL screen, category, and option builders.
 * Eliminates parameter clumps across factories and builders.
 *
 * @param <T> The config model type.
 * @param session The active configuration session holding the draft buffer.
 * @param synchronizer The screen-scoped registry that keeps widgets of the same option in sync.
 * @param modId The mod identifier.
 * @param namingStrategy The strategy for resolving localized text and keys.
 * @param controllerRegistry The registry for controller builders and type lenses.
 * @param availabilityRule The strategy for evaluating option interactability.
 */
public record YaclContext<T>(
        ConfigSession<T> session,
        YaclBindingSynchronizer synchronizer,
        String modId,
        YaclNamingStrategy namingStrategy,
        YaclControllerRegistry controllerRegistry,
        OptionAvailabilityRule availabilityRule
) {
    public YaclContext {
        Objects.requireNonNull(session, "session cannot be null");
        Objects.requireNonNull(synchronizer, "synchronizer cannot be null");
        Objects.requireNonNull(modId, "modId cannot be null");
        Objects.requireNonNull(namingStrategy, "namingStrategy cannot be null");
        Objects.requireNonNull(controllerRegistry, "controllerRegistry cannot be null");
        Objects.requireNonNull(availabilityRule, "availabilityRule cannot be null");
    }

    public YaclContext(
            ConfigSession<T> session,
            String modId,
            YaclNamingStrategy namingStrategy,
            YaclControllerRegistry controllerRegistry,
            OptionAvailabilityRule availabilityRule
    ) {
        this(session, new YaclBindingSynchronizer(session), modId, namingStrategy, controllerRegistry, availabilityRule);
    }

    /**
     * Resolves the presentation metadata for a given configuration option.
     *
     * @param info The option metadata.
     * @return The resolved {@link YaclOptionMetadata}.
     */
    public YaclOptionMetadata resolveMetadata(OptionInfo info) {
        return YaclOptionMetadata.resolve(session, info, modId, namingStrategy, availabilityRule);
    }
}
