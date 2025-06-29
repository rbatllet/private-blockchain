package com.rbatllet.blockchain.search;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for search functionality including thread-safety
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SearchFunctionalityTest {

    private Blockchain blockchain;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    
    @BeforeEach
    void setUp() throws Exception {
        // Clean up any existing data
        cleanup();
        
        // Wait a bit to ensure cleanup is complete
        Thread.sleep(100);
        
        blockchain = new Blockchain();
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();
        blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(publicKey), "TestUser");
        
        // Create test blocks with different characteristics
        createTestBlocks();
    }
    
    @AfterEach
    void tearDown() {
        cleanup();
    }
    
    private void cleanup() {
        try {
            deleteFileIfExists("blockchain.db");
            deleteFileIfExists("blockchain.db-shm");
            deleteFileIfExists("blockchain.db-wal");
            deleteDirectory(new java.io.File("off-chain-data"));
            deleteDirectory(new java.io.File("off-chain-backup"));
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    private void createTestBlocks() {
        // Block 1: Medical with manual keywords
        String[] medicalKeywords = {"PATIENT-001", "CARDIOLOGY", "ECG-2024", "MEDICAL"};
        blockchain.addBlockWithKeywords(
            "Patient John Doe underwent ECG examination on 2024-01-15. Results show normal cardiac rhythm.",
            medicalKeywords,
            "MEDICAL",
            privateKey,
            publicKey
        );
        
        // Block 2: Finance with manual keywords
        String[] financeKeywords = {"PROJECT-ALPHA", "BUDGET-2024", "EUR", "FINANCE"};
        blockchain.addBlockWithKeywords(
            "Project Alpha budget allocation: 50000 EUR for Q1 2024. Investment approved.",
            financeKeywords,
            "FINANCE",
            privateKey,
            publicKey
        );
        
        // Block 3: Technical with auto keywords only
        blockchain.addBlockWithKeywords(
            "API integration with SQL database completed. JSON format used for data exchange. Contact: admin@company.com",
            null,
            "TECHNICAL",
            privateKey,
            publicKey
        );
        
        // Block 4: Legal with mixed content
        String[] legalKeywords = {"CONTRACT-2024", "LEGAL", "AGREEMENT"};
        blockchain.addBlockWithKeywords(
            "Legal contract signed on 2024-02-20. Reference number: LEG-001. Contact lawyer@lawfirm.com for details.",
            legalKeywords,
            "LEGAL",
            privateKey,
            publicKey
        );
    }

    // =============== BASIC SEARCH FUNCTIONALITY TESTS ===============
    
    @Test
    @Order(1)
    @DisplayName("Test FAST_ONLY search level - keywords only")
    void testFastOnlySearch() {
        // Test manual keyword search
        List<Block> results = blockchain.searchBlocks("MEDICAL", SearchLevel.FAST_ONLY);
        assertEquals(1, results.size());
        assertEquals("MEDICAL", results.get(0).getContentCategory());
        
        // Test auto keyword search
        results = blockchain.searchBlocks("API", SearchLevel.FAST_ONLY);
        assertEquals(1, results.size());
        assertEquals("TECHNICAL", results.get(0).getContentCategory());
        
        // Test year search (auto extracted)
        results = blockchain.searchBlocks("2024", SearchLevel.FAST_ONLY);
        assertTrue(results.size() >= 3); // Multiple blocks contain 2024
    }
    
    @Test
    @Order(2)
    @DisplayName("Test INCLUDE_DATA search level - keywords and block data")
    void testIncludeDataSearch() {
        // Test search in block data
        List<Block> results = blockchain.searchBlocks("John", SearchLevel.INCLUDE_DATA);
        assertTrue(results.size() >= 1, "Should find at least one block containing 'John'");
        boolean foundJohnDoe = results.stream().anyMatch(block -> block.getData().contains("John Doe"));
        assertTrue(foundJohnDoe, "Should find block containing 'John Doe'");
        
        // Test search that finds both keywords and data
        results = blockchain.searchBlocks("EUR", SearchLevel.INCLUDE_DATA);
        assertTrue(results.size() >= 1, "Should find at least one block with EUR");
        boolean foundFinanceBlock = results.stream().anyMatch(block -> "FINANCE".equals(block.getContentCategory()));
        assertTrue(foundFinanceBlock, "Should find FINANCE block");
        
        // Test email search in data
        results = blockchain.searchBlocks("admin@company.com", SearchLevel.INCLUDE_DATA);
        assertTrue(results.size() >= 1, "Should find at least one block with admin@company.com");
        boolean foundTechnicalBlock = results.stream().anyMatch(block -> "TECHNICAL".equals(block.getContentCategory()));
        assertTrue(foundTechnicalBlock, "Should find TECHNICAL block");
    }
    
    @Test
    @Order(3)
    @DisplayName("Test EXHAUSTIVE_OFFCHAIN search level")
    void testExhaustiveOffchainSearch() {
        // For this test we would need off-chain data, but our test blocks are small
        // So we test that it works the same as INCLUDE_DATA for small blocks
        List<Block> results = blockchain.searchBlocks("lawyer@lawfirm.com", SearchLevel.EXHAUSTIVE_OFFCHAIN);
        assertTrue(results.size() >= 1, "Should find at least one block with lawyer@lawfirm.com");
        boolean foundLegalBlock = results.stream().anyMatch(block -> "LEGAL".equals(block.getContentCategory()));
        assertTrue(foundLegalBlock, "Should find LEGAL block");
    }
    
    @Test
    @Order(4)
    @DisplayName("Test search by category")
    void testSearchByCategory() {
        List<Block> medicalBlocks = blockchain.searchByCategory("MEDICAL");
        assertTrue(medicalBlocks.size() >= 1, "Should find at least one MEDICAL block");
        assertTrue(medicalBlocks.stream().allMatch(block -> "MEDICAL".equals(block.getContentCategory())), 
                  "All returned blocks should be MEDICAL category");
        
        List<Block> financeBlocks = blockchain.searchByCategory("FINANCE");
        assertTrue(financeBlocks.size() >= 1, "Should find at least one FINANCE block");
        assertTrue(financeBlocks.stream().allMatch(block -> "FINANCE".equals(block.getContentCategory())), 
                  "All returned blocks should be FINANCE category");
        
        List<Block> technicalBlocks = blockchain.searchByCategory("TECHNICAL");
        assertTrue(technicalBlocks.size() >= 1, "Should find at least one TECHNICAL block");
        assertTrue(technicalBlocks.stream().allMatch(block -> "TECHNICAL".equals(block.getContentCategory())), 
                  "All returned blocks should be TECHNICAL category");
        
        List<Block> legalBlocks = blockchain.searchByCategory("LEGAL");
        assertTrue(legalBlocks.size() >= 1, "Should find at least one LEGAL block");
        assertTrue(legalBlocks.stream().allMatch(block -> "LEGAL".equals(block.getContentCategory())), 
                  "All returned blocks should be LEGAL category");
        
        // Test non-existent category
        List<Block> nonExistent = blockchain.searchByCategory("NONEXISTENT");
        assertTrue(nonExistent.isEmpty());
    }

    // =============== SEARCH VALIDATION TESTS ===============
    
    @Test
    @Order(5)
    @DisplayName("Test search term validation")
    void testSearchTermValidation() {
        // Test empty/null search terms
        List<Block> results = blockchain.searchBlocksFast("");
        assertTrue(results.isEmpty());
        
        results = blockchain.searchBlocksFast(null);
        assertTrue(results.isEmpty());
        
        results = blockchain.searchBlocksFast("   ");
        assertTrue(results.isEmpty());
        
        // Test short terms that should be rejected
        results = blockchain.searchBlocksFast("hi");
        assertTrue(results.isEmpty());
        
        results = blockchain.searchBlocksFast("a");
        assertTrue(results.isEmpty());
        
        // Test short terms that should be accepted (exceptions)
        results = blockchain.searchBlocksFast("API");
        assertFalse(results.isEmpty());
        
        // Test year search
        results = blockchain.searchBlocksFast("2024");
        assertFalse(results.isEmpty());
    }
    
    @Test
    @Order(6)
    @DisplayName("Test SearchValidator directly")
    void testSearchValidator() {
        // Valid terms
        assertTrue(SearchValidator.isValidSearchTerm("MEDICAL"));
        assertTrue(SearchValidator.isValidSearchTerm("2024"));
        assertTrue(SearchValidator.isValidSearchTerm("API"));
        assertTrue(SearchValidator.isValidSearchTerm("SQL"));
        assertTrue(SearchValidator.isValidSearchTerm("XML"));
        assertTrue(SearchValidator.isValidSearchTerm("JSON"));
        assertTrue(SearchValidator.isValidSearchTerm("12345"));
        assertTrue(SearchValidator.isValidSearchTerm("ID-001"));
        
        // Invalid terms
        assertFalse(SearchValidator.isValidSearchTerm("hi"));
        assertFalse(SearchValidator.isValidSearchTerm("a"));
        assertFalse(SearchValidator.isValidSearchTerm("abc"));
        assertFalse(SearchValidator.isValidSearchTerm(""));
        assertFalse(SearchValidator.isValidSearchTerm(null));
        assertFalse(SearchValidator.isValidSearchTerm("   "));
    }

    // =============== KEYWORD EXTRACTION TESTS ===============
    
    @Test
    @Order(7)
    @DisplayName("Test UniversalKeywordExtractor")
    void testUniversalKeywordExtractor() {
        UniversalKeywordExtractor extractor = new UniversalKeywordExtractor();
        
        // Test email extraction
        String content = "Contact us at support@company.com or admin@test.org for help.";
        String keywords = extractor.extractUniversalKeywords(content);
        assertTrue(keywords.contains("support@company.com"));
        assertTrue(keywords.contains("admin@test.org"));
        
        // Test year extraction
        content = "The project started in 2024 and will end in 2025.";
        keywords = extractor.extractUniversalKeywords(content);
        assertTrue(keywords.contains("2024"));
        assertTrue(keywords.contains("2025"));
        
        // Test code/reference extraction
        content = "Reference number PROJ-001 and ID-123 were assigned.";
        keywords = extractor.extractUniversalKeywords(content);
        assertTrue(keywords.contains("proj-001"));
        assertTrue(keywords.contains("id-123"));
        
        // Test date extraction
        content = "Meeting scheduled for 2024-03-15 at 10:00 AM.";
        keywords = extractor.extractUniversalKeywords(content);
        assertTrue(keywords.contains("2024-03-15"));
        assertTrue(keywords.contains("2024"));
        
        // Test currency extraction
        content = "Budget: 50000 EUR, 60000 USD, 45000 GBP.";
        keywords = extractor.extractUniversalKeywords(content);
        assertTrue(keywords.contains("eur"));
        assertTrue(keywords.contains("usd"));
        assertTrue(keywords.contains("gbp"));
        
        // Test file extension extraction
        content = "Please review document.pdf and data.xlsx files.";
        keywords = extractor.extractUniversalKeywords(content);
        assertTrue(keywords.contains("document.pdf"));
        assertTrue(keywords.contains("data.xlsx"));
    }

    // =============== PERFORMANCE TESTS ===============
    
    @Test
    @Order(8)
    @DisplayName("Test search performance across different levels")
    void testSearchPerformance() {
        long startTime, endTime;
        
        // Warm up
        blockchain.searchBlocksFast("2024");
        
        // Test FAST_ONLY performance
        startTime = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            blockchain.searchBlocks("2024", SearchLevel.FAST_ONLY);
        }
        endTime = System.nanoTime();
        long fastTime = endTime - startTime;
        
        // Test INCLUDE_DATA performance
        startTime = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            blockchain.searchBlocks("2024", SearchLevel.INCLUDE_DATA);
        }
        endTime = System.nanoTime();
        long includeDataTime = endTime - startTime;
        
        // Test EXHAUSTIVE_OFFCHAIN performance
        startTime = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            blockchain.searchBlocks("2024", SearchLevel.EXHAUSTIVE_OFFCHAIN);
        }
        endTime = System.nanoTime();
        long exhaustiveTime = endTime - startTime;
        
        System.out.println("Performance Results (100 iterations):");
        System.out.println("FAST_ONLY: " + (fastTime / 1_000_000) + "ms");
        System.out.println("INCLUDE_DATA: " + (includeDataTime / 1_000_000) + "ms");
        System.out.println("EXHAUSTIVE_OFFCHAIN: " + (exhaustiveTime / 1_000_000) + "ms");
        
        // FAST_ONLY should be fastest (or at least not significantly slower)
        assertTrue(fastTime <= includeDataTime * 2, "FAST_ONLY should be reasonably fast compared to INCLUDE_DATA");
    }

    // =============== THREAD-SAFETY TESTS ===============
    
    @Test
    @Order(9)
    @DisplayName("Test concurrent search operations")
    @Execution(ExecutionMode.CONCURRENT)
    void testConcurrentSearchOperations() throws InterruptedException {
        int numThreads = 10;
        int searchesPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger totalResults = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);
        
        // Launch concurrent searches
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < searchesPerThread; j++) {
                        // Vary search terms and levels
                        String searchTerm = (j % 2 == 0) ? "2024" : "MEDICAL";
                        SearchLevel level = SearchLevel.values()[j % SearchLevel.values().length];
                        
                        List<Block> results = blockchain.searchBlocks(searchTerm, level);
                        totalResults.addAndGet(results.size());
                        
                        // Verify results are consistent
                        assertNotNull(results);
                        for (Block block : results) {
                            assertNotNull(block);
                            assertNotNull(block.getBlockNumber());
                        }
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete
        assertTrue(latch.await(30, TimeUnit.SECONDS), "All search threads should complete within 30 seconds");
        executor.shutdown();
        
        // Verify no errors occurred
        assertEquals(0, errors.get(), "No errors should occur during concurrent searches");
        assertTrue(totalResults.get() > 0, "Should find some results across all searches");
        
        System.out.println("Concurrent search test completed: " + 
                         (numThreads * searchesPerThread) + " searches across " + 
                         numThreads + " threads, " + totalResults.get() + " total results");
    }
    
    @Test
    @Order(10)
    @DisplayName("Test search during block creation")
    void testSearchDuringBlockCreation() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(3);
        AtomicInteger searchResults = new AtomicInteger(0);
        AtomicInteger blocksCreated = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);
        
        // Thread 1: Continuous searching
        executor.submit(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    List<Block> results = blockchain.searchBlocksFast("TEST");
                    searchResults.addAndGet(results.size());
                    Thread.sleep(10); // Small delay
                }
            } catch (Exception e) {
                errors.incrementAndGet();
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        
        // Thread 2: Creating blocks
        executor.submit(() -> {
            try {
                for (int i = 0; i < 20; i++) {
                    String[] keywords = {"TEST-" + i, "CONCURRENT"};
                    blockchain.addBlockWithKeywords(
                        "Test block " + i + " created during concurrent search test.",
                        keywords,
                        "TEST",
                        privateKey,
                        publicKey
                    );
                    blocksCreated.incrementAndGet();
                    Thread.sleep(50); // Simulate work
                }
            } catch (Exception e) {
                errors.incrementAndGet();
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        
        // Thread 3: Mixed search operations
        executor.submit(() -> {
            try {
                for (int i = 0; i < 50; i++) {
                    // Mix of different search operations
                    if (i % 3 == 0) {
                        blockchain.searchByCategory("TEST");
                    } else if (i % 3 == 1) {
                        blockchain.searchBlocks("CONCURRENT", SearchLevel.INCLUDE_DATA);
                    } else {
                        blockchain.searchBlocksComplete("2024");
                    }
                    Thread.sleep(20);
                }
            } catch (Exception e) {
                errors.incrementAndGet();
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        
        // Wait for all operations to complete
        assertTrue(latch.await(60, TimeUnit.SECONDS), "All operations should complete within 60 seconds");
        executor.shutdown();
        
        // Verify no errors occurred
        assertEquals(0, errors.get(), "No errors should occur during concurrent operations");
        assertTrue(blocksCreated.get() > 0, "Should have created some blocks");
        
        // Verify we can still search and find the new blocks
        List<Block> testBlocks = blockchain.searchByCategory("TEST");
        assertEquals(blocksCreated.get(), testBlocks.size(), "Should find all created TEST blocks");
        
        System.out.println("Concurrent operations test completed: " + 
                         blocksCreated.get() + " blocks created, " + 
                         searchResults.get() + " search results");
    }

    // =============== EDGE CASE TESTS ===============
    
    @Test
    @Order(11)
    @DisplayName("Test search edge cases")
    void testSearchEdgeCases() {
        // Test case-insensitive search
        List<Block> results1 = blockchain.searchBlocksFast("medical");
        List<Block> results2 = blockchain.searchBlocksFast("MEDICAL");
        List<Block> results3 = blockchain.searchBlocksFast("Medical");
        assertEquals(results1.size(), results2.size());
        assertEquals(results2.size(), results3.size());
        
        // Test search with special characters
        results1 = blockchain.searchBlocks("@company.com", SearchLevel.INCLUDE_DATA);
        assertFalse(results1.isEmpty());
        
        // Test search with numbers
        results1 = blockchain.searchBlocksFast("001");
        assertFalse(results1.isEmpty());
        
        // Test search with hyphens
        results1 = blockchain.searchBlocksFast("ECG-2024");
        assertFalse(results1.isEmpty());
        
        // Test partial matches
        results1 = blockchain.searchBlocks("Alpha", SearchLevel.INCLUDE_DATA);
        assertFalse(results1.isEmpty());
    }
    
    @Test
    @Order(12)
    @DisplayName("Test search result ordering and consistency")
    void testSearchResultConsistency() {
        // Test that search results are consistent across multiple calls
        List<Block> results1 = blockchain.searchBlocksFast("2024");
        List<Block> results2 = blockchain.searchBlocksFast("2024");
        List<Block> results3 = blockchain.searchBlocksFast("2024");
        
        assertEquals(results1.size(), results2.size());
        assertEquals(results2.size(), results3.size());
        
        // Verify same blocks are returned (order might vary due to Set operations)
        for (int i = 0; i < results1.size(); i++) {
            boolean found = false;
            for (Block block : results2) {
                if (block.getBlockNumber().equals(results1.get(i).getBlockNumber())) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Same blocks should be found in multiple searches");
        }
    }

    // =============== UTILITY METHODS ===============
    
    private void deleteFileIfExists(String fileName) {
        java.io.File file = new java.io.File(fileName);
        if (file.exists()) {
            file.delete();
        }
    }
    
    private void deleteDirectory(java.io.File directory) {
        if (!directory.exists()) return;
        
        java.io.File[] files = directory.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
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