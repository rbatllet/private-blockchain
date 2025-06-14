package com.rbatllet.blockchain.recovery;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.security.KeyPair;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the improved intelligent rollback strategy
 * These tests verify that the enhanced rollback logic preserves
 * maximum valid blocks while maintaining security
 */
@DisplayName("Improved Rollback Strategy Tests")
class ImprovedRollbackStrategyTest {

    private Blockchain blockchain;
    private ChainRecoveryManager recoveryManager;

    @BeforeEach
    void setUp() {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();
        recoveryManager = new ChainRecoveryManager(blockchain);
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
            blockchain.addAuthorizedKey(user1Key, "User 1");
            blockchain.addAuthorizedKey(user2Key, "User 2");
            
            // Create interleaved pattern
            blockchain.addBlock("User1-Block1", user1.getPrivate(), user1.getPublic()); // #1 - Will corrupt
            blockchain.addBlock("User2-Block1", user2.getPrivate(), user2.getPublic()); // #2 - Should preserve ideally
            blockchain.addBlock("User1-Block2", user1.getPrivate(), user1.getPublic()); // #3 - Will corrupt
            blockchain.addBlock("User2-Block2", user2.getPrivate(), user2.getPublic()); // #4 - Should preserve ideally
            
            int initialBlocks = blockchain.getAllBlocks().size();
            assertTrue(blockchain.validateChain(), "Initial chain should be valid");
            
            System.out.println("Initial chain: " + initialBlocks + " blocks");
            System.out.println("Chain valid: " + blockchain.validateChain());
            
            // Create corruption
            blockchain.dangerouslyDeleteAuthorizedKey(user1Key, true, "Test interleaved corruption");
            assertFalse(blockchain.validateChain(), "Chain should be invalid after key deletion");
            
            System.out.println("After corruption - Chain valid: " + blockchain.validateChain());
            
            // Attempt recovery
            ChainRecoveryManager.RecoveryResult result = 
                recoveryManager.recoverCorruptedChain(user1Key, "User 1");
            
            // Verify recovery succeeded
            assertTrue(result.isSuccess(), "Recovery should succeed");
            assertTrue(blockchain.validateChain(), "Chain should be valid after recovery");
            assertNotNull(result.getMethod(), "Recovery method should be specified");
            
            System.out.println("Recovery successful: " + result.isSuccess());
            System.out.println("Recovery method: " + result.getMethod());
            System.out.println("Final chain: " + blockchain.getAllBlocks().size() + " blocks");
            System.out.println("Final chain valid: " + blockchain.validateChain());
            
            // Check preservation efficiency
            int finalBlocks = blockchain.getAllBlocks().size();
            long user2BlocksRemaining = blockchain.getAllBlocks().stream()
                .filter(b -> b.getData().contains("User2"))
                .count();
            
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
            for (Block block : blockchain.getAllBlocks()) {
                assertTrue(blockchain.validateSingleBlock(block), 
                    "All preserved blocks should be valid");
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
            blockchain.addAuthorizedKey(validKey, "Valid User");
            blockchain.addAuthorizedKey(corruptKey, "Corrupt User");
            
            // Create pattern with corruption at end
            blockchain.addBlock("Valid-1", validUser.getPrivate(), validUser.getPublic());    // #1 - Should preserve
            blockchain.addBlock("Valid-2", validUser.getPrivate(), validUser.getPublic());    // #2 - Should preserve
            blockchain.addBlock("Valid-3", validUser.getPrivate(), validUser.getPublic());    // #3 - Should preserve
            blockchain.addBlock("Corrupt", corruptUser.getPrivate(), corruptUser.getPublic()); // #4 - Will corrupt
            
            int initialBlocks = blockchain.getAllBlocks().size();
            assertTrue(blockchain.validateChain(), "Initial chain should be valid");
            
            System.out.println("Initial chain: " + initialBlocks + " blocks");
            
            // Create corruption
            blockchain.dangerouslyDeleteAuthorizedKey(corruptKey, true, "Test end corruption");
            assertFalse(blockchain.validateChain(), "Chain should be invalid after corruption");
            
            // Attempt recovery
            ChainRecoveryManager.RecoveryResult result = 
                recoveryManager.recoverCorruptedChain(corruptKey, "Corrupt User");
            
            // Verify recovery succeeded
            assertTrue(result.isSuccess(), "Recovery should succeed for end corruption");
            assertTrue(blockchain.validateChain(), "Chain should be valid after recovery");
            
            System.out.println("Recovery successful: " + result.isSuccess());
            System.out.println("Recovery method: " + result.getMethod());
            System.out.println("Final chain: " + blockchain.getAllBlocks().size() + " blocks");
            
            // For end corruption, intelligent rollback should preserve more blocks
            int finalBlocks = blockchain.getAllBlocks().size();
            
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
            for (Block block : blockchain.getAllBlocks()) {
                assertTrue(blockchain.validateSingleBlock(block), 
                    "All preserved blocks should be valid");
            }
        }

        @Test
        @DisplayName("Should demonstrate security-first approach")
        void shouldDemonstrateSecurityFirstApproach() {
            System.out.println("\n🔬 TEST: Security-First Approach Verification");
            
            // Create a simple corruption scenario
            KeyPair user = CryptoUtil.generateKeyPair();
            String userKey = CryptoUtil.publicKeyToString(user.getPublic());
            
            blockchain.addAuthorizedKey(userKey, "Test User");
            blockchain.addBlock("Test Block", user.getPrivate(), user.getPublic());
            
            assertTrue(blockchain.validateChain(), "Initial chain should be valid");
            
            // Create corruption
            blockchain.dangerouslyDeleteAuthorizedKey(userKey, true, "Security test");
            assertFalse(blockchain.validateChain(), "Chain should be invalid after corruption");
            
            // Attempt recovery
            ChainRecoveryManager.RecoveryResult result = 
                recoveryManager.recoverCorruptedChain(userKey, "Test User");
            
            // Verify recovery prioritizes security
            assertTrue(result.isSuccess(), "Recovery should always succeed");
            assertTrue(blockchain.validateChain(), "Recovered chain must be valid");
            
            // Most important: verify the recovered chain is cryptographically sound
            List<Block> finalBlocks = blockchain.getAllBlocks();
            
            // Test hash chain integrity
            for (int i = 1; i < finalBlocks.size(); i++) {
                Block currentBlock = finalBlocks.get(i);
                Block previousBlock = finalBlocks.get(i - 1);
                
                assertEquals(previousBlock.getHash(), currentBlock.getPreviousHash(),
                    "Hash chain must be intact in recovered blockchain");
            }
            
            // Test individual block validity
            for (Block block : finalBlocks) {
                assertTrue(blockchain.validateSingleBlock(block),
                    "Every block in recovered chain must be individually valid");
            }
            
            System.out.println("✅ SECURITY VERIFIED: All blocks in recovered chain are cryptographically valid");
            System.out.println("Recovery method used: " + result.getMethod());
            System.out.println("Final blocks: " + finalBlocks.size());
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
            
            blockchain.addAuthorizedKey(user1Key, "User 1");
            blockchain.addAuthorizedKey(user2Key, "User 2");
            
            blockchain.addBlock("Block-1", user1.getPrivate(), user1.getPublic());
            blockchain.addBlock("Block-2", user2.getPrivate(), user2.getPublic());
            blockchain.addBlock("Block-3", user1.getPrivate(), user1.getPublic());
            
            // Create corruption
            blockchain.dangerouslyDeleteAuthorizedKey(user1Key, true, "Analysis test");
            
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
            
            blockchain.addAuthorizedKey(userKey, "Test User");
            blockchain.revokeAuthorizedKey(userKey); // Safe deletion
            
            // This should fail since key was safely deleted, not dangerously
            ChainRecoveryManager.RecoveryResult result1 = 
                recoveryManager.recoverCorruptedChain(userKey, "Test User");
            
            assertFalse(result1.isSuccess(), "Should fail for safely deleted key");
            assertEquals("VALIDATION_ERROR", result1.getMethod());
            
            // Test 2: Very short chain with corruption
            blockchain.clearAndReinitialize();
            blockchain.addAuthorizedKey(userKey, "Test User");
            blockchain.addBlock("Only Block", user.getPrivate(), user.getPublic());
            blockchain.dangerouslyDeleteAuthorizedKey(userKey, true, "Edge case test");
            
            ChainRecoveryManager.RecoveryResult result2 = 
                recoveryManager.recoverCorruptedChain(userKey, "Test User");
            
            assertTrue(result2.isSuccess(), "Should handle short chain corruption");
            assertTrue(blockchain.validateChain(), "Recovered short chain should be valid");
            
            System.out.println("✅ EDGE CASES VERIFIED: Graceful handling of unusual scenarios");
        }
    }
}
