package com.stalemated.lib.util.io;

import com.stalemated.lib.helper.PlatformHelper;
import java.nio.file.Path;

/**
 * Path resolution helpers relative to the mod loader's config directory.
 */
public final class PathUtils {

    private PathUtils() {}

    /**
     * Resolves path segments in order against {@link PlatformHelper#getConfigDir()}. With no
     * segments it returns the config directory. Like {@link Path#resolve(String)}, an absolute
     * segment replaces the path built so far.
     *
     * @param paths subdirectory and filename segments, for example
     *              {@code "my_mod", "config.json5"}
     * @return the resolved path
     */
    public static Path buildPath(String... paths) {
        Path current = PlatformHelper.getConfigDir();
        for (String p : paths) {
            current = current.resolve(p);
        }
        return current;
    }
}
