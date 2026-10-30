package com.stalemated.lib.config;

import com.stalemated.lib.config.io.ConfigProvider;
import com.stalemated.lib.config.model.OptionTree;
import com.stalemated.lib.config.permissions.ServerConfigPermissions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Builder for {@link SyncedConfigManager}.
 *
 * @param <T> The config data model type.
 */
public final class SyncedConfigBuilder<T> extends ConfigBuilder<T, SyncedConfigManager<T>, SyncedConfigBuilder<T>> {

    private ResourceLocation syncChannel;
    private Predicate<ServerPlayer> serverPermissionCheck = ServerConfigPermissions.OP_ONLY;
    private final List<BiConsumer<ServerPlayer, T>> informListeners = new ArrayList<>();

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

    public SyncedConfigBuilder<T> onInform(BiConsumer<ServerPlayer, T> listener) {
        informListeners.add(Objects.requireNonNull(listener, "listener cannot be null"));
        return this;
    }

    @Override
    SyncedConfigManager<T> create(ConfigProvider<T> provider, Path path, Logger logger, OptionTree tree, Supplier<T> factory) {
        SyncedConfigManager<T> manager = new SyncedConfigManager<>(provider, path, logger, resolveChannel(), serverPermissionCheck, factory, tree);
        informListeners.forEach(manager::addInformListener);
        return manager;
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
