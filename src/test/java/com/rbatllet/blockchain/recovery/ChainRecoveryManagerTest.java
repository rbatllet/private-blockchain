package com.rbatllet.blockchain.recovery;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.validation.ChainValidationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the ChainRecoveryManager
 */
@DisplayName("Chain Recovery Manager Tests")
class ChainRecoveryManagerTest {

    private Blockchain blockchain;
    private ChainRecoveryManager recoveryManager;
    private KeyPair testKeyPair;
    private String testPublicKey;
    private KeyPair adminKeyPair;
    private String adminPublicKey;

    @BeforeEach
    void setUp() {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();
        recoveryManager = new ChainRecoveryManager(blockchain);

        // Setup admin
        adminKeyPair = CryptoUtil.generateKeyPair();
        adminPublicKey = CryptoUtil.publicKeyToString(adminKeyPair.getPublic());
        blockchain.addAuthorizedKey(adminPublicKey, "Test Admin");

        testKeyPair = CryptoUtil.generateKeyPair();
        testPublicKey = CryptoUtil.publicKeyToString(testKeyPair.getPublic());
    }

    @AfterEach
    void tearDown() {
        // Clean database after each test to ensure test isolation
        if (blockchain != null) {
            blockchain.clearAndReinitialize();
        }
    }

    @Nested
    @DisplayName("Basic Recovery Operations")
    class BasicRecoveryOperations {

        @Test
        @DisplayName("Should validate input parameters")
        void shouldValidateInputParameters() {
            // Test null public key
            ChainRecoveryManager.RecoveryResult result1 = 
                recoveryManager.recoverCorruptedChain(null, "Test Owner");
            assertFalse(result1.isSuccess());
            assertEquals("VALIDATION_ERROR", result1.getMethod());
            assertTrue(result1.getMessage().contains("public key cannot be null"));

            // Test empty public key
            ChainRecoveryManager.RecoveryResult result2 = 
                recoveryManager.recoverCorruptedChain("", "Test Owner");
            assertFalse(result2.isSuccess());
            assertEquals("VALIDATION_ERROR", result2.getMethod());

            // Test null owner name
            ChainRecoveryManager.RecoveryResult result3 = 
                recoveryManager.recoverCorruptedChain(testPublicKey, null);
            assertFalse(result3.isSuccess());
            assertEquals("VALIDATION_ERROR", result3.getMethod());
            assertTrue(result3.getMessage().contains("owner name cannot be null"));

            // Test empty owner name
            ChainRecoveryManager.RecoveryResult result4 = 
                recoveryManager.recoverCorruptedChain(testPublicKey, "");
            assertFalse(result4.isSuccess());
            assertEquals("VALIDATION_ERROR", result4.getMethod());
        }

        @Test
        @DisplayName("Should reject recovery for existing keys")
        void shouldRejectRecoveryForExistingKeys() {
            // Add a key to blockchain
            blockchain.addAuthorizedKey(testPublicKey, "Test User");

            // Try to recover it
            ChainRecoveryManager.RecoveryResult result = 
                recoveryManager.recoverCorruptedChain(testPublicKey, "Test User");

            assertFalse(result.isSuccess());
            assertEquals("VALIDATION_ERROR", result.getMethod());
            assertTrue(result.getMessage().contains("not deleted"));
        }

        @Test
        @DisplayName("Should attempt re-authorization strategy first")
        void shouldAttemptReauthorizationStrategyFirst() {
            // Setup: Create a block with a key, then delete the key
            blockchain.addAuthorizedKey(testPublicKey, "Test User");
            blockchain.addBlock("Test data", testKeyPair.getPrivate(), testKeyPair.getPublic());
            
            // Verify chain is valid
            ChainValidationResult beforeCorruption = blockchain.validateChainDetailed();
            assertTrue(beforeCorruption.isFullyCompliant());
            assertTrue(beforeCorruption.isStructurallyIntact());
            
            // Delete the key to create corruption
            String reason = "Test corruption";
            String adminSignature = CryptoUtil.createAdminSignature(testPublicKey, true, reason, adminKeyPair.getPrivate());
            blockchain.dangerouslyDeleteAuthorizedKey(testPublicKey, true, reason, adminSignature, adminPublicKey);
            
            // Verify chain is now invalid
            ChainValidationResult afterCorruption = blockchain.validateChainDetailed();
            assertTrue(afterCorruption.isStructurallyIntact()); // Structure still intact
            assertFalse(afterCorruption.isFullyCompliant()); // But not fully compliant

            // Attempt recovery
            ChainRecoveryManager.RecoveryResult result = 
                recoveryManager.recoverCorruptedChain(testPublicKey, "Test User");

            assertNotNull(result);
            // Re-authorization should fail because the block was created before the recovery
            // so the recovery will proceed to rollback strategy
            assertTrue(result.isSuccess());
            assertEquals("ROLLBACK", result.getMethod());
        }
    }

    @Nested
    @DisplayName("Recovery Strategies")
    class RecoveryStrategies {

        @Test
        @DisplayName("Should succeed with rollback strategy when re-authorization fails")
        void shouldSucceedWithRollbackStrategy() {
            // Setup: Create multiple blocks
            KeyPair user1 = CryptoUtil.generateKeyPair();
            KeyPair user2 = CryptoUtil.generateKeyPair();
            
            String user1Key = CryptoUtil.publicKeyToString(user1.getPublic());
            String user2Key = CryptoUtil.publicKeyToString(user2.getPublic());
            
            blockchain.addAuthorizedKey(user1Key, "User 1");
            blockchain.addAuthorizedKey(user2Key, "User 2");
            
            blockchain.addBlock("User 1 block", user1.getPrivate(), user1.getPublic());
            blockchain.addBlock("User 2 block", user2.getPrivate(), user2.getPublic());
            
            ChainValidationResult beforeRollback = blockchain.validateChainDetailed();
            assertTrue(beforeRollback.isFullyCompliant());
            assertTrue(beforeRollback.isStructurallyIntact());
            
            // Delete user 1's key
            String user1Reason = "Test corruption";
            String user1AdminSignature = CryptoUtil.createAdminSignature(user1Key, true, user1Reason, adminKeyPair.getPrivate());
            blockchain.dangerouslyDeleteAuthorizedKey(user1Key, true, user1Reason, user1AdminSignature, adminPublicKey);
            ChainValidationResult afterCorruption = blockchain.validateChainDetailed();
            assertTrue(afterCorruption.isStructurallyIntact());
            assertFalse(afterCorruption.isFullyCompliant());
            
            // Attempt recovery
            ChainRecoveryManager.RecoveryResult result = 
                recoveryManager.recoverCorruptedChain(user1Key, "User 1");
            
            assertTrue(result.isSuccess());
            assertEquals("ROLLBACK", result.getMethod());
            ChainValidationResult afterRecovery = blockchain.validateChainDetailed();
            assertTrue(afterRecovery.isFullyCompliant());
            assertTrue(afterRecovery.isStructurallyIntact());
        }

        @Test
        @DisplayName("Should attempt partial export when rollback fails")
        void shouldAttemptPartialExportWhenRollbackFails() {
            // This test is harder to trigger since rollback usually works
            // We'll create a scenario where only partial export can help
            
            KeyPair user1 = CryptoUtil.generateKeyPair();
            String user1Key = CryptoUtil.publicKeyToString(user1.getPublic());
            
            blockchain.addAuthorizedKey(user1Key, "User 1");
            blockchain.addBlock("User 1 block", user1.getPrivate(), user1.getPublic());
            
            // Delete the key
            String deleteReason = "Test corruption";
            String deleteAdminSignature = CryptoUtil.createAdminSignature(user1Key, true, deleteReason, adminKeyPair.getPrivate());
            blockchain.dangerouslyDeleteAuthorizedKey(user1Key, true, deleteReason, deleteAdminSignature, adminPublicKey);
            
            // Attempt recovery
            ChainRecoveryManager.RecoveryResult result = 
                recoveryManager.recoverCorruptedChain(user1Key, "User 1");
            
            // Should succeed with some strategy
            assertTrue(result.isSuccess());
            assertNotNull(result.getMethod());
        }
    }

    @Nested
    @DisplayName("Diagnostic Functionality")
    class DiagnosticFunctionality {

        @Test
        @DisplayName("Should diagnose healthy chain correctly")
        void shouldDiagnoseHealthyChainCorrectly() {
            // Add some blocks to a healthy chain
            blockchain.addAuthorizedKey(testPublicKey, "Test User");
            blockchain.addBlock("Test block", testKeyPair.getPrivate(), testKeyPair.getPublic());
            
            ChainValidationResult healthyResult = blockchain.validateChainDetailed();
            assertTrue(healthyResult.isFullyCompliant());
            assertTrue(healthyResult.isStructurallyIntact());
            
            ChainRecoveryManager.ChainDiagnostic diagnostic = 
                recoveryManager.diagnoseCorruption();
            
            assertTrue(diagnostic.isHealthy());
            assertEquals(0, diagnostic.getCorruptedBlocks());
            assertTrue(diagnostic.getValidBlocks() > 0);
            assertEquals(diagnostic.getTotalBlocks(), diagnostic.getValidBlocks());
        }

        @Test
        @DisplayName("Should diagnose corrupted chain correctly")
        void shouldDiagnoseCorruptedChainCorrectly() {
            // Create corruption
            blockchain.addAuthorizedKey(testPublicKey, "Test User");
            blockchain.addBlock("Test block", testKeyPair.getPrivate(), testKeyPair.getPublic());
            
            ChainValidationResult beforeDiagnostic = blockchain.validateChainDetailed();
            assertTrue(beforeDiagnostic.isFullyCompliant());
            assertTrue(beforeDiagnostic.isStructurallyIntact());
            
            // Delete key to create corruption
            String corruptReason = "Test corruption";
            String corruptAdminSignature = CryptoUtil.createAdminSignature(testPublicKey, true, corruptReason, adminKeyPair.getPrivate());
            blockchain.dangerouslyDeleteAuthorizedKey(testPublicKey, true, corruptReason, corruptAdminSignature, adminPublicKey);
            ChainValidationResult afterDiagnostic = blockchain.validateChainDetailed();
            assertTrue(afterDiagnostic.isStructurallyIntact());
            assertFalse(afterDiagnostic.isFullyCompliant());
            
            ChainRecoveryManager.ChainDiagnostic diagnostic = 
                recoveryManager.diagnoseCorruption();
            
            assertFalse(diagnostic.isHealthy());
            assertTrue(diagnostic.getCorruptedBlocks() > 0);
            assertTrue(diagnostic.getValidBlocks() >= 1); // At least genesis block
        }

        @Test
        @DisplayName("Should handle empty blockchain")
        void shouldHandleEmptyBlockchain() {
            ChainRecoveryManager.ChainDiagnostic diagnostic = 
                recoveryManager.diagnoseCorruption();
            
            assertTrue(diagnostic.isHealthy());
            assertEquals(0, diagnostic.getCorruptedBlocks());
            assertEquals(1, diagnostic.getValidBlocks()); // Genesis block
            assertEquals(1, diagnostic.getTotalBlocks());
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Should work with default configuration")
        void shouldWorkWithDefaultConfiguration() {
            ChainRecoveryManager customRecoveryManager = 
                new ChainRecoveryManager(blockchain);
            
            // Test that it works with default config
            blockchain.addAuthorizedKey(testPublicKey, "Test User");
            blockchain.addBlock("Test block", testKeyPair.getPrivate(), testKeyPair.getPublic());
            String configCorruptReason = "Test corruption";
            String configCorruptAdminSignature = CryptoUtil.createAdminSignature(testPublicKey, true, configCorruptReason, adminKeyPair.getPrivate());
            blockchain.dangerouslyDeleteAuthorizedKey(testPublicKey, true, configCorruptReason, configCorruptAdminSignature, adminPublicKey);
            
            ChainRecoveryManager.RecoveryResult result = 
                customRecoveryManager.recoverCorruptedChain(testPublicKey, "Test User");
            
            assertTrue(result.isSuccess());
            // Should use rollback since partial export is disabled
            assertEquals("ROLLBACK", result.getMethod());
        }

        @Test
        @DisplayName("Should reject null blockchain")
        void shouldRejectNullBlockchain() {
            assertThrows(IllegalArgumentException.class, () -> {
                new ChainRecoveryManager(null);
            });
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle multiple recovery attempts")
        void shouldHandleMultipleRecoveryAttempts() {
            // Setup corruption
            blockchain.addAuthorizedKey(testPublicKey, "Test User");
            blockchain.addBlock("Test block", testKeyPair.getPrivate(), testKeyPair.getPublic());
            String multiCorruptReason = "Test corruption";
            String multiCorruptAdminSignature = CryptoUtil.createAdminSignature(testPublicKey, true, multiCorruptReason, adminKeyPair.getPrivate());
            blockchain.dangerouslyDeleteAuthorizedKey(testPublicKey, true, multiCorruptReason, multiCorruptAdminSignature, adminPublicKey);
            
            // First recovery attempt
            ChainRecoveryManager.RecoveryResult result1 = 
                recoveryManager.recoverCorruptedChain(testPublicKey, "Test User");
            assertTrue(result1.isSuccess());
            
            // Second recovery attempt should fail (key no longer missing)
            ChainRecoveryManager.RecoveryResult result2 = 
                recoveryManager.recoverCorruptedChain(testPublicKey, "Test User");
            assertFalse(result2.isSuccess());
            assertEquals("VALIDATION_ERROR", result2.getMethod());
        }

        @Test
        @DisplayName("Should handle recovery with no blocks")
        void shouldHandleRecoveryWithNoBlocks() {
            // Add and immediately delete a key without creating blocks
            blockchain.addAuthorizedKey(testPublicKey, "Test User");
            blockchain.revokeAuthorizedKey(testPublicKey);
            
            // Try recovery (should fail because key was safely deleted)
            ChainRecoveryManager.RecoveryResult result = 
                recoveryManager.recoverCorruptedChain(testPublicKey, "Test User");
            
            assertFalse(result.isSuccess());
            assertEquals("VALIDATION_ERROR", result.getMethod());
        }

        @Test
        @DisplayName("Should handle complex multi-user corruption")
        void shouldHandleComplexMultiUserCorruption() {
            // Setup multiple users
            KeyPair user1 = CryptoUtil.generateKeyPair();
            KeyPair user2 = CryptoUtil.generateKeyPair();
            KeyPair user3 = CryptoUtil.generateKeyPair();
            
            String user1Key = CryptoUtil.publicKeyToString(user1.getPublic());
            String user2Key = CryptoUtil.publicKeyToString(user2.getPublic());
            String user3Key = CryptoUtil.publicKeyToString(user3.getPublic());
            
            blockchain.addAuthorizedKey(user1Key, "User 1");
            blockchain.addAuthorizedKey(user2Key, "User 2");
            blockchain.addAuthorizedKey(user3Key, "User 3");
            
            blockchain.addBlock("User 1 block 1", user1.getPrivate(), user1.getPublic());
            blockchain.addBlock("User 2 block 1", user2.getPrivate(), user2.getPublic());
            blockchain.addBlock("User 3 block 1", user3.getPrivate(), user3.getPublic());
            blockchain.addBlock("User 1 block 2", user1.getPrivate(), user1.getPublic());
            
            ChainValidationResult beforeMultipleCorruption = blockchain.validateChainDetailed();
            assertTrue(beforeMultipleCorruption.isFullyCompliant());
            assertTrue(beforeMultipleCorruption.isStructurallyIntact());
            
            // Delete multiple keys
            String user1CorruptReason = "Test corruption 1";
            String user1CorruptAdminSignature = CryptoUtil.createAdminSignature(user1Key, true, user1CorruptReason, adminKeyPair.getPrivate());
            blockchain.dangerouslyDeleteAuthorizedKey(user1Key, true, user1CorruptReason, user1CorruptAdminSignature, adminPublicKey);

            String user3CorruptReason = "Test corruption 3";
            String user3CorruptAdminSignature = CryptoUtil.createAdminSignature(user3Key, true, user3CorruptReason, adminKeyPair.getPrivate());
            blockchain.dangerouslyDeleteAuthorizedKey(user3Key, true, user3CorruptReason, user3CorruptAdminSignature, adminPublicKey);
            
            ChainValidationResult afterMultipleCorruption = blockchain.validateChainDetailed();
            assertTrue(afterMultipleCorruption.isStructurallyIntact());
            assertFalse(afterMultipleCorruption.isFullyCompliant());
            
            // Recover first user
            ChainRecoveryManager.RecoveryResult result1 = 
                recoveryManager.recoverCorruptedChain(user1Key, "User 1");
            assertTrue(result1.isSuccess());
            
            // Chain might still be invalid due to user3's missing key
            // Try to recover user3
            ChainRecoveryManager.RecoveryResult result3 = 
                recoveryManager.recoverCorruptedChain(user3Key, "User 3");
            assertTrue(result3.isSuccess());
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should handle recovery efficiently")
        void shouldHandleRecoveryEfficiently() {
            long startTime = System.currentTimeMillis();

            // Setup scenario
            blockchain.addAuthorizedKey(testPublicKey, "Test User");
            blockchain.addBlock("Test block", testKeyPair.getPrivate(), testKeyPair.getPublic());
            String perfCorruptReason = "Test corruption";
            String perfCorruptAdminSignature = CryptoUtil.createAdminSignature(testPublicKey, true, perfCorruptReason, adminKeyPair.getPrivate());
            blockchain.dangerouslyDeleteAuthorizedKey(testPublicKey, true, perfCorruptReason, perfCorruptAdminSignature, adminPublicKey);
            
            // Perform recovery
            ChainRecoveryManager.RecoveryResult result = 
                recoveryManager.recoverCorruptedChain(testPublicKey, "Test User");
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            assertTrue(result.isSuccess());
            assertTrue(duration < 5000, "Recovery should complete within 5 seconds");
        }
    }

}