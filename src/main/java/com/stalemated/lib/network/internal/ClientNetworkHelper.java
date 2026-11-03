//? if fabric {
package com.stalemated.lib.network.internal;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//? if <1.20.5 {
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//?}

final class ClientNetworkHelper {

    private ClientNetworkHelper() {}

    //? if <1.20.5 {
    static void sendToServer(ResourceLocation channel, FriendlyByteBuf buf) {
        ClientPlayNetworking.send(channel, buf);
    }

    static void registerClientReceiver(ResourceLocation channel) {
        ClientPlayNetworking.registerGlobalReceiver(channel, (client, handler, buf, responseSender) -> {
            ResourceLocation configChannel = buf.readResourceLocation();
            byte[] data = buf.readByteArray();
            client.execute(() -> PacketReceivers.dispatchToClient(configChannel, data));
        });
    }
    //?} else {
    /*static void sendToServer(WrapperPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    static void registerClientReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(WrapperPayload.ID, (payload, context) ->
                PacketReceivers.dispatchToClient(payload.channelId(), payload.data()));
    }
    *///?}
}
//?}
