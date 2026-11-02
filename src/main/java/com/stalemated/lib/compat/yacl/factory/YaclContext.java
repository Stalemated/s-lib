package com.stalemated.lib.compat.yacl.factory;

import com.stalemated.lib.compat.yacl.binding.OptionAssembler;
import com.stalemated.lib.compat.yacl.binding.YaclBindingSynchronizer;
import com.stalemated.lib.compat.yacl.registry.OptionAvailabilityRule;
import com.stalemated.lib.compat.yacl.registry.YaclControllerRegistry;
import com.stalemated.lib.compat.yacl.registry.YaclNamingStrategy;
import com.stalemated.lib.compat.yacl.registry.YaclOptionMetadata;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.session.ConfigSession;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Immutable context object grouping the shared state required across YACL screen, category, and option builders.
 * Eliminates parameter clumps across factories and builders.
 *
 * @param <T> The config model type.
 * @param session The active configuration session holding the draft buffer.
 * @param synchronizer The screen-scoped registry that keeps widgets of the same option in sync.
 * @param translationPrefix The prefix of every translation key on the screen.
 * @param namingStrategy The strategy for resolving localized text and keys.
 * @param controllerRegistry The registry for controller builders and type lenses.
 * @param availabilityRule The strategy for evaluating option interactability.
 */
public record YaclContext<T>(
        ConfigSession<T> session,
        YaclBindingSynchronizer synchronizer,
        String translationPrefix,
        YaclNamingStrategy namingStrategy,
        YaclControllerRegistry controllerRegistry,
        OptionAvailabilityRule availabilityRule
) {
    public YaclContext {
        Objects.requireNonNull(session, "session cannot be null");
        Objects.requireNonNull(synchronizer, "synchronizer cannot be null");
        Objects.requireNonNull(translationPrefix, "translationPrefix cannot be null");
        Objects.requireNonNull(namingStrategy, "namingStrategy cannot be null");
        Objects.requireNonNull(controllerRegistry, "controllerRegistry cannot be null");
        Objects.requireNonNull(availabilityRule, "availabilityRule cannot be null");
        requireDistinctTranslationKeys(session, translationPrefix, namingStrategy);
    }

    private static void requireDistinctTranslationKeys(ConfigSession<?> session, String translationPrefix, YaclNamingStrategy namingStrategy) {
        Map<String, String> optionByTranslationKey = new HashMap<>();
        for (OptionInfo option : session.getOptionTree().all()) {
            String translationKey = namingStrategy.resolveBaseKey(translationPrefix, option);
            String sharedWith = optionByTranslationKey.putIfAbsent(translationKey, option.getKey());
            if (sharedWith != null) {
                throw new IllegalStateException("Options '" + sharedWith + "' and '" + option.getKey() + "' share the translation key '" + translationKey + "'");
            }
        }
    }

    public YaclContext(
            ConfigSession<T> session,
            String translationPrefix,
            YaclNamingStrategy namingStrategy,
            YaclControllerRegistry controllerRegistry,
            OptionAvailabilityRule availabilityRule
    ) {
        this(session, new YaclBindingSynchronizer(session), translationPrefix, namingStrategy, controllerRegistry, availabilityRule);
    }

    /**
     * Resolves the presentation metadata for a given configuration option.
     *
     * @param info The option metadata.
     * @return The resolved {@link YaclOptionMetadata}.
     */
    public YaclOptionMetadata resolveMetadata(OptionInfo info) {
        return metadata(namingStrategy.resolveName(translationPrefix, info), namingStrategy.resolveDescription(translationPrefix, info), info, info.getKey());
    }

    public YaclOptionMetadata metadata(Component name, Component description, OptionInfo storedOption, String optionKey) {
        Function<Enum<?>, Component> enumFormatter = value -> namingStrategy.resolveEnumValue(translationPrefix, optionKey, value);
        return availabilityRule.unavailableReason(storedOption, session)
                .map(reason -> YaclOptionMetadata.locked(name, description, reason, enumFormatter))
                .orElseGet(() -> YaclOptionMetadata.editable(name, description, enumFormatter));
    }

    public OptionAssembler assembler() {
        return new OptionAssembler(synchronizer, controllerRegistry);
    }
}
