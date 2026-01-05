package com.rbatllet.blockchain.util;

import java.security.PrivateKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.search.SearchFrameworkEngine;

/**
 * Utility class for common test database operations and setup/teardown.
 * 
 * This class centralizes common test functionality to eliminate code duplication
 * and ensure consistent test isolation across all blockchain tests.
 * 
 * @author rbatllet
 * @since 1.0
 */
public class TestDatabaseUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(TestDatabaseUtils.class);
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private TestDatabaseUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Clear the database and reset all global state to ensure test isolation.
     * 
     * This method performs a complete cleanup of:
     * - Database content (blocks, keys, etc.)
     * - SearchFrameworkEngine global state
     * - IndexingCoordinator state and execution tracking
     * 
     * Should be called in @BeforeEach and @AfterEach to ensure test isolation.
     */
    public static void clearDatabase() {
        try {
            // Use the standard Blockchain clearAndReinitialize method
            Blockchain tempBlockchain = new Blockchain();
            tempBlockchain.clearAndReinitialize();
            
            // Clear global search engine state to prevent cross-test contamination
            SearchFrameworkEngine.resetGlobalState();
            
            // Clear IndexingCoordinator state to prevent duplicate indexing prevention
            IndexingCoordinator.getInstance().reset();
            
            // Small delay to ensure database operations complete
            Thread.sleep(50);
        } catch (Exception e) {
            logger.warn("Warning: Failed to clear database: {}", e.getMessage());
        }
    }
    
    /**
     * Enable test mode for IndexingCoordinator to prevent infinite loops and timing issues.
     * 
     * Should be called at the beginning of test setup.
     */
    public static void enableTestMode() {
        IndexingCoordinator.getInstance().enableTestMode();
    }
    
    /**
     * Disable test mode and perform cleanup of IndexingCoordinator.
     * 
     * Should be called at the end of test teardown.
     */
    public static void disableTestMode() {
        try {
            IndexingCoordinator.getInstance().shutdown();
        } catch (Exception e) {
            logger.warn("IndexingCoordinator shutdown warning: {}", e.getMessage());
        }
        IndexingCoordinator.getInstance().disableTestMode();
        IndexingCoordinator.getInstance().reset();
    }
    
    /**
     * Complete test setup: clear database and enable test mode.
     * 
     * Convenience method that combines the most common setup operations.
     * Use in @BeforeEach for standard test preparation.
     */
    public static void setupTest() {
        clearDatabase();
        enableTestMode();
    }
    
    /**
     * Complete test teardown: disable test mode and clear database.
     * 
     * Convenience method that combines the most common teardown operations.
     * Use in @AfterEach for standard test cleanup.
     */
    public static void teardownTest() {
        disableTestMode();
        clearDatabase();
    }
    
    /**
     * Create a clean blockchain instance for testing.
     * 
     * @return A new Blockchain instance with a clean database
     */
    public static Blockchain createCleanBlockchain() {
        clearDatabase();
        return new Blockchain();
    }
    
    /**
     * Initialize SearchSpecialistAPI with standard test configuration.
     * 
     * @param blockchain The blockchain instance to initialize
     * @param password The password for advanced search
     * @param privateKey The private key for search initialization
     * @throws RuntimeException if initialization fails
     */
    public static void initializeSearchAPI(Blockchain blockchain, String password, PrivateKey privateKey) {
        try {
            blockchain.initializeAdvancedSearch(password);
            blockchain.getSearchSpecialistAPI().initializeWithBlockchain(blockchain, password, privateKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SearchSpecialistAPI in test setup", e);
        }
    }
    
    /**
     * Graceful shutdown of a blockchain instance with error handling.
     * 
     * @param blockchain The blockchain instance to shutdown (can be null)
     */
    public static void shutdownBlockchain(Blockchain blockchain) {
        if (blockchain != null) {
            try {
                blockchain.shutdown();
            } catch (Exception e) {
                logger.warn("Blockchain shutdown warning: {}", e.getMessage());
            }
        }
    }
}