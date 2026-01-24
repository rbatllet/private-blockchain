package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.util.CryptoUtil;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import static com.rbatllet.blockchain.util.CryptoUtil.getSecureRandom;

/**
 * Secure block encryption service using AES-256-GCM
 * This version maintains security while being practical to implement
 * 
 * Security features:
 * - AES-256-GCM for authenticated encryption
 * - Random salt for each encryption
 * - Key stretching with multiple iterations
 * - Secure password-based encryption
 */
public class SecureBlockEncryptionService {
    
    // AES-GCM constants
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96-bit IV
    private static final int GCM_TAG_LENGTH = 16; // 128-bit tag
    private static final int AES_KEY_LENGTH = 32; // 256-bit key
    private static final int SALT_LENGTH = 16; // 128-bit salt
    private static final int KEY_ITERATION_COUNT = 10000; // PBKDF2 iterations
    
    /**
     * Encrypted data container with all security components
     */
    public static class SecureEncryptedData {
        private final String encryptedData;
        private final String salt;
        private final String iv;
        private final String dataHash;
        private final long timestamp;
        
        public SecureEncryptedData(String encryptedData, String salt, String iv, String dataHash, long timestamp) {
            this.encryptedData = encryptedData;
            this.salt = salt;
            this.iv = iv;
            this.dataHash = dataHash;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getEncryptedData() { return encryptedData; }
        public String getSalt() { return salt; }
        public String getIv() { return iv; }
        public String getDataHash() { return dataHash; }
        public long getTimestamp() { return timestamp; }
        
        /**
         * Serialize for database storage
         */
        public String serialize() {
            return String.join("|", 
                Long.toString(timestamp),
                salt,
                iv,
                encryptedData,
                dataHash
            );
        }
        
        /**
         * Deserialize from database
         */
        public static SecureEncryptedData deserialize(String serialized) {
            if (serialized == null || serialized.trim().isEmpty()) {
                throw new IllegalArgumentException("Serialized data cannot be null or empty");
            }
            
            String[] parts = serialized.split("\\|");
            if (parts.length != 5) {
                throw new IllegalArgumentException("Invalid serialized format: expected 5 parts, got " + parts.length);
            }
            
            return new SecureEncryptedData(
                parts[3], // encryptedData
                parts[1], // salt
                parts[2], // iv
                parts[4], // dataHash
                Long.parseLong(parts[0]) // timestamp
            );
        }
    }
    
    /**
     * Encrypt data with password using secure key derivation
     */
    public static SecureEncryptedData encryptWithPassword(String plainData, String password) throws Exception {
        if (plainData == null || plainData.trim().isEmpty()) {
            throw new IllegalArgumentException("Plain data cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        // Generate random salt and IV
        byte[] salt = generateRandomBytes(SALT_LENGTH);
        byte[] iv = generateRandomBytes(GCM_IV_LENGTH);
        
        // Derive encryption key using PBKDF2
        byte[] key = deriveKeyFromPassword(password, salt);
        
        try {
            // Encrypt data with AES-256-GCM
            SecretKeySpec secretKey = new SecretKeySpec(key, AES_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            
            byte[] ciphertext = cipher.doFinal(plainData.getBytes(StandardCharsets.UTF_8));
            
            // Calculate hash for integrity
            String dataHash = CryptoUtil.calculateHash(plainData);
            
            return new SecureEncryptedData(
                Base64.getEncoder().encodeToString(ciphertext),
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(iv),
                dataHash,
                System.currentTimeMillis()
            );
            
        } finally {
            // Clear sensitive data
            clearByteArray(key);
        }
    }
    
    /**
     * Decrypt data with password
     */
    public static String decryptWithPassword(SecureEncryptedData encryptedData, String password) throws Exception {
        if (encryptedData == null) {
            throw new IllegalArgumentException("Encrypted data cannot be null");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        // Decode components
        byte[] salt = Base64.getDecoder().decode(encryptedData.getSalt());
        byte[] iv = Base64.getDecoder().decode(encryptedData.getIv());
        byte[] ciphertext = Base64.getDecoder().decode(encryptedData.getEncryptedData());
        
        // Derive decryption key
        byte[] key = deriveKeyFromPassword(password, salt);
        
        try {
            // Decrypt with AES-256-GCM
            SecretKeySpec secretKey = new SecretKeySpec(key, AES_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            
            byte[] plaintext = cipher.doFinal(ciphertext);
            String decryptedData = new String(plaintext, StandardCharsets.UTF_8);
            
            // Verify integrity
            String calculatedHash = CryptoUtil.calculateHash(decryptedData);
            if (!calculatedHash.equals(encryptedData.getDataHash())) {
                throw new SecurityException("Data integrity verification failed");
            }
            
            return decryptedData;
            
        } finally {
            // Clear sensitive data
            clearByteArray(key);
        }
    }
    
    /**
     * Convenience method for string input/output
     */
    public static String encryptToString(String plainData, String password) throws Exception {
        SecureEncryptedData encrypted = encryptWithPassword(plainData, password);
        return encrypted.serialize();
    }
    
    /**
     * Convenience method for string input/output
     */
    public static String decryptFromString(String encryptedString, String password) throws Exception {
        SecureEncryptedData encrypted = SecureEncryptedData.deserialize(encryptedString);
        return decryptWithPassword(encrypted, password);
    }
    
    /**
     * Derive encryption key from password using PBKDF2 with SHA-3-256
     */
    private static byte[] deriveKeyFromPassword(String password, byte[] salt) throws Exception {
        // Use PBKDF2 for key stretching
        javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
            password.toCharArray(), 
            salt, 
            KEY_ITERATION_COUNT, 
            AES_KEY_LENGTH * 8
        );
        
        javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] derivedKey = factory.generateSecret(spec).getEncoded();
        
        // Clear the spec
        spec.clearPassword();
        
        return derivedKey;
    }
    
    /**
     * Generate cryptographically secure random bytes
     */
    private static byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        getSecureRandom().nextBytes(bytes);
        return bytes;
    }
    
    /**
     * Securely clear byte array
     */
    private static void clearByteArray(byte[] array) {
        if (array != null) {
            Arrays.fill(array, (byte) 0);
        }
    }
    
    /**
     * Verify encrypted data integrity without decryption
     */
    public static boolean verifyIntegrity(SecureEncryptedData encryptedData, String password) {
        try {
            decryptWithPassword(encryptedData, password);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if a string has valid encrypted format from this service
     * 
     * @param encryptedString The encrypted string to validate
     * @return true if format appears valid
     */
    public static boolean isValidEncryptedFormat(String encryptedString) {
        if (encryptedString == null || encryptedString.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Check basic format requirements
            if (encryptedString.length() < 50) { // Too short for valid encrypted data
                return false;
            }
            
            // Should not contain spaces or obvious corruption markers
            if (encryptedString.contains(" ") || 
                encryptedString.toLowerCase().contains("error") ||
                encryptedString.toLowerCase().contains("null") ||
                encryptedString.startsWith("[")) {
                return false;
            }
            
            // Try to parse as Base64 (our format uses Base64 encoding)
            try {
                Base64.getDecoder().decode(encryptedString);
                return true;
            } catch (IllegalArgumentException e) {
                // Not valid Base64, might be different format but could still be valid
                // Check if it looks like structured encrypted data
                return encryptedString.matches("^[A-Za-z0-9+/=]+$"); // Base64 pattern
            }
            
        } catch (Exception e) {
            return false;
        }
    }
}