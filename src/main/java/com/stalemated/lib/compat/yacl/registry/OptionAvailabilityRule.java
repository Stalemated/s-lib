package com.stalemated.lib.compat.yacl.registry;

import com.stalemated.lib.config.SyncedConfigManager;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.network.ConnectionState;
import com.stalemated.lib.config.network.SyncMode;
import com.stalemated.lib.config.session.ConfigSession;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * Strategy interface to evaluate whether a config option is interactively editable on the client.
 */
@FunctionalInterface
public interface OptionAvailabilityRule {

    /**
     * Explains why an option cannot be edited on the screen right now.
     *
     * @param option The configuration option to evaluate.
     * @param session The active configuration session.
     * @return The reason shown below the option description, or empty if the option is editable.
     */
    Optional<Component> unavailableReason(OptionInfo option, ConfigSession<?> session);

    /**
     * Combines this rule with another rule. The option is editable only if both rules allow it,
     * and the reason of this rule wins when both block it.
     *
     * @param other The other rule to combine with.
     * @return A composed rule.
     */
    default OptionAvailabilityRule and(OptionAvailabilityRule other) {
        Objects.requireNonNull(other, "other rule cannot be null");
        return (option, session) -> unavailableReason(option, session).or(() -> other.unavailableReason(option, session));
    }

    OptionAvailabilityRule DEFAULT = (option, session) -> {
        if (option.getSyncMode() != SyncMode.OVERRIDE_CLIENT
                || !(session.getManager() instanceof SyncedConfigManager<?> synced)
                || synced.canChangeOverrides()) {
            return Optional.empty();
        }
        String reason = synced.getConnectionState() == ConnectionState.MULTIPLAYER_UNMODDED
                ? TranslationKeys.SERVER_WITHOUT_MOD
                : TranslationKeys.NO_PERMISSION;
        return Optional.of(Component.translatable(reason));
    };

    OptionAvailabilityRule UNRESTRICTED = (option, session) -> Optional.empty();
}
