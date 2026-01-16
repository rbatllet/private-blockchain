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

/**
 * Simple test to debug why INCLUDE_ENCRYPTED searches fail
 */
public class SimpleExhaustiveTest {
    
    @Test
    void debugExhaustiveSearch() throws Exception {
        System.out.println("🔍 DEBUG: Starting simple exhaustive search test");
        
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
        System.out.println("🔑 Genesis admin created: " + keyAuthorized);
        
        // Create simple block
        boolean blockCreated = blockchain.addBlock(
            "Medical record with patient information", 
            privateKey, publicKey);
        System.out.println("📦 Block created: " + blockCreated);
        System.out.println("📊 Total blocks: " + blockchain.getBlockCount());
        
        // Index blockchain
        searchEngine.indexBlockchain(blockchain, password, privateKey);
        IndexingCoordinator.getInstance().waitForCompletion();
        System.out.println("📋 Blockchain indexed");
        
        // Perform exhaustive search
        System.out.println("🔍 Performing exhaustive search for 'medical'");
        SearchResult result = searchEngine.searchExhaustiveOffChain(
            "medical", password, privateKey, 10);
        
        // Debug output
        System.out.println("📊 DEBUG RESULTS:");
        System.out.println("   isSuccessful(): " + result.isSuccessful());
        System.out.println("   getErrorMessage(): " + result.getErrorMessage());
        System.out.println("   getResultCount(): " + result.getResultCount());
        System.out.println("   getSearchLevel(): " + result.getSearchLevel());
        System.out.println("   getStrategyUsed(): " + result.getStrategyUsed());
        System.out.println("   getTotalTimeMs(): " + result.getTotalTimeMs());
        
        if (result.getErrorMessage() != null) {
            System.err.println("❌ ERROR: " + result.getErrorMessage());
        }
        
        System.out.println("✅ Debug test completed");
    }
}