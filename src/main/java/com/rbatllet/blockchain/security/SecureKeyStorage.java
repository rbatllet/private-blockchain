package com.rbatllet.blockchain.security;

import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.PathSecurityUtil;
import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.AuthorizedKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.MessageDigest;

import static com.rbatllet.blockchain.util.CryptoUtil.getSecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.KeyFactory;
import java.util.Arrays;
import java.util.Base64;

/**
 * Secure storage for private keys using AES-256-GCM encryption
 * Uses PBKDF2WithHmacSHA512 for quantum-resistant password-based key derivation
 */
public class SecureKeyStorage {

    private static final Logger logger = LoggerFactory.getLogger(SecureKeyStorage.class);

    private static final String KEYS_DIRECTORY = "keys";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_EXTENSION = ".key";
    private static final String KEYPAIR_EXTENSION = ".keypair"; // For ML-DSA KeyPairs (public + private)
    private static final int GCM_IV_LENGTH = 12; // 96-bit IV recommended for GCM
    private static final int GCM_TAG_LENGTH = 16; // 128-bit authentication tag

    // Key derivation constants from KeyDerivationUtil
    private static final int SALT_LENGTH = KeyDerivationUtil.getDefaultSaltLength();

    /**
     * Constant-time string comparison to prevent timing attacks.
     *
     * <p>Uses XOR-based comparison that always processes all characters,
     * preventing attackers from measuring response times to guess secrets.</p>
     *
     * @param a first string
     * @param b second string
     * @return true if strings are equal, false otherwise
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }

        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(aBytes, bBytes);
    }

    /**
     * Get the keys directory, respecting user.dir system property for test isolation.
     *
     * @return File object representing the keys directory
     */
    private static File getKeysDirectory() {
        String baseDir = System.getProperty("user.dir", ".");
        return new File(baseDir, KEYS_DIRECTORY);
    }

    /**
     * Get the full path to a key file, respecting user.dir system property.
     *
     * @param ownerName Owner identifier
     * @param extension File extension (.key or .keypair)
     * @return Full path to the key file
     */
    private static String getKeyFilePath(String ownerName, String extension) {
        return new File(getKeysDirectory(), ownerName.trim() + extension).getPath();
    }

    /**
     * Save a private key encrypted with password using AES-256-GCM
     */
    public static boolean savePrivateKey(String ownerName, PrivateKey privateKey, String password) {
        try {
            // Validate inputs
            if (ownerName == null || ownerName.trim().isEmpty() ||
                privateKey == null || password == null || password.trim().isEmpty()) {
                return false;
            }

            // Create directory if it doesn't exist
            File keysDir = getKeysDirectory();
            if (!keysDir.exists()) {
                keysDir.mkdirs();
            }

            // Generate random salt for key derivation
            byte[] salt = KeyDerivationUtil.generateSalt();

            // Derive secret key from password using PBKDF2WithHmacSHA512
            SecretKeySpec secretKey = KeyDerivationUtil.deriveSecretKey(password, salt);

            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            getSecureRandom().nextBytes(iv);

            // Encrypt the private key with AES-256-GCM
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            byte[] privateKeyBytes = privateKey.getEncoded();
            byte[] ciphertext = cipher.doFinal(privateKeyBytes);

            // Combine IV + salt + ciphertext (includes auth tag)
            // New format: [12 bytes IV][16 bytes salt][ciphertext]
            byte[] combined = new byte[iv.length + SALT_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(salt, 0, combined, iv.length, SALT_LENGTH);
            System.arraycopy(ciphertext, 0, combined, iv.length + SALT_LENGTH, ciphertext.length);

            // Save to file
            String fileName = getKeyFilePath(ownerName, KEY_EXTENSION);
            String encodedKey = Base64.getEncoder().encodeToString(combined);
            Files.write(Paths.get(fileName), encodedKey.getBytes(StandardCharsets.UTF_8));

            // Clear sensitive data
            Arrays.fill(salt, (byte) 0);
            Arrays.fill(privateKeyBytes, (byte) 0);

            return true;

        } catch (Exception e) {
            logger.error("❌ Error saving private key: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Load a private key by decrypting with password using AES-256-GCM.
     *
     * <p><strong>OWASP-Compliant Constant-Time Operation:</strong></p>
     * <p>This method implements constant-time execution to prevent timing attacks
     * according to OWASP Authentication Cheat Sheet guidelines:</p>
     * <ul>
     *   <li>No early returns - all code paths execute the full operation</li>
     *   <li>Dummy PBKDF2 operations for invalid inputs to mask failures</li>
     *   <li>No Files.exists() checks - uses try/catch to avoid timing leaks</li>
     *   <li>Single return point after finally block ensures constant-time</li>
     *   <li>Minimum 400ms execution time enforced for all paths</li>
     * </ul>
     *
     * @param ownerName Owner name for key file lookup
     * @param password Password to decrypt the key
     * @return PrivateKey if successful, null otherwise (constant-time in both cases)
     *
     * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html">OWASP Authentication Cheat Sheet</a>
     */
    public static PrivateKey loadPrivateKey(String ownerName, String password) {
        long startTime = System.nanoTime();
        PrivateKey result = null;  // Single return point

        // Reusable arrays for cleanup
        byte[] dummySalt = null;
        byte[] fileSalt = null;
        byte[] decryptedKey = null;

        try {
            // OWASP: Validate inputs but DON'T return early
            boolean validInputs = (ownerName != null && !ownerName.trim().isEmpty() &&
                                   password != null && !password.trim().isEmpty());

            if (!validInputs) {
                // OWASP: Perform dummy PBKDF2 operation for constant-time
                // This ensures invalid inputs take same time as valid ones
                dummySalt = KeyDerivationUtil.generateSalt();
                KeyDerivationUtil.deriveSecretKey(password != null ? password : "", dummySalt);
                // Continue to finally - NO early return!
            } else {
                String fileName = getKeyFilePath(ownerName, KEY_EXTENSION);
                byte[] fileBytes = null;

                // OWASP: Use try/catch instead of Files.exists() to prevent timing leak
                try {
                    fileBytes = Files.readAllBytes(Paths.get(fileName));
                } catch (IOException ioException) {
                    // File not found or read error
                    // OWASP: Perform dummy PBKDF2 to mask file absence
                    dummySalt = KeyDerivationUtil.generateSalt();
                    KeyDerivationUtil.deriveSecretKey(password, dummySalt);
                    // Continue to finally - NO early return!
                }

                // Process file content if available
                if (fileBytes != null && fileBytes.length > 0) {
                    try {
                        String encodedKey = new String(fileBytes, StandardCharsets.UTF_8);
                        byte[] combined = Base64.getDecoder().decode(encodedKey);

                        // Validate format
                        if (combined.length >= GCM_IV_LENGTH + SALT_LENGTH + GCM_TAG_LENGTH) {
                            // Extract IV and salt from file
                            byte[] iv = new byte[GCM_IV_LENGTH];
                            fileSalt = new byte[SALT_LENGTH];
                            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH - SALT_LENGTH];

                            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
                            System.arraycopy(combined, GCM_IV_LENGTH, fileSalt, 0, SALT_LENGTH);
                            System.arraycopy(combined, GCM_IV_LENGTH + SALT_LENGTH, ciphertext, 0, ciphertext.length);

                            // Derive secret key from password using PBKDF2WithHmacSHA512
                            SecretKeySpec secretKey = KeyDerivationUtil.deriveSecretKey(password, fileSalt);

                            // Decrypt with AES-256-GCM
                            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
                            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

                            decryptedKey = cipher.doFinal(ciphertext);

                            // Reconstruct PrivateKey
                            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decryptedKey);
                            KeyFactory keyFactory = KeyFactory.getInstance(CryptoUtil.SIGNATURE_ALGORITHM);

                            result = keyFactory.generatePrivate(keySpec);
                        } else {
                            // Invalid format - perform dummy PBKDF2 for constant-time
                            dummySalt = KeyDerivationUtil.generateSalt();
                            KeyDerivationUtil.deriveSecretKey(password, dummySalt);
                        }
                    } catch (Exception decryptException) {
                        // Decryption failed (wrong password, corrupted file, etc.)
                        // OWASP: Don't log specific errors to avoid information leak
                        // Already did PBKDF2, so timing is consistent
                        // Continue to finally
                    }
                }
            }

        } catch (Exception e) {
            // OWASP: Catch-all for any unexpected errors
            // Don't print password-related errors to avoid security issues
            // Continue to finally - NO early return!
        } finally {
            // OWASP: Clear all sensitive data
            if (dummySalt != null) {
                Arrays.fill(dummySalt, (byte) 0);
            }
            if (fileSalt != null) {
                Arrays.fill(fileSalt, (byte) 0);
            }
            if (decryptedKey != null) {
                Arrays.fill(decryptedKey, (byte) 0);
            }

            // OWASP: CRITICAL - Constant-time enforcement
            // ALL code paths MUST pass through here and take minimum 400ms
            // This prevents attackers from distinguishing:
            //   - Valid vs invalid username/password
            //   - File exists vs doesn't exist
            //   - Decryption success vs failure
            //   - Different exception types
            //
            // Timing breakdown:
            //   - PBKDF2WithHmacSHA512 (210k iterations): ~200-500ms (machine-dependent)
            //   - AES-256-GCM decryption: ~5-20ms
            //   - File I/O + overhead: ~10-50ms
            //   - Total expected: ~215-570ms on typical hardware
            //
            // Setting minTimeMs=400ms ensures:
            //   - Fast machines (PBKDF2 < 300ms) get padded to 400ms
            //   - Slow machines (PBKDF2 > 400ms) don't add excessive delay
            //   - All execution paths appear identical to timing analysis
            long minTimeMs = 400;
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
            if (elapsedMs < minTimeMs) {
                try {
                    Thread.sleep(minTimeMs - elapsedMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // OWASP: Single return point - ensures finally always executes
        return result;
    }
    
    /**
     * Check if a private key exists for an owner
     */
    public static boolean hasPrivateKey(String ownerName) {
        if (ownerName == null || ownerName.trim().isEmpty()) {
            return false;
        }
        String fileName = getKeyFilePath(ownerName, KEY_EXTENSION);
        return Files.exists(Paths.get(fileName));
    }
    
    /**
     * Delete a private key file
     */
    public static boolean deletePrivateKey(String ownerName) {
        try {
            if (ownerName == null || ownerName.trim().isEmpty()) {
                return false;
            }
            String fileName = getKeyFilePath(ownerName, KEY_EXTENSION);
            return Files.deleteIfExists(Paths.get(fileName));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Save a complete KeyPair (public + private) encrypted with password using AES-256-GCM
     * Required for ML-DSA where public keys cannot be derived from private keys
     *
     * @param ownerName Owner identifier (username)
     * @param keyPair Complete KeyPair (public + private)
     * @param password Password for encryption
     * @return true if saved successfully, false otherwise
     */
    public static boolean saveKeyPair(String ownerName, KeyPair keyPair, String password) {
        try {
            // Validate inputs
            if (ownerName == null || ownerName.trim().isEmpty() ||
                keyPair == null || keyPair.getPrivate() == null || keyPair.getPublic() == null ||
                password == null || password.trim().isEmpty()) {
                return false;
            }

            // Create directory if it doesn't exist
            File keysDir = getKeysDirectory();
            if (!keysDir.exists()) {
                keysDir.mkdirs();
            }

            // Serialize both keys
            byte[] privateKeyBytes = keyPair.getPrivate().getEncoded(); // PKCS#8 format
            byte[] publicKeyBytes = keyPair.getPublic().getEncoded();   // X.509 format

            // Create combined payload: [4 bytes: private length][private bytes][public bytes]
            byte[] combined = new byte[4 + privateKeyBytes.length + publicKeyBytes.length];

            // Write private key length (big-endian)
            combined[0] = (byte) (privateKeyBytes.length >> 24);
            combined[1] = (byte) (privateKeyBytes.length >> 16);
            combined[2] = (byte) (privateKeyBytes.length >> 8);
            combined[3] = (byte) privateKeyBytes.length;

            // Copy private and public keys
            System.arraycopy(privateKeyBytes, 0, combined, 4, privateKeyBytes.length);
            System.arraycopy(publicKeyBytes, 0, combined, 4 + privateKeyBytes.length, publicKeyBytes.length);

            // Generate random salt for key derivation
            byte[] salt = KeyDerivationUtil.generateSalt();

            // Derive secret key from password using PBKDF2WithHmacSHA512
            SecretKeySpec secretKey = KeyDerivationUtil.deriveSecretKey(password, salt);

            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            getSecureRandom().nextBytes(iv);

            // Encrypt with AES-256-GCM
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            byte[] ciphertext = cipher.doFinal(combined);

            // Combine IV + salt + ciphertext
            // New format: [12 bytes IV][16 bytes salt][ciphertext]
            byte[] encrypted = new byte[iv.length + SALT_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, encrypted, 0, iv.length);
            System.arraycopy(salt, 0, encrypted, iv.length, SALT_LENGTH);
            System.arraycopy(ciphertext, 0, encrypted, iv.length + SALT_LENGTH, ciphertext.length);

            // Save to file with .keypair extension
            String fileName = getKeyFilePath(ownerName, KEYPAIR_EXTENSION);
            String encodedKey = Base64.getEncoder().encodeToString(encrypted);
            Files.write(Paths.get(fileName), encodedKey.getBytes(StandardCharsets.UTF_8));

            // Clear sensitive data
            Arrays.fill(salt, (byte) 0);
            Arrays.fill(privateKeyBytes, (byte) 0);
            Arrays.fill(combined, (byte) 0);

            return true;

        } catch (Exception e) {
            logger.error("❌ Error saving KeyPair: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Load a complete KeyPair (public + private) by decrypting with password
     * Required for ML-DSA where public keys cannot be derived from private keys
     *
     * @param ownerName Owner identifier (username)
     * @param password Password for decryption
     * @return Complete KeyPair if successful, null otherwise
     */
    public static KeyPair loadKeyPair(String ownerName, String password) {
        try {
            // Validate inputs
            if (ownerName == null || ownerName.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
                return null;
            }

            // Read encrypted file
            String fileName = getKeyFilePath(ownerName, KEYPAIR_EXTENSION);
            if (!Files.exists(Paths.get(fileName))) {
                return null;
            }

            String encodedKey = new String(Files.readAllBytes(Paths.get(fileName)), StandardCharsets.UTF_8);
            byte[] encrypted = Base64.getDecoder().decode(encodedKey);

            // New format: IV (12 bytes) + salt (16 bytes) + ciphertext
            if (encrypted.length < GCM_IV_LENGTH + SALT_LENGTH + GCM_TAG_LENGTH) {
                return null;
            }

            // Extract IV and salt
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] salt = new byte[SALT_LENGTH];
            byte[] ciphertext = new byte[encrypted.length - GCM_IV_LENGTH - SALT_LENGTH];
            System.arraycopy(encrypted, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encrypted, GCM_IV_LENGTH, salt, 0, SALT_LENGTH);
            System.arraycopy(encrypted, GCM_IV_LENGTH + SALT_LENGTH, ciphertext, 0, ciphertext.length);

            // Derive secret key from password using PBKDF2WithHmacSHA512
            SecretKeySpec secretKey = KeyDerivationUtil.deriveSecretKey(password, salt);

            // Decrypt with AES-256-GCM
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            byte[] decrypted = cipher.doFinal(ciphertext);

            if (decrypted.length < 4) {
                return null;
            }

            // Read private key length (big-endian)
            int privateKeyLength = ((decrypted[0] & 0xFF) << 24) |
                                   ((decrypted[1] & 0xFF) << 16) |
                                   ((decrypted[2] & 0xFF) << 8) |
                                   (decrypted[3] & 0xFF);

            if (4 + privateKeyLength > decrypted.length) {
                return null;
            }

            // Extract private and public key bytes
            byte[] privateKeyBytes = new byte[privateKeyLength];
            byte[] publicKeyBytes = new byte[decrypted.length - 4 - privateKeyLength];
            System.arraycopy(decrypted, 4, privateKeyBytes, 0, privateKeyLength);
            System.arraycopy(decrypted, 4 + privateKeyLength, publicKeyBytes, 0, publicKeyBytes.length);

            // Reconstruct keys
            KeyFactory keyFactory = KeyFactory.getInstance(CryptoUtil.SIGNATURE_ALGORITHM);

            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            // Clear sensitive data
            Arrays.fill(salt, (byte) 0);
            Arrays.fill(decrypted, (byte) 0);
            Arrays.fill(privateKeyBytes, (byte) 0);

            return new KeyPair(publicKey, privateKey);

        } catch (Exception e) {
            // Don't print password-related errors to avoid security issues
            return null;
        }
    }

    /**
     * Check if a KeyPair exists for an owner
     *
     * @param ownerName Owner identifier (username)
     * @return true if KeyPair file exists, false otherwise
     */
    public static boolean hasKeyPair(String ownerName) {
        if (ownerName == null || ownerName.trim().isEmpty()) {
            return false;
        }
        String fileName = getKeyFilePath(ownerName, KEYPAIR_EXTENSION);
        return Files.exists(Paths.get(fileName));
    }

    /**
     * Delete a KeyPair file
     *
     * @param ownerName Owner identifier (username)
     * @return true if file was deleted, false otherwise
     */
    public static boolean deleteKeyPair(String ownerName) {
        try {
            if (ownerName == null || ownerName.trim().isEmpty()) {
                return false;
            }
            String fileName = getKeyFilePath(ownerName, KEYPAIR_EXTENSION);
            return Files.deleteIfExists(Paths.get(fileName));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * List all stored private key owners
     */
    public static String[] listStoredKeys() {
        File keysDir = getKeysDirectory();
        if (!keysDir.exists() || !keysDir.isDirectory()) {
            return new String[0];
        }

        File[] keyFiles = keysDir.listFiles((dir, name) -> name.endsWith(KEY_EXTENSION));
        if (keyFiles == null) {
            return new String[0];
        }

        String[] owners = new String[keyFiles.length];
        for (int i = 0; i < keyFiles.length; i++) {
            String fileName = keyFiles[i].getName();
            owners[i] = fileName.substring(0, fileName.length() - KEY_EXTENSION.length());
        }

        return owners;
    }

    /**
     * Import a private key from external files with blockchain verification
     *
     * SECURITY: Verifies that the public key matches the AuthorizedKey on blockchain
     * to prevent an attacker from importing their own key with a legitimate username
     *
     * @param ownerName Owner identifier (must be authorized on blockchain)
     * @param privateKeyFile Path to the private key file (PEM or raw format)
     * @param publicKeyFile Path to the public key file (PEM or raw format)
     * @param password Password to encrypt the stored private key
     * @return true if import successful, false otherwise
     * @throws SecurityException if public key doesn't match blockchain AuthorizedKey
     * @throws IllegalArgumentException if owner is not authorized on blockchain
     */
    public static boolean importPrivateKey(String ownerName, String privateKeyFile,
                                          String publicKeyFile, String password) {
        // Validate inputs (let these exceptions propagate)
        if (ownerName == null || ownerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner name cannot be null or empty");
        }
        if (privateKeyFile == null || publicKeyFile == null) {
            throw new IllegalArgumentException("Key files cannot be null");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        // SECURITY: Validate file paths to prevent path traversal attacks (let SecurityException propagate)
        PathSecurityUtil.validateFilePath(privateKeyFile, "import private key");
        PathSecurityUtil.validateFilePath(publicKeyFile, "import public key");

        try {

            // Read private key from file
            byte[] privateKeyBytes = Files.readAllBytes(Paths.get(privateKeyFile));
            String privateKeyContent = new String(privateKeyBytes, StandardCharsets.UTF_8).trim();

            // Remove PEM headers if present
            privateKeyContent = privateKeyContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

            byte[] privateKeyDecoded = Base64.getDecoder().decode(privateKeyContent);
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyDecoded);
            KeyFactory keyFactory = KeyFactory.getInstance(CryptoUtil.SIGNATURE_ALGORITHM);
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

            // Read public key from file
            byte[] publicKeyBytes = Files.readAllBytes(Paths.get(publicKeyFile));
            String publicKeyContent = new String(publicKeyBytes, StandardCharsets.UTF_8).trim();

            // Remove PEM headers if present
            publicKeyContent = publicKeyContent
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

            byte[] publicKeyDecoded = Base64.getDecoder().decode(publicKeyContent);
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyDecoded);
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            // CRITICAL SECURITY: Verify against blockchain AuthorizedKey
            Blockchain blockchain = new Blockchain();
            AuthorizedKey authorizedKey = blockchain.getAuthorizedKeyByOwner(ownerName);

            if (authorizedKey == null) {
                throw new IllegalArgumentException(
                    "Owner '" + ownerName + "' is not authorized on blockchain. " +
                    "Use 'add-key' command to authorize first."
                );
            }

            if (!authorizedKey.isActive()) {
                throw new SecurityException(
                    "AuthorizedKey for '" + ownerName + "' is not active on blockchain"
                );
            }

            String publicKeyString = CryptoUtil.publicKeyToString(publicKey);
            // SECURITY: Use constant-time comparison to prevent timing attacks
            if (!constantTimeEquals(authorizedKey.getPublicKey(), publicKeyString)) {
                throw new SecurityException(
                    "Public key mismatch! The public key in the file does not match " +
                    "the AuthorizedKey on blockchain for owner '" + ownerName + "'. " +
                    "This could be an attack attempt."
                );
            }

            // All security checks passed - save the private key
            boolean saved = savePrivateKey(ownerName, privateKey, password);

            if (saved) {
                logger.info("Private key imported successfully for: {}", ownerName);
            }

            return saved;

        } catch (SecurityException | IllegalArgumentException e) {
            // Re-throw security and validation exceptions
            throw e;
        } catch (Exception e) {
            logger.error("Error importing private key: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Export a stored private key to a file
     *
     * SECURITY WARNING: Exported keys can be stolen. Use restrictive file permissions.
     *
     * @param ownerName Owner identifier
     * @param outputFile Path where to write the private key (PEM format)
     * @param password Password to decrypt the stored private key
     * @return true if export successful, false otherwise
     */
    public static boolean exportPrivateKey(String ownerName, String outputFile, String password) {
        // Validate inputs (let these exceptions propagate)
        if (ownerName == null || ownerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner name cannot be null or empty");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("Output file cannot be null");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        // SECURITY: Validate output file path to prevent path traversal attacks
        PathSecurityUtil.validateFilePath(outputFile, "export private key");

        try {

            // Load the private key
            PrivateKey privateKey = loadPrivateKey(ownerName, password);
            if (privateKey == null) {
                logger.error("Failed to load private key for: {}", ownerName);
                return false;
            }

            // Convert to PEM format
            String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());
            StringBuilder pemContent = new StringBuilder();
            pemContent.append("-----BEGIN PRIVATE KEY-----\n");

            // Split into 64-character lines (PEM standard)
            int index = 0;
            while (index < privateKeyBase64.length()) {
                int endIndex = Math.min(index + 64, privateKeyBase64.length());
                pemContent.append(privateKeyBase64, index, endIndex).append("\n");
                index = endIndex;
            }

            pemContent.append("-----END PRIVATE KEY-----\n");

            // Write to file
            Files.write(Paths.get(outputFile), pemContent.toString().getBytes(StandardCharsets.UTF_8));

            // Set restrictive file permissions (Unix/Linux/macOS only)
            try {
                File file = new File(outputFile);
                file.setReadable(false, false); // Remove read for everyone
                file.setWritable(false, false); // Remove write for everyone
                file.setExecutable(false, false); // Remove execute for everyone

                file.setReadable(true, true); // Add read for owner only
                file.setWritable(true, true); // Add write for owner only
            } catch (Exception e) {
                logger.warn("Could not set restrictive file permissions: {}", e.getMessage());
            }

            // SECURITY: Log the export operation
            logger.warn("⚠️  SECURITY ALERT: Private key exported for '{}' to file: {}",
                       ownerName, outputFile);

            return true;

        } catch (Exception e) {
            logger.error("Error exporting private key: {}", e.getMessage());
            return false;
        }
    }
}
