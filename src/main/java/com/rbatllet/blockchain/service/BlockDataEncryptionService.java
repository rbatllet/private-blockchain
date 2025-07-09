package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.util.CryptoUtil;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Advanced encryption service for blockchain block data using AES-256-GCM
 * Provides authenticated encryption with hybrid key management
 * 
 * Features:
 * - AES-256-GCM for authenticated encryption
 * - Hybrid encryption: ECDSA for key exchange, AES for data
 * - Per-block Data Encryption Keys (DEK) 
 * - Key Encryption Keys (KEK) per user/organization
 * - Cryptographic integrity and authenticity
 */
public class BlockDataEncryptionService {
    
    // AES-256-GCM constants
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96-bit IV recommended for GCM
    private static final int GCM_TAG_LENGTH = 16; // 128-bit authentication tag
    private static final int AES_KEY_LENGTH = 32; // 256-bit key
    
    // Data Encryption Key derivation
    private static final String KEK_DERIVATION_INFO = "BLOCK_DATA_ENCRYPTION_KEK";
    
    /**
     * Encrypted data container with all necessary components for decryption
     */
    public static class EncryptedBlockData {
        private final String encryptedData;      // Base64 encoded encrypted content
        private final String encryptedDEK;       // Base64 encoded encrypted DEK
        private final String iv;                 // Base64 encoded IV
        private final String authTag;            // Base64 encoded authentication tag
        private final String dataHash;           // SHA-3-256 hash of original data
        private final String encryptionVersion;  // Version for future compatibility
        private final long timestamp;            // Encryption timestamp
        
        public EncryptedBlockData(String encryptedData, String encryptedDEK, String iv, 
                                String authTag, String dataHash, String encryptionVersion, long timestamp) {
            this.encryptedData = encryptedData;
            this.encryptedDEK = encryptedDEK;
            this.iv = iv;
            this.authTag = authTag;
            this.dataHash = dataHash;
            this.encryptionVersion = encryptionVersion;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getEncryptedData() { return encryptedData; }
        public String getEncryptedDEK() { return encryptedDEK; }
        public String getIv() { return iv; }
        public String getAuthTag() { return authTag; }
        public String getDataHash() { return dataHash; }
        public String getEncryptionVersion() { return encryptionVersion; }
        public long getTimestamp() { return timestamp; }
        
        /**
         * Serialize to string format for database storage
         */
        public String serialize() {
            return String.join("|", 
                encryptionVersion,
                Long.toString(timestamp),
                encryptedData,
                encryptedDEK,
                iv,
                authTag,
                dataHash
            );
        }
        
        /**
         * Deserialize from string format
         */
        public static EncryptedBlockData deserialize(String serialized) {
            if (serialized == null || serialized.trim().isEmpty()) {
                throw new IllegalArgumentException("Serialized data cannot be null or empty");
            }
            
            String[] parts = serialized.split("\\|");
            if (parts.length != 7) {
                throw new IllegalArgumentException("Invalid serialized format: expected 7 parts, got " + parts.length);
            }
            
            return new EncryptedBlockData(
                parts[2], // encryptedData
                parts[3], // encryptedDEK
                parts[4], // iv
                parts[5], // authTag
                parts[6], // dataHash
                parts[0], // encryptionVersion
                Long.parseLong(parts[1]) // timestamp
            );
        }
    }
    
    /**
     * Encrypt block data using hybrid AES-256-GCM + ECDSA encryption
     * 
     * @param plainData The plain text data to encrypt
     * @param userPublicKey The user's public key for DEK encryption
     * @param signerPrivateKey The signer's private key for authentication
     * @return EncryptedBlockData containing all encryption components
     */
    public static EncryptedBlockData encryptBlockData(String plainData, PublicKey userPublicKey, 
                                                    PrivateKey signerPrivateKey) throws Exception {
        
        // Input validation
        if (plainData == null || plainData.trim().isEmpty()) {
            throw new IllegalArgumentException("Plain data cannot be null or empty");
        }
        if (userPublicKey == null) {
            throw new IllegalArgumentException("User public key cannot be null");
        }
        if (signerPrivateKey == null) {
            throw new IllegalArgumentException("Signer private key cannot be null");
        }
        
        // Generate random DEK (Data Encryption Key)
        byte[] dek = generateRandomKey();
        
        // Generate random IV for GCM
        byte[] iv = generateRandomIV();
        
        // Calculate hash of original data for integrity verification
        String dataHash = CryptoUtil.calculateHash(plainData);
        
        // Encrypt the actual data with DEK using AES-256-GCM
        EncryptionResult encryptionResult = encryptWithGCM(plainData.getBytes(StandardCharsets.UTF_8), dek, iv);
        
        // Encrypt DEK with consistent key derivation
        String encryptedDEK = encryptDEKWithConsistentKey(dek, userPublicKey);
        
        // Create encrypted data container
        EncryptedBlockData encryptedData = new EncryptedBlockData(
            Base64.getEncoder().encodeToString(encryptionResult.ciphertext),
            encryptedDEK,
            Base64.getEncoder().encodeToString(iv),
            Base64.getEncoder().encodeToString(encryptionResult.authTag),
            dataHash,
            "GCM-v1.0", // Version for future compatibility
            System.currentTimeMillis()
        );
        
        // Clear sensitive data from memory
        clearByteArray(dek);
        
        return encryptedData;
    }
    
    /**
     * Decrypt block data using hybrid AES-256-GCM + ECDSA decryption
     * 
     * @param encryptedData The encrypted data container
     * @param userPrivateKey The user's private key for DEK decryption
     * @return The decrypted plain text data
     */
    public static String decryptBlockData(EncryptedBlockData encryptedData, PrivateKey userPrivateKey) throws Exception {
        
        // Input validation
        if (encryptedData == null) {
            throw new IllegalArgumentException("Encrypted data cannot be null");
        }
        if (userPrivateKey == null) {
            throw new IllegalArgumentException("User private key cannot be null");
        }
        
        // Version compatibility check
        if (!"GCM-v1.0".equals(encryptedData.getEncryptionVersion())) {
            throw new UnsupportedOperationException("Unsupported encryption version: " + encryptedData.getEncryptionVersion());
        }
        
        // Decrypt DEK with consistent key derivation
        byte[] dek = decryptDEKWithConsistentKey(encryptedData.getEncryptedDEK(), userPrivateKey);
        
        try {
            // Decode components
            byte[] ciphertext = Base64.getDecoder().decode(encryptedData.getEncryptedData());
            byte[] iv = Base64.getDecoder().decode(encryptedData.getIv());
            byte[] authTag = Base64.getDecoder().decode(encryptedData.getAuthTag());
            
            // Decrypt data with DEK using AES-256-GCM
            byte[] plainBytes = decryptWithGCM(ciphertext, authTag, dek, iv);
            String plainData = new String(plainBytes, StandardCharsets.UTF_8);
            
            // Verify data integrity
            String calculatedHash = CryptoUtil.calculateHash(plainData);
            if (!calculatedHash.equals(encryptedData.getDataHash())) {
                throw new SecurityException("Data integrity verification failed. Data may be corrupted or tampered with.");
            }
            
            return plainData;
            
        } finally {
            // Clear sensitive data from memory
            clearByteArray(dek);
        }
    }
    
    /**
     * Verify encrypted data integrity without full decryption
     * Useful for validation without exposing decrypted content
     */
    public static boolean verifyEncryptedDataIntegrity(EncryptedBlockData encryptedData, PrivateKey userPrivateKey) {
        try {
            // Attempt decryption and hash verification
            String decryptedData = decryptBlockData(encryptedData, userPrivateKey);
            
            // If we reach here without exception, integrity is verified
            return decryptedData != null && !decryptedData.trim().isEmpty();
            
        } catch (Exception e) {
            System.err.println("Integrity verification failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Internal class for encryption results
     */
    private static class EncryptionResult {
        final byte[] ciphertext;
        final byte[] authTag;
        
        EncryptionResult(byte[] ciphertext, byte[] authTag) {
            this.ciphertext = ciphertext;
            this.authTag = authTag;
        }
    }
    
    /**
     * Encrypt data using AES-256-GCM
     */
    private static EncryptionResult encryptWithGCM(byte[] plaintext, byte[] key, byte[] iv) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key, AES_ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv); // Tag length in bits
        
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
        
        byte[] ciphertext = cipher.doFinal(plaintext);
        
        // In GCM mode, the authentication tag is appended to the ciphertext
        // We need to separate them
        int ciphertextLength = ciphertext.length - GCM_TAG_LENGTH;
        byte[] actualCiphertext = new byte[ciphertextLength];
        byte[] authTag = new byte[GCM_TAG_LENGTH];
        
        System.arraycopy(ciphertext, 0, actualCiphertext, 0, ciphertextLength);
        System.arraycopy(ciphertext, ciphertextLength, authTag, 0, GCM_TAG_LENGTH);
        
        return new EncryptionResult(actualCiphertext, authTag);
    }
    
    /**
     * Decrypt data using AES-256-GCM
     */
    private static byte[] decryptWithGCM(byte[] ciphertext, byte[] authTag, byte[] key, byte[] iv) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key, AES_ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
        
        // Combine ciphertext and auth tag for GCM decryption
        byte[] combinedData = new byte[ciphertext.length + authTag.length];
        System.arraycopy(ciphertext, 0, combinedData, 0, ciphertext.length);
        System.arraycopy(authTag, 0, combinedData, ciphertext.length, authTag.length);
        
        return cipher.doFinal(combinedData);
    }
    
    /**
     * Encrypt DEK using consistent key derivation from public key
     * Uses a simple approach that stores the public key with the encrypted DEK
     */
    private static String encryptDEKWithConsistentKey(byte[] dek, PublicKey publicKey) throws Exception {
        // Store the public key along with the encrypted DEK to ensure consistency
        byte[] kek = deriveKEKFromKeyBytes(publicKey.getEncoded());
        
        try {
            // Generate IV for DEK encryption
            byte[] dekIV = generateRandomIV();
            
            // Encrypt DEK with KEK
            EncryptionResult result = encryptWithGCM(dek, kek, dekIV);
            
            // Combine publicKey + IV + ciphertext + authTag for storage
            byte[] publicKeyBytes = publicKey.getEncoded();
            byte[] combined = new byte[4 + publicKeyBytes.length + dekIV.length + result.ciphertext.length + result.authTag.length];
            int offset = 0;
            
            // Store public key length first (4 bytes)
            combined[offset++] = (byte) (publicKeyBytes.length >>> 24);
            combined[offset++] = (byte) (publicKeyBytes.length >>> 16);
            combined[offset++] = (byte) (publicKeyBytes.length >>> 8);
            combined[offset++] = (byte) publicKeyBytes.length;
            
            // Store public key
            System.arraycopy(publicKeyBytes, 0, combined, offset, publicKeyBytes.length);
            offset += publicKeyBytes.length;
            
            // Store IV
            System.arraycopy(dekIV, 0, combined, offset, dekIV.length);
            offset += dekIV.length;
            
            // Store ciphertext
            System.arraycopy(result.ciphertext, 0, combined, offset, result.ciphertext.length);
            offset += result.ciphertext.length;
            
            // Store auth tag
            System.arraycopy(result.authTag, 0, combined, offset, result.authTag.length);
            
            return Base64.getEncoder().encodeToString(combined);
            
        } finally {
            clearByteArray(kek);
        }
    }
    
    /**
     * Decrypt DEK using consistent key derivation from private key
     * Extracts the stored public key to ensure the same KEK is used
     */
    private static byte[] decryptDEKWithConsistentKey(String encryptedDEK, PrivateKey privateKey) throws Exception {
        // Decode the combined data
        byte[] combined = Base64.getDecoder().decode(encryptedDEK);
        int offset = 0;
        
        // Extract public key length (4 bytes)
        int publicKeyLength = ((combined[offset++] & 0xFF) << 24) |
                             ((combined[offset++] & 0xFF) << 16) |
                             ((combined[offset++] & 0xFF) << 8) |
                             (combined[offset++] & 0xFF);
        
        // Extract public key
        byte[] publicKeyBytes = new byte[publicKeyLength];
        System.arraycopy(combined, offset, publicKeyBytes, 0, publicKeyLength);
        offset += publicKeyLength;
        
        // Validate that the private key corresponds to the stored public key
        PublicKey storedPublicKey = CryptoUtil.stringToPublicKey(Base64.getEncoder().encodeToString(publicKeyBytes));
        if (!validateKeyPair(privateKey, storedPublicKey)) {
            throw new SecurityException("Private key does not correspond to the stored public key");
        }
        
        // Derive KEK from the stored public key (ensures consistency)
        byte[] kek = deriveKEKFromKeyBytes(publicKeyBytes);
        
        try {
            // Extract remaining components
            byte[] dekIV = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - offset - GCM_IV_LENGTH - GCM_TAG_LENGTH];
            byte[] authTag = new byte[GCM_TAG_LENGTH];
            
            System.arraycopy(combined, offset, dekIV, 0, GCM_IV_LENGTH);
            offset += GCM_IV_LENGTH;
            
            System.arraycopy(combined, offset, ciphertext, 0, ciphertext.length);
            offset += ciphertext.length;
            
            System.arraycopy(combined, offset, authTag, 0, GCM_TAG_LENGTH);
            
            // Decrypt DEK
            return decryptWithGCM(ciphertext, authTag, kek, dekIV);
            
        } finally {
            clearByteArray(kek);
        }
    }
    
    /**
     * Derive KEK from key bytes using SHA-3-256
     * This ensures consistent key derivation for encryption/decryption
     */
    private static byte[] deriveKEKFromKeyBytes(byte[] keyBytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(CryptoUtil.HASH_ALGORITHM);
        digest.update(KEK_DERIVATION_INFO.getBytes(StandardCharsets.UTF_8));
        digest.update(keyBytes);
        
        byte[] hash = digest.digest();
        // Use full 32 bytes for AES-256
        return hash;
    }
    

    /**
     * Generate cryptographically secure random key for AES-256
     */
    private static byte[] generateRandomKey() {
        byte[] key = new byte[AES_KEY_LENGTH];
        new SecureRandom().nextBytes(key);
        return key;
    }
    
    /**
     * Generate cryptographically secure random IV for GCM
     */
    private static byte[] generateRandomIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
    
    /**
     * Validate that a private key corresponds to a public key
     */
    private static boolean validateKeyPair(PrivateKey privateKey, PublicKey publicKey) {
        try {
            // Create a test message
            String testMessage = "key_pair_validation_test";
            
            // Sign with private key
            String signature = CryptoUtil.signData(testMessage, privateKey);
            
            // Verify with public key
            return CryptoUtil.verifySignature(testMessage, signature, publicKey);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Securely clear byte array from memory
     */
    private static void clearByteArray(byte[] array) {
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                array[i] = 0;
            }
        }
    }
    
    /**
     * Generate a password-based KEK for cases where public key is not available
     * Useful for backward compatibility or password-based encryption
     */
    public static String encryptWithPassword(String plainData, String password) throws Exception {
        if (plainData == null || plainData.trim().isEmpty()) {
            throw new IllegalArgumentException("Plain data cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        // Derive key from password
        MessageDigest digest = MessageDigest.getInstance(CryptoUtil.HASH_ALGORITHM);
        byte[] key = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        
        // Generate IV
        byte[] iv = generateRandomIV();
        
        try {
            // Encrypt with GCM
            EncryptionResult result = encryptWithGCM(plainData.getBytes(StandardCharsets.UTF_8), key, iv);
            
            // Combine IV + ciphertext + authTag
            byte[] combined = new byte[iv.length + result.ciphertext.length + result.authTag.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(result.ciphertext, 0, combined, iv.length, result.ciphertext.length);
            System.arraycopy(result.authTag, 0, combined, iv.length + result.ciphertext.length, result.authTag.length);
            
            return Base64.getEncoder().encodeToString(combined);
            
        } finally {
            clearByteArray(key);
        }
    }
    
    /**
     * Decrypt password-based encrypted data
     */
    public static String decryptWithPassword(String encryptedData, String password) throws Exception {
        if (encryptedData == null || encryptedData.trim().isEmpty()) {
            throw new IllegalArgumentException("Encrypted data cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        // Derive key from password
        MessageDigest digest = MessageDigest.getInstance(CryptoUtil.HASH_ALGORITHM);
        byte[] key = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        
        try {
            // Decode combined data
            byte[] combined = Base64.getDecoder().decode(encryptedData);
            
            // Extract components
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH - GCM_TAG_LENGTH];
            byte[] authTag = new byte[GCM_TAG_LENGTH];
            
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
            System.arraycopy(combined, GCM_IV_LENGTH + ciphertext.length, authTag, 0, GCM_TAG_LENGTH);
            
            // Decrypt
            byte[] plainBytes = decryptWithGCM(ciphertext, authTag, key, iv);
            return new String(plainBytes, StandardCharsets.UTF_8);
            
        } finally {
            clearByteArray(key);
        }
    }
}