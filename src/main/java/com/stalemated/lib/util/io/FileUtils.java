package com.stalemated.lib.util.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * File helpers for config files: checking whether a file is new or empty, and making backups.
 */
public final class FileUtils {

    private static final DateTimeFormatter BACKUP_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private FileUtils() {}

    /**
     * Returns whether {@code file} does not exist or has zero bytes.
     *
     * @param file the file to inspect
     * @return {@code true} if the file is absent or empty
     */
    public static boolean isNewOrEmpty(File file) {
        return !file.exists() || file.length() == 0;
    }

    /**
     * Copies {@code path} next to itself as {@code <name>_backup_yyyyMMdd-HHmmss.<ext>}, using
     * local time. If that name is taken, it adds {@code -2}, {@code -3}, and so on before the
     * extension, so existing backups are never overwritten. A name without an extension gets
     * no dot.
     *
     * @param path path to the existing source file
     * @return path to the newly created backup file
     * @throws IOException if copying fails, for example when {@code path} does not exist
     */
    public static Path createTimestampedBackup(Path path) throws IOException {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        String extension = dotIndex > 0 ? fileName.substring(dotIndex) : "";
        String backupPrefix = baseName + "_backup_" + LocalDateTime.now().format(BACKUP_TIMESTAMP);

        Path backupPath = path.resolveSibling(backupPrefix + extension);
        for (int copy = 2; Files.exists(backupPath); copy++) {
            backupPath = path.resolveSibling(backupPrefix + "-" + copy + extension);
        }

        Files.copy(path, backupPath);
        return backupPath;
    }
}
