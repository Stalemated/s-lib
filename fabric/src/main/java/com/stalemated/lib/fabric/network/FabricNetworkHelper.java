package com.stalemated.lib.fabric.network;

import com.stalemated.lib.network.NetworkHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class FabricNetworkHelper implements NetworkHelper {

    @Override
    public void sendToClient(ServerPlayerEntity player, Identifier id, PacketByteBuf buf) {
        ServerPlayNetworking.send(player, id, buf);
    }

    @Override
    public void sendToServer(Identifier id, PacketByteBuf buf) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            FabricClientNetworkDelegator.sendToServer(id, buf);
        }
    }

    @Override
    public void registerServerReceiver(Identifier id, ServerReceiver receiver) {
        ServerPlayNetworking.registerGlobalReceiver(
                id, (
                        server,
                        player,
                        handler,
                        buf,
                        responseSender
                ) -> receiver.receive(player, buf)
        );
    }

    @Override
    public void registerClientReceiver(Identifier id, ClientReceiver receiver) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            FabricClientNetworkDelegator.registerClientReceiver(id, receiver);
        }
    }
}
