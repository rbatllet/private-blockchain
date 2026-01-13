package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.config.MemorySafetyConstants;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.validation.ChainValidationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.security.KeyPair;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the core Blockchain functionality
 */
@DisplayName("Blockchain Core Tests")
class BlockchainTest {

    private Blockchain blockchain;
    private KeyPair testKeyPair;
    private String testPublicKey;
    private KeyPair adminKeyPair;
    private String adminPublicKey;

    @BeforeEach
    void setUp() {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();

        // Setup admin
        adminKeyPair = CryptoUtil.generateKeyPair();
        adminPublicKey = CryptoUtil.publicKeyToString(adminKeyPair.getPublic());
        blockchain.createBootstrapAdmin(adminPublicKey, "Test Admin");

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
            // Add some data (admin creates test user)
            blockchain.addAuthorizedKey(testPublicKey, "Test User", adminKeyPair, UserRole.USER);
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
            boolean result = blockchain.addAuthorizedKey(testPublicKey, "Test User", adminKeyPair, UserRole.USER);

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
            assertTrue(blockchain.addAuthorizedKey(testPublicKey, "Test User", adminKeyPair, UserRole.USER));
            assertFalse(blockchain.addAuthorizedKey(testPublicKey, "Another User", adminKeyPair, UserRole.USER));
        }

        @Test
        @DisplayName("Should revoke authorized key successfully")
        void shouldRevokeAuthorizedKeySuccessfully() {
            blockchain.addAuthorizedKey(testPublicKey, "Test User", adminKeyPair, UserRole.USER);
            
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
            // Revoking non-existent key should throw IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> {
                blockchain.revokeAuthorizedKey(testPublicKey);
            }, "Should throw IllegalArgumentException when revoking non-existent key");
        }

        @Test
        @DisplayName("Should assess deletion impact correctly")
        void shouldAssessDeletionImpactCorrectly() {
            // Key that doesn't exist
            var impact1 = blockchain.canDeleteAuthorizedKey("non-existent-key");
            assertFalse(impact1.keyExists());
            
            // Key that exists but has no blocks
            blockchain.addAuthorizedKey(testPublicKey, "Test User", adminKeyPair, UserRole.USER);
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
            blockchain.addAuthorizedKey(testPublicKey, "Test User", adminKeyPair, UserRole.USER);
            
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
            blockchain.addAuthorizedKey(testPublicKey, "Test User", adminKeyPair, UserRole.USER);
            blockchain.addBlock("Test data", testKeyPair.getPrivate(), testKeyPair.getPublic());

            assertTrue(blockchain.getBlockCount() >= 2);

            // Genesis block should be valid
            Block genesisBlock = blockchain.getBlock(0L);
            assertTrue(blockchain.validateSingleBlock(genesisBlock));

            // Test block should be valid
            Block testBlock = blockchain.getBlock(1L);
            assertTrue(blockchain.validateSingleBlock(testBlock));
        }

        @Test
        @DisplayName("Should handle block size limits")
        void shouldHandleBlockSizeLimits() {
            blockchain.addAuthorizedKey(testPublicKey, "Test User", adminKeyPair, UserRole.USER);
            
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
            blockchain.addAuthorizedKey(testPublicKey, "Test User", adminKeyPair, UserRole.USER);
            blockchain.addBlock("Test data 1", testKeyPair.getPrivate(), testKeyPair.getPublic());
            blockchain.addBlock("Test data 2", testKeyPair.getPrivate(), testKeyPair.getPublic());
            
            ChainValidationResult result = blockchain.validateChainDetailed();
            assertTrue(result.isFullyCompliant());
            assertTrue(result.isStructurallyIntact());
        }

        @Test
        @DisplayName("Should detect corrupted chain")
        void shouldDetectCorruptedChain() {
            blockchain.addAuthorizedKey(testPublicKey, "Test User", adminKeyPair, UserRole.USER);
            blockchain.addBlock("Test data", testKeyPair.getPrivate(), testKeyPair.getPublic());
            
            ChainValidationResult beforeCorruption = blockchain.validateChainDetailed();
            assertTrue(beforeCorruption.isFullyCompliant());
            assertTrue(beforeCorruption.isStructurallyIntact());
            
            // Delete the key to corrupt the chain
            String corruption1Reason = "Test corruption";
            String corruption1AdminSignature = CryptoUtil.createAdminSignature(testPublicKey, true, corruption1Reason, adminKeyPair.getPrivate());
            blockchain.dangerouslyDeleteAuthorizedKey(testPublicKey, true, corruption1Reason, corruption1AdminSignature, adminPublicKey);
            
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
            blockchain.addAuthorizedKey(testPublicKey, "Test User", adminKeyPair, UserRole.USER);
            blockchain.addBlock("Test data", testKeyPair.getPrivate(), testKeyPair.getPublic());
            
            ChainValidationResult beforeCorruption = blockchain.validateChainDetailed();
            assertTrue(beforeCorruption.isFullyCompliant());
            assertTrue(beforeCorruption.isStructurallyIntact());
            
            // Create corruption
            String corruption2Reason = "Test corruption";
            String corruption2AdminSignature = CryptoUtil.createAdminSignature(testPublicKey, true, corruption2Reason, adminKeyPair.getPrivate());
            blockchain.dangerouslyDeleteAuthorizedKey(testPublicKey, true, corruption2Reason, corruption2AdminSignature, adminPublicKey);
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
            blockchain.addAuthorizedKey(testPublicKey, "Test User", adminKeyPair, UserRole.USER);
            blockchain.addBlock("Test data", testKeyPair.getPrivate(), testKeyPair.getPublic());
            
            // Healthy chain should validate normally
            assertTrue(blockchain.validateChainWithRecovery());
            
            // Corrupted chain should trigger diagnostic
            String corruption3Reason = "Test corruption";
            String corruption3AdminSignature = CryptoUtil.createAdminSignature(testPublicKey, true, corruption3Reason, adminKeyPair.getPrivate());
            blockchain.dangerouslyDeleteAuthorizedKey(testPublicKey, true, corruption3Reason, corruption3AdminSignature, adminPublicKey);
            assertFalse(blockchain.validateChainWithRecovery());
        }
    }

    @Nested
    @DisplayName("Search Initialization")
    class SearchInitialization {

        @Test
        @DisplayName("Should initialize advanced search with multiple passwords")
        void shouldInitializeAdvancedSearchWithMultiplePasswords() {
            // Setup test data
            blockchain.addAuthorizedKey(testPublicKey, "Test User", adminKeyPair, UserRole.USER);

            // Store some encrypted data first
            String medicalPassword = "MedicalDept2024!SecureKey_abcd1234";
            String financialPassword = "FinanceDept2024!SecureKey_efgh5678";
            String legalPassword = "LegalDept2024!SecureKey_ijkl9012";

            // Add multiple blocks with different content
            blockchain.addBlock("Medical patient record data", testKeyPair.getPrivate(), testKeyPair.getPublic());
            blockchain.addBlock("Financial transaction report", testKeyPair.getPrivate(), testKeyPair.getPublic());
            blockchain.addBlock("Legal contract agreement", testKeyPair.getPrivate(), testKeyPair.getPublic());

            // Test initialization with multiple passwords
            String[] departmentPasswords = {medicalPassword, financialPassword, legalPassword};

            assertDoesNotThrow(() -> {
                blockchain.initializeAdvancedSearchWithMultiplePasswords(departmentPasswords);
            });

            // Verify blockchain state is maintained after initialization
            assertEquals(4, blockchain.getBlockCount()); // Genesis + 3 test blocks
            ChainValidationResult result = blockchain.validateChainDetailed();
            assertTrue(result.isFullyCompliant());
            assertTrue(result.isStructurallyIntact());
        }

        @Test
        @DisplayName("Should handle empty password array gracefully")
        void shouldHandleEmptyPasswordArrayGracefully() {
            blockchain.addAuthorizedKey(testPublicKey, "Test User", adminKeyPair, UserRole.USER);
            blockchain.addBlock("Test data", testKeyPair.getPrivate(), testKeyPair.getPublic());

            // Empty password array should not throw
            assertDoesNotThrow(() -> {
                blockchain.initializeAdvancedSearchWithMultiplePasswords(new String[0]);
            });

            // Blockchain should remain valid
            ChainValidationResult result = blockchain.validateChainDetailed();
            assertTrue(result.isFullyCompliant());
            assertTrue(result.isStructurallyIntact());
        }

        @Test
        @DisplayName("Should handle null password array gracefully")
        void shouldHandleNullPasswordArrayGracefully() {
            blockchain.addAuthorizedKey(testPublicKey, "Test User", adminKeyPair, UserRole.USER);
            blockchain.addBlock("Test data", testKeyPair.getPrivate(), testKeyPair.getPublic());

            // Null password array should not throw
            assertDoesNotThrow(() -> {
                blockchain.initializeAdvancedSearchWithMultiplePasswords(null);
            });

            // Blockchain should remain valid
            ChainValidationResult result = blockchain.validateChainDetailed();
            assertTrue(result.isFullyCompliant());
            assertTrue(result.isStructurallyIntact());
        }

        @Test
        @DisplayName("Should initialize with empty blockchain")
        void shouldInitializeWithEmptyBlockchain() {
            // Don't add any blocks beyond genesis
            String[] passwords = {"TestPassword123!"};

            // Should handle empty blockchain gracefully
            assertDoesNotThrow(() -> {
                blockchain.initializeAdvancedSearchWithMultiplePasswords(passwords);
            });

            // Only genesis block should remain
            assertEquals(1, blockchain.getBlockCount());
            ChainValidationResult result = blockchain.validateChainDetailed();
            assertTrue(result.isFullyCompliant());
            assertTrue(result.isStructurallyIntact());
        }

        @Test
        @DisplayName("Should handle passwords with null elements")
        void shouldHandlePasswordsWithNullElements() {
            blockchain.addAuthorizedKey(testPublicKey, "Test User", adminKeyPair, UserRole.USER);
            blockchain.addBlock("Test data", testKeyPair.getPrivate(), testKeyPair.getPublic());

            // Password array with null elements
            String[] passwordsWithNulls = {"ValidPassword123!", null, "AnotherPassword456!"};

            assertDoesNotThrow(() -> {
                blockchain.initializeAdvancedSearchWithMultiplePasswords(passwordsWithNulls);
            });

            // Blockchain should remain valid
            ChainValidationResult result = blockchain.validateChainDetailed();
            assertTrue(result.isFullyCompliant());
            assertTrue(result.isStructurallyIntact());
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should reject null parameters with exceptions")
        void shouldRejectNullParametersWithExceptions() {
            // UPDATED: Matching BlockchainRobustnessTest strict validation standard
            blockchain.addAuthorizedKey(testPublicKey, "Test User", adminKeyPair, UserRole.USER);
            
            assertThrows(IllegalArgumentException.class, () -> {
                blockchain.addBlock(null, testKeyPair.getPrivate(), testKeyPair.getPublic());
            }, "Null data should throw exception");
            
            assertThrows(IllegalArgumentException.class, () -> {
                blockchain.addBlock("Test", null, testKeyPair.getPublic());
            }, "Null private key should throw exception");
            
            assertThrows(IllegalArgumentException.class, () -> {
                blockchain.addBlock("Test", testKeyPair.getPrivate(), null);
            }, "Null public key should throw exception");
        }

        @Test
        @DisplayName("Should handle empty parameters gracefully")
        void shouldHandleEmptyParametersGracefully() {
            // addAuthorizedKey throws IllegalArgumentException for empty parameters
            assertThrows(IllegalArgumentException.class, () ->
                blockchain.addAuthorizedKey("", "Test User", adminKeyPair, UserRole.USER));
            assertThrows(IllegalArgumentException.class, () ->
                blockchain.addAuthorizedKey(testPublicKey, "", adminKeyPair, UserRole.USER));
            // revokeAuthorizedKey also throws IllegalArgumentException for empty parameter
            assertThrows(IllegalArgumentException.class, () ->
                blockchain.revokeAuthorizedKey(""));
        }
        
        @Test
        @DisplayName("Should reject empty data")
        void shouldRejectEmptyData() {
            // Add a test user
            blockchain.addAuthorizedKey(testPublicKey, "Test User", adminKeyPair, UserRole.USER);
            
            // Empty data should be rejected (no legitimate use case for empty blocks)
            // UPDATED: Matching BlockchainRobustnessTest strict validation standard
            assertThrows(IllegalArgumentException.class, () -> {
                blockchain.addBlock("", testKeyPair.getPrivate(), testKeyPair.getPublic());
            }, "Empty data should throw exception");

            // Verify no block was created (only Genesis exists)
            assertEquals(1, blockchain.getBlockCount(), "Should only have Genesis block");
        }
    }

    @Nested
    @DisplayName("Recipient Filtering (P0 Performance Fix)")
    class RecipientFiltering {

        private KeyPair senderKeyPair;
        private String senderPublicKey;
        private KeyPair recipientKeyPair;
        private String recipientPublicKey;
        private KeyPair otherKeyPair;
        private String otherPublicKey;

        @BeforeEach
        void setUpRecipientFiltering() {
            // Create three users: sender, recipient, and other
            senderKeyPair = CryptoUtil.generateKeyPair();
            senderPublicKey = CryptoUtil.publicKeyToString(senderKeyPair.getPublic());

            recipientKeyPair = CryptoUtil.generateKeyPair();
            recipientPublicKey = CryptoUtil.publicKeyToString(recipientKeyPair.getPublic());

            otherKeyPair = CryptoUtil.generateKeyPair();
            otherPublicKey = CryptoUtil.publicKeyToString(otherKeyPair.getPublic());

            // Authorize all keys
            blockchain.addAuthorizedKey(senderPublicKey, "Sender", adminKeyPair, UserRole.USER);
            blockchain.addAuthorizedKey(recipientPublicKey, "Recipient", adminKeyPair, UserRole.USER);
            blockchain.addAuthorizedKey(otherPublicKey, "Other", adminKeyPair, UserRole.USER);
        }

        @Test
        @DisplayName("Should add block with recipient public key")
        void shouldAddBlockWithRecipientPublicKey() {
            // Add block with recipient public key
            Block block = blockchain.addBlockAndReturn(
                "Secret message for recipient",
                senderKeyPair.getPrivate(),
                senderKeyPair.getPublic(),
                recipientPublicKey
            );

            assertNotNull(block, "Block should be created");
            assertEquals(2, blockchain.getBlockCount(), "Should have 2 blocks (Genesis + new block)");

            // Verify recipient public key is set
            assertEquals(recipientPublicKey, block.getRecipientPublicKey(),
                "Recipient public key should be set");
        }

        @Test
        @DisplayName("Should add block without recipient public key")
        void shouldAddBlockWithoutRecipientPublicKey() {
            // Add block without recipient public key
            Block block = blockchain.addBlockAndReturn(
                "Public message",
                senderKeyPair.getPrivate(),
                senderKeyPair.getPublic()
            );

            assertNotNull(block, "Block should be created");
            assertEquals(2, blockchain.getBlockCount(), "Should have 2 blocks (Genesis + new block)");

            // Verify recipient public key is null
            assertNull(block.getRecipientPublicKey(),
                "Recipient public key should be null for non-recipient blocks");
        }

        @Test
        @DisplayName("Should get blocks by recipient public key")
        void shouldGetBlocksByRecipientPublicKey() {
            // Add 3 blocks: 2 for recipient, 1 for other
            blockchain.addBlockAndReturn("Message 1 for recipient",
                senderKeyPair.getPrivate(), senderKeyPair.getPublic(), recipientPublicKey);
            blockchain.addBlockAndReturn("Message 2 for recipient",
                senderKeyPair.getPrivate(), senderKeyPair.getPublic(), recipientPublicKey);
            blockchain.addBlockAndReturn("Message for other",
                senderKeyPair.getPrivate(), senderKeyPair.getPublic(), otherPublicKey);

            // Get blocks for recipient
            var recipientBlocks = blockchain.getBlocksByRecipientPublicKey(recipientPublicKey);

            assertEquals(2, recipientBlocks.size(), "Should find 2 blocks for recipient");

            // Verify all blocks have the correct recipient public key
            assertTrue(recipientBlocks.stream().allMatch(b -> recipientPublicKey.equals(b.getRecipientPublicKey())),
                "All blocks should have recipient public key set");
        }

        @Test
        @DisplayName("Should get blocks by recipient public key with limit")
        void shouldGetBlocksByRecipientPublicKeyWithLimit() {
            // Add 5 blocks for recipient
            for (int i = 1; i <= 5; i++) {
                blockchain.addBlockAndReturn("Message " + i,
                    senderKeyPair.getPrivate(), senderKeyPair.getPublic(), recipientPublicKey);
            }

            // Get blocks with limit of 3
            var recipientBlocks = blockchain.getBlocksByRecipientPublicKey(recipientPublicKey, 3);

            assertEquals(3, recipientBlocks.size(), "Should return only 3 blocks");
        }

        @Test
        @DisplayName("Should count blocks by recipient public key")
        void shouldCountBlocksByRecipientPublicKey() {
            // Add 3 blocks for recipient, 2 for other
            blockchain.addBlockAndReturn("Message 1 for recipient",
                senderKeyPair.getPrivate(), senderKeyPair.getPublic(), recipientPublicKey);
            blockchain.addBlockAndReturn("Message 2 for recipient",
                senderKeyPair.getPrivate(), senderKeyPair.getPublic(), recipientPublicKey);
            blockchain.addBlockAndReturn("Message 3 for recipient",
                senderKeyPair.getPrivate(), senderKeyPair.getPublic(), recipientPublicKey);
            blockchain.addBlockAndReturn("Message 1 for other",
                senderKeyPair.getPrivate(), senderKeyPair.getPublic(), otherPublicKey);
            blockchain.addBlockAndReturn("Message 2 for other",
                senderKeyPair.getPrivate(), senderKeyPair.getPublic(), otherPublicKey);

            // Count blocks for recipient
            long recipientCount = blockchain.countBlocksByRecipientPublicKey(recipientPublicKey);
            assertEquals(3, recipientCount, "Should count 3 blocks for recipient");

            // Count blocks for other
            long otherCount = blockchain.countBlocksByRecipientPublicKey(otherPublicKey);
            assertEquals(2, otherCount, "Should count 2 blocks for other");
        }

        @Test
        @DisplayName("Should return empty list for non-existent recipient")
        void shouldReturnEmptyListForNonExistentRecipient() {
            // Add a block for recipient
            blockchain.addBlockAndReturn("Message",
                senderKeyPair.getPrivate(), senderKeyPair.getPublic(), recipientPublicKey);

            // Get blocks for non-existent recipient
            String fakePublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEFAKE000000000000000";
            var blocks = blockchain.getBlocksByRecipientPublicKey(fakePublicKey);

            assertEquals(0, blocks.size(), "Should return empty list for non-existent recipient");
        }

        @Test
        @DisplayName("Should return zero count for non-existent recipient")
        void shouldReturnZeroCountForNonExistentRecipient() {
            // Add a block for recipient
            blockchain.addBlockAndReturn("Message",
                senderKeyPair.getPrivate(), senderKeyPair.getPublic(), recipientPublicKey);

            // Count blocks for non-existent recipient
            String fakePublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEFAKE000000000000000";
            long count = blockchain.countBlocksByRecipientPublicKey(fakePublicKey);

            assertEquals(0, count, "Should return zero count for non-existent recipient");
        }

        @Test
        @DisplayName("Should include recipient public key in hash calculation")
        void shouldIncludeRecipientPublicKeyInHashCalculation() {
            // Add two blocks with same content but different recipients
            Block block1 = blockchain.addBlockAndReturn("Same content",
                senderKeyPair.getPrivate(), senderKeyPair.getPublic(), recipientPublicKey);
            Block block2 = blockchain.addBlockAndReturn("Same content",
                senderKeyPair.getPrivate(), senderKeyPair.getPublic(), otherPublicKey);

            // Hashes should be different because recipientPublicKey is part of hash
            assertNotEquals(block1.getHash(), block2.getHash(),
                "Hashes should differ when recipient public keys differ");
        }

        @Test
        @DisplayName("Should throw exception for null recipient public key in count")
        void shouldThrowExceptionForNullRecipientPublicKeyInCount() {
            assertThrows(IllegalArgumentException.class, () -> {
                blockchain.countBlocksByRecipientPublicKey(null);
            }, "Should throw exception for null recipient public key");
        }

        @Test
        @DisplayName("Should throw exception for empty recipient public key in count")
        void shouldThrowExceptionForEmptyRecipientPublicKeyInCount() {
            assertThrows(IllegalArgumentException.class, () -> {
                blockchain.countBlocksByRecipientPublicKey("");
            }, "Should throw exception for empty recipient public key");
        }

        /**
         * BUG REPRODUCTION TEST: Blocks with recipient public key should validate correctly.
         *
         * <p>This test reproduces a bug where blocks created with recipientPublicKey fail validation.
         * The issue is in buildBlockContent() which incorrectly uses encryptionMetadata (JSON) instead
         * of the encrypted data for hash calculation when the block is marked as encrypted.</p>
         *
         * <p><strong>Expected behavior:</strong> Blocks with recipientPublicKey should pass validation.</p>
         * <p><strong>Bug:</strong> After createRecipientEncryptedBlock() sets encryptionMetadata to a JSON string,
         * validateBlock() fails because buildBlockContent() uses the JSON for hash calculation instead of
         * the actual encrypted data.</p>
         */
        @Test
        @DisplayName("BUG: Blocks with recipient public key should validate correctly")
        void shouldValidateBlocksWithRecipientPublicKey() {
            // Add block with recipient public key
            Block block = blockchain.addBlockAndReturn(
                "Secret message for recipient",
                senderKeyPair.getPrivate(),
                senderKeyPair.getPublic(),
                recipientPublicKey
            );

            assertNotNull(block, "Block should be created");
            assertEquals(2, blockchain.getBlockCount(), "Should have 2 blocks (Genesis + new block)");
            assertEquals(recipientPublicKey, block.getRecipientPublicKey(),
                "Recipient public key should be set");

            // CRITICAL: Validate the blockchain - this should pass but fails due to the bug
            ChainValidationResult result = blockchain.validateChainDetailed();

            // This assertion FAILS before the bug fix (blocks don't validate)
            // and PASSES after the bug fix
            assertTrue(result.isStructurallyIntact(),
                "Blockchain with recipient block should be structurally intact. " +
                "Bug: validateBlock() fails for blocks with recipientPublicKey because " +
                "buildBlockContent() uses encryptionMetadata (JSON) instead of encrypted data for hash. " +
                "Block data: [" + block.getData() + "], " +
                "isEncrypted: [" + block.isDataEncrypted() + "], " +
                "encryptionMetadata: [" + block.getEncryptionMetadata() + "]");
            assertTrue(result.isFullyCompliant(),
                "Blockchain with recipient block should be fully compliant. " +
                "Bug: encryptionMetadata contains JSON metadata, not encrypted data, " +
                "causing hash validation to fail.");
        }
    }

    @Nested
    @DisplayName("Accessible Blocks Filtering Tests")
    class AccessibleBlocksFiltering {

        @BeforeEach
        void setUpAccessibleBlocksFiltering() {
            // Authorize testKeyPair so tests can add blocks (using unique owner name for this nested class)
            blockchain.addAuthorizedKey(testPublicKey, "Default Test User", adminKeyPair, UserRole.USER);
        }

        @Test
        @DisplayName("Should get accessible blocks (public + created by user)")
        void shouldGetAccessibleBlocks() {
            KeyPair userKeyPair = CryptoUtil.generateKeyPair();
            String publicKey = CryptoUtil.publicKeyToString(userKeyPair.getPublic());

            // Add authorized key for user (unique name)
            blockchain.addAuthorizedKey(publicKey, "Accessible Test User", adminKeyPair, UserRole.USER);

            // Add public block (accessible to everyone)
            blockchain.addBlock("Public data", testKeyPair.getPrivate(), testKeyPair.getPublic());

            // Add block created by user
            blockchain.addBlock("Created by user", userKeyPair.getPrivate(), userKeyPair.getPublic());

            // Add encrypted block (isEncrypted=true, also accessible to everyone via password)
            blockchain.addEncryptedBlock("Encrypted data", "Password123!", testKeyPair.getPrivate(), testKeyPair.getPublic());

            // Get accessible blocks
            List<Block> accessible = blockchain.getAccessibleBlocks(publicKey);

            // Should include public blocks, blocks created by user, and encrypted blocks
            assertTrue(accessible.size() >= 2, "Should get accessible blocks");
        }

        @Test
        @DisplayName("Should get blocks created by user in accessible blocks")
        void shouldGetBlocksCreatedByUserInAccessibleBlocks() {
            KeyPair userKeyPair = CryptoUtil.generateKeyPair();
            String publicKey = CryptoUtil.publicKeyToString(userKeyPair.getPublic());

            // Add authorized key (unique name)
            blockchain.addAuthorizedKey(publicKey, "Creator User", adminKeyPair, UserRole.USER);

            // Add block created by user
            blockchain.addBlock("Created by user", userKeyPair.getPrivate(), userKeyPair.getPublic());

            // Add another block created by someone else
            blockchain.addBlock("By someone else", testKeyPair.getPrivate(), testKeyPair.getPublic());

            List<Block> accessible = blockchain.getAccessibleBlocks(publicKey);

            assertTrue(accessible.stream().anyMatch(b -> publicKey.equals(b.getSignerPublicKey())),
                "Should include blocks created by user");
        }

        @Test
        @DisplayName("Should exclude genesis block from accessible blocks")
        void shouldExcludeGenesisBlockFromAccessibleBlocks() {
            KeyPair userKeyPair = CryptoUtil.generateKeyPair();
            String publicKey = CryptoUtil.publicKeyToString(userKeyPair.getPublic());

            blockchain.addAuthorizedKey(publicKey, "Genesis Exclusion User", adminKeyPair, UserRole.USER);

            // Add regular block
            blockchain.addBlock("Regular block", userKeyPair.getPrivate(), userKeyPair.getPublic());

            List<Block> accessible = blockchain.getAccessibleBlocks(publicKey);

            // Genesis block (blockNumber=0) should be excluded
            assertTrue(accessible.stream().noneMatch(b -> b.getBlockNumber() != null && b.getBlockNumber() == 0L),
                "Genesis block should be excluded from accessible blocks");
        }

        @Test
        @DisplayName("Should respect maxResults limit in accessible blocks")
        void shouldRespectMaxResultsLimitInAccessibleBlocks() {
            KeyPair userKeyPair = CryptoUtil.generateKeyPair();
            String publicKey = CryptoUtil.publicKeyToString(userKeyPair.getPublic());

            blockchain.addAuthorizedKey(publicKey, "Max Results User", adminKeyPair, UserRole.USER);

            // Add many blocks
            for (int i = 1; i <= 10; i++) {
                blockchain.addBlock("Block " + i, testKeyPair.getPrivate(), testKeyPair.getPublic());
            }

            // Request only 5
            List<Block> accessible = blockchain.getAccessibleBlocks(publicKey, 5);

            assertEquals(5, accessible.size(), "Should respect maxResults limit");
        }

        @Test
        @DisplayName("Should throw exception for invalid maxResults in accessible blocks")
        void shouldThrowExceptionForInvalidMaxResultsInAccessibleBlocks() {
            String publicKey = testPublicKey;

            // Negative limit
            assertThrows(IllegalArgumentException.class, () -> {
                blockchain.getAccessibleBlocks(publicKey, -1);
            }, "Should throw exception for negative maxResults");

            // Zero limit
            assertThrows(IllegalArgumentException.class, () -> {
                blockchain.getAccessibleBlocks(publicKey, 0);
            }, "Should throw exception for zero maxResults");

            // Exceeds maximum
            assertThrows(IllegalArgumentException.class, () -> {
                blockchain.getAccessibleBlocks(publicKey, MemorySafetyConstants.DEFAULT_MAX_SEARCH_RESULTS + 1);
            }, "Should throw exception for maxResults exceeding limit");
        }

        @Test
        @DisplayName("Should throw exception for null public key in accessible blocks")
        void shouldThrowExceptionForNullPublicKeyInAccessibleBlocks() {
            assertThrows(IllegalArgumentException.class, () -> {
                blockchain.getAccessibleBlocks(null);
            }, "Should throw exception for null public key");
        }

        @Test
        @DisplayName("Should throw exception for empty public key in accessible blocks")
        void shouldThrowExceptionForEmptyPublicKeyInAccessibleBlocks() {
            assertThrows(IllegalArgumentException.class, () -> {
                blockchain.getAccessibleBlocks("  ");
            }, "Should throw exception for empty/whitespace public key");
        }

        @Test
        @DisplayName("Should get blocks by encryption status (public)")
        void shouldGetBlocksByEncryptionStatus() {
            // Add public blocks (genesis is public, and we add more)
            blockchain.addBlock("Public block 1", testKeyPair.getPrivate(), testKeyPair.getPublic());
            blockchain.addBlock("Public block 2", testKeyPair.getPrivate(), testKeyPair.getPublic());

            // Get public blocks
            List<Block> publicBlocks = blockchain.getBlocksByIsEncrypted(false);

            assertTrue(publicBlocks.size() >= 3, "Should get at least 3 public blocks (genesis + 2 added)");
            assertTrue(publicBlocks.stream().allMatch(b -> !b.getIsEncrypted()),
                "All returned blocks should be public");
        }

        @Test
        @DisplayName("Should get encrypted blocks by encryption status")
        void shouldGetEncryptedBlocksByEncryptionStatus() {
            // Add encrypted blocks
            blockchain.addEncryptedBlock("Encrypted 1", "Password1!", testKeyPair.getPrivate(), testKeyPair.getPublic());
            blockchain.addEncryptedBlock("Encrypted 2", "Password2!", testKeyPair.getPrivate(), testKeyPair.getPublic());

            // Add public block
            blockchain.addBlock("Public block", testKeyPair.getPrivate(), testKeyPair.getPublic());

            // Get encrypted blocks
            List<Block> encryptedBlocks = blockchain.getBlocksByIsEncrypted(true);

            assertTrue(encryptedBlocks.size() >= 2, "Should get at least 2 encrypted blocks");
            assertTrue(encryptedBlocks.stream().allMatch(b -> b.getIsEncrypted()),
                "All returned blocks should be encrypted");
        }

        @Test
        @DisplayName("Should respect maxResults limit in blocks by encryption status")
        void shouldRespectMaxResultsLimitInBlocksByEncryptionStatus() {
            // Add many public blocks
            for (int i = 1; i <= 10; i++) {
                blockchain.addBlock("Public block " + i, testKeyPair.getPrivate(), testKeyPair.getPublic());
            }

            // Request only 5
            List<Block> publicBlocks = blockchain.getBlocksByIsEncrypted(false, 5);

            assertEquals(5, publicBlocks.size(), "Should respect maxResults limit");
        }

        @Test
        @DisplayName("Should return empty list when no encrypted blocks exist")
        void shouldReturnEmptyListWhenNoEncryptedBlocksExist() {
            // Clear database - only genesis exists (public)
            blockchain.clearAndReinitialize();

            // Add only public blocks
            blockchain.addBlock("Public block", testKeyPair.getPrivate(), testKeyPair.getPublic());

            // Get encrypted blocks
            List<Block> encryptedBlocks = blockchain.getBlocksByIsEncrypted(true);

            assertNotNull(encryptedBlocks, "Should return empty list, not null");
            assertTrue(encryptedBlocks.isEmpty(), "Should return empty list when no encrypted blocks");
        }

        @Test
        @DisplayName("Should handle large number of public blocks efficiently")
        void shouldHandleLargeNumberOfPublicBlocksEfficiently() {
            KeyPair userKeyPair = CryptoUtil.generateKeyPair();
            String publicKey = CryptoUtil.publicKeyToString(userKeyPair.getPublic());

            blockchain.addAuthorizedKey(publicKey, "Performance Test User", adminKeyPair, UserRole.USER);

            // Add many public blocks (test performance)
            int blockCount = 100;
            for (int i = 1; i <= blockCount; i++) {
                blockchain.addBlock("Public block " + i, testKeyPair.getPrivate(), testKeyPair.getPublic());
            }

            long startTime = System.nanoTime();
            List<Block> accessible = blockchain.getAccessibleBlocks(publicKey);
            long duration = System.nanoTime() - startTime;

            // Should be efficient (O(1) query, not O(n) iteration)
            assertTrue(accessible.size() >= blockCount,
                "Should retrieve all accessible blocks");
            assertTrue(duration < 1_000_000_000L, // Less than 1 second
                "Query should be efficient (indexed lookup, not full scan), took: " + duration / 1_000_000 + "ms");
        }
    }

    @Nested
    @DisplayName("UserFriendlyEncryptionAPI Recipient Encryption Tests")
    class UserFriendlyEncryptionAPIRecipientTests {

        private UserFriendlyEncryptionAPI api;

        @BeforeEach
        void setUpApiTests() {
            api = new UserFriendlyEncryptionAPI(blockchain);
        }

        /**
         * BUG REPRODUCTION TEST: Recipient-encrypted blocks via UserFriendlyEncryptionAPI should validate correctly.
         *
         * <p>This test reproduces the actual bug where blocks created with recipientUsername via
         * UserFriendlyEncryptionAPI fail validation. The issue is in createRecipientEncryptedBlock()
         * which sets encryptionMetadata to a JSON string instead of the encrypted data.</p>
         *
         * <p><strong>Expected behavior:</strong> Recipient-encrypted blocks should pass validation.</p>
         * <p><strong>Bug:</strong> createRecipientEncryptedBlock() sets encryptionMetadata to JSON metadata
         * (e.g., {"type":"RECIPIENT_ENCRYPTED","recipient":"username"}), but buildBlockContent() expects
         * encryptionMetadata to contain the actual encrypted data for hash calculation.</p>
         */
        @Test
        @DisplayName("BUG: Recipient-encrypted blocks via API should validate correctly")
        void shouldValidateRecipientEncryptedBlocksCreatedViaAPI() {
            // Arrange: Create sender and recipient users
            KeyPair senderKeyPair = CryptoUtil.generateKeyPair();
            String senderPublicKey = CryptoUtil.publicKeyToString(senderKeyPair.getPublic());

            KeyPair recipientKeyPair = CryptoUtil.generateKeyPair();
            String recipientPublicKey = CryptoUtil.publicKeyToString(recipientKeyPair.getPublic());

            // Authorize both users
            assertTrue(blockchain.addAuthorizedKey(senderPublicKey, "Sender", adminKeyPair, UserRole.USER),
                "Sender authorization should succeed");
            assertTrue(blockchain.addAuthorizedKey(recipientPublicKey, "Recipient", adminKeyPair, UserRole.USER),
                "Recipient authorization should succeed");

            // Act: Create recipient-encrypted block using UserFriendlyEncryptionAPI
            String testContent = "Secret message for recipient via API";
            UserFriendlyEncryptionAPI.BlockCreationOptions options =
                new UserFriendlyEncryptionAPI.BlockCreationOptions()
                    .withEncryption(true)
                    .withRecipient("Recipient");  // This triggers createRecipientEncryptedBlock()

            Block createdBlock = api.createBlockWithExistingUser(
                testContent,
                senderKeyPair,
                options
            );

            // Assert: Block was created
            assertNotNull(createdBlock, "Block should be created");
            assertEquals(2, blockchain.getBlockCount(), "Should have 2 blocks (Genesis + new block)");
            assertEquals(recipientPublicKey, createdBlock.getRecipientPublicKey(),
                "Recipient public key should be set");

            // CRITICAL: Validate the blockchain - this FAILS due to the bug
            ChainValidationResult result = blockchain.validateChainDetailed();

            // This assertion FAILS before the bug fix:
            // validateBlock() fails because createRecipientEncryptedBlock() sets encryptionMetadata
            // to JSON (e.g., {"type":"RECIPIENT_ENCRYPTED","recipient":"Recipient"})
            // but buildBlockContent() expects encryptionMetadata to contain encrypted data for hash
            assertTrue(result.isStructurallyIntact(),
                "Blockchain with recipient-encrypted block should be structurally intact. " +
                "BUG: encryptionMetadata contains JSON metadata, not encrypted data. " +
                "Block data: [" + createdBlock.getData() + "], " +
                "isEncrypted: [" + createdBlock.isDataEncrypted() + "], " +
                "encryptionMetadata: [" + createdBlock.getEncryptionMetadata() + "]");
            assertTrue(result.isFullyCompliant(),
                "Blockchain should be fully compliant. " +
                "BUG: Hash validation fails because buildBlockContent() uses JSON for hash instead of encrypted data.");
        }
    }

}
