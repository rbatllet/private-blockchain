package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.SearchResult;
import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Simple test to debug why INCLUDE_OFFCHAIN searches fail
 */
public class SimpleExhaustiveTest {
    private static final Logger logger = LoggerFactory.getLogger(SimpleExhaustiveTest.class);

    
    @Test
    void debugExhaustiveSearch() throws Exception {
        logger.info("🔍 DEBUG: Starting simple exhaustive search test");
        
        // Setup
        Blockchain blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        SearchFrameworkEngine searchEngine = new SearchFrameworkEngine(
            EncryptionConfig.createHighSecurityConfig());
        
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        String password = "TestPassword123!";
        
        // Create genesis admin
        String publicKeyString = CryptoUtil.publicKeyToString(publicKey);
        boolean keyAuthorized = blockchain.createBootstrapAdmin(publicKeyString, "TestUser");
        logger.info("🔑 Genesis admin created: " + keyAuthorized);
        
        // Create simple block
        boolean blockCreated = blockchain.addBlock(
            "Medical record with patient information", 
            privateKey, publicKey);
        logger.info("📦 Block created: " + blockCreated);
        logger.info("📊 Total blocks: " + blockchain.getBlockCount());
        
        // Index blockchain
        searchEngine.indexBlockchain(blockchain, password, privateKey);
        IndexingCoordinator.getInstance().waitForCompletion();
        logger.info("📋 Blockchain indexed");
        
        // Perform exhaustive search
        logger.info("🔍 Performing exhaustive search for 'medical'");
        SearchResult result = searchEngine.searchExhaustiveOffChain(
            "medical", password, 10);
        
        // Debug output
        logger.info("📊 DEBUG RESULTS:");
        logger.info("   isSuccessful(): " + result.isSuccessful());
        logger.info("   getErrorMessage(): " + result.getErrorMessage());
        logger.info("   getResultCount(): " + result.getResultCount());
        logger.info("   getSearchLevel(): " + result.getSearchLevel());
        logger.info("   getStrategyUsed(): " + result.getStrategyUsed());
        logger.info("   getTotalTimeMs(): " + result.getTotalTimeMs());
        
        if (result.getErrorMessage() != null) {
            logger.error("❌ ERROR: " + result.getErrorMessage());
        }
        
        logger.info("✅ Debug test completed");
    }
}