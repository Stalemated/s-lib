package com.stalemated.lib.config.io.json5;

import blue.endless.jankson.JsonElement;
import com.stalemated.lib.config.io.DeserializationResult;
import com.stalemated.lib.config.io.InvalidEntry;

import java.util.ArrayList;
import java.util.List;

final class LoadReport {

    private final List<InvalidEntry> rejectedValues = new ArrayList<>();
    private final List<InvalidEntry> unknownKeys = new ArrayList<>();
    private boolean requiresMigration;

    static InvalidEntry rejection(String path, JsonElement raw, Exception cause) {
        return new InvalidEntry(path, raw.toJson(false, false), rootMessage(cause));
    }

    void requireMigration() {
        requiresMigration = true;
    }

    void reject(String path, JsonElement raw, Exception cause) {
        rejectedValues.add(rejection(path, raw, cause));
    }

    void reject(String path, JsonElement raw, String reason) {
        rejectedValues.add(new InvalidEntry(path, raw.toJson(false, false), reason));
    }

    void rejectAll(List<InvalidEntry> rejections) {
        rejectedValues.addAll(rejections);
    }

    void unknownKey(String path, JsonElement raw) {
        unknownKeys.add(new InvalidEntry(path, raw.toJson(false, false), "not an option of this config"));
    }

    <T> DeserializationResult<T> toResult(T instance) {
        return new DeserializationResult<>(instance, requiresMigration, List.copyOf(rejectedValues), List.copyOf(unknownKeys));
    }

    private static String rootMessage(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}
