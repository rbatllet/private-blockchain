package com.rbatllet.blockchain.service.utils;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Common assertion utilities for UserFriendlyEncryptionAPI tests.
 * 
 * Provides standardized assertions to eliminate duplication and
 * improve consistency across test files.
 */
public class ApiAssertions {
    
    /**
     * Assert that operation result is not null
     * @param result The result to check
     * @param message Custom assertion message
     */
    public static void assertResultNotNull(Object result, String message) {
        assertNotNull(result, message != null ? message : "Operation result should not be null");
    }
    
    /**
     * Assert that operation result is not null with default message
     * @param result The result to check
     */
    public static void assertResultNotNull(Object result) {
        assertResultNotNull(result, null);
    }
    
    /**
     * Assert that a block was created successfully
     * @param block The block to validate
     */
    public static void assertBlockCreated(Block block) {
        assertNotNull(block, "Block should be created");
        assertNotNull(block.getId(), "Block should have ID");
        assertNotNull(block.getHash(), "Block should have hash");
        assertNotNull(block.getData(), "Block should have data");
        assertNotNull(block.getTimestamp(), "Block should have timestamp");
    }
    
    /**
     * Assert that a block contains expected data
     * @param block The block to check
     * @param expectedData Expected data content
     */
    public static void assertBlockContainsData(Block block, String expectedData) {
        assertNotNull(block, "Block should not be null");
        assertEquals(expectedData, block.getData(), "Block should contain expected data");
    }
    
    /**
     * Assert that blockchain is in healthy state
     * @param blockchain The blockchain to check
     */
    public static void assertBlockchainHealthy(Blockchain blockchain) {
        assertNotNull(blockchain, "Blockchain should not be null");
        long blockCount = blockchain.getBlockCount();
        assertTrue(blockCount > 0, "Blockchain should contain at least one block");
    }
    
    /**
     * Assert that API instance is properly initialized
     * @param api The API instance to check
     */
    public static void assertApiInitialized(UserFriendlyEncryptionAPI api) {
        assertNotNull(api, "API instance should not be null");
        assertNotNull(api.getBlockchain(), "API should have blockchain reference");
        assertTrue(api.hasDefaultCredentials(), "API should have default credentials set");
        assertNotNull(api.getDefaultUsername(), "API should have default username");
    }
    
    /**
     * Assert that a search operation returned valid results
     * @param results Search results
     * @param expectedMinSize Minimum expected result count
     */
    public static void assertSearchResults(List<Block> results, int expectedMinSize) {
        assertNotNull(results, "Search results should not be null");
        assertTrue(results.size() >= expectedMinSize, 
                  String.format("Should have at least %d results, got %d", expectedMinSize, results.size()));
        
        // Validate each result block
        for (Block block : results) {
            assertNotNull(block, "Search result block should not be null");
            assertNotNull(block.getId(), "Search result should have valid ID");
        }
    }
    
    /**
     * Assert that an operation throws the expected exception
     * @param expectedExceptionClass Expected exception type
     * @param operation Operation to execute
     * @param message Custom assertion message
     * @param <T> Exception type
     */
    public static <T extends Throwable> void assertThrowsWithMessage(
            Class<T> expectedExceptionClass, 
            Runnable operation, 
            String message) {
        
        T exception = assertThrows(expectedExceptionClass, 
                                 operation::run, 
                                 "Should throw " + expectedExceptionClass.getSimpleName());
        
        if (message != null) {
            assertTrue(exception.getMessage().contains(message), 
                      "Exception message should contain: " + message);
        }
    }
    
    /**
     * Assert that password validation works correctly
     * @param api API instance
     * @param password Password to validate
     * @param shouldBeValid Expected validation result
     */
    public static void assertPasswordValidation(UserFriendlyEncryptionAPI api, String password, boolean shouldBeValid) {
        if (shouldBeValid) {
            assertDoesNotThrow(() -> {
                String generated = api.generateValidatedPassword(password.length(), false);
                assertNotNull(generated, "Valid password should be generated");
            }, "Valid password should not throw exception");
        } else {
            assertThrowsWithMessage(IllegalArgumentException.class, 
                                  () -> api.generateValidatedPassword(password.length(), false),
                                  null);
        }
    }
    
    /**
     * Assert that encryption and decryption work correctly
     * @param api API instance
     * @param originalData Original data to encrypt
     * @param password Password for encryption
     */
    public static void assertEncryptionDecryption(UserFriendlyEncryptionAPI api, String originalData, String password) {
        // Store encrypted data
        Block block = api.storeSecret(originalData, password);
        assertBlockCreated(block);
        
        // Verify data can be retrieved
        String retrievedData = api.retrieveSecret(block.getId(), password);
        assertNotNull(retrievedData, "Should be able to retrieve encrypted data");
        assertEquals(originalData, retrievedData, "Retrieved data should match original");
    }
    
    /**
     * Assert that validation report contains expected information
     * @param report Validation report
     * @param shouldBeValid Expected overall validation result
     */
    public static void assertValidationReport(String report, boolean shouldBeValid) {
        assertNotNull(report, "Validation report should not be null");
        assertFalse(report.trim().isEmpty(), "Validation report should not be empty");
        
        if (shouldBeValid) {
            assertTrue(report.contains("✅") || report.contains("valid") || report.contains("success"), 
                      "Report should indicate success");
        } else {
            assertTrue(report.contains("❌") || report.contains("invalid") || report.contains("failed"), 
                      "Report should indicate failure");
        }
    }
    
    /**
     * Assert that performance metrics are within acceptable ranges
     * @param operationName Name of the operation
     * @param executionTimeMs Execution time in milliseconds
     * @param maxExpectedTimeMs Maximum expected time
     */
    public static void assertPerformance(String operationName, long executionTimeMs, long maxExpectedTimeMs) {
        assertTrue(executionTimeMs <= maxExpectedTimeMs, 
                  String.format("%s took %dms, expected max %dms", 
                               operationName, executionTimeMs, maxExpectedTimeMs));
        
        assertTrue(executionTimeMs >= 0, "Execution time should be non-negative");
    }
    
    /**
     * Assert that a list contains all expected elements
     * @param actualList Actual list
     * @param expectedElements Expected elements
     * @param <T> Element type
     */
    @SafeVarargs
    public static <T> void assertContainsAll(List<T> actualList, T... expectedElements) {
        assertNotNull(actualList, "List should not be null");
        
        for (T expected : expectedElements) {
            assertTrue(actualList.contains(expected), 
                      "List should contain: " + expected);
        }
    }
    
    /**
     * Assert that memory usage is within acceptable limits
     * @param operationName Name of the operation
     * @param memoryBeforeBytes Memory before operation
     * @param memoryAfterBytes Memory after operation
     * @param maxMemoryIncreaseBytes Maximum acceptable memory increase
     */
    public static void assertMemoryUsage(String operationName, 
                                       long memoryBeforeBytes, 
                                       long memoryAfterBytes, 
                                       long maxMemoryIncreaseBytes) {
        long memoryIncrease = memoryAfterBytes - memoryBeforeBytes;
        
        assertTrue(memoryIncrease <= maxMemoryIncreaseBytes, 
                  String.format("%s used %d bytes, expected max %d bytes increase", 
                               operationName, memoryIncrease, maxMemoryIncreaseBytes));
    }
}