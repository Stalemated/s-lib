package com.stalemated.lib.config;

import com.stalemated.lib.config.io.ConfigProvider;
import com.stalemated.lib.config.model.OptionChange;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.OptionTree;
import com.stalemated.lib.config.network.ConnectionState;
import com.stalemated.lib.config.network.SyncMode;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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

    private final ResourceLocation syncChannel;
    private final Predicate<ServerPlayer> serverPermissionCheck;
    private final Listeners<BiConsumer<ServerPlayer, T>> informListeners = new Listeners<>(this::getName);

    private volatile ConnectionState state = ConnectionState.DISCONNECTED;
    private volatile T serverConfig = null;

    private final ConfigNetworkHandler<T> networkHandler;

    SyncedConfigManager(
            ConfigProvider<T> provider,
            Path configPath,
            Logger logger,
            ResourceLocation syncChannel,
            Predicate<ServerPlayer> serverPermissionCheck,
            Supplier<T> defaultFactory,
            OptionTree optionTree) {
        super(provider, configPath, logger, optionTree, defaultFactory);
        if (provider.getSerializer() == null) {
            throw new IllegalArgumentException("Synced config '" + getName() + "' needs a provider with a serializer to sync over the network");
        }
        this.syncChannel = syncChannel;
        this.serverPermissionCheck = serverPermissionCheck;

        //? if <1.21 {
        ResourceLocation s2cPacket = new ResourceLocation(syncChannel.getNamespace(), syncChannel.getPath() + "_s2c");
        ResourceLocation c2sPacket = new ResourceLocation(syncChannel.getNamespace(), syncChannel.getPath() + "_c2s");
        ResourceLocation informC2sPacket = new ResourceLocation(syncChannel.getNamespace(), syncChannel.getPath() + "_inform_c2s");
        //?} else {
        /*ResourceLocation s2cPacket = ResourceLocation.fromNamespaceAndPath(syncChannel.getNamespace(), syncChannel.getPath() + "_s2c");
        ResourceLocation c2sPacket = ResourceLocation.fromNamespaceAndPath(syncChannel.getNamespace(), syncChannel.getPath() + "_c2s");
        ResourceLocation informC2sPacket = ResourceLocation.fromNamespaceAndPath(syncChannel.getNamespace(), syncChannel.getPath() + "_inform_c2s");
        *///?}

        this.networkHandler = new ConfigNetworkHandler<>(this, s2cPacket, c2sPacket, informC2sPacket);
    }

    @Override
    void onRegister() {
        super.onRegister();
        ConfigRegistry.claimChannel(this);
        networkHandler.registerReceivers();
    }

    void addInformListener(BiConsumer<ServerPlayer, T> listener) {
        informListeners.add(listener);
    }

    ConfigNetworkHandler<T> networkHandler() {
        return networkHandler;
    }

    ResourceLocation getSyncChannel() {
        return syncChannel;
    }

    public ConnectionState getConnectionState() {
        return state;
    }

    T getServerConfig() {
        return serverConfig;
    }

    void setServerConfig(T serverConfig) {
        this.serverConfig = serverConfig;
    }

    boolean checkServerPermission(ServerPlayer player) {
        return serverPermissionCheck.test(player);
    }

    T createDefaultOrClone() {
        return defaultFactory != null ? defaultFactory.get() : cloneConfig(local());
    }

    void notifyInformListeners(ServerPlayer player, T config) {
        informListeners.fire(listener -> listener.accept(player, config));
    }

    /**
     * Changes the current connection state of the client.
     *
     * @param newState The new state.
     */
    void setConnectionState(ConnectionState newState) {
        this.state = newState;

        if (newState == ConnectionState.MULTIPLAYER_UNMODDED) {
            T unmoddedConfig = cloneConfig(local());
            T defaultValues = createDefaultOrClone();

            for (OptionInfo option : optionTree.all()) {
                if (option.getSyncMode() == SyncMode.OVERRIDE_CLIENT) {
                    option.setValue(unmoddedConfig, option.getValue(defaultValues));
                }
            }
            this.serverConfig = unmoddedConfig;
        }
    }

    void onClientJoinMultiplayer() {
        if (this.state != ConnectionState.MULTIPLAYER_MODDED) {
            setConnectionState(ConnectionState.MULTIPLAYER_UNMODDED);
        }
    }

    void onClientJoinSingleplayer() {
        setConnectionState(ConnectionState.SINGLEPLAYER);
    }

    /**
     * Retrieves the config values in effect. Never null after {@link #register()}.
     * If playing on a multiplayer server with this mod installed, this returns the synced server config.
     * If playing on a vanilla multiplayer server, synced options fall back to their defaults.
     * Otherwise, it returns the local config.
     *
     * @return The active config data model.
     */
    @Override
    public T get() {
        T local = super.get();
        ConnectionState currentState = this.state;
        T currentServer = this.serverConfig;

        if ((currentState == ConnectionState.MULTIPLAYER_MODDED || currentState == ConnectionState.MULTIPLAYER_UNMODDED) && currentServer != null) {
            return currentServer;
        }
        return local;
    }

    public T local() {
        return super.get();
    }

    void clearServerConfig() {
        this.serverConfig = null;
        setConnectionState(ConnectionState.DISCONNECTED);
        fireChanged();
    }

    /**
     * Serializes the current server config and pushes it to a specific player.
     * S-Lib already does this when a player joins; call it to resend after changing the config outside {@link #modify(Consumer)}.
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
     * only the changed options are sent to the server via C2S override packet and applied to the active cache
     * once sent, without saving locally until server echo; rejected with an error if the packet is over the
     * server-bound size limit; on unmodded multiplayer, rejected with a warning.</li>
     * </ul>
     * All disk I/O and network packet transmissions are batched and executed at most once per transaction.
     *
     * @param mutator The consumer performing strongly typed property updates on the working copy.
     */
    @Override
    public void modify(Consumer<T> mutator) {
        T active = get();

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
        switch (change.syncMode()) {
            case NONE -> applyLocalChange(change.option(), change.clampedValue(), batch);
            case INFORM_SERVER -> applyInformChange(change.option(), change.clampedValue(), batch);
            case OVERRIDE_CLIENT -> applyOverrideChange(change, batch);
        }
    }

    private void applyLocalChange(OptionInfo option, Object value, SyncBatch batch) {
        option.setValue(local(), value);
        if (this.serverConfig != null) {
            option.setValue(this.serverConfig, value);
        }
        batch.localChanged = true;
    }

    private void applyInformChange(OptionInfo option, Object value, SyncBatch batch) {
        applyLocalChange(option, value, batch);
        if (this.state == ConnectionState.MULTIPLAYER_MODDED || this.state == ConnectionState.SINGLEPLAYER) {
            batch.sendInform = true;
        }
    }

    private void applyOverrideChange(OptionChange change, SyncBatch batch) {
        if (this.state == ConnectionState.MULTIPLAYER_MODDED && this.serverConfig != null) {
            batch.serverOverrides.add(change);
        } else if (this.state == ConnectionState.MULTIPLAYER_UNMODDED) {
            logger.warn("Cannot modify synced option '{}' while connected to an unmodded server.", change.option().getKey());
            batch.rejected = true;
        } else {
            applyLocalChange(change.option(), change.clampedValue(), batch);
            if (this.state == ConnectionState.SINGLEPLAYER) {
                batch.requestOverrideBroadcast = true;
            }
        }
    }

    private void flush(SyncBatch batch) {
        if (!batch.serverOverrides.isEmpty() && !sendOverridesToServer(batch.serverOverrides)) {
            batch.rejected = true;
        }

        if (batch.localChanged) {
            save();
        } else if (batch.rejected) {
            fireChanged();
        }

        if (batch.sendInform) {
            networkHandler.sendInformToServer();
        }
        if (batch.requestOverrideBroadcast) {
            networkHandler.requestOverrideBroadcast();
        }
    }

    private boolean sendOverridesToServer(List<OptionChange> overrides) {
        if (!networkHandler.sendOverridesToServer(overrides)) return false;

        for (OptionChange override : overrides) {
            override.option().setValue(this.serverConfig, override.clampedValue());
        }
        return true;
    }

    private static class SyncBatch {
        final List<OptionChange> serverOverrides = new ArrayList<>();
        boolean localChanged = false;
        boolean sendInform = false;
        boolean requestOverrideBroadcast = false;
        boolean rejected = false;
    }
}
