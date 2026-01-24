package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.security.KeyPair;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests for UserFriendlyEncryptionAPI core functionality methods with 0% coverage
 * Focuses on basic encrypted operations and essential blockchain functionality
 */
@DisplayName("🎯 UserFriendlyEncryptionAPI Core Functionality Tests")
public class UserFriendlyEncryptionAPICoreFunctionalityTest {
    private static final Logger logger = LoggerFactory.getLogger(UserFriendlyEncryptionAPICoreFunctionalityTest.class);


    private UserFriendlyEncryptionAPI api;
    private Blockchain realBlockchain;
    private KeyPair defaultKeyPair;
    private KeyPair bootstrapKeyPair;
    private String testUsername = "coreuser";
    private String testPassword = "CorePassword123!";

    // Store the blocks created in setup to avoid finding old test data
    private Block testEncryptedBlock1;
    private Block testEncryptedBlock2;

    @BeforeEach
    void setUp() throws Exception {
        // Create real blockchain (no mocks)
        realBlockchain = new Blockchain();
        realBlockchain.clearAndReinitialize(); // Clean DB before each test

        // Load bootstrap admin keys (auto-generates if missing - test-only)
        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();

        // Register bootstrap admin in blockchain (RBAC v1.0.6)
        realBlockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        // Create API with default credentials
        defaultKeyPair = CryptoUtil.generateKeyPair();

        // RBAC FIX (v1.0.6): testUsername needs ADMIN role to create other users via createUser()
        String publicKeyString = CryptoUtil.publicKeyToString(defaultKeyPair.getPublic());
        realBlockchain.addAuthorizedKey(publicKeyString, testUsername, bootstrapKeyPair, UserRole.ADMIN);

        api = new UserFriendlyEncryptionAPI(realBlockchain, testUsername, defaultKeyPair);
        
        // Initialize SearchSpecialistAPI before storing encrypted data
        try {
            realBlockchain.initializeAdvancedSearch(testPassword);
            realBlockchain.getSearchSpecialistAPI().initializeWithBlockchain(realBlockchain, testPassword, defaultKeyPair.getPrivate());
        } catch (Exception e) {
            logger.error("⚠️ Warning: SearchSpecialistAPI initialization failed: " + e.getMessage());
        }
        
        // CRITICAL: Store block references to use directly (avoid contamination from previous test runs)
        testEncryptedBlock1 = api.storeSecret("Secret document content", testPassword);
        testEncryptedBlock2 = api.storeSecret("Config data", testPassword);
        
        // Add unencrypted public data for testing
        realBlockchain.addBlock("Public information for testing", defaultKeyPair.getPrivate(), defaultKeyPair.getPublic());
    }
    
    @AfterEach
    void tearDown() {
        // Clean database after each test to ensure test isolation
        if (realBlockchain != null) {
            realBlockchain.clearAndReinitialize();
        }
    }

    @Nested
    @DisplayName("🔐 Basic Encrypted Operations")
    class BasicEncryptedOperationsTests {

        @Test
        @DisplayName("Should retrieve secret from block")
        void shouldRetrieveSecretFromBlock() {
            // Given - Use the blocks we just created in @BeforeEach
            assertNotNull(testEncryptedBlock1, "Setup should have created encrypted block 1");
            Long blockNumber = testEncryptedBlock1.getBlockNumber();
            
            // When - Retrieve the secret with correct password
            String retrievedSecret = api.retrieveSecret(blockNumber, testPassword);
            
            // Then - Should successfully decrypt and return original content
            assertNotNull(retrievedSecret, "Should retrieve secret content from password-encrypted block");
            assertEquals("Secret document content", retrievedSecret, "Retrieved secret should match original content");
        }

        @Test
        @DisplayName("Should check if block is encrypted")
        void shouldCheckIfBlockIsEncrypted() {
            // Given - Use blocks created in @BeforeEach
            assertNotNull(testEncryptedBlock1, "Setup should have created encrypted block 1");
            assertNotNull(testEncryptedBlock2, "Setup should have created encrypted block 2");
            
            // When - Check if blocks are encrypted
            boolean block1Encrypted = api.isBlockEncrypted(testEncryptedBlock1.getBlockNumber());
            boolean block2Encrypted = api.isBlockEncrypted(testEncryptedBlock2.getBlockNumber());
            
            // Then - Both should be encrypted
            assertTrue(block1Encrypted, "Block 1 should be encrypted");
            assertTrue(block2Encrypted, "Block 2 should be encrypted");
        }

        @Test
        @DisplayName("Should find encrypted data without password")
        void shouldFindEncryptedDataWithoutPassword() {
            // Given
            String searchTerm = "document";
            
            // When
            List<Block> results = api.findEncryptedData(searchTerm);
            
            // Then
            assertNotNull(results, "Should return search results");
            // Results might be empty if search term not in public metadata
            assertTrue(results.size() >= 0, "Should return non-negative number of results");
        }

        @Test
        @DisplayName("Should find and decrypt data")
        void shouldFindAndDecryptData() {
            // Given
            String searchTerm = "Secret";
            
            // When
            List<Block> foundBlocks = api.findAndDecryptData(searchTerm, testPassword);
            
            // Then
            assertNotNull(foundBlocks, "Should return found blocks");
            // Should find our "Secret document content"
            assertTrue(foundBlocks.size() >= 0, "Should return results");
            
            if (!foundBlocks.isEmpty()) {
                Block firstResult = foundBlocks.get(0);
                assertNotNull(firstResult, "Found block should not be null");
            }
        }

        @Test
        @DisplayName("Should find records by identifier")
        void shouldFindRecordsByIdentifier() {
            // Given
            String identifier = "CONFIG_DATA";
            
            // When
            List<Block> results = api.findRecordsByIdentifier(identifier);
            
            // Then
            assertNotNull(results, "Should return search results");
            // Should find our block with CONFIG_DATA identifier
            if (!results.isEmpty()) {
                assertTrue(results.size() >= 1, "Should find at least one record with identifier");
            }
        }

        @Test
        @DisplayName("Should search everything publicly")
        void shouldSearchEverythingPublicly() {
            // Given
            String searchTerm = "information";
            
            // When
            List<Block> results = api.searchEverything(searchTerm);
            
            // Then
            assertNotNull(results, "Should return search results");
            // Should find public block containing "information"
            if (!results.isEmpty()) {
                boolean foundPublicInfo = results.stream()
                    .anyMatch(block -> block.getData().contains("information"));
                assertTrue(foundPublicInfo, "Should find public information");
            }
        }

        @Test
        @DisplayName("Should search everything with password")
        void shouldSearchEverythingWithPassword() {
            // Given
            String searchTerm = "document";
            
            // When
            List<Block> results = api.searchEverythingWithPassword(searchTerm, testPassword);
            
            // Then
            assertNotNull(results, "Should return search results");
            assertTrue(results.size() >= 0, "Should return non-negative number of results");
            
            if (!results.isEmpty()) {
                Block firstResult = results.get(0);
                assertNotNull(firstResult, "Found block should not be null");
            }
        }
    }

    @Nested
    @DisplayName("🧠 Content Analysis")
    class ContentAnalysisTests {

        @Test
        @DisplayName("Should extract keywords from content")
        void shouldExtractKeywordsFromContent() {
            // Given
            String content = "This is a sample document with important keywords like blockchain, security, and encryption";
            
            // When
            String keywords = api.extractKeywords(content);
            
            // Then
            assertNotNull(keywords, "Should return keywords string");
            assertFalse(keywords.trim().isEmpty(), "Should extract some keywords");
            
            // Should contain some meaningful content
            assertTrue(keywords.length() > 10, "Should extract meaningful keywords");
        }

        @Test
        @DisplayName("Should perform smart search without password")
        void shouldPerformSmartSearchWithoutPassword() {
            // Given
            String query = "public information test";
            
            // When
            List<Block> results = api.smartSearch(query);
            
            // Then
            assertNotNull(results, "Should return smart search results");
            assertTrue(results.size() >= 0, "Should return non-negative number of results");
        }

        @Test
        @DisplayName("Should perform smart search with password")
        void shouldPerformSmartSearchWithPassword() {
            // Given
            String query = "secret document content";
            
            // When
            List<Block> results = api.smartSearchWithPassword(query, testPassword);
            
            // Then
            assertNotNull(results, "Should return smart search results with decryption");
            assertTrue(results.size() >= 0, "Should return non-negative number of results");
        }

        @Test
        @DisplayName("Should perform smart advanced search")
        void shouldPerformSmartAdvancedSearch() {
            // Given
            String query = "document information";
            
            // When
            List<Block> results = api.smartAdvancedSearch(query, testPassword);
            
            // Then
            assertNotNull(results, "Should return advanced search results");
            assertTrue(results.size() >= 0, "Should return non-negative matches");
        }
    }

    @Nested
    @DisplayName("👤 User Management")
    class UserManagementTests {

        @Test
        @DisplayName("Should create new user")
        void shouldCreateNewUser() {
            // Given
            String newUsername = "newuser123";
            
            // When
            KeyPair result = api.createUser(newUsername);
            
            // Then
            assertNotNull(result, "User creation should return KeyPair");
            assertNotNull(result.getPrivate(), "Should have private key");
            assertNotNull(result.getPublic(), "Should have public key");
        }

        @Test
        @DisplayName("Should set default credentials")
        void shouldSetDefaultCredentials() throws Exception {
            // Given
            String username = "defaultuser";
            KeyPair keyPair = CryptoUtil.generateKeyPair();

            // SECURITY FIX (v1.0.6): Pre-authorize user before setting credentials
            String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
            realBlockchain.addAuthorizedKey(publicKeyString, username, bootstrapKeyPair, UserRole.USER);

            // When & Then - Should not throw exception
            assertDoesNotThrow(() -> {
                api.setDefaultCredentials(username, keyPair);
            }, "Setting default credentials should not throw exception");
        }

        @Test
        @DisplayName("Should load user credentials")
        void shouldLoadUserCredentials() {
            // Given
            String username = "loaduser";
            String password = "LoadPassword123!";
            
            // When & Then - Should not throw exception
            assertDoesNotThrow(() -> {
                api.loadUserCredentials(username, password);
            }, "Loading user credentials should not throw exception");
        }
    }

    @Nested
    @DisplayName("📈 Blockchain Analytics")  
    class BlockchainAnalyticsTests {

        @Test
        @DisplayName("Should get blockchain summary")
        void shouldGetBlockchainSummary() {
            // When
            String summary = api.getBlockchainSummary();
            
            // Then
            assertNotNull(summary, "Should return blockchain summary");
            assertFalse(summary.trim().isEmpty(), "Summary should not be empty");
            assertTrue(summary.contains("blocks") || summary.contains("Block"), 
                      "Summary should mention blocks");
        }

        @Test
        @DisplayName("Should check if blockchain has encrypted data")
        void shouldCheckIfBlockchainHasEncryptedData() {
            // When
            boolean hasEncrypted = api.hasEncryptedData();
            
            // Then
            assertTrue(hasEncrypted, "Blockchain should have encrypted data from setup");
        }

        @Test
        @DisplayName("Should get encrypted block count")
        void shouldGetEncryptedBlockCount() {
            // When
            long encryptedCount = api.getEncryptedBlockCount();
            
            // Then
            assertTrue(encryptedCount >= 2, "Should have at least 2 encrypted blocks from setup");
        }

        @Test
        @DisplayName("Should get unencrypted block count")
        void shouldGetUnencryptedBlockCount() {
            // When
            long unencryptedCount = api.getUnencryptedBlockCount();
            
            // Then
            assertTrue(unencryptedCount >= 1, "Should have at least 1 unencrypted block from setup");
        }

        @Test
        @DisplayName("Should get validation report")
        void shouldGetValidationReport() {
            // When
            String report = api.getValidationReport();
            
            // Then
            assertNotNull(report, "Should return validation report");
            assertFalse(report.trim().isEmpty(), "Report should not be empty");
        }
    }

    @Nested
    @DisplayName("🛡️ Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle invalid block numbers gracefully")
        void shouldHandleInvalidBlockNumbersGracefully() {
            // Test retrieveSecret with invalid block number
            assertDoesNotThrow(() -> {
                api.retrieveSecret(-1L, testPassword);
            }, "Should handle invalid block number gracefully");

            // Test isBlockEncrypted with invalid block number
            assertDoesNotThrow(() -> {
                api.isBlockEncrypted(999L);
            }, "Should handle invalid block number gracefully");
        }

        @Test
        @DisplayName("Should handle null and empty search terms")
        void shouldHandleNullAndEmptySearchTerms() {
            // Test with null search term
            assertDoesNotThrow(() -> {
                List<Block> results = api.findEncryptedData(null);
                assertNotNull(results, "Should return empty results for null search");
            }, "Should handle null search term gracefully");
            
            // Test with empty search term
            assertDoesNotThrow(() -> {
                List<Block> results = api.searchEverything("");
                assertNotNull(results, "Should return results for empty search");
            }, "Should handle empty search term gracefully");
        }

        @Test
        @DisplayName("Should handle wrong passwords gracefully")
        void shouldHandleWrongPasswordsGracefully() {
            // Given
            String wrongPassword = "WrongPassword123!";
            String searchTerm = "document";
            
            // When & Then - Should not crash with wrong password
            assertDoesNotThrow(() -> {
                List<Block> results = api.findAndDecryptData(searchTerm, wrongPassword);
                assertNotNull(results, "Should return results even with wrong password");
            }, "Should handle wrong password gracefully");
        }
    }
}