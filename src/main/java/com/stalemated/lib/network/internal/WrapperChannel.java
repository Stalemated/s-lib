package com.stalemated.lib.network.internal;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

//? if forge || neoforge || <1.20.5
import static com.stalemated.lib.SLib.MOD_ID;

//? if forge || fabric && <1.20.5
import net.minecraft.network.FriendlyByteBuf;

//? if fabric{
import com.stalemated.lib.helper.PlatformHelper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
//?if <1.20.5 {
import io.netty.buffer.Unpooled;
//?} else {
/*import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
*///?}
//?}

//? if forge{
/*import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.function.Supplier;
*///?}

//? if neoforge{
/*import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.bus.api.IEventBus;

//? if <=1.20.4 {
/^import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
^///?} else {
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
//?}

//? if >=1.21.7
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
*///?}
public final class WrapperChannel {

    private WrapperChannel() {}

    //? if forge {
    /*private static final String PROTOCOL_VERSION = "1";
    private static SimpleChannel channel;

    @SuppressWarnings("removal")
    public static void registerChannel() {
        channel = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(MOD_ID, "network"),
                () -> PROTOCOL_VERSION,
                NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION),
                NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION)
        );
        channel.registerMessage(0, WrapperPacket.class, WrapperPacket::encode, WrapperPacket::new, WrapperChannel::handlePacket);
    }

    private static SimpleChannel channel() {
        if (channel == null) {
            throw new IllegalStateException("S-Lib network channel used before SLib registered it");
        }
        return channel;
    }

    private static final class WrapperPacket {
        private final ResourceLocation id;
        private final byte[] data;

        private WrapperPacket(ResourceLocation id, byte[] data) {
            this.id = id;
            this.data = data;
        }

        private WrapperPacket(FriendlyByteBuf buf) {
            this.id = buf.readResourceLocation();
            this.data = buf.readByteArray();
        }

        private void encode(FriendlyByteBuf buf) {
            buf.writeResourceLocation(this.id);
            buf.writeByteArray(this.data);
        }
    }

    private static void handlePacket(WrapperPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER && sender != null) {
                PacketReceivers.dispatchToServer(sender, packet.id, packet.data);
            } else if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                PacketReceivers.dispatchToClient(packet.id, packet.data);
            }
        });
        context.setPacketHandled(true);
    }
    *///?}

    //? if neoforge && <=1.20.4 {
    /*public static void registerModBusListeners(IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlerEvent.class, WrapperChannel::register);
    }

    private static void register(RegisterPayloadHandlerEvent event) {
        IPayloadRegistrar registrar = event.registrar(MOD_ID).optional();
        registrar.play(
                WrapperPayload.ID,
                WrapperPayload::new,
                WrapperChannel::handlePayload
        );
    }
    *///?} elif neoforge {
    /*public static void registerModBusListeners(IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, WrapperChannel::register);
    }

    private static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MOD_ID).optional();
        registrar.playBidirectional(
                WrapperPayload.ID,
                WrapperPayload.CODEC,
                WrapperChannel::handlePayload
        );
    }
    *///?}

    //? if neoforge && <=1.20.4 {
    /*private static void handlePayload(WrapperPayload payload, IPayloadContext context) {
        context.workHandler().execute(() -> {
            if (context.flow().isServerbound()) {
                if (context.player().orElse(null) instanceof ServerPlayer serverPlayer) {
                    PacketReceivers.dispatchToServer(serverPlayer, payload.channelId(), payload.data());
                }
            } else {
                PacketReceivers.dispatchToClient(payload.channelId(), payload.data());
            }
        });
    }
    *///?} elif neoforge {
    /*private static void handlePayload(WrapperPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isServerbound()) {
                if (context.player() instanceof ServerPlayer serverPlayer) {
                    PacketReceivers.dispatchToServer(serverPlayer, payload.channelId(), payload.data());
                }
            } else {
                PacketReceivers.dispatchToClient(payload.channelId(), payload.data());
            }
        });
    }
    *///?}

    //? if fabric && <1.20.5 {
    private static final ResourceLocation CHANNEL_ID = new ResourceLocation(MOD_ID, "network");

    public static void registerPayloads() {
        ServerPlayNetworking.registerGlobalReceiver(CHANNEL_ID, (server, player, handler, buf, responseSender) -> {
            ResourceLocation channel = buf.readResourceLocation();
            byte[] data = buf.readByteArray();
            server.execute(() -> PacketReceivers.dispatchToServer(player, channel, data));
        });

        if (!PlatformHelper.isDedicatedServer()) {
            ClientNetworkHelper.registerClientReceiver(CHANNEL_ID);
        }
    }

    private static FriendlyByteBuf wrap(ResourceLocation channel, byte[] data) {
        FriendlyByteBuf wrapped = new FriendlyByteBuf(Unpooled.buffer());
        wrapped.writeResourceLocation(channel);
        wrapped.writeByteArray(data);
        return wrapped;
    }
    //?} elif fabric {
    /*public static void registerPayloads() {

        //? if <26.1 {
        PayloadTypeRegistry.playC2S().register(WrapperPayload.ID, WrapperPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WrapperPayload.ID, WrapperPayload.CODEC);
        //?} else {
        /^PayloadTypeRegistry.serverboundPlay().register(WrapperPayload.ID, WrapperPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(WrapperPayload.ID, WrapperPayload.CODEC);
        ^///?}

        ServerPlayNetworking.registerGlobalReceiver(WrapperPayload.ID, (payload, context) ->
                PacketReceivers.dispatchToServer(context.player(), payload.channelId(), payload.data()));

        if (!PlatformHelper.isDedicatedServer()) {
            ClientNetworkHelper.registerClientReceiver();
        }
    }
    *///?}

    public static void sendToClient(ServerPlayer player, ResourceLocation id, byte[] data) {
        //? if fabric && <1.20.5
        ServerPlayNetworking.send(player, CHANNEL_ID, wrap(id, data));
        //? if fabric && >=1.20.5
        //ServerPlayNetworking.send(player, new WrapperPayload(id, data));
        //? if forge
        //channel().send(PacketDistributor.PLAYER.with(() -> player), new WrapperPacket(id, data));
        //? if neoforge && <=1.20.4
        //PacketDistributor.PLAYER.with(player).send(new WrapperPayload(id, data));
        //? if neoforge && >1.20.4
        //PacketDistributor.sendToPlayer(player, new WrapperPayload(id, data));
    }

    public static void sendToServer(ResourceLocation id, byte[] data) {
        //? if fabric && <1.20.5
        ClientNetworkHelper.sendToServer(CHANNEL_ID, wrap(id, data));
        //? if fabric && >=1.20.5
        //ClientNetworkHelper.sendToServer(new WrapperPayload(id, data));
        //? if forge
        //channel().sendToServer(new WrapperPacket(id, data));
        //? if neoforge && <=1.20.4
        //PacketDistributor.SERVER.noArg().send(new WrapperPayload(id, data));
        //? if neoforge && >1.20.4 && <1.21.7
        //PacketDistributor.sendToServer(new WrapperPayload(id, data));
        //? if neoforge && >=1.21.7
        //ClientPacketDistributor.sendToServer(new WrapperPayload(id, data));
    }
}
