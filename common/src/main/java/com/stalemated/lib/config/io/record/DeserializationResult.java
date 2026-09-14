package com.stalemated.lib.config.io.record;

/**
 * Holds deserialization results and migration status.
 */
public record DeserializationResult<T>(T instance, boolean requiresSave) {
}
