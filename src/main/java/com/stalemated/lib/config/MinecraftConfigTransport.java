package com.stalemated.lib.config;

import com.stalemated.lib.helper.PlatformHelper;
import com.stalemated.lib.network.NetworkHelper;
import net.minecraft.network.FriendlyByteBuf;
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
    public void sendToClient(ServerPlayer player, ResourceLocation channel, FriendlyByteBuf buf) {
        NetworkHelper.sendToClient(player, channel, buf);
    }

    @Override
    public void sendToServer(ResourceLocation channel, FriendlyByteBuf buf) {
        NetworkHelper.sendToServer(channel, buf);
    }

    @Override
    public boolean isSingleplayerOwner(ServerPlayer player) {
        return PlatformHelper.isSingleplayerOwner(player);
    }

    @Override
    public List<ServerPlayer> playersOnServerOf(ServerPlayer player) {
        MinecraftServer server = PlatformHelper.getServer(player);
        return server == null ? List.of() : server.getPlayerList().getPlayers();
    }
}
