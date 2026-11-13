package com.stalemated.lib.config.io;

import java.io.UncheckedIOException;
import java.util.Optional;

/**
 * Provider for config loading and saving.
 */
public interface ConfigProvider<T> {
    /**
     * Loads the config from disk.
     * @return Why the stored config was unreadable or had invalid values or empty if it loaded as written.
     */
    Optional<ConfigLoadFailure> load();

    /**
     * Saves the config to disk.
     * @throws UncheckedIOException If the config could not be written.
     */
    void save();

    /**
     * Gets the current config instance.
     * @return The config instance.
     */
    T instance();
}
