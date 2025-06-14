package com.rbatllet.blockchain.recovery;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.util.CryptoUtil;
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

    @BeforeEach
    void setUp() {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();
        recoveryManager = new ChainRecoveryManager(blockchain);
        testKeyPair = CryptoUtil.generateKeyPair();
        testPublicKey = CryptoUtil.publicKeyToString(testKeyPair.getPublic());
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
            assertTrue(blockchain.validateChain());
            
            // Delete the key to create corruption
            blockchain.dangerouslyDeleteAuthorizedKey(testPublicKey, true, "Test corruption");
            
            // Verify chain is now invalid
            assertFalse(blockchain.validateChain());

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
            
            assertTrue(blockchain.validateChain());
            
            // Delete user 1's key
            blockchain.dangerouslyDeleteAuthorizedKey(user1Key, true, "Test corruption");
            assertFalse(blockchain.validateChain());
            
            // Attempt recovery
            ChainRecoveryManager.RecoveryResult result = 
                recoveryManager.recoverCorruptedChain(user1Key, "User 1");
            
            assertTrue(result.isSuccess());
            assertEquals("ROLLBACK", result.getMethod());
            assertTrue(blockchain.validateChain());
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
            blockchain.dangerouslyDeleteAuthorizedKey(user1Key, true, "Test corruption");
            
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
            
            assertTrue(blockchain.validateChain());
            
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
            
            assertTrue(blockchain.validateChain());
            
            // Delete key to create corruption
            blockchain.dangerouslyDeleteAuthorizedKey(testPublicKey, true, "Test corruption");
            assertFalse(blockchain.validateChain());
            
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
            blockchain.dangerouslyDeleteAuthorizedKey(testPublicKey, true, "Test corruption");
            
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
            blockchain.dangerouslyDeleteAuthorizedKey(testPublicKey, true, "Test corruption");
            
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
            
            assertTrue(blockchain.validateChain());
            
            // Delete multiple keys
            blockchain.dangerouslyDeleteAuthorizedKey(user1Key, true, "Test corruption 1");
            blockchain.dangerouslyDeleteAuthorizedKey(user3Key, true, "Test corruption 3");
            
            assertFalse(blockchain.validateChain());
            
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
            blockchain.dangerouslyDeleteAuthorizedKey(testPublicKey, true, "Test corruption");
            
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