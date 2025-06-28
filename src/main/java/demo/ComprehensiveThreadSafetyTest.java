package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.io.File;

/**
 * Comprehensive thread safety test for blockchain operations with off-chain storage
 * Tests various scenarios including concurrent block addition, rollbacks, exports, and validations
 */
public class ComprehensiveThreadSafetyTest {
    
    private static final Logger logger = LogManager.getLogger(ComprehensiveThreadSafetyTest.class);
    
    private static final int NUM_THREADS = 20;
    private static final int OPERATIONS_PER_THREAD = 10;
    private static final long TEST_TIMEOUT_SECONDS = 120;
    
    private static Blockchain blockchain;
    private static PrivateKey[] privateKeys;
    private static PublicKey[] publicKeys;
    private static String[] publicKeyStrings;
    
    // Thread safety monitoring
    private static final AtomicInteger successfulOperations = new AtomicInteger(0);
    private static final AtomicInteger failedOperations = new AtomicInteger(0);
    private static final AtomicLong totalOperationTime = new AtomicLong(0);
    private static final Set<Long> processedBlockNumbers = Collections.synchronizedSet(new HashSet<>());
    private static final List<String> errors = Collections.synchronizedList(new ArrayList<>());
    
    public static void main(String[] args) {
        System.out.println("=== 🔒 COMPREHENSIVE THREAD SAFETY TEST ===");
        System.out.println("Configuration: " + NUM_THREADS + " threads, " + OPERATIONS_PER_THREAD + " operations per thread, " + TEST_TIMEOUT_SECONDS + " seconds timeout");
        System.out.println();
        
        logger.info("=== 🔒 COMPREHENSIVE THREAD SAFETY TEST ===");
        logger.info("Configuration: {} threads, {} operations per thread, {} seconds timeout", 
                   NUM_THREADS, OPERATIONS_PER_THREAD, TEST_TIMEOUT_SECONDS);
        
        try {
            // Initialize test environment
            initializeTest();
            
            // Run concurrent tests
            runConcurrentBlockAddition();
            runConcurrentMixedOperations();
            runConcurrentValidationStress();
            runConcurrentOffChainOperations();
            runConcurrentRollbackTest();
            runConcurrentExportImportTest();
            runEdgeCaseTests();
            
            // Analyze results
            analyzeResults();
            
            System.out.println("✅ ALL THREAD SAFETY TESTS COMPLETED!");
            logger.info("✅ ALL THREAD SAFETY TESTS COMPLETED!");
            
        } catch (Exception e) {
            System.err.println("❌ Thread safety test failed: " + e.getMessage());
            logger.error("❌ Thread safety test failed: {}", e.getMessage(), e);
        } finally {
            cleanup();
        }
    }
    
    private static void initializeTest() throws Exception {
        System.out.println("🚀 Initializing thread safety test environment...");
        
        // Clean up any existing files
        cleanupFiles();
        
        // Initialize blockchain
        blockchain = new Blockchain();
        
        // Generate multiple key pairs for testing
        privateKeys = new PrivateKey[NUM_THREADS];
        publicKeys = new PublicKey[NUM_THREADS];
        publicKeyStrings = new String[NUM_THREADS];
        
        for (int i = 0; i < NUM_THREADS; i++) {
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            privateKeys[i] = keyPair.getPrivate();
            publicKeys[i] = keyPair.getPublic();
            publicKeyStrings[i] = CryptoUtil.publicKeyToString(publicKeys[i]);
            
            // Authorize all keys
            blockchain.addAuthorizedKey(publicKeyStrings[i], "TestUser" + i);
        }
        
        System.out.println("✅ Initialized " + NUM_THREADS + " key pairs and authorized them");
        System.out.println();
    }
    
    private static void runConcurrentBlockAddition() throws Exception {
        System.out.println("📝 Test 1: Concurrent Block Addition");
        logger.info("📝 Test 1: Concurrent Block Addition");
        
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(NUM_THREADS);
        
        AtomicInteger blockCount = new AtomicInteger(0);
        
        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        long startTime = System.nanoTime();
                        
                        // Create data of varying sizes (some trigger off-chain storage)
                        String data = generateTestData(threadId, j);
                        
                        Block block = blockchain.addBlockAndReturn(
                            data, 
                            privateKeys[threadId], 
                            publicKeys[threadId]
                        );
                        
                        if (block != null) {
                            processedBlockNumbers.add(block.getBlockNumber());
                            blockCount.incrementAndGet();
                            successfulOperations.incrementAndGet();
                        } else {
                            failedOperations.incrementAndGet();
                            errors.add("Thread " + threadId + " failed to add block " + j);
                        }
                        
                        long endTime = System.nanoTime();
                        totalOperationTime.addAndGet(endTime - startTime);
                    }
                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                    errors.add("Thread " + threadId + " exception: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        startLatch.countDown(); // Start all threads simultaneously
        boolean completed = endLatch.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        executor.shutdown();
        
        if (!completed) {
            errors.add("Concurrent block addition test timed out");
        }
        
        System.out.println("   📊 Blocks added: " + blockCount.get());
        logger.info("   📊 Blocks added: {}", blockCount.get());
        System.out.println("   🔍 Unique block numbers: " + processedBlockNumbers.size());
        logger.info("   🔍 Unique block numbers: {}", processedBlockNumbers.size());
        System.out.println("   ✅ No duplicate block numbers: " + (processedBlockNumbers.size() == blockCount.get()));
        logger.info("   ✅ No duplicate block numbers: {}", (processedBlockNumbers.size() == blockCount.get()));
        System.out.println();
    }
    
    private static void runConcurrentMixedOperations() throws Exception {
        System.out.println("📝 Test 2: Concurrent Mixed Operations");
        logger.info("📝 Test 2: Concurrent Mixed Operations");
        
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(NUM_THREADS);
        
        Random random = new Random();
        
        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        long startTime = System.nanoTime();
                        
                        int operation = random.nextInt(4);
                        boolean success = false;
                        
                        switch (operation) {
                            case 0: // Add block
                                String data = generateTestData(threadId, j);
                                Block block = blockchain.addBlockAndReturn(data, privateKeys[threadId], publicKeys[threadId]);
                                success = (block != null);
                                break;
                                
                            case 1: // Validate chain
                                var result = blockchain.validateChainDetailed();
                                success = result.isValid();
                                break;
                                
                            case 2: // Get block count
                                long blockCount = blockchain.getBlockCount();
                                success = (blockCount >= 0);
                                break;
                                
                            case 3: // Get block by number
                                long blockNum = Math.max(0, blockchain.getBlockCount() - 1);
                                if (blockNum > 0) {
                                    Block retrievedBlock = blockchain.getBlock(blockNum);
                                    success = (retrievedBlock != null);
                                } else {
                                    success = true; // No blocks to retrieve yet
                                }
                                break;
                        }
                        
                        if (success) {
                            successfulOperations.incrementAndGet();
                        } else {
                            failedOperations.incrementAndGet();
                            errors.add("Thread " + threadId + " operation " + operation + " failed");
                        }
                        
                        long endTime = System.nanoTime();
                        totalOperationTime.addAndGet(endTime - startTime);
                    }
                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                    errors.add("Thread " + threadId + " exception: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        boolean completed = endLatch.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        executor.shutdown();
        
        if (!completed) {
            errors.add("Mixed operations test timed out");
        }
        
        System.out.println("   ✅ Mixed operations completed");
        logger.info("   ✅ Mixed operations completed");
        System.out.println();
    }
    
    private static void runConcurrentValidationStress() throws Exception {
        System.out.println("📝 Test 3: Concurrent Validation Stress");
        logger.info("📝 Test 3: Concurrent Validation Stress");
        
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(NUM_THREADS);
        
        AtomicInteger validationCount = new AtomicInteger(0);
        
        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < OPERATIONS_PER_THREAD * 2; j++) {
                        long startTime = System.nanoTime();
                        
                        var result = blockchain.validateChainDetailed();
                        if (result.isValid()) {
                            validationCount.incrementAndGet();
                            successfulOperations.incrementAndGet();
                        } else {
                            failedOperations.incrementAndGet();
                            errors.add("Thread " + threadId + " validation failed: " + result.toString());
                        }
                        
                        long endTime = System.nanoTime();
                        totalOperationTime.addAndGet(endTime - startTime);
                    }
                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                    errors.add("Thread " + threadId + " validation exception: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        boolean completed = endLatch.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        executor.shutdown();
        
        if (!completed) {
            errors.add("Validation stress test timed out");
        }
        
        System.out.println("   📊 Successful validations: " + validationCount.get());
        logger.info("   📊 Successful validations: {}", validationCount.get());
        System.out.println("   ✅ Validation stress test completed");
        logger.info("   ✅ Validation stress test completed");
        System.out.println();
    }
    
    private static void runConcurrentOffChainOperations() throws Exception {
        System.out.println("📝 Test 4: Concurrent Off-Chain Operations");
        logger.info("📝 Test 4: Concurrent Off-Chain Operations");
        
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(NUM_THREADS);
        
        AtomicInteger offChainBlocks = new AtomicInteger(0);
        
        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        long startTime = System.nanoTime();
                        
                        // Generate large data to force off-chain storage
                        String largeData = generateLargeTestData(threadId, j);
                        
                        Block block = blockchain.addBlockAndReturn(largeData, privateKeys[threadId], publicKeys[threadId]);
                        
                        if (block != null && block.hasOffChainData()) {
                            offChainBlocks.incrementAndGet();
                            
                            // Verify off-chain file exists
                            String filePath = block.getOffChainData().getFilePath();
                            File file = new File(filePath);
                            if (!file.exists()) {
                                errors.add("Thread " + threadId + " off-chain file missing: " + filePath);
                                failedOperations.incrementAndGet();
                            } else {
                                successfulOperations.incrementAndGet();
                            }
                        } else {
                            failedOperations.incrementAndGet();
                            errors.add("Thread " + threadId + " failed to create off-chain block");
                        }
                        
                        long endTime = System.nanoTime();
                        totalOperationTime.addAndGet(endTime - startTime);
                    }
                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                    errors.add("Thread " + threadId + " off-chain exception: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        boolean completed = endLatch.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        executor.shutdown();
        
        if (!completed) {
            errors.add("Off-chain operations test timed out");
        }
        
        System.out.println("   📊 Off-chain blocks created: " + offChainBlocks.get());
        logger.info("   📊 Off-chain blocks created: {}", offChainBlocks.get());
        System.out.println("   ✅ Off-chain operations completed");
        logger.info("   ✅ Off-chain operations completed");
        System.out.println();
    }
    
    private static void runConcurrentRollbackTest() throws Exception {
        System.out.println("📝 Test 5: Concurrent Rollback Safety");
        logger.info("📝 Test 5: Concurrent Rollback Safety");
        
        // Add some blocks first
        for (int i = 0; i < 10; i++) {
            blockchain.addBlockAndReturn("Block " + i, privateKeys[0], publicKeys[0]);
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(NUM_THREADS);
        
        AtomicInteger rollbackAttempts = new AtomicInteger(0);
        AtomicInteger rollbackSuccesses = new AtomicInteger(0);
        
        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    // Each thread attempts one rollback operation
                    long startTime = System.nanoTime();
                    rollbackAttempts.incrementAndGet();
                    
                    try {
                        boolean success = blockchain.rollbackBlocks(1L);
                        if (success) {
                            rollbackSuccesses.incrementAndGet();
                            successfulOperations.incrementAndGet();
                        } else {
                            // Rollback can legitimately fail if chain is too short
                            successfulOperations.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failedOperations.incrementAndGet();
                        errors.add("Thread " + threadId + " rollback exception: " + e.getMessage());
                    }
                    
                    long endTime = System.nanoTime();
                    totalOperationTime.addAndGet(endTime - startTime);
                    
                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                    errors.add("Thread " + threadId + " rollback setup exception: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        boolean completed = endLatch.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        executor.shutdown();
        
        if (!completed) {
            errors.add("Rollback test timed out");
        }
        
        System.out.println("   📊 Rollback attempts: " + rollbackAttempts.get());
        logger.info("   📊 Rollback attempts: {}", rollbackAttempts.get());
        System.out.println("   📊 Rollback successes: " + rollbackSuccesses.get());
        logger.info("   📊 Rollback successes: {}", rollbackSuccesses.get());
        System.out.println("   ✅ Rollback safety test completed");
        logger.info("   ✅ Rollback safety test completed");
        System.out.println();
    }
    
    private static void runConcurrentExportImportTest() throws Exception {
        System.out.println("📝 Test 6: Concurrent Export/Import Safety");
        logger.info("📝 Test 6: Concurrent Export/Import Safety");
        
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(NUM_THREADS, 5)); // Limit concurrent exports
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(Math.min(NUM_THREADS, 5));
        
        AtomicInteger exportAttempts = new AtomicInteger(0);
        AtomicInteger exportSuccesses = new AtomicInteger(0);
        
        for (int i = 0; i < Math.min(NUM_THREADS, 5); i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    long startTime = System.nanoTime();
                    exportAttempts.incrementAndGet();
                    
                    String exportFile = "export_test_" + threadId + "_" + System.currentTimeMillis() + ".json";
                    
                    try {
                        boolean success = blockchain.exportChain(exportFile);
                        if (success) {
                            exportSuccesses.incrementAndGet();
                            successfulOperations.incrementAndGet();
                            
                            // Clean up export file
                            new File(exportFile).delete();
                        } else {
                            failedOperations.incrementAndGet();
                            errors.add("Thread " + threadId + " export failed");
                        }
                    } catch (Exception e) {
                        failedOperations.incrementAndGet();
                        errors.add("Thread " + threadId + " export exception: " + e.getMessage());
                    }
                    
                    long endTime = System.nanoTime();
                    totalOperationTime.addAndGet(endTime - startTime);
                    
                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                    errors.add("Thread " + threadId + " export setup exception: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        boolean completed = endLatch.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        executor.shutdown();
        
        if (!completed) {
            errors.add("Export/Import test timed out");
        }
        
        System.out.println("   📊 Export attempts: " + exportAttempts.get());
        logger.info("   📊 Export attempts: {}", exportAttempts.get());
        System.out.println("   📊 Export successes: " + exportSuccesses.get());
        logger.info("   📊 Export successes: {}", exportSuccesses.get());
        System.out.println("   ✅ Export/Import safety test completed");
        logger.info("   ✅ Export/Import safety test completed");
        System.out.println();
    }
    
    private static void runEdgeCaseTests() throws Exception {
        System.out.println("📝 Test 7: Edge Case Thread Safety");
        logger.info("📝 Test 7: Edge Case Thread Safety");
        
        // Test rapid key authorization/revocation
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(NUM_THREADS);
        
        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    // Generate new key pair
                    KeyPair keyPair = CryptoUtil.generateKeyPair();
                    String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
                    
                    // Rapid authorize/revoke cycles
                    for (int j = 0; j < 5; j++) {
                        try {
                            blockchain.addAuthorizedKey(publicKeyString + "_" + j, "EdgeTestUser" + threadId + "_" + j);
                            Thread.sleep(1); // Small delay to create potential race conditions
                            blockchain.revokeAuthorizedKey(publicKeyString + "_" + j);
                            successfulOperations.incrementAndGet();
                        } catch (Exception e) {
                            failedOperations.incrementAndGet();
                            errors.add("Thread " + threadId + " edge case exception: " + e.getMessage());
                        }
                    }
                    
                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                    errors.add("Thread " + threadId + " edge case setup exception: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        boolean completed = endLatch.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        executor.shutdown();
        
        if (!completed) {
            errors.add("Edge case test timed out");
        }
        
        System.out.println("   ✅ Edge case tests completed");
        logger.info("   ✅ Edge case tests completed");
        System.out.println();
    }
    
    private static void analyzeResults() {
        System.out.println("=== 📊 THREAD SAFETY TEST RESULTS ===");
        System.out.println();
        
        logger.info("=== 📊 THREAD SAFETY TEST RESULTS ===");
        
        int totalOperations = successfulOperations.get() + failedOperations.get();
        double successRate = totalOperations > 0 ? (successfulOperations.get() * 100.0 / totalOperations) : 0;
        double avgOperationTime = totalOperations > 0 ? (totalOperationTime.get() / 1_000_000.0 / totalOperations) : 0;
        
        System.out.println("📈 Performance Metrics:");
        logger.info("📈 Performance Metrics:");
        System.out.println("   Total operations: " + totalOperations);
        logger.info("   Total operations: {}", totalOperations);
        System.out.println("   Successful operations: " + successfulOperations.get());
        logger.info("   Successful operations: {}", successfulOperations.get());
        System.out.println("   Failed operations: " + failedOperations.get());
        logger.info("   Failed operations: {}", failedOperations.get());
        System.out.println("   Success rate: " + String.format("%.2f%%", successRate));
        logger.info("   Success rate: {}%", String.format("%.2f", successRate));
        System.out.println("   Average operation time: " + String.format("%.2f ms", avgOperationTime));
        logger.info("   Average operation time: {} ms", String.format("%.2f", avgOperationTime));
        System.out.println();
        
        System.out.println("🔍 Data Integrity:");
        System.out.println("   Unique block numbers: " + processedBlockNumbers.size());
        System.out.println("   Chain length: " + blockchain.getBlockCount());
        System.out.println("   Chain valid: " + blockchain.validateChainDetailed().isValid());
        System.out.println();
        
        if (!errors.isEmpty()) {
            System.out.println("❌ Errors detected:");
            for (int i = 0; i < Math.min(errors.size(), 10); i++) {
                System.out.println("   - " + errors.get(i));
            }
            if (errors.size() > 10) {
                System.out.println("   ... and " + (errors.size() - 10) + " more errors");
            }
            System.out.println();
        }
        
        // Check for race conditions
        boolean raceConditionsDetected = false;
        
        // Check for duplicate block numbers
        if (processedBlockNumbers.size() < successfulOperations.get()) {
            System.out.println("⚠️ Potential race condition: Duplicate block numbers detected");
            raceConditionsDetected = true;
        }
        
        // Check chain integrity with detailed validation and off-chain analysis
        System.out.println();
        System.out.println("🔍 Final detailed validation with off-chain data analysis:");
        var chainValidation = blockchain.validateChainDetailed();
        if (!chainValidation.isValid()) {
            System.out.println("⚠️ Chain integrity compromised: " + chainValidation.toString());
            raceConditionsDetected = true;
        }
        
        if (!raceConditionsDetected && errors.isEmpty()) {
            System.out.println("✅ NO RACE CONDITIONS DETECTED");
            System.out.println("✅ THREAD SAFETY VERIFIED");
        } else {
            System.out.println("❌ POTENTIAL THREAD SAFETY ISSUES DETECTED");
        }
    }
    
    private static String generateTestData(int threadId, int iteration) {
        Random random = new Random(threadId * 1000 + iteration);
        int dataSize = random.nextInt(1000) + 100; // 100-1100 bytes
        
        StringBuilder sb = new StringBuilder();
        sb.append("Thread-").append(threadId).append("-Iteration-").append(iteration).append("-");
        sb.append("Timestamp-").append(System.currentTimeMillis()).append("-");
        
        while (sb.length() < dataSize) {
            sb.append("ThreadSafetyTestData-").append(random.nextInt(10000)).append("-");
        }
        
        return sb.substring(0, dataSize);
    }
    
    private static String generateLargeTestData(int threadId, int iteration) {
        // Generate data larger than off-chain threshold (512KB+)
        StringBuilder sb = new StringBuilder();
        String baseData = "Thread-" + threadId + "-Iteration-" + iteration + "-LargeOffChainData-";
        
        // Generate approximately 600KB of data
        for (int i = 0; i < 15000; i++) {
            sb.append(baseData).append(i).append("-");
        }
        
        return sb.toString();
    }
    
    private static void cleanup() {
        System.out.println("🧹 Cleaning up test environment...");
        cleanupFiles();
        System.out.println("✅ Cleanup completed");
    }
    
    private static void cleanupFiles() {
        try {
            // Clean up database files
            deleteFileIfExists("blockchain.db");
            deleteFileIfExists("blockchain.db-shm");
            deleteFileIfExists("blockchain.db-wal");
            
            // Clean up off-chain directory
            File offChainDir = new File("off-chain-data");
            if (offChainDir.exists()) {
                deleteDirectory(offChainDir);
            }
            
            // Clean up backup directory
            File backupDir = new File("off-chain-backup");
            if (backupDir.exists()) {
                deleteDirectory(backupDir);
            }
            
            // Clean up export files
            File currentDir = new File(".");
            File[] files = currentDir.listFiles((dir, name) -> 
                name.startsWith("export_test_") && name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            
        } catch (Exception e) {
            System.err.println("Warning: Could not clean up all files: " + e.getMessage());
        }
    }
    
    private static void deleteFileIfExists(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }
    }
    
    private static void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}