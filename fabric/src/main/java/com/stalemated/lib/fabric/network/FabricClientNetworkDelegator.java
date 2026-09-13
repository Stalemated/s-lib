package com.stalemated.lib.fabric.network;

import com.stalemated.lib.network.NetworkHelper;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class FabricClientNetworkDelegator {
    public static void sendToServer(Identifier id, PacketByteBuf buf) {
        ClientPlayNetworking.send(id, buf);
    }

    public static void registerClientReceiver(Identifier id, NetworkHelper.ClientReceiver receiver) {
        ClientPlayNetworking.registerGlobalReceiver(
                id, (
                        client,
                        handler,
                        buf,
                        responseSender
                ) -> {
                    byte[] data = new byte[buf.readableBytes()];
                    buf.readBytes(data);
                    client.execute(() -> receiver.receive(new PacketByteBuf(Unpooled.wrappedBuffer(data))));
                });
    }
}
