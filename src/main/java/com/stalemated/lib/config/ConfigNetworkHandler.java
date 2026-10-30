package com.stalemated.lib.config;

import com.stalemated.lib.config.model.OptionChange;
import com.stalemated.lib.config.network.ConfigNetworkPayload;
import com.stalemated.lib.config.network.ConnectionState;
import com.stalemated.lib.config.network.SyncMode;
import com.stalemated.lib.helper.PlatformHelper;
import com.stalemated.lib.network.NetworkHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.stalemated.lib.SLib.LOGGER;

final class ConfigNetworkHandler<T> {

    private final SyncedConfigManager<T> manager;
    private final ResourceLocation s2cPacket;
    private final ResourceLocation c2sPacket;
    private final ResourceLocation informC2sPacket;

    ConfigNetworkHandler(SyncedConfigManager<T> manager, ResourceLocation s2cPacket, ResourceLocation c2sPacket, ResourceLocation informC2sPacket) {
        this.manager = manager;
        this.s2cPacket = s2cPacket;
        this.c2sPacket = c2sPacket;
        this.informC2sPacket = informC2sPacket;
    }

    void registerReceivers() {
        registerClientReceivers();
        registerServerReceivers();
    }

    private void registerClientReceivers() {
        NetworkHelper.registerClientReceiver(s2cPacket, this::handleServerConfigPayload);
    }

    private void registerServerReceivers() {
        NetworkHelper.registerServerReceiver(c2sPacket, this::handleClientOverridePayload);
        NetworkHelper.registerServerReceiver(informC2sPacket, this::handleClientInformPayload);
    }

    /**
     * Handles an incoming S2C config synchronization payload from the server.
     *
     * @param buf The buffer containing the payload data.
     */
    void handleServerConfigPayload(FriendlyByteBuf buf) {
        if (manager.getConnectionState() == ConnectionState.SINGLEPLAYER) {
            return; // Ignore network loopback in singleplayer
        }
        T target = manager.cloneConfig(manager.local());
        ConfigNetworkPayload.readAndApply(buf, manager.getOptionTree(), SyncMode.OVERRIDE_CLIENT, target, manager.serializer());
        manager.setServerConfig(target);
        manager.setConnectionState(ConnectionState.MULTIPLAYER_MODDED);
        manager.fireChanged();
    }

    /**
     * Handles an incoming C2S client override request payload on the server.
     * The payload only carries the options the client changed.
     *
     * @param player The sender player.
     * @param buf The buffer containing the payload data.
     */
    void handleClientOverridePayload(ServerPlayer player, FriendlyByteBuf buf) {
        if (manager.checkServerPermission(player)) {
            boolean isHost = PlatformHelper.isSingleplayerOwner(player);

            if (!isHost) {
                ConfigNetworkPayload.readAndApply(buf, manager.getOptionTree(), SyncMode.OVERRIDE_CLIENT, manager.local(), manager.serializer());
                manager.save();
            }

            MinecraftServer server = PlatformHelper.getServer(player);
            if (server != null) {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    sendConfigToPlayer(p);
                }
            }
        } else {
            // If rejected, send back the config to avoid desyncs
            if (player != null) {
                sendConfigToPlayer(player);
            }
        }
    }

    /**
     * Handles an incoming C2S client inform notification payload on the server.
     *
     * @param player The sender player.
     * @param buf The buffer containing the payload data.
     */
    void handleClientInformPayload(ServerPlayer player, FriendlyByteBuf buf) {
        // Read informed data into a temporary clone of the default config
        T tempConfig = manager.createDefaultOrClone();
        ConfigNetworkPayload.readAndApply(buf, manager.getOptionTree(), SyncMode.INFORM_SERVER, tempConfig, manager.serializer());

        // Notify listeners about this player's specific choices
        manager.notifyInformListeners(player, tempConfig);
    }

    /**
     * Serializes the current server config and pushes it to a specific player.
     * Typically called during player join events on the server side.
     *
     * @param player The target player to sync the config to.
     */
    void sendConfigToPlayer(ServerPlayer player) {
        // Prevent local loopback race condition: don't send the S2C sync packet to the integrated server host.
        if (PlatformHelper.isSingleplayerOwner(player)) return;

        String json = ConfigNetworkPayload.snapshot(manager.getOptionTree(), manager.local(), SyncMode.OVERRIDE_CLIENT, manager.serializer());
        if (fits(json, ConfigNetworkPayload.MAX_PAYLOAD_BYTES, "player " + player.getName().getString())) {
            NetworkHelper.sendToClient(player, s2cPacket, ConfigNetworkPayload.encode(json));
        }
    }

    /**
     * Sends the changed override options to the server.
     *
     * @param overrides The override options that changed and their new values.
     * @return false if the payload is over the server-bound limit and nothing was sent.
     */
    boolean sendOverridesToServer(List<OptionChange> overrides) {
        return sendToServer(c2sPacket, ConfigNetworkPayload.changes(overrides, manager.serializer()));
    }

    /**
     * Asks the integrated server to broadcast the host's config to LAN players.
     * The host shares its config instance with the integrated server, so the payload carries no options.
     */
    void requestOverrideBroadcast() {
        sendToServer(c2sPacket, ConfigNetworkPayload.changes(List.of(), manager.serializer()));
    }

    /**
     * Sends a C2S packet to inform the server of local preferences.
     */
    void sendInformToServer() {
        sendToServer(informC2sPacket, ConfigNetworkPayload.snapshot(manager.getOptionTree(), manager.local(), SyncMode.INFORM_SERVER, manager.serializer()));
    }

    private boolean sendToServer(ResourceLocation packet, String json) {
        if (!fits(json, ConfigNetworkPayload.MAX_SERVER_BOUND_BYTES, "the server")) return false;
        NetworkHelper.sendToServer(packet, ConfigNetworkPayload.encode(json));
        return true;
    }

    private boolean fits(String json, int maxBytes, String destination) {
        int bytes = json.getBytes(StandardCharsets.UTF_8).length;
        if (bytes <= maxBytes) return true;

        LOGGER.error("Sync of config '{}' to {} is {} bytes, limit is {}; nothing was sent", manager.getName(), destination, bytes, maxBytes);
        return false;
    }
}
