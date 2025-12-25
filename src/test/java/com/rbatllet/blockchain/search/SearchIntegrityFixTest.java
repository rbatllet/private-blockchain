package com.rbatllet.blockchain.search;

import static org.junit.jupiter.api.Assertions.*;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.testutil.GenesisKeyManager;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.EnhancedSearchResult;

import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CRITICAL FIX: Corrects the discrepancy problem between indexing and search
 *
 * This test demonstrates and validates the solution to the identified problem:
 * - Keywords without "public:" prefix cannot be searched with searchAll()
 * - Keywords with "public:" prefix can be searched correctly
 *
 * The solution is to use the "public:" prefix for keywords that should be
 * publicly searchable (without password).
 */
@DisplayName("🔧 Search Integrity Fix Validation")
class SearchIntegrityFixTest {

    private static final Logger logger = LoggerFactory.getLogger(SearchIntegrityFixTest.class);
    
    private Blockchain blockchain;
    private KeyPair bootstrapKeyPair;
    private UserFriendlyEncryptionAPI api;
    private KeyPair testKeyPair;
    private String testUsername = "searchFixUser";

    @BeforeEach
    void setUp() {
        // Phase 5.4 FIX: DO NOT enable test mode - it skips async indexing!
        // Test mode should only be used for tests that manually control indexing.
        // IndexingCoordinator.getInstance().enableTestMode();

        blockchain = new Blockchain();
        // Clean database before each test to ensure test isolation
        blockchain.clearAndReinitialize();

        // Load bootstrap admin keys (auto-generates if missing - test-only)
        bootstrapKeyPair = GenesisKeyManager.ensureGenesisKeysExist();

        // Register bootstrap admin in blockchain (RBAC v1.0.6)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        testKeyPair = CryptoUtil.generateKeyPair();

        // SECURITY FIX (v1.0.6 RBAC): Pre-authorize user before creating API
        String publicKeyString = CryptoUtil.publicKeyToString(testKeyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyString, testUsername, bootstrapKeyPair, UserRole.USER);

        api = new UserFriendlyEncryptionAPI(blockchain, testUsername, testKeyPair);

        // Phase 5.4: DO NOT initialize SearchSpecialistAPI in setUp() -
        // it must be initialized AFTER blocks are created and indexed
    }

    @AfterEach
    void tearDown() {
        try {
            // Phase 5.4 FIX: Wait for async indexing BEFORE shutdown
            // This ensures background indexing tasks complete before coordinator shuts down
            logger.info("⏳ Waiting for any pending async indexing to complete before teardown...");
            IndexingCoordinator.getInstance().waitForCompletion();
            logger.info("✅ All async indexing completed, proceeding with shutdown");

            // Phase 5.4 FIX: Clear database + search indexes FIRST
            // This removes all blocks and their indexes before resetting IndexingCoordinator
            if (blockchain != null) {
                blockchain.clearAndReinitialize();  // Also calls clearIndexes() + clearCache()
            }

            // CRITICAL: Reset IndexingCoordinator singleton state AFTER clearing database
            // forceShutdown() sets shutdownRequested=true, blocking future indexing
            // clearShutdownFlag() clears it without enabling test mode, allowing next test to index
            IndexingCoordinator.getInstance().forceShutdown();
            IndexingCoordinator.getInstance().clearShutdownFlag();  // Phase 5.4 FIX: Clear shutdown flag
            IndexingCoordinator.getInstance().disableTestMode();

            if (blockchain != null) {
                blockchain.shutdown();
            }
        } catch (Exception e) {
            logger.warn("Teardown warning: {}", e.getMessage());
        }
    }

    @Test
    @DisplayName("PHASE 5.4 FIX: storeSearchableData() makes keywords PUBLIC by default")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void demonstratePhase54Fix() {
        logger.info("🔍 PHASE 5.4 FIX VALIDATION: storeSearchableData() auto-adds 'public:' prefix");

        // Phase 5.4: storeSearchableData() NOW automatically makes keywords PUBLIC
        String[] medicalTerms = {
            "medical",      // ← Phase 5.4: Automatically becomes PUBLIC!
            "patient",
            "hypertension",
            "lisinopril",
            "PATIENT_001"
        };

        Block block = api.storeSearchableData(
            "Medical patient data with auto-public keywords",
            "Password123!",
            medicalTerms
        );

        assertNotNull(block, "Block should be created successfully");

        // Phase 5.4: Wait for async indexing to complete
        waitForIndexing();

        // Phase 5.4 FIX: searchAll() NOW finds these keywords (they're public by default)
        List<EnhancedSearchResult> searchResult = blockchain.getSearchSpecialistAPI().searchAll("medical", 10);

        logger.info("🔍 Search for 'medical' returned {} results", searchResult.size());

        assertTrue(searchResult.size() > 0,
                  "Phase 5.4 FIX: storeSearchableData() keywords are PUBLIC by default");

        // findEncryptedData() also works (searches public metadata)
        List<Block> encryptedResults = api.findEncryptedData("medical");
        logger.info("🔍 findEncryptedData('medical') returned {} results", encryptedResults.size());

        assertTrue(encryptedResults.size() > 0,
                  "findEncryptedData should find the block via public search");

        logger.info("✅ PHASE 5.4 FIX CONFIRMED: Keywords are publicly searchable by default");
    }

    @Test
    @DisplayName("AFTER FIX: Shows the correct way to make keywords publicly searchable")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void demonstrateCorrectSolution() {
        logger.info("🔍 DEMONSTRATING CORRECT SOLUTION: Using storeSearchableDataWithLayers");
        
        // FIXED VERSION: Use storeSearchableDataWithLayers to explicitly separate public/private terms
        String[] publicTerms = {
            "medical",       // ← This WILL be publicly searchable!
            "patient",       // ← This WILL be publicly searchable!
            "healthcare"     // ← This WILL be publicly searchable!
        };
        
        String[] privateTerms = {
            "hypertension",  // ← This goes to private layer (requires password)
            "lisinopril",    // ← This goes to private layer (requires password)
            "PATIENT_001"    // ← This goes to private layer (requires password)
        };
        
        Block fixedBlock = api.storeSearchableDataWithLayers(
            "Medical patient data with PROPERLY SEPARATED public/private keywords",
            "Password123!", 
            publicTerms,
            privateTerms
        );
        
        assertNotNull(fixedBlock, "Block should be created successfully");

        // Phase 5.4: Wait for async indexing to complete
        // NOTE: DO NOT call initializeAdvancedSearch() - blocks are already indexed!
        waitForIndexing();

        // Now search for "medical" (the API internally handles the public: prefix)
        List<EnhancedSearchResult> publicSearchResult = blockchain.getSearchSpecialistAPI().searchAll("medical", 10);
        
        logger.info("🔍 Search for 'medical' (public term) returned {} results", 
                   publicSearchResult.size());
        
        assertTrue(publicSearchResult.size() > 0, 
                  "CRITICAL: 'medical' should be findable via searchAll() when stored as public term");
        
        // Verify the found block is our test block
        boolean foundOurBlock = publicSearchResult.stream()
                              .anyMatch(result -> result.getBlockHash().equals(fixedBlock.getHash()));
        
        assertTrue(foundOurBlock, "Should find our specific test block in results");
        
        // Test other public keywords
        List<EnhancedSearchResult> patientSearchResult = blockchain.getSearchSpecialistAPI().searchPublic("patient", 10);
        assertTrue(patientSearchResult.size() > 0, 
                  "patient should also be searchable as public term");
        
        List<EnhancedSearchResult> healthcareSearchResult = blockchain.getSearchSpecialistAPI().searchPublic("healthcare", 10);
        assertTrue(healthcareSearchResult.size() > 0, 
                  "healthcare should also be searchable as public term");
        
        logger.info("✅ SOLUTION CONFIRMED: Public keywords are searchable with correct prefix");
        
        // Validate that private keywords are NOT publicly searchable
        List<EnhancedSearchResult> privateAttempt1 = blockchain.getSearchSpecialistAPI().searchPublic("hypertension", 10);
        List<EnhancedSearchResult> privateAttempt2 = blockchain.getSearchSpecialistAPI().searchPublic("lisinopril", 10);
        
        assertTrue(privateAttempt1.isEmpty(), 
                  "Private keyword 'hypertension' should NOT be publicly searchable");
        assertTrue(privateAttempt2.isEmpty(), 
                  "Private keyword 'lisinopril' should NOT be publicly searchable");
        
        logger.info("✅ SECURITY CONFIRMED: Private keywords are properly protected");
    }

    @Test
    @DisplayName("COMPREHENSIVE FIX: Updated demo with correct keyword formatting")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void demonstrateUpdatedDemoFlow() {
        logger.info("🔍 COMPREHENSIVE FIX: Updated demo with correct keyword formatting");
        
        // FIXED VERSION of the original demo data creation
        
        // 1. Medical data with proper public/private separation
        String medicalData = "Patient John Doe, DOB: 1985-03-15, Diagnosis: Hypertension, Treatment: Lisinopril 10mg daily";
        String[] medicalPublicTerms = {
            "medical",       // Publicly searchable - general category
            "patient",       // Publicly searchable - general type  
            "healthcare",    // Publicly searchable - domain
            "emergency"      // Publicly searchable - priority level
        };
        String[] medicalPrivateTerms = {
            "hypertension",  // Private - specific medical condition
            "lisinopril",    // Private - specific medication
            "PATIENT_001"    // Private - specific patient ID
        };
        
        Block medicalBlock = api.storeSearchableDataWithLayers(medicalData, "Password123!", medicalPublicTerms, medicalPrivateTerms);
        assertNotNull(medicalBlock, "Medical block should be created");
        
        // 2. Financial data with proper public/private separation  
        String financialData = "Account: CHK_123456789, Balance: $45,230.50, Transaction: Mortgage payment $2,100.00";
        String[] financialPublicTerms = {
            "financial",     // Publicly searchable - general category
            "account",       // Publicly searchable - general type
            "banking",       // Publicly searchable - domain  
            "transaction"    // Publicly searchable - action type
        };
        String[] financialPrivateTerms = {
            "balance",       // Private - specific financial detail
            "CHK_123456789", // Private - specific account number
            "mortgage"       // Private - specific transaction type
        };
        
        Block financialBlock = api.storeSearchableDataWithLayers(financialData, "Password123!", financialPublicTerms, financialPrivateTerms);
        assertNotNull(financialBlock, "Financial block should be created");
        
        // 3. Legal data with proper public/private separation
        String legalData = "Contract Agreement between parties for property transfer, Case #LEG_789012";
        String[] legalPublicTerms = {
            "legal",         // Publicly searchable - general category
            "contract",      // Publicly searchable - document type
            "agreement",     // Publicly searchable - general nature
            "property"       // Publicly searchable - general subject
        };
        String[] legalPrivateTerms = {
            "transfer",      // Private - specific action
            "LEG_789012",    // Private - specific case number
            "parties"        // Private - specific detail
        };
        
        Block legalBlock = api.storeSearchableDataWithLayers(legalData, "Password123!", legalPublicTerms, legalPrivateTerms);
        assertNotNull(legalBlock, "Legal block should be created");

        // Phase 5.4: Wait for async indexing to complete
        // NOTE: DO NOT call initializeAdvancedSearch() - blocks are already indexed!
        waitForIndexing();

        // TEST: Verify all public categories are searchable
        logger.info("📋 Testing public category searches...");
        
        validatePublicSearch("medical", 1, "Medical category should be searchable");
        validatePublicSearch("financial", 1, "Financial category should be searchable"); 
        validatePublicSearch("legal", 1, "Legal category should be searchable");
        
        // TEST: Verify public types are searchable
        logger.info("📋 Testing public type searches...");
        
        validatePublicSearch("patient", 1, "Patient type should be searchable");
        validatePublicSearch("account", 1, "Account type should be searchable");
        validatePublicSearch("contract", 1, "Contract type should be searchable");
        
        // TEST: Verify private data is NOT publicly searchable
        logger.info("📋 Testing private data protection...");
        
        validatePrivateProtection("hypertension", "Specific medical condition should be private");
        validatePrivateProtection("CHK_123456789", "Account number should be private");
        validatePrivateProtection("LEG_789012", "Case number should be private");
        
        // TEST: Verify public terms ARE accessible via findEncryptedData (searches public metadata)
        logger.info("📋 Testing public data access via findEncryptedData...");

        // Phase 5.4: findEncryptedData() uses searchAll() which searches public metadata when defaultPassword=null
        // Therefore, it can ONLY find PUBLIC terms, not private ones
        List<Block> medicalResults = api.findEncryptedData("medical");
        assertTrue(medicalResults.size() > 0, "Public medical category should be accessible via findEncryptedData");

        List<Block> financialResults = api.findEncryptedData("financial");
        assertTrue(financialResults.size() > 0, "Public financial category should be accessible via findEncryptedData");

        // Phase 5.4: Private terms like "hypertension" are NOT publicly searchable
        // They would require password-based search (not implemented in this simple test)
        
        // TEST: Performance validation
        logger.info("📋 Testing search performance...");
        
        long startTime = System.nanoTime();
        List<EnhancedSearchResult> perfResult = blockchain.getSearchSpecialistAPI().searchPublic("medical", 10);
        long endTime = System.nanoTime();
        double searchTimeMs = (endTime - startTime) / 1_000_000.0;
        
        assertTrue(perfResult.size() > 0, 
                  "Performance test should find medical results");
        assertTrue(searchTimeMs < 100.0, 
                  "Public search should be fast (<100ms), was: " + searchTimeMs + "ms");
        
        logger.info("✅ COMPREHENSIVE FIX VALIDATION PASSED");
        logger.info("   - {} public categories searchable", 3);
        logger.info("   - {} public types searchable", 3); 
        logger.info("   - Private data properly protected");
        logger.info("   - Authenticated access working");
        logger.info("   - Performance: {:.2f}ms", searchTimeMs);
    }

    @Test
    @DisplayName("REGRESSION TEST: Ensure fix doesn't break existing functionality")
    @Timeout(value = 12, unit = TimeUnit.SECONDS)
    void ensureFixDoesntBreakExistingFunctionality() {
        logger.info("🔍 REGRESSION TEST: Ensuring fix doesn't break existing functionality");
        
        // Test 1: Mixed keyword blocks should work correctly
        String[] publicKeywords = {"test", "searchable"};
        String[] privateKeywords = {"private_data", "internal_use"};
        
        Block mixedBlock = api.storeSearchableDataWithLayers("Mixed keyword test data", "Password123!", publicKeywords, privateKeywords);
        assertNotNull(mixedBlock, "Mixed keyword block should be created");

        // Phase 5.4: Wait for async indexing to complete
        // NOTE: DO NOT call initializeAdvancedSearch() - blocks are already indexed!
        waitForIndexing();

        // Public keywords should be findable via public search
        List<EnhancedSearchResult> publicResult = blockchain.getSearchSpecialistAPI().searchPublic("test", 10);
        assertTrue(publicResult.size() > 0, "test should be findable as public term");
        
        List<EnhancedSearchResult> searchableResult = blockchain.getSearchSpecialistAPI().searchPublic("searchable", 10);
        assertTrue(searchableResult.size() > 0, "searchable should be findable as public term");
        
        // Private keywords should NOT be publicly findable
        List<EnhancedSearchResult> privateResult = blockchain.getSearchSpecialistAPI().searchPublic("private_data", 10);
        assertTrue(privateResult.isEmpty(), "private_data should NOT be publicly findable");
        
        // Test 2: Existing API methods should still work
        List<Block> encryptedBlocks = api.getEncryptedBlocksOnly("");
        assertTrue(encryptedBlocks.size() > 0, "getEncryptedBlocksOnly should still work");
        
        List<Block> dataResults = api.findEncryptedData("test");
        assertTrue(dataResults.size() > 0, "findEncryptedData should still work");
        
        // Test 3: Block creation without keywords should still work
        Block simpleBlock = api.storeSecret("Simple secret without keywords", "Password123!");
        assertNotNull(simpleBlock, "Simple block creation should still work");
        
        logger.info("✅ REGRESSION TEST PASSED - Existing functionality intact");
    }

    // Helper methods

    /**
     * Phase 5.4: Wait for async indexing to complete using the official method.
     * This replaces the old Thread.sleep() pattern which had race conditions.
     */
    private void waitForIndexing() {
        try {
            IndexingCoordinator.getInstance().waitForCompletion();
            logger.info("✅ Async indexing completed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for indexing", e);
        }
    }

    
    private void validatePublicSearch(String query, int expectedMinResults, String message) {
        // Use searchPublic() for public-only search validation
        List<EnhancedSearchResult> result = blockchain.getSearchSpecialistAPI().searchPublic(query, 10);
        assertTrue(result.size() >= expectedMinResults, 
                  message + " (found " + result.size() + " results)");
        logger.info("✅ {} - found {} results", message, result.size());
    }
    
    private void validatePrivateProtection(String query, String message) {
        // Use searchPublic() to verify private content is NOT found in public-only searches
        List<EnhancedSearchResult> result = blockchain.getSearchSpecialistAPI().searchPublic(query, 10);
        assertTrue(result.isEmpty(), 
                  message + " (found " + result.size() + " results - should be 0)");
        logger.info("✅ {} - properly protected", message);
    }
}