package com.stalemated.lib.config;

import com.stalemated.lib.config.network.ConfigNetworkPayload;
import com.stalemated.lib.helper.PlatformHelper;
import com.stalemated.lib.network.NetworkHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

final class MinecraftConfigTransport implements ConfigTransport {

    static final ConfigTransport INSTANCE = new MinecraftConfigTransport();

    private MinecraftConfigTransport() {}

    @Override
    public void registerClientReceiver(ResourceLocation channel, NetworkHelper.ClientReceiver receiver) {
        NetworkHelper.registerClientReceiver(channel, receiver);
    }

    @Override
    public void registerServerReceiver(ResourceLocation channel, NetworkHelper.ServerReceiver receiver) {
        NetworkHelper.registerServerReceiver(channel, receiver);
    }

    @Override
    public void unregisterClientReceiver(ResourceLocation channel) {
        NetworkHelper.unregisterClientReceiver(channel);
    }

    @Override
    public void unregisterServerReceiver(ResourceLocation channel) {
        NetworkHelper.unregisterServerReceiver(channel);
    }

    @Override
    public void sendToClient(ServerPlayer player, ResourceLocation channel, String json) {
        NetworkHelper.sendToClient(player, channel, ConfigNetworkPayload.encode(json));
    }

    @Override
    @SuppressWarnings("resource")
    public void sendToAllClients(ResourceLocation channel, String json) {
        MinecraftServer server = ConfigRegistry.currentServer();
        if (server == null) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!isSingleplayerOwner(player)) {
                sendToClient(player, channel, json);
            }
        }
    }

    @Override
    public void sendToServer(ResourceLocation channel, String json) {
        NetworkHelper.sendToServer(channel, ConfigNetworkPayload.encode(json));
    }

    @Override
    public boolean isSingleplayerOwner(ServerPlayer player) {
        return PlatformHelper.isSingleplayerOwner(player);
    }
}
