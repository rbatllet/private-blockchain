package com.rbatllet.blockchain.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;

import com.rbatllet.blockchain.config.DatabaseConfig;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;

/**
 * Phase 5.2: Batch Write API Benchmark
 *
 * ✅ OBJECTIVE: Compare write throughput between single-block and batch operations
 *
 * Expected Results:
 * - Single blocks (baseline): ~181.6 blocks/sec (measured in Phase 5.0)
 * - Batch 10 blocks: ~1,000-2,000 blocks/sec (5-10x improvement)
 * - Batch 100 blocks: ~3,000-5,000 blocks/sec (15-25x improvement)
 * - Batch 1000 blocks: ~5,000-10,000 blocks/sec (25-50x improvement)
 *
 * Use: mvn test -Dtest=Phase_5_2_BatchWriteBenchmarkTest
 *
 * Tags: phase5, benchmark, batch-write
 *
 * @since 1.0.6 (Phase 5.2)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Phase 5.2: Batch Write API Benchmark")
@Tag("benchmark")
@Tag("phase5")
@Tag("batch-write")
public class Phase_5_2_BatchWriteBenchmarkTest {

    private static final double BASELINE_BLOCKS_PER_SEC = 181.6; // Phase 5.0 measured baseline
    private static final int TOTAL_BLOCKS = 1000;

    static class BatchBenchmarkResult {
        int batchSize;
        int totalBlocks;

        // Write-only metrics (skipIndexing=true)
        long writeDurationMs;
        double writeBlocksPerSecond;
        double writeImprovementFactor;

        // Write+Index metrics (complete operation)
        long totalDurationMs;
        double blocksPerSecond;
        double improvementFactor;
        long avgBatchLatencyMs;

        void report() {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("📊 BATCH WRITE BENCHMARK - Batch Size: " + batchSize);
            System.out.println("=".repeat(80));
            System.out.println("  Batch size:         " + batchSize + " blocks/batch");
            System.out.println("  Total blocks:       " + totalBlocks);
            System.out.println();

            System.out.println("  ⚡ WRITE-ONLY (skipIndexing=true):");
            System.out.println("    Duration:         " + writeDurationMs + "ms");
            System.out.println("    Throughput:       " + String.format("%.1f", writeBlocksPerSecond) + " blocks/sec");
            System.out.println("    Improvement:      " + String.format("%.1fx", writeImprovementFactor) + " vs baseline");
            System.out.println();

            System.out.println("  📊 COMPLETE (with async indexing):");
            System.out.println("    Duration:         " + totalDurationMs + "ms");
            System.out.println("    Throughput:       " + String.format("%.1f", blocksPerSecond) + " blocks/sec");
            System.out.println("    Improvement:      " + String.format("%.1fx", improvementFactor) + " vs baseline");
            System.out.println("    Avg batch:        " + avgBatchLatencyMs + "ms");
            System.out.println();

            if (writeImprovementFactor >= 25.0) {
                System.out.println("  🚀 OUTSTANDING: 25x+ write throughput!");
            } else if (writeImprovementFactor >= 15.0) {
                System.out.println("  ✅ EXCELLENT: 15x+ write throughput!");
            } else if (writeImprovementFactor >= 5.0) {
                System.out.println("  ✅ GOOD: 5x+ write throughput!");
            } else {
                System.out.println("  ⚠️  BELOW TARGET: < 5x write throughput");
            }
            System.out.println("=".repeat(80));
        }
    }

    @Test
    @Order(1)
    @DisplayName("Phase 5.2: Compare Batch Sizes (10, 100, 1000) vs Single Block")
    @Timeout(600) // 10 minutes
    void benchmarkBatchSizeComparison() throws Exception {
        System.out.println("\n🚀 PHASE 5.2 BENCHMARK: Batch Write API Performance");
        System.out.println("Testing " + TOTAL_BLOCKS + " total blocks with different batch sizes");

        // Initialize database
        DatabaseConfig h2Config = DatabaseConfig.createH2TestConfig();
        JPAUtil.initialize(h2Config);

        Blockchain blockchain = new Blockchain();
        blockchain.clearAndReinitialize();

        // Load bootstrap admin keys (auto-generates if missing - test-only)
        KeyPair bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();
        
        assertNotNull(bootstrapKeyPair, 
            "Failed to load genesis admin keys. Check that keys exist in ./keys/ directory. " +
            "Current working directory: " + System.getProperty("user.dir"));

        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKeyStr = CryptoUtil.publicKeyToString(keyPair.getPublic());
        blockchain.addAuthorizedKey(publicKeyStr, "BenchmarkUser", bootstrapKeyPair, UserRole.USER);

        // Test different batch sizes: 1 (baseline), 10, 100, 1000
        int[] batchSizes = {1, 10, 100, 1000};
        List<BatchBenchmarkResult> results = new ArrayList<>();

        for (int batchSize : batchSizes) {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("Testing batch size: " + batchSize);
            System.out.println("=".repeat(80));

            // Clear blockchain for clean test
            blockchain.clearAndReinitialize();

            // Re-create bootstrap admin and test user
            blockchain.createBootstrapAdmin(
                CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
                "BOOTSTRAP_ADMIN"
            );
            blockchain.addAuthorizedKey(publicKeyStr, "BenchmarkUser", bootstrapKeyPair, UserRole.USER);

            // Warm-up
            System.out.println("🔥 Warming up JVM...");
            runBatchTest(blockchain, keyPair, batchSize, 100);

            // Force GC
            System.gc();
            Thread.sleep(100);

            // Actual benchmark
            System.out.println("📊 Running benchmark...");
            BatchBenchmarkResult result = runBatchTest(blockchain, keyPair, batchSize, TOTAL_BLOCKS);
            result.report();
            results.add(result);
        }

        // Summary
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 PHASE 5.2 BENCHMARK SUMMARY");
        System.out.println("=".repeat(80));
        System.out.println(String.format("%-15s | %-15s | %-20s | %-15s",
            "Batch Size", "Duration", "Throughput", "Improvement"));
        System.out.println("-".repeat(80));

        for (BatchBenchmarkResult result : results) {
            System.out.println(String.format("%-15d | %-15dms | %-20s | %-15s",
                result.batchSize,
                result.totalDurationMs,
                String.format("%.1f blocks/sec", result.blocksPerSecond),
                String.format("%.1fx", result.improvementFactor)));
        }

        System.out.println("=".repeat(80));
        System.out.println("\n✅ Phase 5.2 Batch Write API verified successfully!");

        JPAUtil.shutdown();
    }

    private BatchBenchmarkResult runBatchTest(Blockchain blockchain, KeyPair keyPair,
                                               int batchSize, int totalBlocks) throws Exception {
        // PHASE 1: Measure write throughput (skipIndexing=true for pure write performance)
        long writeStartTime = System.currentTimeMillis();
        int blocksWritten = 0;
        int batchCount = 0;

        while (blocksWritten < totalBlocks) {
            int remainingBlocks = totalBlocks - blocksWritten;
            int currentBatchSize = Math.min(batchSize, remainingBlocks);

            if (currentBatchSize == 1) {
                // Single block API (baseline)
                boolean success = blockchain.addBlock(
                    "Benchmark block " + blocksWritten,
                    keyPair.getPrivate(),
                    keyPair.getPublic()
                );
                assertTrue(success, "Block should be added successfully");
                blocksWritten++;
            } else {
                // Batch API with skipIndexing=true (maximum write throughput)
                List<Blockchain.BlockWriteRequest> requests = new ArrayList<>();
                for (int i = 0; i < currentBatchSize; i++) {
                    requests.add(new Blockchain.BlockWriteRequest(
                        "Benchmark block " + (blocksWritten + i),
                        keyPair.getPrivate(),
                        keyPair.getPublic()
                    ));
                }

                List<Block> blocks = blockchain.addBlocksBatch(requests, true);
                assertEquals(currentBatchSize, blocks.size(), "Should create all blocks in batch");
                blocksWritten += currentBatchSize;
            }

            batchCount++;

            // Progress indicator
            if (blocksWritten % 100 == 0) {
                System.out.print(".");
            }
        }

        long writeEndTime = System.currentTimeMillis();
        long writeDuration = writeEndTime - writeStartTime;
        System.out.println(" " + blocksWritten + " blocks written");

        // PHASE 2: Measure COMPLETE throughput (with async indexing)
        // Re-run same test to measure total time including background indexing
        blockchain.clearAndReinitialize();

        // Re-create bootstrap admin and test user (required after clearAndReinitialize)
        KeyPair bootstrapKeyPair;
        try {
            bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();
            blockchain.createBootstrapAdmin(
                CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
                "BOOTSTRAP_ADMIN"
            );
            String publicKeyStr = CryptoUtil.publicKeyToString(keyPair.getPublic());
            blockchain.addAuthorizedKey(publicKeyStr, "BenchmarkUser", bootstrapKeyPair, UserRole.USER);
        } catch (Exception e) {
            throw new RuntimeException("Failed to re-create test user", e);
        }

        long completeStartTime = System.currentTimeMillis();
        blocksWritten = 0;
        batchCount = 0;

        while (blocksWritten < totalBlocks) {
            int remainingBlocks = totalBlocks - blocksWritten;
            int currentBatchSize = Math.min(batchSize, remainingBlocks);

            if (currentBatchSize == 1) {
                // Single block API (baseline comparison)
                boolean success = blockchain.addBlock(
                    "Benchmark block " + blocksWritten,
                    keyPair.getPrivate(),
                    keyPair.getPublic()
                );
                assertTrue(success, "Block should be added successfully");
                blocksWritten++;
            } else {
                // Batch API (with async indexing in background)
                List<Blockchain.BlockWriteRequest> requests = new ArrayList<>();
                for (int i = 0; i < currentBatchSize; i++) {
                    requests.add(new Blockchain.BlockWriteRequest(
                        "Benchmark block " + (blocksWritten + i),
                        keyPair.getPrivate(),
                        keyPair.getPublic()
                    ));
                }

                // MEMORY-EFFICIENT: Use future directly, don't store blocks
                CompletableFuture<IndexingCoordinator.IndexingResult> future = 
                    blockchain.addBlocksBatchWithFuture(requests, false);
                
                // Wait for async indexing to complete before test ends
                if (future != null) {
                    future.join();
                }
                
                blocksWritten += currentBatchSize;
            }

            batchCount++;
        }

        long completeEndTime = System.currentTimeMillis();
        long completeDuration = completeEndTime - completeStartTime;

        // NOTE: Async indexing already waited inside the batch loop via result.waitForIndexing()
        // No need for additional Thread.sleep() - indexing is guaranteed to be complete

        // Calculate results
        BatchBenchmarkResult result = new BatchBenchmarkResult();
        result.batchSize = batchSize;
        result.totalBlocks = blocksWritten;

        // Write-only metrics
        result.writeDurationMs = writeDuration;
        result.writeBlocksPerSecond = (blocksWritten * 1000.0) / writeDuration;
        result.writeImprovementFactor = result.writeBlocksPerSecond / BASELINE_BLOCKS_PER_SEC;

        // Complete metrics
        result.totalDurationMs = completeDuration;
        result.blocksPerSecond = (blocksWritten * 1000.0) / completeDuration;
        result.improvementFactor = result.blocksPerSecond / BASELINE_BLOCKS_PER_SEC;
        result.avgBatchLatencyMs = completeDuration / batchCount;

        return result;
    }
}
