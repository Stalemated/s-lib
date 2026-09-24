package com.stalemated.lib.config.manager;

import com.stalemated.lib.config.io.ConfigProvider;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.OptionTree;
import com.stalemated.lib.config.network.ConfigNetworkHandler;
import com.stalemated.lib.config.network.ConnectionState;
import com.stalemated.lib.config.network.SyncMode;
import com.stalemated.lib.config.registry.ConfigRegistry;
import com.stalemated.lib.helper.PlatformHelper;
import com.stalemated.lib.util.reflection.ReflectionUtils;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
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

    private final Class<T> configClass;
    private final Predicate<ServerPlayerEntity> serverPermissionCheck;
    private final Supplier<T> defaultFactory;
    private final List<Consumer<T>> syncListeners = new CopyOnWriteArrayList<>();
    private final List<BiConsumer<ServerPlayerEntity, T>> informListeners = new CopyOnWriteArrayList<>();

    private volatile ConnectionState state = PlatformHelper.INSTANCE.isDedicatedServer() ? ConnectionState.DEDICATED_SERVER : ConnectionState.DISCONNECTED;
    private volatile T serverConfig = null;

    private final ConfigNetworkHandler<T> networkHandler;

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
        this.configClass = configClass;
        this.serverPermissionCheck = serverPermissionCheck;
        this.defaultFactory = defaultFactory;

        Identifier s2cPacket = Identifier.of(basePacketId.getNamespace(), basePacketId.getPath() + "_s2c");
        Identifier c2sPacket = Identifier.of(basePacketId.getNamespace(), basePacketId.getPath() + "_c2s");
        Identifier informC2sPacket = Identifier.of(basePacketId.getNamespace(), basePacketId.getPath() + "_inform_c2s");
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

    public boolean checkServerPermission(ServerPlayerEntity player) {
        return serverPermissionCheck.test(player);
    }

    public T createDefaultOrClone() {
        return defaultFactory != null ? defaultFactory.get() : cloneConfig(getConfig());
    }

    public void notifyInformListeners(ServerPlayerEntity player, T config) {
        for (BiConsumer<ServerPlayerEntity, T> listener : informListeners) {
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
    public void onConfigInformed(BiConsumer<ServerPlayerEntity, T> listener) {
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
    public void sendConfigToPlayer(ServerPlayerEntity player) {
        networkHandler.sendConfigToPlayer(player);
    }

    /**
     * Updates an option identified by its dot-separated key (e.g. {@code "attackDamage"} or {@code "combat.attackSpeed"}).
     *
     * @param optionKey The dot-separated option path.
     * @param value The new value to set.
     */
    @Override
    public void updateOption(String optionKey, Object value) {
        OptionInfo option = optionTree.get(optionKey);
        if (option == null) {
            throw new IllegalArgumentException("Unknown config option: " + optionKey);
        }

        // Diff check against ACTIVE config to prevent I/O and network spam
        Object clampedValue = option.clampValue(value);
        if (Objects.equals(option.getValue(getActiveConfig()), clampedValue)) {
            return;
        }

        switch (option.getSyncMode()) {
            case NONE -> processLocalOnlyOption(optionKey, option, clampedValue);
            case INFORM_SERVER -> processInformServerOption(optionKey, option, clampedValue);
            case OVERRIDE_CLIENT -> processOverrideClientOption(optionKey, option, clampedValue);
        }
    }

    private void applyToLocalAndCache(String optionKey, OptionInfo option, Object value) {
        super.updateOption(optionKey, value);

        if (this.serverConfig != null) {
            option.setValue(this.serverConfig, value);
        }

        notifySyncListeners(getActiveConfig());
    }

    private void processLocalOnlyOption(String optionKey, OptionInfo option, Object value) {
        applyToLocalAndCache(optionKey, option, value);
    }

    private void processInformServerOption(String optionKey, OptionInfo option, Object value) {
        applyToLocalAndCache(optionKey, option, value);

        if (this.state == ConnectionState.MULTIPLAYER_MODDED) {
            networkHandler.sendInformPacketToServer();
        } else if (this.state == ConnectionState.SINGLEPLAYER) {
            networkHandler.sendLocalInformPacketToServer();
        }
    }

    private void processOverrideClientOption(String optionKey, OptionInfo option, Object value) {
        if (this.state == ConnectionState.MULTIPLAYER_MODDED && this.serverConfig != null) {
            // Apply to cache and send to server.
            // DO NOT call notifySyncListeners locally. Wait for the server's S2C echo packet to confirm.
            option.setValue(this.serverConfig, value);
            networkHandler.sendOverridePacketToServer();
        } else if (this.state == ConnectionState.MULTIPLAYER_UNMODDED) {
            logger.warn("Cannot modify synced option '{}' while connected to an unmodded server.", optionKey);
            notifySyncListeners(getActiveConfig());
        } else {
            applyToLocalAndCache(optionKey, option, value);
            if (this.state == ConnectionState.SINGLEPLAYER) {
                networkHandler.sendLocalOverridePacketToServer();
            }
        }
    }

    public T cloneConfig(T source) {
        T copy = defaultFactory != null ? defaultFactory.get() : ReflectionUtils.tryInstantiate(configClass);
        optionTree.copyAllValues(source, copy);
        return copy;
    }
}
