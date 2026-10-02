package com.stalemated.lib.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

//? if fabric && <1.20.5
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
//? if fabric && >=1.20.5
/*import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
*/
/*import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;*/
//? if fabric
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

//? if forge
/*import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.function.Supplier;*/

//? if neoforge
/*import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;*/

//? if >=1.20.5 {
/*import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;*/
//?}

//? if neoforge
/*@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD)*/
public class NetworkHelper {

    public static final Map<ResourceLocation, ServerReceiver> SERVER_RECEIVERS = new HashMap<>();
    public static final Map<ResourceLocation, ClientReceiver> CLIENT_RECEIVERS = new HashMap<>();

    //? if forge {
    /*private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MOD_ID, "network"),
            () -> PROTOCOL_VERSION,
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION),
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION)
    );

    static {
        CHANNEL.registerMessage(0, WrapperPacket.class, WrapperPacket::encode, WrapperPacket::new, NetworkHelper::handleForge);
    }*/
    //?}

    //? if >=1.20.5 {
    /*public record WrapperPayload(ResourceLocation channelId, byte[] data) implements CustomPayload {
        //? if >=1.21
        public static final CustomPayload.Id<WrapperPayload> ID = new CustomPayload.Id<>(ResourceLocation.of("s_lib", "network"));
        //? if <1.21
        ^public static final CustomPayload.Id<WrapperPayload> ID = new CustomPayload.Id<>(new ResourceLocation("s_lib", "network"));

        public static final PacketCodec<PacketByteBuf, WrapperPayload> CODEC = PacketCodec.tuple(
                ResourceLocation.PACKET_CODEC, WrapperPayload::channelId,
                PacketCodecs.BYTE_ARRAY, WrapperPayload::data,
                WrapperPayload::new
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }*/
    //?}

    //? if forge {
    /*public static class WrapperPacket {
        public final ResourceLocation id;
        public final byte[] data;

        public WrapperPacket(ResourceLocation id, byte[] data) {
            this.id = id;
            this.data = data;
        }

        public WrapperPacket(PacketByteBuf buf) {
            this.id = buf.readResourceLocation();
            this.data = buf.readByteArray();
        }

        public void encode(PacketByteBuf buf) {
            buf.writeResourceLocation(this.id);
            buf.writeByteArray(this.data);
        }
    }

    public static void handleForge(WrapperPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ResourceLocation id = packet.id;
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
    }*/
    //?}

    //? if neoforge {
    /*@SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("s_lib").optional();
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
    }*/
    //?}

    /**
     * Initializes network registry. Call this in common initialization.
     */
    public static void init() {
        //? if fabric && >=1.20.5 {
        /*PayloadTypeRegistry.playC2S().register(WrapperPayload.ID, WrapperPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WrapperPayload.ID, WrapperPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(WrapperPayload.ID, (payload, context) -> {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.wrappedBuffer(payload.data()));
            ServerReceiver receiver = SERVER_RECEIVERS.get(payload.channelId());
            if (receiver != null) {
                receiver.receive(context.player(), buf);
            }
        });

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientNetworkHelper.registerFabricClientReceiver();
        }*/
        //?}
    }

    public static void sendToClient(ServerPlayerEntity player, ResourceLocation id, PacketByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        
        //? if fabric && >=1.20.5
        /*ServerPlayNetworking.send(player, new WrapperPayload(id, data));*/
        //? if fabric && <1.20.5
        ServerPlayNetworking.send(player, id, new PacketByteBuf(Unpooled.wrappedBuffer(data)));
        //? if forge
        /*CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new WrapperPacket(id, data));*/
        //? if neoforge
        /*PacketDistributor.sendToPlayer(player, new WrapperPayload(id, data));*/
    }

    public static void sendToServer(ResourceLocation id, PacketByteBuf buf) {
        //? if fabric
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientNetworkHelper.sendToServer(id, buf);
        }
        //? if forge {
        /*byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        CHANNEL.sendToServer(new WrapperPacket(id, data));*/
        //?}
        //? if neoforge {
        /*byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        PacketDistributor.sendToServer(new WrapperPayload(id, data));*/
        //?}
    }

    public static void registerServerReceiver(ResourceLocation id, ServerReceiver receiver) {
        SERVER_RECEIVERS.put(id, receiver);
        //? if fabric && <1.20.5 {
        ServerPlayNetworking.registerGlobalReceiver(id, (server, player, handler, buf, responseSender) -> {
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            server.execute(() -> receiver.receive(player, new PacketByteBuf(Unpooled.wrappedBuffer(data))));
        });
        //?}
    }

    public static void registerClientReceiver(ResourceLocation id, ClientReceiver receiver) {
        CLIENT_RECEIVERS.put(id, receiver);
        //? if fabric && <1.20.5 {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientNetworkHelper.registerFabricClientReceiverLegacy(id);
        }
        //?}
    }

    @FunctionalInterface
    public interface ServerReceiver {
        void receive(ServerPlayerEntity player, PacketByteBuf buf);
    }

    @FunctionalInterface
    public interface ClientReceiver {
        void receive(PacketByteBuf buf);
    }
}
