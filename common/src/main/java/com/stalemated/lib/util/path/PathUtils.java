package com.stalemated.lib.util.path;

import com.stalemated.lib.helper.PlatformHelper;
import java.nio.file.Path;

public final class PathUtils {

    private PathUtils() {}

    /**
     * Simplifies path creation by resolving segments against the game's config directory.
     *
     * @param paths Subdirectories and filename segments (e.g., "my_mod", "config.json5").
     * @return The fully resolved absolute Path.
     */
    public static Path buildPath(String... paths) {
        Path current = PlatformHelper.INSTANCE.getConfigDir();
        for (String p : paths) {
            current = current.resolve(p);
        }
        return current;
    }
}
