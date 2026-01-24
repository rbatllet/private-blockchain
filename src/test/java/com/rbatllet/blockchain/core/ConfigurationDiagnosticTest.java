package com.rbatllet.blockchain.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Diagnostic test to identify configuration contamination issues
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Configuration Contamination Diagnostic")
public class ConfigurationDiagnosticTest {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationDiagnosticTest.class);

    @Test
    @Order(1)
    @DisplayName("Step 1: Check initial defaults")
    void testInitialDefaults() {
        Blockchain blockchain = new Blockchain();
        logger.info("=== STEP 1: Initial Defaults ===");
        logger.info("MaxBlockSizeBytes: {}", blockchain.getMaxBlockSizeBytes());
        logger.info("OffChainThresholdBytes: {}", blockchain.getCurrentOffChainThresholdBytes());

        assertEquals(10 * 1024 * 1024, blockchain.getMaxBlockSizeBytes(), "Initial MaxBlockSizeBytes should be 10MB");
        assertEquals(512 * 1024, blockchain.getCurrentOffChainThresholdBytes(), "Initial OffChainThresholdBytes should be 512KB");
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Modify configuration")
    void testModifyConfiguration() {
        Blockchain blockchain = new Blockchain();
        logger.info("\n=== STEP 2: Before Modification ===");
        logger.info("OffChainThresholdBytes: {}", blockchain.getCurrentOffChainThresholdBytes());

        blockchain.setOffChainThresholdBytes(256 * 1024); // 256KB

        logger.info("=== STEP 2: After Modification ===");
        logger.info("OffChainThresholdBytes: {}", blockchain.getCurrentOffChainThresholdBytes());

        assertEquals(256 * 1024, blockchain.getCurrentOffChainThresholdBytes(), "Modified OffChainThresholdBytes should be 256KB");
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: Check if new instance has defaults")
    void testNewInstanceDefaults() {
        Blockchain blockchain = new Blockchain();
        logger.info("\n=== STEP 3: New Instance After Modification ===");
        logger.info("MaxBlockSizeBytes: {}", blockchain.getMaxBlockSizeBytes());
        logger.info("OffChainThresholdBytes: {}", blockchain.getCurrentOffChainThresholdBytes());

        assertEquals(10 * 1024 * 1024, blockchain.getMaxBlockSizeBytes(),
            "New instance should have default MaxBlockSizeBytes (10MB)");
        assertEquals(512 * 1024, blockchain.getCurrentOffChainThresholdBytes(),
            "New instance should have default OffChainThresholdBytes (512KB), but got: " + blockchain.getCurrentOffChainThresholdBytes());
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: Test resetLimitsToDefault()")
    void testResetLimitsToDefault() {
        Blockchain blockchain = new Blockchain();
        logger.info("\n=== STEP 4: Test Reset ===");

        blockchain.setOffChainThresholdBytes(128 * 1024); // 128KB
        logger.info("After setting to 128KB: {}", blockchain.getCurrentOffChainThresholdBytes());

        blockchain.resetLimitsToDefault();
        logger.info("After reset: {}", blockchain.getCurrentOffChainThresholdBytes());

        assertEquals(512 * 1024, blockchain.getCurrentOffChainThresholdBytes(),
            "After reset should be 512KB, but got: " + blockchain.getCurrentOffChainThresholdBytes());
    }
}
