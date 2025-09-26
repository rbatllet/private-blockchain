package com.rbatllet.blockchain.search;

import static org.junit.jupiter.api.Assertions.*;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
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
 * CRITICAL FIX: Corregeix el problema de discrepància entre indexació i cerca
 * 
 * Aquest test demostra i valida la solució al problema identificat:
 * - Keywords sense prefix "public:" no es poden cercar amb searchSimple()
 * - Keywords amb prefix "public:" es poden cercar correctament
 * 
 * La solució és utilitzar el prefix "public:" per keywords que han de ser
 * searchables públicament (sense password).
 */
@DisplayName("🔧 Search Integrity Fix Validation")
class SearchIntegrityFixTest {

    private static final Logger logger = LoggerFactory.getLogger(SearchIntegrityFixTest.class);
    
    private Blockchain blockchain;
    private UserFriendlyEncryptionAPI api;
    private KeyPair testKeyPair;
    private String testUsername = "searchFixUser";

    @BeforeEach
    void setUp() {
        IndexingCoordinator.getInstance().enableTestMode();

        blockchain = new Blockchain();
        // Clean database before each test to ensure test isolation
        blockchain.clearAndReinitialize();
        testKeyPair = CryptoUtil.generateKeyPair();
        api = new UserFriendlyEncryptionAPI(blockchain, testUsername, testKeyPair);

        try {
            blockchain.initializeAdvancedSearch("password123");
            blockchain.getSearchSpecialistAPI().initializeWithBlockchain(blockchain, "password123", testKeyPair.getPrivate());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize search: " + e.getMessage(), e);
        }
    }

    @AfterEach
    void tearDown() {
        try {
            IndexingCoordinator.getInstance().shutdown();
            IndexingCoordinator.getInstance().disableTestMode();
            if (blockchain != null) {
                // Clean database after each test to ensure test isolation
                blockchain.clearAndReinitialize();
                blockchain.shutdown();
            }
        } catch (Exception e) {
            logger.warn("Teardown warning: {}", e.getMessage());
        }
    }

    @Test
    @DisplayName("BEFORE FIX: Demonstrates the original problem")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void demonstrateOriginalProblem() {
        logger.info("🔍 DEMONSTRATING ORIGINAL PROBLEM: Keywords without 'public:' prefix");
        
        // Create blocks with keywords WITHOUT "public:" prefix (original problem)
        String[] originalMedicalTerms = {
            "medical",      // ← This won't be publicly searchable!
            "patient",
            "hypertension", 
            "lisinopril",
            "PATIENT_001"
        };
        
        Block problemBlock = api.storeSearchableData(
            "Medical patient data with non-public keywords",
            "password123",
            originalMedicalTerms
        );
        
        assertNotNull(problemBlock, "Block should be created successfully");
        
        // Wait for indexing
        waitForIndexing();
        
        // Try to search for "medical" - this WILL FAIL with original implementation
        List<EnhancedSearchResult> searchResult = blockchain.getSearchSpecialistAPI().searchSimple("medical", 10);
        
        logger.info("🔍 Search for 'medical' (without prefix) returned {} results", 
                   searchResult.size());
        
        if (searchResult.isEmpty()) {
            logger.warn("❌ CONFIRMED: Original problem exists - 'medical' keyword not publicly searchable");
            logger.warn("💡 This is because keywords without 'public:' prefix go to private metadata layer");
        } else {
            logger.info("✅ Unexpected: 'medical' keyword was found (problem may be fixed elsewhere)");
        }
        
        // However, findEncryptedData() SHOULD work (uses password-based search)
        List<Block> encryptedResults = api.findEncryptedData("medical");
        logger.info("🔍 findEncryptedData('medical') returned {} results", encryptedResults.size());
        
        assertTrue(encryptedResults.size() > 0, 
                  "findEncryptedData should find the block even without public prefix");
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
            "password123", 
            publicTerms,
            privateTerms
        );
        
        assertNotNull(fixedBlock, "Block should be created successfully");
        
        // Wait for indexing
        waitForIndexing();
        
        // Now search for "medical" (the API internally handles the public: prefix)
        List<EnhancedSearchResult> publicSearchResult = blockchain.getSearchSpecialistAPI().searchSimple("medical", 10);
        
        logger.info("🔍 Search for 'medical' (public term) returned {} results", 
                   publicSearchResult.size());
        
        assertTrue(publicSearchResult.size() > 0, 
                  "CRITICAL: 'medical' should be findable via searchSimple() when stored as public term");
        
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
        
        Block medicalBlock = api.storeSearchableDataWithLayers(medicalData, "password123", medicalPublicTerms, medicalPrivateTerms);
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
        
        Block financialBlock = api.storeSearchableDataWithLayers(financialData, "password123", financialPublicTerms, financialPrivateTerms);
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
        
        Block legalBlock = api.storeSearchableDataWithLayers(legalData, "password123", legalPublicTerms, legalPrivateTerms);
        assertNotNull(legalBlock, "Legal block should be created");
        
        // Wait for indexing to complete
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
        
        // TEST: Verify private data IS accessible via authenticated search
        logger.info("📋 Testing authenticated private data access...");
        
        List<Block> medicalResults = api.findEncryptedData("hypertension");
        assertTrue(medicalResults.size() > 0, "Private medical data should be accessible via authenticated search");
        
        List<Block> financialResults = api.findEncryptedData("balance");
        assertTrue(financialResults.size() > 0, "Private financial data should be accessible via authenticated search");
        
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
        
        Block mixedBlock = api.storeSearchableDataWithLayers("Mixed keyword test data", "password123", publicKeywords, privateKeywords);
        assertNotNull(mixedBlock, "Mixed keyword block should be created");
        
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
        Block simpleBlock = api.storeSecret("Simple secret without keywords", "password123");
        assertNotNull(simpleBlock, "Simple block creation should still work");
        
        logger.info("✅ REGRESSION TEST PASSED - Existing functionality intact");
    }

    // Helper methods
    
    private void waitForIndexing() {
        try {
            Thread.sleep(2000); // Wait for indexing
            for (int i = 0; i < 10; i++) {
                if (IndexingCoordinator.getInstance().isIndexing()) {
                    Thread.sleep(500);
                } else {
                    break;
                }
            }
            Thread.sleep(500); // Grace period
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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