package com.rbatllet.blockchain.advanced;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.security.KeyPair;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Data Integrity and Persistence Thread-Safety Tests
 * Focuses on ensuring data consistency under concurrent access
 */
public class DataIntegrityThreadSafetyTest {

    private Blockchain blockchain;
    private ExecutorService executorService;
    private final int THREAD_COUNT = 12;

    @BeforeEach
    void setUp() {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();
        executorService = Executors.newVirtualThreadPerTaskExecutor(); // Java 25 Virtual Threads;
    }

    @Test
    @DisplayName("🔐 Data Integrity Under Concurrent Modifications")
    @Timeout(40)
    void testDataIntegrityUnderConcurrentModifications() throws InterruptedException {
        // Setup test data
        List<KeyPair> keyPairs = setupMultipleKeys(3);
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);
        
        AtomicInteger dataCorruptions = new AtomicInteger(0);
        AtomicInteger hashMismatches = new AtomicInteger(0);
        AtomicInteger signatureFailures = new AtomicInteger(0);
        AtomicLong totalOperations = new AtomicLong(0);
        
        ConcurrentLinkedQueue<String> integrityErrors = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    Random random = new Random(threadId);
                    
                    for (int op = 0; op < 15; op++) {
                        try {
                            totalOperations.incrementAndGet();
                            
                            // Add blocks with verification
                            KeyPair keyPair = keyPairs.get(random.nextInt(keyPairs.size()));
                            String data = generateComplexData(threadId, op);
                            
                            // Use addBlockAndReturn to get the specific block created by this thread
                            Block addedBlock = blockchain.addBlockAndReturn(data, keyPair.getPrivate(), keyPair.getPublic());
                            
                            if (addedBlock != null) {
                                // Immediately verify the added block
                                if (!verifyBlockIntegrity(addedBlock, keyPair)) {
                                    dataCorruptions.incrementAndGet();
                                    integrityErrors.offer("T" + threadId + ": Block integrity failure on block #" + 
                                        addedBlock.getBlockNumber());
                                }
                            }
                            
                            // Periodically verify chain integrity
                            if (op % 5 == 0) {
                                if (!verifyChainIntegrity(threadId, integrityErrors, hashMismatches, signatureFailures)) {
                                    dataCorruptions.incrementAndGet();
                                }
                            }
                            
                            // Small random delay
                            if (random.nextInt(20) == 0) {
                                Thread.sleep(random.nextInt(3));
                            }
                            
                        } catch (Exception e) {
                            integrityErrors.offer("T" + threadId + ": Exception during operation: " + e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(35, TimeUnit.SECONDS);
        assertTrue(completed, "Data integrity test should complete within timeout");

        // Final comprehensive integrity check
        var validationResult = blockchain.validateChainDetailed();
        boolean finalIntegrityCheck = validationResult.isStructurallyIntact() && validationResult.isFullyCompliant();

        System.out.println("🔐 Data Integrity Results:");
        System.out.println("   - Total operations: " + totalOperations.get());
        System.out.println("   - Data corruptions: " + dataCorruptions.get());
        System.out.println("   - Hash mismatches: " + hashMismatches.get());
        System.out.println("   - Signature failures: " + signatureFailures.get());
        System.out.println("   - Final block count: " + blockchain.getBlockCount());
        System.out.println("   - Final integrity check: " + (finalIntegrityCheck ? "✅ PASS" : "❌ FAIL"));
        System.out.println("   - Structurally intact: " + validationResult.isStructurallyIntact());
        System.out.println("   - Fully compliant: " + validationResult.isFullyCompliant());

        if (!integrityErrors.isEmpty()) {
            System.out.println("⚠️ Integrity errors:");
            integrityErrors.stream().limit(5).forEach(System.out::println);
        }

        assertEquals(0, dataCorruptions.get(), "No data corruptions should occur");
        assertEquals(0, hashMismatches.get(), "No hash mismatches should occur");
        assertEquals(0, signatureFailures.get(), "No signature failures should occur");
        assertTrue(validationResult.isStructurallyIntact(), "Final chain should be structurally intact");
        assertTrue(validationResult.isFullyCompliant(), "Final chain should be fully compliant");
    }

    @Test
    @DisplayName("⏱️ Timestamp Consistency Under Concurrent Access")
    @Timeout(30)
    void testTimestampConsistency() throws InterruptedException {
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
        blockchain.createBootstrapAdmin(publicKeyString, "TimestampUser");

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);
        
        AtomicInteger timestampInconsistencies = new AtomicInteger(0);
        ConcurrentLinkedQueue<String> timestampErrors = new ConcurrentLinkedQueue<>();
        List<LocalDateTime> blockTimestamps = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int op = 0; op < 8; op++) {
                        try {
                            LocalDateTime beforeAdd = LocalDateTime.now();
                            String data = "TimestampBlock-T" + threadId + "-" + op;
                            
                            // Use addBlockAndReturn to get the specific block created by this thread
                            Block addedBlock = blockchain.addBlockAndReturn(data, keyPair.getPrivate(), keyPair.getPublic());
                            LocalDateTime afterAdd = LocalDateTime.now();
                            
                            if (addedBlock != null) {
                                LocalDateTime blockTimestamp = addedBlock.getTimestamp();
                                blockTimestamps.add(blockTimestamp);
                                
                                // Verify timestamp is within reasonable bounds
                                if (blockTimestamp.isBefore(beforeAdd.minusSeconds(1)) || 
                                    blockTimestamp.isAfter(afterAdd.plusSeconds(1))) {
                                    timestampInconsistencies.incrementAndGet();
                                    timestampErrors.offer("T" + threadId + ": Timestamp out of bounds for block #" + 
                                        addedBlock.getBlockNumber() + " - " + blockTimestamp);
                                }
                            }
                            
                            Thread.sleep(1); // Small delay to create timestamp variations
                            
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (Exception e) {
                            timestampErrors.offer("T" + threadId + ": Exception: " + e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(25, TimeUnit.SECONDS);
        assertTrue(completed, "Timestamp consistency test should complete within timeout");

        // Analyze timestamp ordering
        List<Block> allBlocks = new ArrayList<>();
        blockchain.processChainInBatches(batch -> allBlocks.addAll(batch), 1000);

        int timestampOrderingIssues = 0;

        for (int i = 1; i < allBlocks.size(); i++) {
            LocalDateTime prevTimestamp = allBlocks.get(i-1).getTimestamp();
            LocalDateTime currTimestamp = allBlocks.get(i).getTimestamp();

            // Timestamps should generally be non-decreasing (allowing for same timestamps)
            if (currTimestamp.isBefore(prevTimestamp.minusSeconds(1))) {
                timestampOrderingIssues++;
                timestampErrors.offer("Timestamp ordering issue: Block #" + allBlocks.get(i).getBlockNumber() +
                    " has timestamp " + currTimestamp + " which is before previous block's " + prevTimestamp);
            }
        }

        System.out.println("⏱️ Timestamp Consistency Results:");
        System.out.println("   - Timestamp inconsistencies: " + timestampInconsistencies.get());
        System.out.println("   - Timestamp ordering issues: " + timestampOrderingIssues);
        System.out.println("   - Total blocks analyzed: " + allBlocks.size());

        if (!timestampErrors.isEmpty()) {
            System.out.println("⚠️ Timestamp errors:");
            timestampErrors.stream().limit(3).forEach(System.out::println);
        }

        assertEquals(0, timestampInconsistencies.get(), "No timestamp inconsistencies should occur");
        assertTrue(timestampOrderingIssues <= 2, "Minimal timestamp ordering issues are acceptable in concurrent environment");
    }

    @Test
    @DisplayName("🔢 Block Number Sequence Integrity")
    @Timeout(25)
    void testBlockNumberSequenceIntegrity() throws InterruptedException {
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
        blockchain.createBootstrapAdmin(publicKeyString, "SequenceUser");

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);
        
        AtomicInteger successfulBlocks = new AtomicInteger(0);
        Set<Long> observedBlockNumbers = ConcurrentHashMap.newKeySet();
        ConcurrentLinkedQueue<String> sequenceErrors = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int op = 0; op < 7; op++) {
                        try {
                            String data = "SequenceBlock-T" + threadId + "-" + op;
                            
                            // Use addBlockAndReturn to get the specific block created by this thread
                            Block addedBlock = blockchain.addBlockAndReturn(data, keyPair.getPrivate(), keyPair.getPublic());
                            
                            if (addedBlock != null) {
                                successfulBlocks.incrementAndGet();
                                
                                Long blockNumber = addedBlock.getBlockNumber();
                                
                                // Check for duplicate block numbers
                                if (!observedBlockNumbers.add(blockNumber)) {
                                    sequenceErrors.offer("T" + threadId + ": Duplicate block number detected: " + blockNumber);
                                }
                                
                                // Verify block exists at correct position
                                Block blockByNumber = blockchain.getBlock(blockNumber);
                                if (blockByNumber == null || !blockByNumber.getHash().equals(addedBlock.getHash())) {
                                    sequenceErrors.offer("T" + threadId + ": Block retrieval inconsistency for block #" + blockNumber);
                                }
                            }
                            
                        } catch (Exception e) {
                            sequenceErrors.offer("T" + threadId + ": Exception: " + e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(20, TimeUnit.SECONDS);
        assertTrue(completed, "Block numbering test should complete within timeout");

        // Show detailed validation after concurrent operations
        System.out.println("🔍 Detailed validation after concurrent block number sequence operations:");
        blockchain.validateChainDetailed();
        
        // Verify final sequence integrity
        List<Block> allBlocks = new ArrayList<>();
        blockchain.processChainInBatches(batch -> allBlocks.addAll(batch), 1000);

        int sequenceGaps = 0;

        for (int i = 1; i < allBlocks.size(); i++) {
            Long prevNumber = allBlocks.get(i-1).getBlockNumber();
            Long currNumber = allBlocks.get(i).getBlockNumber();

            if (!currNumber.equals(prevNumber + 1)) {
                sequenceGaps++;
                sequenceErrors.offer("Sequence gap: Block #" + prevNumber + " followed by block #" + currNumber);
            }
        }

        System.out.println("🔢 Block Numbering Results:");
        System.out.println("   - Successful blocks: " + successfulBlocks.get());
        System.out.println("   - Observed unique numbers: " + observedBlockNumbers.size());
        System.out.println("   - Sequence gaps: " + sequenceGaps);
        System.out.println("   - Total blocks: " + blockchain.getBlockCount());

        if (!sequenceErrors.isEmpty()) {
            System.out.println("⚠️ Sequence errors:");
            sequenceErrors.stream().limit(3).forEach(System.out::println);
        }

        assertEquals(0, sequenceGaps, "No sequence gaps should exist");
        assertEquals(successfulBlocks.get() + 1, blockchain.getBlockCount(), "Block count should match successful operations plus genesis");
        assertEquals(observedBlockNumbers.size(), successfulBlocks.get(), "All successful blocks should have unique numbers");
    }

    // Helper methods
    
    private List<KeyPair> setupMultipleKeys(int count) {
        List<KeyPair> keyPairs = new ArrayList<>();

        // RBAC FIX (v1.0.6): First user is bootstrap admin, rest are created by admin
        for (int i = 0; i < count; i++) {
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());

            if (i == 0) {
                // First user is bootstrap admin
                blockchain.createBootstrapAdmin(publicKeyString, "IntegrityUser-" + i);
            } else {
                // Subsequent users are created by the first admin
                blockchain.addAuthorizedKey(publicKeyString, "IntegrityUser-" + i, keyPairs.get(0), UserRole.USER);
            }
            keyPairs.add(keyPair);
        }

        return keyPairs;
    }
    
    private String generateComplexData(int threadId, int op) {
        return String.format("ComplexData-T%d-Op%d-Time%d-Random%d", 
            threadId, op, System.currentTimeMillis(), new Random().nextInt(10000));
    }
    
    private boolean verifyBlockIntegrity(Block block, KeyPair expectedKeyPair) {
        try {
            // Verify hash integrity
            String expectedHash = CryptoUtil.calculateHash(blockchain.buildBlockContent(block));
            if (!expectedHash.equals(block.getHash())) {
                return false;
            }
            
            // Verify signature
            String blockContent = blockchain.buildBlockContent(block);
            return CryptoUtil.verifySignature(blockContent, block.getSignature(), expectedKeyPair.getPublic());
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean verifyChainIntegrity(int threadId, ConcurrentLinkedQueue<String> errors,
                                       AtomicInteger hashMismatches, AtomicInteger signatureFailures) {
        try {
            List<Block> allBlocks = new ArrayList<>();
            blockchain.processChainInBatches(batch -> allBlocks.addAll(batch), 1000);

            boolean allValid = true;

            for (int i = 1; i < allBlocks.size(); i++) {
                Block currentBlock = allBlocks.get(i);
                Block previousBlock = allBlocks.get(i-1);

                // Verify hash chain
                if (!currentBlock.getPreviousHash().equals(previousBlock.getHash())) {
                    hashMismatches.incrementAndGet();
                    errors.offer("T" + threadId + ": Hash chain break at block #" + currentBlock.getBlockNumber());
                    allValid = false;
                }

                // Verify block hash
                String expectedHash = CryptoUtil.calculateHash(blockchain.buildBlockContent(currentBlock));
                if (!expectedHash.equals(currentBlock.getHash())) {
                    hashMismatches.incrementAndGet();
                    errors.offer("T" + threadId + ": Hash mismatch on block #" + currentBlock.getBlockNumber());
                    allValid = false;
                }
            }

            return allValid;

        } catch (Exception e) {
            errors.offer("T" + threadId + ": Exception during chain verification: " + e.getMessage());
            return false;
        }
    }
}
