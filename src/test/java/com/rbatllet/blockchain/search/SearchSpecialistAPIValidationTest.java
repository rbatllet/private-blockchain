package com.rbatllet.blockchain.search;

import static org.junit.jupiter.api.Assertions.*;

import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.security.KeyFileLoader;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.CryptoUtil;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import org.junit.jupiter.api.*;

/**
 * Comprehensive validation test suite for SearchSpecialistAPI robustness.
 *
 * This test focuses on validating the current API design including:
 * - Constructor behavior and input validation
 * - Proper initialization patterns
 * - Fail-fast behavior for invalid inputs
 * - Core search functionality validation
 * - Error message quality and clarity
 * 
 * Adapted from SearchSpecialistAPIDesignTest.java.disabled to work with
 * the actual implemented API (constructor-based instead of builder pattern).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SearchSpecialistAPIValidationTest {

    private Blockchain testBlockchain;
    private KeyPair bootstrapKeyPair;
    private String testPassword;
    private PrivateKey testPrivateKey;
    private PublicKey testPublicKey;

    @BeforeEach
    void setUp() throws Exception {
        SearchFrameworkEngine.clearGlobalProcessingMapForTesting();
        IndexingCoordinator.getInstance().reset();

        testBlockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        testBlockchain.clearAndReinitialize();

        // Load bootstrap admin keys
        bootstrapKeyPair = KeyFileLoader.loadKeyPairFromFiles(
            "./keys/genesis-admin.private",
            "./keys/genesis-admin.public"
        );

        // Register bootstrap admin in blockchain (RBAC v1.0.6)
        testBlockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        testPassword = "ValidationTestPassword123!";

        // Generate test key pair
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        testPrivateKey = keyPair.getPrivate();
        testPublicKey = keyPair.getPublic();

        // Authorize key and add test blocks
        String publicKeyString = CryptoUtil.publicKeyToString(testPublicKey);
        testBlockchain.addAuthorizedKey(
            publicKeyString,
            "ValidationTestUser",
            bootstrapKeyPair,
            UserRole.USER
        );

        // Add some test blocks
        testBlockchain.addBlock(
            "Test data for validation testing",
            testPrivateKey,
            testPublicKey
        );
        testBlockchain.addBlock(
            "Another test block for comprehensive validation",            testPrivateKey,
            testPublicKey
        );
    }

    // ===== CONSTRUCTOR VALIDATION TESTS =====

    @Test
    @Order(1)
    @DisplayName("Auto-initializing constructor should work immediately")
    void testAutoInitializingConstructor() {
        // Should work immediately without separate initialization
        SearchSpecialistAPI api = new SearchSpecialistAPI(
            testBlockchain,
            testPassword,
            testPrivateKey
        );

        assertTrue(
            api.isReady(),
            "API should be ready immediately after auto-initializing constructor"
        );

        // Should be able to use search methods immediately
        assertDoesNotThrow(
            () -> {
                api.searchAll("test");
            },
            "Search methods should work immediately after auto-initializing constructor"
        );

        // Should have indexed blocks
        assertTrue(
            api.getStatistics().getTotalBlocksIndexed() > 0,
            "Should have indexed blocks from blockchain"
        );
    }

    @Test
    @Order(2)
    @DisplayName("Auto-initializing constructor with custom config should work")
    void testAutoInitializingConstructorWithConfig() {
        EncryptionConfig customConfig = EncryptionConfig.createBalancedConfig();
        
        SearchSpecialistAPI api = new SearchSpecialistAPI(
            testBlockchain,
            testPassword,
            testPrivateKey,
            customConfig
        );

        assertTrue(api.isReady(), "Should be ready immediately");
        assertEquals(customConfig.getSecurityLevel(), 
                    api.getEncryptionConfig().getSecurityLevel(),
                    "Should use custom encryption config");
        
        assertDoesNotThrow(() -> api.searchAll("test"),
                          "Should work with custom config");
    }

    @Test
    @Order(3)
    @DisplayName("Constructor should initialize immediately")
    void testConstructorInitializesImmediately() {
        SearchSpecialistAPI api = new SearchSpecialistAPI(testBlockchain, testPassword, testPrivateKey);

        assertTrue(
            api.isReady(),
            "API should be ready immediately after proper construction"
        );

        // Should work immediately without additional initialization
        assertDoesNotThrow(
            () -> {
                api.searchAll("test");
            },
            "Should not throw exception with properly constructed API"
        );

        // Verify it actually finds results (assuming test blockchain has data)
        var results = api.searchAll("test");
        assertNotNull(results, "Results should not be null");
    }

    // ===== CONSTRUCTOR INPUT VALIDATION =====    @Test
    @Order(10)
    @DisplayName("Auto-initializing constructor should validate null blockchain")
    void testConstructorNullBlockchainValidation() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SearchSpecialistAPI(null, testPassword, testPrivateKey),
            "Should throw IllegalArgumentException for null blockchain"
        );
    }

    @Test
    @Order(11)
    @DisplayName("Auto-initializing constructor should validate null password")
    void testConstructorNullPasswordValidation() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SearchSpecialistAPI(testBlockchain, null, testPrivateKey),
            "Should throw IllegalArgumentException for null password"
        );
    }

    @Test
    @Order(12)
    @DisplayName("Auto-initializing constructor should validate null private key")
    void testConstructorNullPrivateKeyValidation() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SearchSpecialistAPI(testBlockchain, testPassword, null),
            "Should throw IllegalArgumentException for null private key"
        );
    }

    @Test
    @Order(13)
    @DisplayName("Constructor with config should validate all parameters")
    void testConstructorWithConfigValidation() {
        EncryptionConfig config = EncryptionConfig.createBalancedConfig();
        
        // Test null blockchain
        assertThrows(IllegalArgumentException.class, () -> 
            new SearchSpecialistAPI(null, testPassword, testPrivateKey, config));
        
        // Test null password
        assertThrows(IllegalArgumentException.class, () -> 
            new SearchSpecialistAPI(testBlockchain, null, testPrivateKey, config));
        
        // Test null private key
        assertThrows(IllegalArgumentException.class, () -> 
            new SearchSpecialistAPI(testBlockchain, testPassword, null, config));
        
        // Test null config
        assertThrows(IllegalArgumentException.class, () -> 
            new SearchSpecialistAPI(testBlockchain, testPassword, testPrivateKey, null));
    }

    // ===== SEARCH INPUT VALIDATION TESTS =====    @Test
    @Order(20)
    @DisplayName("Search methods should validate null queries")
    void testSearchNullQueryValidation() {
        SearchSpecialistAPI api = new SearchSpecialistAPI(
            testBlockchain,
            testPassword,
            testPrivateKey
        );

        // Test null query validation for all search methods
        assertThrows(IllegalArgumentException.class, () ->
            api.searchAll(null),
            "searchAll should reject null query"
        );
        
        assertThrows(IllegalArgumentException.class, () ->
            api.searchSecure(null, testPassword),
            "searchSecure should reject null query"
        );
        
        assertThrows(IllegalArgumentException.class, () ->
            api.searchIntelligent(null, testPassword, 10),
            "searchIntelligent should reject null query"
        );
    }

    @Test
    @Order(21)
    @DisplayName("Search methods should validate empty and whitespace queries")
    void testSearchEmptyQueryValidation() {
        SearchSpecialistAPI api = new SearchSpecialistAPI(
            testBlockchain,
            testPassword,
            testPrivateKey
        );

        // Test empty string queries
        assertThrows(IllegalArgumentException.class, () ->
            api.searchAll(""),
            "searchAll should reject empty query"
        );
        
        assertThrows(IllegalArgumentException.class, () ->
            api.searchSecure("", testPassword),
            "searchSecure should reject empty query"
        );

        // Test whitespace-only queries
        assertThrows(IllegalArgumentException.class, () ->
            api.searchAll("   "),
            "searchAll should reject whitespace-only query"
        );
        
        assertThrows(IllegalArgumentException.class, () ->
            api.searchIntelligent("   ", testPassword, 10),
            "searchIntelligent should reject whitespace-only query"
        );
    }

    @Test
    @Order(22)
    @DisplayName("Secure search methods should validate null passwords")
    void testSearchNullPasswordValidation() {
        SearchSpecialistAPI api = new SearchSpecialistAPI(
            testBlockchain,
            testPassword,
            testPrivateKey
        );

        // Test null password validation
        assertThrows(IllegalArgumentException.class, () ->
            api.searchSecure("test", null),
            "searchSecure should reject null password"
        );
        
        assertThrows(IllegalArgumentException.class, () ->
            api.searchIntelligent("test", null, 10),
            "searchIntelligent should reject null password"
        );
    }    @Test
    @Order(23)
    @DisplayName("Search methods should validate maxResults parameter")
    void testSearchMaxResultsValidation() {
        SearchSpecialistAPI api = new SearchSpecialistAPI(
            testBlockchain,
            testPassword,
            testPrivateKey
        );

        // Test invalid maxResults validation
        assertThrows(IllegalArgumentException.class, () ->
            api.searchAll("test", 0),
            "searchAll should reject maxResults = 0"
        );
        
        assertThrows(IllegalArgumentException.class, () ->
            api.searchAll("test", -1),
            "searchAll should reject negative maxResults"
        );
        
        assertThrows(IllegalArgumentException.class, () ->
            api.searchSecure("test", testPassword, 0),
            "searchSecure should reject maxResults = 0"
        );
        
        assertThrows(IllegalArgumentException.class, () ->
            api.searchIntelligent("test", testPassword, -5),
            "searchIntelligent should reject negative maxResults"
        );
    }

    // ===== CORE FUNCTIONALITY TESTS =====

    @Test
    @Order(40)
    @DisplayName("Core search functionality should work correctly")
    void testCoreFunctionality() {
        SearchSpecialistAPI api = new SearchSpecialistAPI(
            testBlockchain,
            testPassword,
            testPrivateKey
        );

        // All core search methods should work without throwing exceptions
        assertDoesNotThrow(() -> {
            api.searchAll("test");
            api.searchSecure("test", testPassword);
            api.searchIntelligent("test", testPassword, 10);
        }, "Core search functionality should work without exceptions");

        // System methods should work
        assertDoesNotThrow(() -> {
            api.getStatistics();
            api.getEncryptionConfig();
            api.getPerformanceMetrics();
            api.getCapabilitiesSummary();
        }, "System methods should work without exceptions");
    }

    @Test
    @Order(41)
    @DisplayName("Search methods should return valid results")
    void testSearchResultsValidity() {
        SearchSpecialistAPI api = new SearchSpecialistAPI(
            testBlockchain,
            testPassword,
            testPrivateKey
        );

        // Search results should be non-null (but may be empty)
        assertNotNull(api.searchAll("test"), 
                     "searchAll should return non-null result");
        assertNotNull(api.searchSecure("test", testPassword), 
                     "searchSecure should return non-null result");
        assertNotNull(api.searchIntelligent("test", testPassword, 5), 
                     "searchIntelligent should return non-null result");

        // Results should respect maxResults parameter
        assertTrue(api.searchAll("test", 3).size() <= 3,
                  "searchAll should respect maxResults limit");
        assertTrue(api.searchSecure("test", testPassword, 2).size() <= 2,
                  "searchSecure should respect maxResults limit");
    }

    @Test
    @Order(42)
    @DisplayName("Statistics and config methods should return valid data")
    void testSystemMethodsValidity() {
        SearchSpecialistAPI api = new SearchSpecialistAPI(
            testBlockchain,
            testPassword,
            testPrivateKey
        );

        // System methods should return valid data
        assertNotNull(api.getStatistics(), "getStatistics should return non-null");
        assertNotNull(api.getEncryptionConfig(), "getEncryptionConfig should return non-null");
        assertNotNull(api.getPerformanceMetrics(), "getPerformanceMetrics should return non-null");
        assertNotNull(api.getCapabilitiesSummary(), "getCapabilitiesSummary should return non-null");

        // Statistics should show indexed blocks
        assertTrue(api.getStatistics().getTotalBlocksIndexed() > 0,
                  "Should have indexed some blocks");
    }

    // ===== ERROR MESSAGE QUALITY TESTS =====

    @Test
    @Order(60)
    @DisplayName("API should work correctly with proper initialization")
    void testAPIFunctionality() {
        SearchSpecialistAPI api = new SearchSpecialistAPI(testBlockchain, testPassword, testPrivateKey);

        // API should work correctly after proper initialization
        assertDoesNotThrow(() -> {
            api.searchAll("test");
            api.searchSecure("test", testPassword);
            api.searchIntelligent("test", testPassword, 5);
        }, "API should work correctly when properly initialized");

        // Results should be non-null (even if empty)
        assertNotNull(api.searchAll("test"), "searchAll should return non-null result");
        assertNotNull(api.searchSecure("test", testPassword), "searchSecure should return non-null result");
        assertNotNull(api.searchIntelligent("test", testPassword, 5), "searchIntelligent should return non-null result");

        // Test input validation error messages
        SearchSpecialistAPI readyApi = new SearchSpecialistAPI(
            testBlockchain, testPassword, testPrivateKey
        );

        try {
            readyApi.searchAll(null);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            assertTrue(
                message.contains("query") || message.contains("null"),
                "Error message should mention query problem: " + message
            );
        }
    }

    // ===== BACKWARD COMPATIBILITY TESTS =====

    @Test
    @Order(70)
    @DisplayName("API should maintain backward compatibility for existing usage patterns")
    void testBackwardCompatibility() {
        // Test that existing usage patterns still work
        SearchSpecialistAPI api = new SearchSpecialistAPI(
            testBlockchain,
            testPassword,
            testPrivateKey
        );

        // Basic search functionality should work as expected
        assertDoesNotThrow(() -> {
            api.searchAll("test");
            api.searchSecure("test", testPassword);
            api.searchIntelligent("test", testPassword, 10);
        }, "Core search functionality should remain compatible");

        // Advanced features should work
        assertDoesNotThrow(() -> {
            api.getStatistics();
            api.getEncryptionConfig();
            api.getPerformanceMetrics();
            api.isReady();
        }, "Advanced features should remain compatible");
    }
}