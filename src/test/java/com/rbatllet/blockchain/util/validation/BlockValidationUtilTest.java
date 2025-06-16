package com.rbatllet.blockchain.util.validation;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.core.Blockchain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Test class for BlockValidationUtil
 */
@ExtendWith(MockitoExtension.class)
public class BlockValidationUtilTest {
    
    @Mock
    private Block mockGenesisBlock;
    
    @Mock
    private Block mockNonGenesisBlock;
    
    @Mock
    private Blockchain mockBlockchain;
    
    @BeforeEach
    public void setUp() {
        // Set up genesis block mock
        lenient().when(mockGenesisBlock.getBlockNumber()).thenReturn(0L);
        lenient().when(mockGenesisBlock.getPreviousHash()).thenReturn("0");
        
        // Set up non-genesis block mock
        lenient().when(mockNonGenesisBlock.getBlockNumber()).thenReturn(1L);
        lenient().when(mockNonGenesisBlock.getPreviousHash()).thenReturn("abcdef1234567890");
    }
    
    @Test
    public void testValidateGenesisBlock_Valid() {
        assertTrue(BlockValidationUtil.validateGenesisBlock(mockGenesisBlock), 
                "Valid genesis block should pass validation");
    }
    
    @Test
    public void testValidateGenesisBlock_InvalidBlockNumber() {
        when(mockGenesisBlock.getBlockNumber()).thenReturn(1L);
        
        assertFalse(BlockValidationUtil.validateGenesisBlock(mockGenesisBlock), 
                "Genesis block with non-zero block number should fail validation");
    }
    
    @Test
    public void testValidateGenesisBlock_InvalidPreviousHash() {
        when(mockGenesisBlock.getPreviousHash()).thenReturn("not-zero-hash");
        
        assertFalse(BlockValidationUtil.validateGenesisBlock(mockGenesisBlock), 
                "Genesis block with non-zero previous hash should fail validation");
    }
    
    @Test
    public void testWasKeyAuthorizedAt_Authorized() {
        String publicKey = "test-public-key";
        LocalDateTime timestamp = LocalDateTime.now();
        
        when(mockBlockchain.wasKeyAuthorizedAt(eq(publicKey), eq(timestamp))).thenReturn(true);
        
        assertTrue(BlockValidationUtil.wasKeyAuthorizedAt(mockBlockchain, publicKey, timestamp),
                "Should return true when key was authorized");
    }
    
    @Test
    public void testWasKeyAuthorizedAt_NotAuthorized() {
        String publicKey = "test-public-key";
        LocalDateTime timestamp = LocalDateTime.now();
        
        when(mockBlockchain.wasKeyAuthorizedAt(eq(publicKey), eq(timestamp))).thenReturn(false);
        
        assertFalse(BlockValidationUtil.wasKeyAuthorizedAt(mockBlockchain, publicKey, timestamp),
                "Should return false when key was not authorized");
    }
    
    @Test
    public void testWasKeyAuthorizedAt_Exception() {
        String publicKey = "test-public-key";
        LocalDateTime timestamp = LocalDateTime.now();
        
        when(mockBlockchain.wasKeyAuthorizedAt(eq(publicKey), eq(timestamp)))
                .thenThrow(new RuntimeException("Test exception"));
        
        assertFalse(BlockValidationUtil.wasKeyAuthorizedAt(mockBlockchain, publicKey, timestamp),
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
