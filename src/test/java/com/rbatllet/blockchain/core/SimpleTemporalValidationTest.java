package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.dao.AuthorizedKeyDAO;
import com.rbatllet.blockchain.entity.AuthorizedKey;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.*;

import java.security.KeyPair;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to verify the temporal validation fixes work correctly
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Simple Temporal Validation Test")
class SimpleTemporalValidationTest {

    private Blockchain blockchain;
    private KeyPair aliceKeyPair;
    private String alicePublicKey;

    @BeforeEach
    void setUp() {
        // Generate fresh key pair using the new hierarchical key system
        CryptoUtil.KeyInfo aliceKeyInfo = CryptoUtil.createRootKey();
        
        // Convert to KeyPair object
        aliceKeyPair = new KeyPair(
            CryptoUtil.stringToPublicKey(aliceKeyInfo.getPublicKeyEncoded()),
            CryptoUtil.stringToPrivateKey(aliceKeyInfo.getPrivateKeyEncoded())
        );
        
        // Get public key string
        alicePublicKey = aliceKeyInfo.getPublicKeyEncoded();
    }

    @AfterEach
    void tearDown() {
        // Clean database state for next test
        if (blockchain != null) {
            blockchain.clearAndReinitialize();
        }
        
        // Allow connections to close properly
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test Basic Temporal Validation")
    void testBasicTemporalValidation() {
        // Create a fresh blockchain with clean state
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();
        
        // 1. Authorize Alice
        assertTrue(blockchain.addAuthorizedKey(alicePublicKey, "Alice"));
        
        // 2. Alice creates a block
        Block historicalBlock = blockchain.addBlockAndReturn("Historical block", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic());
        assertNotNull(historicalBlock, "Alice should be able to create a block with authorized key");
        
        // 3. Verify the block exists and is valid
        assertNotNull(historicalBlock);
        assertEquals("Historical block", historicalBlock.getData());
        assertEquals(alicePublicKey, historicalBlock.getSignerPublicKey());
        
        // 4. Test temporal validation directly
        AuthorizedKeyDAO dao = new AuthorizedKeyDAO();
        assertTrue(dao.wasKeyAuthorizedAt(alicePublicKey, historicalBlock.getTimestamp()),
                "Key should be authorized at block creation time");
        
        // 5. Revoke Alice
        assertTrue(blockchain.revokeAuthorizedKey(alicePublicKey));
        
        // 6. Alice cannot create new blocks
        assertFalse(blockchain.addBlock("Future block", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        
        // 7. But historical block should still be temporally valid
        assertTrue(dao.wasKeyAuthorizedAt(alicePublicKey, historicalBlock.getTimestamp()),
                "Historical block should remain temporally valid after revocation");
        
        System.out.println("✅ Basic temporal validation works correctly!");
    }

    @Test
    @Order(2)
    @DisplayName("Test Re-authorization Works")
    void testReAuthorizationWorks() {
        // Create a fresh blockchain
        Blockchain blockchain = new Blockchain();
        
        // 1. Authorize Alice
        assertTrue(blockchain.addAuthorizedKey(alicePublicKey, "Alice"));
        
        // 2. Alice creates a block
        assertTrue(blockchain.addBlock("First period", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        
        // 3. Revoke Alice
        assertTrue(blockchain.revokeAuthorizedKey(alicePublicKey));
        
        // 4. Alice cannot create blocks now
        assertFalse(blockchain.addBlock("Blocked period", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        
        // 5. Re-authorize Alice (this should work with our fixes)
        assertTrue(blockchain.addAuthorizedKey(alicePublicKey, "Alice-Reauthorized"),
                "Re-authorization should work");
        
        // 6. Alice can create blocks again
        assertTrue(blockchain.addBlock("Second period", aliceKeyPair.getPrivate(), aliceKeyPair.getPublic()));
        
        // 7. Verify we have at least the expected blocks (allowing for previous test data)
        assertTrue(blockchain.getBlockCount() >= 3, "Should have at least 3 blocks"); // Genesis + 2 Alice blocks
        
        System.out.println("✅ Re-authorization works correctly!");
    }

    @Test
    @Order(3)
    @DisplayName("Test Temporal Validation Entity Method")
    void testTemporalValidationEntityMethod() {
        // Test the new wasActiveAt method directly
        
        AuthorizedKey key = new AuthorizedKey(alicePublicKey, "Alice");
        LocalDateTime now = LocalDateTime.now();
        key.setCreatedAt(now.minusHours(1)); // Created 1 hour ago
        
        // 1. Key should be active at current time
        assertTrue(key.wasActiveAt(now), "Key should be active at current time");
        
        // 2. Key should not be active before it was created
        assertFalse(key.wasActiveAt(now.minusHours(2)), "Key should not be active before creation");
        
        // 3. Revoke the key
        key.setActive(false);
        key.setRevokedAt(now.minusMinutes(30)); // Revoked 30 minutes ago
        
        // 4. Key should still be active at a time before revocation
        assertTrue(key.wasActiveAt(now.minusMinutes(45)), "Key should be active before revocation");
        
        // 5. Key should not be active after revocation
        assertFalse(key.wasActiveAt(now.minusMinutes(15)), "Key should not be active after revocation");
        
        System.out.println("✅ Entity temporal validation method works correctly!");
    }
}