package com.rbatllet.blockchain.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.security.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;
import com.rbatllet.blockchain.util.CryptoUtil;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Thread-safety tests for ECKeyDerivation
 */
public class ECKeyDerivationThreadSafetyTest {

    private ECKeyDerivation keyDerivation;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        keyDerivation = new ECKeyDerivation();
        executor = Executors.newFixedThreadPool(10);
    }

    @Test
    void testConcurrentKeyDerivation() throws InterruptedException, ExecutionException {
        int numThreads = 10;
        int operationsPerThread = 50;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            Future<?> future = executor.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        try {
                            KeyPair keyPair = CryptoUtil.generateECKeyPair();
                            PublicKey derivedKey = keyDerivation.derivePublicKeyFromPrivate(keyPair.getPrivate());
                            
                            assertNotNull(derivedKey);
                            assertEquals(CryptoUtil.EC_ALGORITHM, derivedKey.getAlgorithm());
                            
                            // Verify the derived key works for signature verification
                            String testData = "Test data " + Thread.currentThread().getName() + "_" + j;
                            String signature = CryptoUtil.signData(testData, keyPair.getPrivate());
                            assertTrue(CryptoUtil.verifySignature(testData, signature, derivedKey));
                            
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            e.printStackTrace();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
            futures.add(future);
        }

        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for all threads to complete
        boolean completed = completionLatch.await(60, TimeUnit.SECONDS);
        assertTrue(completed, "All threads should complete within timeout");

        // Check for any exceptions in futures
        for (Future<?> future : futures) {
            future.get(); // This will throw if there was an exception
        }

        int expectedSuccesses = numThreads * operationsPerThread;
        assertEquals(expectedSuccesses, successCount.get(), 
            "All operations should succeed. Errors: " + errorCount.get());
        assertEquals(0, errorCount.get(), "No errors should occur");
        
        System.out.println("✅ Concurrent test passed: " + successCount.get() + " operations completed successfully");
    }

    @Test
    void testConcurrentCacheAccess() throws InterruptedException, ExecutionException {
        int numThreads = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            Future<?> future = executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    // Access the curve cache concurrently
                    for (int j = 0; j < 10; j++) {
                        try {
                            var params = keyDerivation.getCurveParameters(CryptoUtil.EC_CURVE);
                            assertNotNull(params);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
            futures.add(future);
        }

        startLatch.countDown();
        boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "Cache access test should complete within timeout");

        for (Future<?> future : futures) {
            future.get();
        }

        assertEquals(numThreads * 10, successCount.get(), "All cache accesses should succeed");
        System.out.println("✅ Concurrent cache test passed: " + successCount.get() + " cache accesses completed");
    }

    @Test
    void testProviderInitializationThreadSafety() throws InterruptedException {
        // This test ensures that multiple ECKeyDerivation class loads don't cause issues
        int numThreads = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    
                    // Create multiple instances to test static initialization
                    ECKeyDerivation derivation = new ECKeyDerivation();
                    KeyPair keyPair = CryptoUtil.generateECKeyPair();
                    PublicKey derivedKey = derivation.derivePublicKeyFromPrivate(keyPair.getPrivate());
                    
                    assertNotNull(derivedKey);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    completionLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "Provider initialization test should complete");
        assertEquals(numThreads, successCount.get(), "All threads should succeed");
        
        System.out.println("✅ Provider initialization test passed: " + successCount.get() + " threads completed");
    }
}
