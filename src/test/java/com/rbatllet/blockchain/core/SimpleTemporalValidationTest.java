package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.dao.AuthorizedKeyDAO;
import com.rbatllet.blockchain.entity.AuthorizedKey;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.util.TestGenesisKeyManager;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.*;

import java.security.KeyPair;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Simple test to verify the temporal validation fixes work correctly
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Simple Temporal Validation Test")
class SimpleTemporalValidationTest {
    private static final Logger logger = LoggerFactory.getLogger(SimpleTemporalValidationTest.class);


    private Blockchain blockchain;
    private KeyPair aliceKeyPair;
    private String alicePublicKey;

    @BeforeEach
    void setUp() throws Exception {
        // RBAC FIX (v1.0.6): Load genesis-admin keys for createBootstrapAdmin()
        // createBootstrapAdmin() validates that the public key matches genesis-admin.public
        aliceKeyPair = TestGenesisKeyManager.ensureGenesisKeysExist();

        // Get public key string
        alicePublicKey = CryptoUtil.publicKeyToString(aliceKeyPair.getPublic());
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

        // 1. Authorize Alice as bootstrap admin (SUPER_ADMIN)
        assertTrue(blockchain.createBootstrapAdmin(alicePublicKey, "Alice"));

        // 2. Create Bob as a regular USER (not SUPER_ADMIN) to test revocation
        // RBAC v1.0.6: Cannot revoke last SUPER_ADMIN, so we test with a USER instead
        KeyPair bobKeyPair = CryptoUtil.generateKeyPair();
        String bobPublicKey = CryptoUtil.publicKeyToString(bobKeyPair.getPublic());
        blockchain.addAuthorizedKey(bobPublicKey, "Bob", aliceKeyPair, UserRole.USER);

        // 3. Bob creates a block
        Block historicalBlock = blockchain.addBlockAndReturn("Historical block", bobKeyPair.getPrivate(), bobKeyPair.getPublic());
        assertNotNull(historicalBlock, "Bob should be able to create a block with authorized key");

        // 4. Verify the block exists and is valid
        assertNotNull(historicalBlock);
        assertEquals("Historical block", historicalBlock.getData());
        assertEquals(bobPublicKey, historicalBlock.getSignerPublicKey());

        // 5. Test temporal validation directly
        AuthorizedKeyDAO dao = new AuthorizedKeyDAO();
        assertTrue(dao.wasKeyAuthorizedAt(bobPublicKey, historicalBlock.getTimestamp()),
                "Key should be authorized at block creation time");

        // 6. Revoke Bob (Alice can revoke Bob because Alice is SUPER_ADMIN)
        assertTrue(blockchain.revokeAuthorizedKey(bobPublicKey));

        // 7. Bob cannot create new blocks
        assertFalse(blockchain.addBlock("Future block", bobKeyPair.getPrivate(), bobKeyPair.getPublic()));

        // 8. But historical block should still be temporally valid
        assertTrue(dao.wasKeyAuthorizedAt(bobPublicKey, historicalBlock.getTimestamp()),
                "Historical block should remain temporally valid after revocation");

        logger.info("✅ Basic temporal validation works correctly!");
    }

    @Test
    @Order(2)
    @DisplayName("Test Re-authorization Works")
    void testReAuthorizationWorks() {
        // Create a fresh blockchain
        Blockchain blockchain = new Blockchain();
        blockchain.clearAndReinitialize();  // Ensure clean state before bootstrap

        // 1. Authorize Alice as bootstrap admin (SUPER_ADMIN)
        assertTrue(blockchain.createBootstrapAdmin(alicePublicKey, "Alice"));

        // 2. Create Bob as a regular USER to test re-authorization
        // RBAC v1.0.6: Cannot revoke last SUPER_ADMIN, so we test with a USER instead
        KeyPair bobKeyPair = CryptoUtil.generateKeyPair();
        String bobPublicKey = CryptoUtil.publicKeyToString(bobKeyPair.getPublic());
        blockchain.addAuthorizedKey(bobPublicKey, "Bob", aliceKeyPair, UserRole.USER);

        // 3. Bob creates a block
        assertTrue(blockchain.addBlock("First period", bobKeyPair.getPrivate(), bobKeyPair.getPublic()));

        // 4. Revoke Bob (Alice can revoke Bob)
        assertTrue(blockchain.revokeAuthorizedKey(bobPublicKey));

        // 5. Bob cannot create blocks now
        assertFalse(blockchain.addBlock("Blocked period", bobKeyPair.getPrivate(), bobKeyPair.getPublic()));

        // 6. Re-authorize Bob using addAuthorizedKey (Alice has SUPER_ADMIN role for authorization)
        assertTrue(blockchain.addAuthorizedKey(bobPublicKey, "Bob-Reauthorized", aliceKeyPair, UserRole.USER),
                "Re-authorization should work");

        // 7. Bob can create blocks again
        assertTrue(blockchain.addBlock("Second period", bobKeyPair.getPrivate(), bobKeyPair.getPublic()));

        // 8. Verify we have at least the expected blocks
        assertTrue(blockchain.getBlockCount() >= 2, "Should have at least 2 blocks"); // Genesis + 2 Bob blocks

        logger.info("✅ Re-authorization works correctly!");
    }

    @Test
    @Order(3)
    @DisplayName("Test Temporal Validation Entity Method")
    void testTemporalValidationEntityMethod() {
        // Test the new wasActiveAt method directly

        LocalDateTime now = LocalDateTime.now();
        AuthorizedKey key = new AuthorizedKey(alicePublicKey, "Alice", UserRole.USER, "TestCreator", now.minusHours(1));
        // Created 1 hour ago (via constructor)
        
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
        
        logger.info("✅ Entity temporal validation method works correctly!");
    }
}