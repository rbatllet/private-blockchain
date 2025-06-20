import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RaceConditionTest {
    public static void main(String[] args) throws Exception {
        System.out.println("🔍 RACE CONDITION FIX VALIDATION TEST");
        System.out.println("=====================================");
        
        // Setup
        Blockchain blockchain = new Blockchain();
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        
        // Add authorized key
        String publicKeyString = CryptoUtil.publicKeyToString(publicKey);
        blockchain.addAuthorizedKey(publicKeyString, "TestUser");
        
        // Test parameters
        final int THREAD_COUNT = 50;
        final int BLOCKS_PER_THREAD = 10;
        final int EXPECTED_TOTAL_BLOCKS = THREAD_COUNT * BLOCKS_PER_THREAD + 1; // +1 for genesis
        
        System.out.println("⚙️  Test Configuration:");
        System.out.println("   Threads: " + THREAD_COUNT);
        System.out.println("   Blocks per thread: " + BLOCKS_PER_THREAD);
        System.out.println("   Expected total blocks: " + EXPECTED_TOTAL_BLOCKS);
        System.out.println("");
        
        // PHASE 1: Add all blocks first (like the working tests do)
        System.out.println("🚀 PHASE 1: Adding blocks concurrently...");
        CountDownLatch addStartLatch = new CountDownLatch(1);
        CountDownLatch addEndLatch = new CountDownLatch(THREAD_COUNT);
        List<String> addErrors = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger addSuccessCount = new AtomicInteger(0);
        
        long addStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    addStartLatch.await();
                    
                    for (int j = 0; j < BLOCKS_PER_THREAD; j++) {
                        String data = "Thread-" + threadId + "-Block-" + j;
                        
                        if (blockchain.addBlock(data, privateKey, publicKey)) {
                            addSuccessCount.incrementAndGet();
                        } else {
                            addErrors.add("Thread " + threadId + " failed to add block " + j);
                        }
                    }
                } catch (Exception e) {
                    addErrors.add("Thread " + threadId + " exception: " + e.getMessage());
                } finally {
                    addEndLatch.countDown();
                }
            }).start();
        }
        
        addStartLatch.countDown();
        addEndLatch.await();
        
        long addEndTime = System.currentTimeMillis();
        System.out.println("✅ Block addition completed in " + (addEndTime - addStartTime) + " ms");
        System.out.println("   Successfully added: " + addSuccessCount.get() + " blocks");
        System.out.println("   Errors: " + addErrors.size());
        System.out.println("");
        
        // PHASE 2: Test concurrent reads of getLastBlock (the real race condition test)
        System.out.println("🔍 PHASE 2: Testing concurrent getLastBlock() calls...");
        final int READ_THREAD_COUNT = 100;
        final int READS_PER_THREAD = 20;
        
        List<Long> capturedBlockNumbers = Collections.synchronizedList(new ArrayList<>());
        List<String> readErrors = Collections.synchronizedList(new ArrayList<>());
        
        CountDownLatch readStartLatch = new CountDownLatch(1);
        CountDownLatch readEndLatch = new CountDownLatch(READ_THREAD_COUNT);
        
        long readStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < READ_THREAD_COUNT; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    readStartLatch.await();
                    
                    for (int j = 0; j < READS_PER_THREAD; j++) {
                        Block lastBlock = blockchain.getLastBlock();
                        if (lastBlock != null) {
                            capturedBlockNumbers.add(lastBlock.getBlockNumber());
                        } else {
                            readErrors.add("Thread " + threadId + " got null last block on read " + j);
                        }
                        
                        // Small delay to increase concurrency
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    readErrors.add("Thread " + threadId + " read exception: " + e.getMessage());
                } finally {
                    readEndLatch.countDown();
                }
            }).start();
        }
        
        readStartLatch.countDown();
        readEndLatch.await();
        
        long readEndTime = System.currentTimeMillis();
        System.out.println("✅ Concurrent reads completed in " + (readEndTime - readStartTime) + " ms");
        System.out.println("");
        
        // Analyze results for race condition
        System.out.println("📊 RACE CONDITION ANALYSIS:");
        System.out.println("=============================");
        
        Set<Long> uniqueNumbers = new HashSet<>(capturedBlockNumbers);
        
        System.out.println("Total read operations: " + capturedBlockNumbers.size());
        System.out.println("Expected reads: " + (READ_THREAD_COUNT * READS_PER_THREAD));
        System.out.println("Unique block numbers seen: " + uniqueNumbers.size());
        System.out.println("Read errors: " + readErrors.size());
        
        // In this test, ALL reads should return the SAME block number (the last one)
        // If we see multiple different block numbers, that would indicate a real race condition
        if (uniqueNumbers.size() == 1) {
            Long lastBlockNumber = uniqueNumbers.iterator().next();
            System.out.println("✅ SUCCESS! All reads returned the same block number: " + lastBlockNumber);
            System.out.println("✅ getLastBlock() is consistent under concurrency!");
        } else {
            System.out.println("❌ FAILURE! Reads returned " + uniqueNumbers.size() + " different block numbers.");
            System.out.println("❌ This indicates a race condition in getLastBlock()!");
            
            System.out.println("\n🔍 Different block numbers seen:");
            uniqueNumbers.stream().sorted().forEach(num -> {
                long count = capturedBlockNumbers.stream().mapToLong(Long::longValue).filter(n -> n == num).count();
                System.out.println("   Block " + num + " seen " + count + " times");
            });
        }
        
        // Verify blockchain integrity
        System.out.println("");
        System.out.println("🔗 BLOCKCHAIN INTEGRITY CHECK:");
        System.out.println("===============================");
        
        long finalBlockCount = blockchain.getBlockCount();
        boolean chainValid = blockchain.validateChain();
        
        System.out.println("Final block count: " + finalBlockCount);
        System.out.println("Expected block count: " + EXPECTED_TOTAL_BLOCKS);
        System.out.println("Chain validation: " + (chainValid ? "PASSED" : "FAILED"));
        System.out.println("Total errors: " + (addErrors.size() + readErrors.size()));
        
        // Final result
        System.out.println("");
        System.out.println("🎯 FINAL RESULT:");
        System.out.println("================");
        
        boolean raceConditionFixed = (uniqueNumbers.size() == 1);
        boolean integrityGood = chainValid && finalBlockCount == EXPECTED_TOTAL_BLOCKS;
        boolean noErrors = (addErrors.size() + readErrors.size()) == 0;
        
        if (raceConditionFixed && integrityGood && noErrors) {
            System.out.println("🎉 PERFECT! All tests passed!");
            System.out.println("✅ No race condition detected in getLastBlock()");
            System.out.println("✅ Blockchain integrity maintained");
            System.out.println("✅ No errors during operations");
            System.exit(0);
        } else if (raceConditionFixed) {
            System.out.println("✅ Race condition is fixed, but other issues detected");
            if (!integrityGood) System.out.println("⚠️  Blockchain integrity issues");
            if (!noErrors) System.out.println("⚠️  " + (addErrors.size() + readErrors.size()) + " errors occurred");
            System.exit(1);
        } else {
            System.out.println("❌ Race condition still exists in getLastBlock()");
            System.exit(2);
        }
    }
}
