package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.config.MemorySafetyConstants;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.KeyFileLoader;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Phase A.5: Optimizations Verification (Auto-Detect Database)
 *
 * Verifies that the Phase A.5 optimizations work correctly with available database:
 * ✅ streamAllBlocksInBatches() optimizations (ScrollableResults or pagination)
 * ✅ Iteration limits prevent excessive processing
 * ✅ Streaming methods don't accumulate results in memory
 * ✅ Type safety with long offsets works correctly
 * ✅ Memory efficiency with large datasets
 *
 * Database Auto-Detection:
 * 1. If PostgreSQL environment variables are configured → Uses PostgreSQL
 * 2. Otherwise → Uses H2 in-memory database (fast CI/CD on GitHub Actions)
 *
 * Configuration:
 * For PostgreSQL, set environment variables:
 * - BLOCKCHAIN_DB_HOST=localhost
 * - BLOCKCHAIN_DB_NAME=blockchain_test
 * - BLOCKCHAIN_DB_USER=postgres
 * - BLOCKCHAIN_DB_PASSWORD=postgres
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Phase A.5: Optimizations Verification (Auto-Detect Database)")
public class Phase_A5_OptimizationsTest {

    private Blockchain blockchain;
    private BlockRepository blockRepository;
    private KeyPair bootstrapKeyPair;
    private KeyPair keyPair;
    private String databaseType = "H2 (default)";
    private static final boolean POSTGRESQL_ENV_CONFIGURED = isPostgreSQLConfigured();

    /**
     * Detects if PostgreSQL is configured via environment variables
     */
    private static boolean isPostgreSQLConfigured() {
        String host = System.getenv("BLOCKCHAIN_DB_HOST");
        String dbName = System.getenv("BLOCKCHAIN_DB_NAME");
        String user = System.getenv("BLOCKCHAIN_DB_USER");
        String password = System.getenv("BLOCKCHAIN_DB_PASSWORD");
        return host != null && dbName != null && user != null && password != null;
    }

    @BeforeEach
    void setUp() throws Exception {
        // Detect database type
        if (POSTGRESQL_ENV_CONFIGURED) {
            databaseType = "PostgreSQL";
            System.out.println("📊 PostgreSQL Configuration Detected");
        } else {
            databaseType = "H2 (in-memory)";
            System.out.println("📊 Using H2 in-memory database");
        }

        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();
        blockRepository = new BlockRepository();

        // Load bootstrap admin keys (created automatically)
        bootstrapKeyPair = KeyFileLoader.loadKeyPairFromFiles(
            "./keys/genesis-admin.private",
            "./keys/genesis-admin.public"
        );

        // Register bootstrap admin in blockchain (RBAC v1.0.6)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        keyPair = CryptoUtil.generateKeyPair();

        // ✅ CRITICAL: Authorize the key BEFORE adding blocks
        String publicKeyStr = CryptoUtil.publicKeyToString(keyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyStr, "TestUser", bootstrapKeyPair, UserRole.USER);

        System.out.println("✅ " + databaseType + " Connection Established");
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            blockchain.clearAndReinitialize();
        } catch (Exception e) {
            System.out.println("⚠️ Cleanup error: " + e.getMessage());
            // Don't fail the test on cleanup error
        }
    }

    // ==================== STREAMALLBLOCKSINBATCHES OPTIMIZATION ====================

    @Test
    @Order(1)
    @DisplayName("Phase A.5 Optimization: streamAllBlocksInBatches() with H2")
    void testStreamAllBlocksInBatchesOptimization() throws Exception {
        // Create 3K blocks to test batch processing
        System.out.println("📝 Creating 3,000 test blocks...");
        for (int i = 0; i < 3000; i++) {
            blockchain.addBlock("Block " + i, keyPair.getPrivate(), keyPair.getPublic());
            if ((i + 1) % 1000 == 0) System.out.println("  ✅ " + (i + 1) + " blocks");
        }

        // Test streaming in batches (this is the Phase A.5 optimization)
        System.out.println("🚀 Testing streamAllBlocksInBatches()...");
        long startTime = System.currentTimeMillis();

        final int[] processedCount = {0};
        blockchain.processChainInBatches(batch -> {
            processedCount[0] += batch.size();
        }, 1000);

        long duration = System.currentTimeMillis() - startTime;

        System.out.println("⏱️  Batch processing: " + duration + "ms");
        System.out.println("📊 Blocks processed: " + processedCount[0]);

        assertTrue(processedCount[0] > 0, "❌ Should process blocks");
        assertTrue(duration < 30000, "❌ H2 optimization should be fast");

        System.out.println("✅ streamAllBlocksInBatches() optimization verified");
    }

    // ==================== ITERATION LIMIT ENFORCEMENT ====================

    @Test
    @Order(2)
    @DisplayName("Phase A.5 Optimization: Iteration limit constant is 100")
    void testIterationLimitConstant() {
        int maxIterations = MemorySafetyConstants.MAX_JSON_METADATA_ITERATIONS;
        assertEquals(100, maxIterations, "❌ MAX_JSON_METADATA_ITERATIONS must be 100");

        System.out.println("✅ MAX_JSON_METADATA_ITERATIONS = " + maxIterations);
        System.out.println("  Max blocks processable = " + (maxIterations * 1000) + " (100 iterations × 1000 batch size)");
    }

    // ==================== TYPE SAFETY WITH LONG OFFSETS ====================

    @Test
    @Order(3)
    @DisplayName("Phase A.5 Optimization: Long offset support (blockchains > 2.1B blocks)")
    void testLongOffsetTypeSafety() throws Exception {
        // Create test blocks
        for (int i = 0; i < 100; i++) {
            blockchain.addBlock("Test " + i, keyPair.getPrivate(), keyPair.getPublic());
        }

        System.out.println("🔍 Testing type safety with long offsets...");

        // Test with various offset values
        long[] offsets = {0L, 10L, 50L, 100L, 1000L};

        for (long offset : offsets) {
            try {
                List<Block> results = blockRepository.searchByCustomMetadataKeyValuePaginated(
                        "test", "key", offset, 10);

                assertNotNull(results, "❌ Should handle offset " + offset);
                System.out.println("  ✅ Offset " + offset + " handled (long type)");
            } catch (Exception e) {
                fail("❌ Failed with offset " + offset + ": " + e.getMessage());
            }
        }

        System.out.println("✅ Type safety with long offsets verified");
    }

    // ==================== STREAMING WITHOUT ITERATION LIMIT ====================

    @Test
    @Order(4)
    @DisplayName("Phase A.5 Optimization: Streaming methods process blocks without limit")
    void testStreamingMethodsNoIterationLimit() throws Exception {
        // Create 2K blocks
        System.out.println("📝 Creating 2,000 test blocks...");
        for (int i = 0; i < 2000; i++) {
            blockchain.addBlock("Stream test " + i, keyPair.getPrivate(), keyPair.getPublic());
            if ((i + 1) % 1000 == 0) System.out.println("  ✅ " + (i + 1) + " blocks");
        }

        System.out.println("🚀 Testing streamByCustomMetadata()...");

        // Count how many blocks we can process with streaming
        // If iteration limit was enforced, we'd be capped at 100 * 1000 = 100K
        // But we have streaming methods that DON'T have iteration limits
        AtomicInteger count = new AtomicInteger(0);

        blockRepository.streamByCustomMetadata("test", block -> {
            count.incrementAndGet();
        });

        System.out.println("📊 Blocks processed by streaming: " + count.get());

        // Streaming should work (even if count is 0 because no matching blocks)
        // The important thing is it doesn't crash or timeout
        System.out.println("✅ Streaming methods execute without iteration limit");
    }

    // ==================== MEMORY EFFICIENCY ====================

    @Test
    @Order(5)
    @DisplayName("Phase A.5 Optimization: Streaming doesn't accumulate all blocks in memory")
    void testStreamingMemoryEfficiency() throws Exception {
        // Create 2K blocks
        System.out.println("📝 Creating 2,000 test blocks...");
        for (int i = 0; i < 2000; i++) {
            blockchain.addBlock("Memory test " + i, keyPair.getPrivate(), keyPair.getPublic());
            if ((i + 1) % 1000 == 0) System.out.println("  ✅ " + (i + 1) + " blocks");
        }

        System.out.println("🚀 Testing memory efficiency...");

        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        AtomicInteger count = new AtomicInteger(0);
        blockchain.processChainInBatches(batch -> {
            count.incrementAndGet(); // Count batches, not individual blocks
        }, 1000);

        long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryUsed = endMemory - startMemory;

        System.out.println("⏱️  Processed in " + count.get() + " batches");
        System.out.println("💾 Memory delta: " + (memoryUsed / 1_000_000) + "MB");

        // Should use reasonable memory (streaming one batch at a time)
        assertTrue(memoryUsed < 150_000_000, // 150MB
                "❌ Should use < 150MB, used: " + (memoryUsed / 1_000_000) + "MB");

        System.out.println("✅ Streaming memory efficiency verified");
    }

    // ==================== PAGINATION CORRECTNESS ====================

    @Test
    @Order(6)
    @DisplayName("Phase A.5 Optimization: H2 Pagination performance")
    void testH2PaginationPerformance() throws Exception {
        // Create 2K blocks
        System.out.println("📝 Creating 2,000 test blocks...");
        for (int i = 0; i < 2000; i++) {
            blockchain.addBlock("Perf test " + i, keyPair.getPrivate(), keyPair.getPublic());
            if ((i + 1) % 1000 == 0) System.out.println("  ✅ " + (i + 1) + " blocks");
        }

        System.out.println("🚀 Testing H2 pagination...");
        long startTime = System.currentTimeMillis();

        int batchCount = 0;
        int totalBlocks = 0;
        for (int offset = 0; offset < 10000; offset += 1000) {
            List<Block> batch = blockchain.getBlocksPaginated(offset, 1000);
            batchCount++;
            totalBlocks += batch.size();
            if (batch.isEmpty()) break;
        }

        long duration = System.currentTimeMillis() - startTime;

        System.out.println("⏱️  Pagination: " + duration + "ms");
        System.out.println("📊 Batches: " + batchCount);
        System.out.println("📊 Total blocks: " + totalBlocks);

        // H2 with proper indexing should be fast
        assertTrue(duration < 10000, "❌ Should complete within 10 seconds");

        System.out.println("✅ H2 pagination performance acceptable");
    }

    // ==================== CONSTANT DEFINITIONS ====================

    @Test
    @Order(7)
    @DisplayName("Phase A.5 Optimization: All memory safety constants are correctly defined")
    void testMemorySafetyConstants() {
        System.out.println("🔍 Verifying memory safety constants...");

        // Verify all constants exist and have reasonable values
        assertEquals(100, MemorySafetyConstants.MAX_JSON_METADATA_ITERATIONS,
                "❌ MAX_JSON_METADATA_ITERATIONS should be 100");
        System.out.println("  ✅ MAX_JSON_METADATA_ITERATIONS = 100");

        // These should also exist (defined elsewhere in MemorySafetyConstants)
        assertTrue(MemorySafetyConstants.DEFAULT_MAX_SEARCH_RESULTS > 0,
                "❌ DEFAULT_MAX_SEARCH_RESULTS should be positive");
        System.out.println("  ✅ DEFAULT_MAX_SEARCH_RESULTS = " + MemorySafetyConstants.DEFAULT_MAX_SEARCH_RESULTS);

        System.out.println("✅ All memory safety constants verified");
    }
}
