package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;
import org.junit.jupiter.api.*;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Debug test to understand the block sequence issue
 */
public class DebugBlockSequenceTest {
    
    private static Blockchain blockchain;
    private static KeyPair authorizedKeyPair;
    private static final String ENCRYPTION_PASSWORD = "DebugTest123!@#";
    
    @BeforeAll
    static void setUpClass() {
        blockchain = new Blockchain();
        authorizedKeyPair = CryptoUtil.generateKeyPair();
    }
    
    @AfterAll
    static void tearDownClass() {
        JPAUtil.closeEntityManager();
    }
    
    @BeforeEach
    void cleanDatabase() {
        // Clean database before each test to ensure isolation
        blockchain.clearAndReinitialize();
        
        // Re-add authorized key after cleanup for test to work
        blockchain.addAuthorizedKey(
            CryptoUtil.publicKeyToString(authorizedKeyPair.getPublic()),
            "Test User"
        );
    }
    
    @Test
    void debugBlockSequenceIssue() {
        System.out.println("\n=== DEBUG: Block Sequence Issue ===");
        
        // Check initial state
        checkSequenceState("Initial state");
        
        // Create first public block
        System.out.println("\n--- Creating first public block ---");
        Block block1 = blockchain.addBlockAndReturn(
            "Public block #1",
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        
        assertNotNull(block1, "First block should not be null");
        assertEquals(Long.valueOf(1), block1.getBlockNumber(), "First block should have number 1");
        System.out.println("✅ First block created: " + block1.getBlockNumber());
        
        // Check sequence state after first block
        checkSequenceState("After first block");
        checkBlockState("After first block");
        
        // Create second block (encrypted)
        System.out.println("\n--- Creating second encrypted block ---");
        Block block2 = blockchain.addEncryptedBlock(
            "Encrypted block #2",
            ENCRYPTION_PASSWORD,
            authorizedKeyPair.getPrivate(),
            authorizedKeyPair.getPublic()
        );
        
        if (block2 != null) {
            System.out.println("✅ Second block created: " + block2.getBlockNumber());
            assertEquals(Long.valueOf(2), block2.getBlockNumber(), "Second block should have number 2");
        } else {
            System.out.println("❌ Second block creation failed!");
            checkSequenceState("After failed second block");
            checkBlockState("After failed second block");
        }
    }
    
    private void checkSequenceState(String label) {
        System.out.println("\n📊 " + label + " - Sequence State:");
        JPAUtil.beginTransaction();
        try {
            var em = JPAUtil.getEntityManager();
            var query = em.createQuery("SELECT s FROM BlockSequence s", 
                                     Class.forName("com.rbatllet.blockchain.entity.BlockSequence"));
            var sequences = query.getResultList();
            
            if (sequences.isEmpty()) {
                System.out.println("   No sequence found in database");
            } else {
                for (Object seq : sequences) {
                    // Use reflection to get nextValue
                    java.lang.reflect.Method getNextValue = seq.getClass().getMethod("getNextValue");
                    Long nextValue = (Long) getNextValue.invoke(seq);
                    System.out.println("   Sequence nextValue: " + nextValue);
                }
            }
            JPAUtil.commitTransaction();
        } catch (Exception e) {
            JPAUtil.rollbackTransaction();
            System.out.println("   Error checking sequence: " + e.getMessage());
        }
    }
    
    private void checkBlockState(String label) {
        System.out.println("\n📋 " + label + " - Block State:");
        try {
            long totalBlocks = blockchain.getBlockCount();
            System.out.println("   Total blocks in chain: " + totalBlocks);

            List<Block> allBlocks = new ArrayList<>();
            blockchain.processChainInBatches(batch -> allBlocks.addAll(batch), 1000);

            for (Block block : allBlocks) {
                System.out.printf("   Block #%d: ID=%d, Hash=%s, Encrypted=%s%n",
                    block.getBlockNumber(),
                    block.getId(),
                    block.getHash() != null ? block.getHash().substring(0, 8) + "..." : "null",
                    block.isDataEncrypted());
            }
        } catch (Exception e) {
            System.out.println("   Error checking blocks: " + e.getMessage());
        }
    }
}