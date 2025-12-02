package tools;

import com.rbatllet.blockchain.security.KeyFileLoader;
import com.rbatllet.blockchain.util.CryptoUtil;

import java.security.KeyPair;

/**
 * Utility to generate genesis-admin key pair files.
 *
 * This tool generates the bootstrap admin keys required by tests and demos.
 * The keys are saved to ./keys/genesis-admin.private and ./keys/genesis-admin.public
 *
 * Usage:
 *   mvn exec:java -Dexec.mainClass="tools.GenerateGenesisAdminKeys"
 */
public class GenerateGenesisAdminKeys {

    public static void main(String[] args) {
        System.out.println("🔑 Generating genesis admin key pair...");

        try {
            // Generate a new key pair using Dilithium (ML-DSA-87)
            KeyPair genesisKeys = CryptoUtil.generateKeyPair();

            // Save to files
            boolean success = KeyFileLoader.saveKeyPairToFiles(
                genesisKeys,
                "./keys/genesis-admin.private",
                "./keys/genesis-admin.public"
            );

            if (success) {
                System.out.println("✅ Genesis admin keys generated successfully!");
                System.out.println("   📂 Private key: ./keys/genesis-admin.private");
                System.out.println("   📂 Public key:  ./keys/genesis-admin.public");
            } else {
                System.err.println("❌ Failed to save genesis admin keys");
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("❌ Error generating keys: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
