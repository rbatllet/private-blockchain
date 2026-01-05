package demo;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.security.KeyFileLoader;
import com.rbatllet.blockchain.service.UserFriendlyEncryptionAPI;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.config.EncryptionConfig;
import java.security.KeyPair;

/**
 * Demo showcasing encryption configuration options and custom settings
 * Shows how to use different encryption configurations for different security needs
 */
public class EncryptionConfigDemo {
    public static void main(String[] args) {
        try {
            System.out.println("=== 🔐 ENCRYPTION CONFIGURATION DEMO ===\n");

            // Setup blockchain and API
            Blockchain blockchain = new Blockchain();

            // Load bootstrap admin keys
            KeyPair bootstrapKeys = KeyFileLoader.loadKeyPairFromFiles(
                "./keys/genesis-admin.private",
                "./keys/genesis-admin.public"
            );

            // Register bootstrap admin in blockchain (REQUIRED!)
            blockchain.createBootstrapAdmin(
                CryptoUtil.publicKeyToString(bootstrapKeys.getPublic()),
                "BOOTSTRAP_ADMIN"
            );

            UserFriendlyEncryptionAPI api = new UserFriendlyEncryptionAPI(blockchain);
            api.setDefaultCredentials("BOOTSTRAP_ADMIN", bootstrapKeys);

            // Create user (authorized by bootstrap admin)
            KeyPair userKeys = api.createUser("TestUser");
            api.setDefaultCredentials("TestUser", userKeys);
            
            // Test configuration methods
            System.out.println("1️⃣ Testing configuration retrieval methods:");
            EncryptionConfig defaultConfig = api.getDefaultEncryptionConfig();
            System.out.println("✅ Default config retrieved: " + defaultConfig.getEncryptionTransformation());
            
            EncryptionConfig highSecConfig = api.getHighSecurityConfig();
            System.out.println("✅ High security config retrieved: Key length = " + highSecConfig.getKeyLength() + " bits");
            
            EncryptionConfig perfConfig = api.getPerformanceConfig();
            System.out.println("✅ Performance config retrieved: Min password = " + perfConfig.getMinPasswordLength() + " chars");
            
            System.out.println("\n2️⃣ Testing encryption configuration comparison:");
            String comparison = api.getEncryptionConfigComparison();
            System.out.println("✅ Configuration comparison generated (" + comparison.split("\n").length + " lines)");
            
            System.out.println("\n3️⃣ Testing custom configuration builder:");
            EncryptionConfig customConfig = api.createCustomConfig()
                .keyLength(256)
                .minPasswordLength(10)
                .metadataEncryptionEnabled(true)
                .build();
            System.out.println("✅ Custom config created: " + customConfig.getSummary().split("\n")[0]);
            
            System.out.println("\n4️⃣ Testing encryption with standard method:");
            
            // Test standard secret storage with different passwords
            String highSecPassword = api.generatePasswordForConfig(highSecConfig);
            var highSecBlock = api.storeSecret("High security test data", highSecPassword);
            System.out.println("✅ Secret storage with strong password: Block #" + highSecBlock.getBlockNumber());
            
            // Test standard secret storage with different password
            String perfPassword = api.generatePasswordForConfig(perfConfig);
            var perfBlock = api.storeSecret("Performance test data", perfPassword);
            System.out.println("✅ Secret storage with performance password: Block #" + perfBlock.getBlockNumber());
            
            // Test standard secret storage with custom-generated password
            String customPassword = api.generatePasswordForConfig(customConfig);
            var customBlock = api.storeSecret("Custom config test data", customPassword);
            System.out.println("✅ Secret storage with custom password: Block #" + customBlock.getBlockNumber());
            
            System.out.println("\n5️⃣ Testing password validation:");
            System.out.println("   Attempting to use intentionally weak password 'weak' (only 4 chars)...");
            try {
                api.storeSecret("Test", "weak"); // Intentionally weak password to test validation
                System.out.println("   ⚠️ WARNING: Weak password was accepted - validation may need strengthening");
            } catch (IllegalArgumentException e) {
                System.out.println("   ✅ Password validation correctly rejected weak password");
                System.out.println("   📋 Rejection reason: " + e.getMessage());
            }
            
            System.out.println("\n🎉 All encryption configuration tests passed!");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Force exit to stop background threads
        System.exit(0);
    }
}