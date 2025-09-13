package com.rbatllet.blockchain.service.base;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base test class for UserFriendlyEncryptionAPI tests.
 * 
 * Provides common setup, test data, and utilities to eliminate duplication
 * across test files and improve maintainability.
 */
public abstract class UserFriendlyEncryptionAPIBaseTest {
    
    // Common test constants
    protected static final String TEST_USERNAME = "testuser";
    protected static final String TEST_PASSWORD = "TestPassword123!";
    protected static final String SECURE_PASSWORD = "SecurePassword123!";
    protected static final String WEAK_PASSWORD = "weak";
    protected static final String LONG_PASSWORD = "VeryLongSecurePasswordForTesting123!@#$%";
    
    // Test data sizes
    protected static final String SMALL_DATA = "Small test data";
    protected static final String MEDIUM_DATA = "Medium test data ".repeat(10);
    protected static final String LARGE_DATA = "Large test data ".repeat(100);
    
    // Core test objects
    protected UserFriendlyEncryptionAPI api;
    protected Blockchain blockchain;
    protected KeyPair testKeyPair;
    
    // Test execution tracking
    private static final AtomicInteger testCounter = new AtomicInteger(0);
    
    /**
     * Standard setup for all UserFriendlyEncryptionAPI tests.
     * Creates fresh blockchain and API instance with test credentials.
     */
    @BeforeEach
    void setUp(TestInfo testInfo) {
        int testNumber = testCounter.incrementAndGet();
        
        // Create blockchain instance
        blockchain = new Blockchain();
        
        // Generate test key pair
        testKeyPair = CryptoUtil.generateKeyPair();
        
        // Create API instance with test credentials
        api = new UserFriendlyEncryptionAPI(blockchain, TEST_USERNAME, testKeyPair);
        
        // Initialize SearchSpecialistAPI to enable storeSecret and storeSearchableData methods
        try {
            blockchain.initializeAdvancedSearch(TEST_PASSWORD);
            blockchain.getSearchSpecialistAPI().initializeWithBlockchain(blockchain, TEST_PASSWORD, testKeyPair.getPrivate());
        } catch (Exception e) {
            System.err.printf("⚠️ Warning: SearchSpecialistAPI initialization failed in test #%d: %s%n", testNumber, e.getMessage());
        }
        
        // Log test execution for debugging
        System.out.printf("🧪 Test #%d: %s.%s%n", 
                         testNumber, 
                         testInfo.getTestClass().map(Class::getSimpleName).orElse("Unknown"),
                         testInfo.getDisplayName());
    }
    
    /**
     * Create a test block with basic data
     * @param data The data for the block
     * @return Created block
     */
    protected Block createTestBlock(String data) {
        return api.storeSecret(data, TEST_PASSWORD);
    }
    
    /**
     * Create multiple test blocks with incrementing data
     * @param count Number of blocks to create
     * @return List of created blocks
     */
    protected List<Block> createTestBlocks(int count) {
        return createTestBlocks(count, "Test data ");
    }
    
    /**
     * Create multiple test blocks with custom prefix
     * @param count Number of blocks to create
     * @param dataPrefix Prefix for test data
     * @return List of created blocks
     */
    protected List<Block> createTestBlocks(int count, String dataPrefix) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> api.storeSecret(dataPrefix + i, TEST_PASSWORD))
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Create a secondary user for multi-user tests
     * @param username Username for the new user
     * @return API instance for the new user
     */
    protected UserFriendlyEncryptionAPI createSecondaryUser(String username) {
        KeyPair userKeyPair = CryptoUtil.generateKeyPair();
        return new UserFriendlyEncryptionAPI(blockchain, username, userKeyPair);
    }
    
    /**
     * Verify blockchain is in healthy state
     * @return true if blockchain is healthy
     */
    protected boolean isBlockchainHealthy() {
        try {
            List<Block> blocks = blockchain.getAllBlocks();
            return blocks != null && !blocks.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get total number of blocks in blockchain
     * @return Block count
     */
    protected int getBlockCount() {
        return blockchain.getAllBlocks().size();
    }
    
    /**
     * Clean up any resources after test execution
     * Override in subclasses if needed
     */
    protected void cleanUp() {
        // Default implementation - override if needed
    }
}