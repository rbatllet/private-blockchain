package com.rbatllet.blockchain.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.security.KeyPair;

import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.core.Blockchain;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security test to verify protection against owner name duplication attacks.
 *
 * This test validates that the blockchain prevents an ADMIN from creating
 * a new authorized key with an existing user's owner name, which would
 * effectively replace their identity and break import/export functionality.
 */
public class OwnerNameDuplicationSecurityTest {

    private Blockchain blockchain;
    private KeyPair genesisKeyPair;
    private String aliceOwner = "AliceSecurityTest";

    @BeforeEach
    void setUp() {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();

        // Create genesis admin
        genesisKeyPair = CryptoUtil.generateKeyPair();
        String genesisPublicKey = CryptoUtil.publicKeyToString(genesisKeyPair.getPublic());
        blockchain.createBootstrapAdmin(genesisPublicKey, "GenesisAdmin");
    }

    @Test
    @DisplayName("🔒 SECURITY: Prevent owner name duplication attack by ADMIN")
    void testPreventOwnerNameDuplicationAttack() {
        // Step 1: Genesis creates Alice (legitimate user)
        KeyPair aliceKeyPair = CryptoUtil.generateKeyPair();
        String alicePublicKey = CryptoUtil.publicKeyToString(aliceKeyPair.getPublic());

        boolean aliceCreated = blockchain.addAuthorizedKey(
            alicePublicKey,
            aliceOwner,
            genesisKeyPair,
            UserRole.USER
        );
        assertTrue(aliceCreated, "Alice should be created successfully");

        // Step 2: Genesis creates Bob (malicious ADMIN)
        KeyPair bobAdminKeyPair = CryptoUtil.generateKeyPair();
        String bobPublicKey = CryptoUtil.publicKeyToString(bobAdminKeyPair.getPublic());

        boolean bobCreated = blockchain.addAuthorizedKey(
            bobPublicKey,
            "BobMaliciousAdmin",
            genesisKeyPair,
            UserRole.ADMIN
        );
        assertTrue(bobCreated, "Bob admin should be created successfully");

        // Step 3: Bob (ADMIN) tries to create a new key with Alice's owner name
        // This is an identity substitution attack
        KeyPair bobMaliciousKeyPair = CryptoUtil.generateKeyPair();
        String bobMaliciousPublicKey = CryptoUtil.publicKeyToString(bobMaliciousKeyPair.getPublic());

        // This should throw SecurityException
        SecurityException exception = assertThrows(
            SecurityException.class,
            () -> blockchain.addAuthorizedKey(
                bobMaliciousPublicKey,
                aliceOwner,  // ← Trying to use Alice's name!
                bobAdminKeyPair,
                UserRole.USER
            ),
            "Should throw SecurityException when trying to create duplicate owner name"
        );

        // Verify the error message mentions identity substitution
        assertTrue(exception.getMessage().contains("already exists with an active authorized key"),
                  "Error message should mention existing key");
        assertTrue(exception.getMessage().contains("identity substitution"),
                  "Error message should mention identity substitution attack");
    }

    @Test
    @DisplayName("🔒 SECURITY: Allow re-authorization after revocation")
    void testAllowReauthorizationAfterRevocation() {
        // Step 1: Create Alice
        KeyPair aliceKeyPair = CryptoUtil.generateKeyPair();
        String alicePublicKey = CryptoUtil.publicKeyToString(aliceKeyPair.getPublic());

        blockchain.addAuthorizedKey(
            alicePublicKey,
            aliceOwner,
            genesisKeyPair,
            UserRole.USER
        );

        // Step 2: Revoke Alice's key
        boolean revoked = blockchain.revokeAuthorizedKey(alicePublicKey);
        assertTrue(revoked, "Alice's key should be revoked");

        // Step 3: Now should be able to create a NEW Alice with different key
        KeyPair aliceNewKeyPair = CryptoUtil.generateKeyPair();
        String aliceNewPublicKey = CryptoUtil.publicKeyToString(aliceNewKeyPair.getPublic());

        // This should succeed because the old key is revoked (not active)
        boolean recreated = blockchain.addAuthorizedKey(
            aliceNewPublicKey,
            aliceOwner,  // ← Same owner name is OK after revocation
            genesisKeyPair,
            UserRole.USER
        );

        assertTrue(recreated, "Should allow re-authorization with same owner name after revocation");
    }

    @Test
    @DisplayName("🔒 SECURITY: Verify getAuthorizedKeyByOwner returns the active key")
    void testGetAuthorizedKeyByOwnerReturnsActiveKey() {
        // Create and revoke Alice's first key
        KeyPair aliceKeyPair1 = CryptoUtil.generateKeyPair();
        String alicePublicKey1 = CryptoUtil.publicKeyToString(aliceKeyPair1.getPublic());

        blockchain.addAuthorizedKey(alicePublicKey1, aliceOwner, genesisKeyPair, UserRole.USER);
        blockchain.revokeAuthorizedKey(alicePublicKey1);

        // Create Alice's second key
        KeyPair aliceKeyPair2 = CryptoUtil.generateKeyPair();
        String alicePublicKey2 = CryptoUtil.publicKeyToString(aliceKeyPair2.getPublic());

        blockchain.addAuthorizedKey(alicePublicKey2, aliceOwner, genesisKeyPair, UserRole.USER);

        // Get authorized key by owner should return the ACTIVE one (second key)
        var authorizedKey = blockchain.getAuthorizedKeyByOwner(aliceOwner);

        assertNotNull(authorizedKey, "Should find authorized key");
        assertEquals(alicePublicKey2, authorizedKey.getPublicKey(),
                    "Should return the active (second) key, not the revoked one");
        assertTrue(authorizedKey.isActive(), "Returned key should be active");
    }

    @Test
    @DisplayName("🔒 SECURITY: Prevent same public key with different owner name")
    void testPreventSamePublicKeyDifferentOwner() {
        // Create Alice
        KeyPair aliceKeyPair = CryptoUtil.generateKeyPair();
        String alicePublicKey = CryptoUtil.publicKeyToString(aliceKeyPair.getPublic());

        blockchain.addAuthorizedKey(alicePublicKey, aliceOwner, genesisKeyPair, UserRole.USER);

        // Try to authorize the SAME public key with a different owner name
        boolean result = blockchain.addAuthorizedKey(
            alicePublicKey,  // ← Same public key
            "DifferentOwner",  // ← Different owner name
            genesisKeyPair,
            UserRole.USER
        );

        // Should return false because the key is already authorized
        assertFalse(result, "Should not allow same public key with different owner name");
    }
}
