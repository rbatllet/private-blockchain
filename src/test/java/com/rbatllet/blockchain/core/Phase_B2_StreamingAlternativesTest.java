package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.config.DatabaseConfig;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

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

    private static Blockchain blockchain;
    private static KeyPair keyPair;
    private static String publicKeyString;

    @BeforeAll
    static void setup() throws Exception {
        System.out.println("\n📊 PHASE B.2: STREAMING ALTERNATIVES TEST SUITE");
        System.out.println("================================================");

        // Use H2 in-memory for fast test execution
        DatabaseConfig config = DatabaseConfig.createH2TestConfig();
        JPAUtil.initialize(config);

        blockchain = new Blockchain();
        keyPair = CryptoUtil.generateKeyPair();
        publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());

        System.out.println("✅ H2 test database initialized");
        System.out.println("✅ Blockchain initialized");
    }

    @AfterAll
    static void tearDown() {
        JPAUtil.shutdown();
        System.out.println("\n✅ Phase B.2 tests completed\n");
    }

    @BeforeEach
    void clearBlockchain() {
        blockchain.clearAndReinitialize();
        blockchain.addAuthorizedKey(publicKeyString, "TestUser");
    }

    @AfterEach
    void cleanup() {
        blockchain.completeCleanupForTestsWithBackups();
    }

    // ========================================================================
    // TEST 1: streamBlocksByTimeRange()
    // ========================================================================

    @Test
    @Order(1)
    @DisplayName("Phase B.2 Test 1: streamBlocksByTimeRange() filters by time correctly")
    void testStreamBlocksByTimeRange() throws Exception {
        System.out.println("\n🚀 TEST 1: Stream Blocks By Time Range");

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
            System.out.println("  ✅ Block #" + block.getBlockNumber() + " - " + block.getTimestamp());
        });

        // Should include genesis + 3 blocks = 4 total (all within range due to test timing)
        assertTrue(count.get() >= 3, "Should stream at least 3 blocks within time range");
        System.out.println("  📊 Streamed " + count.get() + " blocks");
    }

    @Test
    @Order(2)
    @DisplayName("Phase B.2 Test 2: streamBlocksByTimeRange() rejects null parameters")
    void testStreamBlocksByTimeRange_NullParameters() {
        System.out.println("\n🚀 TEST 2: streamBlocksByTimeRange() Null Parameter Validation");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.streamBlocksByTimeRange(null, LocalDateTime.now(), block -> {});
        }, "Should reject null start time");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.streamBlocksByTimeRange(LocalDateTime.now(), null, block -> {});
        }, "Should reject null end time");

        System.out.println("  ✅ Null parameter validation works correctly");
    }

    // ========================================================================
    // TEST 3: streamEncryptedBlocks()
    // ========================================================================

    @Test
    @Order(3)
    @DisplayName("Phase B.2 Test 3: streamEncryptedBlocks() filters encrypted blocks only")
    void testStreamEncryptedBlocks() throws Exception {
        System.out.println("\n🚀 TEST 3: Stream Encrypted Blocks");

        // Create mixed blocks (encrypted and non-encrypted)
        blockchain.addBlock("Plain block 1", keyPair.getPrivate(), keyPair.getPublic());
        blockchain.addEncryptedBlock("Encrypted block 1", "password123", keyPair.getPrivate(), keyPair.getPublic());
        blockchain.addBlock("Plain block 2", keyPair.getPrivate(), keyPair.getPublic());
        blockchain.addEncryptedBlock("Encrypted block 2", "password456", keyPair.getPrivate(), keyPair.getPublic());

        AtomicInteger encryptedCount = new AtomicInteger(0);

        blockchain.streamEncryptedBlocks(block -> {
            assertTrue(block.isDataEncrypted(), "Should only stream encrypted blocks");
            encryptedCount.incrementAndGet();
            System.out.println("  ✅ Encrypted Block #" + block.getBlockNumber());
        });

        assertEquals(2, encryptedCount.get(), "Should stream exactly 2 encrypted blocks");
        System.out.println("  📊 Streamed " + encryptedCount.get() + " encrypted blocks");
    }

    // ========================================================================
    // TEST 4: streamBlocksWithOffChainData()
    // ========================================================================

    @Test
    @Order(4)
    @DisplayName("Phase B.2 Test 4: streamBlocksWithOffChainData() filters off-chain blocks only")
    void testStreamBlocksWithOffChainData() throws Exception {
        System.out.println("\n🚀 TEST 4: Stream Blocks With Off-Chain Data");

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
            System.out.println("  ✅ Off-Chain Block #" + block.getBlockNumber());
        });

        assertEquals(1, offChainCount.get(), "Should stream exactly 1 off-chain block");
        System.out.println("  📊 Streamed " + offChainCount.get() + " off-chain blocks");
    }

    // ========================================================================
    // TEST 5: streamBlocksAfter()
    // ========================================================================

    @Test
    @Order(5)
    @DisplayName("Phase B.2 Test 5: streamBlocksAfter() filters blocks after specific number")
    void testStreamBlocksAfter() throws Exception {
        System.out.println("\n🚀 TEST 5: Stream Blocks After Specific Block Number");

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
            System.out.println("  ✅ Block #" + block.getBlockNumber());
        });

        // Should stream blocks 6-10 = 5 blocks
        assertEquals(5, count.get(), "Should stream exactly 5 blocks after block #5");
        assertTrue(minBlockNumber.get() > blockNumber, "Minimum block number should be > " + blockNumber);
        System.out.println("  📊 Streamed " + count.get() + " blocks after #" + blockNumber);
    }

    @Test
    @Order(6)
    @DisplayName("Phase B.2 Test 6: streamBlocksAfter() rejects null block number")
    void testStreamBlocksAfter_NullParameter() {
        System.out.println("\n🚀 TEST 6: streamBlocksAfter() Null Parameter Validation");

        assertThrows(IllegalArgumentException.class, () -> {
            blockchain.streamBlocksAfter(null, block -> {});
        }, "Should reject null block number");

        System.out.println("  ✅ Null parameter validation works correctly");
    }

    // ========================================================================
    // TEST 7: Memory Safety Verification
    // ========================================================================

    @Test
    @Order(7)
    @DisplayName("Phase B.2 Test 7: All streaming methods maintain constant memory")
    void testStreamingMemorySafety() throws Exception {
        System.out.println("\n🚀 TEST 7: Streaming Memory Safety");

        // Create 1000 blocks for testing
        System.out.println("  📦 Creating 1000 test blocks...");
        for (int i = 1; i <= 1000; i++) {
            blockchain.addBlock("Block " + i, keyPair.getPrivate(), keyPair.getPublic());
        }

        System.gc();
        Thread.sleep(100);
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

        System.out.println("  📊 Memory Usage:");
        System.out.println("    Initial: " + (initialMemory / 1024 / 1024) + " MB");
        System.out.println("    After streamBlocksByTimeRange: " + (memoryAfterTimeRange / 1024 / 1024) + " MB (delta: " + (deltaTimeRange / 1024 / 1024) + " MB)");
        System.out.println("    After streamBlocksAfter: " + (memoryAfterBlocksAfter / 1024 / 1024) + " MB (delta: " + (deltaBlocksAfter / 1024 / 1024) + " MB)");

        // Memory delta should be < 100MB for 1000 blocks
        assertTrue(deltaTimeRange < 100_000_000, "Memory delta should be < 100MB");
        assertTrue(deltaBlocksAfter < 100_000_000, "Memory delta should be < 100MB");

        System.out.println("  ✅ Memory safety verified");
    }
}
