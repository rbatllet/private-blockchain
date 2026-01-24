package com.rbatllet.blockchain.search.metadata;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;
import com.rbatllet.blockchain.util.CryptoUtil;
import java.security.KeyPair;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Comprehensive test suite for newly implemented metadata extraction methods
 * Tests extractOwnerDetails(), extractTechnicalDetails(), and extractValidationInfo()
 */
class MetadataExtractionMethodsTest {
    private static final Logger logger = LoggerFactory.getLogger(MetadataExtractionMethodsTest.class);


    private Blockchain blockchain;
    private KeyPair bootstrapKeyPair;
    private MetadataLayerManager metadataManager;
    private KeyPair testKeyPair;
    private EncryptionConfig encryptionConfig;
    private final String testPassword = "test-password-123";

    @BeforeEach
    void setUp() throws Exception {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();

        // Load bootstrap admin keys (auto-generates if missing - test-only)
        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();

        // Register bootstrap admin in blockchain (RBAC v1.0.6)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        metadataManager = new MetadataLayerManager();
        testKeyPair = CryptoUtil.generateKeyPair();

        // Create encryption config with default settings
        encryptionConfig = new EncryptionConfig();

        // Authorize the test key pair
        String publicKeyString = CryptoUtil.publicKeyToString(testKeyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyString, "test-user", bootstrapKeyPair, UserRole.USER);
    }

    @AfterEach
    void tearDown() {
        if (blockchain != null) {
            blockchain.clearAndReinitialize();
        }
    }

    @Test
    @DisplayName("🔍 Should extract real owner details instead of placeholder")
    void testExtractOwnerDetails() throws Exception {
        // Arrange: Create a block with known properties
        String testContent = "Medical record for patient ID: 12345. Diagnosis: mild headache";
        Block block = blockchain.addBlockAndReturn(testContent, testKeyPair.getPrivate(), testKeyPair.getPublic());
        block.setContentCategory("MEDICAL");
        
        // Act: Generate metadata with private layer
        Set<String> publicTerms = new HashSet<>();
        publicTerms.add("medical");
        publicTerms.add("diagnosis");
        
        Set<String> privateTerms = new HashSet<>();
        privateTerms.add("patient");
        privateTerms.add("record");
        
        BlockMetadataLayers metadata = metadataManager.generateMetadataLayers(
            block, encryptionConfig, testPassword, testKeyPair.getPrivate(), 
            publicTerms, privateTerms, null
        );
        
        // Assert: Verify private metadata has real owner details
        assertNotNull(metadata.getEncryptedPrivateLayer(), "Should have encrypted private layer");
        
        PrivateMetadata privateMetadata = metadataManager.decryptPrivateMetadata(
            metadata.getEncryptedPrivateLayer(), testPassword);
        
        assertNotNull(privateMetadata, "Private metadata should be decryptable");
        
        String ownerDetails = privateMetadata.getOwnerDetails();
        assertNotNull(ownerDetails, "Owner details should not be null");
        assertFalse(ownerDetails.equals("encrypted"), "Should not be placeholder value");
        
        // Verify owner details contain real information
        assertTrue(ownerDetails.contains("signer_hash:"), "Should contain signer hash");
        assertTrue(ownerDetails.contains("created:"), "Should contain creation timestamp");
        assertTrue(ownerDetails.contains("category:medical"), "Should contain content category");
        assertTrue(ownerDetails.contains("content_size:"), "Should contain content size");
        
        logger.info("✅ Owner details extracted: " + ownerDetails);
    }

    @Test
    @DisplayName("🔧 Should extract comprehensive technical details")
    void testExtractTechnicalDetails() throws Exception {
        // Arrange: Create block with off-chain data to test all technical aspects
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeContent.append("Financial transaction ").append(i).append(" amount: $1000.50 ");
        }
        
        Block block = blockchain.addBlockAndReturn(largeContent.toString(), testKeyPair.getPrivate(), testKeyPair.getPublic());
        block.setContentCategory("FINANCE");
        
        // Act: Generate metadata
        Set<String> publicTerms = new HashSet<>();
        publicTerms.add("financial");
        publicTerms.add("transaction");
        
        Set<String> privateTerms = new HashSet<>();
        privateTerms.add("amount");
        privateTerms.add("payment");
        
        BlockMetadataLayers metadata = metadataManager.generateMetadataLayers(
            block, encryptionConfig, testPassword, testKeyPair.getPrivate(), 
            publicTerms, privateTerms, null
        );
        
        PrivateMetadata privateMetadata = metadataManager.decryptPrivateMetadata(
            metadata.getEncryptedPrivateLayer(), testPassword);
        
        // Assert: Verify technical details are comprehensive
        Map<String, Object> techDetails = privateMetadata.getTechnicalDetails();
        assertNotNull(techDetails, "Technical details should not be null");
        assertFalse(techDetails.isEmpty(), "Should not be empty placeholder");
        
        // Verify block technical info
        assertTrue(techDetails.containsKey("block_number"), "Should contain block number");
        assertTrue(techDetails.containsKey("block_hash"), "Should contain block hash");
        assertTrue(techDetails.containsKey("previous_hash"), "Should contain previous hash");
        
        // Verify content analysis details
        assertTrue(techDetails.containsKey("content_length"), "Should contain content length");
        assertTrue(techDetails.containsKey("word_count"), "Should contain word count");
        assertTrue(techDetails.containsKey("keyword_density"), "Should contain keyword density");
        assertTrue(techDetails.containsKey("numerical_values_count"), "Should contain numerical values count");
        
        // Verify encryption details
        assertTrue(techDetails.containsKey("encryption_status"), "Should contain encryption status");
        
        // Verify timestamp details
        assertTrue(techDetails.containsKey("creation_timestamp"), "Should contain creation timestamp");
        assertTrue(techDetails.containsKey("timestamp_epoch"), "Should contain epoch timestamp");
        
        // Verify values are meaningful
        Integer contentLength = (Integer) techDetails.get("content_length");
        assertNotNull(contentLength, "Content length should be available");
        assertTrue(contentLength > 0, "Content length should be positive");
        
        Double keywordDensity = (Double) techDetails.get("keyword_density");
        assertNotNull(keywordDensity, "Keyword density should be calculated");
        assertTrue(keywordDensity >= 0, "Keyword density should be non-negative");
        
        logger.info("✅ Technical details keys: " + techDetails.keySet());
        logger.info("📊 Content length: " + contentLength);
        logger.info("📊 Keyword density: " + String.format("%.2f%%", keywordDensity));
    }

    @Test
    @DisplayName("🛡️ Should extract comprehensive validation information")  
    void testExtractValidationInfo() throws Exception {
        // Arrange: Create encrypted block with signature
        String testContent = "Legal document: Contract #789 between parties A and B";
        Block block = blockchain.addBlockAndReturn(testContent, testKeyPair.getPrivate(), testKeyPair.getPublic());
        block.setContentCategory("LEGAL");
        
        // Act: Generate metadata
        Set<String> publicTerms = new HashSet<>();
        publicTerms.add("legal");
        publicTerms.add("contract");
        
        Set<String> privateTerms = new HashSet<>();
        privateTerms.add("document");
        privateTerms.add("parties");
        
        BlockMetadataLayers metadata = metadataManager.generateMetadataLayers(
            block, encryptionConfig, testPassword, testKeyPair.getPrivate(), 
            publicTerms, privateTerms, null
        );
        
        PrivateMetadata privateMetadata = metadataManager.decryptPrivateMetadata(
            metadata.getEncryptedPrivateLayer(), testPassword);
        
        // Assert: Verify validation info is comprehensive
        Map<String, Object> validationInfo = privateMetadata.getValidationInfo();
        assertNotNull(validationInfo, "Validation info should not be null");
        assertFalse(validationInfo.isEmpty(), "Should not be empty placeholder");
        
        // Verify cryptographic validation
        assertTrue(validationInfo.containsKey("block_hash_algorithm"), "Should contain hash algorithm");
        assertEquals("SHA3-256", validationInfo.get("block_hash_algorithm"), "Should use SHA3-256");
        
        assertTrue(validationInfo.containsKey("block_hash_length"), "Should contain hash length");
        assertTrue(validationInfo.containsKey("block_hash_prefix"), "Should contain hash prefix");
        
        // Verify signature validation
        assertTrue(validationInfo.containsKey("signature_algorithm"), "Should contain signature algorithm");
        assertTrue(validationInfo.containsKey("has_digital_signature"), "Should check digital signature");
        Boolean hasSignature = (Boolean) validationInfo.get("has_digital_signature");
        assertNotNull(hasSignature, "Digital signature check should return boolean");
        
        // Verify chain validation
        assertTrue(validationInfo.containsKey("has_previous_hash"), "Should check previous hash");
        assertTrue(validationInfo.containsKey("block_position"), "Should contain block position");
        
        // Verify content validation
        assertTrue(validationInfo.containsKey("content_integrity_check"), "Should check content integrity");
        assertTrue(validationInfo.containsKey("has_keywords"), "Should check keywords presence");
        assertTrue(validationInfo.containsKey("content_category_assigned"), "Should check category assignment");
        
        // Verify timestamp validation
        assertTrue(validationInfo.containsKey("timestamp_validation"), "Should validate timestamp");
        String timestampValidation = (String) validationInfo.get("timestamp_validation");
        assertNotNull(timestampValidation, "Timestamp validation should return result");
        assertTrue(timestampValidation.equals("valid_range") || timestampValidation.equals("suspicious_timestamp"),
                  "Timestamp validation should be valid_range or suspicious_timestamp");
        
        logger.info("✅ Validation info keys: " + validationInfo.keySet());
        logger.info("🛡️ Hash algorithm: " + validationInfo.get("block_hash_algorithm"));
        logger.info("🛡️ Has digital signature: " + hasSignature);
        logger.info("🛡️ Timestamp validation: " + timestampValidation);
    }

    @Test
    @DisplayName("📦 Should handle blocks with off-chain data in technical details")
    void testTechnicalDetailsWithOffChainData() throws Exception {
        // Arrange: Create large content to trigger off-chain storage (between 512KB and 1MB)
        String largeContent = "Research data point with extensive details value: 123.45 ".repeat(10000); // ~600KB

        Block block = blockchain.addBlockAndReturn(largeContent, testKeyPair.getPrivate(), testKeyPair.getPublic());
        
        // Verify block has off-chain data
        assertTrue(block.hasOffChainData(), "Large block should have off-chain data");
        
        // Act: Generate metadata
        Set<String> publicTerms = new HashSet<>();
        publicTerms.add("research");
        
        Set<String> privateTerms = new HashSet<>();
        privateTerms.add("data");
        
        BlockMetadataLayers metadata = metadataManager.generateMetadataLayers(
            block, encryptionConfig, testPassword, testKeyPair.getPrivate(), 
            publicTerms, privateTerms, null
        );
        
        PrivateMetadata privateMetadata = metadataManager.decryptPrivateMetadata(
            metadata.getEncryptedPrivateLayer(), testPassword);
        
        // Assert: Verify off-chain technical details are included
        Map<String, Object> techDetails = privateMetadata.getTechnicalDetails();
        
        assertTrue(techDetails.containsKey("offchain_file_path"), "Should contain off-chain file path");
        assertTrue(techDetails.containsKey("offchain_file_size"), "Should contain off-chain file size");
        assertTrue(techDetails.containsKey("offchain_content_type"), "Should contain off-chain content type");
        assertTrue(techDetails.containsKey("offchain_data_hash"), "Should contain off-chain data hash");
        
        // Verify values are meaningful
        String filePath = (String) techDetails.get("offchain_file_path");
        assertNotNull(filePath, "Off-chain file path should be available");
        assertFalse(filePath.trim().isEmpty(), "Off-chain file path should not be empty");
        
        Object fileSizeObj = techDetails.get("offchain_file_size");
        Long fileSize = (fileSizeObj instanceof Integer) ? ((Integer) fileSizeObj).longValue() : (Long) fileSizeObj;
        assertNotNull(fileSize, "Off-chain file size should be available");
        assertTrue(fileSize > 0, "Off-chain file size should be positive");
        
        logger.info("✅ Off-chain file path: " + filePath);
        logger.info("📦 Off-chain file size: " + fileSize + " bytes");
    }

    @Test
    @DisplayName("⚡ Should generate metadata efficiently for multiple blocks")
    void testMetadataExtractionPerformance() throws Exception {
        // Arrange: Create multiple blocks with different characteristics
        int numBlocks = 10;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numBlocks; i++) {
            String content = "Test block " + i + " with content length variation " + "x".repeat(i * 100);
            Block block = blockchain.addBlockAndReturn(content, testKeyPair.getPrivate(), testKeyPair.getPublic());
            block.setContentCategory("TEST");
            
            // Act: Generate metadata for each block
            Set<String> publicTerms = new HashSet<>();
            publicTerms.add("test");
            
            Set<String> privateTerms = new HashSet<>();
            privateTerms.add("block");
            
            BlockMetadataLayers metadata = metadataManager.generateMetadataLayers(
                block, encryptionConfig, testPassword, testKeyPair.getPrivate(), 
                publicTerms, privateTerms, null
            );
            
            // Verify metadata was generated
            assertNotNull(metadata.getEncryptedPrivateLayer(), "Should have encrypted private layer");
            
            PrivateMetadata privateMetadata = metadataManager.decryptPrivateMetadata(
                metadata.getEncryptedPrivateLayer(), testPassword);
            
            // Assert: Verify all extraction methods work for each block
            assertNotNull(privateMetadata.getOwnerDetails(), "Owner details should be extracted");
            assertFalse(privateMetadata.getOwnerDetails().equals("encrypted"), "Should not be placeholder");
            
            assertNotNull(privateMetadata.getTechnicalDetails(), "Technical details should be extracted");
            assertFalse(privateMetadata.getTechnicalDetails().isEmpty(), "Should not be empty");
            
            assertNotNull(privateMetadata.getValidationInfo(), "Validation info should be extracted");
            assertFalse(privateMetadata.getValidationInfo().isEmpty(), "Should not be empty");
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double avgTimePerBlock = totalTime / (double) numBlocks;
        
        logger.info("✅ Generated metadata for " + numBlocks + " blocks in " + totalTime + "ms");
        logger.info("⚡ Average time per block: " + String.format("%.2f", avgTimePerBlock) + "ms");
        
        // Performance assertion - should be reasonable
        assertTrue(avgTimePerBlock < 500, "Average metadata generation should be under 500ms per block");
    }

    @Test
    @DisplayName("🔒 Should not leak sensitive information in owner details")
    void testOwnerDetailsPrivacySafety() throws Exception {
        // Arrange: Create block with sensitive content
        String sensitiveContent = "Private key: abc123xyz789, SSN: 123-45-6789, Password: secret123";
        Block block = blockchain.addBlockAndReturn(sensitiveContent, testKeyPair.getPrivate(), testKeyPair.getPublic());
        
        // Act: Generate metadata
        Set<String> publicTerms = new HashSet<>();
        publicTerms.add("secure");
        
        Set<String> privateTerms = new HashSet<>();
        privateTerms.add("private");
        
        BlockMetadataLayers metadata = metadataManager.generateMetadataLayers(
            block, encryptionConfig, testPassword, testKeyPair.getPrivate(), 
            publicTerms, privateTerms, null
        );
        
        PrivateMetadata privateMetadata = metadataManager.decryptPrivateMetadata(
            metadata.getEncryptedPrivateLayer(), testPassword);
        
        // Assert: Verify owner details don't leak sensitive information
        String ownerDetails = privateMetadata.getOwnerDetails();
        
        // Should not contain sensitive data from content
        assertFalse(ownerDetails.contains("abc123xyz789"), "Should not leak private key");
        assertFalse(ownerDetails.contains("123-45-6789"), "Should not leak SSN");
        assertFalse(ownerDetails.contains("secret123"), "Should not leak password");
        
        // Should only contain limited signer hash (first 16 characters)
        assertTrue(ownerDetails.contains("signer_hash:"), "Should contain signer hash");
        String[] parts = ownerDetails.split(",");
        for (String part : parts) {
            if (part.startsWith("signer_hash:")) {
                String hashPart = part.substring("signer_hash:".length());
                assertEquals(16, hashPart.length(), "Signer hash should be limited to 16 characters");
                break;
            }
        }
        
        logger.info("✅ Owner details privacy-safe: " + ownerDetails);
    }
}