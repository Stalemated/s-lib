package com.stalemated.lib.fabric.network;

import com.stalemated.lib.network.NetworkHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class FabricClientNetworkDelegator {
    public static void sendToServer(Identifier id, PacketByteBuf buf) {
        ClientPlayNetworking.send(id, buf);
    }

    public static void registerClientReceiver(Identifier id, NetworkHelper.ClientReceiver receiver) {
        ClientPlayNetworking.registerGlobalReceiver(id,
                (
                        client,
                        handler,
                        buf,
                        responseSender
                ) -> receiver.receive(buf));
    }
}
