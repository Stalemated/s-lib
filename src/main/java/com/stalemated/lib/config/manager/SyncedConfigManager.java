package com.stalemated.lib.config.manager;

import com.stalemated.lib.config.io.ConfigProvider;
import com.stalemated.lib.config.model.OptionChange;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.OptionTree;
import com.stalemated.lib.config.network.ConfigNetworkHandler;
import com.stalemated.lib.config.network.ConnectionState;
import com.stalemated.lib.config.network.SyncMode;
import com.stalemated.lib.config.registry.ConfigRegistry;
import com.stalemated.lib.helper.PlatformHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
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

    private final Predicate<ServerPlayer> serverPermissionCheck;
    private final List<Consumer<T>> syncListeners = new CopyOnWriteArrayList<>();
    private final List<BiConsumer<ServerPlayer, T>> informListeners = new CopyOnWriteArrayList<>();

    private volatile ConnectionState state = PlatformHelper.isDedicatedServer() ? ConnectionState.DEDICATED_SERVER : ConnectionState.DISCONNECTED;
    private volatile T serverConfig = null;

    private final ConfigNetworkHandler<T> networkHandler;

    /**
     * Constructs a new SyncedConfigManager.
     *
     * @param provider The config provider detailing how to load/save instances.
     * @param configPath The absolute file path where the config JSON will be stored locally.
     * @param logger The SLF4J logger instance for your mod.
     * @param basePacketId The base ResourceLocation used for network channels.
     * @param serverPermissionCheck A predicate evaluating whether a player is authorized to edit the config.
     * @param defaultFactory A supplier creating a fresh default instance of your config.
     */
    public SyncedConfigManager(
            ConfigProvider<T> provider,
            Path configPath,
            Logger logger,
            ResourceLocation basePacketId,
            Predicate<ServerPlayer> serverPermissionCheck,
            Supplier<T> defaultFactory,
            OptionTree optionTree) {
        super(provider, configPath, logger, optionTree, defaultFactory);
        this.serverPermissionCheck = serverPermissionCheck;

        //? if <1.21 {
        ResourceLocation s2cPacket = new ResourceLocation(basePacketId.getNamespace(), basePacketId.getPath() + "_s2c");
        ResourceLocation c2sPacket = new ResourceLocation(basePacketId.getNamespace(), basePacketId.getPath() + "_c2s");
        ResourceLocation informC2sPacket = new ResourceLocation(basePacketId.getNamespace(), basePacketId.getPath() + "_inform_c2s");
        //?} else {
        /*ResourceLocation s2cPacket = ResourceLocation.fromNamespaceAndPath(basePacketId.getNamespace(), basePacketId.getPath() + "_s2c");
        ResourceLocation c2sPacket = ResourceLocation.fromNamespaceAndPath(basePacketId.getNamespace(), basePacketId.getPath() + "_c2s");
        ResourceLocation informC2sPacket = ResourceLocation.fromNamespaceAndPath(basePacketId.getNamespace(), basePacketId.getPath() + "_inform_c2s");
        *///?}

        this.networkHandler = new ConfigNetworkHandler<>(this, s2cPacket, c2sPacket, informC2sPacket);

        this.onConfigLoaded(config -> {
            ConfigRegistry.register(this);
            this.networkHandler.registerReceivers();
            notifySyncListeners(config);
        });
    }

    public OptionTree getOptionTree() {
        return optionTree;
    }

    public ConfigNetworkHandler<T> getNetworkHandler() {
        return networkHandler;
    }

    public ConnectionState getConnectionState() {
        return state;
    }

    public T getServerConfig() {
        return serverConfig;
    }

    public void setServerConfig(T serverConfig) {
        this.serverConfig = serverConfig;
    }

    public boolean checkServerPermission(ServerPlayer player) {
        return serverPermissionCheck.test(player);
    }

    public T createDefaultOrClone() {
        return defaultFactory != null ? defaultFactory.get() : cloneConfig(getConfig());
    }

    public void notifyInformListeners(ServerPlayer player, T config) {
        for (BiConsumer<ServerPlayer, T> listener : informListeners) {
            try {
                listener.accept(player, config);
            } catch (Exception e) {
                logger.error("Error notifying inform listener: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Registers a listener callback triggered when a client informs the server of its local config.
     *
     * @param listener Consumer receiving the player and their informed config data.
     */
    public void onConfigInformed(BiConsumer<ServerPlayer, T> listener) {
        if (listener != null) {
            this.informListeners.add(listener);
        }
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
    public void notifySyncListeners(T config) {
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

        if (newState == ConnectionState.MULTIPLAYER_UNMODDED) {
            T unmoddedConfig = cloneConfig(getConfig());
            T defaultValues = createDefaultOrClone();
            
            for (OptionInfo option : optionTree.all()) {
                if (option.getSyncMode() == SyncMode.OVERRIDE_CLIENT) {
                    option.setValue(unmoddedConfig, option.getValue(defaultValues));
                }
            }
            this.serverConfig = unmoddedConfig;
        }
    }

    /**
     * Call this from the ClientPlayConnectionEvents.JOIN event if the client joins a Multiplayer server.
     */
    public void onClientJoinMultiplayer() {
        if (this.state != ConnectionState.MULTIPLAYER_MODDED) {
            setConnectionState(ConnectionState.MULTIPLAYER_UNMODDED);
        }
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

        if ((currentState == ConnectionState.MULTIPLAYER_MODDED || currentState == ConnectionState.MULTIPLAYER_UNMODDED) && currentServer != null) {
            return currentServer;
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
     * Serializes the current server config and pushes it to a specific player.
     * Typically called during player join events on the server side.
     *
     * @param player The target player to sync the config to.
     */
    public void sendConfigToPlayer(ServerPlayer player) {
        networkHandler.sendConfigToPlayer(player);
    }

    /**
     * Executes a transactional modification on the active config instance.
     * <p>
     * Mutations are diffed against the active config and dispatched according to
     * each option's {@link SyncMode}:
     * <ul>
     *     <li>{@link SyncMode#NONE}: Applied locally and saved to disk.</li>
     *     <li>{@link SyncMode#INFORM_SERVER}: Applied locally, saved, and dispatched via C2S inform packet.</li>
     *     <li>{@link SyncMode#OVERRIDE_CLIENT}: On dedicated/singleplayer, applied locally; on modded multiplayer,
     *         applied to active cache and sent to server via C2S override packet without saving locally until server echo;
     *         on unmodded multiplayer, rejected with a warning.</li>
     * </ul>
     * All disk I/O and network packet transmissions are batched and executed at most once per transaction.
     *
     * @param mutator The consumer performing strongly typed property updates on the working copy.
     */
    @Override
    public void modify(Consumer<T> mutator) {
        T active = getActiveConfig();
        if (active == null) return;

        T workingCopy = cloneConfig(active);
        mutator.accept(workingCopy);

        List<OptionChange> changes = optionTree.findChanges(active, workingCopy);
        if (changes.isEmpty()) return;

        SyncBatch batch = new SyncBatch();
        for (OptionChange change : changes) {
            applyChange(change, batch);
        }
        flush(batch);
    }

    private void applyChange(OptionChange change, SyncBatch batch) {
        OptionInfo option = change.option();
        Object value = change.clampedValue();

        switch (change.syncMode()) {
            case NONE -> applyLocalChange(option, value, batch);
            case INFORM_SERVER -> applyInformChange(option, value, batch);
            case OVERRIDE_CLIENT -> applyOverrideChange(option, value, batch);
        }
    }

    private void applyLocalChange(OptionInfo option, Object value, SyncBatch batch) {
        option.setValue(getConfig(), value);
        if (this.serverConfig != null) {
            option.setValue(this.serverConfig, value);
        }
        batch.localChanged = true;
    }

    private void applyInformChange(OptionInfo option, Object value, SyncBatch batch) {
        applyLocalChange(option, value, batch);
        if (this.state == ConnectionState.MULTIPLAYER_MODDED) {
            batch.sendInform = true;
        } else if (this.state == ConnectionState.SINGLEPLAYER) {
            batch.sendLocalInform = true;
        }
    }

    private void applyOverrideChange(OptionInfo option, Object value, SyncBatch batch) {
        if (this.state == ConnectionState.MULTIPLAYER_MODDED && this.serverConfig != null) {
            option.setValue(this.serverConfig, value);
            batch.sendOverride = true;
        } else if (this.state == ConnectionState.MULTIPLAYER_UNMODDED) {
            logger.warn("Cannot modify synced option '{}' while connected to an unmodded server.", option.getKey());
            batch.rejectedUnmodded = true;
        } else {
            applyLocalChange(option, value, batch);
            if (this.state == ConnectionState.SINGLEPLAYER) {
                batch.sendLocalOverride = true;
            }
        }
    }

    private void flush(SyncBatch batch) {
        if (batch.localChanged) {
            super.save();
            notifySyncListeners(getActiveConfig());
        } else if (batch.rejectedUnmodded) {
            notifySyncListeners(getActiveConfig());
        }

        if (batch.sendInform) {
            networkHandler.sendInformPacketToServer();
        } else if (batch.sendLocalInform) {
            networkHandler.sendLocalInformPacketToServer();
        }

        if (batch.sendOverride) {
            networkHandler.sendOverridePacketToServer();
        } else if (batch.sendLocalOverride) {
            networkHandler.sendLocalOverridePacketToServer();
        }
    }

    private static class SyncBatch {
        boolean localChanged = false;
        boolean sendInform = false;
        boolean sendOverride = false;
        boolean sendLocalOverride = false;
        boolean sendLocalInform = false;
        boolean rejectedUnmodded = false;
    }
}
