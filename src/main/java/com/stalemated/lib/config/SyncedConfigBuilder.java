package com.stalemated.lib.config;

import com.stalemated.lib.config.permissions.ServerConfigPermissions;
import com.stalemated.lib.util.ResourceIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Fluent builder for {@link SyncedConfigManager}.
 * <p>
 * Get one from {@link SLibConfig#synced(String, Class)}.
 *
 * @param <T> the config model type
 */
public final class SyncedConfigBuilder<T> extends ConfigBuilder<T, SyncedConfigManager<T>, SyncedConfigBuilder<T>> {

    private ResourceLocation syncChannel;
    private Predicate<ServerPlayer> serverPermissionCheck = ServerConfigPermissions.OP_ONLY;
    private final List<BiConsumer<ServerPlayer, T>> informListeners = new ArrayList<>();
    private ConfigTransport transport = MinecraftConfigTransport.INSTANCE;

    SyncedConfigBuilder(String modId, Class<T> configClass) {
        super(modId, configClass);
    }

    @Override
    SyncedConfigBuilder<T> self() {
        return this;
    }

    /**
     * Sets the base sync channel. Defaults to {@code <modId>:sync_config}. Client and server must
     * use the same one.
     * <p>
     * S-Lib sends on three channels under it: {@code <channel>/s2c},
     * {@code <channel>/override_c2s} and {@code <channel>/inform_c2s}. If another config already
     * uses the channel, {@code register()} fails.
     *
     * @param channel the base channel id
     * @return this builder
     * @throws NullPointerException if {@code channel} is {@code null}
     */
    public SyncedConfigBuilder<T> syncChannel(ResourceLocation channel) {
        this.syncChannel = Objects.requireNonNull(channel, "channel cannot be null");
        return this;
    }

    /**
     * Sets who may change {@code OVERRIDE_CLIENT} options from a client. Defaults to
     * {@link ServerConfigPermissions#OP_ONLY}.
     * <p>
     * The server checks it when it sends the config to a player, to tell the client whether it
     * may edit, and again when a change arrives. The host of a singleplayer or LAN world can
     * always change them. If a permission plugin changes the result, call
     * {@link SyncedConfigManager#sendConfigToPlayer(ServerPlayer)}.
     *
     * @param check returns {@code true} if the player may change {@code OVERRIDE_CLIENT} options
     * @return this builder
     * @throws NullPointerException if {@code check} is {@code null}
     */
    public SyncedConfigBuilder<T> permissionCheck(Predicate<ServerPlayer> check) {
        this.serverPermissionCheck = Objects.requireNonNull(check, "check cannot be null");
        return this;
    }

    SyncedConfigBuilder<T> transport(ConfigTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport cannot be null");
        return this;
    }

    /**
     * Adds a listener that runs on the server thread each time a client sends its
     * {@code INFORM_SERVER} options: when it joins, if the config has such options, and after
     * each change.
     * <p>
     * It gets the player and a copy of the local config of the server with the
     * {@code INFORM_SERVER} values of that client. The same copy is kept for
     * {@link SyncedConfigManager#informedConfigOf(ServerPlayer)}.
     *
     * @param listener gets the player and the config it sent
     * @return this builder
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public SyncedConfigBuilder<T> onInform(BiConsumer<ServerPlayer, T> listener) {
        informListeners.add(Objects.requireNonNull(listener, "listener cannot be null"));
        return this;
    }

    @Override
    SyncedConfigManager<T> create(LocalConfigManager<T> localManager) {
        SyncedConfigManager<T> manager = new SyncedConfigManager<>(localManager, resolveChannel(), serverPermissionCheck, transport);
        informListeners.forEach(manager::addInformListener);
        return manager;
    }

    @Override
    void register(SyncedConfigManager<T> manager) {
        manager.register();
    }

    private ResourceLocation resolveChannel() {
        if (syncChannel != null) return syncChannel;
        return ResourceIds.of(modId, "sync_config");
    }
}
