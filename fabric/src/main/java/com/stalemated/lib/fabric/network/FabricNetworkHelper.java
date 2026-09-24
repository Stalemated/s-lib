package com.stalemated.lib.fabric.network;

import com.stalemated.lib.network.AbstractNetworkHelper;
import com.stalemated.lib.network.WrapperPayload;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class FabricNetworkHelper extends AbstractNetworkHelper {

    public static void registerPayloads() {
        PayloadTypeRegistry.playC2S().register(WrapperPayload.ID, WrapperPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WrapperPayload.ID, WrapperPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(WrapperPayload.ID, (payload, context) -> {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.wrappedBuffer(payload.data()));
            ServerReceiver receiver = SERVER_RECEIVERS.get(payload.channelId());
            if (receiver != null) {
                receiver.receive(context.player(), buf);
            }
        });

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            FabricClientNetworkDelegator.registerClientPayloadReceiver();
        }
    }

    @Override
    public void sendToClient(ServerPlayerEntity player, Identifier id, PacketByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        ServerPlayNetworking.send(player, new WrapperPayload(id, data));
    }

    @Override
    public void sendToServer(Identifier id, PacketByteBuf buf) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            FabricClientNetworkDelegator.sendToServer(new WrapperPayload(id, data));
        }
    }

    @Override
    public void registerClientReceiver(Identifier id, ClientReceiver receiver) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            FabricClientNetworkDelegator.registerClientReceiver(id, receiver);
        }
    }
}
