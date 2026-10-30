package com.stalemated.lib.config.io;

import java.nio.file.Path;
import java.util.List;

public record ConfigLoadFailure(Path file, String reason, List<InvalidEntry> rejectedValues) {

    public ConfigLoadFailure {
        rejectedValues = List.copyOf(rejectedValues);
    }

    public static ConfigLoadFailure unreadable(Path file, String reason) {
        return new ConfigLoadFailure(file, reason, List.of());
    }

    public static ConfigLoadFailure invalidValues(Path file, List<InvalidEntry> rejectedValues) {
        return new ConfigLoadFailure(file, rejectedValues.size() + " invalid values were replaced by their defaults", rejectedValues);
    }
}
