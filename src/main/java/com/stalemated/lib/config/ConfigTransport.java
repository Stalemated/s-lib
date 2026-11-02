package com.stalemated.lib.config;

import com.stalemated.lib.network.NetworkHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

interface ConfigTransport {

    void registerClientReceiver(ResourceLocation channel, NetworkHelper.ClientReceiver receiver);

    void registerServerReceiver(ResourceLocation channel, NetworkHelper.ServerReceiver receiver);

    void unregisterClientReceiver(ResourceLocation channel);

    void unregisterServerReceiver(ResourceLocation channel);

    void sendToClient(ServerPlayer player, ResourceLocation channel, String json);

    List<ServerPlayer> remotePlayers();

    void sendToServer(ResourceLocation channel, String json);

    boolean isSingleplayerOwner(ServerPlayer player);
}
