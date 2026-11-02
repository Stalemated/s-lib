package com.stalemated.lib.config;

import com.stalemated.lib.config.permissions.ServerConfigPermissions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Builder for {@link SyncedConfigManager}.
 *
 * @param <T> The config data model type.
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

    public SyncedConfigBuilder<T> syncChannel(ResourceLocation channel) {
        this.syncChannel = Objects.requireNonNull(channel, "channel cannot be null");
        return this;
    }

    public SyncedConfigBuilder<T> permissionCheck(Predicate<ServerPlayer> check) {
        this.serverPermissionCheck = Objects.requireNonNull(check, "check cannot be null");
        return this;
    }

    SyncedConfigBuilder<T> transport(ConfigTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport cannot be null");
        return this;
    }

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
        //? if <1.21 {
        return new ResourceLocation(modId, "sync_config");
        //?} else {
        /*return ResourceLocation.fromNamespaceAndPath(modId, "sync_config");
        *///?}
    }
}
