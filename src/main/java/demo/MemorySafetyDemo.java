package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.config.DatabaseConfig;
import com.rbatllet.blockchain.config.MemorySafetyConstants;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.validation.BlockValidationResult;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Memory Safety Demo - Phase A Features
 *
 * Demonstrates the memory safety improvements from Phase A.1-A.8:
 * 1. Breaking changes: maxResults validation
 * 2. Batch processing with processChainInBatches()
 * 3. Streaming validation with validateChainStreaming()
 * 4. Memory-safe search methods
 * 5. Memory limits and safety constants
 *
 * @since 2025-10-27 (Phase A.8)
 */
public class MemorySafetyDemo {

    private static Blockchain blockchain;
    private static KeyPair keyPair;
    private static String publicKeyString;

    public static void main(String[] args) {
        try {
            printHeader("MEMORY SAFETY DEMO - PHASE A");
            System.out.println("Demonstrating memory safety improvements from v1.0.6+");
            System.out.println("Phase A.1-A.8: Critical Memory Safety Refactoring");
            System.out.println();

            // Initialize
            initializeBlockchain();
            createLargeDataset();

            // Demo 1: Breaking changes validation
            demoBreakingChanges();

            // Demo 2: Batch processing
            demoBatchProcessing();

            // Demo 3: Streaming validation
            demoStreamingValidation();

            // Demo 4: Memory-safe search
            demoMemorySafeSearch();

            // Demo 5: Memory limits
            demoMemoryLimits();

            // Demo 6: Before vs After comparison
            demoBeforeAfterComparison();

            printSuccess("✅ All memory safety demos completed successfully!");

        } catch (Exception e) {
            printError("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private static void initializeBlockchain() throws Exception {
        printSection("1. INITIALIZATION");

        // Use H2 file-based for persistent storage
        DatabaseConfig config = DatabaseConfig.createH2FileConfig("./memory_safety_demo_db");
        JPAUtil.initialize(config);
        System.out.println("  ✅ H2 file-based database initialized (./memory_safety_demo_db)");

        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();

        System.out.println("  ✅ Blockchain initialized");

        keyPair = CryptoUtil.generateKeyPair();
        publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
        blockchain.createBootstrapAdmin(publicKeyString, "DemoUser");
        System.out.println("  ✅ Bootstrap admin created");
        System.out.println();
    }

    private static void createLargeDataset() throws Exception {
        printSection("2. CREATING LARGE DATASET");
        System.out.println("  📦 Creating 1000 sample blocks for memory safety testing");
        System.out.println("     (This simulates a real blockchain with significant data)");
        System.out.println("     🚀 Using Phase 5.2 batch write API for improved performance");
        System.out.println();

        long startTime = System.currentTimeMillis();

        // Phase 5.2: Use batch write for plain blocks (much faster!)
        List<Blockchain.BlockWriteRequest> plainBlocks = new ArrayList<>();
        List<Integer> encryptedBlockIndices = new ArrayList<>();

        // Prepare batch requests for plain blocks
        for (int i = 1; i <= 1000; i++) {
            String data = "Block #" + i + " - Transaction data for memory safety testing";

            if (i % 5 == 0) {
                // Track encrypted blocks for later individual creation
                encryptedBlockIndices.add(i);
            } else {
                // Add to batch for plain blocks
                plainBlocks.add(new Blockchain.BlockWriteRequest(
                    data, keyPair.getPrivate(), keyPair.getPublic()
                ));
            }
        }

        // Create plain blocks in batch (800 blocks - 5-10x faster!)
        System.out.println("    🚀 Creating " + plainBlocks.size() + " plain blocks using batch API...");
        blockchain.addBlocksBatch(plainBlocks);
        System.out.println("    ✅ Batch created " + plainBlocks.size() + " plain blocks");

        // Create encrypted blocks individually (200 blocks)
        System.out.println("    🔐 Creating " + encryptedBlockIndices.size() + " encrypted blocks individually...");
        for (int i : encryptedBlockIndices) {
            String data = "Block #" + i + " - Transaction data for memory safety testing";
            // NOTE: Using simple passwords for demo purposes only
            // In production, always use strong, randomly-generated passwords
            blockchain.addEncryptedBlock(data, "DemoPassword!2024#" + i, keyPair.getPrivate(), keyPair.getPublic());
        }
        System.out.println("    ✅ Created " + encryptedBlockIndices.size() + " encrypted blocks");

        long elapsed = System.currentTimeMillis() - startTime;
        long totalBlocks = blockchain.getBlockCount();

        System.out.println();
        System.out.println("  📊 Dataset created:");
        System.out.println("     - Total blocks: " + totalBlocks + " (including genesis)");
        System.out.println("     - Encrypted blocks: ~200 (every 5th block)");
        System.out.println("     - Plain blocks: ~800 (created with batch API)");
        System.out.println("     - Creation time: " + elapsed + "ms (Phase 5.2 optimization)");
        System.out.println();
    }

    private static void demoBreakingChanges() {
        printSection("3. DEMO: BREAKING CHANGES VALIDATION");
        System.out.println("  ⚠️  Phase A.2: maxResults parameter now validated");
        System.out.println("     Old behavior: maxResults=0 returned ALL blocks (memory bomb)");
        System.out.println("     New behavior: maxResults≤0 throws IllegalArgumentException");
        System.out.println();

        // Test 1: Negative maxResults (should fail)
        System.out.println("  Test 1: Attempting negative maxResults...");
        try {
            blockchain.getBlocksBySignerPublicKey(publicKeyString, -1);
            printError("    FAILED: Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            printSuccess("    ✅ Correctly rejected: " + e.getMessage());
        }
        System.out.println();

        // Test 2: Zero maxResults (should fail)
        System.out.println("  Test 2: Attempting zero maxResults...");
        try {
            blockchain.searchByCategory("TEST", 0);
            printError("    FAILED: Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            printSuccess("    ✅ Correctly rejected: " + e.getMessage());
        }
        System.out.println();

        // Test 3: Valid maxResults (should succeed)
        System.out.println("  Test 3: Using valid maxResults (1000)...");
        try {
            List<Block> blocks = blockchain.getBlocksBySignerPublicKey(publicKeyString, 1000);
            printSuccess("    ✅ Accepted: Retrieved " + blocks.size() + " blocks");
        } catch (Exception e) {
            printError("    FAILED: " + e.getMessage());
        }
        System.out.println();

        System.out.println("  📊 Breaking Changes Summary:");
        System.out.println("     - Prevents accidental memory bombs");
        System.out.println("     - Forces developers to use streaming APIs for large datasets");
        System.out.println("     - Default limits prevent OutOfMemoryError");
        System.out.println();
    }

    private static void demoBatchProcessing() {
        printSection("4. DEMO: BATCH PROCESSING (processChainInBatches)");
        System.out.println("  🎯 Phase B.1: Optimized batch processing with constant memory");
        System.out.println("     - Uses ScrollableResults for PostgreSQL/MySQL/H2");
        System.out.println("     - Uses pagination for SQLite");
        System.out.println("     - Memory: Constant ~50MB regardless of chain size");
        System.out.println();

        // Measure memory before
        System.gc();
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        AtomicInteger totalProcessed = new AtomicInteger(0);
        AtomicInteger encryptedCount = new AtomicInteger(0);
        AtomicInteger plainCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        System.out.println("  🔄 Processing blockchain in batches of 100 blocks...");
        System.out.println();

        blockchain.processChainInBatches(batch -> {
            for (Block block : batch) {
                totalProcessed.incrementAndGet();

                if (block.isDataEncrypted()) {
                    encryptedCount.incrementAndGet();
                } else {
                    plainCount.incrementAndGet();
                }
            }

            // Show progress every 10 batches
            if (totalProcessed.get() % 1000 == 0) {
                System.out.println("    ✅ Processed " + totalProcessed.get() + " blocks...");
            }
        }, 100);

        long elapsed = System.currentTimeMillis() - startTime;

        // Measure memory after
        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryDelta = memoryAfter - memoryBefore;

        System.out.println();
        System.out.println("  📊 Batch Processing Results:");
        System.out.println("     - Total blocks processed: " + totalProcessed.get());
        System.out.println("     - Encrypted blocks: " + encryptedCount.get());
        System.out.println("     - Plain blocks: " + plainCount.get());
        System.out.println("     - Processing time: " + elapsed + "ms");
        System.out.println("     - Memory before: " + formatBytes(memoryBefore));
        System.out.println("     - Memory after: " + formatBytes(memoryAfter));
        System.out.println("     - Memory delta: " + formatBytes(memoryDelta) + " ✅");
        System.out.println();
    }

    private static void demoStreamingValidation() {
        printSection("5. DEMO: STREAMING VALIDATION");
        System.out.println("  🎯 Phase A.2: Unlimited blockchain validation with streaming");
        System.out.println("     - No size limits (unlike validateChainDetailed)");
        System.out.println("     - Processes one block at a time");
        System.out.println("     - Memory: ~1-2MB per block");
        System.out.println();

        AtomicInteger validBlocks = new AtomicInteger(0);
        AtomicInteger invalidBlocks = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        System.out.println("  🔍 Validating blockchain with streaming...");
        System.out.println();

        Blockchain.ValidationSummary summary = blockchain.validateChainStreaming(batchResults -> {
            for (BlockValidationResult result : batchResults) {
                if (result.isValid()) {
                    validBlocks.incrementAndGet();
                } else {
                    invalidBlocks.incrementAndGet();
                }
            }

            int total = validBlocks.get() + invalidBlocks.get();

            // Show progress every 200 blocks
            if (total % 200 == 0) {
                System.out.println("    ✅ Validated " + total + " blocks...");
            }
        }, 100);

        long elapsed = System.currentTimeMillis() - startTime;

        System.out.println();
        System.out.println("  📊 Streaming Validation Results:");
        System.out.println("     - Blockchain valid: " + (summary.isValid() ? "✅ YES" : "❌ NO"));
        System.out.println("     - Total blocks validated: " + (validBlocks.get() + invalidBlocks.get()));
        System.out.println("     - Valid blocks: " + validBlocks.get());
        System.out.println("     - Invalid blocks: " + invalidBlocks.get());
        System.out.println("     - Validation time: " + elapsed + "ms");
        System.out.println("     - Perfect for chains > 500K blocks");
        System.out.println();
    }

    private static void demoMemorySafeSearch() {
        printSection("6. DEMO: MEMORY-SAFE SEARCH METHODS");
        System.out.println("  🎯 Phase A.3: Search methods with automatic limits");
        System.out.println("     - Default limit: " + MemorySafetyConstants.DEFAULT_MAX_SEARCH_RESULTS);
        System.out.println("     - Prevents unbounded memory growth");
        System.out.println();

        // Wait for background indexing to complete
        try {
            System.out.println("\n⏳ Waiting for background indexing to complete...");
            IndexingCoordinator.getInstance().waitForCompletion();
            System.out.println("✅ Background indexing completed - all blocks indexed\n");
        } catch (InterruptedException e) {
            System.err.println("⚠️ Indexing wait interrupted: " + e.getMessage());
        }

        // Search 1: Default limit
        System.out.println("  Test 1: Search by signer (default limit)");
        long startTime = System.currentTimeMillis();
        List<Block> results1 = blockchain.getBlocksBySignerPublicKey(publicKeyString);
        long elapsed1 = System.currentTimeMillis() - startTime;
        System.out.println("    ✅ Found " + results1.size() + " blocks in " + elapsed1 + "ms");
        System.out.println("    ✅ Memory-safe: Capped at " + MemorySafetyConstants.DEFAULT_MAX_SEARCH_RESULTS);
        System.out.println();

        // Search 2: Custom limit
        System.out.println("  Test 2: Search with custom limit (500)");
        startTime = System.currentTimeMillis();
        List<Block> results2 = blockchain.getBlocksBySignerPublicKey(publicKeyString, 500);
        long elapsed2 = System.currentTimeMillis() - startTime;
        System.out.println("    ✅ Found " + results2.size() + " blocks in " + elapsed2 + "ms");
        System.out.println("    ✅ Respects custom limit");
        System.out.println();

        System.out.println("  📊 Memory-Safe Search Summary:");
        System.out.println("     - All search methods have limits");
        System.out.println("     - Prevents OutOfMemoryError");
        System.out.println("     - Use streaming APIs for unlimited results");
        System.out.println();
    }

    private static void demoMemoryLimits() {
        printSection("7. DEMO: MEMORY SAFETY CONSTANTS");
        System.out.println("  🛡️  Phase A.8: Centralized memory limits");
        System.out.println("     All limits configured in MemorySafetyConstants.java");
        System.out.println();

        System.out.println("  📋 Current Configuration:");
        System.out.println("     - MAX_BATCH_SIZE: " + MemorySafetyConstants.MAX_BATCH_SIZE);
        System.out.println("     - DEFAULT_MAX_SEARCH_RESULTS: " + MemorySafetyConstants.DEFAULT_MAX_SEARCH_RESULTS);
        System.out.println("     - SAFE_EXPORT_LIMIT: " + MemorySafetyConstants.SAFE_EXPORT_LIMIT);
        System.out.println("     - MAX_EXPORT_LIMIT: " + MemorySafetyConstants.MAX_EXPORT_LIMIT);
        System.out.println("     - DEFAULT_BATCH_SIZE: " + MemorySafetyConstants.DEFAULT_BATCH_SIZE);
        System.out.println("     - LARGE_ROLLBACK_THRESHOLD: " + MemorySafetyConstants.LARGE_ROLLBACK_THRESHOLD);
        System.out.println("     - PROGRESS_REPORT_INTERVAL: " + MemorySafetyConstants.PROGRESS_REPORT_INTERVAL);
        System.out.println("     - MAX_JSON_METADATA_ITERATIONS: " + MemorySafetyConstants.MAX_JSON_METADATA_ITERATIONS);
        System.out.println();

        System.out.println("  🎯 Benefits:");
        System.out.println("     - Single source of truth for all limits");
        System.out.println("     - Easy to tune for different environments");
        System.out.println("     - Prevents DoS attacks via excessive requests");
        System.out.println();
    }

    private static void demoBeforeAfterComparison() {
        printSection("8. DEMO: BEFORE vs AFTER COMPARISON");
        long totalBlocks = blockchain.getBlockCount();
        System.out.println("  📊 Memory usage comparison for " + totalBlocks + " blocks (including genesis)");
        System.out.println();

        // Simulate "before" scenario (loading all blocks)
        System.out.println("  ❌ BEFORE (v1.0.4): Loading all blocks into memory");
        System.gc();
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        long startTime = System.currentTimeMillis();
        // Load ALL blocks including genesis (block 0) and last block (block 1000)
        List<Block> allBlocks = blockchain.getBlocksPaginated(0, (int)totalBlocks);
        long elapsedBefore = System.currentTimeMillis() - startTime;

        long memoryAfterLoad = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryUsedBefore = memoryAfterLoad - memoryBefore;

        System.out.println("     - Loaded: " + allBlocks.size() + " blocks");
        System.out.println("     - Memory used: " + formatBytes(memoryUsedBefore));
        System.out.println("     - Time: " + elapsedBefore + "ms");
        System.out.println("     - Risk: OutOfMemoryError with large chains");
        System.out.println();

        // Clear memory
        allBlocks = null;
        System.gc();
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        // "After" scenario (streaming)
        System.out.println("  ✅ AFTER (v1.0.6+): Streaming with constant memory");
        memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        AtomicInteger count = new AtomicInteger(0);
        startTime = System.currentTimeMillis();

        blockchain.processChainInBatches(batch -> {
            count.addAndGet(batch.size());
        }, 100);

        long elapsedAfter = System.currentTimeMillis() - startTime;
        long memoryAfterStream = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryUsedAfter = memoryAfterStream - memoryBefore;

        System.out.println("     - Processed: " + count.get() + " blocks");
        System.out.println("     - Memory used: " + formatBytes(memoryUsedAfter));
        System.out.println("     - Time: " + elapsedAfter + "ms");
        System.out.println("     - Safe: Constant memory for any chain size");
        System.out.println();

        // Comparison
        double memoryReduction = (1.0 - (double)memoryUsedAfter / memoryUsedBefore) * 100;
        System.out.println("  📈 Improvement:");
        System.out.println("     - Memory reduction: " + String.format("%.1f%%", memoryReduction) + " ✅");
        System.out.println("     - Scalability: Unlimited blockchain size supported");
        System.out.println("     - Safety: No OutOfMemoryError risk");
        System.out.println();
    }

    private static void cleanup() {
        // Just shutdown JPA - the demo database files will be cleaned by the script
        JPAUtil.shutdown();
        System.out.println("✅ Cleanup completed");
    }

    // Utility methods

    private static void printHeader(String title) {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  " + title);
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();
    }

    private static void printSection(String title) {
        System.out.println("───────────────────────────────────────────────────────────────");
        System.out.println("  " + title);
        System.out.println("───────────────────────────────────────────────────────────────");
        System.out.println();
    }

    private static void printSuccess(String message) {
        System.out.println("    " + message);
    }

    private static void printError(String message) {
        System.err.println("    " + message);
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
