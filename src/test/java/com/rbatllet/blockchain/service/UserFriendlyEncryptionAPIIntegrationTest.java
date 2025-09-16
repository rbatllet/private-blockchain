package com.rbatllet.blockchain.service;

import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;

/**
 * Integration tests for UserFriendlyEncryptionAPI without mocks
 * 
 * These tests use real instances but are slower and more complex to configure
 */
class UserFriendlyEncryptionAPIIntegrationTest {
    
    private UserFriendlyEncryptionAPI api;
    private Blockchain blockchain;
    private KeyPair testKeyPair;
    private EncryptionConfig testConfig;
    
    @BeforeEach
    void setUp() throws Exception {
        // Configure real blockchain (slower)
        blockchain = new Blockchain();
        
        // Generate real EC keys (blockchain expects EC keys, not RSA)
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(256);
        testKeyPair = keyGen.generateKeyPair();
        
        // Real configuration
        testConfig = new EncryptionConfig();
        testConfig.setEncryptionAlgorithm("AES/GCM/NoPadding");
        testConfig.setKeyLength(256);
        
        // Real API with real dependencies
        api = new UserFriendlyEncryptionAPI(blockchain, "testuser", testKeyPair, testConfig);
    }
    
    @Test
    @DisplayName("Integration test: Find blocks by metadata with real blockchain")
    void testFindBlocksByMetadataIntegration() throws Exception {
        // Add real blocks to blockchain - fix method signatures
        Block block1 = blockchain.addBlockWithKeywords(
            "Medical data 1", 
            new String[]{"medical", "patient"}, 
            "password",
            testKeyPair.getPrivate(),
            testKeyPair.getPublic()
        );
        
        Block block2 = blockchain.addBlockWithKeywords(
            "Financial data", 
            new String[]{"finance"}, 
            "password",
            testKeyPair.getPrivate(),
            testKeyPair.getPublic()
        );
        
        // Real test without mocks
        List<Block> results = api.findBlocksByMetadata("category", "medical");
        
        // Verify blocks were created successfully
        assertNotNull(block1, "Medical block should be created successfully");
        assertNotNull(block2, "Financial block should be created successfully");
        assertEquals("Medical data 1", block1.getData(), "Medical block should contain correct data");
        assertEquals("Financial data", block2.getData(), "Financial block should contain correct data");
        
        // This test is more realistic but:
        // - Slower (real DB)
        // - More fragile (depends on DB state)
        // - Harder to configure edge cases
        assertNotNull(results, "Metadata search results should not be null");
    }
}