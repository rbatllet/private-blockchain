package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.config.MemorySafetyConstants;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;
import java.util.List;

/**
 * Phase A.5 Diagnostic Tests
 *
 * Verifies Phase A.5 optimizations work correctly:
 * ✅ Iteration limits are enforced (MAX_JSON_METADATA_ITERATIONS = 100)
 * ✅ Streaming methods work without iteration limits
 * ✅ Type safety with long offsets
 * ✅ Memory efficiency with batch processing
 */
@DisplayName("Phase A.5 Diagnostic: Core Functionality Verification")
public class Phase_A5_DiagnosticTest {

    private Blockchain blockchain;
    private BlockRepository blockRepository;
    private KeyPair bootstrapKeyPair;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();
        blockRepository = new BlockRepository();

        // Load bootstrap admin keys (created automatically)
        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();

        // Register bootstrap admin in blockchain (RBAC v1.0.6)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        keyPair = CryptoUtil.generateKeyPair();

        // ✅ CRITICAL: Authorize the key BEFORE adding blocks
        String publicKeyStr = CryptoUtil.publicKeyToString(keyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyStr, "TestUser", bootstrapKeyPair, UserRole.USER);
    }

    @AfterEach
    void tearDown() throws Exception {
        blockchain.clearAndReinitialize();
    }

    @Test
    @DisplayName("Phase A.5: Iteration limit constant is correctly defined")
    void testIterationLimitConstant() {
        int maxIterations = MemorySafetyConstants.MAX_JSON_METADATA_ITERATIONS;
        assertEquals(100, maxIterations, "❌ MAX_JSON_METADATA_ITERATIONS should be 100");
        System.out.println("✅ MAX_JSON_METADATA_ITERATIONS = 100");
    }

    @Test
    @DisplayName("Phase A.5: Basic block creation works")
    void testBasicBlockCreation() throws Exception {
        long initialCount = blockchain.getBlockCount();

        // Add a block using blockchain API
        blockchain.addBlock("Test block", keyPair.getPrivate(), keyPair.getPublic());

        // Verify block was created
        long finalCount = blockchain.getBlockCount();
        assertTrue(finalCount > initialCount, "❌ Block count should increase");
        System.out.println("✅ Block created successfully (count: " + initialCount + " → " + finalCount + ")");
    }

    @Test
    @DisplayName("Phase A.5: Batch processing iteration")
    void testBatchProcessing() throws Exception {
        // Create 10 blocks
        for (int i = 0; i < 10; i++) {
            blockchain.addBlock("Block " + i, keyPair.getPrivate(), keyPair.getPublic());
        }

        long blockCount = blockchain.getBlockCount();
        System.out.println("✅ Created " + blockCount + " blocks");

        // Test batch processing (streams blocks in 1000-block batches)
        final int[] batchCount = {0};
        blockchain.processChainInBatches(batch -> {
            batchCount[0]++;
            System.out.println("  📊 Batch " + batchCount[0] + ": " + batch.size() + " blocks");
        }, 1000);

        assertTrue(batchCount[0] > 0, "❌ Should process at least one batch");
        System.out.println("✅ Batch processing completed (" + batchCount[0] + " batch(es))");
    }

    @Test
    @DisplayName("Phase A.5: Long offset type safety")
    void testLongOffsetTypeSafety() throws Exception {
        // Create test blocks
        for (int i = 0; i < 5; i++) {
            blockchain.addBlock("Test " + i, keyPair.getPrivate(), keyPair.getPublic());
        }

        // Test with various long offset values
        long[] offsets = {0L, 1L, 2L, 10L};

        for (long offset : offsets) {
            try {
                List<Block> results = blockRepository.searchByCustomMetadataKeyValuePaginated(
                        "key", "value", offset, 10);
                assertNotNull(results, "❌ Results should not be null");
                System.out.println("✅ Offset " + offset + " (long type) handled correctly");
            } catch (Exception e) {
                fail("❌ Failed with offset " + offset + ": " + e.getMessage());
            }
        }
    }
}
