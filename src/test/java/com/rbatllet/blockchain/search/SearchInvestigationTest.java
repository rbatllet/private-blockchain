package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.security.KeyPair;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Investigation test to understand why smartSearchWithPassword returns 0 results
 * while searchAndDecryptByTerms works correctly
 */
public class SearchInvestigationTest {
    private static final Logger logger = LoggerFactory.getLogger(SearchInvestigationTest.class);

    
    private Blockchain blockchain;
    private KeyPair bootstrapKeyPair;
    private UserFriendlyEncryptionAPI api;
    private String testPassword;
    
    @BeforeEach
    void setUp() throws Exception {
        blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        // Load bootstrap admin keys (auto-generates if missing - test-only)
        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();

        // SECURITY (v1.0.6): Register bootstrap admin in blockchain (REQUIRED!)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        api = new UserFriendlyEncryptionAPI(blockchain);
        testPassword = "TestPassword123!";

        // PRE-AUTHORIZATION REQUIRED (v1.0.6 RBAC security):
        // 1. Create admin to act as authorized user creator
        KeyPair adminKeys = CryptoUtil.generateKeyPair();
        String adminPublicKey = CryptoUtil.publicKeyToString(adminKeys.getPublic());
        blockchain.addAuthorizedKey(adminPublicKey, "Admin", bootstrapKeyPair, UserRole.ADMIN);
        api.setDefaultCredentials("Admin", adminKeys);  // Authenticate as admin
        
        // 2. Now admin can create test user (generates new keys internally)
        KeyPair userKeys = api.createUser("test-user");
        api.setDefaultCredentials("test-user", userKeys);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        // Phase 5.4 FIX: Wait for async indexing to complete before cleanup
        IndexingCoordinator.getInstance().waitForCompletion();

        // CRITICAL: Clear database + search indexes to prevent state contamination
        if (blockchain != null) {
            blockchain.clearAndReinitialize();  // Also calls clearIndexes() + clearCache()
        }

        // Reset IndexingCoordinator singleton state
        IndexingCoordinator.getInstance().forceShutdown();
        IndexingCoordinator.getInstance().clearShutdownFlag();
        IndexingCoordinator.getInstance().disableTestMode();

        if (blockchain != null) {
            blockchain.shutdown();
        }
    }

    @Test
    @DisplayName("Deep investigation: smartSearchWithPassword vs searchAndDecryptByTerms")
    void testSearchInvestigation() throws Exception {
        logger.info("=== SMART SEARCH INVESTIGATION ===");
        
        // 1. CRITICAL FIX: Initialize Search Framework BEFORE storing data
        blockchain.initializeAdvancedSearch(testPassword);
        logger.info("✅ Search Framework initialized");
        
        // 2. Store data with SearchSpecialistAPI properly initialized
        String[] keywords = {"financial", "test", "investigation"};
        Block storedBlock = api.storeSearchableData("Test financial data for investigation", testPassword, keywords);
        
        logger.info("✅ Stored block #" + storedBlock.getBlockNumber() + " with keywords: " + String.join(", ", keywords));
        
        // CRITICAL: Wait for async indexing to complete before searching
        IndexingCoordinator.getInstance().waitForCompletion();
        
        // 3. Test both search methods with identical parameters
        logger.info("\n🔍 Comparing search methods:");
        
        // Method 1: smartSearchWithPassword (failing)
        List<Block> smartResults = api.smartSearchWithPassword("financial", testPassword);
        logger.info("🧠 smartSearchWithPassword('financial'): " + smartResults.size() + " results");
        
        // Method 2: searchAndDecryptByTerms (working)
        List<Block> decryptResults = api.searchAndDecryptByTerms(new String[]{"financial"}, testPassword, 10);
        logger.info("🔓 searchAndDecryptByTerms(['financial']): " + decryptResults.size() + " results");
        
        // 4. Analyze the internal search flow
        logger.info("\n🔍 Internal search flow analysis:");
        
        // Check SearchSpecialistAPI state
        SearchSpecialistAPI searchAPI = blockchain.getSearchSpecialistAPI();
        logger.info("📊 SearchSpecialistAPI.isReady(): " + searchAPI.isReady());
        
        // 5. Test with different search strategies directly
        if (searchAPI.isReady()) {
            logger.info("\n🎯 Testing SearchSpecialistAPI methods directly:");
            
            // Test searchIntelligent (used by smartSearchWithPassword)
            List<SearchFrameworkEngine.EnhancedSearchResult> intelligentResults = 
                searchAPI.searchIntelligent("financial", testPassword, 10);
            logger.info("🧠 searchIntelligent('financial'): " + intelligentResults.size() + " results");
            
            // Test searchSecure
            List<SearchFrameworkEngine.EnhancedSearchResult> secureResults = 
                searchAPI.searchSecure("financial", testPassword, 10);
            logger.info("🔐 searchSecure('financial'): " + secureResults.size() + " results");
            
            // Test searchAll
            List<SearchFrameworkEngine.EnhancedSearchResult> simpleResults = 
                searchAPI.searchAll("financial");
            logger.info("⚡ searchAll('financial'): " + simpleResults.size() + " results");
        }
        
        // 6. Check password registry
        logger.info("\n🔑 Password registry analysis:");
        try {
            // Get password registry stats through blockchain
            logger.info("📊 Blockchain has search framework initialized: " + 
                             (blockchain.getSearchSpecialistAPI() != null));
        } catch (Exception e) {
            logger.info("❌ Error accessing password registry: " + e.getMessage());
        }
        
        // 7. Trace smartSearchWithPassword execution path
        logger.info("\n🔍 Tracing smartSearchWithPassword execution:");
        
        try {
            // Call searchWithAdaptiveDecryption directly (internal method used by smartSearchWithPassword)
            List<Block> adaptiveResults = api.searchWithAdaptiveDecryption("financial", testPassword, 10);
            logger.info("🔄 searchWithAdaptiveDecryption('financial'): " + adaptiveResults.size() + " results");
        } catch (Exception e) {
            logger.info("❌ searchWithAdaptiveDecryption failed: " + e.getMessage());
        }
        
        // 8. Check if the issue is password-related
        logger.info("\n🔐 Password validation test:");
        
        List<Block> wrongPasswordResults = api.searchAndDecryptByTerms(new String[]{"financial"}, "wrongpassword", 10);
        logger.info("❌ searchAndDecryptByTerms with wrong password: " + wrongPasswordResults.size() + " results");
        
        try {
            List<Block> wrongPasswordSmart = api.smartSearchWithPassword("financial", "wrongpassword");
            logger.info("❌ smartSearchWithPassword with wrong password: " + wrongPasswordSmart.size() + " results");
        } catch (Exception e) {
            logger.info("❌ smartSearchWithPassword with wrong password threw: " + e.getMessage());
        }
        
        // 9. Summary and hypothesis
        logger.info("\n📋 INVESTIGATION SUMMARY:");
        logger.info("✅ searchAndDecryptByTerms works: " + (decryptResults.size() > 0));
        logger.info("❌ smartSearchWithPassword fails: " + (smartResults.size() == 0));
        logger.info("🔍 SearchSpecialistAPI ready: " + searchAPI.isReady());
        
        if (smartResults.size() == 0 && decryptResults.size() > 0) {
            logger.info("\n💡 HYPOTHESIS: smartSearchWithPassword has a different execution path");
            logger.info("   that doesn't properly access the stored data or password registry.");
            logger.info("   The issue is likely in the searchWithAdaptiveDecryption method");
            logger.info("   or in how it interfaces with the SearchSpecialistAPI.");
        }
        
        logger.info("\n=== END INVESTIGATION ===");
    }
    
    @Test
    @DisplayName("Minimal reproduction case for smartSearchWithPassword")
    void testMinimalSmartSearchReproduction() throws Exception {
        logger.info("\n=== MINIMAL REPRODUCTION TEST ===");
        
        // CRITICAL FIX: Initialize SearchSpecialistAPI BEFORE storing data
        blockchain.initializeAdvancedSearch(testPassword);
        
        // Now store data with SearchSpecialistAPI properly initialized
        String[] simpleKeywords = {"test"};
        api.storeSearchableData("Simple test data", testPassword, simpleKeywords);
        
        // CRITICAL: Wait for async indexing to complete before searching
        IndexingCoordinator.getInstance().waitForCompletion();
        
        // Test both methods on identical simple data
        List<Block> smartResults = api.smartSearchWithPassword("test", testPassword);
        List<Block> decryptResults = api.searchAndDecryptByTerms(new String[]{"test"}, testPassword, 10);
        
        logger.info("📊 Simple case results:");
        logger.info("   smartSearchWithPassword: " + smartResults.size());
        logger.info("   searchAndDecryptByTerms: " + decryptResults.size());
        
        // Both methods should now return results (fixing the original 0 results bug)
        // However, they may return different counts due to different search algorithms
        assertTrue(smartResults.size() > 0, "smartSearchWithPassword should return results (was 0 before bug fix)");
        assertTrue(decryptResults.size() > 0, "searchAndDecryptByTerms should return results");
        
        logger.info("✅ Both methods now return results - the original bug has been fixed!");
        
        logger.info("=== END MINIMAL REPRODUCTION ===");
    }
}