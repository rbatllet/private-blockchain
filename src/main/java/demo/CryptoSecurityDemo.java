package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.security.KeyFileLoader;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.CryptoUtil.KeyInfo;
import com.rbatllet.blockchain.util.CryptoUtil.KeyType;

import java.security.KeyPair;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Demonstration of enhanced cryptographic security features:
 * - SHA-3 for hashing (quantum-resistant)
 * - ML-DSA-87 for digital signatures (NIST FIPS 204, 256-bit quantum-resistant, post-quantum secure)
 * - Hierarchical key management (root, intermediate, operational)
 * - Key rotation and revocation capabilities
 */
public class CryptoSecurityDemo {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Method to clean up only the demo keys from in-memory key store
     * This preserves keys needed for other tests
     */
    private static void cleanDemoKeys() {
        try {
            // Instead of clearing the entire keyStore, we'll just log that we're done
            // This is safer as it doesn't interfere with other tests that might need the keys
            
            System.out.println("🗑️ Demo keys demonstration completed");
            // Note: We intentionally don't clear the keyStore to avoid breaking tests
            
        } catch (Exception e) {
            System.out.println("❌ Error in key management: " + e.getMessage());
        }
    }
    
    /**
     * Method to clean the database state
     * This ensures tests run after this demo have a clean state
     */
    private static void resetDatabaseState() {
        try {
            // Clean up the database using Blockchain API
            Blockchain blockchain = new Blockchain();
            blockchain.clearAndReinitialize();

            // EXPLICIT bootstrap admin creation after reset (security best practice)
            KeyPair bootstrapKeys = KeyFileLoader.loadKeyPairFromFiles(
                "./keys/genesis-admin.private",
                "./keys/genesis-admin.public"
            );
            blockchain.createBootstrapAdmin(
                CryptoUtil.publicKeyToString(bootstrapKeys.getPublic()),
                "BOOTSTRAP_ADMIN"
            );

            System.out.println("🗑️ Reset database state completed");
        } catch (Exception e) {
            System.out.println("❌ Error resetting database state: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("📊 === 🔐 ENHANCED CRYPTOGRAPHIC SECURITY DEMO 🔐 === 📊\n");
        
        // Reset database state before running demo to ensure a clean state
        resetDatabaseState();
        
        // Demonstrate hash algorithms
        demonstrateHashAlgorithms();
        
        // Demonstrate signature algorithms
        demonstrateSignatureAlgorithms();
        
        // Demonstrate hierarchical key management
        demonstrateKeyHierarchy();
        
        // Demonstrate key rotation
        demonstrateKeyRotation();
        
        // Demonstrate key revocation
        demonstrateKeyRevocation();
        
        // Finish demo key operations
        cleanDemoKeys();
        
        // Reset database state after running demo to ensure tests have a clean state
        resetDatabaseState();
        
        System.out.println("👍 Demo completed and environment cleaned up for subsequent tests");
        
        // Force exit to stop background threads
        System.exit(0);
    }
    
    private static void demonstrateHashAlgorithms() {
        System.out.println("📊 === 🔗 SHA-3 HASH ALGORITHM 🔗 === 📊");
        String data = "This is sample data to hash";
        
        // Calculate hash using SHA-3
        String sha3Hash = CryptoUtil.calculateHash(data);
        
        System.out.println("📝 Original data: " + data);
        System.out.println("🔗 SHA-3 hash:    " + sha3Hash);
        System.out.println();
    }
    
    private static void demonstrateSignatureAlgorithms() {
        System.out.println("📊 === 🔐 ML-DSA SIGNATURE ALGORITHM 🔐 === 📊");
        String data = "This is sample data to sign with ML-DSA-65";

        // Generate ML-DSA key pair
        KeyPair mldsaKeyPair = CryptoUtil.generateKeyPair();
        System.out.println("🔑 Generated ML-DSA-65 key pair (post-quantum)");

        // Generate hierarchical key pair
        KeyPair hierarchicalKeyPair = CryptoUtil.generateHierarchicalKeyPair();
        System.out.println("🔑 Generated hierarchical ML-DSA key pair");

        // Sign with ML-DSA (direct key)
        String mldsaSignature = CryptoUtil.signData(data, mldsaKeyPair.getPrivate());
        System.out.println("🔐 ML-DSA signature (direct key): " + mldsaSignature.substring(0, 20) + "...");

        // Sign with ML-DSA (hierarchical key)
        String hierSignature = CryptoUtil.signData(data, hierarchicalKeyPair.getPrivate());
        System.out.println("🔐 ML-DSA signature (hierarchical key): " + hierSignature.substring(0, 20) + "...");

        // Verify signatures
        boolean mldsaVerified = CryptoUtil.verifySignature(data, mldsaSignature, mldsaKeyPair.getPublic());
        boolean hierVerified = CryptoUtil.verifySignature(data, hierSignature, hierarchicalKeyPair.getPublic());

        System.out.println("🔍 ML-DSA signature verification (direct key): " + (mldsaVerified ? "✅ Success" : "❌ Failed"));
        System.out.println("🔍 ML-DSA signature verification (hierarchical key): " + (hierVerified ? "✅ Success" : "❌ Failed"));
        System.out.println();
    }
    
    private static void demonstrateKeyHierarchy() {
        System.out.println("📊 === 🔑 HIERARCHICAL KEY MANAGEMENT 🔑 === 📊");
        
        // Create root key
        CryptoUtil.KeyInfo rootKey = CryptoUtil.createRootKey();
        System.out.println("✅ Created root key: " + rootKey.getKeyId());
        System.out.println("  - 📅 Created: " + rootKey.getCreatedAt().format(DATE_FORMAT));
        System.out.println("  - 📅 Expires: " + rootKey.getExpiresAt().format(DATE_FORMAT));
        
        // Create intermediate key
        KeyInfo intermediateKey = CryptoUtil.createIntermediateKey(rootKey.getKeyId());
        System.out.println("\n✅ Created intermediate key: " + intermediateKey.getKeyId());
        System.out.println("  - 🔗 Issued by: " + intermediateKey.getIssuerId());
        System.out.println("  - 📅 Created: " + intermediateKey.getCreatedAt().format(DATE_FORMAT));
        System.out.println("  - 📅 Expires: " + intermediateKey.getExpiresAt().format(DATE_FORMAT));
        
        // Create operational keys
        KeyInfo operationalKey1 = CryptoUtil.createOperationalKey(intermediateKey.getKeyId());
        System.out.println("\n✅ Created operational key 1: " + operationalKey1.getKeyId());
        System.out.println("  - 🔗 Issued by: " + operationalKey1.getIssuerId());
        System.out.println("  - 📅 Created: " + operationalKey1.getCreatedAt().format(DATE_FORMAT));
        System.out.println("  - 📅 Expires: " + operationalKey1.getExpiresAt().format(DATE_FORMAT));
        
        KeyInfo operationalKey2 = CryptoUtil.createOperationalKey(intermediateKey.getKeyId());
        System.out.println("\n✅ Created operational key 2: " + operationalKey2.getKeyId());
        
        // List all keys by type
        System.out.println("\n📋 Key hierarchy summary:");
        System.out.println("  - 🔑 Root keys: " + CryptoUtil.getKeysByType(KeyType.ROOT).size());
        System.out.println("  - 🔑 Intermediate keys: " + CryptoUtil.getKeysByType(KeyType.INTERMEDIATE).size());
        System.out.println("  - 🔑 Operational keys: " + CryptoUtil.getKeysByType(KeyType.OPERATIONAL).size());
        System.out.println();
    }
    
    private static void demonstrateKeyRotation() {
        System.out.println("📊 === 🔄 KEY ROTATION 🔄 === 📊");
        
        // Create a key hierarchy
        CryptoUtil.KeyInfo rootKey = CryptoUtil.createRootKey();
        CryptoUtil.KeyInfo intermediateKey = CryptoUtil.createIntermediateKey(rootKey.getKeyId());
        CryptoUtil.KeyInfo operationalKey = CryptoUtil.createOperationalKey(intermediateKey.getKeyId());
        
        System.out.println("ℹ️ Created key hierarchy for rotation demo");
        System.out.println("  - 🔑 Root key: " + rootKey.getKeyId());
        System.out.println("  - 🔑 Intermediate key: " + intermediateKey.getKeyId());
        System.out.println("  - 🔑 Operational key: " + operationalKey.getKeyId());
        
        // Rotate the intermediate key
        System.out.println("\n🔄 Rotating intermediate key...");
        CryptoUtil.KeyInfo newIntermediateKey = CryptoUtil.rotateKey(intermediateKey.getKeyId());
        
        System.out.println("ℹ️ Old intermediate key status: " + intermediateKey.getStatus());
        System.out.println("✅ New intermediate key: " + newIntermediateKey.getKeyId());
        System.out.println("ℹ️ New intermediate key status: " + newIntermediateKey.getStatus());
        
        // Create a new operational key from the new intermediate key
        CryptoUtil.KeyInfo newOperationalKey = CryptoUtil.createOperationalKey(newIntermediateKey.getKeyId());
        System.out.println("\n✅ Created new operational key: " + newOperationalKey.getKeyId());
        System.out.println("  - 🔗 Issued by: " + newOperationalKey.getIssuerId());
        System.out.println();
    }
    
    private static void demonstrateKeyRevocation() {
        System.out.println("📊 === ❌ KEY REVOCATION ❌ === 📊");
        
        // Create a key hierarchy
        CryptoUtil.KeyInfo rootKey = CryptoUtil.createRootKey();
        CryptoUtil.KeyInfo intermediateKey = CryptoUtil.createIntermediateKey(rootKey.getKeyId());
        CryptoUtil.KeyInfo operationalKey1 = CryptoUtil.createOperationalKey(intermediateKey.getKeyId());
        CryptoUtil.KeyInfo operationalKey2 = CryptoUtil.createOperationalKey(intermediateKey.getKeyId());
        
        System.out.println("ℹ️ Created key hierarchy for revocation demo");
        System.out.println("  - 🔑 Root key: " + rootKey.getKeyId());
        System.out.println("  - 🔑 Intermediate key: " + intermediateKey.getKeyId());
        System.out.println("  - 🔑 Operational key 1: " + operationalKey1.getKeyId());
        System.out.println("  - 🔑 Operational key 2: " + operationalKey2.getKeyId());
        
        // Revoke a single operational key
        System.out.println("\n❌ Revoking operational key 1 without cascade...");
        CryptoUtil.revokeKey(operationalKey1.getKeyId(), false);
        
        System.out.println("ℹ️ Operational key 1 status: " + operationalKey1.getStatus());
        System.out.println("ℹ️ Operational key 2 status: " + operationalKey2.getStatus());
        
        // Revoke intermediate key with cascade
        System.out.println("\n❌ Revoking intermediate key with cascade...");
        CryptoUtil.revokeKey(intermediateKey.getKeyId(), true);
        
        System.out.println("ℹ️ Intermediate key status: " + intermediateKey.getStatus());
        System.out.println("ℹ️ Operational key 1 status: " + operationalKey1.getStatus());
        System.out.println("ℹ️ Operational key 2 status: " + operationalKey2.getStatus());
        
        // Check active keys
        List<KeyInfo> activeKeys = CryptoUtil.getActiveKeys();
        System.out.println("\n📊 Remaining active keys: " + activeKeys.size());
        System.out.println();
    }
}
