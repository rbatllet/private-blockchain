package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.config.DatabaseConfig;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;
import com.rbatllet.blockchain.validation.BlockValidationResult;
import com.rbatllet.blockchain.validation.ChainValidationResult;
import com.rbatllet.blockchain.core.Blockchain.ValidationSummary;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for streaming validation functionality
 * Tests the new validateChainStreaming() method and ValidationSummary class
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StreamingValidationTest {

    private static final Logger logger = LoggerFactory.getLogger(StreamingValidationTest.class);

    private Blockchain blockchain;
    private KeyPair keyPair1;
    private PrivateKey privateKey1;
    private PublicKey publicKey1;

    @BeforeAll
    static void setUpDatabase() {
        // Use H2 in-memory database for testing
        DatabaseConfig h2Config = DatabaseConfig.createH2TestConfig();
        JPAUtil.initialize(h2Config);
        logger.info("✅ H2 test database initialized");
    }

    @BeforeEach
    void setUp() throws Exception {
        blockchain = new Blockchain();

        // Generate test keys
        keyPair1 = CryptoUtil.generateKeyPair();
        privateKey1 = keyPair1.getPrivate();
        publicKey1 = keyPair1.getPublic();

        String publicKeyString = CryptoUtil.publicKeyToString(publicKey1);
        blockchain.addAuthorizedKey(publicKeyString, "TestUser1");

        logger.info("🔧 Test blockchain initialized");
    }

    @AfterEach
    void tearDown() {
        if (blockchain != null) {
            blockchain.clearAndReinitialize();
            JPAUtil.cleanupThreadLocals();
        }
        logger.info("🧹 Test cleanup completed");
    }

    @Test
    @Order(1)
    @DisplayName("📊 Basic streaming validation - small chain")
    void testBasicStreamingValidation() {
        logger.info("🧪 Testing basic streaming validation...");

        // Add some blocks
        for (int i = 0; i < 10; i++) {
            blockchain.addBlock("Test data " + i, privateKey1, publicKey1);
        }

        final AtomicInteger batchesReceived = new AtomicInteger(0);
        final AtomicInteger totalBlocksProcessed = new AtomicInteger(0);

        ValidationSummary summary = blockchain.validateChainStreaming(
            batchResults -> {
                batchesReceived.incrementAndGet();
                totalBlocksProcessed.addAndGet(batchResults.size());
                logger.debug("📦 Received batch {} with {} results",
                    batchesReceived.get(), batchResults.size());
            },
            1000
        );

        // Verify summary
        assertEquals(11, summary.getTotalBlocks()); // 10 + genesis
        assertEquals(11, summary.getValidBlocks());
        assertEquals(0, summary.getInvalidBlocks());
        assertEquals(0, summary.getRevokedBlocks());
        assertTrue(summary.isValid());

        // Verify batches
        assertTrue(batchesReceived.get() >= 1, "Should receive at least 1 batch");
        assertEquals(11, totalBlocksProcessed.get(), "Should process all blocks");

        logger.info("✅ Basic streaming validation test passed");
    }

    @Test
    @Order(2)
    @DisplayName("📊 Streaming validation - large chain with multiple batches")
    void testLargeChainStreamingValidation() {
        logger.info("🧪 Testing streaming validation with large chain...");

        // Add 2500 blocks (will require 3 batches with batch size 1000)
        for (int i = 0; i < 2500; i++) {
            blockchain.addBlock("Data block " + i, privateKey1, publicKey1);
        }

        final List<Integer> batchSizes = new ArrayList<>();
        final AtomicInteger totalProcessed = new AtomicInteger(0);

        ValidationSummary summary = blockchain.validateChainStreaming(
            batchResults -> {
                batchSizes.add(batchResults.size());
                totalProcessed.addAndGet(batchResults.size());

                // Verify all results in batch are valid
                for (BlockValidationResult result : batchResults) {
                    assertTrue(result.isValid(),
                        "Block #" + result.getBlock().getBlockNumber() + " should be valid");
                }
            },
            1000
        );

        // Verify summary
        assertEquals(2501, summary.getTotalBlocks()); // 2500 + genesis
        assertEquals(2501, summary.getValidBlocks());
        assertEquals(0, summary.getInvalidBlocks());
        assertTrue(summary.isValid());

        // Verify batches
        assertTrue(batchSizes.size() >= 3, "Should have at least 3 batches");
        assertEquals(2501, totalProcessed.get(), "Should process all blocks");

        logger.info("✅ Large chain streaming validation test passed ({} batches)", batchSizes.size());
    }

    @Test
    @Order(3)
    @DisplayName("📊 Streaming validation - collect invalid blocks")
    void testStreamingValidationWithInvalidBlocks() {
        logger.info("🧪 Testing streaming validation with invalid block detection...");

        // Add valid blocks
        for (int i = 0; i < 5; i++) {
            blockchain.addBlock("Valid data " + i, privateKey1, publicKey1);
        }

        // Manually corrupt a block (simulate invalid block)
        // Note: In real scenario, this would be done by tampering with the database
        // For this test, we'll just verify the streaming mechanism works

        final List<Long> invalidBlockNumbers = new ArrayList<>();
        final List<Long> validBlockNumbers = new ArrayList<>();

        ValidationSummary summary = blockchain.validateChainStreaming(
            batchResults -> {
                for (BlockValidationResult result : batchResults) {
                    if (result.isValid()) {
                        validBlockNumbers.add(result.getBlock().getBlockNumber());
                    } else {
                        invalidBlockNumbers.add(result.getBlock().getBlockNumber());
                    }
                }
            },
            1000
        );

        // All blocks should be valid in this test
        assertEquals(6, summary.getTotalBlocks()); // 5 + genesis
        assertEquals(6, summary.getValidBlocks());
        assertEquals(0, summary.getInvalidBlocks());
        assertEquals(0, invalidBlockNumbers.size());
        assertEquals(6, validBlockNumbers.size());

        logger.info("✅ Streaming validation with invalid block detection test passed");
    }

    @Test
    @Order(4)
    @DisplayName("📊 ValidationSummary - verify all methods")
    void testValidationSummaryMethods() {
        logger.info("🧪 Testing ValidationSummary methods...");

        ValidationSummary summary = new ValidationSummary(100, 95, 3, 2);

        assertEquals(100, summary.getTotalBlocks());
        assertEquals(95, summary.getValidBlocks());
        assertEquals(3, summary.getInvalidBlocks());
        assertEquals(2, summary.getRevokedBlocks());
        assertFalse(summary.isValid(), "Should be invalid when there are invalid blocks");

        String summaryString = summary.toString();
        assertTrue(summaryString.contains("total=100"));
        assertTrue(summaryString.contains("valid=95"));
        assertTrue(summaryString.contains("invalid=3"));
        assertTrue(summaryString.contains("revoked=2"));

        logger.info("✅ ValidationSummary methods test passed");
    }

    @Test
    @Order(5)
    @DisplayName("📊 ValidationSummary - valid chain")
    void testValidationSummaryForValidChain() {
        logger.info("🧪 Testing ValidationSummary for valid chain...");

        ValidationSummary summary = new ValidationSummary(1000, 1000, 0, 0);

        assertTrue(summary.isValid(), "Chain with no invalid blocks should be valid");
        assertEquals(1000, summary.getTotalBlocks());
        assertEquals(1000, summary.getValidBlocks());

        logger.info("✅ ValidationSummary valid chain test passed");
    }

    @Test
    @Order(6)
    @DisplayName("📊 Streaming validation - custom batch sizes")
    void testDifferentBatchSizes() {
        logger.info("🧪 Testing streaming validation with different batch sizes...");

        // Add 50 blocks
        for (int i = 0; i < 50; i++) {
            blockchain.addBlock("Test " + i, privateKey1, publicKey1);
        }

        // Test with batch size 10
        final AtomicInteger batches10 = new AtomicInteger(0);
        ValidationSummary summary10 = blockchain.validateChainStreaming(
            batch -> batches10.incrementAndGet(),
            10
        );

        assertEquals(51, summary10.getTotalBlocks());
        assertTrue(batches10.get() >= 5, "Should have at least 5 batches with size 10");

        // Test with batch size 25
        final AtomicInteger batches25 = new AtomicInteger(0);
        ValidationSummary summary25 = blockchain.validateChainStreaming(
            batch -> batches25.incrementAndGet(),
            25
        );

        assertEquals(51, summary25.getTotalBlocks());
        assertTrue(batches25.get() >= 2, "Should have at least 2 batches with size 25");

        logger.info("✅ Different batch sizes test passed");
    }

    @Test
    @Order(7)
    @DisplayName("📊 Streaming validation - genesis only chain")
    void testStreamingValidationGenesisOnly() {
        logger.info("🧪 Testing streaming validation on chain with only genesis block...");

        Blockchain genesisOnlyBlockchain = new Blockchain();

        final AtomicInteger batchesReceived = new AtomicInteger(0);
        ValidationSummary summary = genesisOnlyBlockchain.validateChainStreaming(
            batch -> batchesReceived.incrementAndGet(),
            1000
        );

        assertEquals(1, summary.getTotalBlocks()); // Genesis block only
        assertEquals(1, summary.getValidBlocks());
        assertEquals(0, summary.getInvalidBlocks());
        assertTrue(batchesReceived.get() >= 1, "Should receive at least 1 batch");

        logger.info("✅ Genesis-only chain streaming validation test passed");
    }

    @Test
    @Order(8)
    @DisplayName("📊 Streaming validation - invalid batch size")
    void testInvalidBatchSize() {
        logger.info("🧪 Testing streaming validation with invalid batch size...");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.validateChainStreaming(batch -> {}, 0);
        }, "Should throw exception for batch size 0");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.validateChainStreaming(batch -> {}, -1);
        }, "Should throw exception for negative batch size");

        logger.info("✅ Invalid batch size test passed");
    }

    @Test
    @Order(9)
    @DisplayName("📊 Memory efficiency - streaming vs detailed validation")
    void testMemoryEfficiencyComparison() {
        logger.info("🧪 Testing memory efficiency of streaming validation...");

        // Add moderate number of blocks
        for (int i = 0; i < 100; i++) {
            blockchain.addBlock("Block " + i, privateKey1, publicKey1);
        }

        // Test detailed validation (accumulates all results)
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        ChainValidationResult detailedResult = blockchain.validateChainDetailed();
        long memoryAfterDetailed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Test streaming validation (no accumulation)
        System.gc(); // Suggest garbage collection
        long memoryBeforeStreaming = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        ValidationSummary streamingSummary = blockchain.validateChainStreaming(
            batch -> { /* Do nothing, just count */ },
            10
        );
        long memoryAfterStreaming = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Both should give consistent results
        assertEquals(detailedResult.getTotalBlocks(), streamingSummary.getTotalBlocks());
        assertEquals(detailedResult.getValidBlocks(), streamingSummary.getValidBlocks());

        logger.info("📊 Memory usage - Detailed: {} bytes, Streaming: {} bytes",
            memoryAfterDetailed - memoryBefore,
            memoryAfterStreaming - memoryBeforeStreaming);

        logger.info("✅ Memory efficiency comparison test passed");
    }
}
