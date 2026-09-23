package com.stalemated.lib.config.network;

import com.stalemated.lib.config.manager.SyncedConfigManager;
import com.stalemated.lib.network.NetworkHelper;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ConfigNetworkHandler<T> {

    private final SyncedConfigManager<T> manager;
    private final Identifier s2cPacket;
    private final Identifier c2sPacket;
    private final Identifier informC2sPacket;

    public ConfigNetworkHandler(SyncedConfigManager<T> manager, Identifier s2cPacket, Identifier c2sPacket, Identifier informC2sPacket) {
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
        NetworkHelper.INSTANCE.registerClientReceiver(s2cPacket, buf -> {
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
        NetworkHelper.INSTANCE.registerServerReceiver(c2sPacket, (player, buf) -> {
            if (manager.checkServerPermission(player)) {
                ConfigNetworkPayload.readAndApply(buf, manager.getOptionTree(), manager.getConfig(), manager.getProvider().getSerializer());
                manager.save();
                manager.notifySyncListeners(manager.getConfig());

                if (player != null && player.server != null) {
                    for (ServerPlayerEntity p : player.server.getPlayerManager().getPlayerList()) {
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

        NetworkHelper.INSTANCE.registerServerReceiver(informC2sPacket, (player, buf) -> {
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
    public void sendConfigToPlayer(ServerPlayerEntity player) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        ConfigNetworkPayload.writeSynced(buf, manager.getOptionTree(), manager.getConfig(), manager.getProvider().getSerializer());
        NetworkHelper.INSTANCE.sendToClient(player, s2cPacket, buf);
    }

    /**
     * Sends a C2S packet with the requested config overrides.
     */
    public void sendOverridePacketToServer() {
        T serverConfig = manager.getServerConfig();
        if (serverConfig != null) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            ConfigNetworkPayload.writeSynced(buf, manager.getOptionTree(), serverConfig, manager.getProvider().getSerializer());
            NetworkHelper.INSTANCE.sendToServer(c2sPacket, buf);
        }
    }

    /**
     * Sends a C2S packet to inform the server of local preferences.
     */
    public void sendInformPacketToServer() {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        ConfigNetworkPayload.write(buf, manager.getOptionTree(), manager.getConfig(), SyncMode.INFORM_SERVER, manager.getProvider().getSerializer());
        NetworkHelper.INSTANCE.sendToServer(informC2sPacket, buf);
    }
}
