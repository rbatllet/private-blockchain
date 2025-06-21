package com.rbatllet.blockchain.util.validation;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.core.Blockchain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for BlockValidationUtil
 */
public class BlockValidationUtilTest {
    
    // Test implementations instead of mocks
    private Block testGenesisBlock;
    // Using this in the InvalidBlockNumber and InvalidPreviousHash tests
    private TestBlockchain testBlockchain;
    
    // Test implementation of Block
    private static class TestBlock extends Block {
        public TestBlock(Long blockNumber, String previousHash) {
            setBlockNumber(blockNumber);
            setPreviousHash(previousHash);
            setHash("hash" + blockNumber);
            setTimestamp(LocalDateTime.now());
            setData("Test data for block " + blockNumber);
        }
    }
    
    // Test implementation of Blockchain
    private static class TestBlockchain extends Blockchain {
        private boolean keyAuthorized;
        private RuntimeException keyAuthorizedException;
        
        public TestBlockchain() {
            // Call default constructor of Blockchain
            super();
            this.keyAuthorized = false;
            this.keyAuthorizedException = null;
        }
        
        public void setKeyAuthorized(boolean authorized) {
            this.keyAuthorized = authorized;
        }
        
        public void setKeyAuthorizedException(RuntimeException exception) {
            this.keyAuthorizedException = exception;
        }
        
        // Override wasKeyAuthorizedAt for testing
        public boolean wasKeyAuthorizedAt(String publicKey, LocalDateTime timestamp) {
            if (keyAuthorizedException != null) {
                throw keyAuthorizedException;
            }
            return keyAuthorized;
        }
    }
    
    @BeforeEach
    public void setUp() {
        // Initialize test objects
        testGenesisBlock = new TestBlock(0L, "0");
        testBlockchain = new TestBlockchain();
    }
    
    @Test
    public void testValidateGenesisBlock_Valid() {
        assertTrue(BlockValidationUtil.validateGenesisBlock(testGenesisBlock), 
                "Valid genesis block should pass validation");
    }
    
    @Test
    public void testValidateGenesisBlock_InvalidBlockNumber() {
        // Create a test block with invalid block number for genesis
        Block invalidBlock = new TestBlock(1L, "0");
        
        assertFalse(BlockValidationUtil.validateGenesisBlock(invalidBlock), 
                "Genesis block with non-zero block number should fail validation");
    }
    
    @Test
    public void testValidateGenesisBlock_InvalidPreviousHash() {
        // Create a test block with invalid previous hash for genesis
        Block invalidBlock = new TestBlock(0L, "not-zero-hash");
        
        assertFalse(BlockValidationUtil.validateGenesisBlock(invalidBlock), 
                "Genesis block with non-zero previous hash should fail validation");
    }
    
    @Test
    public void testWasKeyAuthorizedAt_Authorized() {
        String publicKey = "test-public-key";
        LocalDateTime timestamp = LocalDateTime.now();
        
        // Set up the test blockchain to return true for key authorization
        testBlockchain.setKeyAuthorized(true);
        
        assertTrue(BlockValidationUtil.wasKeyAuthorizedAt(testBlockchain, publicKey, timestamp),
                "Should return true when key was authorized");
    }
    
    @Test
    public void testWasKeyAuthorizedAt_NotAuthorized() {
        String publicKey = "test-public-key";
        LocalDateTime timestamp = LocalDateTime.now();
        
        // Set up the test blockchain to return false for key authorization
        testBlockchain.setKeyAuthorized(false);
        
        assertFalse(BlockValidationUtil.wasKeyAuthorizedAt(testBlockchain, publicKey, timestamp),
                "Should return false when key was not authorized");
    }
    
    @Test
    public void testWasKeyAuthorizedAt_Exception() {
        String publicKey = "test-public-key";
        LocalDateTime timestamp = LocalDateTime.now();
        
        // Set up the test blockchain to throw an exception for key authorization
        testBlockchain.setKeyAuthorizedException(new RuntimeException("Test exception"));
        
        assertFalse(BlockValidationUtil.wasKeyAuthorizedAt(testBlockchain, publicKey, timestamp),
                "Should return false when an exception occurs");
    }
    
    @Test
    public void testTruncateHash_NullHash() {
        assertEquals("null", BlockValidationUtil.truncateHash(null),
                "Should return 'null' for null hash");
    }
    
    @Test
    public void testTruncateHash_ShortHash() {
        String shortHash = "abcdef";
        assertEquals(shortHash, BlockValidationUtil.truncateHash(shortHash),
                "Short hash should not be truncated");
    }
    
    @Test
    public void testTruncateHash_LongHash() {
        String longHash = "abcdef1234567890abcdef1234567890abcdef1234567890";
        String expected = "abcdef1234567890...7890abcdef1234567890";
        
        assertEquals(expected, BlockValidationUtil.truncateHash(longHash),
                "Long hash should be truncated with ellipsis in the middle");
    }
}
