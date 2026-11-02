package com.stalemated.lib.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.stalemated.lib.SLib.LOGGER;

/**
 * Registry of the config files and sync channels in use, and of the synced managers that S-Lib's platform hooks drive.
 */
public final class ConfigRegistry {

    private static final Map<Path, LocalConfigManager<?>> FILES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, SyncedConfigManager<?>> CHANNELS = new ConcurrentHashMap<>();

    private ConfigRegistry() {}

    static synchronized void claimFile(LocalConfigManager<?> manager) {
        Path file = fileOf(manager);
        requireFree(FILES, file, manager, "Config file");
        FILES.put(file, manager);
    }

    static synchronized void claimFileAndChannel(SyncedConfigManager<?> manager) {
        Path file = fileOf(manager);
        requireFree(FILES, file, manager, "Config file");
        requireFree(CHANNELS, manager.getSyncChannel(), manager, "Sync channel");
        FILES.put(file, manager);
        CHANNELS.put(manager.getSyncChannel(), manager);
    }

    private static Path fileOf(LocalConfigManager<?> manager) {
        return manager.getConfigPath().toAbsolutePath().normalize();
    }

    private static <K> void requireFree(Map<K, ? extends LocalConfigManager<?>> claims, K key, LocalConfigManager<?> manager, String kind) {
        LocalConfigManager<?> owner = claims.get(key);
        if (owner != null) {
            throw new IllegalStateException(kind + " " + key + " of " + describe(manager) + " is already used by " + describe(owner));
        }
    }

    private static String describe(LocalConfigManager<?> manager) {
        return manager.getOptionTree().getRootClass().getSimpleName() + " ('" + manager.getName() + "')";
    }

    /**
     * Releases every claimed file and channel, and the network receivers of the synced configs.
     * Meant for tests that register the same config more than once.
     */
    public static synchronized void clear() {
        CHANNELS.values().forEach(manager -> manager.networkHandler().unregisterReceivers());
        FILES.clear();
        CHANNELS.clear();
    }

    static List<SyncedConfigManager<?>> getManagers() {
        return List.copyOf(CHANNELS.values());
    }

    /**
     * Dispatches server player join event to all registered managers to sync configs to the connecting player.
     * Invoked automatically by S-Lib's platform hooks on the server logical side.
     *
     * @param player The connected player.
     */
    public static void onPlayerJoinServer(ServerPlayer player) {
        for (SyncedConfigManager<?> manager : CHANNELS.values()) {
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
        for (SyncedConfigManager<?> manager : CHANNELS.values()) {
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
        for (SyncedConfigManager<?> manager : CHANNELS.values()) {
            try {
                manager.clearServerConfig();
            } catch (Exception e) {
                LOGGER.error("Failed to clear server config on disconnect: {}", e.getMessage(), e);
            }
        }
    }
}
