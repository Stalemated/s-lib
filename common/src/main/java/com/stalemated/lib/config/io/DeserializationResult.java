package com.stalemated.lib.config.io;

/**
 * Holds deserialization results and migration status.
 */
public record DeserializationResult<T>(
        T instance,
        boolean requiresSave,
        boolean partialCorruptionDetected
) {}
