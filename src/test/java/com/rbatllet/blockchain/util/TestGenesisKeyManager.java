package com.rbatllet.blockchain.util;

import com.rbatllet.blockchain.security.KeyFileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.security.KeyPair;

/**
 * Test utility for managing genesis-admin keys.
 *
 * <p><strong>⚠️ SECURITY WARNING: ONLY FOR TESTS!</strong></p>
 *
 * <p>This class automatically generates genesis-admin keys if they don't exist,
 * but ONLY in test context. This is safe for tests because:
 * <ul>
 *   <li>Tests use temporary databases (H2 in-memory or test-specific SQLite)</li>
 *   <li>Tests don't persist state between runs</li>
 *   <li>Keys are generated deterministically for each test suite</li>
 * </ul>
 *
 * <p><strong>❌ NEVER USE IN PRODUCTION CODE!</strong></p>
 * <p>In production/development, keys must be generated manually via:
 * {@code ./tools/generate_genesis_keys.zsh}
 *
 * <p>Automatic key generation in production would cause:
 * <ul>
 *   <li>Loss of access to existing blockchain if keys are deleted</li>
 *   <li>Security breach (keys should be sacred and consciously generated)</li>
 *   <li>Non-determinism (each generation creates different keys)</li>
 * </ul>
 *
 * @since 1.0.6
 */
public class TestGenesisKeyManager {

    private static final Logger logger = LoggerFactory.getLogger(TestGenesisKeyManager.class);

    private static final String GENESIS_PRIVATE_KEY_PATH = "./keys/genesis-admin.private";
    private static final String GENESIS_PUBLIC_KEY_PATH = "./keys/genesis-admin.public";

    // Singleton pattern to avoid multiple generations
    private static KeyPair cachedGenesisKeyPair = null;

    /**
     * Ensures genesis-admin keys exist, generating them if necessary.
     *
     * <p><strong>⚠️ TEST-ONLY METHOD</strong></p>
     *
     * <p>This method checks if genesis-admin keys exist at ./keys/genesis-admin.*
     * If they don't exist, it generates new keys automatically.
     *
     * <p><strong>Thread-safe:</strong> Uses synchronized block to prevent race conditions
     * when multiple tests run in parallel.
     *
     * <p><strong>Idempotent:</strong> Safe to call multiple times (uses cached keys).
     *
     * <p><strong>Usage in tests:</strong>
     * <pre>{@code
     * @BeforeAll
     * static void setUpClass() {
     *     TestGenesisKeyManager.ensureGenesisKeysExist();
     * }
     * }</pre>
     *
     * @return KeyPair containing the genesis-admin keys
     * @throws RuntimeException if key generation or loading fails
     */
    public static synchronized KeyPair ensureGenesisKeysExist() {
        // Return cached keys if already loaded
        if (cachedGenesisKeyPair != null) {
            logger.debug("🔑 Using cached genesis-admin keys");
            return cachedGenesisKeyPair;
        }

        File privateKeyFile = new File(GENESIS_PRIVATE_KEY_PATH);
        File publicKeyFile = new File(GENESIS_PUBLIC_KEY_PATH);

        // Check if keys already exist
        if (privateKeyFile.exists() && publicKeyFile.exists()) {
            logger.info("🔑 Genesis-admin keys found at ./keys/");
            cachedGenesisKeyPair = loadExistingKeys();
            return cachedGenesisKeyPair;
        }

        // Keys don't exist - generate them (TEST-ONLY!)
        logger.warn("⚠️  Genesis-admin keys not found. Generating new keys (TEST CONTEXT ONLY)");
        logger.warn("⚠️  If you're in production, use: ./tools/generate_genesis_keys.zsh");

        cachedGenesisKeyPair = generateAndSaveKeys();
        return cachedGenesisKeyPair;
    }

    /**
     * Load existing genesis-admin keys from ./keys/ directory.
     *
     * @return KeyPair loaded from files
     * @throws RuntimeException if loading fails
     */
    private static KeyPair loadExistingKeys() {
        try {
            KeyPair keyPair = KeyFileLoader.loadKeyPairFromFiles(
                GENESIS_PRIVATE_KEY_PATH,
                GENESIS_PUBLIC_KEY_PATH
            );

            if (keyPair == null) {
                throw new RuntimeException(
                    "Failed to load genesis-admin keys from " + GENESIS_PRIVATE_KEY_PATH
                );
            }

            logger.debug("✅ Successfully loaded genesis-admin keys");
            return keyPair;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load genesis-admin keys", e);
        }
    }

    /**
     * Generate new genesis-admin keys and save to ./keys/ directory.
     *
     * <p><strong>⚠️ PRIVATE METHOD - ONLY CALLED FROM ensureGenesisKeysExist()</strong></p>
     *
     * @return KeyPair newly generated keys
     * @throws RuntimeException if generation or saving fails
     */
    private static KeyPair generateAndSaveKeys() {
        try {
            // Create ./keys/ directory if it doesn't exist
            File keysDir = new File("./keys");
            if (!keysDir.exists()) {
                if (!keysDir.mkdirs()) {
                    throw new RuntimeException("Failed to create ./keys/ directory");
                }
                logger.info("📁 Created ./keys/ directory");
            }

            // Generate new key pair using Dilithium (ML-DSA-87)
            logger.info("🔑 Generating new genesis-admin key pair (Dilithium ML-DSA-87)...");
            KeyPair keyPair = CryptoUtil.generateKeyPair();

            // Save to files
            boolean saved = KeyFileLoader.saveKeyPairToFiles(
                keyPair,
                GENESIS_PRIVATE_KEY_PATH,
                GENESIS_PUBLIC_KEY_PATH
            );

            if (!saved) {
                throw new RuntimeException("Failed to save genesis-admin keys to ./keys/");
            }

            logger.info("✅ Genesis-admin keys generated and saved successfully!");
            logger.info("   📂 Private key: {}", GENESIS_PRIVATE_KEY_PATH);
            logger.info("   📂 Public key:  {}", GENESIS_PUBLIC_KEY_PATH);

            return keyPair;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate genesis-admin keys", e);
        }
    }

    /**
     * Clear cached keys (useful for tests that need to reload keys).
     *
     * <p><strong>Note:</strong> This only clears the in-memory cache, not the files.
     */
    public static synchronized void clearCache() {
        cachedGenesisKeyPair = null;
        logger.debug("🧹 Cleared genesis-admin key cache");
    }

    /**
     * Check if genesis-admin keys exist on disk.
     *
     * @return true if both private and public key files exist
     */
    public static boolean keysExist() {
        File privateKeyFile = new File(GENESIS_PRIVATE_KEY_PATH);
        File publicKeyFile = new File(GENESIS_PUBLIC_KEY_PATH);
        return privateKeyFile.exists() && publicKeyFile.exists();
    }
}
