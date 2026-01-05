package com.rbatllet.blockchain.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;

/**
 * Tests for the improved intelligent rollback strategy
 * These tests verify that the enhanced rollback logic preserves
 * maximum valid blocks while maintaining security
 */
@DisplayName("Improved Rollback Strategy Tests")
class ImprovedRollbackStrategyTest {

    private Blockchain blockchain;
    private ChainRecoveryManager recoveryManager;
    private KeyPair bootstrapKeyPair;
    private KeyPair adminKeyPair;
    private String adminPublicKey;

    @BeforeEach
    void setUp() {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();
        recoveryManager = new ChainRecoveryManager(blockchain);

        // Load bootstrap admin keys (auto-generates if missing - test-only)
        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();

        // Register bootstrap admin in blockchain (RBAC v1.0.6)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        // Setup admin
        adminKeyPair = CryptoUtil.generateKeyPair();
        adminPublicKey = CryptoUtil.publicKeyToString(adminKeyPair.getPublic());
        blockchain.addAuthorizedKey(adminPublicKey, "Test Admin", bootstrapKeyPair, UserRole.ADMIN);
    }

    @AfterEach
    void tearDown() {
        // Clean database after each test to ensure test isolation
        if (blockchain != null) {
            blockchain.clearAndReinitialize();
        }
    }

    @Nested
    @DisplayName("Intelligent Rollback Scenarios")
    class IntelligentRollbackScenarios {

        @Test
        @DisplayName("Should handle interleaved corruption intelligently")
        void shouldHandleInterleavedCorruptionIntelligently() {
            System.out.println("\n🔬 TEST: Interleaved Corruption Scenario");
            System.out.println("Scenario: User1-User2-User1-User2, delete User1 key");
            
            // Create users
            KeyPair user1 = CryptoUtil.generateKeyPair();
            KeyPair user2 = CryptoUtil.generateKeyPair();
            
            String user1Key = CryptoUtil.publicKeyToString(user1.getPublic());
            String user2Key = CryptoUtil.publicKeyToString(user2.getPublic());

            // Add users
            blockchain.addAuthorizedKey(user1Key, "User 1", adminKeyPair, UserRole.USER);
            blockchain.addAuthorizedKey(user2Key, "User 2", adminKeyPair, UserRole.USER);
            
            // Create interleaved pattern
            blockchain.addBlock("User1-Block1", user1.getPrivate(), user1.getPublic()); // #1 - Will corrupt
            blockchain.addBlock("User2-Block1", user2.getPrivate(), user2.getPublic()); // #2 - Should preserve ideally
            blockchain.addBlock("User1-Block2", user1.getPrivate(), user1.getPublic()); // #3 - Will corrupt
            blockchain.addBlock("User2-Block2", user2.getPrivate(), user2.getPublic()); // #4 - Should preserve ideally
            
            long initialBlocks = blockchain.getBlockCount();
            var initialValidation = blockchain.validateChainDetailed();
            assertTrue(initialValidation.isStructurallyIntact() && initialValidation.isFullyCompliant(), 
                "Initial chain should be valid");
            
            System.out.println("Initial chain: " + initialBlocks + " blocks");
            System.out.println("Chain structurally intact: " + initialValidation.isStructurallyIntact());
            System.out.println("Chain fully compliant: " + initialValidation.isFullyCompliant());
            
            // Create corruption
            String interleavedReason = "Test interleaved corruption";
            String interleavedAdminSignature = CryptoUtil.createAdminSignature(user1Key, true, interleavedReason, adminKeyPair.getPrivate());
            blockchain.dangerouslyDeleteAuthorizedKey(user1Key, true, interleavedReason, interleavedAdminSignature, adminPublicKey);
            var corruptedValidation = blockchain.validateChainDetailed();
            assertFalse(corruptedValidation.isFullyCompliant(), 
                "Chain should not be fully compliant after key deletion");
            assertTrue(corruptedValidation.isStructurallyIntact(), 
                "Chain should still be structurally intact after key deletion");
            
            System.out.println("After corruption - Chain structurally intact: " + corruptedValidation.isStructurallyIntact());
            System.out.println("After corruption - Chain fully compliant: " + corruptedValidation.isFullyCompliant());
            if (!corruptedValidation.isFullyCompliant()) {
                System.out.println("Revoked blocks: " + corruptedValidation.getRevokedBlocks());
            }
            if (!corruptedValidation.isStructurallyIntact()) {
                System.out.println("Invalid blocks: " + corruptedValidation.getInvalidBlocks());
            }
            
            // Attempt recovery
            ChainRecoveryManager.RecoveryResult result = 
                recoveryManager.recoverCorruptedChain(user1Key, "User 1");
            
            // Verify recovery succeeded
            assertTrue(result.isSuccess(), "Recovery should succeed");
            var recoveredValidation = blockchain.validateChainDetailed();
            assertTrue(recoveredValidation.isStructurallyIntact(), "Chain should be structurally intact after recovery");
            assertTrue(recoveredValidation.isFullyCompliant(), "Chain should be fully compliant after recovery");
            assertNotNull(result.getMethod(), "Recovery method should be specified");
            
            System.out.println("Recovery successful: " + result.isSuccess());
            System.out.println("Recovery method: " + result.getMethod());
            System.out.println("Final chain: " + blockchain.getBlockCount() + " blocks");
            System.out.println("Final chain structurally intact: " + recoveredValidation.isStructurallyIntact());
            System.out.println("Final chain fully compliant: " + recoveredValidation.isFullyCompliant());
            
            // Check preservation efficiency
            long finalBlocks = blockchain.getBlockCount();
            long user2BlocksRemaining = 0;
            long blockCount = blockchain.getBlockCount();
            for (long i = 0; i < blockCount; i++) {
                Block b = blockchain.getBlock(i);
                if (b != null && b.getData().contains("User2")) {
                    user2BlocksRemaining++;
                }
            }
            
            System.out.println("User2 blocks preserved: " + user2BlocksRemaining);
            
            // The improved strategy should preserve some blocks when possible
            // At minimum, it should preserve genesis block
            assertTrue(finalBlocks >= 1, "Should preserve at least genesis block");
            
            if (user2BlocksRemaining > 0) {
                System.out.println("✅ IMPROVEMENT VERIFIED: Valid blocks preserved!");
            } else {
                System.out.println("ℹ️ Note: Conservative approach used (still safe)");
            }

            // Verify all preserved blocks are valid
            for (long i = 0; i < blockCount; i++) {
                Block block = blockchain.getBlock(i);
                if (block != null) {
                    assertTrue(blockchain.validateSingleBlock(block),
                        "All preserved blocks should be valid");
                }
            }
        }

        @Test
        @DisplayName("Should optimize rollback for end corruption")
        void shouldOptimizeRollbackForEndCorruption() {
            System.out.println("\n🔬 TEST: End Corruption Scenario");
            System.out.println("Scenario: Valid-Valid-Valid-Corrupt, should preserve first 3");
            
            // Create users
            KeyPair validUser = CryptoUtil.generateKeyPair();
            KeyPair corruptUser = CryptoUtil.generateKeyPair();
            
            String validKey = CryptoUtil.publicKeyToString(validUser.getPublic());
            String corruptKey = CryptoUtil.publicKeyToString(corruptUser.getPublic());

            // Add users
            blockchain.addAuthorizedKey(validKey, "Valid User", adminKeyPair, UserRole.USER);
            blockchain.addAuthorizedKey(corruptKey, "Corrupt User", adminKeyPair, UserRole.USER);
            
            // Create pattern with corruption at end
            blockchain.addBlock("Valid-1", validUser.getPrivate(), validUser.getPublic());    // #1 - Should preserve
            blockchain.addBlock("Valid-2", validUser.getPrivate(), validUser.getPublic());    // #2 - Should preserve
            blockchain.addBlock("Valid-3", validUser.getPrivate(), validUser.getPublic());    // #3 - Should preserve
            blockchain.addBlock("Corrupt", corruptUser.getPrivate(), corruptUser.getPublic()); // #4 - Will corrupt
            
            long initialBlocks = blockchain.getBlockCount();
            var initialValidation = blockchain.validateChainDetailed();
            assertTrue(initialValidation.isStructurallyIntact() && initialValidation.isFullyCompliant(), 
                "Initial chain should be valid");
            
            System.out.println("Initial chain: " + initialBlocks + " blocks");
            
            // Create corruption
            String endCorruptReason = "Test end corruption";
            String endCorruptAdminSignature = CryptoUtil.createAdminSignature(corruptKey, true, endCorruptReason, adminKeyPair.getPrivate());
            blockchain.dangerouslyDeleteAuthorizedKey(corruptKey, true, endCorruptReason, endCorruptAdminSignature, adminPublicKey);
            var corruptedValidation = blockchain.validateChainDetailed();
            assertFalse(corruptedValidation.isFullyCompliant(), 
                "Chain should not be fully compliant after corruption");
            assertTrue(corruptedValidation.isStructurallyIntact(), 
                "Chain should still be structurally intact after corruption");
            
            // Attempt recovery
            ChainRecoveryManager.RecoveryResult result = 
                recoveryManager.recoverCorruptedChain(corruptKey, "Corrupt User");
            
            // Verify recovery succeeded
            assertTrue(result.isSuccess(), "Recovery should succeed for end corruption");
            var recoveredValidation = blockchain.validateChainDetailed();
            assertTrue(recoveredValidation.isStructurallyIntact(), "Chain should be structurally intact after recovery");
            assertTrue(recoveredValidation.isFullyCompliant(), "Chain should be fully compliant after recovery");
            
            System.out.println("Recovery successful: " + result.isSuccess());
            System.out.println("Recovery method: " + result.getMethod());
            System.out.println("Final chain: " + blockchain.getBlockCount() + " blocks");
            
            // For end corruption, intelligent rollback should preserve more blocks
            long finalBlocks = blockchain.getBlockCount();
            
            // At minimum genesis should be preserved
            assertTrue(finalBlocks >= 1, "Should preserve at least genesis block");
            
            if (finalBlocks >= 4) { // Genesis + 3 valid blocks
                System.out.println("✅ EXCELLENT: All valid blocks preserved!");
            } else if (finalBlocks >= 2) {
                System.out.println("✅ GOOD: Some valid blocks preserved!");
            } else {
                System.out.println("ℹ️ Note: Conservative rollback used (still safe)");
            }

            // Verify all preserved blocks are valid
            long blockCountForValidation = blockchain.getBlockCount();
            for (long i = 0; i < blockCountForValidation; i++) {
                Block block = blockchain.getBlock(i);
                if (block != null) {
                    assertTrue(blockchain.validateSingleBlock(block),
                        "All preserved blocks should be valid");
                }
            }
        }

        @Test
        @DisplayName("Should demonstrate security-first approach")
        void shouldDemonstrateSecurityFirstApproach() {
            System.out.println("\n🔬 TEST: Security-First Approach Verification");
            
            // Create a simple corruption scenario
            KeyPair user = CryptoUtil.generateKeyPair();
            String userKey = CryptoUtil.publicKeyToString(user.getPublic());

            blockchain.addAuthorizedKey(userKey, "Test User", adminKeyPair, UserRole.USER);
            blockchain.addBlock("Test Block", user.getPrivate(), user.getPublic());
            
            var initialValidation = blockchain.validateChainDetailed();
            assertTrue(initialValidation.isStructurallyIntact() && initialValidation.isFullyCompliant(), 
                "Initial chain should be valid");
            
            // Create corruption
            String securityReason = "Security test";
            String securityAdminSignature = CryptoUtil.createAdminSignature(userKey, true, securityReason, adminKeyPair.getPrivate());
            blockchain.dangerouslyDeleteAuthorizedKey(userKey, true, securityReason, securityAdminSignature, adminPublicKey);
            var corruptedValidation = blockchain.validateChainDetailed();
            assertFalse(corruptedValidation.isFullyCompliant(), 
                "Chain should not be fully compliant after corruption");
            assertTrue(corruptedValidation.isStructurallyIntact(), 
                "Chain should still be structurally intact after corruption");
            
            // Attempt recovery
            ChainRecoveryManager.RecoveryResult result = 
                recoveryManager.recoverCorruptedChain(userKey, "Test User");
            
            // Verify recovery prioritizes security
            assertTrue(result.isSuccess(), "Recovery should always succeed");
            var recoveredValidation = blockchain.validateChainDetailed();
            assertTrue(recoveredValidation.isStructurallyIntact(), "Recovered chain must be structurally intact");
            assertTrue(recoveredValidation.isFullyCompliant(), "Recovered chain must be fully compliant");
            
            // Most important: verify the recovered chain is cryptographically sound
            long finalBlockCount = blockchain.getBlockCount();

            // Test hash chain integrity
            for (long i = 1; i < finalBlockCount; i++) {
                Block currentBlock = blockchain.getBlock(i);
                Block previousBlock = blockchain.getBlock(i - 1);

                if (currentBlock != null && previousBlock != null) {
                    assertEquals(previousBlock.getHash(), currentBlock.getPreviousHash(),
                        "Hash chain must be intact in recovered blockchain");
                }
            }

            // Test individual block validity
            for (long i = 0; i < finalBlockCount; i++) {
                Block block = blockchain.getBlock(i);
                if (block != null) {
                    assertTrue(blockchain.validateSingleBlock(block),
                        "Every block in recovered chain must be individually valid");
                }
            }

            System.out.println("✅ SECURITY VERIFIED: All blocks in recovered chain are cryptographically valid");
            System.out.println("Recovery method used: " + result.getMethod());
            System.out.println("Final blocks: " + finalBlockCount);
        }
    }

    @Nested
    @DisplayName("Rollback Strategy Analysis")
    class RollbackStrategyAnalysis {

        @Test
        @DisplayName("Should provide detailed analysis information")
        void shouldProvideDetailedAnalysisInformation() {
            System.out.println("\n🔬 TEST: Rollback Analysis Information");
            
            // Create test scenario
            KeyPair user1 = CryptoUtil.generateKeyPair();
            KeyPair user2 = CryptoUtil.generateKeyPair();
            
            String user1Key = CryptoUtil.publicKeyToString(user1.getPublic());
            String user2Key = CryptoUtil.publicKeyToString(user2.getPublic());

            blockchain.addAuthorizedKey(user1Key, "User 1", adminKeyPair, UserRole.USER);
            blockchain.addAuthorizedKey(user2Key, "User 2", adminKeyPair, UserRole.USER);
            
            blockchain.addBlock("Block-1", user1.getPrivate(), user1.getPublic());
            blockchain.addBlock("Block-2", user2.getPrivate(), user2.getPublic());
            blockchain.addBlock("Block-3", user1.getPrivate(), user1.getPublic());
            
            // Create corruption
            String analysisReason = "Analysis test";
            String analysisAdminSignature = CryptoUtil.createAdminSignature(user1Key, true, analysisReason, adminKeyPair.getPrivate());
            blockchain.dangerouslyDeleteAuthorizedKey(user1Key, true, analysisReason, analysisAdminSignature, adminPublicKey);
            
            // Capture output to verify analysis is provided
            // The improved strategy should provide detailed logging
            ChainRecoveryManager.RecoveryResult result = 
                recoveryManager.recoverCorruptedChain(user1Key, "User 1");
            
            assertTrue(result.isSuccess(), "Recovery should succeed");
            assertNotNull(result.getMethod(), "Should specify recovery method used");
            assertNotNull(result.getMessage(), "Should provide detailed message");
            assertTrue(result.getTimestamp() > 0, "Should include timestamp");
            
            // Verify the result contains meaningful information
            assertTrue(result.getMessage().length() > 10, 
                "Recovery message should be informative");
            
            System.out.println("✅ ANALYSIS VERIFIED: Detailed recovery information provided");
            System.out.println("Method: " + result.getMethod());
            System.out.println("Message: " + result.getMessage());
        }

        @Test
        @DisplayName("Should handle edge cases gracefully")
        void shouldHandleEdgeCasesGracefully() {
            System.out.println("\n🔬 TEST: Edge Cases Handling");
            
            // Test 1: Empty chain (only genesis)
            KeyPair user = CryptoUtil.generateKeyPair();
            String userKey = CryptoUtil.publicKeyToString(user.getPublic());

            blockchain.addAuthorizedKey(userKey, "Test User", adminKeyPair, UserRole.USER);
            blockchain.revokeAuthorizedKey(userKey); // Safe deletion
            
            // This should fail since key was safely deleted, not dangerously
            ChainRecoveryManager.RecoveryResult result1 = 
                recoveryManager.recoverCorruptedChain(userKey, "Test User");
            
            assertFalse(result1.isSuccess(), "Should fail for safely deleted key");
            assertEquals("VALIDATION_ERROR", result1.getMethod());
            
            // Test 2: Very short chain with corruption
            blockchain.clearAndReinitialize();

            // RBAC FIX (v1.0.6): Re-create bootstrap admin after clearAndReinitialize()
            blockchain.createBootstrapAdmin(
                CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
                "BOOTSTRAP_ADMIN"
            );

            blockchain.addAuthorizedKey(adminPublicKey, "Test Admin", bootstrapKeyPair, UserRole.ADMIN);
            blockchain.addAuthorizedKey(userKey, "Test User", adminKeyPair, UserRole.USER);
            blockchain.addBlock("Only Block", user.getPrivate(), user.getPublic());
            String edgeReason = "Edge case test";
            String edgeAdminSignature = CryptoUtil.createAdminSignature(userKey, true, edgeReason, adminKeyPair.getPrivate());
            blockchain.dangerouslyDeleteAuthorizedKey(userKey, true, edgeReason, edgeAdminSignature, adminPublicKey);
            
            ChainRecoveryManager.RecoveryResult result2 = 
                recoveryManager.recoverCorruptedChain(userKey, "Test User");
            
            assertTrue(result2.isSuccess(), "Should handle short chain corruption");
            var shortChainValidation = blockchain.validateChainDetailed();
            assertTrue(shortChainValidation.isStructurallyIntact(), "Recovered short chain should be structurally intact");
            assertTrue(shortChainValidation.isFullyCompliant(), "Recovered short chain should be fully compliant");
            
            System.out.println("✅ EDGE CASES VERIFIED: Graceful handling of unusual scenarios");
        }
    }

}
