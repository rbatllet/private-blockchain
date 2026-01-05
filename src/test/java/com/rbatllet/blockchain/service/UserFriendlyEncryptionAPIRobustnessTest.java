package com.rbatllet.blockchain.service;

import static org.junit.jupiter.api.Assertions.*;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.entity.OffChainData;
import com.rbatllet.blockchain.security.PasswordUtil;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;
import com.rbatllet.blockchain.util.CryptoUtil;
import java.security.KeyPair;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Robustness tests for UserFriendlyEncryptionAPI methods that lack adequate coverage.
 * This test validates error handling, edge cases, and general robustness of the methods.
 *
 * Tests focus on exposing weaknesses in implementation that could cause failures
 * in production environments.
 */
@DisplayName("UserFriendlyEncryptionAPI Robustness Tests")
public class UserFriendlyEncryptionAPIRobustnessTest {

    private UserFriendlyEncryptionAPI api;
    private Blockchain blockchain;
    private KeyPair testKeyPair;
    private KeyPair bootstrapKeyPair;
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "SecurePassword123!";

    @BeforeEach
    void setUp() throws Exception {
        // Create real blockchain for integration testing
        blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        // Load bootstrap admin keys (auto-generates if missing - test-only)
        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();

        // Register bootstrap admin first (RBAC v1.0.6)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        testKeyPair = CryptoUtil.generateKeyPair();

        // SECURITY FIX (v1.0.6): Pre-authorize user before creating API
        String publicKeyString = CryptoUtil.publicKeyToString(testKeyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyString, TEST_USERNAME, bootstrapKeyPair, UserRole.ADMIN);

        api = new UserFriendlyEncryptionAPI(
            blockchain,
            TEST_USERNAME,
            testKeyPair
        );
    }

    @AfterEach
    void tearDown() {
        if (blockchain != null) {
            blockchain.shutdown();
        }
    }

    @Nested
    @DisplayName("Password Management Robustness")
    class PasswordManagementRobustnessTest {

        @Test
        @DisplayName(
            "readPasswordSecurely should handle null prompt gracefully"
        )
        void testReadPasswordSecurelyWithNullPrompt() {
            try (
                MockedStatic<PasswordUtil> mockedPasswordUtil =
                    Mockito.mockStatic(PasswordUtil.class)
            ) {
                mockedPasswordUtil
                    .when(() -> PasswordUtil.readPassword(null))
                    .thenThrow(
                        new IllegalArgumentException("Prompt cannot be null")
                    );

                // The method should propagate the exception since it doesn't handle nulls
                assertThrows(IllegalArgumentException.class, () -> {
                    api.readPasswordSecurely(null);
                });
            }
        }

        @Test
        @DisplayName("readPasswordSecurely should handle empty prompt")
        void testReadPasswordSecurelyWithEmptyPrompt() {
            try (
                MockedStatic<PasswordUtil> mockedPasswordUtil =
                    Mockito.mockStatic(PasswordUtil.class)
            ) {
                mockedPasswordUtil
                    .when(() -> PasswordUtil.readPassword("Password: "))
                    .thenReturn("testPassword");

                String result = api.readPasswordSecurely("");
                assertEquals("testPassword", result);
            }
        }

        @Test
        @DisplayName(
            "readPasswordWithConfirmation should handle null prompt gracefully"
        )
        void testReadPasswordWithConfirmationWithNullPrompt() {
            try (
                MockedStatic<PasswordUtil> mockedPasswordUtil =
                    Mockito.mockStatic(PasswordUtil.class)
            ) {
                mockedPasswordUtil
                    .when(() -> PasswordUtil.readPasswordWithConfirmation(null))
                    .thenThrow(
                        new IllegalArgumentException("Prompt cannot be null")
                    );

                // The method should propagate the exception since it doesn't handle nulls
                assertThrows(IllegalArgumentException.class, () -> {
                    api.readPasswordWithConfirmation(null);
                });
            }
        }

        @Test
        @DisplayName(
            "readPasswordWithConfirmation should return null when passwords don't match"
        )
        void testReadPasswordWithConfirmationMismatch() {
            try (
                MockedStatic<PasswordUtil> mockedPasswordUtil =
                    Mockito.mockStatic(PasswordUtil.class)
            ) {
                mockedPasswordUtil
                    .when(() ->
                        PasswordUtil.readPasswordWithConfirmation(
                            "Enter password: "
                        )
                    )
                    .thenReturn(null); // Simulate passwords that don't match

                String result = api.readPasswordWithConfirmation(
                    "Enter password: "
                );
                assertNull(result);
            }
        }

        @Test
        @DisplayName(
            "readPasswordWithConfirmation should handle PasswordUtil exceptions"
        )
        void testReadPasswordWithConfirmationException() {
            try (
                MockedStatic<PasswordUtil> mockedPasswordUtil =
                    Mockito.mockStatic(PasswordUtil.class)
            ) {
                mockedPasswordUtil
                    .when(() ->
                        PasswordUtil.readPasswordWithConfirmation(
                            "Enter password: "
                        )
                    )
                    .thenThrow(new RuntimeException("Console not available"));

                // The method should propagate the exception since it doesn't handle errors
                assertThrows(RuntimeException.class, () -> {
                    api.readPasswordWithConfirmation("Enter password: ");
                });
            }
        }
    }

    @Nested
    @DisplayName("Cache Management Robustness")
    class CacheManagementRobustnessTest {

        @Test
        @DisplayName(
            "clearLowPerformingCacheEntries should not throw exceptions"
        )
        void testClearLowPerformingCacheEntriesRobustness() {
            // This method currently only does logging, but should be robust
            assertDoesNotThrow(() -> {
                // Use reflection to access private method
                var method = UserFriendlyEncryptionAPI.class.getDeclaredMethod(
                    "clearLowPerformingCacheEntries"
                );
                method.setAccessible(true);
                method.invoke(api);
            });
        }

        @Test
        @DisplayName(
            "warmUpCacheWithPopularSearches should handle search failures gracefully"
        )
        void testWarmUpCacheWithPopularSearchesRobustness() {
            assertDoesNotThrow(() -> {
                // Use reflection to access private method
                var method = UserFriendlyEncryptionAPI.class.getDeclaredMethod(
                    "warmUpCacheWithPopularSearches"
                );
                method.setAccessible(true);
                method.invoke(api);
            });
        }
    }

    @Nested
    @DisplayName("Large File Operations Robustness")
    class LargeFileOperationsRobustnessTest {

        @Test
        @DisplayName("getLargeFileSize should handle null OffChainData")
        void testGetLargeFileSizeWithNull() {
            long result = api.getLargeFileSize(null);
            assertEquals(-1, result, "Should return -1 for null OffChainData");
        }

        @Test
        @DisplayName("largeFileExists should handle null OffChainData")
        void testLargeFileExistsWithNull() {
            boolean result = api.largeFileExists(null);
            assertFalse(result, "Should return false for null OffChainData");
        }

        @Test
        @DisplayName("deleteLargeFile should handle null OffChainData")
        void testDeleteLargeFileWithNull() {
            boolean result = api.deleteLargeFile(null);
            assertFalse(result, "Should return false for null OffChainData");
        }

        @Test
        @DisplayName("Large file operations should handle valid OffChainData")
        void testLargeFileOperationsWithValidData() {
            // Create a test block with off-chain data to get OffChainData reference
            Block testBlock = api.storeSecret(
                "Test data for large file ops",
                TEST_PASSWORD
            );
            assertNotNull(testBlock);

            // Test with a mock OffChainData (since we can't easily create real off-chain files)
            OffChainData offChainData = new OffChainData(
                "test-hash",
                "test-signature",
                "test-file-path",
                1024L,
                "test-iv",
                "AAAAAAAAAAAAAAAAAAAAAA==",  // dummy salt
                "application/text",
                "test-public-key"
            );

            // These operations might fail due to missing actual files, but shouldn't crash
            assertDoesNotThrow(() -> {
                api.getLargeFileSize(offChainData);
                api.largeFileExists(offChainData);
                api.deleteLargeFile(offChainData);
            });
        }
    }

    @Nested
    @DisplayName("Block Metadata Robustness")
    class BlockMetadataRobustnessTest {

        @Test
        @DisplayName("getBlockMetadata should handle null Block")
        void testGetBlockMetadataWithNullBlock() {
            Map<String, String> result = api.getBlockMetadata(null);
            assertNotNull(result, "Should return non-null map");
            assertTrue(
                result.isEmpty(),
                "Should return empty map for null block"
            );
        }

        @Test
        @DisplayName("getBlockMetadata should handle Block with null metadata")
        void testGetBlockMetadataWithNullMetadata() {
            // Create a block and test metadata extraction
            Block testBlock = api.storeSecret("Test data", TEST_PASSWORD);
            assertNotNull(testBlock);

            Map<String, String> result = api.getBlockMetadata(testBlock);
            assertNotNull(
                result,
                "Should return non-null map even for block without custom metadata"
            );
        }

        @Test
        @DisplayName("getBlockMetadata should handle blocks with metadata")
        void testGetBlockMetadataWithValidMetadata() {
            // Create a block with metadata using the BlockCreationOptions
            UserFriendlyEncryptionAPI.BlockCreationOptions options =
                new UserFriendlyEncryptionAPI.BlockCreationOptions()
                    .withUsername("metadatauser")
                    .withPassword(TEST_PASSWORD)
                    .withMetadata("key1", "value1")
                    .withMetadata("key2", "value2");

            assertDoesNotThrow(() -> {
                Block testBlock = api.createBlockWithOptions(
                    "Test data",
                    options
                );
                if (testBlock != null) {
                    Map<String, String> metadata = api.getBlockMetadata(
                        testBlock
                    );
                    assertNotNull(metadata);
                }
            });
        }
    }

    @Nested
    @DisplayName("Cryptographic Operations Robustness")
    class CryptographicOperationsRobustnessTest {

        // EC-specific tests removed - ML-DSA uses lattice-based cryptography (no elliptic curves)
    }

    @Nested
    @DisplayName("Search Operations Robustness")
    class SearchOperationsRobustnessTest {

        @Test
        @DisplayName(
            "runSearchDiagnostics should handle API unavailability gracefully"
        )
        void testRunSearchDiagnosticsRobustness() {
            // This should not throw exceptions even if search API has issues
            assertDoesNotThrow(() -> {
                String result = api.runSearchDiagnostics();
                assertNotNull(result, "Should return a non-null result");
                assertFalse(
                    result.isEmpty(),
                    "Should return meaningful diagnostic information"
                );
            });
        }

        @Test
        @DisplayName(
            "getSearchAnalytics should handle API unavailability gracefully"
        )
        void testGetSearchAnalyticsRobustness() {
            // This should not throw exceptions even if search API has issues
            assertDoesNotThrow(() -> {
                String result = api.getSearchAnalytics();
                assertNotNull(result, "Should return a non-null result");
                assertFalse(
                    result.isEmpty(),
                    "Should return meaningful analytics information"
                );
            });
        }

        @Test
        @DisplayName("findBlockByHash should handle null hash")
        void testFindBlockByHashWithNull() throws Exception {
            var method = UserFriendlyEncryptionAPI.class.getDeclaredMethod(
                "findBlockByHash",
                String.class
            );
            method.setAccessible(true);

            Block result = (Block) method.invoke(api, (String) null);
            assertNull(result, "Should return null for null hash");
        }

        @Test
        @DisplayName("findBlockByHash should handle empty hash")
        void testFindBlockByHashWithEmptyString() throws Exception {
            var method = UserFriendlyEncryptionAPI.class.getDeclaredMethod(
                "findBlockByHash",
                String.class
            );
            method.setAccessible(true);

            Block result = (Block) method.invoke(api, "");
            assertNull(result, "Should return null for empty hash");
        }

        @Test
        @DisplayName("findBlockByHash should find existing blocks")
        void testFindBlockByHashWithValidHash() throws Exception {
            // Create a test block first
            Block testBlock = api.storeSecret(
                "Test data for hash search",
                TEST_PASSWORD
            );
            assertNotNull(testBlock);
            String blockHash = testBlock.getHash();

            var method = UserFriendlyEncryptionAPI.class.getDeclaredMethod(
                "findBlockByHash",
                String.class
            );
            method.setAccessible(true);

            Block foundBlock = (Block) method.invoke(api, blockHash);
            assertNotNull(foundBlock, "Should find the created block");
            assertEquals(
                blockHash,
                foundBlock.getHash(),
                "Should return block with matching hash"
            );
        }
    }

    @Nested
    @DisplayName("Data Validation Robustness")
    class DataValidationRobustnessTest {

        @Test
        @DisplayName("validateOffChainData should handle null blockNumber")
        void testValidateOffChainDataWithNullBlockNumber() {
            boolean result = api.validateOffChainData(null);
            assertFalse(result, "Should return false for null blockNumber");
        }

        @Test
        @DisplayName("validateOffChainData should handle non-existent blockNumber")
        void testValidateOffChainDataWithNonExistentBlockNumber() {
            boolean result = api.validateOffChainData(999999L);
            assertFalse(result, "Should return false for non-existent blockNumber");
        }

        @Test
        @DisplayName("validateOffChainData should handle existing blockNumber")
        void testValidateOffChainDataWithValidBlockNumber() {
            // Create a test block first
            Block testBlock = api.storeSecret(
                "Test data for validation",
                TEST_PASSWORD
            );
            assertNotNull(testBlock);

            // Test validation with the created block's ID
            assertDoesNotThrow(() -> {
                boolean result = api.validateOffChainData(
                    testBlock.getBlockNumber()
                );
                // The result might be false if there's no actual off-chain data, but it shouldn't crash
                assertNotNull(result);
            });
        }
    }

    @Nested
    @DisplayName("Block Repair Robustness")
    class BlockRepairRobustnessTest {

        @Test
        @DisplayName("identifyCorruptedBlocks should handle empty blockchain")
        void testIdentifyCorruptedBlocksWithEmptyBlockchain() throws Exception {
            // Create a fresh API with empty blockchain
            Blockchain emptyBlockchain = new Blockchain();

            // RBAC FIX (v1.0.6): Clear database and setup bootstrap admin
            emptyBlockchain.clearAndReinitialize();
            emptyBlockchain.createBootstrapAdmin(
                CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
                "BOOTSTRAP_ADMIN"
            );

            // Pre-authorize TEST_USERNAME for API creation
            String publicKeyString = CryptoUtil.publicKeyToString(testKeyPair.getPublic());
            emptyBlockchain.addAuthorizedKey(publicKeyString, TEST_USERNAME, bootstrapKeyPair, UserRole.ADMIN);

            UserFriendlyEncryptionAPI emptyApi = new UserFriendlyEncryptionAPI(
                emptyBlockchain,
                TEST_USERNAME,
                testKeyPair
            );

            var method = UserFriendlyEncryptionAPI.class.getDeclaredMethod(
                "identifyCorruptedBlocks"
            );
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<Long> result = (List<Long>) method.invoke(emptyApi);
            assertNotNull(result, "Should return non-null list");
            assertTrue(
                result.isEmpty(),
                "Should return empty list for empty blockchain"
            );
        }

        @Test
        @DisplayName("isBlockCorrupted should handle valid blocks")
        void testIsBlockCorruptedWithValidBlock() throws Exception {
            // Create a valid test block
            Block testBlock = api.storeSecret("Valid test data", TEST_PASSWORD);
            assertNotNull(testBlock);

            var method = UserFriendlyEncryptionAPI.class.getDeclaredMethod(
                "isBlockCorrupted",
                Block.class
            );
            method.setAccessible(true);

            boolean result = (boolean) method.invoke(api, testBlock);
            assertFalse(
                result,
                "Valid block should not be identified as corrupted"
            );
        }

        @Test
        @DisplayName("repairSingleBlock should handle non-existent block")
        void testRepairSingleBlockWithNonExistentBlock() throws Exception {
            var method = UserFriendlyEncryptionAPI.class.getDeclaredMethod(
                "repairSingleBlock",
                Long.class
            );
            method.setAccessible(true);

            boolean result = (boolean) method.invoke(api, 999999L);
            assertFalse(result, "Should return false for non-existent block");
        }

        @Test
        @DisplayName(
            "repairBlockLink should handle valid blocks that don't need repair"
        )
        void testRepairBlockLinkWithValidBlocks() throws Exception {
            // Create two connected blocks
            Block firstBlock = api.storeSecret("First block", TEST_PASSWORD);
            Block secondBlock = api.storeSecret("Second block", TEST_PASSWORD);

            assertNotNull(firstBlock);
            assertNotNull(secondBlock);

            var method = UserFriendlyEncryptionAPI.class.getDeclaredMethod(
                "repairBlockLink",
                Block.class,
                Block.class
            );
            method.setAccessible(true);

            // This should succeed or at least not crash
            assertDoesNotThrow(() -> {
                method.invoke(api, secondBlock, firstBlock);
            });
        }
    }

    @Nested
    @DisplayName("Storage Operations Robustness")
    class StorageOperationsRobustnessTest {

        @Test
        @DisplayName(
            "storeDataWithOffChainFile should handle null parameters gracefully"
        )
        void testStoreDataWithOffChainFileNullParams() {
            // These operations should handle null parameters gracefully or throw meaningful exceptions
            assertThrows(
                Exception.class,
                () -> {
                    api.storeDataWithOffChainFile(null, null, null, null, null);
                },
                "Should throw exception for null parameters"
            );
        }

        @Test
        @DisplayName(
            "storeDataWithOffChainText should handle null parameters gracefully"
        )
        void testStoreDataWithOffChainTextNullParams() {
            assertThrows(
                Exception.class,
                () -> {
                    api.storeDataWithOffChainText(null, null, null, null, null);
                },
                "Should throw exception for null parameters"
            );
        }

        @Test
        @DisplayName(
            "storeSearchableDataWithOffChainFile should handle null parameters gracefully"
        )
        void testStoreSearchableDataWithOffChainFileNullParams() {
            assertThrows(
                Exception.class,
                () -> {
                    api.storeSearchableDataWithOffChainFile(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                    );
                },
                "Should throw exception for null parameters"
            );
        }
    }

    @Nested
    @DisplayName("Search by Criteria Robustness")
    class SearchByCriteriaRobustnessTest {

        @Test
        @DisplayName("findBlocksByDateRange should handle null dates")
        void testFindBlocksByDateRangeWithNullDates() {
            List<Block> result = api.findBlocksByDateRange(null, null);
            assertNotNull(result, "Should return non-null list");
            assertTrue(
                result.isEmpty(),
                "Should return empty list for null dates"
            );
        }

        @Test
        @DisplayName("findBlocksByCategory should handle null category")
        void testFindBlocksByCategoryWithNullCategory() {
            List<Block> result = api.findBlocksByCategory(null);
            assertNotNull(result, "Should return non-null list");
            assertTrue(
                result.isEmpty(),
                "Should return empty list for null category"
            );
        }

        @Test
        @DisplayName("findBlocksByRecipient should handle null recipient")
        void testFindBlocksByRecipientWithNullRecipient() {
            List<Block> result = api.findBlocksByRecipient(null);
            assertNotNull(result, "Should return non-null list");
            assertTrue(
                result.isEmpty(),
                "Should return empty list for null recipient"
            );
        }

        @Test
        @DisplayName("findBlocksByMetadata should handle null parameters")
        void testFindBlocksByMetadataWithNullParams() {
            List<Block> result = api.findBlocksByMetadata(null, null);
            assertNotNull(result, "Should return non-null list");
            assertTrue(
                result.isEmpty(),
                "Should return empty list for null parameters"
            );
        }

        @Test
        @DisplayName("findBlocksByMetadataKeys should handle null set")
        void testFindBlocksByMetadataKeysWithNullSet() {
            List<Block> result = api.findBlocksByMetadataKeys(null);
            assertNotNull(result, "Should return non-null list");
            assertTrue(
                result.isEmpty(),
                "Should return empty list for null set"
            );
        }

        @Test
        @DisplayName("findBlocksByMetadataKeys should handle empty set")
        void testFindBlocksByMetadataKeysWithEmptySet() {
            List<Block> result = api.findBlocksByMetadataKeys(
                Collections.emptySet()
            );
            assertNotNull(result, "Should return non-null list");
            assertTrue(
                result.isEmpty(),
                "Should return empty list for empty set"
            );
        }
    }

    @Nested
    @DisplayName("Performance Operations Robustness")
    class PerformanceOperationsRobustnessTest {

        @Test
        @DisplayName("performQuickIntegrityCheck should handle null blockNumber")
        void testPerformQuickIntegrityCheckWithNull() throws Exception {
            var method = UserFriendlyEncryptionAPI.class.getDeclaredMethod(
                "performQuickIntegrityCheck",
                Long.class
            );
            method.setAccessible(true);

            boolean result = (boolean) method.invoke(api, (Long) null);
            assertFalse(result, "Should return false for null blockNumber");
        }



        @Test
        @DisplayName("getEncryptedBlocksOnly should handle null username")
        void testGetEncryptedBlocksOnlyWithNullUsername() {
            List<Block> result = api.getEncryptedBlocksOnly(null);
            assertNotNull(result, "Should return non-null list");
        }

        @Test
        @DisplayName("getEncryptedBlocksOnly should work with valid username")
        void testGetEncryptedBlocksOnlyWithValidUsername() {
            // Create some test blocks first
            api.storeSecret("Test encrypted data 1", TEST_PASSWORD);
            api.storeSecret("Test encrypted data 2", TEST_PASSWORD);

            List<Block> result = api.getEncryptedBlocksOnly(TEST_USERNAME);
            assertNotNull(result, "Should return non-null list");
        }
    }

    @Nested
    @DisplayName("Security Operations Robustness")
    class SecurityOperationsRobustnessTest {

        @Test
        @DisplayName("searchSecure should handle null parameters")
        void testSearchSecureWithNullParams() {
            List<Block> result = api.searchSecure(null, null);
            assertNotNull(result, "Should return non-null list");
            assertTrue(
                result.isEmpty(),
                "Should return empty list for null parameters"
            );
        }

        @Test
        @DisplayName("searchSecure should handle empty search query")
        void testSearchSecureWithEmptyQuery() {
            List<Block> result = api.searchSecure("", TEST_PASSWORD);
            assertNotNull(result, "Should return non-null list");
            assertTrue(
                result.isEmpty(),
                "Should return empty list for empty query"
            );
        }

        @Test
        @DisplayName("searchSecure should handle null password")
        void testSearchSecureWithNullPassword() {
            List<Block> result = api.searchSecure("test query", null);
            assertNotNull(result, "Should return non-null list");
            assertTrue(
                result.isEmpty(),
                "Should return empty list for null password"
            );
        }

        @Test
        @DisplayName("searchSecure should work with valid parameters")
        void testSearchSecureWithValidParams() {
            // Create some test data to search
            api.storeSecret(
                "Secret document with important information",
                TEST_PASSWORD
            );

            assertDoesNotThrow(() -> {
                List<Block> result = api.searchSecure(
                    "important",
                    TEST_PASSWORD
                );
                assertNotNull(result, "Should return non-null result");
            });
        }
    }

    @Nested
    @DisplayName("Integration Robustness Tests")
    class IntegrationRobustnessTest {

        @Test
        @DisplayName("API should maintain consistency under stress operations")
        void testAPIConsistencyUnderStress() {
            // Perform multiple operations that might cause state inconsistencies
            assertDoesNotThrow(
                () -> {
                    // Create multiple blocks
                    for (int i = 0; i < 10; i++) {
                        api.storeSecret("Stress test data " + i, TEST_PASSWORD);
                    }

                    // Run diagnostics
                    api.runSearchDiagnostics();
                    api.getSearchAnalytics();

                    // Perform searches
                    api.searchEverything("stress");
                    api.findBlocksByCategory("test");

                    // Check integrity
                    api.validateOffChainData(1L);

                    // Run analysis
                    api.analyzeEncryption();
                },
                "API should handle stress operations without crashing"
            );
        }

        @Test
        @DisplayName("Error conditions should not corrupt blockchain state")
        void testErrorConditionsDoNotCorruptState() {
            // Get initial blockchain state
            long initialSize = blockchain.getBlockCount();

            // Perform operations that might fail
            assertDoesNotThrow(() -> {
                try {
                    api.getLargeFileSize(null);
                    api.findBlocksByDateRange(null, null);
                    api.searchSecure(null, null);
                } catch (Exception e) {
                    // Expected - operations with null parameters might fail
                }

                // Blockchain should remain consistent
                long finalSize = blockchain.getBlockCount();
                assertEquals(
                    initialSize,
                    finalSize,
                    "Blockchain size should not change due to failed operations"
                );
            });
        }
    }
}
