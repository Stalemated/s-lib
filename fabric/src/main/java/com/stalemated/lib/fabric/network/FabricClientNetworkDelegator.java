package com.stalemated.lib.fabric.network;

import com.stalemated.lib.network.NetworkHelper;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.stalemated.lib.network.WrapperPayload;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class FabricClientNetworkDelegator {
    public static final Map<Identifier, NetworkHelper.ClientReceiver> CLIENT_RECEIVERS = new HashMap<>();

    public static void sendToServer(WrapperPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    public static void registerClientReceiver(Identifier id, NetworkHelper.ClientReceiver receiver) {
        CLIENT_RECEIVERS.put(id, receiver);
    }

    public static void registerClientPayloadReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(WrapperPayload.ID, (payload, context) -> {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.wrappedBuffer(payload.data()));
            NetworkHelper.ClientReceiver receiver = CLIENT_RECEIVERS.get(payload.channelId());
            if (receiver != null) {
                receiver.receive(buf);
            }
        });
    }
}
