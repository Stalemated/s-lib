package com.stalemated.lib.config.manager.builder;

import blue.endless.jankson.Jankson;
import com.stalemated.lib.config.io.ConfigProvider;
import com.stalemated.lib.config.manager.SyncedConfigManager;
import com.stalemated.lib.config.manager.builder.record.ResolvedConfig;
import com.stalemated.lib.config.permissions.ServerConfigPermissions;
import com.stalemated.lib.config.registry.ConfigRegistry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Builder for {@link SyncedConfigManager}.
 *
 * @param <T> The config data model type.
 */
public class SyncedConfigBuilder<T> {
    private final Class<T> configClass;
    private String modId;
    private Path configPath;
    private Logger logger;
    private Identifier syncChannel;
    private Predicate<ServerPlayerEntity> serverPermissionCheck = ServerConfigPermissions.OP_ONLY;
    private Supplier<T> defaultFactory;
    private ConfigProvider<T> provider;
    private Consumer<Jankson.Builder> janksonCustomizer;
    private final List<Consumer<T>> syncListeners = new ArrayList<>();

    public SyncedConfigBuilder(Class<T> configClass) {
        this.configClass = configClass;
    }

    public SyncedConfigBuilder<T> modId(String modId) {
        this.modId = modId;
        return this;
    }

    public SyncedConfigBuilder<T> configPath(Path path) {
        this.configPath = path;
        return this;
    }

    public SyncedConfigBuilder<T> logger(Logger logger) {
        this.logger = logger;
        return this;
    }

    public SyncedConfigBuilder<T> syncChannel(Identifier channel) {
        this.syncChannel = channel;
        return this;
    }

    public SyncedConfigBuilder<T> permissionCheck(Predicate<ServerPlayerEntity> check) {
        this.serverPermissionCheck = check;
        return this;
    }

    public SyncedConfigBuilder<T> janksonCustomizer(Consumer<Jankson.Builder> customizer) {
        this.janksonCustomizer = customizer;
        return this;
    }

    public SyncedConfigBuilder<T> defaultFactory(Supplier<T> defaultFactory) {
        this.defaultFactory = defaultFactory;
        return this;
    }

    public SyncedConfigBuilder<T> provider(ConfigProvider<T> provider) {
        this.provider = provider;
        return this;
    }

    public SyncedConfigBuilder<T> onSync(Consumer<T> listener) {
        if (listener != null) {
            this.syncListeners.add(listener);
        }
        return this;
    }

    /**
     * Builds the {@link SyncedConfigManager} instance applying conventions for any unspecified properties.
     *
     * @return The configured manager.
     */
    public SyncedConfigManager<T> build() {
        ResolvedConfig<T> resolved = ConfigBuilderHelper.resolveDefaults(
                configClass,
                modId,
                configPath,
                logger,
                defaultFactory,
                provider,
                janksonCustomizer,
                "SyncedConfigBuilder"
        );
        SyncedConfigManager<T> manager = getSyncedConfigManager(resolved);

        for (Consumer<T> listener : syncListeners) {
            manager.onConfigSynced(listener);
        }
        return manager;
    }

    private SyncedConfigManager<T> getSyncedConfigManager(ResolvedConfig<T> resolved) {
        Identifier resolvedChannel = syncChannel != null ? syncChannel : new Identifier(resolved.modId(), "sync_config");

        return new SyncedConfigManager<>(
                resolved.provider(),
                resolved.path(),
                resolved.logger(),
                resolvedChannel,
                configClass,
                serverPermissionCheck != null ? serverPermissionCheck : ServerConfigPermissions.OP_ONLY,
                resolved.factory(),
                resolved.tree()
        );
    }

    /**
     * Builds the manager, invokes {@link SyncedConfigManager#register()} to perform initial disk loading/creation,
     * and registers it with {@link ConfigRegistry} for automatic network lifecycle handling.
     *
     * @return The fully initialized and registered manager.
     */
    public SyncedConfigManager<T> register() {
        SyncedConfigManager<T> manager = build();
        manager.register();
        ConfigRegistry.register(manager);
        return manager;
    }
}
