package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.entity.Block;
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
 * Comprehensive tests for the core Blockchain functionality
 */
@DisplayName("Blockchain Core Tests")
class BlockchainTest {

    private Blockchain blockchain;
    private KeyPair testKeyPair;
    private String testPublicKey;

    @BeforeEach
    void setUp() {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();
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
    @DisplayName("Blockchain Initialization")
    class BlockchainInitialization {

        @Test
        @DisplayName("Should initialize with genesis block")
        void shouldInitializeWithGenesisBlock() {
            assertEquals(1, blockchain.getBlockCount());
            ChainValidationResult result = blockchain.validateChainDetailed();
            assertTrue(result.isFullyCompliant());
            assertTrue(result.isStructurallyIntact());
        }

        @Test
        @DisplayName("Should clear and reinitialize properly")
        void shouldClearAndReinitializeProperly() {
            // Add some data
            blockchain.addAuthorizedKey(testPublicKey, "Test User");
            blockchain.addBlock("Test data", testKeyPair.getPrivate(), testKeyPair.getPublic());
            
            assertTrue(blockchain.getBlockCount() > 1);
            
            // Clear and reinitialize
            blockchain.clearAndReinitialize();
            
            assertEquals(1, blockchain.getBlockCount());
            ChainValidationResult result = blockchain.validateChainDetailed();
            assertTrue(result.isFullyCompliant());
            assertTrue(result.isStructurallyIntact());
            assertEquals(0, blockchain.getAuthorizedKeys().size());
        }
    }

    @Nested
    @DisplayName("Key Management")
    class KeyManagement {

        @Test
        @DisplayName("Should add authorized key successfully")
        void shouldAddAuthorizedKeySuccessfully() {
            boolean result = blockchain.addAuthorizedKey(testPublicKey, "Test User");
            
            assertTrue(result);
            
            // Check if key exists in the list
            boolean keyFound = blockchain.getAuthorizedKeys().stream()
                .anyMatch(key -> key.getPublicKey().equals(testPublicKey));
            assertTrue(keyFound);
            
            // Check owner name
            String ownerName = blockchain.getAuthorizedKeys().stream()
                .filter(key -> key.getPublicKey().equals(testPublicKey))
                .findFirst()
                .map(key -> key.getOwnerName())
                .orElse(null);
            assertEquals("Test User", ownerName);
        }

        @Test
        @DisplayName("Should reject duplicate keys")
        void shouldRejectDuplicateKeys() {
            assertTrue(blockchain.addAuthorizedKey(testPublicKey, "Test User"));
            assertFalse(blockchain.addAuthorizedKey(testPublicKey, "Another User"));
        }

        @Test
        @DisplayName("Should revoke authorized key successfully")
        void shouldRevokeAuthorizedKeySuccessfully() {
            blockchain.addAuthorizedKey(testPublicKey, "Test User");
            
            // Check key exists
            boolean keyFound = blockchain.getAuthorizedKeys().stream()
                .anyMatch(key -> key.getPublicKey().equals(testPublicKey));
            assertTrue(keyFound);
            
            boolean result = blockchain.revokeAuthorizedKey(testPublicKey);
            
            assertTrue(result);
            
            // Check key no longer exists
            boolean keyStillFound = blockchain.getAuthorizedKeys().stream()
                .anyMatch(key -> key.getPublicKey().equals(testPublicKey));
            assertFalse(keyStillFound);
        }

        @Test
        @DisplayName("Should handle revoke of non-existent key")
        void shouldHandleRevokeOfNonExistentKey() {
            assertFalse(blockchain.revokeAuthorizedKey(testPublicKey));
        }

        @Test
        @DisplayName("Should assess deletion impact correctly")
        void shouldAssessDeletionImpactCorrectly() {
            // Key that doesn't exist
            var impact1 = blockchain.canDeleteAuthorizedKey("non-existent-key");
            assertFalse(impact1.keyExists());
            
            // Key that exists but has no blocks
            blockchain.addAuthorizedKey(testPublicKey, "Test User");
            var impact2 = blockchain.canDeleteAuthorizedKey(testPublicKey);
            assertTrue(impact2.keyExists());
            assertTrue(impact2.canSafelyDelete());
            assertEquals(0, impact2.getAffectedBlocks());
            
            // Key that exists and has blocks
            blockchain.addBlock("Test data", testKeyPair.getPrivate(), testKeyPair.getPublic());
            var impact3 = blockchain.canDeleteAuthorizedKey(testPublicKey);
            assertTrue(impact3.keyExists());
            assertFalse(impact3.canSafelyDelete());
            assertEquals(1, impact3.getAffectedBlocks());
        }
    }

    @Nested
    @DisplayName("Block Operations")
    class BlockOperations {

        @Test
        @DisplayName("Should add block successfully")
        void shouldAddBlockSuccessfully() {
            blockchain.addAuthorizedKey(testPublicKey, "Test User");
            
            boolean result = blockchain.addBlock("Test data", testKeyPair.getPrivate(), testKeyPair.getPublic());
            
            assertTrue(result);
            assertEquals(2, blockchain.getBlockCount());
            ChainValidationResult validation = blockchain.validateChainDetailed();
            assertTrue(validation.isFullyCompliant());
            assertTrue(validation.isStructurallyIntact());
        }

        @Test
        @DisplayName("Should reject block from unauthorized key")
        void shouldRejectBlockFromUnauthorizedKey() {
            // Don't add the key to authorized keys
            boolean result = blockchain.addBlock("Test data", testKeyPair.getPrivate(), testKeyPair.getPublic());
            
            assertFalse(result);
            assertEquals(1, blockchain.getBlockCount()); // Only genesis
        }

        @Test
        @DisplayName("Should validate single block correctly")
        void shouldValidateSingleBlockCorrectly() {
            blockchain.addAuthorizedKey(testPublicKey, "Test User");
            blockchain.addBlock("Test data", testKeyPair.getPrivate(), testKeyPair.getPublic());
            
            var blocks = blockchain.getAllBlocks();
            assertTrue(blocks.size() >= 2);
            
            // Genesis block should be valid
            assertTrue(blockchain.validateSingleBlock(blocks.get(0)));
            
            // Test block should be valid
            assertTrue(blockchain.validateSingleBlock(blocks.get(1)));
        }

        @Test
        @DisplayName("Should handle block size limits")
        void shouldHandleBlockSizeLimits() {
            blockchain.addAuthorizedKey(testPublicKey, "Test User");
            
            // Normal size should work
            String normalData = "A".repeat(1000);
            assertTrue(blockchain.addBlock(normalData, testKeyPair.getPrivate(), testKeyPair.getPublic()));
            
            // Very large data should now be stored off-chain (not rejected)
            String largeData = "A".repeat(20000);
            assertTrue(blockchain.addBlock(largeData, testKeyPair.getPrivate(), testKeyPair.getPublic()));
        }
    }

    @Nested
    @DisplayName("Chain Validation")
    class ChainValidation {

        @Test
        @DisplayName("Should validate healthy chain")
        void shouldValidateHealthyChain() {
            blockchain.addAuthorizedKey(testPublicKey, "Test User");
            blockchain.addBlock("Test data 1", testKeyPair.getPrivate(), testKeyPair.getPublic());
            blockchain.addBlock("Test data 2", testKeyPair.getPrivate(), testKeyPair.getPublic());
            
            ChainValidationResult result = blockchain.validateChainDetailed();
            assertTrue(result.isFullyCompliant());
            assertTrue(result.isStructurallyIntact());
        }

        @Test
        @DisplayName("Should detect corrupted chain")
        void shouldDetectCorruptedChain() {
            blockchain.addAuthorizedKey(testPublicKey, "Test User");
            blockchain.addBlock("Test data", testKeyPair.getPrivate(), testKeyPair.getPublic());
            
            ChainValidationResult beforeCorruption = blockchain.validateChainDetailed();
            assertTrue(beforeCorruption.isFullyCompliant());
            assertTrue(beforeCorruption.isStructurallyIntact());
            
            // Delete the key to corrupt the chain
            blockchain.dangerouslyDeleteAuthorizedKey(testPublicKey, true, "Test corruption");
            
            ChainValidationResult afterCorruption = blockchain.validateChainDetailed();
            assertTrue(afterCorruption.isStructurallyIntact()); // Structure still intact
            assertFalse(afterCorruption.isFullyCompliant()); // But not fully compliant
            assertTrue(afterCorruption.getRevokedBlocks() > 0); // Should have revoked blocks
        }
    }

    @Nested
    @DisplayName("Recovery Integration")
    class RecoveryIntegration {

        @Test
        @DisplayName("Should integrate with recovery manager")
        void shouldIntegrateWithRecoveryManager() {
            blockchain.addAuthorizedKey(testPublicKey, "Test User");
            blockchain.addBlock("Test data", testKeyPair.getPrivate(), testKeyPair.getPublic());
            
            ChainValidationResult beforeCorruption = blockchain.validateChainDetailed();
            assertTrue(beforeCorruption.isFullyCompliant());
            assertTrue(beforeCorruption.isStructurallyIntact());
            
            // Create corruption
            blockchain.dangerouslyDeleteAuthorizedKey(testPublicKey, true, "Test corruption");
            ChainValidationResult afterCorruption = blockchain.validateChainDetailed();
            assertTrue(afterCorruption.isStructurallyIntact());
            assertFalse(afterCorruption.isFullyCompliant());
            
            // Test diagnostic method
            var diagnostic = blockchain.diagnoseCorruption();
            assertNotNull(diagnostic);
            assertFalse(diagnostic.isHealthy());
            assertTrue(diagnostic.getCorruptedBlocks() > 0);
            
            // Test recovery method
            var recoveryResult = blockchain.recoverCorruptedChain(testPublicKey, "Test User");
            assertNotNull(recoveryResult);
            assertTrue(recoveryResult.isSuccess());
        }

        @Test
        @DisplayName("Should validate chain with recovery")
        void shouldValidateChainWithRecovery() {
            blockchain.addAuthorizedKey(testPublicKey, "Test User");
            blockchain.addBlock("Test data", testKeyPair.getPrivate(), testKeyPair.getPublic());
            
            // Healthy chain should validate normally
            assertTrue(blockchain.validateChainWithRecovery());
            
            // Corrupted chain should trigger diagnostic
            blockchain.dangerouslyDeleteAuthorizedKey(testPublicKey, true, "Test corruption");
            assertFalse(blockchain.validateChainWithRecovery());
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should handle null parameters gracefully")
        void shouldHandleNullParametersGracefully() {
            assertFalse(blockchain.addAuthorizedKey(null, "Test User"));
            assertFalse(blockchain.addAuthorizedKey(testPublicKey, null));
            assertFalse(blockchain.revokeAuthorizedKey(null));
            
            blockchain.addAuthorizedKey(testPublicKey, "Test User");
            assertFalse(blockchain.addBlock(null, testKeyPair.getPrivate(), testKeyPair.getPublic()));
            assertFalse(blockchain.addBlock("Test", null, testKeyPair.getPublic()));
            assertFalse(blockchain.addBlock("Test", testKeyPair.getPrivate(), null));
        }

        @Test
        @DisplayName("Should handle empty parameters gracefully")
        void shouldHandleEmptyParametersGracefully() {
            assertFalse(blockchain.addAuthorizedKey("", "Test User"));
            assertFalse(blockchain.addAuthorizedKey(testPublicKey, ""));
            assertFalse(blockchain.revokeAuthorizedKey(""));
        }
        
        @Test
        @DisplayName("Should allow empty data for system blocks")
        void shouldAllowEmptyDataForSystemBlocks() {
            // Add a test user
            blockchain.addAuthorizedKey(testPublicKey, "Test User");
            
            // Empty data should be allowed (for system blocks)
            assertTrue(blockchain.addBlock("", testKeyPair.getPrivate(), testKeyPair.getPublic()));
            ChainValidationResult result = blockchain.validateChainDetailed();
            assertTrue(result.isFullyCompliant());
            assertTrue(result.isStructurallyIntact());
            
            // Verify the block was created
            assertEquals(2, blockchain.getAllBlocks().size()); // Genesis + system block
            
            // The system block should have empty data
            Block systemBlock = blockchain.getAllBlocks().get(1);
            assertEquals("", systemBlock.getData());
        }
    }
}