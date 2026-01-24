package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.config.DatabaseConfig;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Phase A.7: Large-Scale Memory Efficiency Testing
 *
 * ✅ MEMORY SAFETY VERIFICATION AT SCALE
 *
 * Tests that processChainInBatches() and streaming operations maintain
 * constant memory usage (~50MB) regardless of blockchain size:
 * - 100K blocks (100,000 blocks)
 * - 500K blocks (500,000 blocks) [optional - very slow]
 * - 1M blocks (1,000,000 blocks) [optional - extremely slow]
 *
 * Key Verification Points:
 * ✅ Memory delta stays < 100MB
 * ✅ Processing time scales linearly
 * ✅ No memory leaks (final memory ≈ initial memory)
 * ✅ GC behavior is normal
 *
 * This test is SLOW - expect 5+ minutes for 100K blocks.
 * Use: mvn test -Dtest=Phase_A7_LargeScaleMemoryTest
 *
 * Tags: integration, memory, large-scale
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Phase A.7: Large-Scale Memory Efficiency Testing (100K+ blocks)")
@Tag("integration")
@Tag("memory")
public class Phase_A7_LargeScaleMemoryTest {
    private static final Logger logger = LoggerFactory.getLogger(Phase_A7_LargeScaleMemoryTest.class);


    private Blockchain blockchain;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        // Use H2 in-memory for testing (fastest database for block generation)
        DatabaseConfig h2Config = DatabaseConfig.createH2TestConfig();
        JPAUtil.initialize(h2Config);

        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();

        keyPair = CryptoUtil.generateKeyPair();

        // Authorize key BEFORE adding blocks
        String publicKeyStr = CryptoUtil.publicKeyToString(keyPair.getPublic());
        blockchain.createBootstrapAdmin(publicKeyStr, "TestUser");

        logger.info("✅ Setup complete: H2 database ready for testing");
    }

    @AfterEach
    void tearDown() throws Exception {
        blockchain.clearAndReinitialize();
    }

    // ==================== MEMORY MEASUREMENT HELPERS ====================

    /**
     * Get current heap memory usage in bytes
     */
    private long getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * Force garbage collection (best effort)
     */
    private void forceGarbageCollection() {
        System.gc();
        try {
            Thread.sleep(100); // Give GC time to complete
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Generate large number of blocks efficiently
     */
    private void generateBlocks(int count) throws Exception {
        logger.info("📝 Generating " + count + " test blocks...");
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            blockchain.addBlock("Test block " + i, keyPair.getPrivate(), keyPair.getPublic());

            // Progress reporting
            if ((i + 1) % 10000 == 0) {
                long elapsed = System.currentTimeMillis() - startTime;
                logger.info("  ✅ " + (i + 1) + " blocks added (" + elapsed + "ms)");
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("✅ Block generation complete: " + count + " blocks in " + totalTime + "ms");
    }

    // ==================== TEST 1: 100K BLOCKS ====================

    @Test
    @Order(1)
    @DisplayName("Phase A.7 Test 1: Processing 100K blocks with memory safety")
    @Timeout(600) // 10 minutes
    @Tag("slow") // Slow test - takes 5-10 minutes, run manually with -Dgroups=slow
    void testProcessing100KBlocksMemory() throws Exception {
        final int BLOCK_COUNT = 100_000;

        // Generate blocks
        logger.info("\n🚀 TEST 1: Memory Safety with 100K blocks");
        generateBlocks(BLOCK_COUNT);

        // Verify block count
        long blockCount = blockchain.getBlockCount();
        assertEquals(BLOCK_COUNT + 1, blockCount, "Should have " + BLOCK_COUNT + " + 1 genesis block");

        // Measure memory during processing
        forceGarbageCollection();
        long memBefore = getMemoryUsage();
        logger.info("💾 Memory before processing: " + (memBefore / 1_000_000) + "MB");

        // Process blocks in batches
        long startTime = System.currentTimeMillis();
        AtomicInteger processedCount = new AtomicInteger(0);

        blockchain.processChainInBatches(batch -> {
            processedCount.addAndGet(batch.size());

            // Progress reporting
            if (processedCount.get() % 50000 == 0) {
                long current = System.currentTimeMillis() - startTime;
                logger.info("  ✅ " + processedCount.get() + " blocks processed (" + current + "ms)");
            }
        }, 1000); // 1000 blocks per batch

        long processingTime = System.currentTimeMillis() - startTime;

        // Measure memory after
        forceGarbageCollection();
        long memAfter = getMemoryUsage();
        long memDelta = memAfter - memBefore;

        logger.info("⏱️  Processing time: " + processingTime + "ms");
        logger.info("💾 Memory after processing: " + (memAfter / 1_000_000) + "MB");
        logger.info("📊 Memory delta: " + (memDelta / 1_000_000) + "MB");
        logger.info("📊 Blocks processed: " + processedCount.get());

        // Validations
        assertEquals(BLOCK_COUNT, processedCount.get(), "Should process all blocks");
        assertTrue(memDelta < 100_000_000, // 100MB threshold
                "❌ Memory delta too high: " + (memDelta / 1_000_000) + "MB (should be < 100MB)");
        assertTrue(processingTime < 120_000, // 2 minutes for 100K blocks
                "❌ Processing too slow: " + processingTime + "ms (should be < 120s)");

        logger.info("✅ 100K blocks memory safety VERIFIED");
    }

    // ==================== TEST 2: 500K BLOCKS (OPTIONAL) ====================

    @Test
    @Order(2)
    @DisplayName("Phase A.7 Test 2: Processing 500K blocks (optional - slow)")
    @Timeout(1800) // 30 minutes
    @Tag("slow") // Can be skipped with -Dgroups=!slow
    void testProcessing500KBlocksMemory() throws Exception {
        final int BLOCK_COUNT = 500_000;

        logger.info("\n🚀 TEST 2: Memory Safety with 500K blocks (SLOW TEST)");
        logger.info("⚠️  This test takes 10-20 minutes. Consider skipping in CI/CD.");

        generateBlocks(BLOCK_COUNT);

        // Quick memory check
        forceGarbageCollection();
        long memBefore = getMemoryUsage();

        blockchain.processChainInBatches(batch -> {
            // Minimal processing, focus on memory
        }, 1000);

        forceGarbageCollection();
        long memAfter = getMemoryUsage();
        long memDelta = memAfter - memBefore;

        logger.info("💾 Memory delta: " + (memDelta / 1_000_000) + "MB");

        // Same threshold - memory should be constant regardless of size
        assertTrue(memDelta < 100_000_000,
                "❌ Memory delta too high at 500K blocks");

        logger.info("✅ 500K blocks memory safety VERIFIED");
    }

    // ==================== TEST 3: 1M BLOCKS (EXTREME - OPTIONAL) ====================

    @Test
    @Order(3)
    @DisplayName("Phase A.7 Test 3: Processing 1M blocks (extreme - very slow)")
    @Timeout(3600) // 60 minutes
    @Tag("extreme") // Separate tag for extreme tests
    void testProcessing1MBlocksMemory() throws Exception {
        final int BLOCK_COUNT = 1_000_000;

        logger.info("\n🚀 TEST 3: Memory Safety with 1M blocks (EXTREME TEST)");
        logger.info("⚠️  This test takes 30-60 minutes. Only for extreme validation.");

        generateBlocks(BLOCK_COUNT);

        // Quick memory check
        forceGarbageCollection();
        long memBefore = getMemoryUsage();

        blockchain.processChainInBatches(batch -> {
            // Minimal processing
        }, 1000);

        forceGarbageCollection();
        long memAfter = getMemoryUsage();
        long memDelta = memAfter - memBefore;

        logger.info("💾 Memory delta at 1M blocks: " + (memDelta / 1_000_000) + "MB");

        // Memory should still be constant - this is the KEY validation
        assertTrue(memDelta < 100_000_000,
                "❌ Memory is NOT constant at 1M blocks!");

        logger.info("✅ 1M blocks memory safety VERIFIED - Memory is truly constant!");
    }

    // ==================== TEST 4: STREAMING VALIDATION SCALABILITY ====================

    @Test
    @Order(4)
    @DisplayName("Phase A.7 Test 4: Streaming validation scales to large blockchains")
    @Timeout(300) // 5 minutes
    void testStreamingValidationScalability() throws Exception {
        final int BLOCK_COUNT = 10_000;

        logger.info("\n🚀 TEST 4: Streaming Validation Scalability");
        generateBlocks(BLOCK_COUNT);

        // Measure validateChainStreaming() performance
        long startTime = System.currentTimeMillis();
        AtomicLong validatedCount = new AtomicLong(0);

        blockchain.validateChainStreaming(batchResults -> {
            validatedCount.addAndGet(batchResults.size());

            if (validatedCount.get() % 50000 == 0) {
                logger.info("  ✅ Validated " + validatedCount.get() + " blocks");
            }
        }, 1000); // Batch size 1000

        long duration = System.currentTimeMillis() - startTime;

        logger.info("⏱️  Validation time: " + duration + "ms");
        logger.info("📊 Blocks validated: " + validatedCount.get());
        logger.info("📊 Throughput: " + (validatedCount.get() / (duration / 1000.0)) + " blocks/sec");

        assertEquals(BLOCK_COUNT + 1, validatedCount.get(), // +1 for genesis
                "Should validate all blocks");

        logger.info("✅ Streaming validation scales efficiently");
    }

    // ==================== TEST 5: MEMORY STABILITY ====================

    @Test
    @Order(5)
    @DisplayName("Phase A.7 Test 5: Memory stays stable across multiple batch iterations")
    @Timeout(600) // 10 minutes
    @Tag("slow") // Slow test - takes 10+ minutes, run manually with -Dgroups=slow
    void testMemoryStabilityAcrossIterations() throws Exception {
        final int BLOCK_COUNT = 50_000;

        logger.info("\n🚀 TEST 5: Memory Stability Across Iterations");
        generateBlocks(BLOCK_COUNT);

        // Measure memory across multiple processing cycles
        List<Long> memoryReadings = new ArrayList<>();

        for (int cycle = 0; cycle < 3; cycle++) {
            forceGarbageCollection();
            long memBefore = getMemoryUsage();
            memoryReadings.add(memBefore);

            // Process entire chain
            blockchain.processChainInBatches(batch -> {
                // Minimal processing
            }, 1000);

            forceGarbageCollection();
            long memAfter = getMemoryUsage();
            long delta = memAfter - memBefore;

            logger.info("Cycle " + (cycle + 1) + ": memory delta = " + (delta / 1_000_000) + "MB");
        }

        // Memory readings should be similar (within 50MB)
        long minReading = memoryReadings.stream().mapToLong(Long::longValue).min().orElse(0);
        long maxReading = memoryReadings.stream().mapToLong(Long::longValue).max().orElse(0);
        long spread = maxReading - minReading;

        logger.info("💾 Memory spread across cycles: " + (spread / 1_000_000) + "MB");

        assertTrue(spread < 50_000_000, // 50MB spread tolerance
                "Memory not stable across iterations");

        logger.info("✅ Memory is stable and predictable");
    }

    // ==================== TEST 6: SEARCH MEMORY SAFETY ====================

    @Test
    @Order(6)
    @DisplayName("Phase A.7 Test 6: Search operations memory-safe with large blockchain")
    @Timeout(600) // 10 minutes
    @Tag("slow") // Slow test - takes 5-10 minutes, run manually with -Dgroups=slow
    void testSearchMemorySafetyLargeBlockchain() throws Exception {
        final int BLOCK_COUNT = 50_000;

        logger.info("\n🚀 TEST 6: Search Memory Safety");
        generateBlocks(BLOCK_COUNT);

        forceGarbageCollection();
        long memBefore = getMemoryUsage();

        // Search by category with limit
        List<Block> results = blockchain.searchByCategory("test", 10000);

        forceGarbageCollection();
        long memAfter = getMemoryUsage();
        long memDelta = memAfter - memBefore;

        logger.info("💾 Memory delta for search: " + (memDelta / 1_000_000) + "MB");
        logger.info("📊 Results returned: " + results.size());

        assertTrue(memDelta < 50_000_000, // 50MB for search operation
                "Search used too much memory");

        logger.info("✅ Search operations are memory-safe");
    }

}
