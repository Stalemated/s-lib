package com.stalemated.lib.config.internal;

import com.stalemated.lib.config.ConfigManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.stalemated.lib.SLib.LOGGER;

/**
 * Registry of the config files and sync channels in use, and of the synced configs that S-Lib's platform hooks drive.
 */
public final class ConfigRegistry {

    private static final Map<Path, ConfigManager<?>> FILES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, SyncedConfigHooks> CHANNELS = new ConcurrentHashMap<>();

    private static volatile MinecraftServer currentServer;

    private ConfigRegistry() {}

    public static MinecraftServer currentServer() {
        return currentServer;
    }

    public static void onServerStarted(MinecraftServer server) {
        currentServer = server;
    }

    public static void onServerStopped() {
        currentServer = null;
    }

    public static synchronized void claimFile(ConfigManager<?> manager, Path configPath) {
        Path file = fileOf(configPath);
        requireFreeFile(file, manager);
        FILES.put(file, manager);
    }

    public static synchronized void claimFileAndChannel(SyncedConfigHooks synced, Path configPath, ResourceLocation channel) {
        Path file = fileOf(configPath);
        requireFreeFile(file, synced.manager());
        requireFreeChannel(channel, synced.manager());
        FILES.put(file, synced.manager());
        CHANNELS.put(channel, synced);
    }

    private static Path fileOf(Path configPath) {
        return configPath.toAbsolutePath().normalize();
    }

    private static void requireFreeFile(Path file, ConfigManager<?> manager) {
        ConfigManager<?> owner = FILES.get(file);
        if (owner != null) {
            throw alreadyUsed("Config file", file, manager, owner);
        }
    }

    private static void requireFreeChannel(ResourceLocation channel, ConfigManager<?> manager) {
        SyncedConfigHooks owner = CHANNELS.get(channel);
        if (owner != null) {
            throw alreadyUsed("Sync channel", channel, manager, owner.manager());
        }
    }

    private static IllegalStateException alreadyUsed(String kind, Object key, ConfigManager<?> manager, ConfigManager<?> owner) {
        return new IllegalStateException(kind + " " + key + " of " + describe(manager) + " is already used by " + describe(owner));
    }

    private static String describe(ConfigManager<?> manager) {
        return manager.getOptionTree().getRootClass().getSimpleName() + " ('" + manager.getName() + "')";
    }

    /**
     * Releases every claimed file and channel, and the network receivers of the synced configs.
     * Meant for tests that register the same config more than once.
     */
    public static synchronized void clear() {
        CHANNELS.values().forEach(SyncedConfigHooks::unregisterReceivers);
        FILES.clear();
        CHANNELS.clear();
    }

    /**
     * Dispatches server player join event to all registered configs to sync them to the connecting player.
     * Invoked automatically by S-Lib's platform hooks on the server logical side.
     *
     * @param player The connected player.
     */
    public static void onPlayerJoinServer(ServerPlayer player) {
        notifySynced(synced -> synced.sendConfigTo(player), () -> "Failed to sync config to player " + player.getName().getString());
    }

    public static void onOperatorStatusChanged(PlayerList players, UUID playerId) {
        ServerPlayer player = players.getPlayer(playerId);
        if (player == null) return;
        onPlayerPermissionChanged(player);
    }

    public static void onPlayersPermissionChanged(PlayerList players) {
        players.getPlayers().forEach(ConfigRegistry::onPlayerPermissionChanged);
    }

    public static void onPlayerPermissionChanged(ServerPlayer player) {
        notifySynced(synced -> synced.sendConfigTo(player), () -> "Failed to resend config to player " + player.getName().getString() + " after a permission change");
    }

    public static void onPlayerLeaveServer(ServerPlayer player) {
        CHANNELS.values().forEach(synced -> synced.onPlayerLeave(player.getUUID()));
    }

    /**
     * Dispatches client join event to all registered configs.
     * Invoked automatically by S-Lib platform hooks on the client logical side.
     *
     * @param isSingleplayer true if connected to an integrated singleplayer server, false if multiplayer.
     */
    public static void onClientJoin(boolean isSingleplayer) {
        notifySynced(synced -> synced.onClientJoin(isSingleplayer), () -> "Failed to handle client connection state update");
    }

    /**
     * Dispatches client disconnect event to all registered configs to clear cached server configs.
     * Invoked automatically by S-Lib platform hooks on the client logical side.
     */
    public static void onClientDisconnect() {
        notifySynced(SyncedConfigHooks::onClientDisconnect, () -> "Failed to clear server config on disconnect");
    }

    private static void notifySynced(Consumer<SyncedConfigHooks> event, Supplier<String> failure) {
        for (SyncedConfigHooks synced : CHANNELS.values()) {
            try {
                event.accept(synced);
            } catch (Exception e) {
                LOGGER.error("{}: {}", failure.get(), e.getMessage(), e);
            }
        }
    }
}
