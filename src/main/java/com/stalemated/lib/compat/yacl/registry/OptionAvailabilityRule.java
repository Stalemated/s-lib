package com.stalemated.lib.compat.yacl.registry;

import com.stalemated.lib.config.manager.SyncedConfigManager;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.network.ConnectionState;
import com.stalemated.lib.config.network.SyncMode;
import com.stalemated.lib.config.permissions.ClientConfigPermissions;
import com.stalemated.lib.config.session.ConfigSession;

import java.util.Objects;

/**
 * Strategy interface to evaluate whether a config option is interactively editable on the client.
 */
@FunctionalInterface
public interface OptionAvailabilityRule {

    /**
     * Evaluates whether an option is available for user interaction under the current connection state.
     *
     * @param option The configuration option to evaluate.
     * @param connectionState The current client-server connection state.
     * @return True if editable, false otherwise.
     */
    boolean isAvailable(OptionInfo option, ConnectionState connectionState);

    /**
     * Evaluates availability by extracting the network {@link ConnectionState} from the session manager.
     *
     * @param option The configuration option to evaluate.
     * @param session The active configuration session.
     * @return True if the option is editable, false otherwise.
     */
    default boolean isAvailable(OptionInfo option, ConfigSession<?> session) {
        ConnectionState state = session.getManager() instanceof SyncedConfigManager<?> synced
                ? synced.getConnectionState()
                : null;
        return isAvailable(option, state);
    }

    /**
     * Combines this rule with another rule using logical AND.
     *
     * @param other The other rule to combine with.
     * @return A composed rule that returns true only if both rules return true.
     */
    default OptionAvailabilityRule and(OptionAvailabilityRule other) {
        Objects.requireNonNull(other, "other rule cannot be null");
        return (option, state) -> this.isAvailable(option, state) && other.isAvailable(option, state);
    }

    /**
     * Combines this rule with another rule using logical OR.
     *
     * @param other The other rule to combine with.
     * @return A composed rule that returns true if either rule returns true.
     */
    default OptionAvailabilityRule or(OptionAvailabilityRule other) {
        Objects.requireNonNull(other, "other rule cannot be null");
        return (option, state) -> this.isAvailable(option, state) || other.isAvailable(option, state);
    }

    /**
     * Returns a rule that represents the logical negation of this rule.
     *
     * @return A negated availability rule.
     */
    default OptionAvailabilityRule negate() {
        return (option, state) -> !this.isAvailable(option, state);
    }

    OptionAvailabilityRule DEFAULT = (option, state) -> {
        // Local-only and INFORM_SERVER client preferences are always editable
        if (state == null || option.getSyncMode() != SyncMode.OVERRIDE_CLIENT) {
            return true;
        }
        // Server-authoritative options cannot be altered when connected to unmodded vanilla servers
        if (state == ConnectionState.MULTIPLAYER_UNMODDED) {
            return false;
        }
        // In singleplayer or modded multiplayer, require operator privileges
        return ClientConfigPermissions.OP_OR_SP.get();
    };

    OptionAvailabilityRule UNRESTRICTED = (option, state) -> ClientConfigPermissions.ANYONE.get();
}
