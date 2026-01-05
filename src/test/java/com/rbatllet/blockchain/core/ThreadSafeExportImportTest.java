package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;
import com.rbatllet.blockchain.util.BaseBlockchainTest;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Thread safety tests for blockchain export/import operations
 * Validates concurrent access patterns and data integrity
 */
public class ThreadSafeExportImportTest extends BaseBlockchainTest {

    private Blockchain blockchain;
    private KeyPair bootstrapKeyPair;
    private KeyPair keyPair;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private String publicKeyString;
    private String masterPassword;
    private ExecutorService executorService;
    
    @BeforeEach
    void setUp() throws Exception {
        blockchain = new Blockchain();

        // Ensure clean state
        blockchain.clearAndReinitialize();

        // Load bootstrap admin keys (auto-generates if missing - test-only)
        bootstrapKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();

        // Register bootstrap admin in blockchain
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        keyPair = CryptoUtil.generateKeyPair();
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();
        publicKeyString = CryptoUtil.publicKeyToString(publicKey);
        masterPassword = "ThreadSafePassword123!";
        executorService = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("TestWorker-", 0).factory()); // Java 25 Virtual Threads;

        blockchain.addAuthorizedKey(publicKeyString, "TestUser", bootstrapKeyPair, UserRole.USER);
    }
    
    @AfterEach
    void tearDown() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
        
        // Clean up test files
        deleteFileIfExists("thread-test-export-1.json");
        deleteFileIfExists("thread-test-export-2.json");
        deleteFileIfExists("thread-test-export-3.json");
        deleteFileIfExists("thread-test-encrypted-1.json");
        deleteFileIfExists("thread-test-encrypted-2.json");
        deleteFileIfExists("concurrent-export.json");
        deleteDirectory("off-chain-backup");
    }
    
    @Test
    @DisplayName("Concurrent regular exports should work without interference")
    void testConcurrentRegularExports() throws Exception {
        // Add test data
        blockchain.addBlockAndReturn("Test data 1", privateKey, publicKey);
        blockchain.addBlockAndReturn("Test data 2", privateKey, publicKey);
        blockchain.addBlockAndReturn("Test data 3", privateKey, publicKey);
        
        // Create concurrent export tasks
        List<CompletableFuture<Boolean>> exportTasks = new ArrayList<>();
        
        for (int i = 1; i <= 3; i++) {
            final int taskId = i;
            CompletableFuture<Boolean> task = CompletableFuture.supplyAsync(() -> {
                try {
                    return blockchain.exportChain("thread-test-export-" + taskId + ".json");
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }, executorService);
            exportTasks.add(task);
        }
        
        // Wait for all exports to complete
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(
            exportTasks.toArray(new CompletableFuture[0])
        );
        allTasks.get(30, TimeUnit.SECONDS);
        
        // Verify all exports succeeded
        for (int i = 0; i < exportTasks.size(); i++) {
            assertTrue(exportTasks.get(i).get(), "Export task " + (i + 1) + " should succeed");
            
            // Verify export file exists and has content
            File exportFile = new File("thread-test-export-" + (i + 1) + ".json");
            assertTrue(exportFile.exists(), "Export file " + (i + 1) + " should exist");
            assertTrue(exportFile.length() > 0, "Export file " + (i + 1) + " should not be empty");
        }
    }
    
    @Test
    @DisplayName("Concurrent encrypted exports should work without interference")
    void testConcurrentEncryptedExports() throws Exception {
        // Add mixed test data
        blockchain.addEncryptedBlock("Encrypted data 1", masterPassword, privateKey, publicKey);
        blockchain.addBlockAndReturn("Regular data 1", privateKey, publicKey);
        blockchain.addEncryptedBlock("Encrypted data 2", masterPassword, privateKey, publicKey);
        
        // Create concurrent encrypted export tasks
        List<CompletableFuture<Boolean>> exportTasks = new ArrayList<>();
        
        for (int i = 1; i <= 2; i++) {
            final int taskId = i;
            CompletableFuture<Boolean> task = CompletableFuture.supplyAsync(() -> {
                try {
                    return blockchain.exportEncryptedChain("thread-test-encrypted-" + taskId + ".json", masterPassword);
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }, executorService);
            exportTasks.add(task);
        }
        
        // Wait for all encrypted exports to complete
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(
            exportTasks.toArray(new CompletableFuture[0])
        );
        allTasks.get(30, TimeUnit.SECONDS);
        
        // Verify all encrypted exports succeeded
        for (int i = 0; i < exportTasks.size(); i++) {
            assertTrue(exportTasks.get(i).get(), "Encrypted export task " + (i + 1) + " should succeed");
            
            // Verify export file exists and has content
            File exportFile = new File("thread-test-encrypted-" + (i + 1) + ".json");
            assertTrue(exportFile.exists(), "Encrypted export file " + (i + 1) + " should exist");
            assertTrue(exportFile.length() > 0, "Encrypted export file " + (i + 1) + " should not be empty");
        }
    }
    
    @Test
    @DisplayName("Import operations should be mutually exclusive")
    void testImportMutualExclusion() throws Exception {
        // Create test data and export it
        blockchain.addBlockAndReturn("Test mutual exclusion", privateKey, publicKey);
        assertTrue(blockchain.exportChain("concurrent-export.json"));
        
        // Create multiple import tasks that should be serialized
        List<CompletableFuture<Boolean>> importTasks = new ArrayList<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        
        for (int i = 0; i < 3; i++) {
            CompletableFuture<Boolean> task = CompletableFuture.supplyAsync(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await(5, TimeUnit.SECONDS);

                    // Clear and import
                    blockchain.clearAndReinitialize();

                    // RBAC FIX (v1.0.6): Re-create bootstrap admin after clearAndReinitialize()
                    blockchain.createBootstrapAdmin(
                        CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
                        "BOOTSTRAP_ADMIN"
                    );

                    blockchain.addAuthorizedKey(publicKeyString, "TestUser", bootstrapKeyPair, UserRole.USER);
                    return blockchain.importChain("concurrent-export.json");
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }, executorService);
            importTasks.add(task);
        }
        
        // Start all import tasks simultaneously
        startLatch.countDown();
        
        // Wait for all imports to complete
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(
            importTasks.toArray(new CompletableFuture[0])
        );
        allTasks.get(30, TimeUnit.SECONDS);
        
        // At least one import should succeed (due to mutual exclusion, they won't interfere)
        long successCount = importTasks.stream()
            .mapToLong(task -> {
                try {
                    return task.get() ? 1L : 0L;
                } catch (Exception e) {
                    return 0L;
                }
            })
            .sum();
        
        assertTrue(successCount >= 1, "At least one import should succeed");
        
        // Verify final blockchain state is consistent
        var validationResult = blockchain.validateChainDetailed();
        assertTrue(validationResult.isStructurallyIntact(), "Blockchain should remain structurally intact");
    }
    
    @Test
    @DisplayName("Mixed export and import operations should not deadlock")
    void testMixedExportImportOperations() throws Exception {
        // Create initial test data
        blockchain.addBlockAndReturn("Initial data", privateKey, publicKey);
        blockchain.addEncryptedBlock("Initial encrypted", masterPassword, privateKey, publicKey);
        
        // Export initial state
        assertTrue(blockchain.exportEncryptedChain("concurrent-export.json", masterPassword));
        
        List<CompletableFuture<Boolean>> mixedTasks = new ArrayList<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        
        // Add export tasks (should not block each other)
        for (int i = 0; i < 3; i++) {
            final int taskId = i;
            CompletableFuture<Boolean> exportTask = CompletableFuture.supplyAsync(() -> {
                try {
                    startLatch.await(5, TimeUnit.SECONDS);
                    return blockchain.exportChain("thread-test-export-" + taskId + ".json");
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }, executorService);
            mixedTasks.add(exportTask);
        }
        
        // Add one import task (should be exclusive)
        CompletableFuture<Boolean> importTask = CompletableFuture.supplyAsync(() -> {
            try {
                startLatch.await(5, TimeUnit.SECONDS);
                Thread.sleep(100); // Let exports start first

                blockchain.clearAndReinitialize();

                // RBAC FIX (v1.0.6): Re-create bootstrap admin after clearAndReinitialize()
                blockchain.createBootstrapAdmin(
                    CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
                    "BOOTSTRAP_ADMIN"
                );

                blockchain.addAuthorizedKey(publicKeyString, "TestUser", bootstrapKeyPair, UserRole.USER);
                return blockchain.importEncryptedChain("concurrent-export.json", masterPassword);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }, executorService);
        mixedTasks.add(importTask);
        
        // Start all tasks
        startLatch.countDown();
        
        // Wait for completion (should not deadlock)
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(
            mixedTasks.toArray(new CompletableFuture[0])
        );
        allTasks.get(30, TimeUnit.SECONDS);
        
        // Verify import succeeded
        assertTrue(importTask.get(), "Import should succeed");
        
        // Verify exports succeeded
        for (int i = 0; i < 3; i++) {
            assertTrue(mixedTasks.get(i).get(), "Export task " + i + " should succeed");
        }
        
        // Verify blockchain integrity
        var validationResult = blockchain.validateChainDetailed();
        assertTrue(validationResult.isStructurallyIntact(), "Blockchain should be intact after mixed operations");
    }
    
    @Test
    @DisplayName("Large dataset export/import stress test")
    void testLargeDatasetStressTest() throws Exception {
        // Create a larger dataset
        for (int i = 0; i < 20; i++) {
            if (i % 3 == 0) {
                blockchain.addEncryptedBlock("Large encrypted data " + i + " with more content", 
                    masterPassword, privateKey, publicKey);
            } else {
                blockchain.addBlockAndReturn("Large regular data " + i + " with more content", 
                    privateKey, publicKey);
            }
        }
        
        // Create large off-chain data
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeData.append("Large off-chain dataset entry ").append(i).append(" with content. ");
        }
        blockchain.addBlockAndReturn(largeData.toString(), privateKey, publicKey);
        
        // Concurrent stress test
        List<CompletableFuture<Boolean>> stressTasks = new ArrayList<>();
        
        // Multiple concurrent exports
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            CompletableFuture<Boolean> task = CompletableFuture.supplyAsync(() -> {
                try {
                    if (taskId % 2 == 0) {
                        return blockchain.exportEncryptedChain("stress-encrypted-" + taskId + ".json", masterPassword);
                    } else {
                        return blockchain.exportChain("stress-regular-" + taskId + ".json");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }, executorService);
            stressTasks.add(task);
        }
        
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(
            stressTasks.toArray(new CompletableFuture[0])
        );
        allTasks.get(60, TimeUnit.SECONDS); // Longer timeout for large data
        
        // Verify all stress tests succeeded
        for (int i = 0; i < stressTasks.size(); i++) {
            assertTrue(stressTasks.get(i).get(), "Stress test task " + i + " should succeed");
        }
        
        // Clean up stress test files
        for (int i = 0; i < 5; i++) {
            deleteFileIfExists("stress-encrypted-" + i + ".json");
            deleteFileIfExists("stress-regular-" + i + ".json");
        }
    }
    
    // Helper methods
    
    private void deleteFileIfExists(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
    }
    
    private void deleteDirectory(String dirPath) {
        File dir = new File(dirPath);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            dir.delete();
        }
    }
}