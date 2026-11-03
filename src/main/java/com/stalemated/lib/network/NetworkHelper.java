package com.stalemated.lib.network;

import com.stalemated.lib.helper.PlatformHelper;
import com.stalemated.lib.network.internal.PacketReceivers;
import com.stalemated.lib.network.internal.WrapperChannel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class NetworkHelper {

    private NetworkHelper() {}

    public static void sendToClient(ServerPlayer player, ResourceLocation id, FriendlyByteBuf buf) {
        WrapperChannel.sendToClient(player, id, readableBytes(buf));
    }

    public static void sendToServer(ResourceLocation id, FriendlyByteBuf buf) {
        if (PlatformHelper.isDedicatedServer()) {
            throw new IllegalStateException("Cannot send packet " + id + " to the server from a dedicated server");
        }
        WrapperChannel.sendToServer(id, readableBytes(buf));
    }

    private static byte[] readableBytes(FriendlyByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return data;
    }

    public static void registerServerReceiver(ResourceLocation id, ServerReceiver receiver) {
        PacketReceivers.registerServer(id, receiver);
    }

    public static void registerClientReceiver(ResourceLocation id, ClientReceiver receiver) {
        PacketReceivers.registerClient(id, receiver);
    }

    public static void unregisterServerReceiver(ResourceLocation id) {
        PacketReceivers.unregisterServer(id);
    }

    public static void unregisterClientReceiver(ResourceLocation id) {
        PacketReceivers.unregisterClient(id);
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
