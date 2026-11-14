package com.stalemated.lib.config;

import com.stalemated.lib.config.internal.ConfigRegistry;
import com.stalemated.lib.config.internal.SyncedConfigHooks;
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
 * Config saved to a file and synced between the server and its clients.
 * <p>
 * Each option has a {@link SyncMode}: {@code NONE} stays local, {@code OVERRIDE_CLIENT} comes
 * from the server, and {@code INFORM_SERVER} is sent by each client to the server. Changing
 * {@code OVERRIDE_CLIENT} options from a client needs the {@code permissionCheck} of the builder.
 * <p>
 * Get one from {@link SLibConfig#synced(String, Class)} with {@code build()} or
 * {@code register()}. Call {@link #register()} before reading or changing the config.
 *
 * @param <T> the config model type
 */
public final class SyncedConfigManager<T> implements ConfigManager<T> {

    private final LocalConfigManager<T> localManager;
    private final ResourceLocation syncChannel;
    private final Predicate<ServerPlayer> serverPermissionCheck;
    private final Listeners<Consumer<T>> changeListeners = new Listeners<>(this::getName);
    private final Listeners<Runnable> viewListeners = new Listeners<>(this::getName);
    private final Listeners<BiConsumer<ServerPlayer, T>> informListeners = new Listeners<>(this::getName);
    private final Map<UUID, T> informedConfigs = new ConcurrentHashMap<>();

    private volatile SyncView<T> view = new SyncView<>(ConnectionState.DISCONNECTED, null, true);

    private final ConfigNetworkHandler<T> networkHandler;

    SyncedConfigManager(LocalConfigManager<T> localManager, ResourceLocation syncChannel, Predicate<ServerPlayer> serverPermissionCheck, ConfigTransport transport) {
        this.localManager = localManager;
        this.syncChannel = syncChannel;
        this.serverPermissionCheck = serverPermissionCheck;
        this.networkHandler = new ConfigNetworkHandler<>(this, SyncChannels.of(syncChannel), transport);
    }

    /**
     * Registers the config: claims the file and the sync channel, registers the network
     * receivers, loads the file, runs the load failure listeners if the load failed, and then the
     * load listeners. The file is handled like in {@link LocalConfigManager#register()}.
     *
     * @throws IllegalStateException if this manager is already registered, if another registered
     *                               config already uses the same file or sync channel, or if
     *                               another receiver is registered on one of its channels
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

    /**
     * Adds a listener that runs on the client when the server values or the permission to edit
     * {@code OVERRIDE_CLIENT} options change, for example on joining or leaving a server, or
     * after {@code /op}. Config screens use it to rebuild themselves.
     *
     * @param listener runs after the change
     * @return a {@link Subscription} that removes the listener
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public Subscription onViewChange(Runnable listener) {
        return viewListeners.add(listener);
    }

    void addInformListener(BiConsumer<ServerPlayer, T> listener) {
        informListeners.add(listener);
    }

    ConfigNetworkHandler<T> networkHandler() {
        return networkHandler;
    }

    /**
     * Returns the connection state of this config on this side. A dedicated server is always
     * {@link ConnectionState#DISCONNECTED}.
     * <p>
     * The state is per config: a server with S-Lib that does not have this config leaves it at
     * {@link ConnectionState#MULTIPLAYER_UNMODDED}.
     *
     * @return the connection state
     */
    public ConnectionState getConnectionState() {
        return view.state();
    }

    /**
     * Returns whether this side may change {@code OVERRIDE_CLIENT} options right now.
     *
     * @return {@code true} while disconnected, in singleplayer and on a dedicated server;
     *         {@code false} on a server without the mod; on a server with the mod, what its
     *         {@code permissionCheck} said for this player
     */
    public boolean canChangeOverrides() {
        return view.canChangeOverrides();
    }

    boolean checkServerPermission(ServerPlayer player) {
        return serverPermissionCheck.test(player);
    }

    /**
     * Returns the last config that {@code player} sent: a copy of the local config of the server
     * with the {@link SyncMode#INFORM_SERVER} values of that client. Call it on the server.
     * <p>
     * A remote client sends it after the first config packet from the server and after each
     * change. The host of a singleplayer or LAN world sends it when joining, if the config has
     * {@code INFORM_SERVER} options. It is removed when the player leaves.
     *
     * @param player a player on this server
     * @return the config the player sent, or empty if it sent none yet
     * @throws NullPointerException if {@code player} is {@code null}
     */
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
        if (getOptionTree().hasOptionsSyncedAs(SyncMode.INFORM_SERVER)) {
            networkHandler.sendInformToServer();
        }
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
        boolean couldChangeOverrides = view.canChangeOverrides();
        view = next;
        boolean valuesChanged = !getOptionTree().findChanges(before, get()).isEmpty();
        if (valuesChanged) {
            fireChanged();
        }
        if (valuesChanged || couldChangeOverrides != next.canChangeOverrides()) {
            viewListeners.fire(Runnable::run);
        }
    }

    /**
     * Returns the current config.
     * <p>
     * On a server with the mod, this is the server's copy: its {@code OVERRIDE_CLIENT} values and
     * the local values for the rest. On a server without the mod, it is a local copy with the
     * {@code OVERRIDE_CLIENT} options at their defaults. In singleplayer, while disconnected and on
     * the server side, it is {@link #local()}. Changing fields of a copy by hand is never saved.
     *
     * @return the current config, never {@code null} after {@link #register()}
     * @throws IllegalStateException if called before {@link #register()}
     */
    @Override
    public T get() {
        return view.effective(local());
    }

    /**
     * Returns the local config, the one saved in the file, whatever the connection state.
     * <p>
     * On the server this is the config that counts. In singleplayer, the client and the
     * integrated server share this instance.
     *
     * @return the local config
     * @throws IllegalStateException if called before {@link #register()}
     */
    public T local() {
        return localManager.get();
    }

    void clearServerConfig() {
        switchView(new SyncView<>(ConnectionState.DISCONNECTED, null, true));
    }

    /**
     * Sends the {@code OVERRIDE_CLIENT} options of {@link #local()} to {@code player}, with whether
     * that player may change them. Does nothing for the host of a singleplayer or LAN world.
     * <p>
     * S-Lib already does this when a player joins, when {@code /op} or {@code /deop} changes their
     * operator status and, from 26.2, when the host changes who may use commands in World Options
     * or in the LAN options. Call it after changing the config outside {@link #modify(Consumer)},
     * or after a permission plugin changes what the player may edit. If the packet is over 262144
     * bytes, nothing is sent and an error is logged.
     *
     * @param player the player to send the config to
     */
    public void sendConfigToPlayer(ServerPlayer player) {
        networkHandler.sendConfigToPlayer(player);
    }

    /**
     * Clamps the local config, writes it to disk, and runs the save and change listeners.
     * Nothing is sent over the network.
     * <p>
     * Fields changed by hand are only clamped here and never sent to other players; use
     * {@link #modify(Consumer)} for both, or call {@link #sendConfigToPlayer(ServerPlayer)}.
     *
     * @throws IllegalStateException if called before {@link #register()}
     * @throws UncheckedIOException if writing fails; change listeners still run, save listeners
     *                              do not
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
     * Runs {@code mutator} on a copy of the current config and sends each changed option where its
     * {@link SyncMode} says.
     * <ul>
     *     <li>{@link SyncMode#NONE}: applied locally and saved.</li>
     *     <li>{@link SyncMode#INFORM_SERVER}: applied locally and saved, then sent to the server in
     *     singleplayer or on a server with the mod.</li>
     *     <li>{@link SyncMode#OVERRIDE_CLIENT}: on a dedicated server or while disconnected, applied,
     *     saved and sent to all players. In singleplayer, applied, saved and sent to LAN players.
     *     On a server with the mod, only the changed options go to the server and are applied to
     *     its copy, without saving locally; they are dropped with a warning if this player may not
     *     change them, and rejected with an error if the packet is too big for the server. On a
     *     server without the mod, they are rejected with a warning.</li>
     * </ul>
     * It saves and sends at most once per call. If {@code mutator} throws, nothing is applied.
     *
     * @param mutator changes the copy it gets
     * @throws IllegalStateException if called before {@link #register()}
     * @throws IllegalArgumentException if the mutator leaves a value that can never be valid,
     *                                  like {@code NaN}
     * @throws UncheckedIOException if writing local changes fails; the new values stay in memory
     */
    @Override
    public void modify(Consumer<T> mutator) {
        applyChanges(LocalConfigManager.changesMadeBy(this, get(), mutator));
    }

    /**
     * Sets several options in one change and sends them like {@link #modify(Consumer)}. Every
     * option must come from {@link #getOptionTree()}; this is checked before anything is applied.
     * Each value is clamped and values that do not change are skipped.
     *
     * @param values new value per option
     * @throws IllegalStateException if called before {@link #register()}
     * @throws IllegalArgumentException if an option belongs to another config, or a value can
     *                                  never be valid
     * @throws RuntimeException if a value does not match the type of its field
     * @throws UncheckedIOException if writing local changes fails
     */
    @Override
    public void modifyOptions(Map<OptionInfo, ?> values) {
        applyChanges(LocalConfigManager.changesIn(this, get(), values));
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
            case MULTIPLAYER_MODDED -> applyServerOverride(change, batch);
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

    private void applyServerOverride(OptionChange change, SyncBatch batch) {
        if (!view.canChangeOverrides()) {
            localManager.logger().warn("Dropped the change of synced option '{}' to {}: the server does not let this player change it", change.option().getKey(), change.clampedValue());
            return;
        }
        batch.serverOverrides.add(change);
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

    /**
     * Returns the path of the local config file.
     *
     * @return the config file path
     */
    public Path getConfigPath() {
        return localManager.getConfigPath();
    }

    private final class RegistryHooks implements SyncedConfigHooks {

        @Override
        public ConfigManager<?> manager() {
            return SyncedConfigManager.this;
        }

        @Override
        public void sendConfigTo(ServerPlayer player) {
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
