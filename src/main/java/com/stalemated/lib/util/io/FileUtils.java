package com.stalemated.lib.util.io;

import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class FileUtils {

    private FileUtils() {}

    /**
     * Deletes a file if it is empty.
     *
     * @param file The file to check and delete.
     * @param logger The {@link Logger} to report any errors.
     */
    public static void deleteIfEmpty(File file, Logger logger) {
        if (file.exists() && file.length() == 0) {
            try {
                boolean ignored = file.delete();
            } catch (Exception e) {
                logger.warn("Failed to delete empty file: ", e);
            }
        }
    }

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
     * Creates a safe backup of the given file.
     * The backup will be named using the original filename with a "_backup" suffix.
     *
     * @param path The path of the original file.
     * @param logger The logger to report success or failure.
     */
    public static void createBackupSafe(Path path, Logger logger) {
        if (path == null || !Files.exists(path)) return;

        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String backupFileName = dotIndex > 0
                ? fileName.substring(0, dotIndex) + "_backup" + fileName.substring(dotIndex)
                : fileName + "_backup";

        Path backupPath = path.getParent().resolve(backupFileName);
        try {
            Files.copy(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
            logger.error("A backup of the corrupted file was saved to: {}", backupPath.getFileName());
        } catch (Exception e) {
            logger.error("Failed to create backup of the corrupted file!", e);
        }
    }
}
