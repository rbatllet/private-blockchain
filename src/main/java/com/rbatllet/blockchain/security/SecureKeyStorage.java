package com.rbatllet.blockchain.security;

import com.rbatllet.blockchain.util.CryptoUtil;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.KeyFactory;
import java.util.Base64;

/**
 * Secure storage for private keys using AES-256 encryption
 */
public class SecureKeyStorage {
    
    private static final String KEYS_DIRECTORY = "private-keys";
    private static final String ALGORITHM = "AES";
    private static final String KEY_EXTENSION = ".key";
    
    /**
     * Save a private key encrypted with password
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
            
            // Generate secret key from password
            SecretKeySpec secretKey = generateSecretKey(password);
            
            // Encrypt the private key
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] privateKeyBytes = privateKey.getEncoded();
            byte[] encryptedKey = cipher.doFinal(privateKeyBytes);
            
            // Save to file
            String fileName = KEYS_DIRECTORY + File.separator + ownerName.trim() + KEY_EXTENSION;
            String encodedKey = Base64.getEncoder().encodeToString(encryptedKey);
            Files.write(Paths.get(fileName), encodedKey.getBytes());
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Error saving private key: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Load a private key by decrypting with password
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
            
            String encodedKey = new String(Files.readAllBytes(Paths.get(fileName)));
            byte[] encryptedKey = Base64.getDecoder().decode(encodedKey);
            
            // Generate secret key from password
            SecretKeySpec secretKey = generateSecretKey(password);
            
            // Decrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedKey = cipher.doFinal(encryptedKey);
            
            // Reconstruct PrivateKey
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decryptedKey);
            KeyFactory keyFactory = KeyFactory.getInstance(CryptoUtil.EC_ALGORITHM);
            
            return keyFactory.generatePrivate(keySpec);
            
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
     * Generate secret key from password using SHA3-256 (upgraded to AES-256)
     */
    private static SecretKeySpec generateSecretKey(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(CryptoUtil.HASH_ALGORITHM);
        byte[] hash = digest.digest(password.getBytes("UTF-8"));
        // Use full 32 bytes for AES-256 (SHA-3-256 produces 32 bytes)
        byte[] keyBytes = new byte[32];
        System.arraycopy(hash, 0, keyBytes, 0, 32);
        return new SecretKeySpec(keyBytes, ALGORITHM);
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
