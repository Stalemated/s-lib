package com.stalemated.lib.config;

/**
 * Provider for config loading and saving.
 */
public interface ConfigProvider<T> {
    /**
     * Loads the configuration from disk.
     * @return true if successful, false if it failed and requires a backup.
     */
    boolean load();

    /**
     * Saves the configuration to disk.
     */
    void save();

    /**
     * Gets the current configuration instance.
     * @return The configuration instance.
     */
    T instance();

    /**
     * Sets the active configuration instance in memory.
     * @param instance The new configuration instance.
     */
    default void setInstance(T instance) {}
}
