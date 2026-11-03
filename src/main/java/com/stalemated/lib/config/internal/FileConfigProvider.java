package com.stalemated.lib.config.internal;

import com.stalemated.lib.config.io.ConfigLoadFailure;
import com.stalemated.lib.config.io.ConfigProvider;
import com.stalemated.lib.config.io.InvalidEntry;
import com.stalemated.lib.util.io.FileUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
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
    public synchronized Optional<ConfigLoadFailure> load() {
        if (FileUtils.isNewOrEmpty(configPath.toFile())) {
            this.instance = defaultFactory.get();
            return saveLoaded(List.of());
        }

        DeserializationResult<T> result;
        try {
            result = serializer.deserialize(Files.readString(configPath, StandardCharsets.UTF_8), defaultFactory);
        } catch (IOException e) {
            logger.error("Could not read {}: {}", configPath, e.getMessage(), e);
            return Optional.of(ConfigLoadFailure.unreadable(configPath, "could not be read: " + e.getMessage()));
        } catch (ConfigSyntaxException e) {
            logger.error("{} is not valid JSON5 and was not loaded: {}", configPath, e.getMessage());
            String reason = backUp()
                    ? "is not valid JSON5: " + e.getMessage()
                    : "is not valid JSON5 and could not be backed up: " + e.getMessage();
            return Optional.of(ConfigLoadFailure.unreadable(configPath, reason));
        }

        return applyLoaded(result);
    }

    private Optional<ConfigLoadFailure> applyLoaded(DeserializationResult<T> result) {
        this.instance = result.instance();
        logDroppedContent(result);

        if (result.dropsFileContent() && !backUp()) {
            return Optional.of(ConfigLoadFailure.unwritten(configPath, "was left unchanged because it could not be backed up", result.rejectedValues()));
        }
        if (result.requiresSave()) {
            Optional<ConfigLoadFailure> unsaved = saveLoaded(result.rejectedValues());
            if (unsaved.isPresent()) return unsaved;
        }
        return result.hasRejectedValues()
                ? Optional.of(ConfigLoadFailure.invalidValues(configPath, result.rejectedValues()))
                : Optional.empty();
    }

    private boolean backUp() {
        try {
            Path backup = FileUtils.createTimestampedBackup(configPath);
            logger.warn("Saved a backup of {} to {}", configPath.getFileName(), backup.getFileName());
            return true;
        } catch (IOException e) {
            logger.error("Could not back up {}: {}", configPath, e.getMessage(), e);
            return false;
        }
    }

    private Optional<ConfigLoadFailure> saveLoaded(List<InvalidEntry> rejectedValues) {
        try {
            save();
            return Optional.empty();
        } catch (UncheckedIOException e) {
            logger.error("{}", e.getMessage(), e);
            return Optional.of(ConfigLoadFailure.unwritten(configPath, "could not be written: " + e.getCause().getMessage(), rejectedValues));
        }
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
    public synchronized void save() {
        Path tempPath = configPath.resolveSibling(configPath.getFileName() + ".tmp");
        try {
            if (configPath.getParent() != null) {
                Files.createDirectories(configPath.getParent());
            }

            Files.writeString(tempPath, serializer.serialize(instance()), StandardCharsets.UTF_8);
            replaceWith(tempPath);
        } catch (IOException e) {
            discard(tempPath, e);
            throw new UncheckedIOException("Could not save config to " + configPath + ": " + e.getMessage(), e);
        }
    }

    private void replaceWith(Path tempPath) throws IOException {
        try {
            Files.move(tempPath, configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempPath, configPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void discard(Path tempPath, IOException failure) {
        try {
            Files.deleteIfExists(tempPath);
        } catch (IOException cleanup) {
            failure.addSuppressed(cleanup);
        }
    }

    @Override
    public T instance() {
        return instance;
    }

    @Override
    public ConfigSerializer<T> getSerializer() {
        return serializer;
    }
}
