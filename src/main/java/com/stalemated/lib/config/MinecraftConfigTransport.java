package com.stalemated.lib.config;

import com.stalemated.lib.config.network.ConfigNetworkPayload;
import com.stalemated.lib.helper.PlatformHelper;
import com.stalemated.lib.network.NetworkHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

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
    public List<ServerPlayer> remotePlayers() {
        MinecraftServer server = ConfigRegistry.currentServer();
        if (server == null) return List.of();

        return server.getPlayerList().getPlayers().stream()
                .filter(player -> !isSingleplayerOwner(player))
                .toList();
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
