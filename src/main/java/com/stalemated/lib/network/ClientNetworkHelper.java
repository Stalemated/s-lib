//? if fabric {
package com.stalemated.lib.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//? if <1.20.5 {
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//?}

public class ClientNetworkHelper {

    //? if <1.20.5 {
    public static void sendToServer(ResourceLocation channel, FriendlyByteBuf buf) {
        ClientPlayNetworking.send(channel, buf);
    }

    public static void registerClientReceiver(ResourceLocation channel) {
        ClientPlayNetworking.registerGlobalReceiver(channel, (client, handler, buf, responseSender) -> {
            ResourceLocation configChannel = buf.readResourceLocation();
            byte[] data = buf.readByteArray();
            client.execute(() -> NetworkHelper.dispatchToClient(configChannel, data));
        });
    }
    //?} else {
    /*public static void sendToServer(WrapperPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    public static void registerClientReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(WrapperPayload.ID, (payload, context) ->
                NetworkHelper.dispatchToClient(payload.channelId(), payload.data()));
    }
    *///?}
}
//?}
