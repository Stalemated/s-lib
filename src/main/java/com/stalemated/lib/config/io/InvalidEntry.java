package com.stalemated.lib.config.io;

import org.jetbrains.annotations.NotNull;

/**
 * One value of the config file that was rejected when loading and replaced by its default. S-Lib
 * also uses it to log keys that are not options.
 *
 * @param path the key of the value in the file, with dots
 * @param rawJson the value as it was in the file, as compact JSON
 * @param reason why it was rejected, usually the message of the error that rejected it
 */
public record InvalidEntry(String path, String rawJson, String reason) {

    private static final int MAX_LOGGED_JSON_LENGTH = 120;

    /**
     * Returns {@code <path> = <rawJson> (<reason>)} for logs, with {@code rawJson} cut to 120
     * characters.
     *
     * @return the text for logs
     */
    @Override
    public @NotNull String toString() {
        String shownJson = rawJson.length() > MAX_LOGGED_JSON_LENGTH
                ? rawJson.substring(0, MAX_LOGGED_JSON_LENGTH) + "..."
                : rawJson;
        return path + " = " + shownJson + " (" + reason + ")";
    }
}
