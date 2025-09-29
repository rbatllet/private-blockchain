package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.entity.AuthorizedKey;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.validation.ChainValidationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DangerousDeleteAuthorizedKeyTest {

    private Blockchain blockchain;
    private KeyPair adminKeyPair;
    private String adminPublicKey;

    @BeforeEach
    void setUp() {
        blockchain = new Blockchain();
        // Clean up any existing data properly
        blockchain.clearAndReinitialize();

        // Create admin key pair for secure operations
        adminKeyPair = CryptoUtil.generateKeyPair();
        adminPublicKey = CryptoUtil.publicKeyToString(adminKeyPair.getPublic());
        blockchain.addAuthorizedKey(adminPublicKey, "Administrator");
    }


    @AfterEach
    void tearDown() {
        // Clean up after tests
        blockchain.clearAndReinitialize();
    }

    @Test
    void testCanDeleteAuthorizedKey_NonExistentKey() {
        // Test with a key that doesn't exist
        String nonExistentKey = "non-existent-key-123";
        
        Blockchain.KeyDeletionImpact impact = blockchain.canDeleteAuthorizedKey(nonExistentKey);
        
        assertFalse(impact.keyExists());
        assertEquals(0, impact.getAffectedBlocks());
        assertEquals("Key not found in database", impact.getMessage());
        assertFalse(impact.canSafelyDelete());
        assertFalse(impact.isSevereImpact());
    }

    @Test
    void testCanDeleteAuthorizedKey_SafeKey() {
        // Create a key that hasn't signed any blocks
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKey = CryptoUtil.publicKeyToString(keyPair.getPublic());
        
        blockchain.addAuthorizedKey(publicKey, "Test User Safe");
        
        Blockchain.KeyDeletionImpact impact = blockchain.canDeleteAuthorizedKey(publicKey);
        
        assertTrue(impact.keyExists());
        assertEquals(0, impact.getAffectedBlocks());
        assertEquals("Key can be safely deleted (no historical blocks)", impact.getMessage());
        assertTrue(impact.canSafelyDelete());
        assertFalse(impact.isSevereImpact());
    }

    @Test
    void testCanDeleteAuthorizedKey_KeyWithBlocks() {
        // Create a key and use it to sign blocks
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKey = CryptoUtil.publicKeyToString(keyPair.getPublic());
        
        blockchain.addAuthorizedKey(publicKey, "Test User With Blocks");
        
        // Add some blocks signed by this key
        blockchain.addBlock("Test block 1", keyPair.getPrivate(), keyPair.getPublic());
        blockchain.addBlock("Test block 2", keyPair.getPrivate(), keyPair.getPublic());
        
        Blockchain.KeyDeletionImpact impact = blockchain.canDeleteAuthorizedKey(publicKey);
        
        assertTrue(impact.keyExists());
        assertEquals(2, impact.getAffectedBlocks());
        assertEquals("Key deletion will affect 2 historical blocks", impact.getMessage());
        assertFalse(impact.canSafelyDelete());
        assertTrue(impact.isSevereImpact());
    }

    @Test
    void testCanDeleteAuthorizedKey_RevokedKeyWithBlocks() {
        // Create a key, use it to sign blocks, then revoke it
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKey = CryptoUtil.publicKeyToString(keyPair.getPublic());
        
        blockchain.addAuthorizedKey(publicKey, "Test User Revoked");
        blockchain.addBlock("Test block before revocation", keyPair.getPrivate(), keyPair.getPublic());
        blockchain.revokeAuthorizedKey(publicKey);
        
        Blockchain.KeyDeletionImpact impact = blockchain.canDeleteAuthorizedKey(publicKey);
        
        assertTrue(impact.keyExists());
        assertEquals(1, impact.getAffectedBlocks());
        assertTrue(impact.getMessage().contains("Key is revoked but still has 1 historical blocks"));
        assertFalse(impact.canSafelyDelete());
        assertTrue(impact.isSevereImpact());
    }

    @Test
    void testDeleteAuthorizedKey_SafeDeletion() {
        // Test safe deletion of a key with no blocks
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKey = CryptoUtil.publicKeyToString(keyPair.getPublic());
        
        blockchain.addAuthorizedKey(publicKey, "Test User Safe Delete");
        
        // Should be able to delete safely
        boolean deleted = blockchain.deleteAuthorizedKey(publicKey);
        assertTrue(deleted);
        
        // Verify key is gone
        Blockchain.KeyDeletionImpact impact = blockchain.canDeleteAuthorizedKey(publicKey);
        assertFalse(impact.keyExists());
    }

    @Test
    void testDeleteAuthorizedKey_BlockedDeletion() {
        // Test that deletion is blocked when key has signed blocks
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKey = CryptoUtil.publicKeyToString(keyPair.getPublic());
        
        blockchain.addAuthorizedKey(publicKey, "Test User Blocked Delete");
        blockchain.addBlock("Blocking block", keyPair.getPrivate(), keyPair.getPublic());
        
        // Should NOT be able to delete safely
        boolean deleted = blockchain.deleteAuthorizedKey(publicKey);
        assertFalse(deleted);
        
        // Verify key still exists
        assertTrue(blockchain.getAuthorizedKeys().stream()
            .anyMatch(key -> key.getPublicKey().equals(publicKey)));
    }

    @Test
    void testDangerouslyDeleteAuthorizedKey_SafeMode() {
        // Test dangerous deletion without force on a key with blocks (should fail)
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKey = CryptoUtil.publicKeyToString(keyPair.getPublic());
        
        blockchain.addAuthorizedKey(publicKey, "Test User Dangerous Safe");
        blockchain.addBlock("Protected block", keyPair.getPrivate(), keyPair.getPublic());
        
        // Should fail without force (using new secure method)
        String signature = CryptoUtil.createAdminSignature(publicKey, false, "Test deletion", adminKeyPair.getPrivate());
        boolean deleted = blockchain.dangerouslyDeleteAuthorizedKey(publicKey, false, "Test deletion", signature, adminPublicKey);
        assertFalse(deleted, "Should fail without force on key with blocks");
        
        // Verify key still exists
        assertTrue(blockchain.getAuthorizedKeys().stream()
            .anyMatch(key -> key.getPublicKey().equals(publicKey)));
    }

    @Test
    void testDangerouslyDeleteAuthorizedKey_ForceMode() {
        // Test dangerous deletion with force on a key with blocks (should succeed)
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKey = CryptoUtil.publicKeyToString(keyPair.getPublic());
        
        blockchain.addAuthorizedKey(publicKey, "Test User Dangerous Force");
        blockchain.addBlock("Block to be orphaned", keyPair.getPrivate(), keyPair.getPublic());
        
        // Verify blockchain is valid before deletion
        ChainValidationResult resultBefore = blockchain.validateChainDetailed();
        assertTrue(resultBefore.isFullyCompliant());
        assertTrue(resultBefore.isStructurallyIntact());
        
        // Should succeed with force (using new secure method)
        String signature = CryptoUtil.createAdminSignature(publicKey, true, "Forced test deletion", adminKeyPair.getPrivate());
        boolean deleted = blockchain.dangerouslyDeleteAuthorizedKey(publicKey, true, "Forced test deletion", signature, adminPublicKey);
        assertTrue(deleted, "Should succeed with force and valid admin signature");
        
        // Verify key is gone
        assertFalse(blockchain.getAuthorizedKeys().stream()
            .anyMatch(key -> key.getPublicKey().equals(publicKey)));
        
        // Verify blockchain validation changes after force deletion
        ChainValidationResult resultAfter = blockchain.validateChainDetailed();
        assertTrue(resultAfter.isStructurallyIntact()); // Chain structure is still intact
        assertFalse(resultAfter.isFullyCompliant()); // But not fully compliant due to orphaned blocks
        assertTrue(resultAfter.getRevokedBlocks() > 0); // Should have revoked blocks
    }

    @Test
    void testDangerouslyDeleteAuthorizedKey_NonExistentKey() {
        // Test deletion of non-existent key (using new secure method)
        String nonExistentKey = "non-existent-key-456";
        String signature = CryptoUtil.createAdminSignature(nonExistentKey, true, "Test non-existent", adminKeyPair.getPrivate());

        boolean deleted = blockchain.dangerouslyDeleteAuthorizedKey(nonExistentKey, true, "Test non-existent", signature, adminPublicKey);
        assertFalse(deleted, "Should fail on non-existent key");
    }

    @Test
    void testDangerouslyDeleteAuthorizedKey_KeyWithoutBlocks() {
        // Test dangerous deletion of a safe key (should succeed)
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        String publicKey = CryptoUtil.publicKeyToString(keyPair.getPublic());
        
        blockchain.addAuthorizedKey(publicKey, "Test User No Blocks");
        
        // Should succeed even without force since no blocks are affected (using new secure method)
        String signature = CryptoUtil.createAdminSignature(publicKey, false, "Test safe dangerous deletion", adminKeyPair.getPrivate());
        boolean deleted = blockchain.dangerouslyDeleteAuthorizedKey(publicKey, false, "Test safe dangerous deletion", signature, adminPublicKey);
        assertTrue(deleted, "Should succeed without force on key with no blocks");
        
        // Verify key is gone
        assertFalse(blockchain.getAuthorizedKeys().stream()
            .anyMatch(key -> key.getPublicKey().equals(publicKey)));
        
        // Verify blockchain is still valid
        ChainValidationResult result = blockchain.validateChainDetailed();
        assertTrue(result.isFullyCompliant());
        assertTrue(result.isStructurallyIntact());
    }

    @Test
    void testMultipleKeyDeletionScenario() {
        // Test complex scenario with multiple keys and blocks
        KeyPair keyPair1 = CryptoUtil.generateKeyPair();
        KeyPair keyPair2 = CryptoUtil.generateKeyPair();
        KeyPair keyPair3 = CryptoUtil.generateKeyPair();
        
        String publicKey1 = CryptoUtil.publicKeyToString(keyPair1.getPublic());
        String publicKey2 = CryptoUtil.publicKeyToString(keyPair2.getPublic());
        String publicKey3 = CryptoUtil.publicKeyToString(keyPair3.getPublic());
        
        // Add all keys
        blockchain.addAuthorizedKey(publicKey1, "User 1 - Has Blocks");
        blockchain.addAuthorizedKey(publicKey2, "User 2 - No Blocks");
        blockchain.addAuthorizedKey(publicKey3, "User 3 - Has Blocks");
        
        // Add blocks for key 1 and 3
        blockchain.addBlock("Block by user 1", keyPair1.getPrivate(), keyPair1.getPublic());
        blockchain.addBlock("Block by user 3", keyPair3.getPrivate(), keyPair3.getPublic());
        blockchain.addBlock("Another block by user 1", keyPair1.getPrivate(), keyPair1.getPublic());
        
        // Initial state: blockchain should be valid
        ChainValidationResult initialResult = blockchain.validateChainDetailed();
        assertTrue(initialResult.isFullyCompliant());
        assertTrue(initialResult.isStructurallyIntact());
        
        // Test impact analysis
        Blockchain.KeyDeletionImpact impact1 = blockchain.canDeleteAuthorizedKey(publicKey1);
        Blockchain.KeyDeletionImpact impact2 = blockchain.canDeleteAuthorizedKey(publicKey2);
        Blockchain.KeyDeletionImpact impact3 = blockchain.canDeleteAuthorizedKey(publicKey3);
        
        assertTrue(impact1.isSevereImpact());
        assertEquals(2, impact1.getAffectedBlocks());
        
        assertFalse(impact2.isSevereImpact());
        assertEquals(0, impact2.getAffectedBlocks());
        assertTrue(impact2.canSafelyDelete());
        
        assertTrue(impact3.isSevereImpact());
        assertEquals(1, impact3.getAffectedBlocks());
        
        // Delete safe key (key 2)
        assertTrue(blockchain.deleteAuthorizedKey(publicKey2));
        ChainValidationResult afterSafeDeletion = blockchain.validateChainDetailed();
        assertTrue(afterSafeDeletion.isFullyCompliant()); // Should still be valid
        assertTrue(afterSafeDeletion.isStructurallyIntact());
        
        // Try to delete key with blocks (should fail without force)
        assertFalse(blockchain.deleteAuthorizedKey(publicKey1));
        ChainValidationResult afterFailedDeletion = blockchain.validateChainDetailed();
        assertTrue(afterFailedDeletion.isFullyCompliant()); // Should still be valid
        assertTrue(afterFailedDeletion.isStructurallyIntact());
        
        // Force delete key with blocks (using new secure method)
        String signature = CryptoUtil.createAdminSignature(publicKey1, true, "Test force deletion", adminKeyPair.getPrivate());
        assertTrue(blockchain.dangerouslyDeleteAuthorizedKey(publicKey1, true, "Test force deletion", signature, adminPublicKey));
        ChainValidationResult afterForceDeletion = blockchain.validateChainDetailed();
        assertTrue(afterForceDeletion.isStructurallyIntact()); // Structure still intact
        assertFalse(afterForceDeletion.isFullyCompliant()); // But not fully compliant
        assertTrue(afterForceDeletion.getRevokedBlocks() > 0); // Should have revoked blocks
        
        // Verify only key 3 and admin remain
        List<AuthorizedKey> remainingKeys = blockchain.getAuthorizedKeys();
        assertEquals(2, remainingKeys.size());
        // Should have admin and publicKey3
        assertTrue(remainingKeys.stream().anyMatch(key -> key.getPublicKey().equals(adminPublicKey)));
        assertTrue(remainingKeys.stream().anyMatch(key -> key.getPublicKey().equals(publicKey3)));
    }

    @Test
    void testKeyDeletionImpactToString() {
        // Test the toString method of KeyDeletionImpact
        Blockchain.KeyDeletionImpact impact1 = new Blockchain.KeyDeletionImpact(true, 0, "Safe to delete");
        Blockchain.KeyDeletionImpact impact2 = new Blockchain.KeyDeletionImpact(true, 5, "Has blocks");
        Blockchain.KeyDeletionImpact impact3 = new Blockchain.KeyDeletionImpact(false, 0, "Key not found");
        
        assertTrue(impact1.toString().contains("canSafelyDelete=true"));
        assertTrue(impact2.toString().contains("affectedBlocks=5"));
        assertTrue(impact3.toString().contains("exists=false"));
        
        assertTrue(impact1.canSafelyDelete());
        assertFalse(impact2.canSafelyDelete());
        assertFalse(impact3.canSafelyDelete());
        
        assertFalse(impact1.isSevereImpact());
        assertTrue(impact2.isSevereImpact());
        assertFalse(impact3.isSevereImpact());
    }
}
