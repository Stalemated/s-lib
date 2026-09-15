package com.stalemated.lib.config.manager;

import com.stalemated.lib.config.io.ConfigProvider;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.OptionTree;
import com.stalemated.lib.util.io.FileUtils;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;

/**
 * Base config manager for handling local configs.
 * Provides default disk read/write strategies via a {@link ConfigProvider}.
 *
 * @param <T> The config data model class.
 */
public class LocalConfigManager<T> {
    
    protected final ConfigProvider<T> provider;
    protected final Path configPath;
    protected final Logger logger;
    protected final OptionTree optionTree;
    
    public boolean configLoadFailed = false;

    /**
     * Constructs a new LocalConfigManager.
     * 
     * @param provider A config provider.
     * @param configPath The absolute path to the config file.
     * @param logger The mod's logger used for warnings and error reporting.
     * @param optionTree The option tree mapping the configuration model.
     */
    public LocalConfigManager(ConfigProvider<T> provider, Path configPath, Logger logger, OptionTree optionTree) {
        this.provider = provider;
        this.configPath = configPath;
        this.logger = logger;
        this.optionTree = optionTree;
    }

    /**
     * Registers the config.
     * This handles file verification, loading the config, creating backups if loading fails,
     * saving defaults if the file is new, and invoking lifecycle hooks.
     */
    public final void register() {
        File configFile = configPath.toFile();
        boolean isNewOrEmpty = FileUtils.isNewOrEmpty(configFile);

        FileUtils.deleteIfEmpty(configFile, logger);
        boolean loaded = provider.load();

        if (!loaded && !isNewOrEmpty) {
            configLoadFailed = true;
            FileUtils.createBackupSafe(configFile.toPath(), logger);
        }

        if (isNewOrEmpty) save();
        onRegisterSuccess(isNewOrEmpty);
    }

    /**
     * Saves the current config instance to disk and invokes the post-save hook.
     */
    public final void save() {
        T instance = provider.instance();
        if (instance != null && optionTree != null) {
            for (OptionInfo option : optionTree.all()) {
                option.enforceLimits(instance);
            }
        }
        
        provider.save();
        onSaveSuccess();
    }

    /**
     * Retrieves the current config instance.
     * 
     * @return The config object.
     */
    public T getConfig() {
        return provider.instance();
    }

    /**
     * Retrieves the active config instance. For local configs, this is identical to {@link #getConfig()}.
     *
     * @return The active config object.
     */
    public T getActiveConfig() {
        return getConfig();
    }

    /**
     * Hook method invoked after the config is successfully registered (loaded or created).
     * Subclasses can override this to execute custom logic like reloading registries.
     * 
     * @param isNewOrEmpty true if the config file did not exist or was empty before this registration.
     */
    protected void onRegisterSuccess(boolean isNewOrEmpty) {
    }

    /**
     * Hook method invoked after the config is successfully saved to disk.
     * Subclasses can override this to trigger events or reload logic.
     */
    protected void onSaveSuccess() {
    }
}
