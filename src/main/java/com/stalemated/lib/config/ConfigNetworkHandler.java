package com.stalemated.lib.config;

import com.stalemated.lib.config.internal.OptionChange;
import com.stalemated.lib.config.network.ConnectionState;
import com.stalemated.lib.config.network.SyncMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.stalemated.lib.SLib.LOGGER;

final class ConfigNetworkHandler<T> {

    private final SyncedConfigManager<T> manager;
    private final SyncChannels channels;
    private final ConfigTransport transport;

    ConfigNetworkHandler(SyncedConfigManager<T> manager, SyncChannels channels, ConfigTransport transport) {
        this.manager = manager;
        this.channels = channels;
        this.transport = transport;
    }

    void registerReceivers() {
        transport.registerClientReceiver(channels.serverConfig(), this::handleServerConfigPayload);
        transport.registerServerReceiver(channels.clientOverride(), this::handleClientOverridePayload);
        transport.registerServerReceiver(channels.clientInform(), this::handleClientInformPayload);
    }

    void unregisterReceivers() {
        transport.unregisterClientReceiver(channels.serverConfig());
        transport.unregisterServerReceiver(channels.clientOverride());
        transport.unregisterServerReceiver(channels.clientInform());
    }

    /**
     * Handles an incoming S2C config synchronization payload from the server.
     * The payload also tells whether this player may change the {@link SyncMode#OVERRIDE_CLIENT} options.
     * The first payload of a connection also sends this client's {@link SyncMode#INFORM_SERVER} options to the server.
     *
     * @param buf The buffer containing the payload data.
     */
    void handleServerConfigPayload(FriendlyByteBuf buf) {
        ConnectionState state = manager.getConnectionState();
        if (state == ConnectionState.SINGLEPLAYER) {
            return; // Ignore network loopback in singleplayer
        }
        ConfigNetworkPayload.ServerConfigMessage message = ConfigNetworkPayload.readServerConfig(buf);
        T serverConfig = manager.cloneConfig(manager.local());
        ConfigNetworkPayload.apply(message.options(), manager.getOptionTree(), SyncMode.OVERRIDE_CLIENT, serverConfig);
        manager.acceptServerConfig(serverConfig, message.canChangeOverrides());

        if (state != ConnectionState.MULTIPLAYER_MODDED && manager.getOptionTree().hasOptionsSyncedAs(SyncMode.INFORM_SERVER)) {
            sendInformToServer();
        }
    }

    /**
     * Handles an incoming C2S client override request payload on the server.
     * The payload only carries the options the client changed. The singleplayer owner may always change them,
     * since opening a world to LAN without cheats does not make its owner an operator.
     *
     * @param player The sender player.
     * @param buf The buffer containing the payload data.
     */
    void handleClientOverridePayload(ServerPlayer player, FriendlyByteBuf buf) {
        if (!canChangeOverrides(player)) {
            if (player != null) {
                sendConfigToPlayer(player);
            }
            return;
        }

        if (!transport.isSingleplayerOwner(player)) {
            ConfigNetworkPayload.readAndApply(buf, ConfigNetworkPayload.MAX_SERVER_BOUND_BYTES, manager.getOptionTree(), SyncMode.OVERRIDE_CLIENT, manager.local());
            saveRemoteChange(player);
        }
        broadcastServerConfig();
    }

    private void saveRemoteChange(ServerPlayer player) {
        try {
            manager.save();
        } catch (UncheckedIOException e) {
            LOGGER.error("Applied the config change from {} to '{}' but could not save it: {}", player.getScoreboardName(), manager.getName(), e.getMessage());
        }
    }

    /**
     * Handles an incoming C2S client inform notification payload on the server.
     *
     * @param player The sender player.
     * @param buf The buffer containing the payload data.
     */
    void handleClientInformPayload(ServerPlayer player, FriendlyByteBuf buf) {
        manager.acceptInformedConfig(player, informedConfig(buf));
    }

    T informedConfig(FriendlyByteBuf buf) {
        T informed = manager.cloneConfig(manager.local());
        ConfigNetworkPayload.readAndApply(buf, ConfigNetworkPayload.MAX_SERVER_BOUND_BYTES, manager.getOptionTree(), SyncMode.INFORM_SERVER, informed);
        return informed;
    }

    /**
     * Serializes the current server config and pushes it to a specific player.
     * Typically called during player join events on the server side.
     *
     * @param player The target player to sync the config to.
     */
    void sendConfigToPlayer(ServerPlayer player) {
        // Prevent local loopback race condition: don't send the S2C sync packet to the integrated server host.
        if (transport.isSingleplayerOwner(player)) return;
        sendServerConfig(player, serverConfigBytes(serverSnapshot(), canChangeOverrides(player)));
    }

    void broadcastServerConfig() {
        String options = serverSnapshot();
        Map<Boolean, byte[]> messagesByPermission = new HashMap<>(2);
        for (ServerPlayer player : transport.remotePlayers()) {
            byte[] json = messagesByPermission.computeIfAbsent(canChangeOverrides(player), canChange -> serverConfigBytes(options, canChange));
            sendServerConfig(player, json);
        }
    }

    private void sendServerConfig(ServerPlayer player, byte[] json) {
        if (fits(json, ConfigNetworkPayload.MAX_PAYLOAD_BYTES, () -> "player " + player.getScoreboardName())) {
            transport.sendToClient(player, channels.serverConfig(), json);
        }
    }

    private static byte[] serverConfigBytes(String options, boolean canChangeOverrides) {
        return ConfigNetworkPayload.serverConfig(options, canChangeOverrides).getBytes(StandardCharsets.UTF_8);
    }

    private boolean canChangeOverrides(ServerPlayer player) {
        return transport.isSingleplayerOwner(player) || manager.checkServerPermission(player);
    }

    private String serverSnapshot() {
        return ConfigNetworkPayload.snapshot(manager.getOptionTree(), manager.local(), SyncMode.OVERRIDE_CLIENT);
    }

    /**
     * Sends the changed override options to the server.
     *
     * @param overrides The override options that changed and their new values.
     * @return false if the payload is over the server-bound limit and nothing was sent.
     */
    boolean sendOverridesToServer(List<OptionChange> overrides) {
        return sendToServer(channels.clientOverride(), ConfigNetworkPayload.changes(manager.getOptionTree(), overrides));
    }

    /**
     * Asks the integrated server to broadcast the host's config to LAN players.
     * The host shares its config instance with the integrated server, so the payload carries no options.
     */
    void requestOverrideBroadcast() {
        sendToServer(channels.clientOverride(), ConfigNetworkPayload.changes(manager.getOptionTree(), List.of()));
    }

    /**
     * Sends a C2S packet to inform the server of local preferences.
     */
    void sendInformToServer() {
        sendToServer(channels.clientInform(), ConfigNetworkPayload.snapshot(manager.getOptionTree(), manager.local(), SyncMode.INFORM_SERVER));
    }

    private boolean sendToServer(ResourceLocation packet, String json) {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        if (!fits(bytes, ConfigNetworkPayload.MAX_SERVER_BOUND_BYTES, () -> "the server")) return false;
        transport.sendToServer(packet, bytes);
        return true;
    }

    private boolean fits(byte[] json, int maxBytes, Supplier<String> destination) {
        if (json.length <= maxBytes) return true;

        LOGGER.error("Sync of config '{}' to {} is {} bytes, limit is {}; nothing was sent", manager.getName(), destination.get(), json.length, maxBytes);
        return false;
    }
}
