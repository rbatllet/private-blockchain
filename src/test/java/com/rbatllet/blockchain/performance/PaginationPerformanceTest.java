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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class PaginationPerformanceTest {
    
    private static final Logger logger = LoggerFactory.getLogger(PaginationPerformanceTest.class);
    
    private Blockchain blockchain;
    private UserFriendlyEncryptionAPI api;
    private KeyPair keyPair;
    private String password;
    
    @BeforeEach
    void setUp() {
        blockchain = new Blockchain();
        api = new UserFriendlyEncryptionAPI(blockchain);
        keyPair = CryptoUtil.generateKeyPair();
        password = "testPassword123";
        
        // Set up default credentials
        api.setDefaultCredentials("TestUser", keyPair);
        
        // Initialize SearchSpecialistAPI before creating test data
        try {
            blockchain.initializeAdvancedSearch(password);
            blockchain.getSearchSpecialistAPI().initializeWithBlockchain(blockchain, password, keyPair.getPrivate());
        } catch (Exception e) {
            logger.error("⚠️ SearchSpecialistAPI initialization failed", e);
        }
        
        // Create just a few test blocks
        createTestBlocks(10);
    }
    
    private void createTestBlocks(int count) {
        for (int i = 0; i < count; i++) {
            String data = "Test block " + i + " data";
            String[] keywords = {"test", "block" + i};
            
            Block block = api.storeSearchableData(data, password, keywords);
            assertNotNull(block, "Block should be created");
        }
        logger.info("✅ Created {} test blocks", count);
    }
    
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testPaginatedBlockRetrieval() {
        // Test basic pagination functionality
        List<Block> page1 = blockchain.getBlocksPaginated(0, 5);
        List<Block> page2 = blockchain.getBlocksPaginated(5, 5);
        
        assertEquals(5, page1.size(), "First page should have 5 blocks");
        assertEquals(5, page2.size(), "Second page should have 5 blocks");
        
        // Verify ordering
        assertTrue(page1.get(0).getBlockNumber() < page2.get(0).getBlockNumber(), 
                  "Pages should be ordered by block number");
        
        logger.info("✅ Pagination test passed");
    }
    
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testBlockCountPerformance() {
        long blockCount = blockchain.getBlockCount();
        assertTrue(blockCount >= 10, "Should have at least 10 blocks");
        
        logger.info("✅ Block count: {}", blockCount);
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testPerformanceComparison() {
        // Compare sequential retrieval vs paginated
        long start1 = System.currentTimeMillis();
        List<Block> allBlocks = new ArrayList<>();
        long blockCount = blockchain.getBlockCount();
        for (long i = 0; i < blockCount; i++) {
            Block block = blockchain.getBlock(i);
            if (block != null) {
                allBlocks.add(block);
            }
        }
        long time1 = System.currentTimeMillis() - start1;

        long start2 = System.currentTimeMillis();
        List<Block> paginatedBlocks = blockchain.getBlocksPaginated(0, (int) blockchain.getBlockCount());
        long time2 = System.currentTimeMillis() - start2;

        assertEquals(allBlocks.size(), paginatedBlocks.size(),
                    "Both methods should return same number of blocks");

        logger.info("📊 Sequential retrieval: {}ms, paginated: {}ms", time1, time2);
        logger.info("✅ Performance comparison completed");
    }
}