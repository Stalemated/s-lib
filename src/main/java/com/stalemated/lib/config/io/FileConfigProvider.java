package com.stalemated.lib.config.io;

import com.stalemated.lib.config.io.exception.ConfigSyntaxException;
import com.stalemated.lib.util.io.FileUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
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
    public Optional<ConfigLoadFailure> load() {
        if (FileUtils.isNewOrEmpty(configPath.toFile())) {
            this.instance = defaultFactory.get();
            save();
            return Optional.empty();
        }

        DeserializationResult<T> result;
        try {
            result = serializer.deserialize(Files.readString(configPath, StandardCharsets.UTF_8), defaultFactory);
        } catch (IOException e) {
            logger.error("Could not read {}: {}", configPath, e.getMessage(), e);
            return Optional.of(ConfigLoadFailure.unreadable(configPath, "could not be read: " + e.getMessage()));
        } catch (ConfigSyntaxException e) {
            logger.error("{} is not valid JSON5 and was not loaded: {}", configPath, e.getMessage());
            FileUtils.createTimestampedBackup(configPath, logger);
            return Optional.of(ConfigLoadFailure.unreadable(configPath, "is not valid JSON5: " + e.getMessage()));
        }

        return applyLoaded(result);
    }

    private Optional<ConfigLoadFailure> applyLoaded(DeserializationResult<T> result) {
        this.instance = result.instance();
        logDroppedContent(result);

        if (result.dropsFileContent()) {
            FileUtils.createTimestampedBackup(configPath, logger);
        }
        if (result.requiresSave()) {
            save();
        }
        return result.hasRejectedValues()
                ? Optional.of(ConfigLoadFailure.invalidValues(configPath, result.rejectedValues()))
                : Optional.empty();
    }

    private void logDroppedContent(DeserializationResult<T> result) {
        if (!result.unknownKeys().isEmpty()) {
            logger.warn("Removing keys that are not options from {}: {}", configPath.getFileName(), result.unknownKeys());
        }
        if (result.hasRejectedValues()) {
            logger.error("Using defaults for invalid values in {}: {}", configPath.getFileName(), result.rejectedValues());
        }
    }

    @Override
    public void save() {
        Path tempPath = configPath.resolveSibling(configPath.getFileName() + ".tmp");
        try {
            if (configPath.getParent() != null) {
                Files.createDirectories(configPath.getParent());
            }

            Files.writeString(tempPath, serializer.serialize(instance()), StandardCharsets.UTF_8);
            Files.move(tempPath, configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
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
