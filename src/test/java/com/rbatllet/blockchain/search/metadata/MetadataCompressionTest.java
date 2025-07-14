package com.rbatllet.blockchain.search.metadata;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Test class for metadata compression and encryption integration
 * Validates that metadata compression works with the Advanced Search architecture
 */
public class MetadataCompressionTest {
    
    private MetadataLayerManager metadataManager;
    private KeyPair keyPair;
    private String testPassword;
    
    @BeforeEach
    void setUp() {
        metadataManager = new MetadataLayerManager();
        keyPair = CryptoUtil.generateKeyPair();
        testPassword = "SecureTestPassword123!";
    }
    
    @Test
    @DisplayName("Medical record metadata should compress and encrypt properly")
    void testMedicalRecordMetadataCompression() {
        // Create a medical record block with detailed content
        Block medicalBlock = new Block();
        medicalBlock.setBlockNumber(1L);
        medicalBlock.setHash("medical_block_hash_12345");
        medicalBlock.setTimestamp(LocalDateTime.now());
        medicalBlock.setData("MEDICAL RECORD - CONFIDENTIAL\n" +
                           "Patient: Jane Smith\n" +
                           "DOB: 1985-03-22\n" +
                           "Medical Record Number: MRN-2024-789456\n" +
                           "Insurance ID: INS-987654321\n" +
                           "Phone: +1-555-123-4567\n" +
                           "Email: jane.smith@email.com\n" +
                           "\n" +
                           "Chief Complaint: Annual physical examination\n" +
                           "\n" +
                           "Vital Signs:\n" +
                           "- Blood Pressure: 120/80 mmHg\n" +
                           "- Heart Rate: 72 bpm\n" +
                           "- Temperature: 98.6°F (37.0°C)\n" +
                           "- Weight: 68 kg (150 lbs)\n" +
                           "- Height: 165 cm (5'5\")\n" +
                           "- BMI: 25.0\n" +
                           "\n" +
                           "Current Medications:\n" +
                           "- Lisinopril 10mg daily for hypertension\n" +
                           "- Metformin 500mg twice daily for Type 2 diabetes\n" +
                           "- Vitamin D3 2000 IU daily\n" +
                           "\n" +
                           "Lab Results (Fasting):\n" +
                           "- HbA1c: 6.8% (target <7.0%)\n" +
                           "- Glucose: 95 mg/dL (normal)\n" +
                           "- Cholesterol Total: 180 mg/dL (normal)\n" +
                           "- LDL: 105 mg/dL (borderline)\n" +
                           "- HDL: 55 mg/dL (normal)\n" +
                           "- Triglycerides: 150 mg/dL (borderline)\n" +
                           "\n" +
                           "Assessment: Well-controlled diabetes and hypertension\n" +
                           "Plan: Continue current medications, follow-up in 6 months\n" +
                           "       Dietary counseling recommended\n" +
                           "       Annual eye exam scheduled\n" +
                           "\n" +
                           "Physician: Dr. Sarah Johnson, MD\n" +
                           "License: MD-12345-CA\n" +
                           "Date: 2024-01-15 14:30:00\n" +
                           "Next Appointment: 2024-07-15 14:30:00");
        
        // Test with compression enabled
        EncryptionConfig compressedConfig = EncryptionConfig.createBalancedConfig();
        assertTrue(compressedConfig.isEnableCompression(), "Balanced config should have compression enabled");
        
        // Generate metadata layers with specific medical search terms
        Set<String> publicTerms = Set.of("medical", "patient", "report");
        Set<String> privateTerms = Set.of("diabetes", "hypertension", "prescription", "medication");
        BlockMetadataLayers layers = metadataManager.generateMetadataLayers(
            medicalBlock, compressedConfig, testPassword, keyPair.getPrivate(), publicTerms, privateTerms, null);
        
        assertNotNull(layers);
        assertNotNull(layers.getPublicLayer());
        assertNotNull(layers.getEncryptedPrivateLayer());
        
        // Verify public layer contains appropriate metadata structure
        PublicMetadata publicLayer = layers.getPublicLayer();
        assertNotNull(publicLayer.getContentType(), "Content type should be detected");
        assertNotNull(publicLayer.getBlockCategory(), "Block category should be determined");
        
        // Decrypt and verify private metadata
        PrivateMetadata decryptedPrivate = metadataManager.decryptPrivateMetadata(
            layers.getEncryptedPrivateLayer(), testPassword);
        
        assertNotNull(decryptedPrivate);
        assertNotNull(decryptedPrivate.getSpecificKeywords());
        assertTrue(decryptedPrivate.getSpecificKeywords().size() > 0);
        
        System.out.println("✅ Medical metadata compression test completed");
        System.out.println("📊 Public keywords: " + publicLayer.getGeneralKeywords().size());
        System.out.println("🔒 Private keywords: " + decryptedPrivate.getSpecificKeywords().size());
    }
    
    @Test
    @DisplayName("Financial transaction metadata should compress efficiently")
    void testFinancialTransactionMetadataCompression() {
        Block financialBlock = new Block();
        financialBlock.setBlockNumber(2L);
        financialBlock.setHash("financial_block_hash_67890");
        financialBlock.setTimestamp(LocalDateTime.now());
        financialBlock.setData("FINANCIAL TRANSACTION - HIGHLY CONFIDENTIAL\n" +
                             "\n" +
                             "Transaction ID: TXN-2024-987654\n" +
                             "Date: 2024-01-15 14:30:22\n" +
                             "Type: Wire Transfer (International)\n" +
                             "\n" +
                             "Account Details:\n" +
                             "- Account Number: ****-****-****-3456\n" +
                             "- Account Holder: ACME Corporation Ltd.\n" +
                             "- IBAN: GB29 NWBK 6016 1331 9268 19\n" +
                             "- SWIFT: NWBKGB2L\n" +
                             "- Bank: NatWest Bank\n" +
                             "\n" +
                             "Transaction Details:\n" +
                             "- Amount: €250,000.00\n" +
                             "- Currency: EUR\n" +
                             "- Exchange Rate: 1.0845 EUR/USD\n" +
                             "- USD Equivalent: $271,125.00\n" +
                             "- Fees: €150.00\n" +
                             "\n" +
                             "Counterparty:\n" +
                             "- Name: Global Tech Solutions S.A.\n" +
                             "- Account: FR14 2004 1010 0505 0001 3M02 606\n" +
                             "- SWIFT: BNPAFRPPXXX\n" +
                             "- Bank: BNP Paribas\n" +
                             "\n" +
                             "Purpose: Software licensing and development services Q1 2024\n" +
                             "Reference: Contract GTS-2024-001, Invoice INV-2024-0123\n" +
                             "\n" +
                             "Risk Assessment:\n" +
                             "- AML Score: Low Risk (Score: 15/100)\n" +
                             "- Sanctions Check: Clear\n" +
                             "- PEP Check: Negative\n" +
                             "- Country Risk: Low (UK to France)\n" +
                             "\n" +
                             "Authorized by: John Manager (CEO)\n" +
                             "Employee ID: EMP-2024-001\n" +
                             "Processed by: Finance Dept.\n" +
                             "Processing Time: 14:30:22 - 14:32:15\n" +
                             "Status: Completed\n" +
                             "Confirmation: CONF-2024-789456123");
        
        EncryptionConfig compressedConfig = EncryptionConfig.createBalancedConfig();
        
        // Generate metadata layers with specific financial search terms
        Set<String> publicTerms = Set.of("transaction", "transfer", "financial");
        Set<String> privateTerms = Set.of("271125", "euros", "swift", "employee", "confirmation", "processing", "authorized", "empid");
        BlockMetadataLayers layers = metadataManager.generateMetadataLayers(
            financialBlock, compressedConfig, testPassword, keyPair.getPrivate(), publicTerms, privateTerms, null);
        
        assertNotNull(layers);
        
        // Verify metadata structure
        PublicMetadata publicLayer = layers.getPublicLayer();
        assertNotNull(publicLayer.getContentType(), "Content type should be detected");
        assertNotNull(publicLayer.getBlockCategory(), "Block category should be determined");
        
        // Test compression effectiveness on financial data
        PrivateMetadata decryptedPrivate = metadataManager.decryptPrivateMetadata(
            layers.getEncryptedPrivateLayer(), testPassword);
        
        assertNotNull(decryptedPrivate);
        assertTrue(decryptedPrivate.getSpecificKeywords().size() > 5, 
                  "Financial data should extract multiple sensitive keywords");
        
        System.out.println("✅ Financial metadata compression test completed");
    }
    
    @Test
    @DisplayName("Compression should be backwards compatible with uncompressed data")
    void testBackwardsCompatibility() {
        Block testBlock = new Block();
        testBlock.setBlockNumber(3L);
        testBlock.setHash("test_block_hash");
        testBlock.setTimestamp(LocalDateTime.now());
        testBlock.setData("Simple test data for backwards compatibility testing");
        
        // Simulate legacy uncompressed metadata by directly encrypting JSON
        PrivateMetadata originalMetadata = new PrivateMetadata();
        originalMetadata.setDetailedCategory("TEST_DETAILED");
        originalMetadata.setExactTimestamp(LocalDateTime.now());
        
        // Create legacy encrypted metadata (without compression)
        String jsonMetadata = "{\"detailedCategory\":\"TEST_DETAILED\",\"metadataVersion\":\"1.0\"}";
        String legacyEncryptedMetadata = CryptoUtil.encryptWithGCM(jsonMetadata, testPassword);
        
        // Should be able to decrypt legacy data
        PrivateMetadata decryptedLegacy = metadataManager.decryptPrivateMetadata(
            legacyEncryptedMetadata, testPassword);
        
        assertNotNull(decryptedLegacy);
        assertEquals("TEST_DETAILED", decryptedLegacy.getDetailedCategory());
        
        System.out.println("✅ Backwards compatibility test passed");
    }
    
    @Test
    @DisplayName("Compression should handle different data sizes appropriately")
    void testCompressionWithDifferentDataSizes() {
        EncryptionConfig compressedConfig = EncryptionConfig.createBalancedConfig();
        
        // Small data
        Block smallBlock = createTestBlock("Small data", "This is small test data.");
        BlockMetadataLayers smallLayers = metadataManager.generateMetadataLayers(
            smallBlock, compressedConfig, testPassword, keyPair.getPrivate());
        
        // Large repetitive data
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            largeData.append("Repetitive medical record entry number ").append(i)
                     .append(" with similar structured content. ");
        }
        
        Block largeBlock = createTestBlock("Large data", largeData.toString());
        BlockMetadataLayers largeLayers = metadataManager.generateMetadataLayers(
            largeBlock, compressedConfig, testPassword, keyPair.getPrivate());
        
        // Both should work correctly
        assertNotNull(smallLayers.getEncryptedPrivateLayer());
        assertNotNull(largeLayers.getEncryptedPrivateLayer());
        
        PrivateMetadata smallDecrypted = metadataManager.decryptPrivateMetadata(
            smallLayers.getEncryptedPrivateLayer(), testPassword);
        PrivateMetadata largeDecrypted = metadataManager.decryptPrivateMetadata(
            largeLayers.getEncryptedPrivateLayer(), testPassword);
        
        assertNotNull(smallDecrypted);
        assertNotNull(largeDecrypted);
        
        System.out.println("✅ Variable data size compression test completed");
    }
    
    @Test
    @DisplayName("Performance config should enable compression")
    void testPerformanceConfigCompression() {
        EncryptionConfig performanceConfig = EncryptionConfig.createPerformanceConfig();
        assertTrue(performanceConfig.isEnableCompression(), 
                  "Performance config should enable compression for efficiency");
        
        Block testBlock = createTestBlock("Performance test", 
                                         "This data should be compressed for performance optimization.");
        
        BlockMetadataLayers layers = metadataManager.generateMetadataLayers(
            testBlock, performanceConfig, testPassword, keyPair.getPrivate());
        
        assertNotNull(layers);
        
        PrivateMetadata decrypted = metadataManager.decryptPrivateMetadata(
            layers.getEncryptedPrivateLayer(), testPassword);
        
        assertNotNull(decrypted);
        
        System.out.println("✅ Performance config compression test completed");
    }
    
    private Block createTestBlock(String hash, String data) {
        Block block = new Block();
        block.setBlockNumber(System.currentTimeMillis());
        block.setHash(hash);
        block.setTimestamp(LocalDateTime.now());
        block.setData(data);
        return block;
    }
}