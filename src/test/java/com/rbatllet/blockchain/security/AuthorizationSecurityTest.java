package com.rbatllet.blockchain.security;

import com.rbatllet.blockchain.config.DatabaseConfig;
import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;
import org.junit.jupiter.api.*;

import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests for authorization mechanism.
 *
 * <p>Tests verify that the auto-authorization security vulnerability (v1.0.6)
 * has been fixed and that the new pre-authorization model works correctly.</p>
 *
 * @since 1.0.6
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthorizationSecurityTest {

    private Blockchain blockchain;
    private KeyPair bootstrapKeyPair;

    @BeforeAll
    void setupOnce() {
        JPAUtil.initialize(DatabaseConfig.createH2TestConfig());
    }

    @BeforeEach
    void setup() {
        blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        // Load bootstrap admin keys
        bootstrapKeyPair = KeyFileLoader.loadKeyPairFromFiles(
            "./keys/genesis-admin.private",
            "./keys/genesis-admin.public"
        );

        // Register bootstrap admin in blockchain (RBAC v1.0.6)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );
    }

    @Test
    @Order(1)
    @DisplayName("🔒 Security: Genesis admin is created on first init")
    void testGenesisAdminCreated() {
        // Fresh blockchain should have 1 authorized key (genesis admin)
        long count = blockchain.getAuthorizedKeyCount();
        assertEquals(1, count, "Genesis admin should be created automatically");
    }

    @Test
    @Order(2)
    @DisplayName("🔒 Security: Genesis admin is created only once")
    void testGenesisAdminCreatedOnce() {
        Blockchain blockchain2 = new Blockchain();
        long count = blockchain2.getAuthorizedKeyCount();
        assertEquals(1, count, "Should still have only 1 authorized key");
    }

    @Test
    @Order(3)
    @DisplayName("🔒 Security: Cannot create API without pre-authorization")
    void testSelfAuthorizationBlocked() {
        KeyPair attackerKeys = CryptoUtil.generateKeyPair();
        String attackerPublicKey = CryptoUtil.publicKeyToString(attackerKeys.getPublic());

        // Verify attacker is NOT authorized before attempting
        assertFalse(blockchain.isKeyAuthorized(attackerPublicKey),
            "Attacker should NOT be authorized before creating API");

        // Try to create API without pre-authorization (should fail)
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            new UserFriendlyEncryptionAPI(
                blockchain,
                "attacker",
                attackerKeys,
                EncryptionConfig.createHighSecurityConfig()
            );
        });

        // SECURITY FIX (v1.0.6): setDefaultCredentials() validates authorization and throws "UNAUTHORIZED"
        assertTrue(exception.getMessage().contains("UNAUTHORIZED"),
            "Exception should indicate keys are not authorized");
    }

    @Test
    @Order(4)
    @DisplayName("✅ Security: Can create API with pre-authorization")
    void testPreAuthorizedWorks() {
        KeyPair userKeys = CryptoUtil.generateKeyPair();
        String publicKey = CryptoUtil.publicKeyToString(userKeys.getPublic());

        // Pre-authorize user
        blockchain.addAuthorizedKey(publicKey, "Alice", bootstrapKeyPair, UserRole.USER);

        // Create API (should work)
        assertDoesNotThrow(() -> {
            new UserFriendlyEncryptionAPI(
                blockchain,
                "Alice",
                userKeys,
                EncryptionConfig.createHighSecurityConfig()
            );
        });
    }

    @Test
    @Order(5)
    @DisplayName("✅ Production: Multiple users can be authorized")
    void testMultipleUsersAuthorized() {
        // Create 3 users
        KeyPair alice = CryptoUtil.generateKeyPair();
        KeyPair bob = CryptoUtil.generateKeyPair();
        KeyPair charlie = CryptoUtil.generateKeyPair();

        // Authorize all users
        blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(alice.getPublic()), "Alice", bootstrapKeyPair, UserRole.USER);
        blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(bob.getPublic()), "Bob", bootstrapKeyPair, UserRole.USER);
        blockchain.addAuthorizedKey(CryptoUtil.publicKeyToString(charlie.getPublic()), "Charlie", bootstrapKeyPair, UserRole.USER);

        // Create APIs for all users (should all work)
        assertDoesNotThrow(() -> {
            new UserFriendlyEncryptionAPI(blockchain, "Alice", alice, EncryptionConfig.createHighSecurityConfig());
        });
        assertDoesNotThrow(() -> {
            new UserFriendlyEncryptionAPI(blockchain, "Bob", bob, EncryptionConfig.createHighSecurityConfig());
        });
        assertDoesNotThrow(() -> {
            new UserFriendlyEncryptionAPI(blockchain, "Charlie", charlie, EncryptionConfig.createHighSecurityConfig());
        });

        // Should have 4 authorized keys (genesis + 3 users)
        assertEquals(4, blockchain.getAuthorizedKeyCount());
    }

    @Test
    @Order(6)
    @DisplayName("🔒 Security: Cannot call loadUserCredentials() without authorization (Vulnerability #4)")
    void testLoadUserCredentialsBlocked() {
        // Create an API instance without credentials (no auto-authorization)
        UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);

        // Try to load credentials without being authorized (should fail)
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            api.loadUserCredentials("attacker", "Password123!");
        });

        assertTrue(exception.getMessage().contains("AUTHORIZATION REQUIRED"),
            "Exception should mention authorization requirement");
        assertTrue(exception.getMessage().contains("Must set authorized credentials"),
            "Exception should explain the bootstrap requirement");
    }

    @Test
    @Order(7)
    @DisplayName("🔒 Security: Cannot call importAndRegisterUser() without authorization (Vulnerability #5)")
    void testImportAndRegisterUserBlocked() {
        // Create an API instance without credentials (no auto-authorization)
        UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);

        // Try to import a user without being authorized (should fail)
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            api.importAndRegisterUser("attacker", "/tmp/fake-keypair.key");
        });

        assertTrue(exception.getMessage().contains("AUTHORIZATION REQUIRED"),
            "Exception should mention authorization requirement");
        assertTrue(exception.getMessage().contains("Must set authorized credentials"),
            "Exception should explain the bootstrap requirement");
    }

    @Test
    @Order(8)
    @DisplayName("🔒 Security: Cannot call importAndSetDefaultUser() without authorization (Vulnerability #6 - MOST CRITICAL)")
    void testImportAndSetDefaultUserBlocked() {
        // Create an API instance without credentials (no auto-authorization)
        UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);

        // Try to import and set default user without being authorized (should fail)
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            api.importAndSetDefaultUser("attacker", "/tmp/fake-keypair.key");
        });

        assertTrue(exception.getMessage().contains("AUTHORIZATION REQUIRED"),
            "Exception should mention authorization requirement");
        assertTrue(exception.getMessage().contains("Must set authorized credentials"),
            "Exception should explain the bootstrap requirement");
    }
}
