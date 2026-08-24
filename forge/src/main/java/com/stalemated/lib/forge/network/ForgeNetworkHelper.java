 package com.stalemated.lib.forge.network;

import com.stalemated.lib.network.NetworkHelper;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@SuppressWarnings("removal")
public class ForgeNetworkHelper implements NetworkHelper {
    private static final String PROTOCOL_VERSION = "1";
    private static final Map<Identifier, ServerReceiver> SERVER_RECEIVERS = new HashMap<>();
    private static final Map<Identifier, ClientReceiver> CLIENT_RECEIVERS = new HashMap<>();

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new Identifier("s_lib", "network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    static {
        CHANNEL.registerMessage(0, WrapperPacket.class, WrapperPacket::encode, WrapperPacket::new, ForgeNetworkHelper::handle);
    }

    @Override
    public void sendToClient(ServerPlayerEntity player, Identifier id, PacketByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new WrapperPacket(id, data));
    }

    @Override
    public void sendToServer(Identifier id, PacketByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        CHANNEL.sendToServer(new WrapperPacket(id, data));
    }

    @Override
    public void registerServerReceiver(Identifier id, ServerReceiver receiver) {
        SERVER_RECEIVERS.put(id, receiver);
    }

    @Override
    public void registerClientReceiver(Identifier id, ClientReceiver receiver) {
        CLIENT_RECEIVERS.put(id, receiver);
    }

    public static void handle(WrapperPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            Identifier id = packet.id;
            PacketByteBuf buf = new PacketByteBuf(Unpooled.wrappedBuffer(packet.data));

            if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                ServerReceiver receiver = SERVER_RECEIVERS.get(id);
                if (receiver != null) {
                    receiver.receive(context.getSender(), buf);
                }
            } else if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                ClientReceiver receiver = CLIENT_RECEIVERS.get(id);
                if (receiver != null) {
                    receiver.receive(buf);
                }
            }
        });
        context.setPacketHandled(true);
    }

    public static class WrapperPacket {
        public final Identifier id;
        public final byte[] data;

        public WrapperPacket(Identifier id, byte[] data) {
            this.id = id;
            this.data = data;
        }

        public WrapperPacket(PacketByteBuf buf) {
            this.id = buf.readIdentifier();
            this.data = buf.readByteArray();
        }

        public void encode(PacketByteBuf buf) {
            buf.writeIdentifier(this.id);
            buf.writeByteArray(this.data);
        }
    }
}
