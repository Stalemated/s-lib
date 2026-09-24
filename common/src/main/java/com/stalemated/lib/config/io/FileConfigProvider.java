package com.stalemated.lib.config.io;

import com.stalemated.lib.util.io.FileUtils;
import org.slf4j.Logger;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;

/**
 * {@link ConfigProvider} implementation that serializes and deserializes configs to files.
 *
 * @param <T> The config data model class.
 */
public class FileConfigProvider<T> implements ConfigProvider<T> {

    private final Path configPath;
    private final Supplier<T> defaultFactory;
    private final Logger logger;
    private final ConfigSerializer<T> serializer;

    private volatile T instance;

    public FileConfigProvider(
            Path configPath,
            Supplier<T> defaultFactory,
            Logger logger,
            ConfigSerializer<T> serializer
    ) {
        this.configPath = configPath;
        this.defaultFactory = defaultFactory;
        this.logger = logger;
        this.serializer = serializer;
        this.instance = defaultFactory.get();
    }

    @Override
    public boolean load() {
        File configFile = configPath.toFile();
        if (!configFile.exists() || configFile.length() == 0) {
            this.instance = defaultFactory.get();
            save();
            return true;
        }

        try {
            String content = Files.readString(configPath, StandardCharsets.UTF_8);
            DeserializationResult<T> result = serializer.deserialize(content, defaultFactory);
            this.instance = result.instance();

            if (result.partialCorruptionDetected()) {
                logger.error("Syntax or type errors detected in {}. Creating a backup before salvaging...", configPath.getFileName());
                FileUtils.createBackupSafe(configPath, logger);
            }

            // If new fields were added, values were clamped, or corrupted types were salvaged, rewrite the file
            if (result.requiresSave()) {
                logger.info("Updating config file format for: {}", configPath.getFileName());
                save();
            }

            return true;
        } catch (Exception e) {
            logger.error("Failed to load config from {}: {}", configPath, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void save() {
        try {
            if (configPath.getParent() != null) {
                Files.createDirectories(configPath.getParent());
            }

            String serialized = serializer.serialize(instance());
            Files.writeString(
                    configPath,
                    serialized,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (Exception e) {
            logger.error("Failed to save config to {}: {}", configPath, e.getMessage(), e);
        }
    }

    @Override
    public T instance() {
        return instance;
    }

    @Override
    public void setInstance(T instance) {
        if (instance == null) {
            throw new IllegalArgumentException("Config instance cannot be null");
        }
        this.instance = instance;
    }

    @Override
    public ConfigSerializer<T> getSerializer() {
        return serializer;
    }
}
