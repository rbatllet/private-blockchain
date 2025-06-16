package com.rbatllet.blockchain.util.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for BlockValidationResult
 */
public class BlockValidationResultTest {
    
    private BlockValidationResult result;
    
    @BeforeEach
    public void setUp() {
        result = new BlockValidationResult();
    }
    
    @Test
    public void testDefaultConstructor() {
        assertTrue(result.isPreviousHashValid(), "Previous hash should be valid by default");
        assertTrue(result.isBlockNumberValid(), "Block number should be valid by default");
        assertTrue(result.isHashIntegrityValid(), "Hash integrity should be valid by default");
        assertTrue(result.isSignatureValid(), "Signature should be valid by default");
        assertTrue(result.isAuthorizedKeyValid(), "Authorized key should be valid by default");
        assertNull(result.getErrorMessage(), "Error message should be null by default");
        assertTrue(result.isValid(), "Result should be valid by default");
    }
    
    @Test
    public void testSetPreviousHashValid() {
        result.setPreviousHashValid(false);
        assertFalse(result.isPreviousHashValid(), "Previous hash should be invalid after setting to false");
        assertFalse(result.isValid(), "Result should be invalid if any validation fails");
    }
    
    @Test
    public void testSetBlockNumberValid() {
        result.setBlockNumberValid(false);
        assertFalse(result.isBlockNumberValid(), "Block number should be invalid after setting to false");
        assertFalse(result.isValid(), "Result should be invalid if any validation fails");
    }
    
    @Test
    public void testSetHashIntegrityValid() {
        result.setHashIntegrityValid(false);
        assertFalse(result.isHashIntegrityValid(), "Hash integrity should be invalid after setting to false");
        assertFalse(result.isValid(), "Result should be invalid if any validation fails");
    }
    
    @Test
    public void testSetSignatureValid() {
        result.setSignatureValid(false);
        assertFalse(result.isSignatureValid(), "Signature should be invalid after setting to false");
        assertFalse(result.isValid(), "Result should be invalid if any validation fails");
    }
    
    @Test
    public void testSetAuthorizedKeyValid() {
        result.setAuthorizedKeyValid(false);
        assertFalse(result.isAuthorizedKeyValid(), "Authorized key should be invalid after setting to false");
        assertFalse(result.isValid(), "Result should be invalid if any validation fails");
    }
    
    @Test
    public void testSetErrorMessage() {
        String errorMessage = "Test error message";
        result.setErrorMessage(errorMessage);
        assertEquals(errorMessage, result.getErrorMessage(), "Error message should be set correctly");
    }
    
    @Test
    public void testMethodChaining() {
        result.setPreviousHashValid(false)
              .setBlockNumberValid(false)
              .setHashIntegrityValid(false)
              .setSignatureValid(false)
              .setAuthorizedKeyValid(false)
              .setErrorMessage("Multiple errors");
        
        assertFalse(result.isPreviousHashValid(), "Previous hash should be invalid");
        assertFalse(result.isBlockNumberValid(), "Block number should be invalid");
        assertFalse(result.isHashIntegrityValid(), "Hash integrity should be invalid");
        assertFalse(result.isSignatureValid(), "Signature should be invalid");
        assertFalse(result.isAuthorizedKeyValid(), "Authorized key should be invalid");
        assertEquals("Multiple errors", result.getErrorMessage(), "Error message should be set correctly");
        assertFalse(result.isValid(), "Result should be invalid");
    }
    
    @Test
    public void testIsValid_AllValid() {
        assertTrue(result.isValid(), "Result should be valid when all validations pass");
    }
    
    @Test
    public void testIsValid_OneInvalid() {
        result.setPreviousHashValid(false);
        assertFalse(result.isValid(), "Result should be invalid if any validation fails");
        
        // Reset and try another field
        result = new BlockValidationResult();
        result.setBlockNumberValid(false);
        assertFalse(result.isValid(), "Result should be invalid if any validation fails");
    }
}
