package com.stalemated.lib.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ServiceLoader;

/**
 * An abstraction layer for handling network packets seamlessly across different modloaders.
 * <p>
 * It uses native {@code ClientPlayNetworking} and {@code ServerPlayNetworking} on Fabric, and {@code SimpleChannel} wrappers on Forge.
 */
public interface NetworkHelper {

    /**
     * The loaded instance of the NetworkHelper.
     * Resolved dynamically via {@link ServiceLoader} at runtime based on the active modloader.
     */
    NetworkHelper INSTANCE = ServiceLoader.load(NetworkHelper.class)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("NetworkHelper not found!"));

    /**
     * Sends a byte buffer packet from the server to a specific player on the client.
     * 
     * @param player The target player connected to the server.
     * @param id The unique identifier for the packet channel.
     * @param buf The buffer payload to send. Can contain serialized data like JSON strings or raw bytes.
     */
    void sendToClient(ServerPlayerEntity player, Identifier id, PacketByteBuf buf);

    /**
     * Sends a byte buffer packet from the local client to the server.
     * <p>
     * <b>Note:</b> This method must only be called from the client logical side.
     * It is internally delegated in Fabric to avoid {@code ClassNotFoundException} on dedicated servers.
     * 
     * @param id The unique identifier for the packet channel.
     * @param buf The buffer payload to send.
     */
    void sendToServer(Identifier id, PacketByteBuf buf);

    /**
     * Registers a global receiver for packets sent from clients to the server.
     * 
     * @param id The unique identifier for the packet channel to listen on.
     * @param receiver The callback logic to execute when a packet is received on the server.
     */
    void registerServerReceiver(Identifier id, ServerReceiver receiver);

    /**
     * Registers a global receiver for packets sent from the server to the client.
     * <p>
     * <b>Note:</b> This method dynamically avoids calling client-only classes on dedicated servers on Fabric, ensuring safe initialization in common mod entrypoints.
     * 
     * @param id The unique identifier for the packet channel to listen on.
     * @param receiver The callback logic to execute when a packet is received on the client.
     */
    void registerClientReceiver(Identifier id, ClientReceiver receiver);

    /**
     * A callback interface representing a listener for packets received on the server side.
     */
    @FunctionalInterface
    interface ServerReceiver {
        /**
         * Invoked when a packet is received from a client.
         * 
         * @param player The player who sent the packet.
         * @param buf The incoming payload buffer. On Forge, this buffer wraps a raw byte array.
         */
        void receive(ServerPlayerEntity player, PacketByteBuf buf);
    }

    /**
     * A callback interface representing a listener for packets received on the client side.
     */
    @FunctionalInterface
    interface ClientReceiver {
        /**
         * Invoked when a packet is received from the server.
         * 
         * @param buf The incoming payload buffer.
         */
        void receive(PacketByteBuf buf);
    }
}
