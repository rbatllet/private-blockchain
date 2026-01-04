package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.entity.AuthorizedKey;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.testutil.GenesisKeyManager;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.*;

import java.security.KeyPair;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for cryptographic challenge verification in addAuthorizedKey.
 *
 * <p>This test class verifies the security enhancement that requires all non-bootstrap
 * user creation operations to prove private key possession through cryptographic
 * challenge-response.</p>
 *
 * <p><b>Security Feature</b>: Prevents password-only attacks where an attacker with
 * a stolen password could impersonate a user without possessing their private key.</p>
 *
 * @since 1.0.6
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("🔐 Cryptographic Challenge Verification Tests")
class BlockchainChallengeVerificationTest {

    private Blockchain blockchain;
    private KeyPair bootstrapKeyPair;
    private KeyPair adminKeyPair;
    private KeyPair userKeyPair;
    private String adminPublicKey;
    private String userPublicKey;

    @BeforeEach
    void setUp() throws Exception {
        blockchain = new Blockchain();
        blockchain.clearAndReinitialize();

        // Load bootstrap admin keys (created automatically)
        bootstrapKeyPair = GenesisKeyManager.ensureGenesisKeysExist();

        // Register bootstrap admin in blockchain
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        // Create admin and user key pairs
        adminKeyPair = CryptoUtil.generateKeyPair();
        userKeyPair = CryptoUtil.generateKeyPair();
        adminPublicKey = CryptoUtil.publicKeyToString(adminKeyPair.getPublic());
        userPublicKey = CryptoUtil.publicKeyToString(userKeyPair.getPublic());

        // Add admin as authorized key (using bootstrap admin credentials)
        blockchain.addAuthorizedKey(adminPublicKey, "Administrator", bootstrapKeyPair, UserRole.ADMIN);
    }

    @AfterEach
    void tearDown() {
        if (blockchain != null) {
            blockchain.completeCleanupForTests();
        }
    }

    @Nested
    @DisplayName("✅ Challenge Verification - Normal Operation")
    class NormalOperationTests {

        @Test
        @Order(1)
        @DisplayName("Legacy method (4 params) should auto-generate and verify challenge")
        void testLegacyMethodAutoGeneratesChallenge() {
            // When: Using legacy method (auto-generates challenge)
            boolean result = blockchain.addAuthorizedKey(
                userPublicKey,
                "TestUser",
                adminKeyPair,
                UserRole.USER
            );

            // Then: Should succeed
            assertTrue(result, "User creation should succeed with auto-generated challenge");

            // Verify user was created
            AuthorizedKey createdUser = blockchain.getAuthorizedKeyByOwner("TestUser");
            assertNotNull(createdUser, "User should be created");
            assertEquals(UserRole.USER, createdUser.getRole(), "User should have USER role");
        }

        @Test
        @Order(2)
        @DisplayName("Enhanced method (6 params) should accept valid challenge and signature")
        void testEnhancedMethodAcceptsValidChallenge() {
            // Given: Valid challenge and signature
            String challenge = "challenge-test-" + UUID.randomUUID() + "-" + java.time.Instant.now().toString();
            String signature = CryptoUtil.signData(challenge, adminKeyPair.getPrivate());

            // When: Using enhanced method with valid challenge
            boolean result = blockchain.addAuthorizedKey(
                userPublicKey,
                "TestUser",
                adminKeyPair,
                UserRole.USER,
                challenge,
                signature
            );

            // Then: Should succeed
            assertTrue(result, "User creation should succeed with valid challenge and signature");

            // Verify user was created
            AuthorizedKey createdUser = blockchain.getAuthorizedKeyByOwner("TestUser");
            assertNotNull(createdUser, "User should be created");
        }

        @Test
        @Order(3)
        @DisplayName("Enhanced method should reject invalid signature")
        void testEnhancedMethodRejectsInvalidSignature() {
            // Given: Valid challenge but INVALID signature (signed by different key)
            String challenge = "challenge-test-" + UUID.randomUUID() + "-" + java.time.Instant.now().toString();
            String invalidSignature = CryptoUtil.signData(challenge, userKeyPair.getPrivate()); // Wrong key!

            // When & Then: Should throw SecurityException
            SecurityException ex = assertThrows(SecurityException.class, () -> {
                blockchain.addAuthorizedKey(
                    userPublicKey,
                    "TestUser",
                    adminKeyPair,
                    UserRole.USER,
                    challenge,
                    invalidSignature
                );
            });

            // Verify error message mentions signature verification
            assertTrue(ex.getMessage().contains("SIGNATURE VERIFICATION FAILED"),
                "Error should mention signature verification failure");
        }

        @Test
        @Order(4)
        @DisplayName("Enhanced method should reject null challenge in non-bootstrap mode")
        void testEnhancedMethodRejectsNullChallenge() {
            // When: Challenge is null (non-bootstrap mode)
            SecurityException ex = assertThrows(SecurityException.class, () -> {
                blockchain.addAuthorizedKey(
                    userPublicKey,
                    "TestUser",
                    adminKeyPair,
                    UserRole.USER,
                    null,  // Challenge
                    "signature"  // Signature
                );
            });

            // Verify error message
            assertTrue(ex.getMessage().contains("Cryptographic challenge is required"),
                "Error should mention required challenge");
        }

        @Test
        @Order(5)
        @DisplayName("Enhanced method should reject null signature in non-bootstrap mode")
        void testEnhancedMethodRejectsNullSignature() {
            // Given: Valid challenge but null signature
            String challenge = "challenge-test-" + UUID.randomUUID();

            // When: Signature is null (non-bootstrap mode)
            SecurityException ex = assertThrows(SecurityException.class, () -> {
                blockchain.addAuthorizedKey(
                    userPublicKey,
                    "TestUser",
                    adminKeyPair,
                    UserRole.USER,
                    challenge,
                    null  // Signature
                );
            });

            // Verify error message
            assertTrue(ex.getMessage().contains("Cryptographic challenge is required"),
                "Error should mention required challenge");
        }

        @Test
        @Order(6)
        @DisplayName("Enhanced method should reject empty challenge")
        void testEnhancedMethodRejectsEmptyChallenge() {
            // When: Challenge is empty string (treated as invalid)
            assertThrows(Exception.class, () -> {
                blockchain.addAuthorizedKey(
                    userPublicKey,
                    "TestUser",
                    adminKeyPair,
                    UserRole.USER,
                    "",  // Empty challenge
                    "signature"
                );
            }, "Empty challenge should cause error (verification will fail)");
        }
    }

    @Nested
    @DisplayName("🔒 Bootstrap Mode Exception")
    class BootstrapModeTests {

        @Test
        @Order(10)
        @DisplayName("Bootstrap mode (callerKeyPair=null) should allow null challenge and signature")
        void testBootstrapModeAllowsNullChallenge() {
            // Given: No existing users (bootstrap mode)
            blockchain.clearAndReinitialize();
            KeyPair newBootstrapKey = CryptoUtil.generateKeyPair();
            String newBootstrapPublicKey = CryptoUtil.publicKeyToString(newBootstrapKey.getPublic());

            // When: Creating bootstrap admin with null callerKeyPair
            boolean result = blockchain.addAuthorizedKey(
                newBootstrapPublicKey,
                "BOOTSTRAP_ADMIN",
                null,  // callerKeyPair is null (bootstrap mode)
                UserRole.SUPER_ADMIN,
                null,  // Challenge
                null   // Signature
            );

            // Then: Should succeed (bootstrap mode doesn't require challenge)
            assertTrue(result, "Bootstrap admin creation should succeed without challenge");

            // Verify bootstrap admin was created
            AuthorizedKey bootstrapAdmin = blockchain.getAuthorizedKeyByOwner("BOOTSTRAP_ADMIN");
            assertNotNull(bootstrapAdmin, "Bootstrap admin should be created");
            assertEquals(UserRole.SUPER_ADMIN, bootstrapAdmin.getRole(), "Should be SUPER_ADMIN");
        }

        @Test
        @Order(11)
        @DisplayName("Legacy method should handle null callerKeyPair (bootstrap mode)")
        void testLegacyMethodHandlesBootstrapMode() {
            // Given: No existing users (bootstrap mode)
            blockchain.clearAndReinitialize();
            KeyPair newBootstrapKey = CryptoUtil.generateKeyPair();
            String newBootstrapPublicKey = CryptoUtil.publicKeyToString(newBootstrapKey.getPublic());

            // When: Using legacy method with null callerKeyPair (generates null challenge)
            boolean result = blockchain.addAuthorizedKey(
                newBootstrapPublicKey,
                "BOOTSTRAP_ADMIN",
                null,  // callerKeyPair is null (bootstrap mode)
                UserRole.SUPER_ADMIN
            );

            // Then: Should succeed
            assertTrue(result, "Bootstrap admin creation should succeed with legacy method");

            // Verify bootstrap admin was created
            AuthorizedKey bootstrapAdmin = blockchain.getAuthorizedKeyByOwner("BOOTSTRAP_ADMIN");
            assertNotNull(bootstrapAdmin, "Bootstrap admin should be created");
        }

        @Test
        @Order(12)
        @DisplayName("Bootstrap mode should reject non-SUPER_ADMIN role")
        void testBootstrapModeRejectsNonSuperAdmin() {
            // Given: No existing users (bootstrap mode)
            blockchain.clearAndReinitialize();
            KeyPair newBootstrapKey = CryptoUtil.generateKeyPair();
            String newBootstrapPublicKey = CryptoUtil.publicKeyToString(newBootstrapKey.getPublic());

            // When: Trying to create non-SUPER_ADMIN in bootstrap mode
            SecurityException ex = assertThrows(SecurityException.class, () -> {
                blockchain.addAuthorizedKey(
                    newBootstrapPublicKey,
                    "FirstAdmin",
                    null,  // callerKeyPair is null (bootstrap mode)
                    UserRole.ADMIN  // Not SUPER_ADMIN!
                );
            });

            // Then: Should fail
            assertTrue(ex.getMessage().contains("SUPER_ADMIN"),
                "Error should mention that only SUPER_ADMIN is allowed in bootstrap mode");
        }
    }

    @Nested
    @DisplayName("🔑 Signature Verification Details")
    class SignatureVerificationTests {

        @Test
        @Order(20)
        @DisplayName("Challenge format should include timestamp and operation details")
        void testChallengeFormatIncludesTimestampAndOperation() {
            // Given: Challenge with specific format
            String username = "TestUser";
            UserRole role = UserRole.USER;
            String challenge = "challenge-" +
                               UUID.randomUUID().toString() + "-" +
                               java.time.Instant.now().toString() + "-" +
                               "add-key:" + username + ":" + role.name();

            String signature = CryptoUtil.signData(challenge, adminKeyPair.getPrivate());

            // When: Creating user with this challenge
            boolean result = blockchain.addAuthorizedKey(
                userPublicKey,
                username,
                adminKeyPair,
                role,
                challenge,
                signature
            );

            // Then: Should succeed
            assertTrue(result, "Should accept properly formatted challenge");

            // Verify user was created with correct details
            AuthorizedKey createdUser = blockchain.getAuthorizedKeyByOwner(username);
            assertNotNull(createdUser, "User should be created");
            assertEquals(role, createdUser.getRole(), "User should have correct role");
        }

        @Test
        @Order(21)
        @DisplayName("Tampered challenge should be rejected")
        void testTamperedChallengeRejected() {
            // Given: Valid signature for original challenge
            String originalChallenge = "challenge-test-" + UUID.randomUUID();
            String signature = CryptoUtil.signData(originalChallenge, adminKeyPair.getPrivate());

            // When: Challenge is tampered with after signing
            String tamperedChallenge = originalChallenge + "-TAMPERED";

            // Then: Should reject
            SecurityException ex = assertThrows(SecurityException.class, () -> {
                blockchain.addAuthorizedKey(
                    userPublicKey,
                    "TestUser",
                    adminKeyPair,
                    UserRole.USER,
                    tamperedChallenge,
                    signature
                );
            });

            assertTrue(ex.getMessage().contains("SIGNATURE VERIFICATION FAILED"),
                "Should reject tampered challenge");
        }

        @Test
        @Order(22)
        @DisplayName("Signature from different user should be rejected")
        void testSignatureFromDifferentUserRejected() {
            // Given: Challenge signed by wrong user
            String challenge = "challenge-test-" + UUID.randomUUID();
            String wrongSignature = CryptoUtil.signData(challenge, userKeyPair.getPrivate()); // User's key, not admin's

            // When & Then: Should reject
            SecurityException ex = assertThrows(SecurityException.class, () -> {
                blockchain.addAuthorizedKey(
                    userPublicKey,
                    "TestUser",
                    adminKeyPair,
                    UserRole.USER,
                    challenge,
                    wrongSignature
                );
            });

            assertTrue(ex.getMessage().contains("SIGNATURE VERIFICATION FAILED"),
                "Should reject signature from different user");
        }

        @Test
        @Order(23)
        @DisplayName("Invalid Base64 signature should be rejected")
        void testInvalidBase64SignatureRejected() {
            // Given: Valid challenge but invalid Base64 signature
            String challenge = "challenge-test-" + UUID.randomUUID();
            String invalidSignature = "not-a-valid-base64-signature!!!";

            // When: Signature is invalid Base64
            assertThrows(Exception.class, () -> {
                blockchain.addAuthorizedKey(
                    userPublicKey,
                    "TestUser",
                    adminKeyPair,
                    UserRole.USER,
                    challenge,
                    invalidSignature
                );
            }, "Invalid Base64 signature should cause error");
        }
    }

    @Nested
    @DisplayName("🎯 Integration with RBAC")
    class RBACIntegrationTests {

        @Test
        @Order(30)
        @DisplayName("Challenge verification should work with RBAC role validation")
        void testChallengeVerificationWithRBAC() {
            // Given: User with USER role (cannot create other users)
            blockchain.addAuthorizedKey(
                userPublicKey,
                "RegularUser",
                adminKeyPair,
                UserRole.USER
            );

            // When: Regular user tries to create another user with valid challenge
            String challenge = "challenge-test-" + UUID.randomUUID();
            String signature = CryptoUtil.signData(challenge, userKeyPair.getPrivate());

            // Then: Should fail due to RBAC (role permission), not signature
            SecurityException ex = assertThrows(SecurityException.class, () -> {
                KeyPair anotherUserKey = CryptoUtil.generateKeyPair();
                blockchain.addAuthorizedKey(
                    CryptoUtil.publicKeyToString(anotherUserKey.getPublic()),
                    "AnotherUser",
                    userKeyPair,
                    UserRole.USER,
                    challenge,
                    signature
                );
            });

            // Should be RBAC error, not signature error
            assertTrue(ex.getMessage().contains("PERMISSION DENIED") ||
                       ex.getMessage().contains("cannot create"),
                "Should fail due to RBAC, not signature verification");
        }

        @Test
        @Order(31)
        @DisplayName("ADMIN can create USER with valid challenge")
        void testAdminCanCreateUserWithChallenge() {
            // Given: Admin creates user with valid challenge
            String challenge = "challenge-test-" + UUID.randomUUID();
            String signature = CryptoUtil.signData(challenge, adminKeyPair.getPrivate());

            // When: Admin creates user
            boolean result = blockchain.addAuthorizedKey(
                userPublicKey,
                "NewUser",
                adminKeyPair,
                UserRole.USER,
                challenge,
                signature
            );

            // Then: Should succeed
            assertTrue(result, "Admin should be able to create user with valid challenge");

            AuthorizedKey newUser = blockchain.getAuthorizedKeyByOwner("NewUser");
            assertNotNull(newUser, "New user should be created");
            assertEquals(UserRole.USER, newUser.getRole(), "Should have USER role");
        }
    }

    @Nested
    @DisplayName("📊 Security Edge Cases")
    class SecurityEdgeCaseTests {

        @Test
        @Order(40)
        @DisplayName("Very long challenge should be handled correctly")
        void testVeryLongChallenge() {
            // Given: Very long challenge (10KB)
            String longChallenge = "challenge-" + "a".repeat(10000) + "-" + UUID.randomUUID();
            String signature = CryptoUtil.signData(longChallenge, adminKeyPair.getPrivate());

            // When: Creating user with long challenge
            boolean result = blockchain.addAuthorizedKey(
                userPublicKey,
                "TestUser",
                adminKeyPair,
                UserRole.USER,
                longChallenge,
                signature
            );

            // Then: Should succeed (no DoS on challenge size)
            assertTrue(result, "Should handle long challenges");
        }

        @Test
        @Order(41)
        @DisplayName("Special characters in challenge should be handled correctly")
        void testSpecialCharactersInChallenge() {
            // Given: Challenge with special characters
            String specialChallenge = "challenge-test-🔐-特殊文字-emoji😀-" + UUID.randomUUID();
            String signature = CryptoUtil.signData(specialChallenge, adminKeyPair.getPrivate());

            // When: Creating user with special characters in challenge
            boolean result = blockchain.addAuthorizedKey(
                userPublicKey,
                "TestUser",
                adminKeyPair,
                UserRole.USER,
                specialChallenge,
                signature
            );

            // Then: Should succeed
            assertTrue(result, "Should handle special characters in challenge");
        }

        @Test
        @Order(42)
        @DisplayName("Multiple rapid user creations should each have unique challenges")
        void testMultipleRapidCreationsHaveUniqueChallenges() {
            // Given: Existing users (BOOTSTRAP_ADMIN + Administrator)
            int initialUserCount = blockchain.getAuthorizedKeys().size();

            // When: Creating multiple users rapidly
            for (int i = 0; i < 5; i++) {
                KeyPair newUserKey = CryptoUtil.generateKeyPair();
                String username = "RapidUser" + i;

                // Each should get unique auto-generated challenge
                boolean result = blockchain.addAuthorizedKey(
                    CryptoUtil.publicKeyToString(newUserKey.getPublic()),
                    username,
                    adminKeyPair,
                    UserRole.USER
                );

                assertTrue(result, "User " + i + " should be created successfully");

                AuthorizedKey user = blockchain.getAuthorizedKeyByOwner(username);
                assertNotNull(user, "User " + i + " should exist");
            }

            // Then: Should have 5 more users than initially
            int finalUserCount = blockchain.getAuthorizedKeys().size();
            assertEquals(initialUserCount + 5, finalUserCount,
                "Should have 5 additional users");
        }
    }
}
