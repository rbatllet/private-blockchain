package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.validation.ChainValidationResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test de consistència de la blockchain - Verificació d'inconsistències críticas
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Critical Blockchain Consistency Tests")
class CriticalConsistencyTest {

    private KeyPair aliceKeyPair;
    private KeyPair bobKeyPair;
    private String alicePublicKey;
    private String bobPublicKey;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // The generateKeyPair() method now uses the new hierarchical key system internally
        aliceKeyPair = CryptoUtil.generateKeyPair();
        bobKeyPair = CryptoUtil.generateKeyPair();
        alicePublicKey = CryptoUtil.publicKeyToString(aliceKeyPair.getPublic());
        bobPublicKey = CryptoUtil.publicKeyToString(bobKeyPair.getPublic());
        
        clearDatabase();
    }
    
    @AfterEach
    void tearDown() {
        // Clean database after each test to ensure test isolation
        clearDatabase();
    }
    
    private void clearDatabase() {
        try {
            // Use the standard clearAndReinitialize method for consistency
            Blockchain tempBlockchain = new Blockchain();
            tempBlockchain.clearAndReinitialize();
            Thread.sleep(50);
        } catch (Exception e) {
            System.err.println("Warning: Could not clear database: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    @DisplayName("🔥 Test Concurrency Issues")
    void testConcurrencyIssues() throws Exception {
        Blockchain blockchain = new Blockchain();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        try {
            assertTrue(blockchain.addAuthorizedKey(alicePublicKey, "Alice"));
            
            // Test concurrent operations
            CompletableFuture<Boolean> future1 = CompletableFuture.supplyAsync(() -> 
                blockchain.addBlock("Block 1", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()), executor);
            
            CompletableFuture<Boolean> future2 = CompletableFuture.supplyAsync(() -> 
                blockchain.addAuthorizedKey(bobPublicKey, "Bob"), executor);
            
            CompletableFuture<Boolean> future3 = CompletableFuture.supplyAsync(() -> 
                blockchain.addBlock("Block 2", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()), executor);
            
            // All should succeed or fail gracefully
            Boolean result1 = future1.get(10, TimeUnit.SECONDS);
            Boolean result2 = future2.get(10, TimeUnit.SECONDS);
            Boolean result3 = future3.get(10, TimeUnit.SECONDS);
            
            assertTrue(result1, "Alice's first block should succeed");
            assertTrue(result2, "Bob's authorization should succeed");
            assertTrue(result3, "Alice's second block should succeed");
            
            // CRITICAL: Chain must remain valid
            ChainValidationResult concurrencyResult = blockchain.validateChainDetailed();
            assertTrue(concurrencyResult.isFullyCompliant(), "Chain should be fully compliant after concurrency test");
            assertTrue(concurrencyResult.isStructurallyIntact(), "Chain should be structurally intact after concurrency test");
            
            System.out.println("✅ Concurrency test passed - No pool exhaustion");
            
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @Order(2)
    @DisplayName("🔥 Test Rapid Key Cycles")
    void testRapidKeyCycles() {
        Blockchain blockchain = new Blockchain();
        
        // Rapid authorization/revocation cycles
        for (int i = 0; i < 10; i++) {
            assertTrue(blockchain.addAuthorizedKey(alicePublicKey, "Alice-" + i));
            assertTrue(blockchain.addBlock("Cycle " + i, aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
            assertTrue(blockchain.revokeAuthorizedKey(alicePublicKey));
            assertFalse(blockchain.addBlock("Should fail " + i, aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        }
        
        ChainValidationResult rapidCyclesResult = blockchain.validateChainDetailed();
        assertTrue(rapidCyclesResult.isStructurallyIntact(), "Chain should be structurally intact after rapid key cycles");
        // Note: May not be fully compliant due to revoked keys, but structure should be intact
        System.out.println("✅ Rapid key cycles test passed");
    }

    @Test
    @Order(3)
    @DisplayName("🔥 Test Import/Export Edge Cases")
    void testImportExportEdgeCases() throws Exception {
        Blockchain original = new Blockchain();
        
        // Create complex scenario
        assertTrue(original.addAuthorizedKey(alicePublicKey, "Alice"));
        assertTrue(original.addBlock("Block 1", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        assertTrue(original.revokeAuthorizedKey(alicePublicKey));
        assertTrue(original.addAuthorizedKey(alicePublicKey, "Alice-Reauth"));
        assertTrue(original.addBlock("Block 2", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        
        // Export/Import
        File exportFile = tempDir.resolve("edge_test.json").toFile();
        assertTrue(original.exportChain(exportFile.getAbsolutePath()));
        
        Blockchain imported = new Blockchain();
        assertTrue(imported.importChain(exportFile.getAbsolutePath()));
        
        // CRITICAL: Imported chain must be consistent
        ChainValidationResult importedResult = imported.validateChainDetailed();
        assertTrue(importedResult.isFullyCompliant(), "Imported chain must be fully compliant");
        assertTrue(importedResult.isStructurallyIntact(), "Imported chain must be structurally intact");
        assertTrue(imported.addBlock("Post-import", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        
        System.out.println("✅ Import/Export edge cases passed");
    }

    @Test
    @Order(4)
    @DisplayName("🔥 Test Rollback Consistency")
    void testRollbackConsistency() {
        Blockchain blockchain = new Blockchain();
        
        assertTrue(blockchain.addAuthorizedKey(alicePublicKey, "Alice"));
        assertTrue(blockchain.addAuthorizedKey(bobPublicKey, "Bob"));
        
        // Create blocks
        for (int i = 0; i < 10; i++) {
            assertTrue(blockchain.addBlock("Alice-" + i, aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
            assertTrue(blockchain.addBlock("Bob-" + i, bobKeyPair.getPrivate(), bobKeyPair.getPublic()));
        }
        
        // Revoke Alice
        assertTrue(blockchain.revokeAuthorizedKey(alicePublicKey));
        
        // Rollback some blocks
        assertTrue(blockchain.rollbackBlocks(5L));
        
        // CRITICAL: Keys should retain their status after rollback
        assertFalse(blockchain.addBlock("Alice should fail", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        assertTrue(blockchain.addBlock("Bob should work", bobKeyPair.getPrivate(), bobKeyPair.getPublic()));
        
        ChainValidationResult rollbackResult = blockchain.validateChainDetailed();
        assertTrue(rollbackResult.isFullyCompliant(), "Chain should be fully compliant after rollback");
        assertTrue(rollbackResult.isStructurallyIntact(), "Chain should be structurally intact after rollback");
        System.out.println("✅ Rollback consistency test passed");
    }

    @Test
    @Order(5)
    @DisplayName("🔥 Test Mass Operations")
    void testMassOperations() {
        Blockchain blockchain = new Blockchain();
        assertTrue(blockchain.addAuthorizedKey(alicePublicKey, "Alice"));
        
        // Create many blocks
        for (int i = 0; i < 50; i++) {
            assertTrue(blockchain.addBlock("Mass-" + i, aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        }
        
        ChainValidationResult massOpsResult = blockchain.validateChainDetailed();
        assertTrue(massOpsResult.isFullyCompliant(), "Chain should be fully compliant after mass operations");
        assertTrue(massOpsResult.isStructurallyIntact(), "Chain should be structurally intact after mass operations");
        assertEquals(51, blockchain.getBlockCount()); // Genesis + 50
        
        // Mass rollback
        assertTrue(blockchain.rollbackBlocks(25L));
        ChainValidationResult massRollbackResult = blockchain.validateChainDetailed();
        assertTrue(massRollbackResult.isFullyCompliant(), "Chain should be fully compliant after mass rollback");
        assertTrue(massRollbackResult.isStructurallyIntact(), "Chain should be structurally intact after mass rollback");
        
        System.out.println("✅ Mass operations test passed");
    }

    @Test
    @Order(6)
    @DisplayName("🔥 Test Error State Recovery")
    void testErrorStateRecovery() {
        Blockchain blockchain = new Blockchain();
        
        long initialBlocks = blockchain.getBlockCount();
        int initialKeys = blockchain.getAuthorizedKeys().size();
        
        // Try operations that should fail
        assertFalse(blockchain.addBlock("No auth", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        assertFalse(blockchain.revokeAuthorizedKey("non-existent"));
        assertFalse(blockchain.addAuthorizedKey(null, "Invalid"));
        
        // State should be unchanged
        assertEquals(initialBlocks, blockchain.getBlockCount());
        assertEquals(initialKeys, blockchain.getAuthorizedKeys().size());
        ChainValidationResult errorRecoveryResult = blockchain.validateChainDetailed();
        assertTrue(errorRecoveryResult.isFullyCompliant(), "Chain should remain fully compliant after failed operations");
        assertTrue(errorRecoveryResult.isStructurallyIntact(), "Chain should remain structurally intact after failed operations");
        
        System.out.println("✅ Error state recovery test passed");
    }

    @Test
    @Order(7)
    @DisplayName("🏁 Final Comprehensive Test")
    void testFinalComprehensive() throws Exception {
        System.out.println("🔍 Running final comprehensive consistency test...");
        
        Blockchain blockchain = new Blockchain();
        
        // Complex scenario combining all operations
        assertTrue(blockchain.addAuthorizedKey(alicePublicKey, "Alice"));
        assertTrue(blockchain.addAuthorizedKey(bobPublicKey, "Bob"));
        
        // Interleaved blocks
        for (int i = 0; i < 5; i++) {
            assertTrue(blockchain.addBlock("Alice-" + i, aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
            assertTrue(blockchain.addBlock("Bob-" + i, bobKeyPair.getPrivate(), bobKeyPair.getPublic()));
        }
        
        // Revoke one key
        assertTrue(blockchain.revokeAuthorizedKey(alicePublicKey));
        assertTrue(blockchain.addBlock("Bob continues", bobKeyPair.getPrivate(), bobKeyPair.getPublic()));
        
        // Export/Import
        File exportFile = tempDir.resolve("final_test.json").toFile();
        assertTrue(blockchain.exportChain(exportFile.getAbsolutePath()));
        
        Blockchain imported = new Blockchain();
        assertTrue(imported.importChain(exportFile.getAbsolutePath()));
        
        // Operations on imported chain
        assertTrue(imported.addBlock("Bob post-import", bobKeyPair.getPrivate(), bobKeyPair.getPublic()));
        assertFalse(imported.addBlock("Alice should fail", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        
        // Rollback
        assertTrue(imported.rollbackBlocks(2L));
        
        // Re-authorization
        assertTrue(imported.addAuthorizedKey(alicePublicKey, "Alice-Reauth"));
        assertTrue(imported.addBlock("Alice works again", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        
        // FINAL CRITICAL VALIDATION
        ChainValidationResult finalResult = imported.validateChainDetailed();
        assertTrue(finalResult.isFullyCompliant(), "Final chain must be fully compliant");
        assertTrue(finalResult.isStructurallyIntact(), "Final chain must be structurally intact");
        
        System.out.println("🎉 FINAL COMPREHENSIVE TEST PASSED!");
        System.out.println("   Chain validation: " + finalResult.getSummary());
        System.out.println("   Chain blocks: " + imported.getBlockCount());
        System.out.println("   Active keys: " + imported.getAuthorizedKeys().size());
        System.out.println("   ✅ ALL CONSISTENCY CHECKS PASSED!");
    }
}