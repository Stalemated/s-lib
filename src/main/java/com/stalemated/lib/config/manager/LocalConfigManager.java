package com.stalemated.lib.config.manager;

import com.stalemated.lib.config.io.ConfigProvider;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.OptionTree;
import com.stalemated.lib.util.io.FileUtils;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

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
    
    protected final List<Consumer<T>> loadListeners = new ArrayList<>();
    protected final List<Consumer<T>> saveListeners = new ArrayList<>();

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
     * Registers a listener to be invoked when the config is successfully loaded or initialized.
     */
    public void onConfigLoaded(Consumer<T> listener) {
        loadListeners.add(listener);
    }

    /**
     * Registers a listener to be invoked when the config is successfully saved to disk.
     */
    public void onConfigSaved(Consumer<T> listener) {
        saveListeners.add(listener);
    }

    /**
     * Registers the config.
     * This handles file verification, loading the config, creating backups if loading fails,
     * saving defaults if the file is new, and invoking lifecycle listeners.
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
        
        for (Consumer<T> listener : loadListeners) {
            listener.accept(getConfig());
        }
    }

    /**
     * Updates an option locally if the value has changed.
     * Respects metadata boundaries (clamping) and triggers disk save and listeners.
     *
     * @param optionKey The dot-separated option path.
     * @param value The new value.
     */
    public void updateOption(String optionKey, Object value) {
        OptionInfo option = optionTree.get(optionKey);
        if (option == null) {
            throw new IllegalArgumentException("Unknown config option: " + optionKey);
        }

        Object clampedValue = option.clampValue(value);
        if (Objects.equals(option.getValue(getConfig()), clampedValue)) {
            return;
        }

        option.setValue(getConfig(), clampedValue);
        save();
    }

    /**
     * Saves the current config instance to disk and invokes the save listeners.
     */
    public final void save() {
        T instance = provider.instance();
        if (instance != null && optionTree != null) {
            for (OptionInfo option : optionTree.all()) {
                option.enforceLimits(instance);
            }
        }
        
        provider.save();
        for (Consumer<T> listener : saveListeners) {
            listener.accept(getConfig());
        }
    }

    /**
     * Retrieves the config provider handling this manager's I/O.
     *
     * @return The config provider.
     */
    public ConfigProvider<T> getProvider() {
        return provider;
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
}
