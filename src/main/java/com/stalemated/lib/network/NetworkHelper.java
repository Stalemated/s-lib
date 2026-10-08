package com.stalemated.lib.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

//? if fabric{
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
    //?if >=1.20.5 {
    /*import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
    import java.util.HashMap;
    import java.util.Map;
    *///?}
//?}

//? if forge{
/*import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.function.Supplier;
import java.util.HashMap;
import java.util.Map;

import static com.stalemated.lib.SLib.MOD_ID;
*///?}

//? if neoforge{
/*import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.bus.api.SubscribeEvent;

//? if <=1.20.4 {
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import net.neoforged.fml.common.Mod.EventBusSubscriber;
//?} else {
/^import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.fml.common.EventBusSubscriber;
^///?}

//? if >=1.21.11
/^import net.neoforged.neoforge.client.network.ClientPacketDistributor;^/

import java.util.HashMap;
import java.util.Map;

import static com.stalemated.lib.SLib.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
*///?}
public class NetworkHelper {

    //? if >=1.20.5 || forge || neoforge {
    /*public static final Map<ResourceLocation, ServerReceiver> SERVER_RECEIVERS = new HashMap<>();
    public static final Map<ResourceLocation, ClientReceiver> CLIENT_RECEIVERS = new HashMap<>();
    *///?}

    //? if forge {
    /*private static final String PROTOCOL_VERSION = "1";

    @SuppressWarnings("removal")
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MOD_ID, "network"),
            () -> PROTOCOL_VERSION,
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION),
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION)
    );

    static {
        CHANNEL.registerMessage(0, WrapperPacket.class, WrapperPacket::encode, WrapperPacket::new, NetworkHelper::handlePacket);
    }

    public static class WrapperPacket {
        public final ResourceLocation id;
        public final byte[] data;

        public WrapperPacket(ResourceLocation id, byte[] data) {
            this.id = id;
            this.data = data;
        }

        public WrapperPacket(FriendlyByteBuf buf) {
            this.id = buf.readResourceLocation();
            this.data = buf.readByteArray();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeResourceLocation(this.id);
            buf.writeByteArray(this.data);
        }
    }

    public static void handlePacket(WrapperPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            ResourceLocation id = packet.id;
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(packet.data));

            if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                ServerReceiver receiver = SERVER_RECEIVERS.get(id);
                if (receiver != null) receiver.receive(context.getSender(), buf);

            } else if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                ClientReceiver receiver = CLIENT_RECEIVERS.get(id);
                if (receiver != null) receiver.receive(buf);
            }
        });
        context.setPacketHandled(true);
    }
    *///?}

    //? if neoforge && <=1.20.4 {
    /*@SubscribeEvent
    public static void register(RegisterPayloadHandlerEvent event) {
        IPayloadRegistrar registrar = event.registrar(MOD_ID).optional();
        registrar.play(
                WrapperPayload.ID,
                WrapperPayload::new,
                NetworkHelper::handlePayload
        );
    }
    *///?} elif neoforge {
    /*@SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MOD_ID).optional();
        registrar.playBidirectional(
                WrapperPayload.ID,
                WrapperPayload.CODEC,
                NetworkHelper::handlePayload
        );
    }
    *///?}

    //? if neoforge && <=1.20.4 {
    /*private static void handlePayload(WrapperPayload payload, IPayloadContext context) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()));

        context.workHandler().execute(() -> {
            if (context.flow().isServerbound()) {
                ServerReceiver receiver = SERVER_RECEIVERS.get(payload.channelId());
                if (receiver != null && context.player().orElse(null) instanceof ServerPlayer serverPlayer) {
                    receiver.receive(serverPlayer, buf);
                }
            } else {
                ClientReceiver receiver = CLIENT_RECEIVERS.get(payload.channelId());
                if (receiver != null) receiver.receive(buf);
            }
        });
    }
    *///?} elif neoforge {
    /*private static void handlePayload(WrapperPayload payload, IPayloadContext context) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()));

        context.enqueueWork(() -> {
            if (context.flow().isServerbound()) {
                ServerReceiver receiver = SERVER_RECEIVERS.get(payload.channelId());
                if (receiver != null && context.player() instanceof ServerPlayer serverPlayer) {
                    receiver.receive(serverPlayer, buf);
                }
            } else {
                ClientReceiver receiver = CLIENT_RECEIVERS.get(payload.channelId());
                if (receiver != null) receiver.receive(buf);
            }
        });
    }
    *///?}

    //? if fabric && >=1.20.5 {
    /*public static void registerPayloads() {

        //? if <26.1 {
        PayloadTypeRegistry.playC2S().register(WrapperPayload.ID, WrapperPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WrapperPayload.ID, WrapperPayload.CODEC);
        //?} else {
        /^PayloadTypeRegistry.serverboundPlay().register(WrapperPayload.ID, WrapperPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(WrapperPayload.ID, WrapperPayload.CODEC);
        ^///?}

        ServerPlayNetworking.registerGlobalReceiver(WrapperPayload.ID, (payload, context) -> {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()));
            ServerReceiver receiver = SERVER_RECEIVERS.get(payload.channelId());
            if (receiver != null) {
                receiver.receive(context.player(), buf);
            }
        });

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientNetworkHelper.registerClientReceiver();
        }
    }
    *///?}

    public static void sendToClient(ServerPlayer player, ResourceLocation id, FriendlyByteBuf buf) {
        //? if fabric && <1.20.5 {
        ServerPlayNetworking.send(player, id, buf);
        //?}
        //? if fabric && >=1.20.5 || forge || neoforge{
        /*byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        //? if fabric
        ServerPlayNetworking.send(player, new WrapperPayload(id, data));
        //? if forge
        //CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new WrapperPacket(id, data));
        //? if neoforge && <=1.20.4
        //PacketDistributor.PLAYER.with(player).send(new WrapperPayload(id, data));
        //? if neoforge && >1.20.4
        //PacketDistributor.sendToPlayer(player, new WrapperPayload(id, data));
        *///?}
    }

    public static void sendToServer(ResourceLocation id, FriendlyByteBuf buf) {
        //? if fabric&& <1.20.5 {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientNetworkHelper.sendToServer(id, buf);
        }
        //?}
        //? if fabric && >=1.20.5 || forge || neoforge{
        /*byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        //? if fabric{
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientNetworkHelper.sendToServer(new WrapperPayload(id, data));
        }
        //?}
        //? if forge
        //CHANNEL.sendToServer(new WrapperPacket(id, data));
        //? if neoforge && <=1.20.4
        //PacketDistributor.SERVER.noArg().send(new WrapperPayload(id, data));
        //? if neoforge && >1.20.4 && <1.21.11
        //PacketDistributor.sendToServer(new WrapperPayload(id, data));
        //? if neoforge && >=1.21.11
        //ClientPacketDistributor.sendToServer(new WrapperPayload(id, data));
        *///?}
    }

    public static void registerServerReceiver(ResourceLocation id, ServerReceiver receiver) {
        //? if fabric && <1.20.5 {
        ServerPlayNetworking.registerGlobalReceiver(id, (server, player, handler, buf, responseSender) -> {
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            server.execute(() -> receiver.receive(player, new FriendlyByteBuf(Unpooled.wrappedBuffer(data))));
        });
        //?} else {
        /*SERVER_RECEIVERS.put(id, receiver);
        *///?}
    }

    public static void registerClientReceiver(ResourceLocation id, ClientReceiver receiver) {
        //? if fabric && <1.20.5 {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientNetworkHelper.registerClientReceiver(id, receiver);
        }
        //?} else {
        /*CLIENT_RECEIVERS.put(id, receiver);
        *///?}
    }

    @FunctionalInterface
    public interface ServerReceiver {
        void receive(ServerPlayer player, FriendlyByteBuf buf);
    }

    @FunctionalInterface
    public interface ClientReceiver {
        void receive(FriendlyByteBuf buf);
    }
}
