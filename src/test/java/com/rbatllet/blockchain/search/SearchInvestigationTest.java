package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.entity.Block;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.security.KeyPair;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Investigation test to understand why smartSearchWithPassword returns 0 results
 * while searchAndDecryptByTerms works correctly
 */
public class SearchInvestigationTest {
    
    private Blockchain blockchain;
    private UserFriendlyEncryptionAPI api;
    private String testPassword;
    
    @BeforeEach
    void setUp() throws Exception {
        blockchain = new Blockchain();
        api = new UserFriendlyEncryptionAPI(blockchain);
        testPassword = "TestPassword123!";
        
        // Create and set default credentials
        KeyPair userKeys = api.createUser("test-user");
        api.setDefaultCredentials("test-user", userKeys);
    }
    
    @Test
    @DisplayName("Deep investigation: smartSearchWithPassword vs searchAndDecryptByTerms")
    void testSearchInvestigation() throws Exception {
        System.out.println("=== SMART SEARCH INVESTIGATION ===");
        
        // 1. CRITICAL FIX: Initialize Search Framework BEFORE storing data
        blockchain.initializeAdvancedSearch(testPassword);
        System.out.println("✅ Search Framework initialized");
        
        // 2. Store data with SearchSpecialistAPI properly initialized
        String[] keywords = {"financial", "test", "investigation"};
        Block storedBlock = api.storeSearchableData("Test financial data for investigation", testPassword, keywords);
        
        System.out.println("✅ Stored block #" + storedBlock.getBlockNumber() + " with keywords: " + String.join(", ", keywords));
        
        // 3. Test both search methods with identical parameters
        System.out.println("\n🔍 Comparing search methods:");
        
        // Method 1: smartSearchWithPassword (failing)
        List<Block> smartResults = api.smartSearchWithPassword("financial", testPassword);
        System.out.println("🧠 smartSearchWithPassword('financial'): " + smartResults.size() + " results");
        
        // Method 2: searchAndDecryptByTerms (working)
        List<Block> decryptResults = api.searchAndDecryptByTerms(new String[]{"financial"}, testPassword, 10);
        System.out.println("🔓 searchAndDecryptByTerms(['financial']): " + decryptResults.size() + " results");
        
        // 4. Analyze the internal search flow
        System.out.println("\n🔍 Internal search flow analysis:");
        
        // Check SearchSpecialistAPI state
        SearchSpecialistAPI searchAPI = blockchain.getSearchSpecialistAPI();
        System.out.println("📊 SearchSpecialistAPI.isReady(): " + searchAPI.isReady());
        
        // 5. Test with different search strategies directly
        if (searchAPI.isReady()) {
            System.out.println("\n🎯 Testing SearchSpecialistAPI methods directly:");
            
            // Test searchIntelligent (used by smartSearchWithPassword)
            List<SearchFrameworkEngine.EnhancedSearchResult> intelligentResults = 
                searchAPI.searchIntelligent("financial", testPassword, 10);
            System.out.println("🧠 searchIntelligent('financial'): " + intelligentResults.size() + " results");
            
            // Test searchSecure
            List<SearchFrameworkEngine.EnhancedSearchResult> secureResults = 
                searchAPI.searchSecure("financial", testPassword, 10);
            System.out.println("🔐 searchSecure('financial'): " + secureResults.size() + " results");
            
            // Test searchSimple
            List<SearchFrameworkEngine.EnhancedSearchResult> simpleResults = 
                searchAPI.searchSimple("financial");
            System.out.println("⚡ searchSimple('financial'): " + simpleResults.size() + " results");
        }
        
        // 6. Check password registry
        System.out.println("\n🔑 Password registry analysis:");
        try {
            // Get password registry stats through blockchain
            System.out.println("📊 Blockchain has search framework initialized: " + 
                             (blockchain.getSearchSpecialistAPI() != null));
        } catch (Exception e) {
            System.out.println("❌ Error accessing password registry: " + e.getMessage());
        }
        
        // 7. Trace smartSearchWithPassword execution path
        System.out.println("\n🔍 Tracing smartSearchWithPassword execution:");
        
        try {
            // Call searchWithAdaptiveDecryption directly (internal method used by smartSearchWithPassword)
            List<Block> adaptiveResults = api.searchWithAdaptiveDecryption("financial", testPassword, 10);
            System.out.println("🔄 searchWithAdaptiveDecryption('financial'): " + adaptiveResults.size() + " results");
        } catch (Exception e) {
            System.out.println("❌ searchWithAdaptiveDecryption failed: " + e.getMessage());
        }
        
        // 8. Check if the issue is password-related
        System.out.println("\n🔐 Password validation test:");
        
        List<Block> wrongPasswordResults = api.searchAndDecryptByTerms(new String[]{"financial"}, "wrongpassword", 10);
        System.out.println("❌ searchAndDecryptByTerms with wrong password: " + wrongPasswordResults.size() + " results");
        
        try {
            List<Block> wrongPasswordSmart = api.smartSearchWithPassword("financial", "wrongpassword");
            System.out.println("❌ smartSearchWithPassword with wrong password: " + wrongPasswordSmart.size() + " results");
        } catch (Exception e) {
            System.out.println("❌ smartSearchWithPassword with wrong password threw: " + e.getMessage());
        }
        
        // 9. Summary and hypothesis
        System.out.println("\n📋 INVESTIGATION SUMMARY:");
        System.out.println("✅ searchAndDecryptByTerms works: " + (decryptResults.size() > 0));
        System.out.println("❌ smartSearchWithPassword fails: " + (smartResults.size() == 0));
        System.out.println("🔍 SearchSpecialistAPI ready: " + searchAPI.isReady());
        
        if (smartResults.size() == 0 && decryptResults.size() > 0) {
            System.out.println("\n💡 HYPOTHESIS: smartSearchWithPassword has a different execution path");
            System.out.println("   that doesn't properly access the stored data or password registry.");
            System.out.println("   The issue is likely in the searchWithAdaptiveDecryption method");
            System.out.println("   or in how it interfaces with the SearchSpecialistAPI.");
        }
        
        System.out.println("\n=== END INVESTIGATION ===");
    }
    
    @Test
    @DisplayName("Minimal reproduction case for smartSearchWithPassword")
    void testMinimalSmartSearchReproduction() throws Exception {
        System.out.println("\n=== MINIMAL REPRODUCTION TEST ===");
        
        // CRITICAL FIX: Initialize SearchSpecialistAPI BEFORE storing data
        blockchain.initializeAdvancedSearch(testPassword);
        
        // Now store data with SearchSpecialistAPI properly initialized
        String[] simpleKeywords = {"test"};
        api.storeSearchableData("Simple test data", testPassword, simpleKeywords);
        
        // Test both methods on identical simple data
        List<Block> smartResults = api.smartSearchWithPassword("test", testPassword);
        List<Block> decryptResults = api.searchAndDecryptByTerms(new String[]{"test"}, testPassword, 10);
        
        System.out.println("📊 Simple case results:");
        System.out.println("   smartSearchWithPassword: " + smartResults.size());
        System.out.println("   searchAndDecryptByTerms: " + decryptResults.size());
        
        // Both methods should now return results (fixing the original 0 results bug)
        // However, they may return different counts due to different search algorithms
        assertTrue(smartResults.size() > 0, "smartSearchWithPassword should return results (was 0 before bug fix)");
        assertTrue(decryptResults.size() > 0, "searchAndDecryptByTerms should return results");
        
        System.out.println("✅ Both methods now return results - the original bug has been fixed!");
        
        System.out.println("=== END MINIMAL REPRODUCTION ===");
    }
}