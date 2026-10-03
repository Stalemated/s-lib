package com.stalemated.lib.config.network;

import com.stalemated.lib.config.manager.SyncedConfigManager;
import com.stalemated.lib.network.NetworkHelper;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ConfigNetworkHandler<T> {

    private final SyncedConfigManager<T> manager;
    private final ResourceLocation s2cPacket;
    private final ResourceLocation c2sPacket;
    private final ResourceLocation informC2sPacket;

    public ConfigNetworkHandler(SyncedConfigManager<T> manager, ResourceLocation s2cPacket, ResourceLocation c2sPacket, ResourceLocation informC2sPacket) {
        this.manager = manager;
        this.s2cPacket = s2cPacket;
        this.c2sPacket = c2sPacket;
        this.informC2sPacket = informC2sPacket;
    }

    public void registerReceivers() {
        registerClientReceivers();
        registerServerReceivers();
    }

    private void registerClientReceivers() {
        NetworkHelper.registerClientReceiver(s2cPacket, buf -> {
            if (manager.getConnectionState() == ConnectionState.SINGLEPLAYER) {
                return; // Ignore network loopback in singleplayer
            }
            T target = manager.cloneConfig(manager.getConfig());
            ConfigNetworkPayload.readAndApply(buf, manager.getOptionTree(), target, manager.getProvider().getSerializer());
            manager.setServerConfig(target);
            manager.setConnectionState(ConnectionState.MULTIPLAYER_MODDED);
            manager.notifySyncListeners(target);
        });
    }

    private void registerServerReceivers() {
        NetworkHelper.registerServerReceiver(c2sPacket, (player, buf) -> {
            if (manager.checkServerPermission(player)) {
                boolean isHost = player != null && player.server != null && player.server.isSingleplayerOwner(player.getGameProfile());
                
                if (!isHost) {
                    ConfigNetworkPayload.readAndApply(buf, manager.getOptionTree(), manager.getConfig(), manager.getProvider().getSerializer());
                    manager.save();
                    manager.notifySyncListeners(manager.getConfig());
                }

                if (player != null && player.server != null) {
                    for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
                        sendConfigToPlayer(p);
                    }
                }
            } else {
                // If rejected, send back the config to avoid desyncs
                if (player != null) {
                    sendConfigToPlayer(player);
                }
            }
        });

        NetworkHelper.registerServerReceiver(informC2sPacket, (player, buf) -> {
            // Read informed data into a temporary clone of the default config
            T tempConfig = manager.createDefaultOrClone();
            ConfigNetworkPayload.readAndApply(buf, manager.getOptionTree(), tempConfig, manager.getProvider().getSerializer());
            
            // Notify listeners about this player's specific choices
            manager.notifyInformListeners(player, tempConfig);
        });
    }

    /**
     * Serializes the current server config and pushes it to a specific player.
     * Typically called during player join events on the server side.
     *
     * @param player The target player to sync the config to.
     */
    public void sendConfigToPlayer(ServerPlayer player) {
        // Prevent local loopback race condition: don't send the S2C sync packet to the integrated server host.
        if (player.server != null && player.server.isSingleplayerOwner(player.getGameProfile())) return;

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ConfigNetworkPayload.writeSynced(buf, manager.getOptionTree(), manager.getConfig(), manager.getProvider().getSerializer());
        NetworkHelper.sendToClient(player, s2cPacket, buf);
    }

    /**
     * Sends a C2S packet with the requested config overrides.
     */
    public void sendOverridePacketToServer() {
        T serverConfig = manager.getServerConfig();
        if (serverConfig != null) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            ConfigNetworkPayload.writeSynced(buf, manager.getOptionTree(), serverConfig, manager.getProvider().getSerializer());
            NetworkHelper.sendToServer(c2sPacket, buf);
        }
    }

    /**
     * Sends a C2S packet to inform the server of local preferences.
     */
    public void sendInformPacketToServer() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ConfigNetworkPayload.write(buf, manager.getOptionTree(), manager.getConfig(), SyncMode.INFORM_SERVER, manager.getProvider().getSerializer());
        NetworkHelper.sendToServer(informC2sPacket, buf);
    }

    /**
     * Sends a C2S packet with the requested config overrides using the local config.
     * Only used by the host in Singleplayer/LAN environments to wake up the integrated server.
     */
    public void sendLocalOverridePacketToServer() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ConfigNetworkPayload.writeSynced(buf, manager.getOptionTree(), manager.getConfig(), manager.getProvider().getSerializer());
        NetworkHelper.sendToServer(c2sPacket, buf);
    }

    /**
     * Sends a C2S inform packet using the local config.
     * Only used by the host in Singleplayer/LAN environments to wake up the integrated server.
     */
    public void sendLocalInformPacketToServer() {
        sendInformPacketToServer();
    }
}
