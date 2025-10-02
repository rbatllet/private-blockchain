package com.rbatllet.blockchain.core;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Diagnostic test to identify configuration contamination issues
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Configuration Contamination Diagnostic")
public class ConfigurationDiagnosticTest {

    @Test
    @Order(1)
    @DisplayName("Step 1: Check initial defaults")
    void testInitialDefaults() {
        Blockchain blockchain = new Blockchain();
        System.out.println("=== STEP 1: Initial Defaults ===");
        System.out.println("MaxBlockDataLength: " + blockchain.getMaxBlockDataLength());
        System.out.println("MaxBlockSizeBytes: " + blockchain.getMaxBlockSizeBytes());
        System.out.println("OffChainThresholdBytes: " + blockchain.getCurrentOffChainThresholdBytes());
        
        assertEquals(10000, blockchain.getMaxBlockDataLength(), "Initial MaxBlockDataLength should be 10000");
        assertEquals(1024 * 1024, blockchain.getMaxBlockSizeBytes(), "Initial MaxBlockSizeBytes should be 1MB");
        assertEquals(512 * 1024, blockchain.getCurrentOffChainThresholdBytes(), "Initial OffChainThresholdBytes should be 512KB");
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Modify configuration")
    void testModifyConfiguration() {
        Blockchain blockchain = new Blockchain();
        System.out.println("\n=== STEP 2: Before Modification ===");
        System.out.println("MaxBlockDataLength: " + blockchain.getMaxBlockDataLength());
        
        blockchain.setMaxBlockDataLength(20000);
        
        System.out.println("=== STEP 2: After Modification ===");
        System.out.println("MaxBlockDataLength: " + blockchain.getMaxBlockDataLength());
        
        assertEquals(20000, blockchain.getMaxBlockDataLength(), "Modified MaxBlockDataLength should be 20000");
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: Check if new instance has defaults")
    void testNewInstanceDefaults() {
        Blockchain blockchain = new Blockchain();
        System.out.println("\n=== STEP 3: New Instance After Modification ===");
        System.out.println("MaxBlockDataLength: " + blockchain.getMaxBlockDataLength());
        System.out.println("MaxBlockSizeBytes: " + blockchain.getMaxBlockSizeBytes());
        System.out.println("OffChainThresholdBytes: " + blockchain.getCurrentOffChainThresholdBytes());
        
        assertEquals(10000, blockchain.getMaxBlockDataLength(), 
            "New instance should have default MaxBlockDataLength (10000), but got: " + blockchain.getMaxBlockDataLength());
        assertEquals(1024 * 1024, blockchain.getMaxBlockSizeBytes(), 
            "New instance should have default MaxBlockSizeBytes");
        assertEquals(512 * 1024, blockchain.getCurrentOffChainThresholdBytes(), 
            "New instance should have default OffChainThresholdBytes");
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: Test resetLimitsToDefault()")
    void testResetLimitsToDefault() {
        Blockchain blockchain = new Blockchain();
        System.out.println("\n=== STEP 4: Test Reset ===");
        
        blockchain.setMaxBlockDataLength(30000);
        System.out.println("After setting to 30000: " + blockchain.getMaxBlockDataLength());
        
        blockchain.resetLimitsToDefault();
        System.out.println("After reset: " + blockchain.getMaxBlockDataLength());
        
        assertEquals(10000, blockchain.getMaxBlockDataLength(), 
            "After reset should be 10000, but got: " + blockchain.getMaxBlockDataLength());
    }
}
