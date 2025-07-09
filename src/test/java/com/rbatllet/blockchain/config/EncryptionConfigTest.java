package com.rbatllet.blockchain.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EncryptionConfig class
 */
public class EncryptionConfigTest {
    
    @Test
    void testDefaultConfiguration() {
        EncryptionConfig config = new EncryptionConfig();
        
        assertEquals("AES", config.getEncryptionAlgorithm());
        assertEquals("GCM", config.getEncryptionMode());
        assertEquals("NoPadding", config.getPadding());
        assertEquals(256, config.getKeyLength());
        assertEquals(12, config.getIvLength());
        assertEquals(128, config.getTagLength());
        assertEquals(32, config.getSaltLength());
        assertEquals(100000, config.getPbkdf2Iterations());
        assertEquals("PBKDF2WithHmacSHA256", config.getPbkdf2Algorithm());
        assertEquals(12, config.getMinPasswordLength());
        assertEquals(1024 * 1024, config.getMaxDataSizeBytes());
        assertTrue(config.isValidateEncryptionFormat());
        assertFalse(config.isEnableCompression());
        assertTrue(config.isSecureRandomEnabled());
        assertFalse(config.isMetadataEncryptionEnabled());
        assertTrue(config.isCorruptionDetectionEnabled());
        
        assertEquals("AES/GCM/NoPadding", config.getEncryptionTransformation());
    }
    
    @Test
    void testHighSecurityConfig() {
        EncryptionConfig config = EncryptionConfig.createHighSecurityConfig();
        
        assertEquals(256, config.getKeyLength());
        assertEquals(150000, config.getPbkdf2Iterations());
        assertEquals(16, config.getMinPasswordLength());
        assertTrue(config.isMetadataEncryptionEnabled());
        assertTrue(config.isCorruptionDetectionEnabled());
        assertTrue(config.isValidateEncryptionFormat());
    }
    
    @Test
    void testPerformanceConfig() {
        EncryptionConfig config = EncryptionConfig.createPerformanceConfig();
        
        assertEquals(128, config.getKeyLength());
        assertEquals(50000, config.getPbkdf2Iterations());
        assertEquals(8, config.getMinPasswordLength());
        assertFalse(config.isValidateEncryptionFormat());
        assertTrue(config.isEnableCompression());
    }
    
    @Test
    void testTestConfig() {
        EncryptionConfig config = EncryptionConfig.createTestConfig();
        
        assertEquals(128, config.getKeyLength());
        assertEquals(1000, config.getPbkdf2Iterations());
        assertEquals(4, config.getMinPasswordLength());
        assertFalse(config.isValidateEncryptionFormat());
        assertFalse(config.isCorruptionDetectionEnabled());
    }
    
    @Test
    void testBuilder() {
        EncryptionConfig config = EncryptionConfig.builder()
                .keyLength(192)
                .pbkdf2Iterations(75000)
                .minPasswordLength(10)
                .enableCompression(true)
                .metadataEncryptionEnabled(true)
                .build();
        
        assertEquals(192, config.getKeyLength());
        assertEquals(75000, config.getPbkdf2Iterations());
        assertEquals(10, config.getMinPasswordLength());
        assertTrue(config.isEnableCompression());
        assertTrue(config.isMetadataEncryptionEnabled());
    }
    
    @Test
    void testValidation() {
        // Test invalid key length
        assertThrows(IllegalArgumentException.class, () -> 
            EncryptionConfig.builder().keyLength(64).build());
        
        // Test invalid IV length
        assertThrows(IllegalArgumentException.class, () -> 
            EncryptionConfig.builder().ivLength(4).build());
        
        // Test invalid tag length
        assertThrows(IllegalArgumentException.class, () -> 
            EncryptionConfig.builder().tagLength(64).build());
        
        // Test invalid salt length
        assertThrows(IllegalArgumentException.class, () -> 
            EncryptionConfig.builder().saltLength(8).build());
        
        // Test invalid PBKDF2 iterations
        assertThrows(IllegalArgumentException.class, () -> 
            EncryptionConfig.builder().pbkdf2Iterations(500).build());
        
        // Test invalid min password length
        assertThrows(IllegalArgumentException.class, () -> 
            EncryptionConfig.builder().minPasswordLength(2).build());
        
        // Test invalid max data size
        assertThrows(IllegalArgumentException.class, () -> 
            EncryptionConfig.builder().maxDataSizeBytes(512).build());
    }
    
    @Test
    void testSummary() {
        EncryptionConfig config = new EncryptionConfig();
        String summary = config.getSummary();
        
        assertNotNull(summary);
        assertTrue(summary.contains("Encryption Configuration Summary"));
        assertTrue(summary.contains("AES/GCM/NoPadding"));
        assertTrue(summary.contains("256 bits"));
        assertTrue(summary.contains("100000"));
    }
    
    @Test
    void testSetters() {
        EncryptionConfig config = new EncryptionConfig();
        
        config.setKeyLength(192);
        config.setPbkdf2Iterations(80000);
        config.setMinPasswordLength(14);
        config.setMetadataEncryptionEnabled(true);
        
        assertEquals(192, config.getKeyLength());
        assertEquals(80000, config.getPbkdf2Iterations());
        assertEquals(14, config.getMinPasswordLength());
        assertTrue(config.isMetadataEncryptionEnabled());
    }
    
    @Test
    void testToString() {
        EncryptionConfig config = new EncryptionConfig();
        String toString = config.toString();
        
        assertNotNull(toString);
        assertEquals(config.getSummary(), toString);
    }
}