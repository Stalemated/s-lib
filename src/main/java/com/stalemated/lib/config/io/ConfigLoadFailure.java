package com.stalemated.lib.config.io;

import java.nio.file.Path;
import java.util.List;

/**
 * Why a load did not go as written: the file could not be read or parsed, invalid values were
 * replaced by their defaults, or the file could not be written or backed up. The
 * {@code onLoadFailed} listeners get it, and {@link ConfigProvider#load()} returns it.
 *
 * @param file the config file
 * @param reason what went wrong, in English, for example
 *               {@code "3 invalid values were replaced by their defaults"}
 * @param rejectedValues the invalid values that were replaced by their defaults; empty if the
 *                       file could not be read, and without the keys that are not options
 */
public record ConfigLoadFailure(Path file, String reason, List<InvalidEntry> rejectedValues) {

    /**
     * Creates a failure with an unmodifiable copy of {@code rejectedValues}.
     *
     * @param file the config file
     * @param reason what went wrong
     * @param rejectedValues the invalid values that were replaced by their defaults
     * @throws NullPointerException if {@code rejectedValues} or one of its entries is {@code null}
     */
    public ConfigLoadFailure {
        rejectedValues = List.copyOf(rejectedValues);
    }

    /**
     * Creates a failure for a file that could not be read or is not valid JSON5. It has no
     * rejected values.
     *
     * @param file the config file
     * @param reason what went wrong, for example {@code "is not valid JSON5: <message>"}
     * @return the failure
     */
    public static ConfigLoadFailure unreadable(Path file, String reason) {
        return new ConfigLoadFailure(file, reason, List.of());
    }

    /**
     * Creates a failure for a file that could not be written or backed up after loading, so it
     * was left as it was.
     *
     * @param file the config file
     * @param reason what went wrong, for example {@code "could not be written: <message>"}
     * @param rejectedValues the values rejected while reading, or an empty list
     * @return the failure
     * @throws NullPointerException if {@code rejectedValues} or one of its entries is {@code null}
     */
    public static ConfigLoadFailure unwritten(Path file, String reason, List<InvalidEntry> rejectedValues) {
        return new ConfigLoadFailure(file, reason, rejectedValues);
    }

    /**
     * Creates a failure for values that were invalid and replaced by their defaults. The reason
     * is {@code "<n> invalid values were replaced by their defaults"}.
     *
     * @param file the config file
     * @param rejectedValues the invalid values
     * @return the failure
     * @throws NullPointerException if {@code rejectedValues} or one of its entries is {@code null}
     */
    public static ConfigLoadFailure invalidValues(Path file, List<InvalidEntry> rejectedValues) {
        return new ConfigLoadFailure(file, rejectedValues.size() + " invalid values were replaced by their defaults", rejectedValues);
    }
}
