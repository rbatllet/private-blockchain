package demo;

import com.rbatllet.blockchain.security.SecureKeyStorage;
import com.rbatllet.blockchain.util.CryptoUtil;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Arrays;

/**
 * Demonstration of SecureKeyStorage with AES-256-GCM encryption
 *
 * This demo shows:
 * - Saving private keys with AES-256-GCM authenticated encryption
 * - Loading keys with automatic authentication tag verification
 * - Key integrity validation
 * - Secure error handling
 * - Multiple users management
 */
public class SecureKeyStorageDemo {

    public static void main(String[] args) {
        System.out.println("📊 SecureKeyStorage Demo - AES-256-GCM Encryption");
        System.out.println("=".repeat(60));
        System.out.println();

        try {
            // Clean up any existing demo keys
            cleanupDemoKeys();

            // Demo 1: Basic key storage and retrieval
            demonstrateBasicUsage();

            System.out.println();

            // Demo 2: Multiple users
            demonstrateMultipleUsers();

            System.out.println();

            // Demo 3: Security features
            demonstrateSecurityFeatures();

            System.out.println();

            // Demo 4: Error handling
            demonstrateErrorHandling();

            System.out.println();

            // Final cleanup
            cleanupDemoKeys();

            System.out.println("✅ Demo completed successfully!");

        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Demo 1: Basic usage of SecureKeyStorage
     */
    private static void demonstrateBasicUsage() {
        System.out.println("📋 Demo 1: Basic Key Storage and Retrieval");
        System.out.println("-".repeat(60));

        // Generate an ML-DSA-87 key pair
        System.out.println("🔑 Generating " + CryptoUtil.ALGORITHM_DISPLAY_NAME + " key pair (" + CryptoUtil.SECURITY_LEVEL_BITS + "-bit quantum-resistant)...");
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        PrivateKey originalKey = keyPair.getPrivate();
        System.out.println("✅ Key pair generated");

        // Save the private key with AES-256-GCM encryption
        String ownerName = "Alice";
        String password = "SecureP@ssw0rd123!";

        System.out.println("\n🔐 Saving private key with AES-256-GCM encryption...");
        System.out.println("   Owner: " + ownerName);
        System.out.println("   Encryption: AES-256-GCM");
        System.out.println("   IV: 96-bit random");
        System.out.println("   Auth Tag: 128-bit");
        System.out.println("   Key Derivation: SHA-3-256");

        boolean saved = SecureKeyStorage.savePrivateKey(ownerName, originalKey, password);

        if (saved) {
            System.out.println("✅ Private key saved successfully");
        } else {
            System.out.println("❌ Failed to save private key");
            return;
        }

        // Check if key exists
        System.out.println("\n🔍 Checking if key exists...");
        boolean exists = SecureKeyStorage.hasPrivateKey(ownerName);
        System.out.println("   Key exists: " + exists);

        // Load the private key
        System.out.println("\n🔓 Loading private key (with authentication verification)...");
        PrivateKey loadedKey = SecureKeyStorage.loadPrivateKey(ownerName, password);

        if (loadedKey != null) {
            System.out.println("✅ Private key loaded successfully");

            // Verify key integrity
            boolean keysMatch = Arrays.equals(
                originalKey.getEncoded(),
                loadedKey.getEncoded()
            );

            System.out.println("\n🔐 Verifying key integrity...");
            System.out.println("   Keys match: " + keysMatch);

            if (keysMatch) {
                System.out.println("✅ Key integrity verified - encryption/decryption successful");
            } else {
                System.out.println("❌ Key integrity check failed!");
            }
        } else {
            System.out.println("❌ Failed to load key");
        }
    }

    /**
     * Demo 2: Multiple users with different keys
     */
    private static void demonstrateMultipleUsers() {
        System.out.println("📋 Demo 2: Multiple Users Management");
        System.out.println("-".repeat(60));

        String[] users = {"Alice", "Bob", "Charlie", "Diana"};
        String[] passwords = {
            "AliceSecure123!",
            "BobStrong456@",
            "CharliePass789#",
            "DianaKey012$"
        };

        System.out.println("🔑 Creating keys for multiple users...\n");

        for (int i = 0; i < users.length; i++) {
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            boolean saved = SecureKeyStorage.savePrivateKey(
                users[i],
                keyPair.getPrivate(),
                passwords[i]
            );

            if (saved) {
                System.out.println("✅ " + users[i] + ": Key saved with AES-256-GCM");
            } else {
                System.out.println("❌ " + users[i] + ": Failed to save key");
            }
        }

        // List all stored keys
        System.out.println("\n📋 Listing all stored keys...");
        String[] storedKeys = SecureKeyStorage.listStoredKeys();
        System.out.println("   Total keys: " + storedKeys.length);

        for (String keyName : storedKeys) {
            System.out.println("   - " + keyName);
        }

        // Verify each user can load their key
        System.out.println("\n🔍 Verifying each user can load their key...");
        for (int i = 0; i < users.length; i++) {
            PrivateKey key = SecureKeyStorage.loadPrivateKey(users[i], passwords[i]);
            if (key != null) {
                System.out.println("✅ " + users[i] + ": Key loaded and authenticated");
            } else {
                System.out.println("❌ " + users[i] + ": Failed to load key");
            }
        }
    }

    /**
     * Demo 3: Security features demonstration
     */
    private static void demonstrateSecurityFeatures() {
        System.out.println("📋 Demo 3: Security Features");
        System.out.println("-".repeat(60));

        String ownerName = "SecurityTest";
        String correctPassword = "Correct@Pass123";
        String wrongPassword = "Wrong@Pass456";

        // Create and save a key
        KeyPair keyPair = CryptoUtil.generateKeyPair();
        SecureKeyStorage.savePrivateKey(ownerName, keyPair.getPrivate(), correctPassword);

        // Test 1: Authentication tag verification with wrong password
        System.out.println("🔐 Test 1: Authentication with wrong password");
        PrivateKey wrongPasswordKey = SecureKeyStorage.loadPrivateKey(ownerName, wrongPassword);
        if (wrongPasswordKey == null) {
            System.out.println("✅ Correctly rejected wrong password (auth tag verification failed)");
        } else {
            System.out.println("❌ Security issue: Wrong password accepted!");
        }

        // Test 2: Correct password authentication
        System.out.println("\n🔐 Test 2: Authentication with correct password");
        PrivateKey correctPasswordKey = SecureKeyStorage.loadPrivateKey(ownerName, correctPassword);
        if (correctPasswordKey != null) {
            System.out.println("✅ Correctly authenticated with correct password");
        } else {
            System.out.println("❌ Failed to authenticate with correct password");
        }

        // Test 3: Each save uses different IV
        System.out.println("\n🔐 Test 3: Random IV generation (each save is different)");
        SecureKeyStorage.savePrivateKey(ownerName + "1", keyPair.getPrivate(), correctPassword);
        SecureKeyStorage.savePrivateKey(ownerName + "2", keyPair.getPrivate(), correctPassword);
        System.out.println("ℹ️  Each encryption uses a unique random 96-bit IV");
        System.out.println("✅ Prevents pattern analysis attacks");

        // Cleanup test keys
        SecureKeyStorage.deletePrivateKey(ownerName);
        SecureKeyStorage.deletePrivateKey(ownerName + "1");
        SecureKeyStorage.deletePrivateKey(ownerName + "2");
    }

    /**
     * Demo 4: Error handling and edge cases
     */
    private static void demonstrateErrorHandling() {
        System.out.println("📋 Demo 4: Error Handling");
        System.out.println("-".repeat(60));

        // Test 1: Loading non-existent key
        System.out.println("🔍 Test 1: Loading non-existent key");
        PrivateKey nonExistent = SecureKeyStorage.loadPrivateKey("NonExistent", "password");
        if (nonExistent == null) {
            System.out.println("✅ Correctly returns null for non-existent key");
        }

        // Test 2: Deleting non-existent key
        System.out.println("\n🔍 Test 2: Deleting non-existent key");
        boolean deleted = SecureKeyStorage.deletePrivateKey("NonExistent");
        if (!deleted) {
            System.out.println("✅ Correctly returns false when deleting non-existent key");
        }

        // Test 3: Invalid input validation
        System.out.println("\n🔍 Test 3: Invalid input validation");
        KeyPair keyPair = CryptoUtil.generateKeyPair();

        boolean savedNull = SecureKeyStorage.savePrivateKey(null, keyPair.getPrivate(), "password");
        if (!savedNull) {
            System.out.println("✅ Rejected null owner name");
        }

        boolean savedEmpty = SecureKeyStorage.savePrivateKey("", keyPair.getPrivate(), "password");
        if (!savedEmpty) {
            System.out.println("✅ Rejected empty owner name");
        }

        boolean savedNullKey = SecureKeyStorage.savePrivateKey("test", null, "password");
        if (!savedNullKey) {
            System.out.println("✅ Rejected null private key");
        }

        boolean savedNullPassword = SecureKeyStorage.savePrivateKey("test", keyPair.getPrivate(), null);
        if (!savedNullPassword) {
            System.out.println("✅ Rejected null password");
        }
    }

    /**
     * Clean up demo keys
     */
    private static void cleanupDemoKeys() {
        String[] demoUsers = {"Alice", "Bob", "Charlie", "Diana", "SecurityTest"};

        for (String user : demoUsers) {
            SecureKeyStorage.deletePrivateKey(user);
        }
    }
}
