package com.rbatllet.blockchain.security;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.JPAUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to verify JPA-level protection of hash-critical fields.
 * Uses @Column(updatable=false) for immutable fields.
 * 
 * @author rbatllet
 * @since 1.0.5
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BlockImmutabilityTest {
    
    private static final Logger logger = LoggerFactory.getLogger(BlockImmutabilityTest.class);
    private static Blockchain blockchain;
    
    @BeforeAll
    public static void setup() {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();
        logger.info("✅ Blockchain initialized for immutability tests");
    }
    
    @AfterAll
    public static void cleanup() {
        if (blockchain != null) {
            blockchain.completeCleanupForTestsWithBackups();
            blockchain.shutdown();
        }
    }
    
    @Test
    @Order(1)
    @DisplayName("1. Safe metadata update should succeed")
    public void testSafeMetadataUpdate() {
        logger.info("🧪 TEST 1: Safe metadata update with Blockchain API");
        
        Block genesisBlock = blockchain.getBlock(0L);
        assertNotNull(genesisBlock, "Genesis block should exist");
        
        // Modify SAFE fields via API
        genesisBlock.setCustomMetadata("{\"test\": \"metadata\"}");
        genesisBlock.setManualKeywords("test, keywords");
        genesisBlock.setContentCategory("TEST");
        
        boolean updated = blockchain.updateBlock(genesisBlock);
        assertTrue(updated, "Safe metadata update should succeed");
        
        // Verify changes persisted
        Block updatedBlock = blockchain.getBlock(0L);
        assertEquals("{\"test\": \"metadata\"}", updatedBlock.getCustomMetadata());
        assertEquals("test, keywords", updatedBlock.getManualKeywords());
        assertEquals("TEST", updatedBlock.getContentCategory());
        
        logger.info("✅ Safe metadata update succeeded");
    }
    
    @Test
    @Order(2)
    @DisplayName("2. JPA ignores updates to immutable fields (updatable=false)")
    public void testImmutableFieldsAreProtected() {
        logger.info("🧪 TEST 2: Verify JPA ignores updates to hash-critical fields");
        
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            
            // Get genesis block
            Block block = em.createQuery(
                "SELECT b FROM Block b WHERE b.blockNumber = :blockNumber", Block.class)
                .setParameter("blockNumber", 0L)
                .getSingleResult();
            
            String originalData = block.getData();
            String originalHash = block.getHash();
            
            logger.info("Original data: {}", originalData);
            logger.info("Original hash: {}", originalHash);
            
            // Attempt to modify immutable fields (JPA should SILENTLY IGNORE these)
            block.setData("MODIFIED DATA");
            block.setHash("FAKE_HASH");
            
            em.getTransaction().commit();
            
            // Re-fetch block to verify changes were NOT persisted
            em.clear(); // Clear persistence context
            Block reloadedBlock = em.createQuery(
                "SELECT b FROM Block b WHERE b.blockNumber = :blockNumber", Block.class)
                .setParameter("blockNumber", 0L)
                .getSingleResult();
            
            // JPA with updatable=false should have IGNORED the changes
            assertEquals(originalData, reloadedBlock.getData(), 
                        "Data should be unchanged (JPA ignored update due to updatable=false)");
            assertEquals(originalHash, reloadedBlock.getHash(), 
                        "Hash should be unchanged (JPA ignored update due to updatable=false)");
            
            logger.info("✅ JPA successfully protected immutable fields");
            logger.info("✅ Data remains: {}", reloadedBlock.getData());
            logger.info("✅ Hash remains: {}", reloadedBlock.getHash());
            
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }
    
    @Test
    @Order(3)
    @DisplayName("3. Blockchain validation confirms integrity after test")
    public void testBlockchainIntegrityMaintained() {
        logger.info("🧪 TEST 3: Final blockchain validation");
        
        var validation = blockchain.validateChainDetailed();
        assertTrue(validation.isStructurallyIntact() && validation.isFullyCompliant(), 
                  "Blockchain should still be valid - all hash-critical fields protected by JPA");
        
        logger.info("✅ Blockchain integrity maintained with JPA protection");
    }
}
