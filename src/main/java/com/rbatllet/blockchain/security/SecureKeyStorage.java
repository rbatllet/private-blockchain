package com.rbatllet.blockchain.security;

import com.rbatllet.blockchain.util.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.KeyFactory;
import java.util.Arrays;
import java.util.Base64;

/**
 * Secure storage for private keys using AES-256-GCM encryption
 */
public class SecureKeyStorage {

    private static final Logger logger = LoggerFactory.getLogger(SecureKeyStorage.class);

    private static final String KEYS_DIRECTORY = "private-keys";
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_EXTENSION = ".key";
    private static final String KEYPAIR_EXTENSION = ".keypair"; // For ML-DSA KeyPairs (public + private)
    private static final int GCM_IV_LENGTH = 12; // 96-bit IV recommended for GCM
    private static final int GCM_TAG_LENGTH = 16; // 128-bit authentication tag
    private static final int AES_KEY_LENGTH = 32; // 256-bit key
    
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
            File keysDir = new File(KEYS_DIRECTORY);
            if (!keysDir.exists()) {
                keysDir.mkdirs();
            }

            // Derive secret key from password using SHA-3-256
            MessageDigest digest = MessageDigest.getInstance(CryptoUtil.HASH_ALGORITHM);
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            // Ensure we have exactly 256 bits (32 bytes) for AES-256
            byte[] keyBytes = new byte[AES_KEY_LENGTH];
            System.arraycopy(hash, 0, keyBytes, 0, Math.min(hash.length, AES_KEY_LENGTH));

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // Encrypt the private key with AES-256-GCM
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            byte[] privateKeyBytes = privateKey.getEncoded();
            byte[] ciphertext = cipher.doFinal(privateKeyBytes);

            // Combine IV + ciphertext (includes auth tag)
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            // Save to file
            String fileName = KEYS_DIRECTORY + File.separator + ownerName.trim() + KEY_EXTENSION;
            String encodedKey = Base64.getEncoder().encodeToString(combined);
            Files.write(Paths.get(fileName), encodedKey.getBytes(StandardCharsets.UTF_8));

            // Clear sensitive data
            Arrays.fill(keyBytes, (byte) 0);
            Arrays.fill(privateKeyBytes, (byte) 0);

            return true;

        } catch (Exception e) {
            logger.error("❌ Error saving private key: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Load a private key by decrypting with password using AES-256-GCM
     */
    public static PrivateKey loadPrivateKey(String ownerName, String password) {
        try {
            // Validate inputs
            if (ownerName == null || ownerName.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
                return null;
            }

            // Read encrypted file
            String fileName = KEYS_DIRECTORY + File.separator + ownerName.trim() + KEY_EXTENSION;
            if (!Files.exists(Paths.get(fileName))) {
                return null;
            }

            String encodedKey = new String(Files.readAllBytes(Paths.get(fileName)), StandardCharsets.UTF_8);
            byte[] combined = Base64.getDecoder().decode(encodedKey);

            if (combined.length < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
                return null;
            }

            // Derive secret key from password using SHA-3-256
            MessageDigest digest = MessageDigest.getInstance(CryptoUtil.HASH_ALGORITHM);
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            // Ensure we have exactly 256 bits (32 bytes) for AES-256
            byte[] keyBytes = new byte[AES_KEY_LENGTH];
            System.arraycopy(hash, 0, keyBytes, 0, Math.min(hash.length, AES_KEY_LENGTH));

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

            // Extract IV and ciphertext
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];

            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            // Decrypt with AES-256-GCM
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            byte[] decryptedKey = cipher.doFinal(ciphertext);

            // Reconstruct PrivateKey
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decryptedKey);
            KeyFactory keyFactory = KeyFactory.getInstance(CryptoUtil.SIGNATURE_ALGORITHM);

            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            // Clear sensitive data
            Arrays.fill(keyBytes, (byte) 0);
            Arrays.fill(decryptedKey, (byte) 0);

            return privateKey;

        } catch (Exception e) {
            // Don't print password-related errors to avoid security issues
            return null;
        }
    }
    
    /**
     * Check if a private key exists for an owner
     */
    public static boolean hasPrivateKey(String ownerName) {
        if (ownerName == null || ownerName.trim().isEmpty()) {
            return false;
        }
        String fileName = KEYS_DIRECTORY + File.separator + ownerName.trim() + KEY_EXTENSION;
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
            String fileName = KEYS_DIRECTORY + File.separator + ownerName.trim() + KEY_EXTENSION;
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
            File keysDir = new File(KEYS_DIRECTORY);
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

            // Derive secret key from password using SHA-3-256
            MessageDigest digest = MessageDigest.getInstance(CryptoUtil.HASH_ALGORITHM);
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            byte[] keyBytes = new byte[AES_KEY_LENGTH];
            System.arraycopy(hash, 0, keyBytes, 0, Math.min(hash.length, AES_KEY_LENGTH));
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // Encrypt with AES-256-GCM
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            byte[] ciphertext = cipher.doFinal(combined);

            // Combine IV + ciphertext
            byte[] encrypted = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encrypted, 0, iv.length);
            System.arraycopy(ciphertext, 0, encrypted, iv.length, ciphertext.length);

            // Save to file with .keypair extension
            String fileName = KEYS_DIRECTORY + File.separator + ownerName.trim() + KEYPAIR_EXTENSION;
            String encodedKey = Base64.getEncoder().encodeToString(encrypted);
            Files.write(Paths.get(fileName), encodedKey.getBytes(StandardCharsets.UTF_8));

            // Clear sensitive data
            Arrays.fill(keyBytes, (byte) 0);
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
            String fileName = KEYS_DIRECTORY + File.separator + ownerName.trim() + KEYPAIR_EXTENSION;
            if (!Files.exists(Paths.get(fileName))) {
                return null;
            }

            String encodedKey = new String(Files.readAllBytes(Paths.get(fileName)), StandardCharsets.UTF_8);
            byte[] encrypted = Base64.getDecoder().decode(encodedKey);

            if (encrypted.length < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
                return null;
            }

            // Derive secret key from password using SHA-3-256
            MessageDigest digest = MessageDigest.getInstance(CryptoUtil.HASH_ALGORITHM);
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            byte[] keyBytes = new byte[AES_KEY_LENGTH];
            System.arraycopy(hash, 0, keyBytes, 0, Math.min(hash.length, AES_KEY_LENGTH));
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

            // Extract IV and ciphertext
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[encrypted.length - GCM_IV_LENGTH];
            System.arraycopy(encrypted, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encrypted, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

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
            Arrays.fill(keyBytes, (byte) 0);
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
        String fileName = KEYS_DIRECTORY + File.separator + ownerName.trim() + KEYPAIR_EXTENSION;
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
            String fileName = KEYS_DIRECTORY + File.separator + ownerName.trim() + KEYPAIR_EXTENSION;
            return Files.deleteIfExists(Paths.get(fileName));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * List all stored private key owners
     */
    public static String[] listStoredKeys() {
        File keysDir = new File(KEYS_DIRECTORY);
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
}
