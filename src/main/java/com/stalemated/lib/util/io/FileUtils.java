package com.stalemated.lib.util.io;

import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class FileUtils {

    private static final DateTimeFormatter BACKUP_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private FileUtils() {}

    /**
     * Checks if a file does not exist or is empty.
     *
     * @param file The file to check.
     * @return True if the file does not exist or is empty.
     */
    public static boolean isNewOrEmpty(File file) {
        return !file.exists() || file.length() == 0;
    }

    /**
     * Copies the given file next to itself as {@code name_backup_yyyyMMdd-HHmmss.ext}.
     * Existing backups are never overwritten.
     *
     * @param path The path of the original file.
     * @param logger The logger to report success or failure.
     */
    public static void createTimestampedBackup(Path path, Logger logger) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        String extension = dotIndex > 0 ? fileName.substring(dotIndex) : "";
        String backupPrefix = baseName + "_backup_" + LocalDateTime.now().format(BACKUP_TIMESTAMP);

        Path backupPath = path.resolveSibling(backupPrefix + extension);
        for (int copy = 2; Files.exists(backupPath); copy++) {
            backupPath = path.resolveSibling(backupPrefix + "-" + copy + extension);
        }

        try {
            Files.copy(path, backupPath);
            logger.warn("Saved a backup of {} to {}", fileName, backupPath.getFileName());
        } catch (IOException e) {
            logger.error("Failed to back up {} to {}", path, backupPath, e);
        }
    }
}
