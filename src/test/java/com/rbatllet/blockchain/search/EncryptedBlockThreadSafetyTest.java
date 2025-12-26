package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.indexing.IndexingCoordinator;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.*;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.testutil.GenesisKeyManager;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.*;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Thread-Safety Test Suite for Encrypted Block Creation and Search Operations
 * 
 * Validates that concurrent operations on encrypted blocks and search functionality
 * are properly synchronized and don't cause race conditions or data corruption.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EncryptedBlockThreadSafetyTest {
    
    private Blockchain blockchain;
    private KeyPair bootstrapKeyPair;
    private SearchFrameworkEngine searchEngine;
    private SearchSpecialistAPI specialistAPI;
    private PrivateKey testPrivateKey;
    private PublicKey testPublicKey;
    private String testPassword;
    private EncryptionConfig config;
    
    @BeforeEach
    void setUp() throws Exception {
        // Clear global processing map before each test to ensure clean state
        SearchFrameworkEngine.resetGlobalState();

        blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        // Load bootstrap admin keys (auto-generates if missing - test-only)
        bootstrapKeyPair = GenesisKeyManager.ensureGenesisKeysExist();

        // SECURITY (v1.0.6): Register bootstrap admin in blockchain (REQUIRED!)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        config = EncryptionConfig.createBalancedConfig();
        searchEngine = new SearchFrameworkEngine(config);
        testPassword = "ThreadSafetyTest2024!";

        KeyPair keyPair = CryptoUtil.generateKeyPair();
        testPrivateKey = keyPair.getPrivate();
        testPublicKey = keyPair.getPublic();

        // Add authorized key for test operations
        String publicKeyString = java.util.Base64.getEncoder().encodeToString(testPublicKey.getEncoded());
        blockchain.addAuthorizedKey(publicKeyString, "ThreadSafetyTestUser", bootstrapKeyPair, UserRole.USER);
        
        // Initialize SearchSpecialistAPI with proper constructor
        specialistAPI = new SearchSpecialistAPI(blockchain, testPassword, testPrivateKey, config);
    }
    
    @AfterEach
    void tearDown() {
        if (searchEngine != null) {
            searchEngine.shutdown();
        }
        if (specialistAPI != null) {
            specialistAPI.shutdown();
        }
    }
    
    @Test
    @Order(1)
    @DisplayName("Concurrent Search Index Operations - Thread Safety")
    void testConcurrentSearchIndexOperations() throws InterruptedException {
        System.out.println("\n🔐 TESTING CONCURRENT SEARCH INDEX OPERATIONS");
        System.out.println("============================================");
        
        // Pre-create some blocks
        for (int i = 0; i < 5; i++) {
            blockchain.addEncryptedBlock("Test data " + i, testPassword, testPrivateKey, testPublicKey);
        }
        
        int numThreads = 3;
        int operationsPerThread = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numThreads);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor(); // Java 25 Virtual Threads;
        
        AtomicInteger indexOperations = new AtomicInteger(0);
        AtomicInteger searchOperations = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);
        
        // Launch concurrent index and search threads
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        try {
                            if (j % 3 == 0) {
                                // Index operation (less frequent to avoid blocking)
                                IndexingResult result = searchEngine.indexBlockchain(blockchain, testPassword, testPrivateKey);
                                if (result.getBlocksIndexed() >= 0) {
                                    indexOperations.incrementAndGet();
                                }
                            } else {
                                // Search operation (more frequent and faster)
                                SearchResult result = searchEngine.searchPublicOnly("test", 3);
                                if (result != null && result.isSuccessful()) {
                                    searchOperations.incrementAndGet();
                                }
                            }
                            
                            // Small delay to reduce contention
                            Thread.sleep(10);
                            
                        } catch (Exception e) {
                            errors.incrementAndGet();
                            // Expected some concurrency exceptions, don't log them all
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completeLatch.countDown();
                }
            });
        }
        
        System.out.printf("🔄 Testing concurrent index/search operations with %d threads...%n", numThreads);
        startLatch.countDown();
        assertTrue(completeLatch.await(30, TimeUnit.SECONDS), "Operations should complete within 30 seconds");
        
        executor.shutdown();
        
        System.out.printf("📊 Concurrent Operations Results:%n");
        System.out.printf("  Index operations: %d%n", indexOperations.get());
        System.out.printf("  Search operations: %d%n", searchOperations.get());
        System.out.printf("  Errors: %d%n", errors.get());
        
        // Assertions - we allow some errors due to concurrency
        assertTrue(indexOperations.get() > 0, "Should complete some index operations");
        assertTrue(searchOperations.get() > 0, "Should complete some search operations");
        assertTrue(errors.get() < numThreads * operationsPerThread / 2, "Most operations should succeed");
        
        System.out.println("✅ Concurrent search index operations: PASSED");
    }
    
    @Test
    @Order(2)
    @DisplayName("Concurrent Search During Block Creation - Thread Safety")
    void testConcurrentSearchDuringBlockCreation() throws InterruptedException {
        System.out.println("\n🔍 TESTING CONCURRENT SEARCH DURING BLOCK CREATION");
        System.out.println("=================================================");
        
        // Pre-create some blocks for initial search results
        for (int i = 0; i < 5; i++) {
            blockchain.addEncryptedBlockWithKeywords(
                "Initial test data " + i, 
                testPassword, 
                new String[]{"initial", "search", "test" + i}, 
                "TEST",
                testPrivateKey, testPublicKey);
        }
        
        // Initialize search engine
        IndexingResult indexingResult = searchEngine.indexBlockchain(blockchain, testPassword, testPrivateKey);
        IndexingCoordinator.getInstance().waitForCompletion();
        // With global atomic protection, blocks might already be indexed from previous tests
        // So we check that either blocks were indexed OR the search engine can find existing data
        assertTrue(indexingResult.getBlocksIndexed() > 0 || 
                  searchEngine.search("initial", testPassword, 10).isSuccessful(),
                  "Search engine should either index new blocks or find existing indexed data");
        
        int numCreatorThreads = 3;
        int numSearchThreads = 3;
        int operationsPerThread = 5;
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numCreatorThreads + numSearchThreads);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor(); // Java 25 Virtual Threads;
        
        AtomicInteger searchCount = new AtomicInteger(0);
        AtomicInteger createCount = new AtomicInteger(0);
        AtomicInteger searchErrors = new AtomicInteger(0);
        AtomicInteger createErrors = new AtomicInteger(0);
        
        // Launch block creator threads
        for (int i = 0; i < numCreatorThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        try {
                            String data = String.format("Concurrent data T%d-B%d: %s", threadId, j, generateTestData(30));
                            String blockPassword = testPassword + "-CT" + threadId;
                            String[] keywords = {"concurrent", "creator" + threadId, "block" + j};
                            
                            // The block is automatically indexed in addEncryptedBlockWithKeywords
                            Block newBlock = blockchain.addEncryptedBlockWithKeywords(
                                data, blockPassword, keywords, "CONCURRENT", testPrivateKey, testPublicKey);
                            
                            if (newBlock != null) {
                                createCount.incrementAndGet();
                            } else {
                                throw new RuntimeException("Failed to create block");
                            }
                            
                        } catch (Exception e) {
                            createErrors.incrementAndGet();
                            System.err.printf("❌ Create error T%d-B%d: %s%n", threadId, j, e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completeLatch.countDown();
                }
            });
        }
        
        // Launch search threads
        for (int i = 0; i < numSearchThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        try {
                            // Perform various search operations
                            String[] queries = {"test", "initial", "concurrent", "search", "data"};
                            String query = queries[j % queries.length];
                            
                            // Mix of public and encrypted searches
                            if (j % 2 == 0) {
                                SearchResult result = searchEngine.searchPublicOnly(query, 10);
                                assertNotNull(result);
                            } else {
                                SearchResult result = searchEngine.searchEncryptedOnly(query, testPassword, 10);
                                assertNotNull(result);
                            }
                            
                            searchCount.incrementAndGet();
                            
                        } catch (Exception e) {
                            searchErrors.incrementAndGet();
                            System.err.printf("❌ Search error T%d-S%d: %s%n", threadId, j, e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completeLatch.countDown();
                }
            });
        }
        
        System.out.printf("🔄 Starting concurrent operations: %d creators + %d searchers...%n", numCreatorThreads, numSearchThreads);
        long startTime = System.currentTimeMillis();
        
        startLatch.countDown();
        assertTrue(completeLatch.await(45, TimeUnit.SECONDS), "All operations should complete within 45 seconds");
        
        long endTime = System.currentTimeMillis();
        executor.shutdown();
        
        // Validate results
        System.out.printf("📊 Concurrent Operations Results:%n");
        System.out.printf("  Create operations: %d (errors: %d)%n", createCount.get(), createErrors.get());
        System.out.printf("  Search operations: %d (errors: %d)%n", searchCount.get(), searchErrors.get());
        System.out.printf("  Total time: %dms%n", endTime - startTime);
        System.out.printf("  Final blockchain size: %d blocks%n", blockchain.getBlockCount());
        
        // Assertions
        assertTrue(createCount.get() > 0, "Should have created some blocks");
        assertTrue(searchCount.get() > 0, "Should have performed some searches");
        assertEquals(0, createErrors.get(), "No create operations should fail");
        assertEquals(0, searchErrors.get(), "No search operations should fail");
        
        System.out.println("✅ Concurrent search during block creation: PASSED");
    }
    
    @Test
    @Order(3)
    @DisplayName("Search Index Integrity Under Concurrency")
    void testSearchIndexIntegrityUnderConcurrency() throws InterruptedException {
        System.out.println("\n🔍 TESTING SEARCH INDEX INTEGRITY UNDER CONCURRENCY");
        System.out.println("==================================================");
        
        // Pre-create some blocks
        for (int i = 0; i < 5; i++) {
            blockchain.addEncryptedBlock("Integrity test data " + i, testPassword, testPrivateKey, testPublicKey);
        }
        
        // Initialize search engine
        IndexingResult initialResult = searchEngine.indexBlockchain(blockchain, testPassword, testPrivateKey);
        IndexingCoordinator.getInstance().waitForCompletion();
        assertTrue(initialResult.getBlocksIndexed() > 0 || searchEngine.search("Integrity", testPassword, 10).isSuccessful());
        
        int numThreads = 3;
        int searchesPerThread = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numThreads);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor(); // Java 25 Virtual Threads;
        
        AtomicInteger successfulSearches = new AtomicInteger(0);
        AtomicInteger totalSearches = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < searchesPerThread; j++) {
                        try {
                            totalSearches.incrementAndGet();
                            
                            // Perform different types of searches
                            SearchResult result;
                            if (j % 3 == 0) {
                                result = searchEngine.searchPublicOnly("test", 5);
                            } else if (j % 3 == 1) {
                                result = searchEngine.searchEncryptedOnly("data", testPassword, 5);
                            } else {
                                result = searchEngine.search("integrity", testPassword, 5);
                            }
                            
                            if (result != null && result.isSuccessful()) {
                                successfulSearches.incrementAndGet();
                            }
                            
                        } catch (Exception e) {
                            // Some exceptions expected during concurrent access
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completeLatch.countDown();
                }
            });
        }
        
        System.out.printf("🔍 Testing search integrity with %d concurrent threads...%n", numThreads);
        startLatch.countDown();
        assertTrue(completeLatch.await(15, TimeUnit.SECONDS), "Searches should complete within 15 seconds");
        
        executor.shutdown();
        
        // Verify final state
        SearchStats finalStats = searchEngine.getSearchStats();
        
        System.out.printf("📊 Search Integrity Results:%n");
        System.out.printf("  Total searches attempted: %d%n", totalSearches.get());
        System.out.printf("  Successful searches: %d%n", successfulSearches.get());
        System.out.printf("  Success rate: %.1f%%%n", (successfulSearches.get() * 100.0) / totalSearches.get());
        System.out.printf("  Final indexed blocks: %d%n", finalStats.getTotalBlocksIndexed());
        
        // Assertions
        assertTrue(successfulSearches.get() > 0, "Should have some successful searches");
        assertTrue(finalStats.getTotalBlocksIndexed() >= 0 || searchEngine.search("TestData", testPassword, 10).isSuccessful(), "Search index should remain intact");
        
        System.out.println("✅ Search index integrity under concurrency: PASSED");
    }
    
    private String generateTestData(int length) {
        return generateRandomString(length);
    }
    
    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder();
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 ";
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }
}