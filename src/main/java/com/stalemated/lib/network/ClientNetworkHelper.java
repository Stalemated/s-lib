//? if fabric {
package com.stalemated.lib.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//? if >=1.20.5
//import com.stalemated.lib.network.WrapperPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientNetworkHelper {

    //? if <1.20.5 {
    public static void sendToServer(ResourceLocation id, FriendlyByteBuf buf) {
        ClientPlayNetworking.send(id, buf);
    }
    //?} else {
    /*public static void sendToServer(WrapperPayload payload) {
        ClientPlayNetworking.send(payload);
    }
    *///?}

    //? if >=1.20.5 {
    /*public static void registerClientReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(WrapperPayload.ID, (payload, context) -> {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()));
            NetworkHelper.ClientReceiver receiver = NetworkHelper.CLIENT_RECEIVERS.get(payload.channelId());
            if (receiver != null) {
                receiver.receive(buf);
            }
        });
    }
    *///?}

    //? if <1.20.5 {
    public static void registerClientReceiver(ResourceLocation id, NetworkHelper.ClientReceiver receiver) {
        ClientPlayNetworking.registerGlobalReceiver(id, (client, handler, buf, responseSender) -> {
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            client.execute(() -> receiver.receive(new FriendlyByteBuf(Unpooled.wrappedBuffer(data))));
        });
    }
    //?}
}
//?}
