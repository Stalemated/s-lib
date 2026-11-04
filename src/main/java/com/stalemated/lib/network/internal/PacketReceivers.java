package com.stalemated.lib.network.internal;

import com.stalemated.lib.network.NetworkHelper;
import com.stalemated.lib.network.NetworkHelper.ClientReceiver;
import com.stalemated.lib.network.NetworkHelper.ServerReceiver;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.stalemated.lib.SLib.LOGGER;

public final class PacketReceivers {

    private static final Map<ResourceLocation, ServerReceiver> SERVER_RECEIVERS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, ClientReceiver> CLIENT_RECEIVERS = new ConcurrentHashMap<>();

    private PacketReceivers() {}

    public static byte[] readData(FriendlyByteBuf buf) {
        return buf.readByteArray(NetworkHelper.MAX_PAYLOAD_BYTES);
    }

    public static void registerServer(ResourceLocation id, ServerReceiver receiver) {
        register(SERVER_RECEIVERS, id, receiver, "Server");
    }

    public static void registerClient(ResourceLocation id, ClientReceiver receiver) {
        register(CLIENT_RECEIVERS, id, receiver, "Client");
    }

    private static <R> void register(Map<ResourceLocation, R> receivers, ResourceLocation id, R receiver, String side) {
        if (receivers.putIfAbsent(id, receiver) != null) {
            throw new IllegalStateException(side + " receiver for channel " + id + " is already registered");
        }
    }

    public static void unregisterServer(ResourceLocation id) {
        SERVER_RECEIVERS.remove(id);
    }

    public static void unregisterClient(ResourceLocation id) {
        CLIENT_RECEIVERS.remove(id);
    }

    static void dispatchToServer(ServerPlayer player, ResourceLocation channel, byte[] data) {
        ServerReceiver receiver = SERVER_RECEIVERS.get(channel);
        if (receiver == null) {
            logUnregisteredChannel(channel);
            return;
        }
        receiver.receive(player, new FriendlyByteBuf(Unpooled.wrappedBuffer(data)));
    }

    static void dispatchToClient(ResourceLocation channel, byte[] data) {
        ClientReceiver receiver = CLIENT_RECEIVERS.get(channel);
        if (receiver == null) {
            logUnregisteredChannel(channel);
            return;
        }
        receiver.receive(new FriendlyByteBuf(Unpooled.wrappedBuffer(data)));
    }

    private static void logUnregisteredChannel(ResourceLocation channel) {
        LOGGER.debug("Ignoring packet for unregistered config channel {}", channel);
    }
}
