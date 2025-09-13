package com.rbatllet.blockchain.performance;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class PerformanceOptimizationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceOptimizationTest.class);
    
    private UserFriendlyEncryptionAPI api;
    private Blockchain blockchain;
    private KeyPair keyPair;
    private String password;
    
    @BeforeEach
    void setUp() {
        try {
            blockchain = new Blockchain();
            api = new UserFriendlyEncryptionAPI(blockchain);
            keyPair = CryptoUtil.generateKeyPair();
            password = "testPassword123";
            
            // Set up default credentials
            api.setDefaultCredentials("TestUser", keyPair);
            
            // Initialize SearchSpecialistAPI before creating test data
            blockchain.initializeAdvancedSearch(password);
            blockchain.getSearchSpecialistAPI().initializeWithBlockchain(blockchain, password, keyPair.getPrivate());
            
            // Create a moderate amount of test data
            createTestBlocks(50);
            
        } catch (Exception e) {
            logger.error("❌ Setup failed", e);
            fail("Setup failed: " + e.getMessage());
        }
    }
    
    private void createTestBlocks(int count) {
        try {
            for (int i = 0; i < count; i++) {
                String data = "Test data for block " + i + " with keywords: blockchain, crypto, test, data" + i;
                String[] keywords = {"blockchain", "crypto", "test", "data" + i};
                
                Block block = api.storeSearchableData(data, password, keywords);
                assertNotNull(block, "Block " + i + " should be created successfully");
            }
            logger.info("✅ Created {} test blocks", count);
        } catch (Exception e) {
            logger.error("❌ Failed to create test blocks", e);
            fail("Failed to create test blocks: " + e.getMessage());
        }
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testPaginatedBlockRetrieval() {
        long startTime = System.currentTimeMillis();
        
        // Test paginated retrieval
        List<Block> page1 = blockchain.getBlocksPaginated(0, 10);
        List<Block> page2 = blockchain.getBlocksPaginated(10, 10);
        List<Block> page3 = blockchain.getBlocksPaginated(20, 10);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Assertions
        assertEquals(10, page1.size(), "Page 1 should have 10 blocks");
        assertEquals(10, page2.size(), "Page 2 should have 10 blocks");
        assertEquals(10, page3.size(), "Page 3 should have 10 blocks");
        
        // Verify no duplicates between pages
        assertTrue(page1.get(0).getBlockNumber() < page2.get(0).getBlockNumber(), 
                  "Page 1 should have lower block numbers than page 2");
        assertTrue(page2.get(0).getBlockNumber() < page3.get(0).getBlockNumber(), 
                  "Page 2 should have lower block numbers than page 3");
        
        logger.info("✅ Paginated retrieval completed in {}ms", duration);
        assertTrue(duration < 5000, "Paginated retrieval should complete within 5 seconds");
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testLightweightBlockRetrieval() {
        long startTime = System.currentTimeMillis();
        
        // Test lightweight retrieval (without off-chain data)
        List<Block> lightweightBlocks = blockchain.getAllBlocksLightweight();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Assertions
        assertTrue(lightweightBlocks.size() >= 50, "Should have at least 50 blocks");
        
        logger.info("✅ Lightweight retrieval of {} blocks completed in {}ms", 
                   lightweightBlocks.size(), duration);
        assertTrue(duration < 3000, "Lightweight retrieval should complete within 3 seconds");
    }
    
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testOptimizedSimilaritySearch() {
        long startTime = System.currentTimeMillis();
        
        // Test optimized search using search terms
        List<Block> similarBlocks = api.searchAndDecryptByTerms(
            new String[]{"blockchain", "crypto", "test"}, password, 10);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Assertions
        assertNotNull(similarBlocks, "Similar blocks should not be null");
        assertFalse(similarBlocks.isEmpty(), "Should find similar blocks");
        
        logger.info("✅ Optimized similarity search found {} blocks in {}ms", 
                   similarBlocks.size(), duration);
        assertTrue(duration < 10000, "Optimized similarity search should complete within 10 seconds");
    }
    
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testBlockCountPerformance() {
        long startTime = System.currentTimeMillis();
        
        // Test block count operation
        long blockCount = blockchain.getBlockCount();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Assertions
        assertTrue(blockCount >= 50, "Should have at least 50 blocks");
        
        logger.info("✅ Block count ({}) retrieved in {}ms", blockCount, duration);
        assertTrue(duration < 1000, "Block count should complete within 1 second");
    }
    
    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void testPerformanceComparisonGetAllVsPaginated() {
        logger.info("📊 Performance comparison: getAllBlocks vs paginated approach");
        
        // Test getAllBlocks performance
        long startTime1 = System.currentTimeMillis();
        List<Block> allBlocks = blockchain.getAllBlocks();
        long endTime1 = System.currentTimeMillis();
        long getAllDuration = endTime1 - startTime1;
        
        // Test paginated approach
        long startTime2 = System.currentTimeMillis();
        List<Block> paginatedBlocks = blockchain.getBlocksPaginated(0, allBlocks.size());
        long endTime2 = System.currentTimeMillis();
        long paginatedDuration = endTime2 - startTime2;
        
        // Assertions
        assertEquals(allBlocks.size(), paginatedBlocks.size(), 
                    "Both approaches should return same number of blocks");
        
        logger.info("📊 getAllBlocks: {}ms, paginated: {}ms", getAllDuration, paginatedDuration);
        logger.info("📊 Performance difference: {}ms", Math.abs(getAllDuration - paginatedDuration));
        
        // For small datasets, performance should be similar
        // This test mainly validates that pagination works correctly
        assertTrue(getAllDuration < 10000, "getAllBlocks should complete within 10 seconds");
        assertTrue(paginatedDuration < 10000, "Paginated approach should complete within 10 seconds");
    }
    
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testMemoryEfficiencyWithBatches() {
        logger.info("📊 Testing memory efficiency with batch processing");
        
        // Get runtime for memory monitoring
        Runtime runtime = Runtime.getRuntime();
        
        // Force garbage collection to get baseline
        System.gc();
        long baselineMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Test batch processing approach
        long startTime = System.currentTimeMillis();
        
        final int BATCH_SIZE = 10;
        long totalBlocks = blockchain.getBlockCount();
        int processedBlocks = 0;
        
        for (int offset = 0; offset < totalBlocks; offset += BATCH_SIZE) {
            List<Block> batch = blockchain.getBlocksPaginated(offset, BATCH_SIZE);
            processedBlocks += batch.size();
            
            // Simulate processing
            for (Block block : batch) {
                assertNotNull(block.getData(), "Block data should not be null");
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Get memory after processing
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = finalMemory - baselineMemory;
        
        // Assertions
        assertEquals(totalBlocks, processedBlocks, "Should process all blocks");
        
        logger.info("📊 Batch processing: {} blocks in {}ms, memory used: {} bytes", 
                   processedBlocks, duration, memoryUsed);
        
        assertTrue(duration < 10000, "Batch processing should complete within 10 seconds");
        assertTrue(memoryUsed < 50_000_000, "Memory usage should be reasonable (< 50MB)");
    }
}