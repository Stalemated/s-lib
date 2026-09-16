package com.stalemated.lib.config.manager;

import com.stalemated.lib.config.io.ConfigProvider;
import com.stalemated.lib.config.permissions.ClientConfigPermissions;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.OptionTree;
import com.stalemated.lib.config.network.ConfigNetworkPayload;
import com.stalemated.lib.config.network.SyncMode;
import com.stalemated.lib.config.network.ConnectionState;
import com.stalemated.lib.config.registry.ConfigRegistry;
import com.stalemated.lib.network.NetworkHelper;
import com.stalemated.lib.util.reflection.ReflectionUtils;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
public class SyncedConfigManager<T> extends LocalConfigManager<T> {

    private final Identifier s2cPacket;
    private final Identifier c2sPacket;
    private final Class<T> configClass;
    private final Predicate<ServerPlayerEntity> serverPermissionCheck;
    private final Supplier<T> defaultFactory;
    private final List<Consumer<T>> syncListeners = new CopyOnWriteArrayList<>();

    private volatile ConnectionState state = ConnectionState.DISCONNECTED;
    private volatile T serverConfig = null;
    private volatile T defaultConfig = null;

    /**
     * Constructs a new SyncedConfigManager.
     *
     * @param provider The config provider detailing how to load/save instances.
     * @param configPath The absolute file path where the config JSON will be stored locally.
     * @param logger The SLF4J logger instance for your mod.
     * @param basePacketId The base Identifier used for network channels.
     * @param configClass The runtime class of your config data model.
     * @param serverPermissionCheck A predicate evaluating whether a player is authorized to edit the config.
     * @param defaultFactory A supplier creating a fresh default instance of your config.
     */
    public SyncedConfigManager(
            ConfigProvider<T> provider,
            Path configPath,
            Logger logger,
            Identifier basePacketId,
            Class<T> configClass,
            Predicate<ServerPlayerEntity> serverPermissionCheck,
            Supplier<T> defaultFactory,
            OptionTree optionTree) {
        super(provider, configPath, logger, optionTree);
        this.s2cPacket = new Identifier(basePacketId.getNamespace(), basePacketId.getPath() + "_s2c");
        this.c2sPacket = new Identifier(basePacketId.getNamespace(), basePacketId.getPath() + "_c2s");
        this.configClass = configClass;
        this.serverPermissionCheck = serverPermissionCheck;
        this.defaultFactory = defaultFactory;
    }

    public OptionTree getOptionTree() {
        return optionTree;
    }

    @Override
    protected void onRegisterSuccess(boolean isNewOrEmpty) {
        super.onRegisterSuccess(isNewOrEmpty);
        ConfigRegistry.register(this);

        registerClientReceivers();
        registerServerReceivers();

        notifySyncListeners(getConfig());
    }

    protected void registerClientReceivers() {
        NetworkHelper.INSTANCE.registerClientReceiver(s2cPacket, buf -> {
            T target = cloneConfig(getConfig());
            ConfigNetworkPayload.readAndApply(buf, optionTree, target);
            this.serverConfig = target;
            setConnectionState(ConnectionState.MULTIPLAYER_MODDED);
            notifySyncListeners(target);
        });
    }

    protected void registerServerReceivers() {
        NetworkHelper.INSTANCE.registerServerReceiver(c2sPacket, (player, buf) -> {
            if (serverPermissionCheck.test(player)) {
                ConfigNetworkPayload.readAndApply(buf, optionTree, getConfig());
                super.save();
                notifySyncListeners(getConfig());

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
     * Registers a listener callback triggered whenever the active config is synchronized or updated.
     *
     * @param listener Consumer receiving the newly active config.
     */
    public void onConfigSynced(Consumer<T> listener) {
        if (listener != null) {
            this.syncListeners.add(listener);
        }
    }

    /**
     * Notifies all registered listeners with the updated config instance.
     *
     * @param config The updated config instance.
     */
    protected void notifySyncListeners(T config) {
        for (Consumer<T> listener : syncListeners) {
            try {
                listener.accept(config);
            } catch (Exception e) {
                logger.error("Error notifying config sync listener: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Changes the current connection state of the client.
     *
     * @param newState The new state.
     */
    public void setConnectionState(ConnectionState newState) {
        this.state = newState;

        // If we connect to an unmodded server, ensure we have a default config generated.
        if (newState == ConnectionState.MULTIPLAYER_UNMODDED && defaultConfig == null && defaultFactory != null) {
            this.defaultConfig = defaultFactory.get();
        }
    }

    /**
     * Call this from the ClientPlayConnectionEvents.JOIN event if the client joins a Multiplayer server.
     */
    public void onClientJoinMultiplayer() {
        setConnectionState(ConnectionState.MULTIPLAYER_UNMODDED);
    }

    /**
     * Call this from the ClientPlayConnectionEvents.JOIN event if the client joins a Singleplayer server.
     */
    public void onClientJoinSingleplayer() {
        setConnectionState(ConnectionState.SINGLEPLAYER);
    }

    /**
     * Retrieves the active config instance.
     * If playing on a multiplayer server with this mod installed, this returns the synced server config.
     * If playing on a vanilla multiplayer server, it returns the default config (if factory was provided).
     * Otherwise, it returns the local config.
     *
     * @return The active config data model.
     */
    public T getActiveConfig() {
        ConnectionState currentState = this.state;
        T currentServer = this.serverConfig;

        if (currentState == ConnectionState.MULTIPLAYER_MODDED && currentServer != null) {
            return currentServer;
        }
        if (currentState == ConnectionState.MULTIPLAYER_UNMODDED && defaultFactory != null) {
            if (defaultConfig == null) {
                defaultConfig = defaultFactory.get();
            }
            return defaultConfig;
        }
        return getConfig();
    }

    /**
     * Clears the cached server config and resets the state.
     * This must be called during client disconnect events to revert the client to its local config.
     */
    public void clearServerConfig() {
        this.serverConfig = null;
        setConnectionState(ConnectionState.DISCONNECTED);
        notifySyncListeners(getConfig());
    }

    /**
     * Handles local saves and dispatches the C2S sync packet to the server if applicable.
     */
    public void saveFromClient() {
        if (serverConfig != null) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            ConfigNetworkPayload.writeSynced(buf, optionTree, getConfig());
            NetworkHelper.INSTANCE.sendToServer(c2sPacket, buf);
        } else {
            super.save();
            notifySyncListeners(getConfig());
        }
    }

    /**
     * Serializes the current server config and pushes it to a specific player.
     * Typically called during player join events on the server side.
     *
     * @param player The target player to sync the config to.
     */
    public void sendConfigToPlayer(ServerPlayerEntity player) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        ConfigNetworkPayload.writeSynced(buf, optionTree, getConfig());
        NetworkHelper.INSTANCE.sendToClient(player, s2cPacket, buf);
    }

    /**
     * Updates an option identified by its dot-separated key (e.g. {@code "attackDamage"} or {@code "combat.attackSpeed"}).
     * <p>
     * If the option is local-only ({@link SyncMode#NONE}), it updates the local config and saves
     * immediately to disk without requiring OP permissions in multiplayer.
     * <p>
     * If the option is synchronized with the server ({@link SyncMode#OVERRIDE_CLIENT}), it updates the local
     * model and, if connected to a multiplayer server, dispatches a C2S update packet.
     *
     * @param optionKey The dot-separated option path.
     * @param value The new value to set.
     */
    public void updateOption(String optionKey, Object value) {
        OptionInfo option = optionTree.get(optionKey);
        if (option == null) {
            throw new IllegalArgumentException("Unknown config option: " + optionKey);
        }

        if (option.getSyncMode() == SyncMode.NONE) {
            option.setValue(getConfig(), value);
            T currentServer = this.serverConfig;

            if (currentServer != null) {
                option.setValue(currentServer, value);
            }
            super.save();
            notifySyncListeners(getActiveConfig());

        } else {
            option.setValue(getConfig(), value);
            T currentServer = this.serverConfig;

            if (currentServer != null) {
                option.setValue(currentServer, value);
                saveFromClient();
            } else {
                super.save();
                notifySyncListeners(getActiveConfig());
            }
        }
    }

    /**
     * Safely changes a field on both the local config and the cached server config at the same time.
     *
     * @param setter A consumer describing how to apply the value to a config instance.
     * @param value The new value to apply.
     * @param clientPermissionCheck A supplier confirming the local client possesses the authority to edit the field. You can use defaults from {@link ClientConfigPermissions}.
     * @param <V> The type of the field being modified.
     */
    public <V> void updateField(BiConsumer<T, V> setter, V value, Supplier<Boolean> clientPermissionCheck) {
        if (clientPermissionCheck.get()) {
            setter.accept(getConfig(), value);

            T currentServer = this.serverConfig;
            if (currentServer != null) {
                setter.accept(currentServer, value);
            }
        }
    }

    private T cloneConfig(T source) {
        T copy = defaultFactory != null ? defaultFactory.get() : ReflectionUtils.tryInstantiate(configClass);
        optionTree.copyAllValues(source, copy);
        return copy;
    }
}
