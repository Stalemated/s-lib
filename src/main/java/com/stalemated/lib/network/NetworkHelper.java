package com.stalemated.lib.network;

import com.stalemated.lib.helper.PlatformHelper;
import com.stalemated.lib.network.internal.PacketReceivers;
import com.stalemated.lib.network.internal.WrapperChannel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Helper for sending and receiving payloads over S-Lib's wrapper channel.
 */
public final class NetworkHelper {

    /**
     * Largest payload for one packet, in bytes: {@value}. That is 1 MiB minus 512 bytes for the
     * wrapper. It is checked when sending and when reading.
     */
    public static final int MAX_PAYLOAD_BYTES = 1_048_064;

    private NetworkHelper() {}

    /**
     * Sends the readable bytes of {@code buf} to {@code player} on channel {@code id}. It reads
     * {@code buf} to the end and does not release it.
     *
     * @param player the recipient player
     * @param id the channel id
     * @param buf the buffer with the payload
     * @throws IllegalArgumentException if the payload exceeds {@link #MAX_PAYLOAD_BYTES}
     */
    public static void sendToClient(ServerPlayer player, ResourceLocation id, FriendlyByteBuf buf) {
        WrapperChannel.sendToClient(player, id, readableBytes(id, buf));
    }

    /**
     * Sends the readable bytes of {@code buf} from the client to the server on channel
     * {@code id}. It reads {@code buf} to the end and does not release it.
     * <p>
     * The vanilla limit for client to server payloads is 32767 bytes, lower than
     * {@link #MAX_PAYLOAD_BYTES}.
     *
     * @param id the channel id
     * @param buf the buffer with the payload
     * @throws IllegalStateException if called on a dedicated server
     * @throws IllegalArgumentException if the payload exceeds {@link #MAX_PAYLOAD_BYTES}
     */
    public static void sendToServer(ResourceLocation id, FriendlyByteBuf buf) {
        if (PlatformHelper.isDedicatedServer()) {
            throw new IllegalStateException("Cannot send packet " + id + " to the server from a dedicated server");
        }
        WrapperChannel.sendToServer(id, readableBytes(id, buf));
    }

    private static byte[] readableBytes(ResourceLocation id, FriendlyByteBuf buf) {
        int size = buf.readableBytes();
        if (size > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Packet " + id + " is " + size + " bytes, limit is " + MAX_PAYLOAD_BYTES);
        }
        byte[] data = new byte[size];
        buf.readBytes(data);
        return data;
    }

    /**
     * Registers a server-side receiver for channel {@code id}.
     *
     * @param id the channel id
     * @param receiver callback handling incoming client packets
     * @throws IllegalStateException if a server receiver is already registered for {@code id}
     */
    public static void registerServerReceiver(ResourceLocation id, ServerReceiver receiver) {
        PacketReceivers.registerServer(id, receiver);
    }

    /**
     * Registers a client-side receiver for channel {@code id}.
     *
     * @param id the channel id
     * @param receiver callback handling incoming server packets
     * @throws IllegalStateException if a client receiver is already registered for {@code id}
     */
    public static void registerClientReceiver(ResourceLocation id, ClientReceiver receiver) {
        PacketReceivers.registerClient(id, receiver);
    }

    /**
     * Unregisters the server-side receiver for channel {@code id}. Does nothing if none is
     * registered.
     *
     * @param id the channel id
     */
    public static void unregisterServerReceiver(ResourceLocation id) {
        PacketReceivers.unregisterServer(id);
    }

    /**
     * Unregisters the client-side receiver for channel {@code id}. Does nothing if none is
     * registered.
     *
     * @param id the channel id
     */
    public static void unregisterClientReceiver(ResourceLocation id) {
        PacketReceivers.unregisterClient(id);
    }

    /**
     * Callback invoked on the server when a client sends a packet to a registered channel.
     * It runs on the server thread, and S-Lib does not catch its exceptions.
     */
    @FunctionalInterface
    public interface ServerReceiver {
        /**
         * Handles an incoming client-to-server payload.
         *
         * @param player the sending player
         * @param buf a new buffer with the received bytes
         */
        void receive(ServerPlayer player, FriendlyByteBuf buf);
    }

    /**
     * Callback invoked on the client when the server sends a packet to a registered channel.
     * It runs on the client thread, and S-Lib does not catch its exceptions.
     */
    @FunctionalInterface
    public interface ClientReceiver {
        /**
         * Handles an incoming server-to-client payload.
         *
         * @param buf a new buffer with the received bytes
         */
        void receive(FriendlyByteBuf buf);
    }
}
