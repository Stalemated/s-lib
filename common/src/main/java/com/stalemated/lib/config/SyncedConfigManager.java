package com.stalemated.lib.config;

import com.google.gson.Gson;
import com.stalemated.lib.network.NetworkHelper;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A specialized ConfigManager that handles network sync between client and server.
 * <p>
 * This manager automates Server-To-Client (S2C) and Client-To-Server (C2S) syncing. It evaluates customizable
 * permission checks to decide if a client is allowed to change the config, with fallback logic to avoid desyncs.
 *
 * @param <T> The config data model class.
 */
public class SyncedConfigManager<T> extends BaseConfigManager<T> {

    private final Identifier s2cPacket;
    private final Identifier c2sPacket;
    private final Class<T> configClass;
    private final Predicate<ServerPlayerEntity> serverPermissionCheck;
    private final ConfigMerger<T> merger;
    private final Gson gson;
    private T serverConfig = null;

    /**
     * Constructs a new SyncedConfigManager.
     *
     * @param provider The config provider detailing how to load/save instances.
     * @param configPath The absolute file path where the config JSON will be stored locally.
     * @param logger The SLF4J logger instance for your mod.
     * @param basePacketId The base Identifier used for network channels. The manager will append "_s2c" and "_c2s" automatically.
     * @param configClass The runtime class of your config data model.
     * @param serverPermissionCheck A predicate evaluating whether a specific player is authorized to submit config updates to the server. You can use defaults from {@link com.stalemated.lib.config.permissions.ServerConfigPermissions}.
     * @param merger A lambda expressing how to merge incoming fields from a source payload into the destination local config.
     */
    public SyncedConfigManager(
            ConfigProvider<T> provider,
            Path configPath,
            Logger logger,
            Identifier basePacketId,
            Class<T> configClass,
            Predicate<ServerPlayerEntity> serverPermissionCheck,
            ConfigMerger<T> merger) {
        super(provider, configPath, logger);
        this.s2cPacket = Identifier.of(basePacketId.getNamespace(), basePacketId.getPath() + "_s2c");
        this.c2sPacket = Identifier.of(basePacketId.getNamespace(), basePacketId.getPath() + "_c2s");
        this.configClass = configClass;
        this.serverPermissionCheck = serverPermissionCheck;
        this.merger = merger;
        this.gson = new Gson();
    }

    @Override
    protected void onRegisterSuccess(boolean isNewOrEmpty) {
        super.onRegisterSuccess(isNewOrEmpty);

        NetworkHelper.INSTANCE.registerClientReceiver(s2cPacket, buf -> {
            String json = buf.readString();
            this.serverConfig = gson.fromJson(json, configClass);
        });

        NetworkHelper.INSTANCE.registerServerReceiver(c2sPacket, (player, buf) -> {
            if (serverPermissionCheck.test(player)) {
                String json = buf.readString();
                T parsed = gson.fromJson(json, configClass);
                
                merger.merge(parsed, getConfig());
                super.save();
                
                for (ServerPlayerEntity p : player.server.getPlayerManager().getPlayerList()) {
                    sendConfigToPlayer(p);
                }
            } else {
                // If rejected, send back the config to avoid desyncs
                sendConfigToPlayer(player);
            }
        });
    }

    /**
     * Retrieves the active config instance.
     * If playing on a multiplayer server with this mod installed, this returns the synced server config.
     * Otherwise, it returns the local config.
     *
     * @return The active config data model.
     */
    public T getActiveConfig() {
        if (serverConfig != null) {
            return serverConfig;
        }
        return getConfig();
    }

    /**
     * Clears the cached server config.
     * This must be called during client disconnect events to revert the client back to its local config.
     */
    public void clearServerConfig() {
        this.serverConfig = null;
    }

    /**
     * Handles local saves and dispatches the C2S sync packet to the server if applicable.
     * <p>
     * This method is intended to be bound to your config GUI's "Save" or "Done" callback (e.g. YACL's save hook).
     */
    public void saveFromClient() {
        if (serverConfig != null) {
            String json = gson.toJson(getConfig());
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeString(json);
            NetworkHelper.INSTANCE.sendToServer(c2sPacket, buf);
        }
        super.save();
    }

    /**
     * Serializes the current server config and pushes it to a specific player.
     * Typically called during player join events on the server side.
     *
     * @param player The target player to sync the config to.
     */
    public void sendConfigToPlayer(ServerPlayerEntity player) {
        String json = gson.toJson(getConfig());
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeString(json);
        NetworkHelper.INSTANCE.sendToClient(player, s2cPacket, buf);
    }

    /**
     * Safely changes a field on both the local config and the cached server config at the same time.
     * <p>
     * This ensures that UI elements reliant on {@link #getActiveConfig()} reflect changes immediately upon click.
     *
     * @param setter A consumer describing how to apply the value to a config instance.
     * @param value The new value to apply.
     * @param clientPermissionCheck A supplier confirming the local client possesses the requisite authority to edit the field. You can use defaults from {@link com.stalemated.lib.config.permissions.ClientConfigPermissions}.
     * @param <V> The type of the field being modified.
     */
    public <V> void updateField(BiConsumer<T, V> setter, V value, Supplier<Boolean> clientPermissionCheck) {
        if (clientPermissionCheck.get()) {
            setter.accept(getConfig(), value);

            if (serverConfig != null) {
                setter.accept(serverConfig, value);
            }
        }
    }
}
