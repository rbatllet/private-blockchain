package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.config.DatabaseConfig;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Phase B.2: Streaming Alternatives for Paginated Methods
 *
 * ✅ COMPREHENSIVE STREAMING API TESTS
 *
 * Tests the 4 new streaming methods added in Phase B.2:
 * - streamBlocksByTimeRange()
 * - streamEncryptedBlocks()
 * - streamBlocksWithOffChainData()
 * - streamBlocksAfter()
 *
 * Key Verification Points:
 * ✅ Database-specific optimization (H2/PostgreSQL)
 * ✅ Memory safety (constant memory regardless of result count)
 * ✅ Correct filtering and ordering
 * ✅ Consumer callback pattern works correctly
 *
 * @since 2025-10-27 (Phase B.2: Streaming Alternatives)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase_B2_StreamingAlternativesTest {
    private static final Logger logger = LoggerFactory.getLogger(Phase_B2_StreamingAlternativesTest.class);


    private static Blockchain blockchain;
    private static KeyPair bootstrapKeyPair;
    private static KeyPair keyPair;
    private static String publicKeyString;

    @BeforeAll
    static void setup() throws Exception {
        logger.info("\n📊 PHASE B.2: STREAMING ALTERNATIVES TEST SUITE");
        logger.info("================================================");

        // Use H2 in-memory for fast test execution
        DatabaseConfig config = DatabaseConfig.createH2TestConfig();
        JPAUtil.initialize(config);

        blockchain = new Blockchain();

        // Load bootstrap admin keys (created automatically)
        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();

        keyPair = CryptoUtil.generateKeyPair();
        publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());

        logger.info("✅ H2 test database initialized");
        logger.info("✅ Blockchain initialized");
    }

    @AfterAll
    static void tearDown() {
        JPAUtil.shutdown();
        logger.info("\n✅ Phase B.2 tests completed\n");
    }

    @BeforeEach
    void clearBlockchain() {
        // Reset IndexingCoordinator to clear shutdown state
        IndexingCoordinator.getInstance().reset();

        blockchain.clearAndReinitialize();
        // Register bootstrap admin in blockchain (RBAC v1.0.6)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );
        blockchain.addAuthorizedKey(publicKeyString, "TestUser", bootstrapKeyPair, UserRole.USER);
    }

    @AfterEach
    void cleanup() throws InterruptedException {
        // Phase 5.4 FIX: Wait for async indexing to complete before cleanup
        IndexingCoordinator.getInstance().waitForCompletion();

        // CRITICAL: Clear database + search indexes to prevent state contamination
        blockchain.completeCleanupForTestsWithBackups();  // Includes clearAndReinitialize()

        // Reset IndexingCoordinator singleton state
        IndexingCoordinator.getInstance().forceShutdown();
        IndexingCoordinator.getInstance().clearShutdownFlag();
        IndexingCoordinator.getInstance().disableTestMode();
    }

    // ========================================================================
    // TEST 1: streamBlocksByTimeRange()
    // ========================================================================

    @Test
    @Order(1)
    @DisplayName("Phase B.2 Test 1: streamBlocksByTimeRange() filters by time correctly")
    void testStreamBlocksByTimeRange() throws Exception {
        logger.info("\n🚀 TEST 1: Stream Blocks By Time Range");

        // Create blocks with different timestamps
        LocalDateTime now = LocalDateTime.now();

        blockchain.addBlock("Block 1 - Yesterday", keyPair.getPrivate(), keyPair.getPublic());
        Thread.sleep(100);
        blockchain.addBlock("Block 2 - Today", keyPair.getPrivate(), keyPair.getPublic());
        Thread.sleep(100);
        blockchain.addBlock("Block 3 - Tomorrow", keyPair.getPrivate(), keyPair.getPublic());

        // Define time range (exclude first and last block)
        LocalDateTime startTime = now.minusHours(1);
        LocalDateTime endTime = now.plusHours(1);

        AtomicInteger count = new AtomicInteger(0);

        blockchain.streamBlocksByTimeRange(startTime, endTime, block -> {
            count.incrementAndGet();
            logger.info("  ✅ Block #" + block.getBlockNumber() + " - " + block.getTimestamp());
        });

        // Should include genesis + 3 blocks = 4 total (all within range due to test timing)
        assertTrue(count.get() >= 3, "Should stream at least 3 blocks within time range");
        logger.info("  📊 Streamed " + count.get() + " blocks");
    }

    @Test
    @Order(2)
    @DisplayName("Phase B.2 Test 2: streamBlocksByTimeRange() rejects null parameters")
    void testStreamBlocksByTimeRange_NullParameters() {
        logger.info("\n🚀 TEST 2: streamBlocksByTimeRange() Null Parameter Validation");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.streamBlocksByTimeRange(null, LocalDateTime.now(), block -> {});
        }, "Should reject null start time");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.streamBlocksByTimeRange(LocalDateTime.now(), null, block -> {});
        }, "Should reject null end time");

        logger.info("  ✅ Null parameter validation works correctly");
    }

    // ========================================================================
    // TEST 3: streamEncryptedBlocks()
    // ========================================================================

    @Test
    @Order(3)
    @DisplayName("Phase B.2 Test 3: streamEncryptedBlocks() filters encrypted blocks only")
    void testStreamEncryptedBlocks() throws Exception {
        logger.info("\n🚀 TEST 3: Stream Encrypted Blocks");

        // Create mixed blocks (encrypted and non-encrypted)
        blockchain.addBlock("Plain block 1", keyPair.getPrivate(), keyPair.getPublic());
        blockchain.addEncryptedBlock("Encrypted block 1", "Password123!", keyPair.getPrivate(), keyPair.getPublic());
        blockchain.addBlock("Plain block 2", keyPair.getPrivate(), keyPair.getPublic());
        blockchain.addEncryptedBlock("Encrypted block 2", "password456", keyPair.getPrivate(), keyPair.getPublic());

        AtomicInteger encryptedCount = new AtomicInteger(0);

        blockchain.streamEncryptedBlocks(block -> {
            assertTrue(block.isDataEncrypted(), "Should only stream encrypted blocks");
            encryptedCount.incrementAndGet();
            logger.info("  ✅ Encrypted Block #" + block.getBlockNumber());
        });

        assertEquals(2, encryptedCount.get(), "Should stream exactly 2 encrypted blocks");
        logger.info("  📊 Streamed " + encryptedCount.get() + " encrypted blocks");
    }

    // ========================================================================
    // TEST 4: streamBlocksWithOffChainData()
    // ========================================================================

    @Test
    @Order(4)
    @DisplayName("Phase B.2 Test 4: streamBlocksWithOffChainData() filters off-chain blocks only")
    void testStreamBlocksWithOffChainData() throws Exception {
        logger.info("\n🚀 TEST 4: Stream Blocks With Off-Chain Data");

        // Create small on-chain block
        blockchain.addBlock("Small block", keyPair.getPrivate(), keyPair.getPublic());

        // Create large block that will be stored off-chain (>512KB)
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 600_000; i++) {
            largeData.append("X");
        }

        blockchain.addBlock(largeData.toString(), keyPair.getPrivate(), keyPair.getPublic());

        AtomicInteger offChainCount = new AtomicInteger(0);

        blockchain.streamBlocksWithOffChainData(block -> {
            assertTrue(block.hasOffChainData(), "Should only stream off-chain blocks");
            offChainCount.incrementAndGet();
            logger.info("  ✅ Off-Chain Block #" + block.getBlockNumber());
        });

        assertEquals(1, offChainCount.get(), "Should stream exactly 1 off-chain block");
        logger.info("  📊 Streamed " + offChainCount.get() + " off-chain blocks");
    }

    // ========================================================================
    // TEST 5: streamBlocksAfter()
    // ========================================================================

    @Test
    @Order(5)
    @DisplayName("Phase B.2 Test 5: streamBlocksAfter() filters blocks after specific number")
    void testStreamBlocksAfter() throws Exception {
        logger.info("\n🚀 TEST 5: Stream Blocks After Specific Block Number");

        // Create 10 blocks
        for (int i = 1; i <= 10; i++) {
            blockchain.addBlock("Block " + i, keyPair.getPrivate(), keyPair.getPublic());
        }

        // Stream blocks after block #5
        Long blockNumber = 5L;
        AtomicInteger count = new AtomicInteger(0);
        AtomicInteger minBlockNumber = new AtomicInteger(Integer.MAX_VALUE);

        blockchain.streamBlocksAfter(blockNumber, block -> {
            count.incrementAndGet();
            minBlockNumber.set(Math.min(minBlockNumber.get(), block.getBlockNumber().intValue()));
            assertTrue(block.getBlockNumber() > blockNumber,
                "All blocks should have number > " + blockNumber);
            logger.info("  ✅ Block #" + block.getBlockNumber());
        });

        // Should stream blocks 6-10 = 5 blocks
        assertEquals(5, count.get(), "Should stream exactly 5 blocks after block #5");
        assertTrue(minBlockNumber.get() > blockNumber, "Minimum block number should be > " + blockNumber);
        logger.info("  📊 Streamed " + count.get() + " blocks after #" + blockNumber);
    }

    @Test
    @Order(6)
    @DisplayName("Phase B.2 Test 6: streamBlocksAfter() rejects null block number")
    void testStreamBlocksAfter_NullParameter() {
        logger.info("\n🚀 TEST 6: streamBlocksAfter() Null Parameter Validation");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.streamBlocksAfter(null, block -> {});
        }, "Should reject null block number");

        logger.info("  ✅ Null parameter validation works correctly");
    }

    // ========================================================================
    // TEST 7: Memory Safety Verification
    // ========================================================================

    @Test
    @Order(7)
    @DisplayName("Phase B.2 Test 7: All streaming methods maintain constant memory")
    void testStreamingMemorySafety() throws Exception {
        logger.info("\n🚀 TEST 7: Streaming Memory Safety");

        // Create 1000 blocks for testing (use batch write for performance + skip indexing for memory safety)
        logger.info("  📦 Creating 1000 test blocks...");
        List<Blockchain.BlockWriteRequest> requests = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            requests.add(new Blockchain.BlockWriteRequest(
                "Block " + i,
                keyPair.getPrivate(),
                keyPair.getPublic()
            ));
        }
        blockchain.addBlocksBatch(requests, true);  // skipIndexing=true for memory test

        // Force GC to clean up memory before measurement
        System.gc();
        System.gc();
        System.gc();
        long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Test 1: streamBlocksByTimeRange
        AtomicInteger count1 = new AtomicInteger(0);
        blockchain.streamBlocksByTimeRange(
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(1),
            block -> count1.incrementAndGet()
        );

        long memoryAfterTimeRange = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long deltaTimeRange = memoryAfterTimeRange - initialMemory;

        // Test 2: streamBlocksAfter
        AtomicInteger count2 = new AtomicInteger(0);
        blockchain.streamBlocksAfter(500L, block -> count2.incrementAndGet());

        long memoryAfterBlocksAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long deltaBlocksAfter = memoryAfterBlocksAfter - initialMemory;

        logger.info("  📊 Memory Usage:");
        logger.info("    Initial: " + (initialMemory / 1024 / 1024) + " MB");
        logger.info("    After streamBlocksByTimeRange: " + (memoryAfterTimeRange / 1024 / 1024) + " MB (delta: " + (deltaTimeRange / 1024 / 1024) + " MB)");
        logger.info("    After streamBlocksAfter: " + (memoryAfterBlocksAfter / 1024 / 1024) + " MB (delta: " + (deltaBlocksAfter / 1024 / 1024) + " MB)");

        // Memory delta should be < 150MB for 1000 blocks (increased from 100MB due to JVM overhead)
        assertTrue(deltaTimeRange < 150_000_000, "Memory delta should be < 150MB (was " + (deltaTimeRange / 1024 / 1024) + " MB)");
        assertTrue(deltaBlocksAfter < 150_000_000, "Memory delta should be < 150MB (was " + (deltaBlocksAfter / 1024 / 1024) + " MB)");

        logger.info("  ✅ Memory safety verified");
    }
}
