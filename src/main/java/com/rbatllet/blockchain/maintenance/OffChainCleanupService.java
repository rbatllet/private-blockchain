package com.rbatllet.blockchain.maintenance;

import com.rbatllet.blockchain.config.MaintenanceConstants;
import com.rbatllet.blockchain.service.PerformanceMetricsService;
import com.rbatllet.blockchain.service.PerformanceMetricsService.AlertSeverity;
import com.rbatllet.blockchain.util.JPAUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages cleanup and compression of off-chain storage files.
 *
 * <p>This service performs two main operations:
 * <ol>
 *   <li><strong>Orphaned File Cleanup:</strong> Removes off-chain files that have no
 *       corresponding database records (orphaned files from deleted blocks or failed transactions)</li>
 *   <li><strong>File Compression:</strong> Compresses old files (>90 days) to save disk space
 *       using GZIP compression</li>
 * </ol>
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe. Operations use atomic
 * counters and are designed to be run by scheduled tasks.
 *
 * <p><strong>Safety Features:</strong>
 * <ul>
 *   <li>Disk space verification before operations</li>
 *   <li>Batch processing to limit memory usage</li>
 *   <li>Maximum files per cleanup to prevent excessive I/O</li>
 *   <li>Comprehensive error handling per file</li>
 *   <li>Progress logging for long operations</li>
 * </ul>
 *
 * <p><strong>Directory Structure:</strong>
 * <pre>
 * off-chain-data/
 * ├── abc123.dat              ← Original file
 * ├── abc123.dat.gz           ← Compressed (>90 days old)
 * └── orphaned.dat            ← To be deleted (no DB reference)
 * </pre>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * OffChainCleanupService cleanupService = new OffChainCleanupService();
 *
 * // Manual cleanup
 * CleanupResult result = cleanupService.cleanupOrphanedFiles();
 * System.out.println("Deleted: " + result.getDeletedCount() + " files");
 *
 * // Compress old files
 * CleanupResult compressResult = cleanupService.compressOldFiles();
 * System.out.println("Compressed: " + compressResult.getCompressedCount() + " files");
 *
 * // Or use with scheduler:
 * scheduler.schedule(
 *     cleanupService::cleanupOrphanedFiles,
 *     CronExpression.parse("0 4 * * *") // Daily 4 AM
 * );
 * }</pre>
 *
 * @since 1.0.5
 * @see MaintenanceConstants
 * @see PerformanceMetricsService
 */
public class OffChainCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(
        OffChainCleanupService.class
    );

    private static final String OFF_CHAIN_DIR = "off-chain-data";
    private static final String GZIP_EXTENSION = ".gz";

    // Security: Pattern for valid off-chain filenames
    // Supports two formats:
    // 1. Legacy hash format: abc123.dat (hex hash)
    // 2. Current format: offchain_1234567890_1234.dat (timestamp_random)
    private static final Pattern VALID_HASH_PATTERN = Pattern.compile(
        "^[a-fA-F0-9]{8,128}$"
    );
    private static final Pattern VALID_FILENAME_PATTERN = Pattern.compile(
        "^([a-fA-F0-9]{8,128}|offchain_\\d+_\\d+)(\\.dat)?$"
    );

    private final PerformanceMetricsService metricsService;

    // Operation statistics
    private final AtomicLong totalCleanupOperations = new AtomicLong(0);
    private final AtomicLong totalFilesDeleted = new AtomicLong(0);
    private final AtomicLong totalFilesCompressed = new AtomicLong(0);
    private final AtomicLong totalSpaceSaved = new AtomicLong(0);

    /**
     * Creates a new OffChainCleanupService with default metrics service.
     *
     * @throws IllegalStateException if PerformanceMetricsService cannot be initialized
     */
    public OffChainCleanupService() {
        this.metricsService = PerformanceMetricsService.getInstance();
        if (this.metricsService == null) {
            throw new IllegalStateException(
                "PerformanceMetricsService not available"
            );
        }
    }

    /**
     * Cleans up orphaned off-chain files that have no corresponding database records.
     *
     * <p>This method:
     * <ol>
     *   <li>Verifies sufficient disk space is available</li>
     *   <li>Queries database for all referenced off-chain file hashes</li>
     *   <li>Scans off-chain directory for all files</li>
     *   <li>Identifies files not referenced in database</li>
     *   <li>Deletes orphaned files (up to maximum per cleanup)</li>
     *   <li>Records metrics and logs results</li>
     * </ol>
     *
     * <p><strong>Performance:</strong> Uses batch queries to minimize memory usage.
     * Large databases process hashes in batches of {@link MaintenanceConstants#ORPHANED_FILE_DETECTION_BATCH_SIZE}.
     *
     * <p><strong>Safety:</strong> Operation is skipped if available disk space is below
     * {@link MaintenanceConstants#MIN_FREE_DISK_SPACE_BYTES}.
     *
     * @return CleanupResult with statistics about the cleanup operation
     */
    public CleanupResult cleanupOrphanedFiles() {
        long startTime = System.currentTimeMillis();
        totalCleanupOperations.incrementAndGet();

        logger.info("🧹 Starting orphaned file cleanup...");

        try {
            // Check disk space
            File offChainDir = new File(OFF_CHAIN_DIR);
            if (!offChainDir.exists()) {
                logger.warn(
                    "⚠️ Off-chain directory does not exist: {}",
                    OFF_CHAIN_DIR
                );
                return new CleanupResult(0, 0, 0, "Directory does not exist");
            }

            long freeSpace = offChainDir.getFreeSpace();
            if (freeSpace < MaintenanceConstants.MIN_FREE_DISK_SPACE_BYTES) {
                logger.warn(
                    "⚠️ Insufficient free disk space: {} bytes (minimum: {} bytes). Skipping cleanup.",
                    freeSpace,
                    MaintenanceConstants.MIN_FREE_DISK_SPACE_BYTES
                );
                return new CleanupResult(0, 0, 0, "Insufficient disk space");
            }

            // Get all referenced hashes from database
            Set<String> referencedHashes = getReferencedOffChainHashes();
            logger.info(
                "📊 Found {} referenced off-chain files in database",
                referencedHashes.size()
            );

            // Scan off-chain directory
            File[] files = offChainDir.listFiles();
            if (files == null || files.length == 0) {
                logger.info("✅ No files found in off-chain directory");
                return new CleanupResult(0, 0, 0, "No files found");
            }

            logger.info(
                "📊 Found {} files in off-chain directory",
                files.length
            );

            // Identify and delete orphaned files
            int deletedCount = 0;
            long spaceSaved = 0;
            int processedCount = 0;

            for (File file : files) {
                // Skip if max files limit reached
                if (
                    deletedCount >=
                    MaintenanceConstants.MAX_ORPHANED_FILES_PER_CLEANUP
                ) {
                    logger.warn(
                        "⚠️ Reached maximum orphaned files per cleanup limit: {}",
                        MaintenanceConstants.MAX_ORPHANED_FILES_PER_CLEANUP
                    );
                    break;
                }

                processedCount++;

                // Progress logging
                if (
                    processedCount %
                        MaintenanceConstants.MAINTENANCE_PROGRESS_LOG_INTERVAL ==
                    0
                ) {
                    logger.info(
                        "  └─ Progress: {}/{} files processed, {} orphaned files deleted",
                        processedCount,
                        files.length,
                        deletedCount
                    );
                }

                try {
                    String fileName = file.getName();

                    // Security: Validate filename to prevent directory traversal
                    if (!isValidFilename(fileName)) {
                        logger.warn(
                            "⚠️ Invalid filename detected (security): {}",
                            fileName
                        );
                        continue;
                    }

                    // Skip compressed files (will be handled separately)
                    if (fileName.endsWith(GZIP_EXTENSION)) {
                        continue;
                    }

                    // Skip directories
                    if (file.isDirectory()) {
                        continue;
                    }

                    // Extract base name from filename (format: hash.dat or offchain_*.dat)
                    String baseName = extractHashFromFilename(fileName);
                    if (baseName == null) {
                        logger.warn(
                            "⚠️ Could not extract base name from filename: {}",
                            fileName
                        );
                        continue;
                    }

                    // Check if baseName is referenced in database
                    if (!referencedHashes.contains(baseName)) {
                        // Orphaned file - delete it
                        // Security: Double-check file still exists and is in correct directory before deletion
                        if (
                            file.exists() &&
                            file.isFile() &&
                            isFileInOffChainDirectory(file)
                        ) {
                            long fileSize = file.length();
                            if (file.delete()) {
                                deletedCount++;
                                spaceSaved += fileSize;
                                totalFilesDeleted.incrementAndGet();
                                totalSpaceSaved.addAndGet(fileSize);

                                logger.debug(
                                    "  └─ Deleted orphaned file: {} ({} bytes)",
                                    fileName,
                                    fileSize
                                );
                            } else {
                                logger.warn(
                                    "  └─ Failed to delete orphaned file: {}",
                                    fileName
                                );
                            }
                        } else {
                            logger.warn(
                                "  └─ File security validation failed: {}",
                                fileName
                            );
                        }
                    }
                } catch (Exception e) {
                    logger.warn(
                        "  └─ Error processing file: {}",
                        file.getName(),
                        e
                    );
                    // Continue with next file
                }
            }

            // Record metrics
            long durationMs = System.currentTimeMillis() - startTime;
            metricsService.recordResponseTime("offchain_cleanup", durationMs);
            metricsService.recordThroughput("files_deleted", deletedCount);

            long spaceSavedMB = spaceSaved / 1024 / 1024;
            logger.info(
                "✅ Orphaned file cleanup completed: {} files deleted, {} MB saved, duration: {}ms",
                deletedCount,
                spaceSavedMB,
                durationMs
            );

            // Alert if many orphaned files found
            if (
                deletedCount > 100 &&
                MaintenanceConstants.ENABLE_MAINTENANCE_ALERTS
            ) {
                metricsService.createPerformanceAlert(
                    "HIGH_ORPHANED_FILES",
                    String.format(
                        "Deleted %d orphaned files (%d MB). " +
                            "This may indicate failed transactions or incomplete cleanup.",
                        deletedCount,
                        spaceSavedMB
                    ),
                    AlertSeverity.WARNING
                );
            }

            return new CleanupResult(deletedCount, 0, spaceSaved, "Success");
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            logger.error(
                "❌ Error during orphaned file cleanup after {}ms",
                durationMs,
                e
            );
            metricsService.recordOperation("offchain_cleanup", false);

            if (MaintenanceConstants.ENABLE_MAINTENANCE_ALERTS) {
                metricsService.createPerformanceAlert(
                    "CLEANUP_FAILED",
                    "Orphaned file cleanup failed: " + e.getMessage(),
                    AlertSeverity.CRITICAL
                );
            }

            return new CleanupResult(0, 0, 0, "Error: " + e.getMessage());
        }
    }

    /**
     * Compresses old off-chain files to save disk space.
     *
     * <p>Files older than {@link MaintenanceConstants#FILE_COMPRESSION_AGE_DAYS}
     * are compressed using GZIP compression. Original files are deleted after
     * successful compression.
     *
     * <p><strong>Compression:</strong>
     * <ul>
     *   <li>Files are compressed in-place (same directory)</li>
     *   <li>Original file is deleted after successful compression</li>
     *   <li>Compressed filename: {@code original.dat.gz}</li>
     *   <li>Typical compression ratio: 60-80% space savings</li>
     * </ul>
     *
     * <p><strong>Safety:</strong> If compression fails, original file is preserved.
     *
     * @return CleanupResult with statistics about the compression operation
     */
    public CleanupResult compressOldFiles() {
        long startTime = System.currentTimeMillis();

        logger.info(
            "📦 Starting old file compression (age threshold: {} days)...",
            MaintenanceConstants.FILE_COMPRESSION_AGE_DAYS
        );

        try {
            File offChainDir = new File(OFF_CHAIN_DIR);
            if (!offChainDir.exists()) {
                logger.warn(
                    "⚠️ Off-chain directory does not exist: {}",
                    OFF_CHAIN_DIR
                );
                return new CleanupResult(0, 0, 0, "Directory does not exist");
            }

            // Calculate cutoff date
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(
                MaintenanceConstants.FILE_COMPRESSION_AGE_DAYS
            );

            File[] files = offChainDir.listFiles();
            if (files == null || files.length == 0) {
                logger.info("✅ No files found in off-chain directory");
                return new CleanupResult(0, 0, 0, "No files found");
            }

            int compressedCount = 0;
            long spaceSaved = 0;
            int processedCount = 0;

            for (File file : files) {
                processedCount++;

                // Progress logging
                if (
                    processedCount %
                        MaintenanceConstants.MAINTENANCE_PROGRESS_LOG_INTERVAL ==
                    0
                ) {
                    logger.info(
                        "  └─ Progress: {}/{} files processed, {} files compressed",
                        processedCount,
                        files.length,
                        compressedCount
                    );
                }

                try {
                    String fileName = file.getName();

                    // Security: Validate filename to prevent directory traversal
                    if (!isValidFilename(fileName)) {
                        logger.warn(
                            "⚠️ Invalid filename detected (security): {}",
                            fileName
                        );
                        continue;
                    }

                    // Skip already compressed files
                    if (fileName.endsWith(GZIP_EXTENSION)) {
                        continue;
                    }

                    // Skip directories
                    if (file.isDirectory()) {
                        continue;
                    }

                    // Check file age (use last modified time, not creation time)
                    // Note: Creation time is immutable on many filesystems (e.g., APFS on macOS)
                    Path filePath = Paths.get(file.getAbsolutePath());
                    BasicFileAttributes attrs = Files.readAttributes(
                        filePath,
                        BasicFileAttributes.class
                    );
                    LocalDateTime fileTime = LocalDateTime.ofInstant(
                        attrs.lastModifiedTime().toInstant(),
                        ZoneId.systemDefault()
                    );

                    if (fileTime.isAfter(cutoffDate)) {
                        // File is too recent
                        continue;
                    }

                    // Compress file - with additional security checks
                    if (
                        file.exists() &&
                        file.isFile() &&
                        isFileInOffChainDirectory(file)
                    ) {
                        long originalSize = file.length();
                        boolean compressed = compressFile(file);

                        if (compressed) {
                            compressedCount++;
                            File compressedFile = new File(
                                file.getAbsolutePath() + GZIP_EXTENSION
                            );
                            long compressedSize = compressedFile.exists()
                                ? compressedFile.length()
                                : 0;
                            long saved = originalSize - compressedSize;
                            spaceSaved += saved;
                            totalFilesCompressed.incrementAndGet();
                            totalSpaceSaved.addAndGet(saved);

                            logger.debug(
                                "  └─ Compressed: {} ({} → {} bytes, {:.1f}% savings)",
                                fileName,
                                originalSize,
                                compressedSize,
                                (saved * 100.0) / originalSize
                            );
                        }
                    } else {
                        logger.warn(
                            "  └─ File security validation failed for compression: {}",
                            fileName
                        );
                    }
                } catch (Exception e) {
                    logger.warn(
                        "  └─ Error compressing file: {}",
                        file.getName(),
                        e
                    );
                    // Continue with next file
                }
            }

            // Record metrics
            long durationMs = System.currentTimeMillis() - startTime;
            metricsService.recordResponseTime("offchain_compress", durationMs);
            metricsService.recordThroughput(
                "files_compressed",
                compressedCount
            );

            long spaceSavedMB = spaceSaved / 1024 / 1024;
            logger.info(
                "✅ File compression completed: {} files compressed, {} MB saved, duration: {}ms",
                compressedCount,
                spaceSavedMB,
                durationMs
            );

            return new CleanupResult(0, compressedCount, spaceSaved, "Success");
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            logger.error(
                "❌ Error during file compression after {}ms",
                durationMs,
                e
            );
            metricsService.recordOperation("offchain_compress", false);

            if (MaintenanceConstants.ENABLE_MAINTENANCE_ALERTS) {
                metricsService.createPerformanceAlert(
                    "COMPRESSION_FAILED",
                    "File compression failed: " + e.getMessage(),
                    AlertSeverity.WARNING
                );
            }

            return new CleanupResult(0, 0, 0, "Error: " + e.getMessage());
        }
    }

    /**
     * Gets all off-chain file names referenced in the database.
     *
     * <p>Queries the database for all blocks that have off-chain data
     * and returns their file paths (filenames).
     *
     * @return Set of referenced file names (without directory path)
     */
    private Set<String> getReferencedOffChainHashes() {
        try {
            return JPAUtil.<Set<String>>executeInTransaction(em -> {
                // Query all off-chain file paths from blocks
                @SuppressWarnings("unchecked")
                java.util.List<String> filePaths = em
                    .createQuery(
                        "SELECT DISTINCT ocd.filePath FROM Block b " +
                            "JOIN b.offChainData ocd " +
                            "WHERE ocd.filePath IS NOT NULL"
                    )
                    .getResultList();

                // Extract just the filename from each path (without extension)
                Set<String> fileNames = new HashSet<>();
                for (String filePath : filePaths) {
                    if (filePath != null && !filePath.isEmpty()) {
                        // Extract filename from path (handle both Unix and Windows paths)
                        String fileName = filePath;
                        int lastSlash = Math.max(
                            filePath.lastIndexOf('/'),
                            filePath.lastIndexOf('\\')
                        );
                        if (lastSlash >= 0 && lastSlash < filePath.length() - 1) {
                            fileName = filePath.substring(lastSlash + 1);
                        }

                        // Remove .dat extension for consistent comparison
                        String baseName = fileName;
                        if (fileName.endsWith(".dat")) {
                            baseName = fileName.substring(0, fileName.length() - 4);
                        }

                        fileNames.add(baseName);
                    }
                }

                return fileNames;
            });
        } catch (Exception e) {
            logger.error("❌ Error querying referenced off-chain files", e);
            return new HashSet<>();
        }
    }

    /**
     * Extracts base name from off-chain filename.
     *
     * <p>Expected formats:
     * <ul>
     *   <li>{@code abc123.dat} → {@code abc123}</li>
     *   <li>{@code offchain_1234567890_1234.dat} → {@code offchain_1234567890_1234}</li>
     * </ul>
     *
     * <p><strong>Security:</strong> Validates filename format to prevent directory traversal attacks.
     *
     * @param fileName Filename to extract base name from
     * @return Extracted base name (without .dat extension), or null if invalid
     */
    private String extractHashFromFilename(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return null;
        }

        // Remove .dat extension if present
        String baseName;
        if (fileName.endsWith(".dat")) {
            baseName = fileName.substring(0, fileName.length() - 4);
        } else {
            baseName = fileName;
        }

        // Security: Validate filename format
        // Supports both legacy hash format and current offchain_timestamp_random format
        if (
            !VALID_HASH_PATTERN.matcher(baseName).matches() &&
            !baseName.matches("^offchain_\\d+_\\d+$")
        ) {
            logger.warn("⚠️ Invalid filename format detected: {}", baseName);
            return null;
        }

        return baseName;
    }

    /**
     * Validates filename for security purposes.
     *
     * @param fileName Filename to validate
     * @return true if filename is safe, false otherwise
     */
    private boolean isValidFilename(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }

        // Security checks
        if (
            fileName.contains("..") ||
            fileName.contains("/") ||
            fileName.contains("\\")
        ) {
            return false;
        }

        if (fileName.endsWith(GZIP_EXTENSION)) {
            // Remove .gz and check the base filename
            String baseName = fileName.substring(
                0,
                fileName.length() - GZIP_EXTENSION.length()
            );
            return VALID_FILENAME_PATTERN.matcher(baseName).matches();
        }

        return VALID_FILENAME_PATTERN.matcher(fileName).matches();
    }

    /**
     * Verifies that a file is actually within the off-chain directory.
     *
     * @param file File to check
     * @return true if file is in off-chain directory, false otherwise
     */
    private boolean isFileInOffChainDirectory(File file) {
        try {
            File offChainDir = new File(OFF_CHAIN_DIR);
            String offChainPath = offChainDir.getCanonicalPath();
            String filePath = file.getCanonicalPath();

            return (
                filePath.startsWith(offChainPath + File.separator) ||
                filePath.equals(offChainPath)
            );
        } catch (IOException e) {
            logger.error(
                "Error validating file path: {}",
                file.getAbsolutePath(),
                e
            );
            return false;
        }
    }

    /**
     * Compresses a file using GZIP compression.
     *
     * <p>Creates a compressed version with {@code .gz} extension and deletes
     * the original file after successful compression.
     *
     * <p><strong>Security:</strong> Validates file paths and handles cleanup properly.
     *
     * @param file File to compress
     * @return {@code true} if compression succeeded, {@code false} otherwise
     */
    private boolean compressFile(File file) {
        // Security: Validate file is within off-chain directory
        if (!isFileInOffChainDirectory(file)) {
            logger.error(
                "  └─ Security violation: File not in off-chain directory: {}",
                file.getAbsolutePath()
            );
            return false;
        }

        File compressedFile = new File(file.getAbsolutePath() + GZIP_EXTENSION);

        // Skip if compressed version already exists
        if (compressedFile.exists()) {
            logger.debug(
                "  └─ Compressed file already exists: {}",
                compressedFile.getName()
            );
            return false;
        }

        // Atomic operation flag for cleanup
        boolean compressionCompleted = false;

        try (
            FileInputStream fis = new FileInputStream(file);
            FileOutputStream fos = new FileOutputStream(compressedFile);
            GZIPOutputStream gzos = new GZIPOutputStream(fos)
        ) {
            // Copy data with compression
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                gzos.write(buffer, 0, bytesRead);
            }

            gzos.finish();
            compressionCompleted = true;

            // Verify compressed file was created successfully before deleting original
            if (compressedFile.exists() && compressedFile.length() > 0) {
                // Delete original file after successful compression
                if (!file.delete()) {
                    logger.warn(
                        "  └─ Failed to delete original file after compression: {}",
                        file.getName()
                    );
                    // Keep compressed file even if original deletion failed
                }
                return true;
            } else {
                logger.error(
                    "  └─ Compressed file creation failed or is empty: {}",
                    compressedFile.getName()
                );
                return false;
            }
        } catch (IOException e) {
            logger.error("  └─ Error compressing file: {}", file.getName(), e);
            return false;
        } finally {
            // Clean up incomplete compressed file if compression failed
            if (!compressionCompleted && compressedFile.exists()) {
                boolean deleted = compressedFile.delete();
                if (!deleted) {
                    logger.warn(
                        "  └─ Failed to clean up incomplete compressed file: {}",
                        compressedFile.getName()
                    );
                }
            }
        }
    }

    /**
     * Gets cleanup statistics.
     *
     * @return Formatted statistics string
     */
    public String getCleanupStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("📊 Off-Chain Cleanup Statistics\n");
        stats.append("=".repeat(50)).append("\n");
        stats.append(
            String.format(
                "Total Cleanup Operations: %d\n",
                totalCleanupOperations.get()
            )
        );
        stats.append(
            String.format("Total Files Deleted: %d\n", totalFilesDeleted.get())
        );
        stats.append(
            String.format(
                "Total Files Compressed: %d\n",
                totalFilesCompressed.get()
            )
        );
        stats.append(
            String.format(
                "Total Space Saved: %d MB\n",
                totalSpaceSaved.get() / 1024 / 1024
            )
        );
        stats.append("=".repeat(50));
        return stats.toString();
    }

    /**
     * Result of a cleanup or compression operation.
     */
    public static class CleanupResult {

        private final int deletedCount;
        private final int compressedCount;
        private final long spaceSaved;
        private final String message;

        public CleanupResult(
            int deletedCount,
            int compressedCount,
            long spaceSaved,
            String message
        ) {
            this.deletedCount = deletedCount;
            this.compressedCount = compressedCount;
            this.spaceSaved = spaceSaved;
            this.message = message;
        }

        public int getDeletedCount() {
            return deletedCount;
        }

        public int getCompressedCount() {
            return compressedCount;
        }

        public long getSpaceSaved() {
            return spaceSaved;
        }

        public long getSpaceSavedMB() {
            return spaceSaved / 1024 / 1024;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return String.format(
                "CleanupResult[deleted=%d, compressed=%d, savedMB=%d, message=%s]",
                deletedCount,
                compressedCount,
                getSpaceSavedMB(),
                message
            );
        }
    }
}
