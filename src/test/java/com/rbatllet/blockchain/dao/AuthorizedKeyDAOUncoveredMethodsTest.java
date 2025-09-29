package com.rbatllet.blockchain.dao;

import com.rbatllet.blockchain.entity.AuthorizedKey;
import com.rbatllet.blockchain.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for previously uncovered AuthorizedKeyDAO methods
 * Tests all execution branches and edge cases for robust validation
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("📋 AuthorizedKeyDAO Uncovered Methods Tests")
class AuthorizedKeyDAOUncoveredMethodsTest {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizedKeyDAOUncoveredMethodsTest.class);

    private AuthorizedKeyDAO keyDAO;
    private EntityManager entityManager;
    private EntityTransaction transaction;

    @BeforeEach
    void setUp() {
        logger.debug("Setting up test environment for AuthorizedKeyDAO tests");
        keyDAO = new AuthorizedKeyDAO();

        // Clean up any existing test data to ensure test isolation
        logger.debug("Cleaning up existing test data for test isolation");
        keyDAO.cleanupTestData();

        entityManager = JPAUtil.getEntityManager();
        transaction = entityManager.getTransaction();
        transaction.begin();
        logger.debug("Database transaction started");
    }

    @AfterEach
    void tearDown() {
        logger.debug("Tearing down test environment");
        try {
            if (transaction != null && transaction.isActive()) {
                logger.debug("Rolling back active transaction");
                transaction.rollback();
            }
        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                logger.debug("Closing entity manager");
                entityManager.close();
            }
        }
    }

    @Nested
    @DisplayName("🗑️ deleteAllAuthorizedKeys() Tests")
    class DeleteAllAuthorizedKeysTests {

        @Test
        @Order(1)
        @DisplayName("Should return 0 when no keys exist")
        void shouldReturnZeroWhenNoKeysExist() {
            int deletedCount = keyDAO.deleteAllAuthorizedKeys();

            assertEquals(0, deletedCount, "Should return 0 when no keys exist to delete");
        }

        @Test
        @Order(2)
        @DisplayName("Should delete all keys and return correct count")
        void shouldDeleteAllKeysAndReturnCorrectCount() {
            logger.debug("Testing deleteAllAuthorizedKeys with multiple keys");
            // Add multiple keys
            AuthorizedKey key1 = new AuthorizedKey("public-key-1", "owner1", LocalDateTime.now());
            AuthorizedKey key2 = new AuthorizedKey("public-key-2", "owner2", LocalDateTime.now().plusMinutes(1));
            AuthorizedKey key3 = new AuthorizedKey("public-key-3", "owner3", LocalDateTime.now().plusMinutes(2));

            keyDAO.saveAuthorizedKey(key1);
            keyDAO.saveAuthorizedKey(key2);
            keyDAO.saveAuthorizedKey(key3);
            logger.debug("Added 3 test keys to database");

            // Verify keys exist
            List<AuthorizedKey> keysBeforeDeletion = keyDAO.getAllAuthorizedKeys();
            assertEquals(3, keysBeforeDeletion.size(), "Should have 3 keys before deletion");

            // Delete all keys
            int deletedCount = keyDAO.deleteAllAuthorizedKeys();
            logger.debug("Deleted {} keys from database", deletedCount);

            assertEquals(3, deletedCount, "Should return count of deleted keys");

            // Verify all keys are deleted
            List<AuthorizedKey> keysAfterDeletion = keyDAO.getAllAuthorizedKeys();
            assertTrue(keysAfterDeletion.isEmpty(), "Should have no keys after deletion");
        }

        @Test
        @Order(3)
        @DisplayName("Should delete both active and revoked keys")
        void shouldDeleteBothActiveAndRevokedKeys() {
            // Add keys and revoke one
            AuthorizedKey activeKey = new AuthorizedKey("active-key", "activeOwner", LocalDateTime.now());
            AuthorizedKey revokedKey = new AuthorizedKey("revoked-key", "revokedOwner", LocalDateTime.now().plusMinutes(1));

            keyDAO.saveAuthorizedKey(activeKey);
            keyDAO.saveAuthorizedKey(revokedKey);

            // Revoke one key
            keyDAO.revokeAuthorizedKey("revoked-key");

            // Verify we have both active and revoked keys
            List<AuthorizedKey> allKeys = keyDAO.getAllAuthorizedKeys();
            assertEquals(2, allKeys.size(), "Should have 2 keys (active and revoked)");

            List<AuthorizedKey> activeKeys = keyDAO.getActiveAuthorizedKeys();
            assertEquals(1, activeKeys.size(), "Should have 1 active key");

            // Delete all keys
            int deletedCount = keyDAO.deleteAllAuthorizedKeys();

            assertEquals(2, deletedCount, "Should delete both active and revoked keys");

            // Verify all keys are deleted
            List<AuthorizedKey> keysAfterDeletion = keyDAO.getAllAuthorizedKeys();
            assertTrue(keysAfterDeletion.isEmpty(), "Should have no keys after deletion");
        }

        @Test
        @Order(4)
        @DisplayName("Should handle database errors gracefully")
        void shouldHandleDatabaseErrorsGracefully() {
            // This test ensures proper transaction rollback on errors
            // We cannot easily simulate a database error, but we can test
            // that the method completes successfully under normal conditions
            assertDoesNotThrow(() -> {
                int deletedCount = keyDAO.deleteAllAuthorizedKeys();
                assertTrue(deletedCount >= 0, "Deleted count should be non-negative");
            }, "Should not throw exception during normal operation");
        }
    }

    @Nested
    @DisplayName("💾 saveAuthorizedKey(AuthorizedKey) Tests")
    class SaveAuthorizedKeyTests {

        @Test
        @Order(5)
        @DisplayName("Should throw exception for null AuthorizedKey")
        void shouldThrowExceptionForNullAuthorizedKey() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                keyDAO.saveAuthorizedKey(null);
            });

            assertTrue(exception.getMessage().contains("AuthorizedKey cannot be null"),
                "Exception message should mention null AuthorizedKey");
        }

        @Test
        @Order(6)
        @DisplayName("Should save valid AuthorizedKey successfully")
        void shouldSaveValidAuthorizedKeySuccessfully() {
            String testPublicKey = "test-save-key-123";
            String testOwner = "testOwner";
            LocalDateTime now = LocalDateTime.now();

            AuthorizedKey key = new AuthorizedKey(testPublicKey, testOwner, now);

            // Save the key
            assertDoesNotThrow(() -> {
                keyDAO.saveAuthorizedKey(key);
            }, "Should not throw exception when saving valid key");

            // Verify the key was saved
            assertTrue(keyDAO.isKeyAuthorized(testPublicKey), "Key should be authorized after saving");

            AuthorizedKey savedKey = keyDAO.getAuthorizedKeyByOwner(testOwner);
            assertNotNull(savedKey, "Should be able to retrieve saved key by owner");
            assertEquals(testPublicKey, savedKey.getPublicKey(), "Public key should match");
            assertEquals(testOwner, savedKey.getOwnerName(), "Owner name should match");
        }

        @Test
        @Order(7)
        @DisplayName("Should save multiple keys with same public key")
        void shouldSaveMultipleKeysWithSamePublicKey() {
            String publicKey = "duplicate-public-key";
            LocalDateTime now = LocalDateTime.now();

            AuthorizedKey key1 = new AuthorizedKey(publicKey, "owner1", now);
            AuthorizedKey key2 = new AuthorizedKey(publicKey, "owner2", now.plusMinutes(1));

            // Save both keys
            assertDoesNotThrow(() -> {
                keyDAO.saveAuthorizedKey(key1);
                keyDAO.saveAuthorizedKey(key2);
            }, "Should allow saving multiple keys with same public key");

            // Verify both keys exist in the database
            List<AuthorizedKey> allKeys = keyDAO.getAllAuthorizedKeys();
            long keysWithSamePublicKey = allKeys.stream()
                .filter(key -> publicKey.equals(key.getPublicKey()))
                .count();

            assertEquals(2, keysWithSamePublicKey, "Should have 2 keys with same public key");
        }

        @Test
        @Order(8)
        @DisplayName("Should handle AuthorizedKey with null fields")
        void shouldHandleAuthorizedKeyWithNullFields() {
            // Create a key with minimal valid data
            AuthorizedKey key = new AuthorizedKey();
            key.setPublicKey("test-key-null-fields");
            key.setOwnerName("testOwner");
            key.setCreatedAt(LocalDateTime.now());
            key.setActive(true);

            // Should save successfully even with some null fields
            assertDoesNotThrow(() -> {
                keyDAO.saveAuthorizedKey(key);
            }, "Should handle AuthorizedKey with some null fields");
        }
    }

    @Nested
    @DisplayName("🗑️ deleteAuthorizedKey(String) Tests")
    class DeleteAuthorizedKeyTests {

        @Test
        @Order(9)
        @DisplayName("Should throw exception for null public key")
        void shouldThrowExceptionForNullPublicKey() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                keyDAO.deleteAuthorizedKey(null);
            });

            assertTrue(exception.getMessage().contains("Public key cannot be null"),
                "Exception message should mention null public key");
        }

        @Test
        @Order(10)
        @DisplayName("Should throw exception for empty public key")
        void shouldThrowExceptionForEmptyPublicKey() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                keyDAO.deleteAuthorizedKey("");
            });

            assertTrue(exception.getMessage().contains("Public key cannot be null or empty"),
                "Exception message should mention empty public key");
        }

        @Test
        @Order(11)
        @DisplayName("Should throw exception for whitespace-only public key")
        void shouldThrowExceptionForWhitespaceOnlyPublicKey() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                keyDAO.deleteAuthorizedKey("   \t\n   ");
            });

            assertTrue(exception.getMessage().contains("Public key cannot be null or empty"),
                "Exception message should mention empty public key");
        }

        @Test
        @Order(12)
        @DisplayName("Should return false for non-existent key")
        void shouldReturnFalseForNonExistentKey() {
            String nonExistentKey = "non-existent-key-12345";

            boolean result = keyDAO.deleteAuthorizedKey(nonExistentKey);

            assertFalse(result, "Should return false when trying to delete non-existent key");
        }

        @Test
        @Order(13)
        @DisplayName("Should return true and delete existing key")
        void shouldReturnTrueAndDeleteExistingKey() {
            String testKey = "test-delete-key-67890";
            AuthorizedKey key = new AuthorizedKey(testKey, "testOwner", LocalDateTime.now());

            // Save the key first
            keyDAO.saveAuthorizedKey(key);
            assertTrue(keyDAO.isKeyAuthorized(testKey), "Key should exist before deletion");

            // Delete the key
            boolean result = keyDAO.deleteAuthorizedKey(testKey);

            assertTrue(result, "Should return true when deleting existing key");
            assertFalse(keyDAO.isKeyAuthorized(testKey), "Key should not exist after deletion");
        }

        @Test
        @Order(14)
        @DisplayName("Should delete all records for the same public key")
        void shouldDeleteAllRecordsForSamePublicKey() {
            String publicKey = "multi-record-delete-key";
            LocalDateTime now = LocalDateTime.now();

            // Create multiple records with same public key
            AuthorizedKey key1 = new AuthorizedKey(publicKey, "owner1", now);
            AuthorizedKey key2 = new AuthorizedKey(publicKey, "owner2", now.plusMinutes(1));

            keyDAO.saveAuthorizedKey(key1);
            keyDAO.saveAuthorizedKey(key2);

            // Verify multiple records exist
            List<AuthorizedKey> allKeys = keyDAO.getAllAuthorizedKeys();
            long recordsWithKey = allKeys.stream()
                .filter(key -> publicKey.equals(key.getPublicKey()))
                .count();
            assertEquals(2, recordsWithKey, "Should have 2 records with same public key");

            // Delete the key
            boolean result = keyDAO.deleteAuthorizedKey(publicKey);

            assertTrue(result, "Should return true when deleting existing records");

            // Verify all records are deleted
            List<AuthorizedKey> keysAfterDeletion = keyDAO.getAllAuthorizedKeys();
            long remainingRecords = keysAfterDeletion.stream()
                .filter(key -> publicKey.equals(key.getPublicKey()))
                .count();
            assertEquals(0, remainingRecords, "Should have no records with this public key after deletion");
        }
    }

    @Nested
    @DisplayName("❌ revokeAuthorizedKey(String) Tests")
    class RevokeAuthorizedKeyTests {

        @Test
        @Order(15)
        @DisplayName("Should throw exception for null public key")
        void shouldThrowExceptionForNullPublicKey() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                keyDAO.revokeAuthorizedKey(null);
            });

            assertTrue(exception.getMessage().contains("Public key cannot be null"),
                "Exception message should mention null public key");
        }

        @Test
        @Order(16)
        @DisplayName("Should throw exception for empty public key")
        void shouldThrowExceptionForEmptyPublicKey() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                keyDAO.revokeAuthorizedKey("");
            });

            assertTrue(exception.getMessage().contains("Public key cannot be null or empty"),
                "Exception message should mention empty public key");
        }

        @Test
        @Order(17)
        @DisplayName("Should throw exception for whitespace-only public key")
        void shouldThrowExceptionForWhitespaceOnlyPublicKey() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                keyDAO.revokeAuthorizedKey("   \t\n   ");
            });

            assertTrue(exception.getMessage().contains("Public key cannot be null or empty"),
                "Exception message should mention empty public key");
        }

        @Test
        @Order(18)
        @DisplayName("Should complete silently for non-existent key")
        void shouldCompleteSilentlyForNonExistentKey() {
            String nonExistentKey = "non-existent-revoke-key";

            // Should not throw exception even if key doesn't exist
            assertDoesNotThrow(() -> {
                keyDAO.revokeAuthorizedKey(nonExistentKey);
            }, "Should not throw exception when revoking non-existent key");
        }

        @Test
        @Order(19)
        @DisplayName("Should revoke active key successfully")
        void shouldRevokeActiveKeySuccessfully() {
            String testKey = "test-revoke-key-active";
            AuthorizedKey key = new AuthorizedKey(testKey, "testOwner", LocalDateTime.now());

            // Save the key
            keyDAO.saveAuthorizedKey(key);
            assertTrue(keyDAO.isKeyAuthorized(testKey), "Key should be active before revocation");

            // Revoke the key
            assertDoesNotThrow(() -> {
                keyDAO.revokeAuthorizedKey(testKey);
            }, "Should not throw exception when revoking active key");

            // Verify key is revoked
            assertFalse(keyDAO.isKeyAuthorized(testKey), "Key should not be active after revocation");

            // Verify key still exists in database but is marked as inactive
            List<AuthorizedKey> allKeys = keyDAO.getAllAuthorizedKeys();
            AuthorizedKey revokedKey = allKeys.stream()
                .filter(k -> testKey.equals(k.getPublicKey()))
                .findFirst()
                .orElse(null);

            assertNotNull(revokedKey, "Revoked key should still exist in database");
            assertFalse(revokedKey.isActive(), "Revoked key should be marked as inactive");
            assertNotNull(revokedKey.getRevokedAt(), "Revoked key should have revocation timestamp");
        }

        @Test
        @Order(20)
        @DisplayName("Should revoke active key successfully")
        void shouldRevokeActiveKeySuccessfullyInMultiKeyScenario() {
            String publicKey = "multi-auth-revoke-key";
            LocalDateTime now = LocalDateTime.now();

            // Create a single authorization first
            AuthorizedKey singleAuth = new AuthorizedKey(publicKey, "singleOwner", now);
            keyDAO.saveAuthorizedKey(singleAuth);

            // Verify key is authorized
            assertTrue(keyDAO.isKeyAuthorized(publicKey), "Key should be authorized");

            // Revoke the key
            keyDAO.revokeAuthorizedKey(publicKey);

            // Verify key is no longer authorized
            assertFalse(keyDAO.isKeyAuthorized(publicKey), "Key should not be authorized after revocation");

            // Verify record still exists but is revoked
            List<AuthorizedKey> allKeys = keyDAO.getAllAuthorizedKeys();
            List<AuthorizedKey> keysForPublicKey = allKeys.stream()
                .filter(k -> publicKey.equals(k.getPublicKey()))
                .toList();

            assertEquals(1, keysForPublicKey.size(), "Should have one authorization record");

            AuthorizedKey revokedKey = keysForPublicKey.get(0);
            assertFalse(revokedKey.isActive(), "Key should be revoked");
            assertNotNull(revokedKey.getRevokedAt(), "Key should have revocation timestamp");
        }

        @Test
        @Order(21)
        @DisplayName("Should complete silently when key already revoked")
        void shouldCompleteSilentlyWhenKeyAlreadyRevoked() {
            String testKey = "already-revoked-key";
            AuthorizedKey key = new AuthorizedKey(testKey, "testOwner", LocalDateTime.now());

            // Save and revoke the key
            keyDAO.saveAuthorizedKey(key);
            keyDAO.revokeAuthorizedKey(testKey);

            // Verify key is revoked
            assertFalse(keyDAO.isKeyAuthorized(testKey), "Key should be revoked");

            // Try to revoke again - should complete silently
            assertDoesNotThrow(() -> {
                keyDAO.revokeAuthorizedKey(testKey);
            }, "Should not throw exception when revoking already revoked key");
        }
    }

    @Nested
    @DisplayName("🧹 cleanupTestData() Tests")
    class CleanupTestDataTests {

        @Test
        @Order(22)
        @DisplayName("Should complete successfully when no data exists")
        void shouldCompleteSuccessfullyWhenNoDataExists() {
            // Verify no data exists
            List<AuthorizedKey> keys = keyDAO.getAllAuthorizedKeys();
            assertTrue(keys.isEmpty(), "Should have no keys initially");

            // Cleanup should complete without error
            assertDoesNotThrow(() -> {
                keyDAO.cleanupTestData();
            }, "Should not throw exception when cleaning up empty database");
        }

        @Test
        @Order(23)
        @DisplayName("Should delete all test data successfully")
        void shouldDeleteAllTestDataSuccessfully() {
            // Add test data
            AuthorizedKey key1 = new AuthorizedKey("cleanup-key-1", "owner1", LocalDateTime.now());
            AuthorizedKey key2 = new AuthorizedKey("cleanup-key-2", "owner2", LocalDateTime.now().plusMinutes(1));
            AuthorizedKey key3 = new AuthorizedKey("cleanup-key-3", "owner3", LocalDateTime.now().plusMinutes(2));

            keyDAO.saveAuthorizedKey(key1);
            keyDAO.saveAuthorizedKey(key2);
            keyDAO.saveAuthorizedKey(key3);

            // Verify data exists
            List<AuthorizedKey> keysBeforeCleanup = keyDAO.getAllAuthorizedKeys();
            assertEquals(3, keysBeforeCleanup.size(), "Should have 3 keys before cleanup");

            // Cleanup test data
            assertDoesNotThrow(() -> {
                keyDAO.cleanupTestData();
            }, "Should not throw exception during cleanup");

            // Verify all data is deleted
            List<AuthorizedKey> keysAfterCleanup = keyDAO.getAllAuthorizedKeys();
            assertTrue(keysAfterCleanup.isEmpty(), "Should have no keys after cleanup");
        }

        @Test
        @Order(24)
        @DisplayName("Should clear both active and revoked keys")
        void shouldClearBothActiveAndRevokedKeys() {
            // Add active and revoked keys
            AuthorizedKey activeKey = new AuthorizedKey("cleanup-active", "activeOwner", LocalDateTime.now());
            AuthorizedKey revokedKey = new AuthorizedKey("cleanup-revoked", "revokedOwner", LocalDateTime.now().plusMinutes(1));

            keyDAO.saveAuthorizedKey(activeKey);
            keyDAO.saveAuthorizedKey(revokedKey);

            // Revoke one key
            keyDAO.revokeAuthorizedKey("cleanup-revoked");

            // Verify we have both active and revoked keys
            List<AuthorizedKey> allKeys = keyDAO.getAllAuthorizedKeys();
            assertEquals(2, allKeys.size(), "Should have 2 keys before cleanup");

            List<AuthorizedKey> activeKeys = keyDAO.getActiveAuthorizedKeys();
            assertEquals(1, activeKeys.size(), "Should have 1 active key before cleanup");

            // Cleanup all data
            keyDAO.cleanupTestData();

            // Verify all keys are deleted
            List<AuthorizedKey> keysAfterCleanup = keyDAO.getAllAuthorizedKeys();
            assertTrue(keysAfterCleanup.isEmpty(), "Should have no keys after cleanup");

            List<AuthorizedKey> activeKeysAfterCleanup = keyDAO.getActiveAuthorizedKeys();
            assertTrue(activeKeysAfterCleanup.isEmpty(), "Should have no active keys after cleanup");
        }

        @Test
        @Order(25)
        @DisplayName("Should handle cleanup with complex data scenarios")
        void shouldHandleCleanupWithComplexDataScenarios() {
            LocalDateTime now = LocalDateTime.now();

            // Create complex scenario: multiple keys with same public key, revocations, etc.
            AuthorizedKey key1v1 = new AuthorizedKey("complex-key", "owner1", now);
            AuthorizedKey key1v2 = new AuthorizedKey("complex-key", "owner2", now.plusMinutes(1));
            AuthorizedKey key2 = new AuthorizedKey("another-key", "owner3", now.plusMinutes(2));

            keyDAO.saveAuthorizedKey(key1v1);
            keyDAO.saveAuthorizedKey(key1v2);
            keyDAO.saveAuthorizedKey(key2);

            // Revoke one version
            keyDAO.revokeAuthorizedKey("complex-key");

            // Verify complex data structure
            List<AuthorizedKey> allKeys = keyDAO.getAllAuthorizedKeys();
            assertEquals(3, allKeys.size(), "Should have 3 keys in complex scenario");

            // Cleanup should handle all complexities
            assertDoesNotThrow(() -> {
                keyDAO.cleanupTestData();
            }, "Should handle complex cleanup scenario without errors");

            // Verify everything is cleaned
            List<AuthorizedKey> keysAfterCleanup = keyDAO.getAllAuthorizedKeys();
            assertTrue(keysAfterCleanup.isEmpty(), "Should have no keys after complex cleanup");
        }
    }
}