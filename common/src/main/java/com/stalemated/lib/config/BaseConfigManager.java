package com.stalemated.lib.config;

import com.stalemated.lib.helper.PlatformHelper;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Base abstract class for Config Managers.
 * 
 * @param <T> The configuration instance type.
 */
public abstract class BaseConfigManager<T> {
    
    protected final ConfigProvider<T> provider;
    protected final Path configPath;
    protected final Logger logger;
    
    public boolean configLoadFailed = false;

    /**
     * Simplifies path creation by resolving segments against the game's config directory.
     * 
     * @param paths Subdirectories and filename segments (e.g., "my_mod", "config.json5").
     * @return The fully resolved absolute Path.
     */
    public static Path buildPath(String... paths) {
        Path current = PlatformHelper.INSTANCE.getConfigDir();
        for (String p : paths) {
            current = current.resolve(p);
        }
        return current;
    }

    /**
     * Constructs a new BaseConfigManager.
     * 
     * @param provider A config provider.
     * @param configPath The absolute path to the configuration file.
     * @param logger The mod's logger used for warnings and error reporting.
     */
    public BaseConfigManager(ConfigProvider<T> provider, Path configPath, Logger logger) {
        this.provider = provider;
        this.configPath = configPath;
        this.logger = logger;
    }

    /**
     * Registers the configuration.
     * This handles file verification, loading the config, creating backups if loading fails,
     * saving defaults if the file is new, and invoking lifecycle hooks.
     */
    public final void register() {
        File configFile = configPath.toFile();
        boolean isNewOrEmpty = isNewOrEmpty(configFile);

        deleteIfEmpty(configFile);

        boolean loaded = provider.load();

        if (!loaded && !isNewOrEmpty) {
            configLoadFailed = true;
            createBackup(configFile);
        }

        if (isNewOrEmpty) save();
        
        onRegisterSuccess(isNewOrEmpty);
    }

    /**
     * Saves the current config instance to disk and invokes the post-save hook.
     */
    public final void save() {
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
     * Hook method invoked after the config is successfully registered (loaded or created).
     * Subclasses can override this to execute custom logic like reloading registries.
     * 
     * @param isNewOrEmpty true if the config file did not exist or was empty before this registration.
     */
    protected void onRegisterSuccess(boolean isNewOrEmpty) {
    }

    /**
     * Hook method invoked after the configuration is successfully saved to disk.
     * Subclasses can override this to trigger events or reload logic.
     */
    protected void onSaveSuccess() {
    }

    private boolean isNewOrEmpty(File configFile) {
        return !configFile.exists() || configFile.length() == 0;
    }

    private void deleteIfEmpty(File configFile) {
        if (configFile.exists() && configFile.length() == 0) {
            try {
                boolean ignored = configFile.delete();
            } catch (Exception e) {
                logger.warn("Failed to delete empty config file: ", e);
            }
        }
    }

    private void createBackup(File configFile) {
        String fileName = configPath.getFileName().toString();

        int dotIndex = fileName.lastIndexOf('.');
        String backupFileName = dotIndex > 0 
            ? fileName.substring(0, dotIndex) + "_backup" + fileName.substring(dotIndex)
            : fileName + "_backup";
            
        File configBackup = configPath.getParent().resolve(backupFileName).toFile();
        try {
            Files.copy(configFile.toPath(), configBackup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.error("A backup of your broken config was saved to: {}", configBackup.getName());
        } catch (Exception e) {
            logger.error("Failed to create backup of the broken config!", e);
        }
    }
}
