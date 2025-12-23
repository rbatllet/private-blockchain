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
        System.out.println("MaxBlockSizeBytes: " + blockchain.getMaxBlockSizeBytes());
        System.out.println("OffChainThresholdBytes: " + blockchain.getCurrentOffChainThresholdBytes());

        assertEquals(1024 * 1024, blockchain.getMaxBlockSizeBytes(), "Initial MaxBlockSizeBytes should be 1MB");
        assertEquals(512 * 1024, blockchain.getCurrentOffChainThresholdBytes(), "Initial OffChainThresholdBytes should be 512KB");
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Modify configuration")
    void testModifyConfiguration() {
        Blockchain blockchain = new Blockchain();
        System.out.println("\n=== STEP 2: Before Modification ===");
        System.out.println("OffChainThresholdBytes: " + blockchain.getCurrentOffChainThresholdBytes());

        blockchain.setOffChainThresholdBytes(256 * 1024); // 256KB

        System.out.println("=== STEP 2: After Modification ===");
        System.out.println("OffChainThresholdBytes: " + blockchain.getCurrentOffChainThresholdBytes());

        assertEquals(256 * 1024, blockchain.getCurrentOffChainThresholdBytes(), "Modified OffChainThresholdBytes should be 256KB");
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: Check if new instance has defaults")
    void testNewInstanceDefaults() {
        Blockchain blockchain = new Blockchain();
        System.out.println("\n=== STEP 3: New Instance After Modification ===");
        System.out.println("MaxBlockSizeBytes: " + blockchain.getMaxBlockSizeBytes());
        System.out.println("OffChainThresholdBytes: " + blockchain.getCurrentOffChainThresholdBytes());

        assertEquals(1024 * 1024, blockchain.getMaxBlockSizeBytes(),
            "New instance should have default MaxBlockSizeBytes");
        assertEquals(512 * 1024, blockchain.getCurrentOffChainThresholdBytes(),
            "New instance should have default OffChainThresholdBytes (512KB), but got: " + blockchain.getCurrentOffChainThresholdBytes());
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: Test resetLimitsToDefault()")
    void testResetLimitsToDefault() {
        Blockchain blockchain = new Blockchain();
        System.out.println("\n=== STEP 4: Test Reset ===");

        blockchain.setOffChainThresholdBytes(128 * 1024); // 128KB
        System.out.println("After setting to 128KB: " + blockchain.getCurrentOffChainThresholdBytes());

        blockchain.resetLimitsToDefault();
        System.out.println("After reset: " + blockchain.getCurrentOffChainThresholdBytes());

        assertEquals(512 * 1024, blockchain.getCurrentOffChainThresholdBytes(),
            "After reset should be 512KB, but got: " + blockchain.getCurrentOffChainThresholdBytes());
    }
}
