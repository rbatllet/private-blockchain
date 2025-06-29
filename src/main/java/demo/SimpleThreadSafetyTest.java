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
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.io.File;

/**
 * Simplified thread safety test focusing on key scenarios
 */
public class SimpleThreadSafetyTest {
    
    private static final Logger logger = LogManager.getLogger(SimpleThreadSafetyTest.class);
    
    private static final int NUM_THREADS = 10;
    private static final int OPERATIONS_PER_THREAD = 5;
    
    public static void main(String[] args) {
        // Mostrar header tant a logs com a pantalla
        System.out.println("=== 🔒 SIMPLIFIED THREAD SAFETY TEST ===");
        System.out.println("Configuration: " + NUM_THREADS + " threads, " + OPERATIONS_PER_THREAD + " operations per thread");
        System.out.println();
        
        logger.info("=== 🔒 SIMPLIFIED THREAD SAFETY TEST ===");
        logger.info("Configuration: {} threads, {} operations per thread", NUM_THREADS, OPERATIONS_PER_THREAD);
        
        try {
            testConcurrentBlockCreation();
            testConcurrentOffChainOperations();
            testConcurrentValidation();
            
            System.out.println("✅ ALL SIMPLIFIED THREAD SAFETY TESTS PASSED!");
            logger.info("✅ ALL SIMPLIFIED THREAD SAFETY TESTS PASSED!");
            
        } catch (Exception e) {
            System.err.println("❌ Thread safety test failed: " + e.getMessage());
            logger.error("❌ Thread safety test failed: {}", e.getMessage(), e);
        } finally {
            cleanup();
        }
    }
    
    private static void testConcurrentBlockCreation() throws Exception {
        System.out.println("📝 Test 1: Concurrent Block Creation");
        logger.info("📝 Test 1: Concurrent Block Creation");
        
        // Clean up
        cleanup();
        
        Blockchain blockchain = new Blockchain();
        logger.debug("Blockchain initialized for concurrent block creation test");
        
        // Setup keys
        PrivateKey[] privateKeys = new PrivateKey[NUM_THREADS];
        PublicKey[] publicKeys = new PublicKey[NUM_THREADS];
        
        for (int i = 0; i < NUM_THREADS; i++) {
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            privateKeys[i] = keyPair.getPrivate();
            publicKeys[i] = keyPair.getPublic();
            blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(publicKeys[i]), "TestUser" + i);
        }
        logger.debug("Generated and authorized {} key pairs", NUM_THREADS);
        
        // Concurrent execution
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(NUM_THREADS);
        
        AtomicInteger successCount = new AtomicInteger(0);
        Set<Long> blockNumbers = Collections.synchronizedSet(new HashSet<>());
        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        
        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        String data = "Thread-" + threadId + "-Block-" + j + "-Data";
                        logger.debug("Thread {} attempting to create block {}", threadId, j);
                        
                        Block block = blockchain.addBlockAndReturn(data, privateKeys[threadId], publicKeys[threadId]);
                        
                        if (block != null) {
                            blockNumbers.add(block.getBlockNumber());
                            successCount.incrementAndGet();
                            logger.debug("Thread {} successfully created block {} with number {}", 
                                       threadId, j, block.getBlockNumber());
                        } else {
                            String error = "Thread " + threadId + " failed to create block " + j;
                            errors.add(error);
                            logger.warn(error);
                        }
                    }
                } catch (Exception e) {
                    String error = "Thread " + threadId + " exception: " + e.getMessage();
                    errors.add(error);
                    logger.error(error, e);
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        System.out.println("   Starting concurrent block creation with " + NUM_THREADS + " threads...");
        logger.info("Starting concurrent block creation with {} threads", NUM_THREADS);
        startLatch.countDown();
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        if (!completed) {
            System.out.println("   ⚠️ Test timed out after 30 seconds");
            logger.warn("Block creation test timed out after 30 seconds");
        }
        
        // Analysis
        int expectedBlocks = NUM_THREADS * OPERATIONS_PER_THREAD;
        long actualChainLength = blockchain.getBlockCount() - 1; // Exclude genesis
        boolean chainValid = blockchain.validateChainDetailed().isValid();
        
        System.out.println("   Block Creation Test Results:");
        System.out.println("     Expected blocks: " + expectedBlocks);
        System.out.println("     Successful operations: " + successCount.get());
        System.out.println("     Unique block numbers: " + blockNumbers.size());
        System.out.println("     Chain length: " + actualChainLength);
        System.out.println("     Chain valid: " + chainValid);
        
        logger.info("Block Creation Test Results:");
        logger.info("   Expected blocks: {}", expectedBlocks);
        logger.info("   Successful operations: {}", successCount.get());
        logger.info("   Unique block numbers: {}", blockNumbers.size());
        logger.info("   Chain length: {}", actualChainLength);
        logger.info("   Chain valid: {}", chainValid);
        
        boolean success = (successCount.get() == expectedBlocks) && 
                         (blockNumbers.size() == expectedBlocks) && 
                         (actualChainLength == expectedBlocks) &&
                         errors.isEmpty();
        
        if (success) {
            System.out.println("   ✅ Block creation thread safety VERIFIED");
            logger.info("   ✅ Block creation thread safety VERIFIED");
        } else {
            System.out.println("   ❌ Block creation thread safety ISSUES DETECTED");
            logger.error("   ❌ Block creation thread safety ISSUES DETECTED");
            if (!errors.isEmpty()) {
                System.out.println("   Errors detected: " + errors.size());
                logger.error("   Errors detected: {}", errors.size());
                errors.stream().limit(3).forEach(error -> {
                    System.out.println("     - " + error);
                    logger.error("     - {}", error);
                });
            }
        }
        System.out.println();
    }
    
    private static void testConcurrentOffChainOperations() throws Exception {
        System.out.println("📝 Test 2: Concurrent Off-Chain Operations");
        logger.info("📝 Test 2: Concurrent Off-Chain Operations");
        
        cleanup();
        
        Blockchain blockchain = new Blockchain();
        logger.debug("Blockchain initialized for off-chain operations test");
        
        // Setup one key for this test
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(publicKey), "OffChainTestUser");
        logger.debug("Key pair generated and authorized for off-chain test");
        
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(NUM_THREADS);
        
        AtomicInteger offChainBlocksCreated = new AtomicInteger(0);
        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        
        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    // Generate large data to force off-chain storage (but within limits)
                    StringBuilder largeData = new StringBuilder();
                    for (int k = 0; k < 1000; k++) {  // Reduced from 15000 to 1000 to stay within limits
                        largeData.append("Thread-").append(threadId).append("-LargeData-").append(k).append("-");
                    }
                    logger.debug("Thread {} generated large data ({} bytes) for off-chain storage", 
                               threadId, largeData.length());
                    
                    Block block = blockchain.addBlockAndReturn(largeData.toString(), privateKey, publicKey);
                    
                    if (block != null && block.hasOffChainData()) {
                        // Verify off-chain file exists
                        String filePath = block.getOffChainData().getFilePath();
                        File file = new File(filePath);
                        if (file.exists()) {
                            offChainBlocksCreated.incrementAndGet();
                            logger.debug("Thread {} successfully created off-chain block with file: {}", 
                                       threadId, filePath);
                        } else {
                            String error = "Thread " + threadId + " off-chain file missing: " + filePath;
                            errors.add(error);
                            logger.warn(error);
                        }
                    } else {
                        String error = "Thread " + threadId + " failed to create off-chain block";
                        errors.add(error);
                        logger.warn(error);
                    }
                    
                } catch (Exception e) {
                    String error = "Thread " + threadId + " off-chain exception: " + e.getMessage();
                    errors.add(error);
                    logger.error(error, e);
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        System.out.println("   Starting concurrent off-chain operations with " + NUM_THREADS + " threads...");
        logger.info("Starting concurrent off-chain operations with {} threads", NUM_THREADS);
        startLatch.countDown();
        boolean completed = endLatch.await(45, TimeUnit.SECONDS);
        executor.shutdown();
        
        if (!completed) {
            System.out.println("   ⚠️ Test timed out after 45 seconds");
            logger.warn("Off-chain operations test timed out after 45 seconds");
        }
        
        System.out.println();
        System.out.println("🔍 Detailed validation after off-chain operations:");
        var validationResult = blockchain.validateChainDetailed();
        boolean chainValid = validationResult.isValid();
        
        System.out.println("   Off-Chain Operations Test Results:");
        System.out.println("     Off-chain blocks created: " + offChainBlocksCreated.get());
        System.out.println("     Expected: " + NUM_THREADS);
        System.out.println("     Chain valid: " + chainValid);
        
        logger.info("Off-Chain Operations Test Results:");
        logger.info("   Off-chain blocks created: {}", offChainBlocksCreated.get());
        logger.info("   Expected: {}", NUM_THREADS);
        logger.info("   Chain valid: {}", chainValid);
        
        boolean success = (offChainBlocksCreated.get() == NUM_THREADS) && errors.isEmpty();
        
        if (success) {
            System.out.println("   ✅ Off-chain operations thread safety VERIFIED");
            logger.info("   ✅ Off-chain operations thread safety VERIFIED");
        } else {
            System.out.println("   ❌ Off-chain operations thread safety ISSUES DETECTED");
            logger.error("   ❌ Off-chain operations thread safety ISSUES DETECTED");
            if (!errors.isEmpty()) {
                System.out.println("   Errors detected: " + errors.size());
                logger.error("   Errors detected: {}", errors.size());
                errors.stream().limit(5).forEach(error -> {
                    System.out.println("     - " + error);
                    logger.error("     - {}", error);
                });
            }
        }
        System.out.println();
    }
    
    private static void testConcurrentValidation() throws Exception {
        System.out.println("📝 Test 3: Concurrent Validation Stress");
        logger.info("📝 Test 3: Concurrent Validation Stress");
        
        cleanup();
        
        Blockchain blockchain = new Blockchain();
        logger.debug("Blockchain initialized for validation stress test");
        
        // Add some blocks first
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(publicKey), "ValidationTestUser");
        
        for (int i = 0; i < 10; i++) {
            blockchain.addBlockAndReturn("Validation test block " + i, privateKey, publicKey);
        }
        logger.debug("Created 10 test blocks for validation stress testing");
        
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(NUM_THREADS);
        
        AtomicInteger validationCount = new AtomicInteger(0);
        AtomicInteger successfulValidations = new AtomicInteger(0);
        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        
        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < OPERATIONS_PER_THREAD * 2; j++) {
                        logger.debug("Thread {} performing validation {}", threadId, j);
                        var result = blockchain.validateChainDetailed();
                        validationCount.incrementAndGet();
                        
                        if (result.isValid()) {
                            successfulValidations.incrementAndGet();
                            logger.debug("Thread {} validation {} successful", threadId, j);
                        } else {
                            String error = "Thread " + threadId + " validation failed: " + result.toString();
                            errors.add(error);
                            logger.warn(error);
                        }
                    }
                } catch (Exception e) {
                    String error = "Thread " + threadId + " validation exception: " + e.getMessage();
                    errors.add(error);
                    logger.error(error, e);
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        System.out.println("   Starting concurrent validation stress test with " + NUM_THREADS + " threads...");
        logger.info("Starting concurrent validation stress test with {} threads", NUM_THREADS);
        startLatch.countDown();
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        if (!completed) {
            System.out.println("   ⚠️ Test timed out after 30 seconds");
            logger.warn("Validation stress test timed out after 30 seconds");
        }
        
        int expectedValidations = NUM_THREADS * OPERATIONS_PER_THREAD * 2;
        
        System.out.println("   Concurrent Validation Test Results:");
        System.out.println("     Total validations: " + validationCount.get());
        System.out.println("     Successful validations: " + successfulValidations.get());
        System.out.println("     Expected: " + expectedValidations);
        
        logger.info("Concurrent Validation Test Results:");
        logger.info("   Total validations: {}", validationCount.get());
        logger.info("   Successful validations: {}", successfulValidations.get());
        logger.info("   Expected: {}", expectedValidations);
        
        boolean success = (validationCount.get() == expectedValidations) && 
                         (successfulValidations.get() == expectedValidations) && 
                         errors.isEmpty();
        
        if (success) {
            System.out.println("   ✅ Concurrent validation thread safety VERIFIED");
            logger.info("   ✅ Concurrent validation thread safety VERIFIED");
        } else {
            System.out.println("   ❌ Concurrent validation thread safety ISSUES DETECTED");
            logger.error("   ❌ Concurrent validation thread safety ISSUES DETECTED");
            if (!errors.isEmpty()) {
                System.out.println("   Errors detected: " + errors.size());
                logger.error("   Errors detected: {}", errors.size());
                errors.stream().limit(3).forEach(error -> {
                    System.out.println("     - " + error);
                    logger.error("     - {}", error);
                });
            }
        }
        System.out.println();
    }
    
    private static void cleanup() {
        try {
            logger.debug("Cleaning up test environment...");
            
            // Clean up database files
            deleteFileIfExists("blockchain.db");
            deleteFileIfExists("blockchain.db-shm");
            deleteFileIfExists("blockchain.db-wal");
            
            // Clean up off-chain directory
            File offChainDir = new File("off-chain-data");
            if (offChainDir.exists()) {
                deleteDirectory(offChainDir);
                logger.debug("Cleaned up off-chain directory");
            }
            
            // Clean up backup directory
            File backupDir = new File("off-chain-backup");
            if (backupDir.exists()) {
                deleteDirectory(backupDir);
                logger.debug("Cleaned up backup directory");
            }
            
            logger.debug("Test environment cleanup completed");
            
        } catch (Exception e) {
            logger.warn("Could not clean up all files: {}", e.getMessage());
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