package com.stalemated.lib.config.io;

import java.io.UncheckedIOException;
import java.util.Optional;

/**
 * Loads and saves the instance of a config. The default one uses a JSON5 file; pass your own to
 * {@code ConfigBuilder.provider(...)} to store it somewhere else. It works for local and synced
 * configs.
 * <p>
 * The manager writes changes straight into the object that {@link #instance()} returns, clamps
 * it and then calls {@link #save()}. Keep returning that same object until the next
 * {@link #load()}.
 *
 * @param <T> the config model type
 */
public interface ConfigProvider<T> {
    /**
     * Loads the config. It may replace the instance, and should create defaults when nothing is
     * stored yet.
     * <p>
     * Exceptions thrown here are not caught; they reach {@code register()}.
     *
     * @return why the stored config was unreadable or had invalid values, or empty if it loaded
     *         as written
     */
    Optional<ConfigLoadFailure> load();

    /**
     * Saves the current {@link #instance()}.
     *
     * @throws UncheckedIOException if it could not be written; the manager lets it through
     */
    void save();

    /**
     * Returns the live config instance.
     *
     * @return the instance; not {@code null} after {@link #load()}
     */
    T instance();
}
