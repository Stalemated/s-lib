//? if neoforge && >1.20.4 || fabric {
package com.stalemated.lib.network.internal;

//? if fabric {
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
//? if <1.20.5 {
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
//?}
//?} else {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
//? if <1.21.7
import net.neoforged.neoforge.network.PacketDistributor;
//? if >=1.21.7
//import net.neoforged.neoforge.client.network.ClientPacketDistributor;
*///?}

final class ClientNetworkHelper {

    private ClientNetworkHelper() {}

    //? if fabric && <1.20.5 {
    static void sendToServer(ResourceLocation channel, FriendlyByteBuf buf) {
        ClientPlayNetworking.send(channel, buf);
    }

    static void registerClientReceiver(ResourceLocation channel) {
        ClientPlayNetworking.registerGlobalReceiver(channel, (client, handler, buf, responseSender) -> {
            ResourceLocation configChannel = buf.readResourceLocation();
            byte[] data = PacketReceivers.readData(buf);
            client.execute(() -> PacketReceivers.dispatchToClient(configChannel, data));
        });
    }
    //?} elif fabric {
    /*static void sendToServer(WrapperPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    static void registerClientReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(WrapperPayload.ID, (payload, context) ->
                PacketReceivers.dispatchToClient(payload.channelId(), payload.data()));
    }
    *///?} else {
    /*static void sendToServer(WrapperPayload payload) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null || !connection.hasChannel(WrapperPayload.ID)) return;
        //? if <1.21.7
        PacketDistributor.sendToServer(payload);
        //? if >=1.21.7
        //ClientPacketDistributor.sendToServer(payload);
    }
    *///?}
}
//?}
