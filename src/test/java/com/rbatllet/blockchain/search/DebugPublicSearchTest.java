package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.security.KeyPair;
import java.util.List;

/**
 * Debug test to understand how public search works
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugPublicSearchTest {
    private static final Logger logger = LoggerFactory.getLogger(DebugPublicSearchTest.class);

    private Blockchain blockchain;
    private KeyPair bootstrapKeyPair;
    private UserFriendlyEncryptionAPI encryptionAPI;
    private SearchSpecialistAPI searchAPI;

    @BeforeEach
    public void setUp() {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();

        // Load bootstrap admin keys (auto-generates if missing - test-only)
        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();

        // Register bootstrap admin in blockchain (RBAC v1.0.6)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        // Set up encryption credentials
        encryptionAPI = new UserFriendlyEncryptionAPI(blockchain);

        // PRE-AUTHORIZATION REQUIRED (v1.0.6 security):
        // 1. Create admin to act as authorized user creator
        KeyPair adminKeys = CryptoUtil.generateKeyPair();
        String adminPublicKey = CryptoUtil.publicKeyToString(adminKeys.getPublic());
        blockchain.addAuthorizedKey(adminPublicKey, "Admin", bootstrapKeyPair, UserRole.ADMIN);
        encryptionAPI.setDefaultCredentials("Admin", adminKeys);  // Authenticate as admin
        
        // 2. Now admin can create test user (generates new keys internally)
        java.security.KeyPair userKeyPair = encryptionAPI.createUser("searchFixUser");
        encryptionAPI.setDefaultCredentials("searchFixUser", userKeyPair);
        
        searchAPI = blockchain.getSearchSpecialistAPI();
    }

    @AfterEach
    public void tearDown() {
        if (blockchain != null) {
            blockchain.clearAndReinitialize();
            blockchain.shutdown();
        }
    }

    @Test
    public void debugPublicSearchIndexing() {
        logger.info("🔍 DEBUG: Testing how public search indexing works");
        
        // Store data with explicit public/private separation
        encryptionAPI.storeSearchableDataWithLayers(
            "Patient medical record with diagnosis",
            "testPassword",
            new String[]{"medical"}, // public terms
            new String[]{"hypertension"} // private terms
        );
        
        logger.info("✅ Data stored with public: [medical], private: [hypertension]");
        
        // Test 1: Search for public term directly
        List<SearchFrameworkEngine.EnhancedSearchResult> results1 = searchAPI.searchAll("medical");
        logger.info("🔍 Search 'medical': {} results", results1.size());
        
        // Test 2: Search for public term with prefix
        List<SearchFrameworkEngine.EnhancedSearchResult> results2 = searchAPI.searchAll("public:medical");
        logger.info("🔍 Search 'public:medical': {} results", results2.size());
        
        // Test 3: Search for private term (should be 0)
        List<SearchFrameworkEngine.EnhancedSearchResult> results3 = searchAPI.searchAll("hypertension");
        logger.info("🔍 Search 'hypertension': {} results", results3.size());
        
        // Test 4: Search using searchSecure (should find both)
        List<SearchFrameworkEngine.EnhancedSearchResult> results4 = searchAPI.searchSecure("medical", "testPassword");
        logger.info("🔍 SearchSecure 'medical': {} results", results4.size());
        
        List<SearchFrameworkEngine.EnhancedSearchResult> results5 = searchAPI.searchSecure("hypertension", "testPassword");
        logger.info("🔍 SearchSecure 'hypertension': {} results", results5.size());
        
        logger.info("\n🔍 DEBUG: With option B, searchAll should find ALL terms (public and private)");
    }
}