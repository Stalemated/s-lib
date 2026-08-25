package com.stalemated.lib.neoforge.network;

import com.stalemated.lib.network.AbstractNetworkHelper;
import com.stalemated.lib.network.WrapperPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = "s_lib")
public class NeoForgeNetworkHelper extends AbstractNetworkHelper {
    public static final Map<Identifier, ClientReceiver> CLIENT_RECEIVERS = new HashMap<>();

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("s_lib");
        registrar.playBidirectional(
            WrapperPayload.ID,
            WrapperPayload.CODEC,
            (payload, context) -> {
                PacketByteBuf buf = new PacketByteBuf(Unpooled.wrappedBuffer(payload.data()));
                
                context.enqueueWork(() -> {
                    if (context.flow().isServerbound()) {
                        ServerReceiver receiver = SERVER_RECEIVERS.get(payload.channelId());
                        if (receiver != null && context.player() instanceof ServerPlayerEntity serverPlayer) {
                            receiver.receive(serverPlayer, buf);
                        }
                    } else {
                        ClientReceiver receiver = CLIENT_RECEIVERS.get(payload.channelId());
                        if (receiver != null) {
                            receiver.receive(buf);
                        }
                    }
                });
            }
        );
    }

    @Override
    public void sendToClient(ServerPlayerEntity player, Identifier id, PacketByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        PacketDistributor.sendToPlayer(player, new WrapperPayload(id, data));
    }

    @Override
    public void sendToServer(Identifier id, PacketByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        PacketDistributor.sendToServer(new WrapperPayload(id, data));
    }

    @Override
    public void registerClientReceiver(Identifier id, ClientReceiver receiver) {
        CLIENT_RECEIVERS.put(id, receiver);
    }
}
