package com.stalemated.lib.config.registry;

import com.stalemated.lib.config.manager.SyncedConfigManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.stalemated.lib.SLib.LOGGER;

/**
 * Registry managing all registered {@link SyncedConfigManager} instances.
 */
public final class ConfigRegistry {

    private static final List<SyncedConfigManager<?>> MANAGERS = new CopyOnWriteArrayList<>();

    private ConfigRegistry() {}

    /**
     * Registers a synced config manager.
     *
     * @param manager The manager to register.
     */
    public static void register(SyncedConfigManager<?> manager) {
        if (manager != null && !MANAGERS.contains(manager)) {
            MANAGERS.add(manager);
        }
    }

    /**
     * Unregisters a synced config manager.
     *
     * @param manager The manager to unregister.
     */
    public static void unregister(SyncedConfigManager<?> manager) {
        MANAGERS.remove(manager);
    }

    /**
     * Clears all registered managers.
     */
    public static void clear() {
        MANAGERS.clear();
    }

    /**
     * Gets an unmodifiable list of all registered managers.
     *
     * @return List of registered managers.
     */
    public static List<SyncedConfigManager<?>> getManagers() {
        return Collections.unmodifiableList(MANAGERS);
    }

    /**
     * Dispatches server player join event to all registered managers to sync configs to the connecting player.
     * Invoked automatically by S-Lib's platform hooks on the server logical side.
     *
     * @param player The connected player.
     */
    public static void onPlayerJoinServer(ServerPlayerEntity player) {
        for (SyncedConfigManager<?> manager : MANAGERS) {
            try {
                manager.sendConfigToPlayer(player);
            } catch (Exception e) {
                LOGGER.error("Failed to sync config to player {}: {}", player.getName().getString(), e.getMessage(), e);
            }
        }
    }

    /**
     * Dispatches client join event to all registered managers.
     * Invoked automatically by S-Lib platform hooks on the client logical side.
     *
     * @param isSingleplayer true if connected to an integrated singleplayer server, false if multiplayer.
     */
    public static void onClientJoin(boolean isSingleplayer) {
        for (SyncedConfigManager<?> manager : MANAGERS) {
            try {
                if (isSingleplayer) {
                    manager.onClientJoinSingleplayer();
                } else {
                    manager.onClientJoinMultiplayer();
                }
            } catch (Exception e) {
                LOGGER.error("Failed to handle client connection state update: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Dispatches client disconnect event to all registered managers to clear cached server configs.
     * Invoked automatically by S-Lib platform hooks on the client logical side.
     */
    public static void onClientDisconnect() {
        for (SyncedConfigManager<?> manager : MANAGERS) {
            try {
                manager.clearServerConfig();
            } catch (Exception e) {
                LOGGER.error("Failed to clear server config on disconnect: {}", e.getMessage(), e);
            }
        }
    }
}
