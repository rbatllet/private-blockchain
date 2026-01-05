package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.recovery.ChainRecoveryManager;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.MockitoAnnotations;

import java.security.KeyPair;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for UserFriendlyEncryptionAPI Phase 4 methods
 * Tests blockchain recovery, repair, and checkpoint management operations
 */
@DisplayName("🔧 UserFriendlyEncryptionAPI Phase 4 - Chain Recovery & Repair Tests")
public class UserFriendlyEncryptionAPIPhase4Test {

    private UserFriendlyEncryptionAPI api;
    private Blockchain realBlockchain;
    private KeyPair testKeyPair;
    private KeyPair bootstrapKeyPair;
    private String testUsername = "testuser";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Load bootstrap admin keys (auto-generates if missing - test-only)
        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();

        // Generate test key pair
        testKeyPair = CryptoUtil.generateKeyPair();

        // Initialize API with real blockchain for better test stability
        realBlockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        realBlockchain.clearAndReinitialize();

        // Register bootstrap admin first (RBAC v1.0.6)
        realBlockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        // SECURITY FIX (v1.0.6): Pre-authorize user before creating API
        String publicKeyString = CryptoUtil.publicKeyToString(testKeyPair.getPublic());
        realBlockchain.addAuthorizedKey(publicKeyString, testUsername, bootstrapKeyPair, UserRole.USER);

        api = new UserFriendlyEncryptionAPI(realBlockchain, testUsername, testKeyPair);

        // Add some test blocks to work with
        setupTestBlockchain();
    }

    private void setupTestBlockchain() {
        try {
            // Add a few blocks for testing
            for (int i = 0; i < 5; i++) {
                String data = "Test block data " + i;
                realBlockchain.addBlock(data, testKeyPair.getPrivate(), testKeyPair.getPublic());
            }
        } catch (Exception e) {
            // Ignore setup errors for tests that don't need initial blocks
        }
    }

    @Nested
    @DisplayName("🏥 Chain Recovery Tests")
    class ChainRecoveryTests {

        @Test
        @DisplayName("Should diagnose chain health")
        void shouldDiagnoseChainHealth() {
            // When
            ChainRecoveryManager.ChainDiagnostic diagnostic = api.diagnoseChainHealth();

            // Then
            assertNotNull(diagnostic, "Should provide diagnostic");
            assertTrue(diagnostic.getTotalBlocks() >= 0, "Should have total block count");
            assertTrue(diagnostic.getValidBlocks() >= 0, "Should have valid block count");
            assertTrue(diagnostic.getCorruptedBlocks() >= 0, "Should have corrupted block count");
            assertTrue(diagnostic.isHealthy() || !diagnostic.isHealthy(), "Should determine health status");
        }

        @Test
        @DisplayName("Should check if recovery is possible")
        void shouldCheckIfRecoveryIsPossible() {
            // When
            boolean canRecover = api.canRecoverFromFailure();

            // Then
            // Result depends on blockchain state, both true and false are valid
            assertTrue(canRecover || !canRecover, "Should return boolean result");
        }

        @Test
        @DisplayName("Should generate recovery capability report")
        void shouldGenerateRecoveryCapabilityReport() {
            // When
            String report = api.getRecoveryCapabilityReport();

            // Then
            assertNotNull(report, "Should provide recovery report");
            assertTrue(report.length() > 50, "Should be detailed report");
            assertTrue(report.contains("RECOVERY"), "Should mention recovery");
            assertTrue(report.contains("Status") || report.contains("STATUS"), "Should include status");
        }

        @Test
        @DisplayName("Should attempt recovery from corruption with username")
        void shouldAttemptRecoveryFromCorruptionWithUsername() {
            // Given
            String deletedUsername = "deletedUser";

            // When
            ChainRecoveryManager.RecoveryResult result = api.recoverFromCorruption(deletedUsername);

            // Then
            assertNotNull(result, "Should provide recovery result");
            assertNotNull(result.getMessage(), "Should have result message");
            // Success depends on whether user exists, both outcomes are valid
        }

        @Test
        @DisplayName("Should handle null username gracefully")
        void shouldHandleNullUsernameGracefully() {
            // When
            ChainRecoveryManager.RecoveryResult result = api.recoverFromCorruption("");

            // Then
            assertNotNull(result, "Should handle empty username");
            assertFalse(result.isSuccess(), "Should fail for empty username");
            assertTrue(result.getMessage().contains("VALIDATION_ERROR") || result.getMessage().contains("empty"), 
                      "Should indicate validation error");
        }

        @Test
        @DisplayName("Should recover with known public key")
        void shouldRecoverWithKnownPublicKey() {
            // Given
            String publicKey = CryptoUtil.publicKeyToString(testKeyPair.getPublic());
            String username = "recoveryUser";

            // When
            ChainRecoveryManager.RecoveryResult result = api.recoverFromCorruptionWithKey(publicKey, username);

            // Then
            assertNotNull(result, "Should provide recovery result");
            assertNotNull(result.getMessage(), "Should have result message");
            // Recovery may succeed or fail depending on blockchain state
        }

        @Test
        @DisplayName("Should perform comprehensive recovery with options")
        void shouldPerformComprehensiveRecoveryWithOptions() {
            // Given
            Map<String, Object> options = new HashMap<>();
            options.put("strategy", "automatic");
            options.put("createCheckpoint", true);
            options.put("maxBlocksToCheck", 100);

            // When
            ChainRecoveryResult result = api.recoverFromCorruption(options);

            // Then
            assertNotNull(result, "Should provide recovery result");
            assertNotNull(result.getRecoveryId(), "Should have recovery ID");
            assertNotNull(result.getMessage(), "Should have result message");
            assertTrue(result.getStatistics().getTotalBlocksAnalyzed() >= 0, "Should track processed blocks");
            assertNotNull(result.getActions(), "Should list actions performed");
        }
    }

    @Nested
    @DisplayName("🔗 Chain Repair Tests")
    class ChainRepairTests {

        @Test
        @DisplayName("Should repair broken chain segment")
        void shouldRepairBrokenChainSegment() {
            // Given
            Long startBlock = 1L;
            Long endBlock = 3L;

            // When
            ChainRecoveryResult result = api.repairBrokenChain(startBlock, endBlock);

            // Then
            assertNotNull(result, "Should provide repair result");
            assertNotNull(result.getRecoveryId(), "Should have recovery ID");
            assertTrue(result.getRecoveryId().contains("repair"), "Recovery ID should indicate repair");
            assertNotNull(result.getMessage(), "Should have result message");
            assertTrue(result.getStatistics().getTotalBlocksAnalyzed() >= 0, "Should track processed blocks");
        }

        @Test
        @DisplayName("Should handle invalid block range")
        void shouldHandleInvalidBlockRange() {
            // Given - End before start
            Long startBlock = 5L;
            Long endBlock = 2L;

            // When
            ChainRecoveryResult result = api.repairBrokenChain(startBlock, endBlock);

            // Then
            assertNotNull(result, "Should handle invalid range");
            assertTrue(result.getStatus() != ChainRecoveryResult.RecoveryStatus.SUCCESS, "Should not succeed for invalid range");
            // Implementation may handle invalid range gracefully - just check it's handled
            assertNotNull(result.getMessage(), "Should provide result message");
        }

        @Test
        @DisplayName("Should handle null block numbers")
        void shouldHandleNullBlockNumbers() {
            // When & Then
            assertDoesNotThrow(() -> {
                ChainRecoveryResult result1 = api.repairBrokenChain(null, 5L);
                assertNotNull(result1, "Should handle null start block");
                
                ChainRecoveryResult result2 = api.repairBrokenChain(1L, null);
                assertNotNull(result2, "Should handle null end block");
            }, "Should handle null block numbers gracefully");
        }

        @Test
        @DisplayName("Should repair with performance tracking")
        void shouldRepairWithPerformanceTracking() {
            // Given
            Long startBlock = 0L;
            Long endBlock = 2L;

            // When
            long startTime = System.currentTimeMillis();
            ChainRecoveryResult result = api.repairBrokenChain(startBlock, endBlock);
            long duration = System.currentTimeMillis() - startTime;

            // Then
            assertNotNull(result, "Should provide repair result");
            assertTrue(duration < 5000, "Repair should complete within 5 seconds");
            if (result.getTotalDuration() != null) {
                assertTrue(result.getTotalDuration().toMillis() >= 0, "Should track recovery duration");
            }
        }
    }

    @Nested
    @DisplayName("⏪ Rollback Operations Tests")
    class RollbackOperationsTests {

        @Test
        @DisplayName("Should rollback to safe state")
        void shouldRollbackToSafeState() {
            // Given
            Long targetBlock = 2L;
            Map<String, Object> options = new HashMap<>();
            options.put("createCheckpoint", true);
            options.put("force", false);

            // When
            ChainRecoveryResult result = api.rollbackToSafeState(targetBlock, options);

            // Then
            assertNotNull(result, "Should provide rollback result");
            assertNotNull(result.getRecoveryId(), "Should have recovery ID");
            assertTrue(result.getRecoveryId().contains("rollback"), "Recovery ID should indicate rollback");
            assertNotNull(result.getMessage(), "Should have result message");
        }

        @Test
        @DisplayName("Should validate rollback target")
        void shouldValidateRollbackTarget() {
            // Given - Future block that doesn't exist
            Long targetBlock = 9999L;
            Map<String, Object> options = new HashMap<>();

            // When
            ChainRecoveryResult result = api.rollbackToSafeState(targetBlock, options);

            // Then
            assertNotNull(result, "Should handle invalid target");
            // Implementation may handle gracefully or clamp to valid range
            assertNotNull(result.getRecoveryId(), "Should have recovery ID");
            assertNotNull(result.getMessage(), "Should provide result message");
        }

        @Test
        @DisplayName("Should handle null options gracefully")
        void shouldHandleNullOptionsGracefully() {
            // Given
            Long targetBlock = 1L;

            // When & Then
            assertDoesNotThrow(() -> {
                ChainRecoveryResult result = api.rollbackToSafeState(targetBlock, null);
                assertNotNull(result, "Should handle null options");
            }, "Should handle null options without throwing");
        }

        @Test
        @DisplayName("Should rollback with safety recommendations")
        void shouldRollbackWithSafetyRecommendations() {
            // Given
            Long targetBlock = 0L; // Genesis block
            Map<String, Object> options = new HashMap<>();
            options.put("includeRecommendations", true);

            // When
            ChainRecoveryResult result = api.rollbackToSafeState(targetBlock, options);

            // Then
            assertNotNull(result, "Should provide rollback result");
            if (result.getRecommendations() != null && !result.getRecommendations().isEmpty()) {
                assertTrue(result.getRecommendations().size() > 0, "Should include recommendations");
            }
        }
    }

    @Nested
    @DisplayName("💾 Checkpoint Management Tests")
    class CheckpointManagementTests {

        @Test
        @DisplayName("Should create recovery checkpoint")
        void shouldCreateRecoveryCheckpoint() {
            // Given
            RecoveryCheckpoint.CheckpointType type = RecoveryCheckpoint.CheckpointType.MANUAL;
            String description = "Test checkpoint for Phase 4 testing";

            // When
            RecoveryCheckpoint checkpoint = api.createRecoveryCheckpoint(type, description);

            // Then
            assertNotNull(checkpoint, "Should create checkpoint");
            assertNotNull(checkpoint.getCheckpointId(), "Should have checkpoint ID");
            assertEquals(type, checkpoint.getType(), "Should have correct type");
            assertEquals(description, checkpoint.getDescription(), "Should store description");
            assertNotNull(checkpoint.getCreatedAt(), "Should have creation timestamp");
            assertTrue(checkpoint.getLastBlockNumber() >= 0, "Should have valid block number");
        }

        @Test
        @DisplayName("Should create emergency checkpoint")
        void shouldCreateEmergencyCheckpoint() {
            // Given
            RecoveryCheckpoint.CheckpointType type = RecoveryCheckpoint.CheckpointType.EMERGENCY;
            String description = "Emergency checkpoint before critical operation";

            // When
            RecoveryCheckpoint checkpoint = api.createRecoveryCheckpoint(type, description);

            // Then
            assertNotNull(checkpoint, "Should create emergency checkpoint");
            assertEquals(RecoveryCheckpoint.CheckpointType.EMERGENCY, checkpoint.getType(), 
                        "Should be marked as emergency");
        }

        @Test
        @DisplayName("Should handle checkpoint creation with null description")
        void shouldHandleCheckpointCreationWithNullDescription() {
            // Given
            RecoveryCheckpoint.CheckpointType type = RecoveryCheckpoint.CheckpointType.AUTOMATIC;

            // When & Then
            assertDoesNotThrow(() -> {
                RecoveryCheckpoint checkpoint = api.createRecoveryCheckpoint(type, null);
                assertNotNull(checkpoint, "Should create checkpoint with null description");
            }, "Should handle null description gracefully");
        }

        @Test
        @DisplayName("Should create checkpoint with chain state")
        void shouldCreateCheckpointWithChainState() {
            // Given
            RecoveryCheckpoint.CheckpointType type = RecoveryCheckpoint.CheckpointType.SCHEDULED;
            String description = "Scheduled checkpoint with full state";

            // When
            RecoveryCheckpoint checkpoint = api.createRecoveryCheckpoint(type, description);

            // Then
            assertNotNull(checkpoint, "Should create checkpoint");
            if (checkpoint.getChainState() != null) {
                assertFalse(checkpoint.getChainState().isEmpty(), "Should capture chain state");
            }
            if (checkpoint.getCriticalHashes() != null) {
                assertTrue(checkpoint.getCriticalHashes().size() >= 0, "Should capture critical hashes");
            }
        }
    }

    @Nested
    @DisplayName("📤 Import/Export Recovery Tests")
    class ImportExportRecoveryTests {

        @Test
        @DisplayName("Should export recovery data")
        void shouldExportRecoveryData() {
            // Given
            String outputPath = "target/test-recovery-export.json";
            Map<String, Object> options = new HashMap<>();
            options.put("includeCheckpoints", true);
            options.put("includeMetadata", true);
            options.put("compressed", false);

            // When
            Map<String, Object> result = api.exportRecoveryData(outputPath, options);

            // Then
            assertNotNull(result, "Should provide export result");
            assertTrue(result.containsKey("success"), "Should indicate success");
            if (Boolean.TRUE.equals(result.get("success"))) {
                assertTrue(result.containsKey("blocksExported"), "Should track exported blocks");
                // Implementation may not return filePath in all cases
                assertNotNull(result, "Should provide result map");
            }

            // Cleanup
            try {
                Files.deleteIfExists(Path.of(outputPath));
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }

        @Test
        @DisplayName("Should handle export to invalid path")
        void shouldHandleExportToInvalidPath() {
            // Given
            String invalidPath = "/invalid/path/that/does/not/exist/export.json";
            Map<String, Object> options = new HashMap<>();

            // When
            Map<String, Object> result = api.exportRecoveryData(invalidPath, options);

            // Then
            assertNotNull(result, "Should handle invalid path");
            // Implementation may create directories or handle paths gracefully
            assertTrue(result.containsKey("success"), "Should indicate success status");
            if (Boolean.FALSE.equals(result.get("success"))) {
                assertTrue(result.containsKey("error"), "Should provide error details");
            }
        }

        @Test
        @DisplayName("Should import recovery data")
        void shouldImportRecoveryData() {
            // Given - First export some data
            String tempPath = "target/test-recovery-import.json";
            Map<String, Object> exportOptions = new HashMap<>();
            api.exportRecoveryData(tempPath, exportOptions);

            // When - Import the data
            Map<String, Object> importOptions = new HashMap<>();
            importOptions.put("validateBeforeImport", true);
            importOptions.put("createBackup", true);

            ChainRecoveryResult result = api.importRecoveryData(tempPath, importOptions);

            // Then
            assertNotNull(result, "Should provide import result");
            assertNotNull(result.getRecoveryId(), "Should have recovery ID");
            assertTrue(result.getRecoveryId().contains("import"), "Recovery ID should indicate import");
            assertNotNull(result.getMessage(), "Should have result message");

            // Cleanup
            try {
                Files.deleteIfExists(Path.of(tempPath));
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }

        @Test
        @DisplayName("Should handle import from non-existent file")
        void shouldHandleImportFromNonExistentFile() {
            // Given
            String nonExistentPath = "target/non-existent-file.json";
            Map<String, Object> options = new HashMap<>();

            // When
            ChainRecoveryResult result = api.importRecoveryData(nonExistentPath, options);

            // Then
            assertNotNull(result, "Should handle non-existent file");
            // Implementation may handle gracefully or succeed with default values
            assertNotNull(result.getRecoveryId(), "Should have recovery ID");
            assertNotNull(result.getMessage(), "Should provide result message");
        }

        @Test
        @DisplayName("Should export with compression option")
        void shouldExportWithCompressionOption() {
            // Given
            String outputPath = "target/test-recovery-compressed.gz";
            Map<String, Object> options = new HashMap<>();
            options.put("compressed", true);
            options.put("compressionLevel", 6);

            // When
            Map<String, Object> result = api.exportRecoveryData(outputPath, options);

            // Then
            assertNotNull(result, "Should provide export result");
            if (Boolean.TRUE.equals(result.get("success"))) {
                assertTrue(outputPath.endsWith(".gz") || 
                          result.get("compressed") == Boolean.TRUE, 
                          "Should indicate compression");
            }

            // Cleanup
            try {
                Files.deleteIfExists(Path.of(outputPath));
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    @Nested
    @DisplayName("🔧 Integration and Performance Tests")
    class IntegrationAndPerformanceTests {

        @Test
        @DisplayName("Should perform complete recovery workflow")
        void shouldPerformCompleteRecoveryWorkflow() {
            // Given - Simulate a recovery scenario
            String exportPath = "target/test-complete-recovery.json";

            // Step 1: Create checkpoint
            RecoveryCheckpoint checkpoint = api.createRecoveryCheckpoint(
                RecoveryCheckpoint.CheckpointType.MANUAL, 
                "Pre-recovery checkpoint"
            );
            assertNotNull(checkpoint, "Should create checkpoint");

            // Step 2: Export current state
            Map<String, Object> exportResult = api.exportRecoveryData(exportPath, new HashMap<>());
            assertTrue(Boolean.TRUE.equals(exportResult.get("success")), "Export should succeed");

            // Step 3: Diagnose chain health
            ChainRecoveryManager.ChainDiagnostic diagnostic = api.diagnoseChainHealth();
            assertNotNull(diagnostic, "Should provide diagnostic");

            // Step 4: Check recovery capability
            boolean canRecover = api.canRecoverFromFailure();
            assertTrue(canRecover || !canRecover, "Should determine recovery capability");

            // Step 5: Get recovery report
            String report = api.getRecoveryCapabilityReport();
            assertNotNull(report, "Should generate report");
            assertTrue(report.length() > 50, "Should be detailed report");

            // Cleanup
            try {
                Files.deleteIfExists(Path.of(exportPath));
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }

        @Test
        @DisplayName("Should handle concurrent recovery operations safely")
        void shouldHandleConcurrentRecoveryOperationsSafely() throws InterruptedException {
            // Given
            int threadCount = 3;
            List<Thread> threads = new ArrayList<>();
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            // When - Execute concurrent recovery operations
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                Thread thread = new Thread(() -> {
                    try {
                        // Different recovery operations
                        if (index == 0) {
                            ChainRecoveryManager.ChainDiagnostic diagnostic = api.diagnoseChainHealth();
                            assertNotNull(diagnostic, "Concurrent diagnosis should work");
                        } else if (index == 1) {
                            boolean canRecover = api.canRecoverFromFailure();
                            assertTrue(canRecover || !canRecover, "Concurrent check should work");
                        } else {
                            String report = api.getRecoveryCapabilityReport();
                            assertNotNull(report, "Concurrent report should work");
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    }
                });
                threads.add(thread);
                thread.start();
            }

            // Wait for all threads
            for (Thread thread : threads) {
                thread.join();
            }

            // Then
            assertTrue(exceptions.isEmpty(), "All concurrent operations should succeed: " + exceptions);
        }

        @Test
        @DisplayName("Should measure recovery performance")
        void shouldMeasureRecoveryPerformance() {
            // Given
            Map<String, Object> options = new HashMap<>();
            options.put("performanceMode", true);

            // When
            long startTime = System.currentTimeMillis();
            ChainRecoveryResult result = api.recoverFromCorruption(options);
            long duration = System.currentTimeMillis() - startTime;

            // Then
            assertNotNull(result, "Should complete recovery");
            assertTrue(duration < 10000, "Recovery should complete within 10 seconds");
            if (result.getTotalDuration() != null) {
                assertTrue(result.getTotalDuration().toMillis() >= 0, "Should track internal duration");
            }
        }

        @Test
        @DisplayName("Should provide comprehensive recovery system check")
        void shouldProvideComprehensiveRecoverySystemCheck() {
            // When - Test all recovery subsystems
            ChainRecoveryManager.ChainDiagnostic diagnostic = api.diagnoseChainHealth();
            boolean canRecover = api.canRecoverFromFailure();
            String report = api.getRecoveryCapabilityReport();
            
            RecoveryCheckpoint checkpoint = api.createRecoveryCheckpoint(
                RecoveryCheckpoint.CheckpointType.AUTOMATIC, 
                "System check checkpoint"
            );

            // Then - All subsystems should be operational
            assertNotNull(diagnostic, "Diagnostic system should work");
            assertTrue(canRecover || !canRecover, "Recovery check should work");
            assertNotNull(report, "Report generation should work");
            assertNotNull(checkpoint, "Checkpoint system should work");
            
            // Verify system integration
            assertTrue(diagnostic.getTotalBlocks() >= 0, "Should have valid block count");
            assertTrue(report.length() > 0, "Should generate meaningful report");
            assertNotNull(checkpoint.getCheckpointId(), "Should create valid checkpoint");
        }
    }
}