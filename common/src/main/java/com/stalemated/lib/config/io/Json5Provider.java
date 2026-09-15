package com.stalemated.lib.config.io;

import blue.endless.jankson.Jankson;
import com.stalemated.lib.config.ConfigProvider;
import com.stalemated.lib.config.io.record.DeserializationResult;
import com.stalemated.lib.config.model.OptionTree;
import com.stalemated.lib.util.io.FileUtils;
import org.slf4j.Logger;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * {@link ConfigProvider} implementation that serializes and deserializes configs to JSON5 files with comments.
 *
 * @param <T> The config data model class.
 */
public class Json5Provider<T> implements ConfigProvider<T> {

    private final Path configPath;
    private final Supplier<T> defaultFactory;
    private final Logger logger;
    private final Json5Serializer<T> serializer;

    private volatile T instance;

    public Json5Provider(
            Class<T> configClass,
            Path configPath,
            Supplier<T> defaultFactory,
            Logger logger,
            OptionTree optionTree
    ) {
        this(
                configClass,
                configPath,
                defaultFactory,
                logger,
                optionTree,
                null
        );
    }

    public Json5Provider(
            Class<T> configClass,
            Path configPath,
            Supplier<T> defaultFactory,
            Logger logger,
            OptionTree optionTree,
            Consumer<Jankson.Builder> janksonCustomizer
    ) {
        this.configPath = configPath;
        this.defaultFactory = defaultFactory;
        this.logger = logger;
        this.serializer = new Json5Serializer<>(configClass, optionTree, janksonCustomizer);
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

            if (instance == null) instance = defaultFactory.get();

            String serialized = serializer.serialize(instance);
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
        if (instance == null) instance = defaultFactory.get();
        return instance;
    }

    @Override
    public void setInstance(T instance) {
        this.instance = instance;
    }
}
