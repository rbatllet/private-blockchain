package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.List;

/**
 * Debug test to understand how public search works
 */
public class DebugPublicSearchTest {
    
    private Blockchain blockchain;
    private UserFriendlyEncryptionAPI encryptionAPI;
    private SearchSpecialistAPI searchAPI;

    @BeforeEach
    public void setUp() {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();
        
        // Set up encryption credentials
        encryptionAPI = new UserFriendlyEncryptionAPI(blockchain);
        
        // Create user and set default credentials 
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
        System.out.println("🔍 DEBUG: Testing how public search indexing works");
        
        // Store data with explicit public/private separation
        encryptionAPI.storeSearchableDataWithLayers(
            "Patient medical record with diagnosis",
            "testPassword",
            new String[]{"medical"}, // public terms
            new String[]{"hypertension"} // private terms
        );
        
        System.out.println("✅ Data stored with public: [medical], private: [hypertension]");
        
        // Test 1: Search for public term directly
        List<SearchFrameworkEngine.EnhancedSearchResult> results1 = searchAPI.searchAll("medical");
        System.out.println("🔍 Search 'medical': " + results1.size() + " results");
        
        // Test 2: Search for public term with prefix
        List<SearchFrameworkEngine.EnhancedSearchResult> results2 = searchAPI.searchAll("public:medical");
        System.out.println("🔍 Search 'public:medical': " + results2.size() + " results");
        
        // Test 3: Search for private term (should be 0)
        List<SearchFrameworkEngine.EnhancedSearchResult> results3 = searchAPI.searchAll("hypertension");
        System.out.println("🔍 Search 'hypertension': " + results3.size() + " results");
        
        // Test 4: Search using searchSecure (should find both)
        List<SearchFrameworkEngine.EnhancedSearchResult> results4 = searchAPI.searchSecure("medical", "testPassword");
        System.out.println("🔍 SearchSecure 'medical': " + results4.size() + " results");
        
        List<SearchFrameworkEngine.EnhancedSearchResult> results5 = searchAPI.searchSecure("hypertension", "testPassword");
        System.out.println("🔍 SearchSecure 'hypertension': " + results5.size() + " results");
        
        System.out.println("\n🔍 DEBUG: Amb l'opció B, searchAll hauria de trobar TOTS els termes (públics i privats)");
    }
}