package com.stalemated.lib.config.io;

import org.jetbrains.annotations.NotNull;

public record InvalidEntry(String path, String rawJson, String reason) {

    private static final int MAX_LOGGED_JSON_LENGTH = 120;

    @Override
    public @NotNull String toString() {
        String shownJson = rawJson.length() > MAX_LOGGED_JSON_LENGTH
                ? rawJson.substring(0, MAX_LOGGED_JSON_LENGTH) + "..."
                : rawJson;
        return path + " = " + shownJson + " (" + reason + ")";
    }
}
