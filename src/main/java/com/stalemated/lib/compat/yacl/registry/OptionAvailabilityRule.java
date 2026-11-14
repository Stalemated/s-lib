package com.stalemated.lib.compat.yacl.registry;

import com.stalemated.lib.config.ConfigManager;
import com.stalemated.lib.config.SyncedConfigManager;
import com.stalemated.lib.config.OptionInfo;
import com.stalemated.lib.config.network.ConnectionState;
import com.stalemated.lib.config.network.SyncMode;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * Decides whether an option can be edited on the screen and, if not, the reason shown under its
 * description. Pass one to {@code YaclScreenBuilder.availabilityRule}. Client only.
 */
@FunctionalInterface
public interface OptionAvailabilityRule {

    /**
     * Explains why an option cannot be edited on the screen right now.
     * <p>
     * With a reason, the option is locked and the reason is added under its description. The
     * screen asks again every time it is built or rebuilt.
     *
     * @param option the option to check; for list elements, POJO fields and the entry buttons, the list or map option that holds them
     * @param manager the manager the screen edits
     * @return the reason, or {@link Optional#empty()} if the option can be edited
     */
    Optional<Component> unavailableReason(OptionInfo option, ConfigManager<?> manager);

    /**
     * Combines this rule with another rule. The option is editable only if both rules allow it,
     * and the reason of this rule wins when both block it.
     *
     * @param other the other rule
     * @return the combined rule
     * @throws NullPointerException if {@code other} is {@code null}
     */
    default OptionAvailabilityRule and(OptionAvailabilityRule other) {
        Objects.requireNonNull(other, "other rule cannot be null");
        return (option, manager) -> unavailableReason(option, manager).or(() -> other.unavailableReason(option, manager));
    }

    /**
     * Locks the {@code OVERRIDE_CLIENT} options of a {@link SyncedConfigManager} when the player
     * can't change them: the server doesn't have the mod, or the player has no permission. The
     * reason is {@code s_lib.config_screen.server_without_mod} or
     * {@code s_lib.config_screen.no_permission}. Other options stay editable.
     */
    OptionAvailabilityRule DEFAULT = (option, manager) -> {
        if (option.getSyncMode() != SyncMode.OVERRIDE_CLIENT
                || !(manager instanceof SyncedConfigManager<?> synced)
                || synced.canChangeOverrides()) {
            return Optional.empty();
        }
        String reason = synced.getConnectionState() == ConnectionState.MULTIPLAYER_UNMODDED
                ? TranslationKeys.SERVER_WITHOUT_MOD
                : TranslationKeys.NO_PERMISSION;
        return Optional.of(Component.translatable(reason));
    };

    /**
     * Never locks an option. A client without permission can then edit {@code OVERRIDE_CLIENT}
     * options, and {@code modify} drops those edits with a warning.
     */
    OptionAvailabilityRule UNRESTRICTED = (option, manager) -> Optional.empty();
}
