package com.stalemated.lib.config;

import com.stalemated.lib.config.internal.ConfigRegistry;
import com.stalemated.lib.config.internal.SyncedConfigHooks;
import com.stalemated.lib.config.internal.ConfigSerializer;
import com.stalemated.lib.config.internal.OptionChange;
import com.stalemated.lib.config.network.ConnectionState;
import com.stalemated.lib.config.network.SyncMode;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A specialized ConfigManager that handles network sync between client and server.
 * <p>
 * This manager automates Server-To-Client (S2C) and Client-To-Server (C2S) syncing. It evaluates customizable
 * permission checks to decide if a client is allowed to change the config, with fallback logic to avoid desyncs.
 * The config file itself is loaded and saved by a {@link LocalConfigManager}.
 *
 * @param <T> The config data model class.
 */
public final class SyncedConfigManager<T> implements ConfigManager<T> {

    private final LocalConfigManager<T> localManager;
    private final ResourceLocation syncChannel;
    private final Predicate<ServerPlayer> serverPermissionCheck;
    private final Listeners<Consumer<T>> changeListeners = new Listeners<>(this::getName);
    private final Listeners<BiConsumer<ServerPlayer, T>> informListeners = new Listeners<>(this::getName);
    private final Map<UUID, T> informedConfigs = new ConcurrentHashMap<>();

    private volatile SyncView<T> view = new SyncView<>(ConnectionState.DISCONNECTED, null, true);

    private final ConfigNetworkHandler<T> networkHandler;

    SyncedConfigManager(LocalConfigManager<T> localManager, ResourceLocation syncChannel, Predicate<ServerPlayer> serverPermissionCheck, ConfigTransport transport) {
        if (!localManager.hasSerializer()) {
            throw new IllegalArgumentException("Synced config '" + localManager.getName() + "' needs a provider with a serializer to sync over the network");
        }
        this.localManager = localManager;
        this.syncChannel = syncChannel;
        this.serverPermissionCheck = serverPermissionCheck;
        this.networkHandler = new ConfigNetworkHandler<>(this, SyncChannels.of(syncChannel), transport);
    }

    /**
     * Registers the config.
     * This claims the config file and sync channel, registers the network receivers, loads the file
     * (creating it with defaults when it is new), and invokes the load and load failure listeners.
     *
     * @throws IllegalStateException If another registered config already uses the same file or sync channel.
     */
    public void register() {
        localManager.registerClaimedBy(() -> {
            ConfigRegistry.claimFileAndChannel(new RegistryHooks(), getConfigPath(), syncChannel);
            networkHandler.registerReceivers();
        });
    }

    @Override
    public Subscription onChange(Consumer<T> listener) {
        return changeListeners.add(listener);
    }

    void addInformListener(BiConsumer<ServerPlayer, T> listener) {
        informListeners.add(listener);
    }

    ConfigNetworkHandler<T> networkHandler() {
        return networkHandler;
    }

    public ConnectionState getConnectionState() {
        return view.state();
    }

    public boolean canChangeOverrides() {
        return view.canChangeOverrides();
    }

    boolean checkServerPermission(ServerPlayer player) {
        return serverPermissionCheck.test(player);
    }

    public Optional<T> informedConfigOf(ServerPlayer player) {
        return Optional.ofNullable(informedConfigs.get(player.getUUID()));
    }

    void acceptInformedConfig(ServerPlayer player, T informedConfig) {
        informedConfigs.put(player.getUUID(), informedConfig);
        informListeners.fire(listener -> listener.accept(player, informedConfig));
    }

    void forgetInformedConfig(UUID playerId) {
        informedConfigs.remove(playerId);
    }

    void acceptServerConfig(T serverConfig, boolean canChangeOverrides) {
        switchView(new SyncView<>(ConnectionState.MULTIPLAYER_MODDED, serverConfig, canChangeOverrides));
    }

    void onClientJoinMultiplayer() {
        if (view.state() != ConnectionState.MULTIPLAYER_MODDED) {
            switchView(new SyncView<>(ConnectionState.MULTIPLAYER_UNMODDED, configWithDefaultOverrides(), false));
        }
    }

    void onClientJoinSingleplayer() {
        switchView(new SyncView<>(ConnectionState.SINGLEPLAYER, null, true));
    }

    private T configWithDefaultOverrides() {
        T config = cloneConfig(local());
        T defaultValues = localManager.createDefaultOrClone();
        for (OptionInfo option : getOptionTree().getSyncedOptions()) {
            if (option.getSyncMode() == SyncMode.OVERRIDE_CLIENT) {
                option.setValue(config, option.getValue(defaultValues));
            }
        }
        return config;
    }

    private void switchView(SyncView<T> next) {
        T before = get();
        view = next;
        if (!getOptionTree().findChanges(before, get()).isEmpty()) {
            fireChanged();
        }
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
        return view.effective(local());
    }

    public T local() {
        return localManager.get();
    }

    void clearServerConfig() {
        switchView(new SyncView<>(ConnectionState.DISCONNECTED, null, true));
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
     * Clamps the local config, writes it to disk, and invokes the save and change listeners.
     * Fields mutated directly are only clamped here and never sent over the network;
     * use {@link #modify(Consumer)} for both.
     *
     * @throws UncheckedIOException If the file could not be written. Change listeners still run, save listeners do not.
     */
    public void save() {
        try {
            localManager.save();
        } finally {
            fireChanged();
        }
    }

    void fireChanged() {
        T current = get();
        changeListeners.fire(listener -> listener.accept(current));
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
     * @throws UncheckedIOException If the changed config could not be written, the new values stay in memory
     * and no packet is sent.
     */
    @Override
    public void modify(Consumer<T> mutator) {
        applyChanges(LocalConfigManager.changesMadeBy(this, get(), mutator));
    }

    @Override
    public void modifyOptions(Map<OptionInfo, ?> values) {
        applyChanges(LocalConfigManager.changesIn(getOptionTree(), get(), values));
    }

    private void applyChanges(List<OptionChange> changes) {
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
        T serverConfig = view.serverConfig();
        if (serverConfig != null) {
            option.setValue(serverConfig, value);
        }
        batch.localChanged = true;
    }

    private void applyInformChange(OptionInfo option, Object value, SyncBatch batch) {
        applyLocalChange(option, value, batch);
        ConnectionState state = view.state();
        if (state == ConnectionState.MULTIPLAYER_MODDED || state == ConnectionState.SINGLEPLAYER) {
            batch.sendInform = true;
        }
    }

    private void applyOverrideChange(OptionChange change, SyncBatch batch) {
        switch (view.state()) {
            case MULTIPLAYER_MODDED -> batch.serverOverrides.add(change);
            case MULTIPLAYER_UNMODDED -> {
                localManager.logger().warn("Cannot modify synced option '{}' while connected to an unmodded server.", change.option().getKey());
                batch.rejected = true;
            }
            case SINGLEPLAYER -> {
                applyLocalChange(change.option(), change.clampedValue(), batch);
                batch.requestOverrideBroadcast = true;
            }
            case DISCONNECTED -> {
                applyLocalChange(change.option(), change.clampedValue(), batch);
                batch.broadcastToClients = true;
            }
        }
    }

    private void flush(SyncBatch batch) {
        if (!batch.serverOverrides.isEmpty()) {
            boolean sent = sendOverridesToServer(batch.serverOverrides);
            batch.overridesApplied = sent;
            batch.rejected |= !sent;
        }

        if (batch.localChanged) {
            save();
        } else if (batch.overridesApplied || batch.rejected) {
            fireChanged();
        }

        if (batch.sendInform) {
            networkHandler.sendInformToServer();
        }
        if (batch.requestOverrideBroadcast) {
            networkHandler.requestOverrideBroadcast();
        }
        if (batch.broadcastToClients) {
            networkHandler.broadcastServerConfig();
        }
    }

    private boolean sendOverridesToServer(List<OptionChange> overrides) {
        if (!networkHandler.sendOverridesToServer(overrides)) return false;

        T serverConfig = view.serverConfig();
        for (OptionChange override : overrides) {
            override.option().setValue(serverConfig, override.clampedValue());
        }
        return true;
    }

    @Override
    public T cloneConfig(T source) {
        return localManager.cloneConfig(source);
    }

    @Override
    public OptionTree getOptionTree() {
        return localManager.getOptionTree();
    }

    @Override
    public String getName() {
        return localManager.getName();
    }

    public Path getConfigPath() {
        return localManager.getConfigPath();
    }

    ConfigSerializer<T> serializer() {
        return localManager.serializer();
    }

    private final class RegistryHooks implements SyncedConfigHooks {

        @Override
        public ConfigManager<?> manager() {
            return SyncedConfigManager.this;
        }

        @Override
        public void onPlayerJoin(ServerPlayer player) {
            sendConfigToPlayer(player);
        }

        @Override
        public void onPlayerLeave(UUID playerId) {
            forgetInformedConfig(playerId);
        }

        @Override
        public void onClientJoin(boolean singleplayer) {
            if (singleplayer) {
                onClientJoinSingleplayer();
            } else {
                onClientJoinMultiplayer();
            }
        }

        @Override
        public void onClientDisconnect() {
            clearServerConfig();
        }

        @Override
        public void unregisterReceivers() {
            networkHandler.unregisterReceivers();
        }
    }

    private record SyncView<T>(ConnectionState state, T serverConfig, boolean canChangeOverrides) {
        T effective(T local) {
            boolean onServer = state == ConnectionState.MULTIPLAYER_MODDED || state == ConnectionState.MULTIPLAYER_UNMODDED;
            return onServer && serverConfig != null ? serverConfig : local;
        }
    }

    private static class SyncBatch {
        final List<OptionChange> serverOverrides = new ArrayList<>();
        boolean localChanged = false;
        boolean sendInform = false;
        boolean requestOverrideBroadcast = false;
        boolean broadcastToClients = false;
        boolean rejected = false;
        boolean overridesApplied = false;
    }
}
