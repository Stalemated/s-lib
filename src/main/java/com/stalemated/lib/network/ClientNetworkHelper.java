package com.stalemated.lib.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.ResourceLocation;

//? if fabric
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientNetworkHelper {

    public static void sendToServer(ResourceLocation id, PacketByteBuf buf) {
        //? if fabric {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        //? if >=1.20.5
        /*ClientPlayNetworking.send(new NetworkHelper.WrapperPayload(id, data));*/
        //? if <1.20.5
        ClientPlayNetworking.send(id, new PacketByteBuf(Unpooled.wrappedBuffer(data)));
        //?}
    }

    //? if fabric && >=1.20.5 {
    /*public static void registerFabricClientReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(NetworkHelper.WrapperPayload.ID, (payload, context) -> {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.wrappedBuffer(payload.data()));
            NetworkHelper.ClientReceiver receiver = NetworkHelper.CLIENT_RECEIVERS.get(payload.channelId());
            if (receiver != null) {
                receiver.receive(buf);
            }
        });
    }*/
    //?}

    //? if fabric && <1.20.5 {
    public static void registerFabricClientReceiverLegacy(ResourceLocation id) {
        ClientPlayNetworking.registerGlobalReceiver(id, (client, handler, buf, responseSender) -> {
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            NetworkHelper.ClientReceiver receiver = NetworkHelper.CLIENT_RECEIVERS.get(id);
            if (receiver != null) {
                client.execute(() -> receiver.receive(new PacketByteBuf(Unpooled.wrappedBuffer(data))));
            }
        });
    }
    //?}
}
