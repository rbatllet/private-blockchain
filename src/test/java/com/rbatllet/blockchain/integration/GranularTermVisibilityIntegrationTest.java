package com.rbatllet.blockchain.integration;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.search.metadata.TermVisibilityMap;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.util.CryptoUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;
import java.util.List;
import java.util.Set;

/**
 * Integration tests for granular term visibility control
 * Tests the complete workflow from storage to search with granular privacy
 */
public class GranularTermVisibilityIntegrationTest {
    
    private Blockchain blockchain;
    private UserFriendlyEncryptionAPI api;
    private String password;
    
    @BeforeEach
    void setUp() throws Exception {
        // Clean database before each test to ensure isolation - using thread-safe DAO method
        blockchain = new Blockchain();
        // BlockRepository now package-private - use clearAndReinitialize();
        blockchain.getAuthorizedKeyDAO().cleanupTestData();
        
        // Initialize test components
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        api = new UserFriendlyEncryptionAPI(blockchain, "test_user", keyPair);
        password = "test_password_123";
    }
    
    @Test
    void testMedicalRecordGranularPrivacy() throws Exception {
        // Medical record with mixed privacy requirements
        String medicalData = "Patient John Smith diagnosed with diabetes. Treatment with insulin therapy.";
        
        Set<String> allTerms = Set.of("patient", "john", "smith", "diagnosed", "diabetes", "treatment", "insulin", "therapy");
        
        TermVisibilityMap visibility = new TermVisibilityMap()
            .setPublic("patient", "diagnosed", "treatment", "therapy")  // Medical terms - public
            .setPrivate("john", "smith", "diabetes", "insulin");        // Personal/specific - private
        
        // Store with granular control
        Block block = api.storeDataWithGranularTermControl(medicalData, password, allTerms, visibility);
        assertNotNull(block);
        assertNotNull(block.getHash());
        
        // Allow time for indexing
        Thread.sleep(500);
        
        // Test public searches (should find general medical terms)
        List<Block> publicResults = api.searchByTerms(new String[]{"patient"}, null, 10);
        assertTrue(publicResults.size() > 0, "Should find results for public term 'patient'");
        
        publicResults = api.searchByTerms(new String[]{"treatment"}, null, 10);
        assertTrue(publicResults.size() > 0, "Should find results for public term 'treatment'");
        
        // Test private searches without password (should not find sensitive terms)
        List<Block> privateResultsNoPassword = api.searchByTerms(new String[]{"john"}, null, 10);
        assertEquals(0, privateResultsNoPassword.size(), "Should not find private terms without password");
        
        privateResultsNoPassword = api.searchByTerms(new String[]{"diabetes"}, null, 10);
        assertEquals(0, privateResultsNoPassword.size(), "Should not find private terms without password");
        
        // Test private searches with password (should find sensitive terms)
        List<Block> privateResultsWithPassword = api.searchAndDecryptByTerms(new String[]{"john"}, password, 10);
        assertTrue(privateResultsWithPassword.size() > 0, "Should find private terms with password");
        
        privateResultsWithPassword = api.searchAndDecryptByTerms(new String[]{"diabetes"}, password, 10);
        assertTrue(privateResultsWithPassword.size() > 0, "Should find private terms with password");
    }
    
    @Test
    void testFinancialDataGranularPrivacy() throws Exception {
        // Financial transaction with default private, selective public
        String financialData = "SWIFT transfer $25000 from account 987-654-321 to Maria Garcia for property purchase.";
        
        Set<String> allTerms = Set.of("swift", "transfer", "25000", "account", "987-654-321", 
                                    "maria", "garcia", "property", "purchase");
        
        // Default PRIVATE with selective PUBLIC terms
        TermVisibilityMap visibility = new TermVisibilityMap(TermVisibilityMap.VisibilityLevel.PRIVATE)
            .setPublic("swift", "transfer", "property", "purchase");  // Transaction type - public
        
        Block block = api.storeDataWithGranularTermControl(financialData, password, allTerms, visibility);
        assertNotNull(block);
        
        Thread.sleep(500);
        
        // Public searches should find transaction type
        List<Block> publicResults = api.searchByTerms(new String[]{"swift"}, null, 10);
        assertTrue(publicResults.size() > 0, "Should find public term 'swift'");
        
        publicResults = api.searchByTerms(new String[]{"property"}, null, 10);
        assertTrue(publicResults.size() > 0, "Should find public term 'property'");
        
        // Private searches without password should fail for amounts and names
        List<Block> privateNoPassword = api.searchByTerms(new String[]{"25000"}, null, 10);
        assertEquals(0, privateNoPassword.size(), "Should not find private amount without password");
        
        privateNoPassword = api.searchByTerms(new String[]{"maria"}, null, 10);
        assertEquals(0, privateNoPassword.size(), "Should not find private name without password");
        
        // Private searches with password should succeed
        List<Block> privateWithPassword = api.searchAndDecryptByTerms(new String[]{"25000"}, password, 10);
        assertTrue(privateWithPassword.size() > 0, "Should find private amount with password");
        
        privateWithPassword = api.searchAndDecryptByTerms(new String[]{"garcia"}, password, 10);
        assertTrue(privateWithPassword.size() > 0, "Should find private name with password");
    }
    
    @Test
    void testConvenienceMethodSeparatedTerms() throws Exception {
        // Test the convenience method for separated terms
        String data = "Employee Alice Johnson salary $75000 department Engineering.";
        
        String[] publicTerms = {"employee", "salary", "department", "engineering"};
        String[] privateTerms = {"alice", "johnson", "75000"};
        
        Block block = api.storeDataWithSeparatedTerms(data, password, publicTerms, privateTerms);
        assertNotNull(block);
        
        Thread.sleep(500);
        
        // Test public access
        List<Block> publicResults = api.searchByTerms(new String[]{"employee"}, null, 10);
        assertTrue(publicResults.size() > 0, "Should find public term 'employee'");
        
        publicResults = api.searchByTerms(new String[]{"engineering"}, null, 10);
        assertTrue(publicResults.size() > 0, "Should find public term 'engineering'");
        
        // Test private access requires password
        List<Block> privateNoPassword = api.searchByTerms(new String[]{"alice"}, null, 10);
        assertEquals(0, privateNoPassword.size(), "Should not find private name without password");
        
        List<Block> privateWithPassword = api.searchAndDecryptByTerms(new String[]{"alice"}, password, 10);
        assertTrue(privateWithPassword.size() > 0, "Should find private name with password");
    }
    
    @Test
    void testTermVisibilityMapValidation() {
        // Test edge cases and validation
        Set<String> allTerms = Set.of("term1", "term2", "term3");
        
        // Null visibility map should work with defaults
        Block block1 = api.storeDataWithGranularTermControl("data1", password, allTerms, null);
        assertNotNull(block1, "Should handle null visibility map gracefully");
        
        // Empty terms should work
        Block block2 = api.storeDataWithGranularTermControl("data2", password, Set.of(), new TermVisibilityMap());
        assertNotNull(block2, "Should handle empty terms gracefully");
        
        // Null terms should fall back to simple storage
        Block block3 = api.storeDataWithGranularTermControl("data3", password, null, new TermVisibilityMap());
        assertNotNull(block3, "Should handle null terms gracefully");
    }
    
    @Test
    void testComplexScenarioWithMultipleBlocks() throws Exception {
        // Store multiple blocks with different visibility patterns
        
        // Block 1: Medical record
        Set<String> medicalTerms = Set.of("patient", "mary", "wilson", "hypertension", "medication");
        TermVisibilityMap medicalVisibility = new TermVisibilityMap()
            .setPublic("patient", "medication")
            .setPrivate("mary", "wilson", "hypertension");
        
        Block medical = api.storeDataWithGranularTermControl(
            "Patient Mary Wilson treated for hypertension with medication", 
            password, medicalTerms, medicalVisibility);
        
        // Block 2: Financial record  
        Set<String> financialTerms = Set.of("transaction", "bob", "smith", "15000", "mortgage");
        TermVisibilityMap financialVisibility = new TermVisibilityMap()
            .setPublic("transaction", "mortgage")
            .setPrivate("bob", "smith", "15000");
            
        Block financial = api.storeDataWithGranularTermControl(
            "Transaction: Bob Smith mortgage payment $15000", 
            password, financialTerms, financialVisibility);
        
        assertNotNull(medical);
        assertNotNull(financial);
        
        Thread.sleep(1000);
        
        // Cross-domain public searches should work
        List<Block> patientResults = api.searchByTerms(new String[]{"patient"}, null, 10);
        assertTrue(patientResults.size() > 0, "Should find medical public terms");
        
        List<Block> transactionResults = api.searchByTerms(new String[]{"transaction"}, null, 10);
        assertTrue(transactionResults.size() > 0, "Should find financial public terms");
        
        // Private searches should be domain-specific
        List<Block> medicalPrivate = api.searchAndDecryptByTerms(new String[]{"hypertension"}, password, 10);
        assertTrue(medicalPrivate.size() > 0, "Should find medical private terms with password");
        
        List<Block> financialPrivate = api.searchAndDecryptByTerms(new String[]{"15000"}, password, 10);
        assertTrue(financialPrivate.size() > 0, "Should find financial private terms with password");
        
        // Verify block isolation - medical terms shouldn't appear in financial searches
        boolean foundMedicalInFinancial = false;
        for (Block block : financialPrivate) {
            if (block.getHash().equals(medical.getHash())) {
                foundMedicalInFinancial = true;
                break;
            }
        }
        assertFalse(foundMedicalInFinancial, "Medical block should not appear in financial-specific searches");
    }
}