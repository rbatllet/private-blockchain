package com.rbatllet.blockchain.validation;

import com.rbatllet.blockchain.entity.Block;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for BlockValidationResult (advanced version)
 */
public class BlockValidationResultTest {
    
    private Block mockBlock;
    
    @BeforeEach
    public void setUp() {
        // Create a mock block for testing
        mockBlock = new Block();
        mockBlock.setBlockNumber(1L);
        mockBlock.setData("Test data");
        mockBlock.setHash("test-hash");
    }
    
    @Test
    public void testBuilderWithValidBlock() {
        BlockValidationResult result = new BlockValidationResult.Builder(mockBlock)
                .status(BlockStatus.VALID)
                .structurallyValid(true)
                .cryptographicallyValid(true)
                .authorizationValid(true)
                .offChainDataValid(true)
                .build();
        
        assertTrue(result.isValid(), "Result should be valid");
        assertTrue(result.isFullyValid(), "Result should be fully valid");
        assertTrue(result.isStructurallyValid(), "Should be structurally valid");
        assertTrue(result.isCryptographicallyValid(), "Should be cryptographically valid");
        assertTrue(result.isAuthorizationValid(), "Should be authorization valid");
        assertTrue(result.isOffChainDataValid(), "Should be off-chain data valid");
        assertEquals(BlockStatus.VALID, result.getStatus(), "Status should be VALID");
        assertEquals(mockBlock, result.getBlock(), "Block should match");
    }
    
    @Test
    public void testBuilderWithInvalidBlock() {
        BlockValidationResult result = new BlockValidationResult.Builder(mockBlock)
                .status(BlockStatus.INVALID)
                .structurallyValid(false)
                .cryptographicallyValid(false)
                .authorizationValid(false)
                .offChainDataValid(false)
                .errorMessage("Test error message")
                .build();
        
        assertFalse(result.isValid(), "Result should be invalid");
        assertFalse(result.isFullyValid(), "Result should not be fully valid");
        assertFalse(result.isStructurallyValid(), "Should be structurally invalid");
        assertFalse(result.isCryptographicallyValid(), "Should be cryptographically invalid");
        assertFalse(result.isAuthorizationValid(), "Should be authorization invalid");
        assertFalse(result.isOffChainDataValid(), "Should be off-chain data invalid");
        assertEquals(BlockStatus.INVALID, result.getStatus(), "Status should be INVALID");
        assertEquals("Test error message", result.getErrorMessage(), "Error message should match");
    }
    
    @Test
    public void testBuilderWithRevokedBlock() {
        BlockValidationResult result = new BlockValidationResult.Builder(mockBlock)
                .status(BlockStatus.REVOKED)
                .structurallyValid(true)
                .cryptographicallyValid(true)
                .authorizationValid(false)
                .offChainDataValid(true)
                .warningMessage("Key revoked")
                .build();
        
        assertTrue(result.isValid(), "Revoked blocks should be considered valid");
        assertFalse(result.isFullyValid(), "Revoked blocks should not be fully valid");
        assertTrue(result.isStructurallyValid(), "Should be structurally valid");
        assertTrue(result.isCryptographicallyValid(), "Should be cryptographically valid");
        assertFalse(result.isAuthorizationValid(), "Should be authorization invalid");
        assertTrue(result.isOffChainDataValid(), "Should be off-chain data valid");
        assertEquals(BlockStatus.REVOKED, result.getStatus(), "Status should be REVOKED");
        assertEquals("Key revoked", result.getWarningMessage(), "Warning message should match");
    }
    
    @Test
    public void testAutoStatusDetermination() {
        // Test auto-determination of VALID status
        BlockValidationResult validResult = new BlockValidationResult.Builder(mockBlock)
                .structurallyValid(true)
                .cryptographicallyValid(true)
                .authorizationValid(true)
                .offChainDataValid(true)
                .build();
        
        assertEquals(BlockStatus.VALID, validResult.getStatus(), "Status should be auto-determined as VALID");
        assertTrue(validResult.isValid(), "Should be valid");
        
        // Test auto-determination of REVOKED status
        BlockValidationResult revokedResult = new BlockValidationResult.Builder(mockBlock)
                .structurallyValid(true)
                .cryptographicallyValid(true)
                .authorizationValid(false)
                .offChainDataValid(true)
                .build();
        
        assertEquals(BlockStatus.REVOKED, revokedResult.getStatus(), "Status should be auto-determined as REVOKED");
        assertTrue(revokedResult.isValid(), "Should be valid (revoked is still valid)");
        
        // Test auto-determination of INVALID status
        BlockValidationResult invalidResult = new BlockValidationResult.Builder(mockBlock)
                .structurallyValid(false)
                .cryptographicallyValid(true)
                .authorizationValid(true)
                .offChainDataValid(true)
                .build();
        
        assertEquals(BlockStatus.INVALID, invalidResult.getStatus(), "Status should be auto-determined as INVALID");
        assertFalse(invalidResult.isValid(), "Should be invalid");
    }
    
    @Test
    public void testMethodChaining() {
        BlockValidationResult result = new BlockValidationResult.Builder(mockBlock)
                .structurallyValid(true)
                .cryptographicallyValid(true)
                .authorizationValid(false)
                .offChainDataValid(true)
                .errorMessage("Authorization failed")
                .warningMessage("Key might be revoked")
                .status(BlockStatus.REVOKED)
                .build();
        
        assertEquals(BlockStatus.REVOKED, result.getStatus(), "Status should be REVOKED");
        assertTrue(result.isStructurallyValid(), "Should be structurally valid");
        assertTrue(result.isCryptographicallyValid(), "Should be cryptographically valid");
        assertFalse(result.isAuthorizationValid(), "Should be authorization invalid");
        assertTrue(result.isOffChainDataValid(), "Should be off-chain data valid");
        assertEquals("Authorization failed", result.getErrorMessage(), "Error message should match");
        assertEquals("Key might be revoked", result.getWarningMessage(), "Warning message should match");
    }
    
    @Test
    public void testDefaultOffChainDataValid() {
        // Test that off-chain data is valid by default (for blocks without off-chain data)
        BlockValidationResult result = new BlockValidationResult.Builder(mockBlock)
                .structurallyValid(true)
                .cryptographicallyValid(true)
                .authorizationValid(true)
                .build();
        
        assertTrue(result.isOffChainDataValid(), "Off-chain data should be valid by default");
        assertEquals(BlockStatus.VALID, result.getStatus(), "Should be VALID");
    }
    
    @Test
    public void testToString() {
        BlockValidationResult result = new BlockValidationResult.Builder(mockBlock)
                .status(BlockStatus.INVALID)
                .structurallyValid(false)
                .cryptographicallyValid(true)
                .authorizationValid(true)
                .offChainDataValid(true)
                .errorMessage("Structural validation failed")
                .warningMessage("Hash mismatch detected")
                .build();
        
        String resultString = result.toString();
        assertTrue(resultString.contains("Block is invalid"), "Should contain status description");
        assertTrue(resultString.contains("Block #1"), "Should contain block number");
        assertTrue(resultString.contains("Structural validation failed"), "Should contain error message");
        assertTrue(resultString.contains("Hash mismatch detected"), "Should contain warning message");
    }
}