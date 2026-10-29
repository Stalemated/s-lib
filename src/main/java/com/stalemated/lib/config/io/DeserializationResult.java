package com.stalemated.lib.config.io;

import java.util.List;

/**
 * Holds deserialization results and migration status.
 */
public record DeserializationResult<T>(
        T instance,
        boolean requiresMigration,
        List<InvalidEntry> rejectedValues,
        List<InvalidEntry> unknownKeys
) {

    public boolean hasRejectedValues() {
        return !rejectedValues.isEmpty();
    }

    public boolean dropsFileContent() {
        return hasRejectedValues() || !unknownKeys.isEmpty();
    }

    public boolean requiresSave() {
        return requiresMigration || dropsFileContent();
    }
}
