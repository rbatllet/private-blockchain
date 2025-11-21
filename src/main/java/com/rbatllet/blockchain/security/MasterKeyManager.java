package com.rbatllet.blockchain.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

/**
 * Blockchain Master Encryption Key (BMEK) Manager
 *
 * <p>Manages the master encryption key used for encrypting all blockchain data.
 * This key is INDEPENDENT of user keys and SUPER_ADMIN keys.</p>
 *
 * <h2>Key Characteristics:</h2>
 * <ul>
 *   <li><strong>Generated once</strong> at blockchain initialization</li>
 *   <li><strong>Immutable</strong> - changing it requires re-encrypting entire blockchain</li>
 *   <li><strong>Independent</strong> - not tied to any user or SUPER_ADMIN</li>
 *   <li><strong>AES-256</strong> - symmetric key for fast encryption/decryption</li>
 *   <li><strong>Stored securely</strong> - file permissions 600 (owner read/write only)</li>
 * </ul>
 *
 * <h2>File Location:</h2>
 * <pre>./keys/blockchain-master-key.aes256</pre>
 *
 * <h2>Security Considerations:</h2>
 * <ul>
 *   <li>⚠️ <strong>CRITICAL:</strong> Loss of this key = loss of all encrypted data</li>
 *   <li>✅ Backup this key in multiple secure locations (HSM, vault, offline storage)</li>
 *   <li>✅ Use hardware security module (HSM) in production</li>
 *   <li>✅ Implement key splitting (Shamir's Secret Sharing) for high-security environments</li>
 * </ul>
 *
 * @since 1.0.6
 */
public class MasterKeyManager {

    private static final Logger logger = LoggerFactory.getLogger(MasterKeyManager.class);

    // Key file configuration
    private static final String KEYS_DIRECTORY = "./keys";
    private static final String BMEK_FILENAME = "blockchain-master-key.aes256";
    private static final Path BMEK_PATH = Paths.get(KEYS_DIRECTORY, BMEK_FILENAME);

    // AES-256 configuration
    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE_BITS = 256;
    private static final int KEY_SIZE_BYTES = KEY_SIZE_BITS / 8;

    // Cached key (loaded once, reused for performance)
    private static volatile SecretKey cachedMasterKey = null;
    private static final Object cacheLock = new Object();

    /**
     * Initialize the blockchain master key.
     *
     * <p>If the key file doesn't exist, generates a new AES-256 key and saves it.
     * If the key file exists, loads and validates it.</p>
     *
     * @return true if initialization succeeded
     * @throws RuntimeException if initialization fails
     */
    public static boolean initializeMasterKey() {
        try {
            // Ensure keys directory exists
            File keysDir = new File(KEYS_DIRECTORY);
            if (!keysDir.exists()) {
                if (!keysDir.mkdirs()) {
                    logger.error("❌ Failed to create keys directory: {}", KEYS_DIRECTORY);
                    return false;
                }
                logger.info("📁 Created keys directory: {}", KEYS_DIRECTORY);
            }

            // Check if master key already exists
            if (Files.exists(BMEK_PATH)) {
                logger.info("✅ Blockchain master key already exists: {}", BMEK_PATH);
                // Validate existing key
                SecretKey existingKey = loadMasterKeyFromFile();
                if (existingKey == null) {
                    logger.error("❌ Existing master key is corrupted or invalid");
                    return false;
                }
                logger.info("✅ Blockchain master key validated successfully");
                return true;
            }

            // Generate new master key
            logger.info("🔑 Generating new blockchain master encryption key (AES-256)...");
            SecretKey masterKey = generateNewMasterKey();

            // Save to file
            if (!saveMasterKeyToFile(masterKey)) {
                logger.error("❌ Failed to save master key to file");
                return false;
            }

            logger.info("✅ Blockchain master key generated and saved: {}", BMEK_PATH);
            logger.warn("⚠️⚠️⚠️ CRITICAL: Backup this key immediately!");
            logger.warn("   Location: {}", BMEK_PATH.toAbsolutePath());
            logger.warn("   Loss of this key = loss of all encrypted blockchain data");

            return true;

        } catch (Exception e) {
            logger.error("❌ Failed to initialize blockchain master key", e);
            throw new RuntimeException("Failed to initialize blockchain master key", e);
        }
    }

    /**
     * Get the blockchain master encryption key.
     *
     * <p>Loads the key from file on first call, then caches it in memory for performance.</p>
     *
     * @return The AES-256 master key
     * @throws RuntimeException if key cannot be loaded
     */
    public static SecretKey getMasterKey() {
        // Double-checked locking for thread-safe singleton pattern
        if (cachedMasterKey == null) {
            synchronized (cacheLock) {
                if (cachedMasterKey == null) {
                    cachedMasterKey = loadMasterKeyFromFile();
                    if (cachedMasterKey == null) {
                        throw new RuntimeException(
                            "Blockchain master key not found. Call initializeMasterKey() first."
                        );
                    }
                }
            }
        }
        return cachedMasterKey;
    }

    /**
     * Check if the master key exists.
     *
     * @return true if the master key file exists
     */
    public static boolean masterKeyExists() {
        return Files.exists(BMEK_PATH);
    }

    /**
     * Export the master key as Base64 string (for backup purposes).
     *
     * <p><strong>⚠️ SECURITY WARNING:</strong> This exports the raw key.
     * Only use for secure backup purposes. Never transmit over network or log.</p>
     *
     * @return Base64-encoded master key
     */
    public static String exportMasterKeyBase64() {
        SecretKey masterKey = getMasterKey();
        return Base64.getEncoder().encodeToString(masterKey.getEncoded());
    }

    /**
     * Import master key from Base64 string (for recovery purposes).
     *
     * <p><strong>⚠️ WARNING:</strong> This will overwrite existing master key.
     * All data encrypted with the old key will become inaccessible.</p>
     *
     * @param base64Key Base64-encoded master key
     * @return true if import succeeded
     */
    public static boolean importMasterKeyBase64(String base64Key) {
        try {
            // Decode Base64
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);

            // Validate key length
            if (keyBytes.length != KEY_SIZE_BYTES) {
                logger.error("❌ Invalid key length: expected {} bytes, got {}",
                           KEY_SIZE_BYTES, keyBytes.length);
                return false;
            }

            // Create SecretKey
            SecretKey masterKey = new SecretKeySpec(keyBytes, ALGORITHM);

            // Save to file
            if (!saveMasterKeyToFile(masterKey)) {
                logger.error("❌ Failed to save imported master key");
                return false;
            }

            // Clear cache to force reload
            synchronized (cacheLock) {
                cachedMasterKey = null;
            }

            logger.info("✅ Master key imported successfully");
            return true;

        } catch (Exception e) {
            logger.error("❌ Failed to import master key", e);
            return false;
        }
    }

    // ========================================================================
    // Private Helper Methods
    // ========================================================================

    /**
     * Generate a new AES-256 master key using SecureRandom.
     */
    private static SecretKey generateNewMasterKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        SecureRandom secureRandom = new SecureRandom();
        keyGen.init(KEY_SIZE_BITS, secureRandom);
        return keyGen.generateKey();
    }

    /**
     * Save the master key to file with secure permissions.
     */
    private static boolean saveMasterKeyToFile(SecretKey masterKey) {
        try {
            // Encode key as Base64
            String encodedKey = Base64.getEncoder().encodeToString(masterKey.getEncoded());

            // Write to file
            Files.writeString(BMEK_PATH, encodedKey);

            // Set secure file permissions (Unix-like systems only)
            setSecureFilePermissions(BMEK_PATH);

            return true;

        } catch (IOException e) {
            logger.error("❌ Failed to save master key to file", e);
            return false;
        }
    }

    /**
     * Load the master key from file.
     */
    private static SecretKey loadMasterKeyFromFile() {
        try {
            if (!Files.exists(BMEK_PATH)) {
                logger.error("❌ Master key file not found: {}", BMEK_PATH);
                return null;
            }

            // Read Base64-encoded key
            String encodedKey = Files.readString(BMEK_PATH).trim();

            // Decode Base64
            byte[] keyBytes = Base64.getDecoder().decode(encodedKey);

            // Validate key length
            if (keyBytes.length != KEY_SIZE_BYTES) {
                logger.error("❌ Invalid key length in file: expected {} bytes, got {}",
                           KEY_SIZE_BYTES, keyBytes.length);
                return null;
            }

            // Create SecretKey
            return new SecretKeySpec(keyBytes, ALGORITHM);

        } catch (IOException e) {
            logger.error("❌ Failed to load master key from file", e);
            return null;
        }
    }

    /**
     * Set secure file permissions (600 = owner read/write only).
     * Only works on Unix-like systems. Windows permissions are set differently.
     */
    private static void setSecureFilePermissions(Path filePath) {
        try {
            // Check if file system supports POSIX permissions
            if (filePath.getFileSystem().supportedFileAttributeViews().contains("posix")) {
                Set<PosixFilePermission> permissions = new HashSet<>();
                permissions.add(PosixFilePermission.OWNER_READ);
                permissions.add(PosixFilePermission.OWNER_WRITE);
                Files.setPosixFilePermissions(filePath, permissions);
                logger.info("✅ Set secure file permissions (600) for: {}", filePath);
            } else {
                logger.warn("⚠️ File system does not support POSIX permissions. " +
                          "Ensure file is protected manually: {}", filePath);
            }
        } catch (IOException e) {
            logger.warn("⚠️ Could not set file permissions: {}", e.getMessage());
        }
    }

    /**
     * Clear the cached master key from memory (for testing or security purposes).
     */
    public static void clearCache() {
        synchronized (cacheLock) {
            cachedMasterKey = null;
        }
    }
}
