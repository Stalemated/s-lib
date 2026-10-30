package com.stalemated.lib.config.io;

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
     */
    void save();

    /**
     * Gets the current config instance.
     * @return The config instance.
     */
    T instance();

    /**
     * Sets the active config instance in memory.
     * @param instance The new config instance.
     */
    default void setInstance(T instance) {}

    /**
     * Gets the associated config serializer if available.
     * @return The serializer, or null if not supported.
     */
    default ConfigSerializer<T> getSerializer() {
        return null;
    }
}
