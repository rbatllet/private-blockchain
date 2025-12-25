package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.testutil.GenesisKeyManager;
import com.rbatllet.blockchain.util.CryptoUtil;
import org.junit.jupiter.api.*;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests for hierarchical key RBAC validation (v1.0.6+).
 *
 * <p>This test suite validates that role-based access control is properly enforced
 * for hierarchical key operations. Tests cover all privilege levels:</p>
 * <ul>
 *   <li><strong>SUPER_ADMIN:</strong> Can create/rotate ROOT and INTERMEDIATE keys</li>
 *   <li><strong>ADMIN:</strong> Can create/rotate INTERMEDIATE keys (but NOT ROOT)</li>
 *   <li><strong>USER:</strong> Cannot create/rotate privileged keys</li>
 *   <li><strong>READ_ONLY:</strong> Cannot create/rotate any keys</li>
 * </ul>
 *
 * @since 1.0.6
 */
@DisplayName("🔐 Hierarchical Key RBAC Security Tests")
class UserFriendlyEncryptionAPIHierarchicalKeySecurityTest {

    private Blockchain blockchain;
    private KeyPair bootstrapKeyPair;

    private KeyPair superAdminKeyPair;
    private KeyPair adminKeyPair;
    private KeyPair userKeyPair;
    private KeyPair readOnlyKeyPair;

    private UserFriendlyEncryptionAPI superAdminApi;
    private UserFriendlyEncryptionAPI adminApi;
    private UserFriendlyEncryptionAPI userApi;
    private UserFriendlyEncryptionAPI readOnlyApi;

    @BeforeEach
    void setUp() throws Exception {
        // Initialize blockchain
        blockchain = new Blockchain();

        // RBAC FIX (v1.0.6): Clear database before bootstrap to avoid "Existing users" error
        blockchain.clearAndReinitialize();

        // Load bootstrap admin keys (auto-generates if missing - test-only)
        bootstrapKeyPair = GenesisKeyManager.ensureGenesisKeysExist();

        // SECURITY (v1.0.6): Register bootstrap admin in blockchain (REQUIRED!)
        blockchain.createBootstrapAdmin(
            CryptoUtil.publicKeyToString(bootstrapKeyPair.getPublic()),
            "BOOTSTRAP_ADMIN"
        );

        // Generate key pairs for each role
        superAdminKeyPair = CryptoUtil.generateKeyPair();
        adminKeyPair = CryptoUtil.generateKeyPair();
        userKeyPair = CryptoUtil.generateKeyPair();
        readOnlyKeyPair = CryptoUtil.generateKeyPair();

        // Pre-authorize users with different roles
        String superAdminPubKey = CryptoUtil.publicKeyToString(superAdminKeyPair.getPublic());
        String adminPubKey = CryptoUtil.publicKeyToString(adminKeyPair.getPublic());
        String userPubKey = CryptoUtil.publicKeyToString(userKeyPair.getPublic());
        String readOnlyPubKey = CryptoUtil.publicKeyToString(readOnlyKeyPair.getPublic());

        blockchain.addAuthorizedKey(superAdminPubKey, "superadmin", bootstrapKeyPair, UserRole.SUPER_ADMIN);
        blockchain.addAuthorizedKey(adminPubKey, "admin", bootstrapKeyPair, UserRole.ADMIN);
        blockchain.addAuthorizedKey(userPubKey, "user", bootstrapKeyPair, UserRole.USER);
        blockchain.addAuthorizedKey(readOnlyPubKey, "readonly", bootstrapKeyPair, UserRole.READ_ONLY);

        // Initialize APIs with different credentials
        superAdminApi = new UserFriendlyEncryptionAPI(blockchain, "superadmin", superAdminKeyPair);
        adminApi = new UserFriendlyEncryptionAPI(blockchain, "admin", adminKeyPair);
        userApi = new UserFriendlyEncryptionAPI(blockchain, "user", userKeyPair);
        readOnlyApi = new UserFriendlyEncryptionAPI(blockchain, "readonly", readOnlyKeyPair);
    }

    @AfterEach
    void tearDown() {
        blockchain = null;
        superAdminApi = null;
        adminApi = null;
        userApi = null;
        readOnlyApi = null;
    }

    @Nested
    @DisplayName("🔑 setupHierarchicalKeys() RBAC Tests")
    class SetupHierarchicalKeysTests {

        @Test
        @DisplayName("SUPER_ADMIN should be able to setup hierarchical keys")
        void superAdminShouldSetupHierarchicalKeys() {
            // Given
            String masterPassword = "SuperSecureMasterPassword123!";

            // When
            KeyManagementResult result = superAdminApi.setupHierarchicalKeys(masterPassword);

            // Then
            assertTrue(result.isSuccess(), "SUPER_ADMIN should be able to setup hierarchical keys");
            assertNotNull(result.getRootKeyId(), "Root key should be created");
            assertNotNull(result.getIntermediateKeyId(), "Intermediate key should be created");
            // Note: setupHierarchicalKeys() creates 3 keys (root, intermediate, operational)
            // The main assertion is success=true with root and intermediate IDs populated
        }

        @Test
        @DisplayName("ADMIN should NOT be able to setup hierarchical keys (creates ROOT)")
        void adminShouldNotSetupHierarchicalKeys() {
            // Given
            String masterPassword = "AdminMasterPassword123!";

            // When & Then
            SecurityException exception = assertThrows(
                SecurityException.class,
                () -> adminApi.setupHierarchicalKeys(masterPassword),
                "ADMIN should NOT be able to setup hierarchical keys (requires ROOT creation)"
            );

            assertTrue(exception.getMessage().contains("PERMISSION DENIED"),
                "Error message should indicate permission denied");
            assertTrue(exception.getMessage().contains("SUPER_ADMIN"),
                "Error message should indicate SUPER_ADMIN is required");
        }

        @Test
        @DisplayName("USER should NOT be able to setup hierarchical keys")
        void userShouldNotSetupHierarchicalKeys() {
            // Given
            String masterPassword = "UserMasterPassword123!";

            // When & Then
            SecurityException exception = assertThrows(
                SecurityException.class,
                () -> userApi.setupHierarchicalKeys(masterPassword),
                "USER should NOT be able to setup hierarchical keys"
            );

            assertTrue(exception.getMessage().contains("PERMISSION DENIED") ||
                       exception.getMessage().contains("SUPER_ADMIN"),
                "Error message should indicate insufficient permissions");
        }

        @Test
        @DisplayName("READ_ONLY should NOT be able to setup hierarchical keys")
        void readOnlyShouldNotSetupHierarchicalKeys() {
            // Given
            String masterPassword = "ReadOnlyPassword123!";

            // When & Then
            SecurityException exception = assertThrows(
                SecurityException.class,
                () -> readOnlyApi.setupHierarchicalKeys(masterPassword),
                "READ_ONLY should NOT be able to setup hierarchical keys"
            );

            assertTrue(exception.getMessage().contains("PERMISSION DENIED") ||
                       exception.getMessage().contains("SUPER_ADMIN"),
                "Error message should indicate insufficient permissions");
        }
    }

    @Nested
    @DisplayName("🌲 generateHierarchicalKey() ROOT (depth=1) RBAC Tests")
    class GenerateRootKeyTests {

        @Test
        @DisplayName("SUPER_ADMIN should be able to generate ROOT keys")
        void superAdminShouldGenerateRootKeys() {
            // Given
            String purpose = "ROOT_KEY_GENERATION";
            int depth = 1;
            Map<String, Object> options = new HashMap<>();
            options.put("keySize", 256);

            // When
            KeyManagementResult result = superAdminApi.generateHierarchicalKey(purpose, depth, options);

            // Then
            assertTrue(result.isSuccess(), "SUPER_ADMIN should be able to generate ROOT keys");
            assertNotNull(result.getGeneratedKeyId(), "ROOT key should be generated");
        }

        @Test
        @DisplayName("ADMIN should NOT be able to generate ROOT keys")
        void adminShouldNotGenerateRootKeys() {
            // Given
            String purpose = "ROOT_KEY_GENERATION";
            int depth = 1;
            Map<String, Object> options = new HashMap<>();

            // When & Then
            SecurityException exception = assertThrows(
                SecurityException.class,
                () -> adminApi.generateHierarchicalKey(purpose, depth, options),
                "ADMIN should NOT be able to generate ROOT keys"
            );

            assertTrue(exception.getMessage().contains("PERMISSION DENIED"),
                "Error message should indicate permission denied");
            assertTrue(exception.getMessage().contains("SUPER_ADMIN"),
                "Error message should indicate SUPER_ADMIN is required");
        }

        @Test
        @DisplayName("USER should NOT be able to generate ROOT keys")
        void userShouldNotGenerateRootKeys() {
            // Given
            String purpose = "ROOT_KEY_GENERATION";
            int depth = 1;
            Map<String, Object> options = new HashMap<>();

            // When & Then
            SecurityException exception = assertThrows(
                SecurityException.class,
                () -> userApi.generateHierarchicalKey(purpose, depth, options),
                "USER should NOT be able to generate ROOT keys"
            );

            assertTrue(exception.getMessage().contains("PERMISSION DENIED") ||
                       exception.getMessage().contains("SUPER_ADMIN"),
                "Error message should indicate insufficient permissions");
        }
    }

    @Nested
    @DisplayName("🌿 generateHierarchicalKey() INTERMEDIATE (depth=2) RBAC Tests")
    class GenerateIntermediateKeyTests {

        @BeforeEach
        void setupRootKey() {
            // Setup requires ROOT key first (only SUPER_ADMIN can do this)
            superAdminApi.generateHierarchicalKey("ROOT_FOR_TESTING", 1, null);
        }

        @Test
        @DisplayName("SUPER_ADMIN should be able to generate INTERMEDIATE keys")
        void superAdminShouldGenerateIntermediateKeys() {
            // Given
            String purpose = "INTERMEDIATE_KEY_GENERATION";
            int depth = 2;
            Map<String, Object> options = new HashMap<>();

            // When
            KeyManagementResult result = superAdminApi.generateHierarchicalKey(purpose, depth, options);

            // Then
            assertTrue(result.isSuccess(), "SUPER_ADMIN should be able to generate INTERMEDIATE keys");
            assertNotNull(result.getGeneratedKeyId(), "INTERMEDIATE key should be generated");
        }

        @Test
        @DisplayName("ADMIN should be able to generate INTERMEDIATE keys")
        void adminShouldGenerateIntermediateKeys() {
            // Given
            String purpose = "INTERMEDIATE_KEY_GENERATION";
            int depth = 2;
            Map<String, Object> options = new HashMap<>();

            // When
            KeyManagementResult result = adminApi.generateHierarchicalKey(purpose, depth, options);

            // Then
            assertTrue(result.isSuccess(), "ADMIN should be able to generate INTERMEDIATE keys");
            assertNotNull(result.getGeneratedKeyId(), "INTERMEDIATE key should be generated");
        }

        @Test
        @DisplayName("USER should NOT be able to generate INTERMEDIATE keys")
        void userShouldNotGenerateIntermediateKeys() {
            // Given
            String purpose = "INTERMEDIATE_KEY_GENERATION";
            int depth = 2;
            Map<String, Object> options = new HashMap<>();

            // When & Then
            SecurityException exception = assertThrows(
                SecurityException.class,
                () -> userApi.generateHierarchicalKey(purpose, depth, options),
                "USER should NOT be able to generate INTERMEDIATE keys"
            );

            assertTrue(exception.getMessage().contains("PERMISSION DENIED") ||
                       exception.getMessage().contains("SUPER_ADMIN") ||
                       exception.getMessage().contains("ADMIN"),
                "Error message should indicate insufficient permissions");
        }
    }

    @Nested
    @DisplayName("🍃 generateHierarchicalKey() OPERATIONAL (depth=3+) RBAC Tests")
    class GenerateOperationalKeyTests {

        @BeforeEach
        void setupHierarchy() {
            // Setup requires ROOT and INTERMEDIATE keys first (only SUPER_ADMIN can do this)
            superAdminApi.generateHierarchicalKey("ROOT_FOR_TESTING", 1, null);
            superAdminApi.generateHierarchicalKey("INTERMEDIATE_FOR_TESTING", 2, null);
        }

        @Test
        @DisplayName("ALL authorized users should be able to generate OPERATIONAL keys")
        void allUsersShouldGenerateOperationalKeys() {
            // Given
            String purpose = "OPERATIONAL_KEY_GENERATION";
            int depth = 3;
            Map<String, Object> options = new HashMap<>();

            // When & Then
            assertAll("All roles should be able to generate operational keys",
                () -> {
                    KeyManagementResult result = superAdminApi.generateHierarchicalKey(purpose, depth, options);
                    assertTrue(result.isSuccess(), "SUPER_ADMIN should generate OPERATIONAL keys");
                },
                () -> {
                    KeyManagementResult result = adminApi.generateHierarchicalKey(purpose, depth, options);
                    assertTrue(result.isSuccess(), "ADMIN should generate OPERATIONAL keys");
                },
                () -> {
                    KeyManagementResult result = userApi.generateHierarchicalKey(purpose, depth, options);
                    assertTrue(result.isSuccess(), "USER should generate OPERATIONAL keys");
                }
            );
        }
    }

    @Nested
    @DisplayName("🔄 rotateHierarchicalKeys() RBAC Tests")
    class RotateHierarchicalKeysTests {

        private String rootKeyId;
        private String intermediateKeyId;
        private String operationalKeyId;

        @BeforeEach
        void setupHierarchy() {
            // Setup full hierarchy (only SUPER_ADMIN can do this)
            KeyManagementResult rootResult = superAdminApi.generateHierarchicalKey("ROOT_FOR_TESTING", 1, null);
            rootKeyId = rootResult.getGeneratedKeyId();

            KeyManagementResult intermediateResult = superAdminApi.generateHierarchicalKey("INTERMEDIATE_FOR_TESTING", 2, null);
            intermediateKeyId = intermediateResult.getGeneratedKeyId();

            KeyManagementResult operationalResult = superAdminApi.generateHierarchicalKey("OPERATIONAL_FOR_TESTING", 3, null);
            operationalKeyId = operationalResult.getGeneratedKeyId();
        }

        @Test
        @DisplayName("Only SUPER_ADMIN should rotate ROOT keys")
        void onlySuperAdminShouldRotateRootKeys() {
            // Given
            Map<String, Object> options = new HashMap<>();
            options.put("preserveHierarchy", true);

            // When & Then
            assertAll("ROOT key rotation permissions",
                () -> {
                    KeyManagementResult result = superAdminApi.rotateHierarchicalKeys(rootKeyId, options);
                    assertTrue(result.isSuccess(), "SUPER_ADMIN should rotate ROOT keys");
                },
                () -> {
                    SecurityException exception = assertThrows(
                        SecurityException.class,
                        () -> adminApi.rotateHierarchicalKeys(rootKeyId, options),
                        "ADMIN should NOT rotate ROOT keys"
                    );
                    assertTrue(exception.getMessage().contains("PERMISSION DENIED") ||
                               exception.getMessage().contains("SUPER_ADMIN"));
                },
                () -> {
                    SecurityException exception = assertThrows(
                        SecurityException.class,
                        () -> userApi.rotateHierarchicalKeys(rootKeyId, options),
                        "USER should NOT rotate ROOT keys"
                    );
                    assertTrue(exception.getMessage().contains("PERMISSION DENIED") ||
                               exception.getMessage().contains("SUPER_ADMIN"));
                }
            );
        }

        @Test
        @DisplayName("SUPER_ADMIN and ADMIN should rotate INTERMEDIATE keys")
        void superAdminAndAdminShouldRotateIntermediateKeys() {
            // Given
            Map<String, Object> options = new HashMap<>();
            options.put("preserveHierarchy", true);

            // When & Then
            assertAll("INTERMEDIATE key rotation permissions",
                () -> {
                    KeyManagementResult result = superAdminApi.rotateHierarchicalKeys(intermediateKeyId, options);
                    assertTrue(result.isSuccess(), "SUPER_ADMIN should rotate INTERMEDIATE keys");
                },
                () -> {
                    KeyManagementResult result = adminApi.rotateHierarchicalKeys(intermediateKeyId, options);
                    assertTrue(result.isSuccess(), "ADMIN should rotate INTERMEDIATE keys");
                },
                () -> {
                    SecurityException exception = assertThrows(
                        SecurityException.class,
                        () -> userApi.rotateHierarchicalKeys(intermediateKeyId, options),
                        "USER should NOT rotate INTERMEDIATE keys"
                    );
                    assertTrue(exception.getMessage().contains("PERMISSION DENIED"));
                }
            );
        }

        @Test
        @DisplayName("All authorized users should rotate OPERATIONAL keys")
        void allUsersShouldRotateOperationalKeys() {
            // Given
            Map<String, Object> options = new HashMap<>();
            options.put("preserveHierarchy", true);

            // When & Then
            assertAll("OPERATIONAL key rotation permissions",
                () -> {
                    KeyManagementResult result = superAdminApi.rotateHierarchicalKeys(operationalKeyId, options);
                    assertTrue(result.isSuccess(), "SUPER_ADMIN should rotate OPERATIONAL keys");
                },
                () -> {
                    KeyManagementResult result = adminApi.rotateHierarchicalKeys(operationalKeyId, options);
                    assertTrue(result.isSuccess(), "ADMIN should rotate OPERATIONAL keys");
                },
                () -> {
                    KeyManagementResult result = userApi.rotateHierarchicalKeys(operationalKeyId, options);
                    assertTrue(result.isSuccess(), "USER should rotate OPERATIONAL keys");
                }
            );
        }
    }
}
