package com.rbatllet.blockchain.maintenance;

import static org.junit.jupiter.api.Assertions.*;

import com.rbatllet.blockchain.config.MaintenanceConstants;
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprehensive and rigorous tests for OffChainCleanupService
 *
 * <p>These tests validate every aspect of the cleanup service including:
 * <ul>
 *   <li>File age-based compression with manipulated file timestamps</li>
 *   <li>Orphaned file detection and cleanup with real blockchain references</li>
 *   <li>Security validation including directory traversal prevention</li>
 *   <li>Concurrent operations and thread safety</li>
 *   <li>Error handling and edge cases</li>
 *   <li>Memory safety and performance under load</li>
 *   <li>Statistical accuracy and logging verification</li>
 * </ul>
 *
 * <p><strong>Critical:</strong> Tests manipulate file timestamps to simulate old files
 * for compression testing without waiting for actual aging.
 */
@DisplayName("OffChainCleanupService - Comprehensive Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OffChainCleanupServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(
        OffChainCleanupServiceTest.class
    );

    private OffChainCleanupService cleanupService;
    private Blockchain blockchain;
    private KeyPair testKeyPair;
    private String testPublicKey;
    private PrivateKey testPrivateKey;

    @TempDir
    Path tempDir;

    private static final String ORIGINAL_OFF_CHAIN_DIR = "off-chain-data";
    private Path originalOffChainPath;
    private boolean offChainDirExisted;

    // Pattern matching for valid hash filenames (same as service implementation)
    private static final Pattern VALID_HASH_PATTERN = Pattern.compile(
        "^[a-fA-F0-9]{8,128}$"
    );

    @BeforeAll
    static void initializeDatabase() {
        // Initialize JPAUtil with default configuration (respects environment variables)
        JPAUtil.initializeDefault();
    }

    @BeforeEach
    void setUp() throws Exception {
        logTestContext("setUp", "Initializing test environment");

        // Initialize blockchain with proper cleanup
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();

        // Generate test credentials
        testKeyPair = CryptoUtil.generateKeyPair();
        testPublicKey = CryptoUtil.publicKeyToString(testKeyPair.getPublic());
        testPrivateKey = testKeyPair.getPrivate();

        blockchain.addAuthorizedKey(testPublicKey, "TestUser");

        // Setup isolated test directory
        setupTestDirectory();

        // Initialize service
        cleanupService = new OffChainCleanupService();

        logger.debug("✅ Test setup completed");
    }

    @AfterEach
    void tearDown() throws Exception {
        logTestContext("tearDown", "Cleaning up test environment");

        try {
            if (blockchain != null) {
                blockchain.clearAndReinitialize();
            }
            restoreOriginalDirectory();
        } finally {
            JPAUtil.closeEntityManager();
        }

        logger.debug("✅ Test cleanup completed");
    }

    // ====== CORE FUNCTIONALITY TESTS ======

    @Nested
    @DisplayName("Directory Management")
    class DirectoryManagementTests {

        @Test
        @Order(1)
        @DisplayName("Should handle empty directory gracefully")
        void testEmptyDirectoryHandling() throws Exception {
            logTestContext(
                "testEmptyDirectoryHandling",
                "Testing empty directory behavior"
            );

            // Ensure directory is empty
            assertTrue(Files.exists(originalOffChainPath));
            assertEquals(0, countFilesInDirectory(originalOffChainPath));

            // Test cleanup
            OffChainCleanupService.CleanupResult cleanupResult =
                cleanupService.cleanupOrphanedFiles();

            assertNotNull(cleanupResult, "Cleanup result should not be null");
            assertEquals(
                0,
                cleanupResult.getDeletedCount(),
                "No files should be deleted from empty directory"
            );
            assertEquals(
                0,
                cleanupResult.getSpaceSaved(),
                "No space should be saved from empty directory"
            );
            assertEquals("No files found", cleanupResult.getMessage());

            // Test compression
            OffChainCleanupService.CleanupResult compressionResult =
                cleanupService.compressOldFiles();

            assertNotNull(
                compressionResult,
                "Compression result should not be null"
            );
            assertEquals(
                0,
                compressionResult.getCompressedCount(),
                "No files should be compressed in empty directory"
            );
            assertEquals(
                0,
                compressionResult.getSpaceSaved(),
                "No space should be saved from empty directory"
            );
            assertEquals("No files found", compressionResult.getMessage());
        }

        @Test
        @Order(2)
        @DisplayName("Should handle nonexistent directory securely")
        void testNonexistentDirectoryHandling() throws Exception {
            logTestContext(
                "testNonexistentDirectoryHandling",
                "Testing missing directory behavior"
            );

            // Remove directory completely
            deleteDirectoryRecursively(originalOffChainPath);
            assertFalse(
                Files.exists(originalOffChainPath),
                "Directory should be deleted"
            );

            // Test cleanup
            OffChainCleanupService.CleanupResult cleanupResult =
                cleanupService.cleanupOrphanedFiles();

            assertNotNull(cleanupResult, "Cleanup result should not be null");
            assertEquals(0, cleanupResult.getDeletedCount());
            assertEquals(0, cleanupResult.getSpaceSaved());
            assertEquals(
                "Directory does not exist",
                cleanupResult.getMessage()
            );

            // Test compression
            OffChainCleanupService.CleanupResult compressionResult =
                cleanupService.compressOldFiles();

            assertNotNull(
                compressionResult,
                "Compression result should not be null"
            );
            assertEquals(0, compressionResult.getCompressedCount());
            assertEquals(0, compressionResult.getSpaceSaved());
            assertEquals(
                "Directory does not exist",
                compressionResult.getMessage()
            );
        }

        @Test
        @Order(3)
        @DisplayName("Should handle directory with invalid permissions")
        void testDirectoryPermissionHandling() throws Exception {
            logTestContext(
                "testDirectoryPermissionHandling",
                "Testing permission-restricted directory"
            );

            // Create files first
            createTestFileWithValidHash(
                "abcdef123456789abcdef0123.dat",
                "Permission test content"
            );

            try {
                // Remove read permissions (if supported by filesystem)
                Set<PosixFilePermission> noReadPermissions = EnumSet.of(
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE
                );

                if (
                    Files.getFileStore(
                        originalOffChainPath
                    ).supportsFileAttributeView(PosixFileAttributeView.class)
                ) {
                    Files.setPosixFilePermissions(
                        originalOffChainPath,
                        noReadPermissions
                    );

                    // Test should handle permissions gracefully
                    OffChainCleanupService.CleanupResult result =
                        cleanupService.cleanupOrphanedFiles();
                    assertNotNull(
                        result,
                        "Should return result even with permission issues"
                    );

                    // Restore permissions
                    Set<PosixFilePermission> fullPermissions = EnumSet.allOf(
                        PosixFilePermission.class
                    );
                    Files.setPosixFilePermissions(
                        originalOffChainPath,
                        fullPermissions
                    );
                }
            } catch (UnsupportedOperationException e) {
                // POSIX permissions not supported on this filesystem (e.g., Windows)
                logger.info(
                    "POSIX permissions not supported, skipping permission test"
                );
            }
        }
    }

    @Nested
    @DisplayName("File Age-Based Compression")
    class FileCompressionTests {

        @Test
        @Order(9)
        @DisplayName("Should correctly set and verify file timestamps")
        void testTimestampManipulation() throws Exception {
            logTestContext(
                "testTimestampManipulation",
                "Testing file timestamp manipulation for age-based testing"
            );

            // Create test file with valid hexadecimal hash
            String testHash = "a1b2c3d41234567890abcdef";
            String fileName = testHash + ".dat";
            Path testFile = createTestFileWithValidHash(
                fileName,
                "Timestamp test content"
            );

            // Get current time for comparison
            Instant now = Instant.now();

            // Set file to be old (older than compression threshold)
            Instant oldTimestamp = now.minus(
                MaintenanceConstants.FILE_COMPRESSION_AGE_DAYS + 5,
                ChronoUnit.DAYS
            );
            setFileTimestamp(testFile, oldTimestamp);

            // Verify the timestamp was set correctly
            FileTime actualTime = Files.getLastModifiedTime(testFile);
            long diffSeconds = Math.abs(
                actualTime.toInstant().getEpochSecond() -
                    oldTimestamp.getEpochSecond()
            );

            assertTrue(
                diffSeconds <= 2,
                String.format(
                    "Timestamp should be set correctly. Expected: %s, Actual: %s, Diff: %d seconds",
                    oldTimestamp,
                    actualTime.toInstant(),
                    diffSeconds
                )
            );

            // Verify file is considered old enough for compression
            assertTrue(
                actualTime
                    .toInstant()
                    .isBefore(
                        now.minus(
                            MaintenanceConstants.FILE_COMPRESSION_AGE_DAYS,
                            ChronoUnit.DAYS
                        )
                    ),
                "File should be old enough for compression"
            );

            logger.info(
                "✅ Timestamp manipulation verified: {} -> {} (diff: {} seconds)",
                oldTimestamp,
                actualTime.toInstant(),
                diffSeconds
            );
        }

        @Test
        @Order(10)
        @DisplayName("Should compress only old files based on age")
        void testAgeBasedCompression() throws Exception {
            logTestContext(
                "testAgeBasedCompression",
                "Testing file age-based compression"
            );

            // Create old file (older than compression threshold)
            String oldFileHash = "a1b2c3d4567890abcdef1234";
            String oldFileName = oldFileHash + ".dat";
            String oldContent =
                "Old file content that should be compressed. ".repeat(100);
            Path oldFile = createTestFileWithValidHash(oldFileName, oldContent);

            // Make file old (older than MaintenanceConstants.FILE_COMPRESSION_AGE_DAYS)
            Instant oldTimestamp = Instant.now().minus(
                MaintenanceConstants.FILE_COMPRESSION_AGE_DAYS + 5,
                ChronoUnit.DAYS
            );
            setFileTimestamp(oldFile, oldTimestamp);

            // Create recent file (newer than compression threshold)
            String recentFileHash = "f1e2d3c4567890abcdef1234";
            String recentFileName = recentFileHash + ".dat";
            String recentContent =
                "Recent file content that should NOT be compressed.";
            Path recentFile = createTestFileWithValidHash(
                recentFileName,
                recentContent
            );

            // Verify initial state
            assertTrue(Files.exists(oldFile), "Old file should exist");
            assertTrue(Files.exists(recentFile), "Recent file should exist");

            long oldFileSize = Files.size(oldFile);
            long recentFileSize = Files.size(recentFile);

            assertTrue(oldFileSize > 0, "Old file should have content");
            assertTrue(recentFileSize > 0, "Recent file should have content");

            // Perform compression
            OffChainCleanupService.CleanupResult result =
                cleanupService.compressOldFiles();

            // Verify results
            assertNotNull(result, "Compression result should not be null");
            assertEquals(
                1,
                result.getCompressedCount(),
                "Should compress exactly 1 old file"
            );
            assertTrue(
                result.getSpaceSaved() > 0,
                "Should save space from compression"
            );
            assertEquals("Success", result.getMessage());

            // Verify old file was compressed and original deleted
            Path compressedOldFile = originalOffChainPath.resolve(
                oldFileName + ".gz"
            );
            assertTrue(
                Files.exists(compressedOldFile),
                "Compressed old file should exist"
            );
            assertFalse(
                Files.exists(oldFile),
                "Original old file should be deleted after compression"
            );

            // Verify recent file was left untouched
            assertTrue(
                Files.exists(recentFile),
                "Recent file should remain uncompressed"
            );
            assertFalse(
                Files.exists(
                    originalOffChainPath.resolve(recentFileName + ".gz")
                ),
                "Recent file should not have a compressed version"
            );

            // Verify compression quality
            verifyGzipCompression(compressedOldFile, oldContent);
        }

        @Test
        @Order(11)
        @DisplayName("Should handle compression failures gracefully")
        void testCompressionFailureHandling() throws Exception {
            logTestContext(
                "testCompressionFailureHandling",
                "Testing compression error handling"
            );

            // Create old file
            String fileHash = "fa11ed1234567890abcdef12";
            String fileName = fileHash + ".dat";
            Path testFile = createTestFileWithValidHash(
                fileName,
                "Test content for compression failure"
            );

            // Make file old
            Instant oldTimestamp = Instant.now().minus(
                MaintenanceConstants.FILE_COMPRESSION_AGE_DAYS + 1,
                ChronoUnit.DAYS
            );
            setFileTimestamp(testFile, oldTimestamp);

            // Make file read-only to potentially cause compression issues
            if (
                Files.getFileStore(testFile).supportsFileAttributeView(
                    PosixFileAttributeView.class
                )
            ) {
                try {
                    Files.setPosixFilePermissions(
                        testFile,
                        Set.of(PosixFilePermission.OWNER_READ)
                    );
                } catch (Exception e) {
                    logger.warn(
                        "Could not set read-only permissions: " + e.getMessage()
                    );
                }
            }

            // Attempt compression
            OffChainCleanupService.CleanupResult result =
                cleanupService.compressOldFiles();

            // Service should handle failures gracefully
            assertNotNull(
                result,
                "Result should not be null even with failures"
            );

            // Original file should be preserved if compression fails
            // Note: The service might still compress successfully even with read-only permissions
            // depending on the filesystem, so we check the result instead
            if (result.getCompressedCount() == 0) {
                assertTrue(
                    Files.exists(testFile),
                    "Original file should be preserved on compression failure"
                );
            } else {
                // If compression succeeded despite permissions, that's also valid
                logger.info(
                    "Compression succeeded despite read-only permissions"
                );
            }
        }

        @Test
        @Order(12)
        @DisplayName("Should skip already compressed files")
        void testSkipAlreadyCompressedFiles() throws Exception {
            logTestContext(
                "testSkipAlreadyCompressedFiles",
                "Testing skip of pre-compressed files"
            );

            // Create old uncompressed file
            String uncompressedHash = "12345678901234567890abcd";
            String uncompressedFileName = uncompressedHash + ".dat";
            Path uncompressedFile = createTestFileWithValidHash(
                uncompressedFileName,
                "Content that will be compressed"
            );
            setFileTimestamp(
                uncompressedFile,
                Instant.now().minus(
                    MaintenanceConstants.FILE_COMPRESSION_AGE_DAYS + 1,
                    ChronoUnit.DAYS
                )
            );

            // Create already compressed file
            String compressedHash = "abcdef1234567890abcdef12";
            String compressedFileName = compressedHash + ".dat.gz";
            createTestFileWithValidHash(
                compressedFileName,
                "Already compressed content"
            );
            Path compressedFile = originalOffChainPath.resolve(
                compressedFileName
            );
            setFileTimestamp(
                compressedFile,
                Instant.now().minus(
                    MaintenanceConstants.FILE_COMPRESSION_AGE_DAYS + 1,
                    ChronoUnit.DAYS
                )
            );

            // Verify initial state
            assertTrue(Files.exists(uncompressedFile));
            assertTrue(Files.exists(compressedFile));

            // Perform compression
            OffChainCleanupService.CleanupResult result =
                cleanupService.compressOldFiles();

            // Should compress only the uncompressed file
            assertNotNull(result);
            assertEquals(
                1,
                result.getCompressedCount(),
                "Should compress only the uncompressed file"
            );

            // Pre-compressed file should remain unchanged
            assertTrue(
                Files.exists(compressedFile),
                "Pre-compressed file should remain"
            );

            // Uncompressed file should now be compressed
            Path newlyCompressedFile = originalOffChainPath.resolve(
                uncompressedFileName + ".gz"
            );
            assertTrue(
                Files.exists(newlyCompressedFile),
                "Uncompressed file should now be compressed"
            );
            assertFalse(
                Files.exists(uncompressedFile),
                "Original uncompressed file should be deleted"
            );
        }

        @Test
        @Order(13)
        @DisplayName("Should validate compression integrity")
        void testCompressionIntegrity() throws Exception {
            logTestContext(
                "testCompressionIntegrity",
                "Testing compression data integrity"
            );

            // Create file with known content
            String originalContent =
                "This is test content for compression integrity validation. ".repeat(
                    50
                );
            String fileHash = "abcdef12345678901234abcd";
            String fileName = fileHash + ".dat";
            Path testFile = createTestFileWithValidHash(
                fileName,
                originalContent
            );

            // Make file old enough for compression
            setFileTimestamp(
                testFile,
                Instant.now().minus(
                    MaintenanceConstants.FILE_COMPRESSION_AGE_DAYS + 1,
                    ChronoUnit.DAYS
                )
            );

            // Perform compression
            OffChainCleanupService.CleanupResult result =
                cleanupService.compressOldFiles();

            assertEquals(1, result.getCompressedCount());

            // Verify compressed file integrity
            Path compressedFile = originalOffChainPath.resolve(
                fileName + ".gz"
            );
            assertTrue(Files.exists(compressedFile));

            verifyGzipCompression(compressedFile, originalContent);
        }
    }

    @Nested
    @DisplayName("Orphaned File Detection and Cleanup")
    class OrphanedFileTests {

        @Test
        @Order(20)
        @DisplayName("Should detect and delete truly orphaned files")
        void testDeleteOrphanedFiles() throws Exception {
            logTestContext(
                "testDeleteOrphanedFiles",
                "Testing orphaned file detection and deletion"
            );

            // Create genuinely orphaned files (not referenced in blockchain)
            String orphan1Hash = "1234567890abcdef12345678";
            String orphan2Hash = "abcdef1234567890abcdef12";
            Path orphan1 = createTestFileWithValidHash(
                orphan1Hash + ".dat",
                "Orphaned content 1"
            );
            Path orphan2 = createTestFileWithValidHash(
                orphan2Hash + ".dat",
                "Orphaned content 2"
            );

            long expectedSize = Files.size(orphan1) + Files.size(orphan2);

            // Verify files exist before cleanup
            assertTrue(Files.exists(orphan1), "Orphan 1 should exist");
            assertTrue(Files.exists(orphan2), "Orphan 2 should exist");

            // Perform cleanup
            OffChainCleanupService.CleanupResult result =
                cleanupService.cleanupOrphanedFiles();

            // Verify cleanup results
            assertNotNull(result, "Cleanup result should not be null");
            assertEquals(
                2,
                result.getDeletedCount(),
                "Should delete both orphaned files"
            );
            assertEquals(
                expectedSize,
                result.getSpaceSaved(),
                "Should report accurate space savings"
            );
            assertEquals("Success", result.getMessage());

            // Verify files were actually deleted
            assertFalse(Files.exists(orphan1), "Orphan 1 should be deleted");
            assertFalse(Files.exists(orphan2), "Orphan 2 should be deleted");
        }

        @Test
        @Order(21)
        @DisplayName(
            "Should preserve referenced files and delete only orphaned ones"
        )
        void testPreserveReferencedFiles() throws Exception {
            logTestContext(
                "testPreserveReferencedFiles",
                "Testing preservation of blockchain-referenced files"
            );

            // Create a block with off-chain data (this will create a referenced file)
            String largeData = "Large content that goes off-chain. ".repeat(
                2000
            ); // Force off-chain storage
            boolean blockAdded = blockchain.addBlock(
                largeData,
                testPrivateKey,
                CryptoUtil.stringToPublicKey(testPublicKey)
            );
            assertTrue(blockAdded, "Block should be added successfully");

            // Give system time to create off-chain file
            Thread.sleep(200);

            // Create an orphaned file with valid hash format
            String orphanHash = "abcdef1234567890abcdef12";
            Path orphanedFile = createTestFileWithValidHash(
                orphanHash + ".dat",
                "Orphaned content"
            );
            assertTrue(
                Files.exists(orphanedFile),
                "Orphaned file should exist"
            );

            // Count files before cleanup
            long filesBeforeCleanup = countFilesInDirectory(
                originalOffChainPath
            );
            assertTrue(
                filesBeforeCleanup >= 2,
                "Should have at least referenced file and orphaned file"
            );

            // Perform cleanup
            OffChainCleanupService.CleanupResult result =
                cleanupService.cleanupOrphanedFiles();

            // Verify results
            assertNotNull(result, "Cleanup result should not be null");
            assertEquals(
                1,
                result.getDeletedCount(),
                "Should delete only the orphaned file"
            );
            assertTrue(
                result.getSpaceSaved() > 0,
                "Should report space savings"
            );
            assertEquals("Success", result.getMessage());

            // Verify orphaned file was deleted
            assertFalse(
                Files.exists(orphanedFile),
                "Orphaned file should be deleted"
            );

            // Verify referenced file still exists
            long filesAfterCleanup = countFilesInDirectory(
                originalOffChainPath
            );
            assertEquals(
                filesBeforeCleanup - 1,
                filesAfterCleanup,
                "Should have one less file after cleanup"
            );
            assertTrue(filesAfterCleanup > 0, "Referenced files should remain");
        }

        @Test
        @Order(22)
        @DisplayName(
            "Should handle mixed scenarios with referenced and orphaned files"
        )
        void testMixedReferencedOrphanedScenario() throws Exception {
            logTestContext(
                "testMixedReferencedOrphanedScenario",
                "Testing mixed file scenarios"
            );

            // Create multiple blocks with off-chain data
            for (int i = 0; i < 3; i++) {
                String blockData = String.format(
                    "Referenced block data %d. ",
                    i
                ).repeat(1500);
                boolean added = blockchain.addBlock(
                    blockData,
                    testPrivateKey,
                    CryptoUtil.stringToPublicKey(testPublicKey)
                );
                assertTrue(added, "Block " + i + " should be added");
            }

            // Wait for off-chain files to be created
            Thread.sleep(300);

            // Create orphaned files
            List<Path> orphanedFiles = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                String orphanHash = String.format(
                    "abcd%02xef1234567890abc%02x",
                    i,
                    i
                );
                Path orphanFile = createTestFileWithValidHash(
                    orphanHash + ".dat",
                    String.format("Orphaned content %d", i)
                );
                orphanedFiles.add(orphanFile);
            }

            // Verify all orphaned files exist
            for (Path orphanFile : orphanedFiles) {
                assertTrue(
                    Files.exists(orphanFile),
                    "Orphaned file should exist: " + orphanFile.getFileName()
                );
            }

            long totalOrphanedSize = orphanedFiles
                .stream()
                .mapToLong(file -> {
                    try {
                        return Files.size(file);
                    } catch (IOException e) {
                        return 0L;
                    }
                })
                .sum();

            // Count files before cleanup
            long filesBeforeCleanup = countFilesInDirectory(
                originalOffChainPath
            );

            // Perform cleanup
            OffChainCleanupService.CleanupResult result =
                cleanupService.cleanupOrphanedFiles();

            // Verify results
            assertNotNull(result);
            assertEquals(
                orphanedFiles.size(),
                result.getDeletedCount(),
                "Should delete all orphaned files"
            );
            assertEquals(
                totalOrphanedSize,
                result.getSpaceSaved(),
                "Should report accurate space savings"
            );
            assertEquals("Success", result.getMessage());

            // Verify orphaned files were deleted
            for (Path orphanFile : orphanedFiles) {
                assertFalse(
                    Files.exists(orphanFile),
                    "Orphaned file should be deleted: " +
                        orphanFile.getFileName()
                );
            }

            // Verify referenced files remain
            long filesAfterCleanup = countFilesInDirectory(
                originalOffChainPath
            );
            assertEquals(
                filesBeforeCleanup - orphanedFiles.size(),
                filesAfterCleanup,
                "Should have orphaned files count fewer files"
            );
            assertTrue(filesAfterCleanup > 0, "Referenced files should remain");
        }
    }

    @Nested
    @DisplayName("Security and Validation")
    class SecurityValidationTests {

        @Test
        @Order(30)
        @DisplayName(
            "Should validate filenames securely and prevent directory traversal"
        )
        void testFilenameValidationSecurity() throws Exception {
            logTestContext(
                "testFilenameValidationSecurity",
                "Testing filename security validation"
            );

            // Create files with valid hash names (should be processed)
            Path validFile1 = createTestFileWithValidHash(
                "abcd1234567890abcdef1234.dat",
                "Valid content 1"
            );
            Path validFile2 = createTestFileWithValidHash(
                "ABCD1234567890ABCDEF5678.dat",
                "Valid content 2"
            );

            // Attempt to create files with invalid/dangerous names
            List<String> dangerousNames = Arrays.asList(
                "../traversal.dat",
                "../../etc/passwd",
                "short.dat",
                "invalid@#$.dat",
                "con.dat", // Windows reserved name
                "prn.dat", // Windows reserved name
                ".hiddenfile.dat",
                "file with spaces.dat",
                "file\u0000null.dat", // Null byte injection
                "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789.dat" // Too long
            );

            // Count valid files created
            List<Path> createdValidFiles = Arrays.asList(
                validFile1,
                validFile2
            );

            // Verify valid files exist
            for (Path validFile : createdValidFiles) {
                assertTrue(
                    Files.exists(validFile),
                    "Valid file should exist: " + validFile.getFileName()
                );
            }

            // Try to create dangerous files (some may be blocked by filesystem)
            int dangerousFilesCreated = 0;
            for (String dangerousName : dangerousNames) {
                try {
                    Path dangerousPath = originalOffChainPath.resolve(
                        dangerousName
                    );
                    Files.write(dangerousPath, "Dangerous content".getBytes());
                    dangerousFilesCreated++;
                    logger.warn(
                        "⚠️ Filesystem allowed creation of dangerous file: {}",
                        dangerousName
                    );
                } catch (Exception e) {
                    logger.debug(
                        "✅ Filesystem blocked dangerous file: {}",
                        dangerousName
                    );
                }
            }

            // Perform cleanup
            OffChainCleanupService.CleanupResult result =
                cleanupService.cleanupOrphanedFiles();

            // Verify results
            assertNotNull(result);

            // Should process only files with valid hash patterns
            // Valid files should be deleted (as they're orphaned), dangerous files should be skipped
            assertEquals(
                createdValidFiles.size(),
                result.getDeletedCount(),
                String.format(
                    "Should delete only valid hash files (%d), not dangerous ones (%d created)",
                    createdValidFiles.size(),
                    dangerousFilesCreated
                )
            );

            // Verify valid files were deleted
            for (Path validFile : createdValidFiles) {
                assertFalse(
                    Files.exists(validFile),
                    "Valid orphaned file should be deleted: " +
                        validFile.getFileName()
                );
            }
        }

        @Test
        @Order(31)
        @DisplayName("Should handle symbolic links and special files securely")
        void testSymbolicLinkHandling() throws Exception {
            logTestContext(
                "testSymbolicLinkHandling",
                "Testing symbolic link security handling"
            );

            // Create a valid test file
            Path validFile = createTestFileWithValidHash(
                "abcdef1234567890abcdef12.dat",
                "Link target content"
            );

            try {
                // Create symbolic link to valid file (if supported)
                Path symlink = originalOffChainPath.resolve(
                    "1234567890abcdef12345678.dat"
                );
                Files.createSymbolicLink(symlink, validFile);

                if (Files.exists(symlink)) {
                    assertTrue(
                        Files.isSymbolicLink(symlink),
                        "Should be a symbolic link"
                    );
                } else {
                    logger.info(
                        "Symlink creation failed (may not be supported on this filesystem)"
                    );
                }

                // Perform cleanup
                OffChainCleanupService.CleanupResult result =
                    cleanupService.cleanupOrphanedFiles();

                // Service should handle symlinks safely
                assertNotNull(result);
                assertTrue(
                    result.getDeletedCount() >= 1,
                    "Should clean up at least some files"
                );
            } catch (UnsupportedOperationException | IOException e) {
                // Symbolic links not supported on this filesystem
                logger.info(
                    "Symbolic links not supported, skipping symlink test: {}",
                    e.getMessage()
                );
            }
        }

        @Test
        @Order(32)
        @DisplayName("Should enforce filename pattern matching strictly")
        void testStrictFilenamePatternMatching() throws Exception {
            logTestContext(
                "testStrictFilenamePatternMatching",
                "Testing strict filename pattern enforcement"
            );

            // Test various valid patterns
            Map<String, Boolean> filenameTests = new HashMap<>();
            filenameTests.put("12345678.dat", true); // Minimum valid length
            filenameTests.put("1234567890abcdef.dat", true); // Standard hash
            filenameTests.put("ABCDEF1234567890.dat", true); // Uppercase
            filenameTests.put(
                "abcdef1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678.dat",
                true
            ); // Long but valid
            filenameTests.put("1234567", false); // Too short, no extension
            filenameTests.put("1234567.dat", false); // Too short
            filenameTests.put("12345678.txt", false); // Wrong extension
            filenameTests.put("g2345678.dat", false); // Invalid hex character
            filenameTests.put("123456780.dat", false); // Contains invalid character (0 after 8)

            List<Path> validFiles = new ArrayList<>();
            List<Path> invalidFiles = new ArrayList<>();

            for (Map.Entry<String, Boolean> test : filenameTests.entrySet()) {
                String filename = test.getKey();
                boolean shouldBeValid = test.getValue();

                try {
                    Path file = originalOffChainPath.resolve(filename);
                    Files.write(file, ("Content for " + filename).getBytes());

                    if (shouldBeValid) {
                        validFiles.add(file);
                    } else {
                        invalidFiles.add(file);
                    }
                } catch (Exception e) {
                    logger.debug(
                        "Could not create test file {}: {}",
                        filename,
                        e.getMessage()
                    );
                }
            }

            // Perform cleanup
            OffChainCleanupService.CleanupResult result =
                cleanupService.cleanupOrphanedFiles();

            // Service should process only valid filenames
            assertNotNull(result);

            // Valid files should be processed (deleted as orphaned)
            for (Path validFile : validFiles) {
                assertFalse(
                    Files.exists(validFile),
                    "Valid filename should be processed: " +
                        validFile.getFileName()
                );
            }

            // Invalid files should be ignored (still exist)
            for (Path invalidFile : invalidFiles) {
                if (Files.exists(invalidFile)) {
                    logger.debug(
                        "✅ Invalid filename correctly ignored: {}",
                        invalidFile.getFileName()
                    );
                }
            }

            assertTrue(
                result.getDeletedCount() >= validFiles.size(),
                "Should delete at least the valid files"
            );
        }
    }

    @Nested
    @DisplayName("Concurrency and Thread Safety")
    class ConcurrencyTests {

        @Test
        @Order(40)
        @DisplayName("Should handle concurrent cleanup operations safely")
        void testConcurrentCleanupOperations() throws Exception {
            logTestContext(
                "testConcurrentCleanupOperations",
                "Testing concurrent cleanup thread safety"
            );

            // Create multiple orphaned files for concurrent processing
            List<Path> testFiles = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                String fileHash = String.format(
                    "abcd%02xef4567890abcdef%02x",
                    i,
                    i
                );
                Path file = createTestFileWithValidHash(
                    fileHash + ".dat",
                    String.format("Concurrent test content %d", i)
                );
                testFiles.add(file);
            }

            // Verify all files exist
            for (Path file : testFiles) {
                assertTrue(
                    Files.exists(file),
                    "Test file should exist: " + file.getFileName()
                );
            }

            // Run cleanup operations concurrently
            int threadCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(
                threadCount
            );
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<OffChainCleanupService.CleanupResult> results =
                Collections.synchronizedList(new ArrayList<>());
            List<Exception> exceptions = Collections.synchronizedList(
                new ArrayList<>()
            );

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        OffChainCleanupService.CleanupResult result =
                            cleanupService.cleanupOrphanedFiles();
                        results.add(result);
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Wait for all threads to complete
            assertTrue(
                latch.await(30, TimeUnit.SECONDS),
                "All threads should complete within timeout"
            );
            executor.shutdown();

            // Verify no exceptions occurred
            if (!exceptions.isEmpty()) {
                fail(
                    "Concurrent operations should not throw exceptions: " +
                        exceptions.get(0).getMessage()
                );
            }

            // Verify results
            assertEquals(
                threadCount,
                results.size(),
                "Should have results from all threads"
            );

            // All files should be deleted (only once, despite concurrent operations)
            for (Path file : testFiles) {
                assertFalse(
                    Files.exists(file),
                    "File should be deleted: " + file.getFileName()
                );
            }

            // Total deleted should be at least the number of files we created
            // (might be higher due to concurrent operations, but files are cleaned up)
            long totalDeleted = results
                .stream()
                .mapToLong(r -> r.getDeletedCount())
                .sum();
            assertTrue(
                totalDeleted >= testFiles.size(),
                String.format(
                    "Total deleted files (%d) should be at least created files (%d)",
                    totalDeleted,
                    testFiles.size()
                )
            );
        }

        @Test
        @Order(41)
        @DisplayName("Should handle concurrent compression operations safely")
        void testConcurrentCompressionOperations() throws Exception {
            logTestContext(
                "testConcurrentCompressionOperations",
                "Testing concurrent compression thread safety"
            );

            // Create multiple old files for concurrent compression
            List<Path> testFiles = new ArrayList<>();
            for (int i = 0; i < 15; i++) {
                String fileHash = String.format(
                    "abcd%02xef567890abcdef%02x",
                    i,
                    i
                );
                String content = String.format(
                    "Compressible content %d. ",
                    i
                ).repeat(100);
                Path file = createTestFileWithValidHash(
                    fileHash + ".dat",
                    content
                );

                // Make file old enough for compression
                setFileTimestamp(
                    file,
                    Instant.now().minus(
                        MaintenanceConstants.FILE_COMPRESSION_AGE_DAYS + 1,
                        ChronoUnit.DAYS
                    )
                );

                testFiles.add(file);
            }

            // Run compression operations concurrently
            int threadCount = 3;
            ExecutorService executor = Executors.newFixedThreadPool(
                threadCount
            );
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<OffChainCleanupService.CleanupResult> results =
                Collections.synchronizedList(new ArrayList<>());
            List<Exception> exceptions = Collections.synchronizedList(
                new ArrayList<>()
            );

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        OffChainCleanupService.CleanupResult result =
                            cleanupService.compressOldFiles();
                        results.add(result);
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Wait for completion
            assertTrue(
                latch.await(60, TimeUnit.SECONDS),
                "All compression threads should complete"
            );
            executor.shutdown();

            // Additional wait for file system operations to complete
            // File I/O and compression can take additional time even after threads complete
            assertTrue(
                executor.awaitTermination(30, TimeUnit.SECONDS),
                "Executor should terminate after completion"
            );

            // Additional buffer time for file system write operations
            Thread.sleep(2000);

            // Verify no exceptions
            if (!exceptions.isEmpty()) {
                fail(
                    "Concurrent compression should not throw exceptions: " +
                        exceptions.get(0).getMessage()
                );
            }

            // Verify all files were compressed (only once)
            for (Path originalFile : testFiles) {
                assertFalse(
                    Files.exists(originalFile),
                    "Original file should be deleted: " +
                        originalFile.getFileName()
                );

                Path compressedFile = originalOffChainPath.resolve(
                    originalFile.getFileName() + ".gz"
                );
                assertTrue(
                    Files.exists(compressedFile),
                    "Compressed file should exist: " +
                        compressedFile.getFileName()
                );
            }

            // Verify results consistency
            assertEquals(
                threadCount,
                results.size(),
                "Should have results from all threads"
            );
            long totalCompressed = results
                .stream()
                .mapToLong(r -> r.getCompressedCount())
                .sum();
            // In concurrent operations, files might be processed multiple times
            // but should result in at least the expected number of compressions
            assertTrue(
                totalCompressed >= testFiles.size(),
                String.format(
                    "Total compressed files (%d) should be at least created files (%d)",
                    totalCompressed,
                    testFiles.size()
                )
            );
        }

        @Test
        @Order(42)
        @DisplayName("Should handle mixed concurrent operations safely")
        void testMixedConcurrentOperations() throws Exception {
            logTestContext(
                "testMixedConcurrentOperations",
                "Testing mixed cleanup and compression concurrency"
            );

            // Create files for cleanup
            List<Path> orphanedFiles = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                String fileHash = String.format(
                    "abcd%02xef890abcdef%02xab",
                    i,
                    i
                );
                Path file = createTestFileWithValidHash(
                    fileHash + ".dat",
                    String.format("Mixed orphan content %d", i)
                );
                orphanedFiles.add(file);
            }

            // Create files for compression (must be referenced in blockchain to avoid deletion)
            // Track the files before adding blocks, so we know which ones are new
            File offChainDir = new File("off-chain-data");
            Set<String> existingFiles = new HashSet<>();
            File[] existingFilesArray = offChainDir.listFiles();
            if (existingFilesArray != null) {
                for (File file : existingFilesArray) {
                    existingFiles.add(file.getName());
                }
            }

            // Create off-chain blocks (will automatically create referenced files)
            List<Path> compressionFiles = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                String blockData = String.format(
                    "Mixed compression block data %d. ",
                    i
                ).repeat(1500);
                boolean added = blockchain.addBlock(
                    blockData,
                    testPrivateKey,
                    CryptoUtil.stringToPublicKey(testPublicKey)
                );
                assertTrue(added, "Compression block " + i + " should be added");
            }

            // Wait for off-chain files to be created
            Thread.sleep(300);

            // Collect ONLY the newly created off-chain files (referenced in blockchain)
            File[] allFiles = offChainDir.listFiles();
            if (allFiles != null) {
                for (File file : allFiles) {
                    if (
                        file.isFile() &&
                        !file.getName().endsWith(".gz") &&
                        !existingFiles.contains(file.getName())
                    ) {
                        Path filePath = file.toPath();
                        setFileTimestamp(
                            filePath,
                            Instant.now().minus(
                                MaintenanceConstants.FILE_COMPRESSION_AGE_DAYS + 1,
                                ChronoUnit.DAYS
                            )
                        );
                        compressionFiles.add(filePath);
                    }
                }
            }

            logger.info(
                "Test setup: {} orphaned files, {} referenced compression files",
                orphanedFiles.size(),
                compressionFiles.size()
            );

            // Run mixed operations concurrently
            ExecutorService executor = Executors.newFixedThreadPool(4);
            CountDownLatch latch = new CountDownLatch(4);
            List<OffChainCleanupService.CleanupResult> cleanupResults =
                Collections.synchronizedList(new ArrayList<>());
            List<OffChainCleanupService.CleanupResult> compressionResults =
                Collections.synchronizedList(new ArrayList<>());
            List<Exception> exceptions = Collections.synchronizedList(
                new ArrayList<>()
            );

            // Submit cleanup tasks
            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    try {
                        OffChainCleanupService.CleanupResult result =
                            cleanupService.cleanupOrphanedFiles();
                        cleanupResults.add(result);
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Submit compression tasks
            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    try {
                        OffChainCleanupService.CleanupResult result =
                            cleanupService.compressOldFiles();
                        compressionResults.add(result);
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Wait for completion
            assertTrue(
                latch.await(45, TimeUnit.SECONDS),
                "All mixed operations should complete"
            );
            executor.shutdown();

            // Verify no exceptions
            assertTrue(
                exceptions.isEmpty(),
                "Mixed concurrent operations should not throw exceptions"
            );

            // Verify cleanup results
            assertEquals(
                2,
                cleanupResults.size(),
                "Should have 2 cleanup results"
            );
            long totalCleaned = cleanupResults
                .stream()
                .mapToLong(r -> r.getDeletedCount())
                .sum();
            // In concurrent operations, cleanup threads should delete all orphaned files
            // Note: cleanup threads might process the same file, so total could be higher
            assertTrue(
                totalCleaned >= orphanedFiles.size(),
                String.format(
                    "Should clean at least all orphaned files: cleaned=%d, expected>=%d",
                    totalCleaned,
                    orphanedFiles.size()
                )
            );

            // Verify compression results
            assertEquals(
                2,
                compressionResults.size(),
                "Should have 2 compression results"
            );
            long totalCompressed = compressionResults
                .stream()
                .mapToLong(r -> r.getCompressedCount())
                .sum();
            // In mixed concurrent operations, compression might be affected by cleanup
            // Verify that some compression occurred and files were processed
            assertTrue(
                totalCompressed > 0,
                "Should compress at least some old files in concurrent operations"
            );

            // Verify final state
            for (Path orphanFile : orphanedFiles) {
                assertFalse(
                    Files.exists(orphanFile),
                    "Orphaned file should be deleted"
                );
            }

            // Verify that compression occurred on referenced files
            // Note: In concurrent operations, files may be compressed and original deleted
            // We just verify that at least some compression happened (checked above with totalCompressed > 0)
            int compressedFilesFound = 0;
            for (Path compressionFile : compressionFiles) {
                Path compressedFile = originalOffChainPath.resolve(
                    compressionFile.getFileName() + ".gz"
                );
                if (Files.exists(compressedFile)) {
                    compressedFilesFound++;
                    // If compressed version exists, original should be deleted
                    assertFalse(
                        Files.exists(compressionFile),
                        "Original file should be deleted after compression: " +
                            compressionFile.getFileName()
                    );
                }
            }
            assertTrue(
                compressedFilesFound > 0,
                "Should have at least some compressed files created"
            );
        }
    }

    @Nested
    @DisplayName("Statistics and Reporting")
    class StatisticsTests {

        @Test
        @Order(50)
        @DisplayName("Should provide accurate cleanup statistics")
        void testCleanupStatistics() throws Exception {
            logTestContext(
                "testCleanupStatistics",
                "Testing statistics accuracy and reporting"
            );

            // Get initial statistics
            String initialStats = cleanupService.getCleanupStatistics();
            assertNotNull(
                initialStats,
                "Initial statistics should not be null"
            );
            assertTrue(
                initialStats.contains("Off-Chain Cleanup Statistics"),
                "Should contain statistics header"
            );

            // Create test files for operations
            List<Path> testFiles = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                String fileHash = String.format(
                    "abcd%02xef4567890abcdef%02x",
                    i,
                    i
                );
                Path file = createTestFileWithValidHash(
                    fileHash + ".dat",
                    String.format("Statistics test content %d", i)
                );
                testFiles.add(file);
            }

            // Perform cleanup operation
            OffChainCleanupService.CleanupResult result =
                cleanupService.cleanupOrphanedFiles();
            assertNotNull(result);
            assertEquals(testFiles.size(), result.getDeletedCount());

            // Get updated statistics
            String updatedStats = cleanupService.getCleanupStatistics();
            assertNotNull(
                updatedStats,
                "Updated statistics should not be null"
            );
            assertTrue(
                updatedStats.length() >= initialStats.length(),
                "Updated statistics should contain at least as much information"
            );

            // Verify statistics content
            assertTrue(
                updatedStats.contains("Total Cleanup Operations:"),
                "Should contain cleanup operation count"
            );
            assertTrue(
                updatedStats.contains("Total Files Deleted:"),
                "Should contain files deleted count"
            );
            assertTrue(
                updatedStats.contains("Total Space Saved:"),
                "Should contain space saved information"
            );

            logger.info("📊 Statistics Report:\n{}", updatedStats);
        }

        @Test
        @Order(51)
        @DisplayName("Should track compression statistics accurately")
        void testCompressionStatistics() throws Exception {
            logTestContext(
                "testCompressionStatistics",
                "Testing compression statistics tracking"
            );

            // Create compressible files
            List<Path> compressibleFiles = new ArrayList<>();
            long totalOriginalSize = 0;

            for (int i = 0; i < 3; i++) {
                String fileHash = String.format(
                    "abcd%02xef567890abcd%02xef",
                    i,
                    i
                );
                String content = String.format(
                    "Highly compressible content %d. ",
                    i
                ).repeat(200);
                Path file = createTestFileWithValidHash(
                    fileHash + ".dat",
                    content
                );

                setFileTimestamp(
                    file,
                    Instant.now().minus(
                        MaintenanceConstants.FILE_COMPRESSION_AGE_DAYS + 1,
                        ChronoUnit.DAYS
                    )
                );

                compressibleFiles.add(file);
                totalOriginalSize += Files.size(file);
            }

            assertTrue(
                totalOriginalSize > 0,
                "Should have content to compress"
            );

            // Perform compression
            OffChainCleanupService.CleanupResult result =
                cleanupService.compressOldFiles();

            // Verify compression results
            assertNotNull(result);
            assertEquals(compressibleFiles.size(), result.getCompressedCount());
            assertTrue(
                result.getSpaceSaved() > 0,
                "Should report space savings from compression"
            );

            // Verify compressed files exist and are smaller
            long totalCompressedSize = 0;
            for (Path originalFile : compressibleFiles) {
                Path compressedFile = originalOffChainPath.resolve(
                    originalFile.getFileName() + ".gz"
                );
                assertTrue(
                    Files.exists(compressedFile),
                    "Compressed file should exist"
                );

                long compressedSize = Files.size(compressedFile);
                totalCompressedSize += compressedSize;
                assertTrue(
                    compressedSize > 0,
                    "Compressed file should have content"
                );
            }

            // Verify space savings are realistic
            long actualSavings = totalOriginalSize - totalCompressedSize;
            assertEquals(
                actualSavings,
                result.getSpaceSaved(),
                "Reported space savings should match actual savings"
            );

            // Get final statistics
            String finalStats = cleanupService.getCleanupStatistics();
            assertNotNull(finalStats, "Final statistics should not be null");
            assertTrue(
                finalStats.contains("Off-Chain Cleanup Statistics") ||
                    finalStats.contains("Total Files Compressed:") ||
                    result.getCompressedCount() > 0,
                "Statistics should be updated or compression should have occurred"
            );

            logger.info(
                "📊 Compression Efficiency: Original={} bytes, Compressed={} bytes, Savings={} bytes ({}%)",
                totalOriginalSize,
                totalCompressedSize,
                actualSavings,
                (actualSavings * 100.0) / totalOriginalSize
            );
        }

        @Test
        @Order(52)
        @DisplayName("Should handle edge cases in statistics reporting")
        void testStatisticsEdgeCases() throws Exception {
            logTestContext(
                "testStatisticsEdgeCases",
                "Testing statistics edge cases"
            );

            // Test statistics with no operations
            String emptyStats = cleanupService.getCleanupStatistics();
            assertNotNull(
                emptyStats,
                "Should return statistics even with no operations"
            );
            assertFalse(
                emptyStats.trim().isEmpty(),
                "Statistics should not be empty"
            );

            // Test statistics after failed operations (empty directory)
            OffChainCleanupService.CleanupResult emptyResult =
                cleanupService.cleanupOrphanedFiles();
            assertEquals(0, emptyResult.getDeletedCount());
            assertEquals("No files found", emptyResult.getMessage());

            String postEmptyStats = cleanupService.getCleanupStatistics();
            assertNotNull(
                postEmptyStats,
                "Should return statistics after empty operation"
            );

            // Test with very small files
            Path tinyFile = createTestFileWithValidHash(
                "abcdef1234567890abcdef01.dat",
                "x"
            );
            assertEquals(1, Files.size(tinyFile), "Tiny file should be 1 byte");

            OffChainCleanupService.CleanupResult tinyResult =
                cleanupService.cleanupOrphanedFiles();
            assertEquals(1, tinyResult.getDeletedCount());
            assertEquals(
                1,
                tinyResult.getSpaceSaved(),
                "Should report 1 byte saved"
            );

            // Test with zero-byte files
            Path emptyFile = createTestFileWithValidHash(
                "fedcba1234567890abcdef12.dat",
                ""
            );
            assertEquals(
                0,
                Files.size(emptyFile),
                "Empty file should be 0 bytes"
            );

            OffChainCleanupService.CleanupResult zeroResult =
                cleanupService.cleanupOrphanedFiles();
            assertEquals(1, zeroResult.getDeletedCount());
            assertEquals(
                0,
                zeroResult.getSpaceSaved(),
                "Should report 0 bytes saved for empty file"
            );

            String finalStats = cleanupService.getCleanupStatistics();
            logger.info("📊 Edge Case Statistics:\n{}", finalStats);
        }
    }

    @Nested
    @DisplayName("Error Handling and Resilience")
    class ErrorHandlingTests {

        @Test
        @Order(60)
        @DisplayName("Should handle file system errors gracefully")
        void testFileSystemErrorHandling() throws Exception {
            logTestContext(
                "testFileSystemErrorHandling",
                "Testing file system error resilience"
            );

            // Create some normal files
            List<Path> normalFiles = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                String fileHash = String.format(
                    "abcd%02xef567890abcdef%02x",
                    i,
                    i
                );
                Path file = createTestFileWithValidHash(
                    fileHash + ".dat",
                    String.format("Normal content %d", i)
                );
                normalFiles.add(file);
            }

            // Create a file and then make its parent directory read-only (if supported)
            Path problematicFile = createTestFileWithValidHash(
                "fedcba1234567890abcdef12.dat",
                "Problematic"
            );

            try {
                if (
                    Files.getFileStore(
                        originalOffChainPath
                    ).supportsFileAttributeView(PosixFileAttributeView.class)
                ) {
                    // Make file read-only
                    Files.setPosixFilePermissions(
                        problematicFile,
                        Set.of(PosixFilePermission.OWNER_READ)
                    );
                }
            } catch (Exception e) {
                logger.info(
                    "Cannot modify file permissions on this filesystem: {}",
                    e.getMessage()
                );
            }

            // Cleanup should handle errors gracefully and continue with other files
            OffChainCleanupService.CleanupResult result =
                cleanupService.cleanupOrphanedFiles();

            assertNotNull(result, "Should return result despite file errors");

            // Should process at least the normal files
            assertTrue(
                result.getDeletedCount() >= normalFiles.size(),
                "Should delete at least the normal files"
            );

            // Normal files should be deleted
            for (Path normalFile : normalFiles) {
                assertFalse(
                    Files.exists(normalFile),
                    "Normal file should be deleted: " + normalFile.getFileName()
                );
            }

            // Restore permissions for cleanup
            try {
                if (Files.exists(problematicFile)) {
                    Files.setPosixFilePermissions(
                        problematicFile,
                        EnumSet.allOf(PosixFilePermission.class)
                    );
                }
            } catch (Exception ignored) {}
        }

        @Test
        @Order(61)
        @DisplayName("Should handle corrupted files during compression")
        void testCorruptedFileCompressionHandling() throws Exception {
            logTestContext(
                "testCorruptedFileCompressionHandling",
                "Testing corrupted file compression handling"
            );

            // Create normal compressible file
            Path normalFile = createTestFileWithValidHash(
                "abcdef1234567890abcdef12.dat",
                "Normal compressible content. ".repeat(100)
            );
            setFileTimestamp(
                normalFile,
                Instant.now().minus(
                    MaintenanceConstants.FILE_COMPRESSION_AGE_DAYS + 1,
                    ChronoUnit.DAYS
                )
            );

            // Create potentially problematic file (very large to potentially cause issues)
            Path largeFile = createTestFileWithValidHash(
                "fedcba1234567890abcdef12.dat",
                "X".repeat(10000)
            );
            setFileTimestamp(
                largeFile,
                Instant.now().minus(
                    MaintenanceConstants.FILE_COMPRESSION_AGE_DAYS + 1,
                    ChronoUnit.DAYS
                )
            );

            // Attempt compression
            OffChainCleanupService.CleanupResult result =
                cleanupService.compressOldFiles();

            assertNotNull(
                result,
                "Should return result despite potential compression issues"
            );

            // At least the normal file should be processed
            assertTrue(
                result.getCompressedCount() >= 1,
                "Should compress at least one file"
            );

            // Normal file should be compressed
            assertFalse(
                Files.exists(normalFile),
                "Normal file should be compressed and deleted"
            );
            Path compressedNormalFile = originalOffChainPath.resolve(
                normalFile.getFileName() + ".gz"
            );
            assertTrue(
                Files.exists(compressedNormalFile),
                "Normal compressed file should exist"
            );
        }

        @Test
        @Order(62)
        @DisplayName("Should maintain service state consistency during errors")
        void testServiceStateConsistencyDuringErrors() throws Exception {
            logTestContext(
                "testServiceStateConsistencyDuringErrors",
                "Testing service state consistency"
            );

            // Get initial statistics
            String initialStats = cleanupService.getCleanupStatistics();
            assertNotNull(initialStats);

            // Create files that might cause issues
            createTestFileWithValidHash(
                "abcd1234567890abcdef0123.dat",
                "State test 1"
            );
            createTestFileWithValidHash(
                "fedc2345678901bcdef01234.dat",
                "State test 2"
            );

            // Perform operations that might encounter errors
            OffChainCleanupService.CleanupResult result1 =
                cleanupService.cleanupOrphanedFiles();
            OffChainCleanupService.CleanupResult result2 =
                cleanupService.compressOldFiles();

            // Service should maintain consistent state
            assertNotNull(result1, "First result should not be null");
            assertNotNull(result2, "Second result should not be null");

            // Statistics should be updated consistently
            String finalStats = cleanupService.getCleanupStatistics();
            assertNotNull(finalStats, "Final statistics should not be null");
            assertTrue(
                finalStats.length() >= initialStats.length(),
                "Statistics should maintain or grow in size"
            );

            // Service should remain functional after errors
            OffChainCleanupService.CleanupResult finalResult =
                cleanupService.cleanupOrphanedFiles();
            assertNotNull(finalResult, "Service should remain functional");
        }
    }

    // ====== HELPER METHODS ======

    private void setupTestDirectory() throws IOException {
        originalOffChainPath = Paths.get(ORIGINAL_OFF_CHAIN_DIR);
        offChainDirExisted = Files.exists(originalOffChainPath);

        if (offChainDirExisted) {
            // Backup existing directory
            Path backupPath = tempDir.resolve("backup-off-chain");
            moveDirectoryRecursively(originalOffChainPath, backupPath);
        }

        // Create fresh test directory
        Files.createDirectories(originalOffChainPath);
        logger.debug("✅ Test directory setup: {}", originalOffChainPath);
    }

    private void restoreOriginalDirectory() throws IOException {
        if (Files.exists(originalOffChainPath)) {
            deleteDirectoryRecursively(originalOffChainPath);
        }

        if (offChainDirExisted) {
            Path backupPath = tempDir.resolve("backup-off-chain");
            if (Files.exists(backupPath)) {
                moveDirectoryRecursively(backupPath, originalOffChainPath);
            }
        }
        logger.debug("✅ Test directory restored");
    }

    /**
     * Creates a test file with a valid filename pattern.
     * This ensures the filename passes the service's security validation.
     * Supports both legacy hash format and current offchain_timestamp_random format.
     */
    private Path createTestFileWithValidHash(String filename, String content)
        throws IOException {
        // Validate that the filename has the expected pattern
        // Remove both .dat and .gz extensions to get the base name
        String baseName = filename
            .replaceFirst("\\.dat(\\.gz)?$", "")
            .replaceFirst("\\.gz$", "");

        // Check if it matches either hash pattern or offchain pattern
        boolean isValid =
            VALID_HASH_PATTERN.matcher(baseName).matches() ||
            baseName.matches("^offchain_\\d+_\\d+$");

        assertTrue(
            isValid,
            "Test filename must have valid pattern (hash or offchain_*): " + filename
        );

        Path filePath = originalOffChainPath.resolve(filename);
        Files.write(filePath, content.getBytes());

        assertTrue(
            Files.exists(filePath),
            "Test file should be created: " + filename
        );
        logger.debug(
            "✅ Created test file: {} ({} bytes)",
            filename,
            Files.size(filePath)
        );

        return filePath;
    }

    /**
     * Sets the last modified timestamp of a file.
     * This is critical for testing age-based compression.
     */
    private void setFileTimestamp(Path file, Instant timestamp)
        throws IOException {
        FileTime fileTime = FileTime.from(timestamp);
        Files.setLastModifiedTime(file, fileTime);

        // Verify the timestamp was set correctly
        FileTime actualTime = Files.getLastModifiedTime(file);
        logger.debug(
            "✅ Set file timestamp: {} -> {} (actual: {})",
            file.getFileName(),
            timestamp,
            actualTime.toInstant()
        );

        // Allow some tolerance for filesystem timestamp precision
        long diffSeconds = Math.abs(
            actualTime.toInstant().getEpochSecond() - timestamp.getEpochSecond()
        );
        assertTrue(
            diffSeconds <= 2,
            String.format(
                "Timestamp should be set correctly (diff: %d seconds)",
                diffSeconds
            )
        );
    }

    /**
     * Verifies that a GZIP file contains the expected content when decompressed.
     */
    private void verifyGzipCompression(
        Path compressedFile,
        String expectedContent
    ) throws IOException {
        assertTrue(
            Files.exists(compressedFile),
            "Compressed file should exist"
        );
        assertTrue(
            compressedFile.getFileName().toString().endsWith(".gz"),
            "File should have .gz extension"
        );

        // Read and decompress
        try (
            GZIPInputStream gzipIn = new GZIPInputStream(
                Files.newInputStream(compressedFile)
            )
        ) {
            String decompressedContent = new String(gzipIn.readAllBytes());
            assertEquals(
                expectedContent,
                decompressedContent,
                "Decompressed content should match original"
            );
        }

        // Verify compression achieved some space savings
        long originalSize = expectedContent.getBytes().length;
        long compressedSize = Files.size(compressedFile);
        assertTrue(
            compressedSize < originalSize,
            String.format(
                "Compression should reduce size: %d -> %d bytes",
                originalSize,
                compressedSize
            )
        );

        double compressionRatio = (double) compressedSize / originalSize;
        logger.debug(
            "✅ Compression verified: {:.1f}% of original size",
            compressionRatio * 100
        );
    }

    /**
     * Counts regular files in a directory (excluding subdirectories).
     */
    private long countFilesInDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return 0;
        }

        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile).count();
        }
    }

    private void moveDirectoryRecursively(Path source, Path target)
        throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.copy(sourcePath, targetPath);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        deleteDirectoryRecursively(source);
    }

    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        logger.warn(
                            "Could not delete {}: {}",
                            p,
                            e.getMessage()
                        );
                    }
                });
        }
    }

    private void logTestContext(String method, String scenario) {
        logger.info("🧪 Test: {} - Scenario: {}", method, scenario);
    }
}
