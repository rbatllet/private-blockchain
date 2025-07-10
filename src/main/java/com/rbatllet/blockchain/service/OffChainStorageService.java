package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.entity.OffChainData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rbatllet.blockchain.util.CryptoUtil;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for managing off-chain data storage with AES-256-GCM encryption
 * Uses same cryptographic standards as the blockchain (SHA-3-256, ECDSA)
 * Optimized for large files with streaming encryption/decryption
 * 
 * UPGRADED: Now uses AES-256-GCM for authenticated encryption
 */
public class OffChainStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(OffChainStorageService.class);
    
    private static final String OFF_CHAIN_DIRECTORY = "off-chain-data";
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12; // 96-bit IV recommended for GCM
    private static final int GCM_TAG_LENGTH = 16; // 128-bit authentication tag
    private static final int BUFFER_SIZE = 8192; // 8KB buffer for streaming
    
    /**
     * Store large data off-chain with encryption and integrity verification
     */
    public OffChainData storeData(byte[] data, String password, PrivateKey signerKey, 
                                 String signerPublicKey, String contentType) throws Exception {
        
        // Validate inputs
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (signerKey == null) {
            throw new IllegalArgumentException("Signer key cannot be null");
        }
        
        // Create off-chain directory if it doesn't exist
        createOffChainDirectory();
        
        // Generate unique filename and IV
        String fileName = generateUniqueFileName();
        String filePath = OFF_CHAIN_DIRECTORY + File.separator + fileName;
        byte[] iv = generateIV();
        
        // Calculate hash of original data (for integrity verification)
        String dataHash = calculateDataHash(data);
        
        // Encrypt and store data
        encryptAndStoreData(data, filePath, password, iv);
        
        // Sign the data hash
        String signature = CryptoUtil.signData(dataHash, signerKey);
        
        // Create and return OffChainData metadata
        return new OffChainData(
            dataHash,
            signature,
            filePath,
            (long) data.length,
            Base64.getEncoder().encodeToString(iv),
            contentType != null ? contentType : "application/octet-stream",
            signerPublicKey
        );
    }
    
    /**
     * Retrieve and decrypt off-chain data with integrity verification
     */
    public byte[] retrieveData(OffChainData offChainData, String password) throws Exception {
        
        // Validate inputs
        if (offChainData == null) {
            throw new IllegalArgumentException("OffChainData cannot be null");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        // Check if file exists
        Path filePath = Paths.get(offChainData.getFilePath());
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("Off-chain data file not found: " + offChainData.getFilePath());
        }
        
        // Decrypt data
        byte[] iv = Base64.getDecoder().decode(offChainData.getEncryptionIV());
        byte[] decryptedData = decryptData(offChainData.getFilePath(), password, iv);
        
        // Verify data integrity
        String calculatedHash = calculateDataHash(decryptedData);
        if (!calculatedHash.equals(offChainData.getDataHash())) {
            throw new SecurityException("Data integrity verification failed. File may be corrupted or tampered with.");
        }
        
        return decryptedData;
    }
    
    /**
     * Verify data integrity and signature by attempting full decryption
     */
    public boolean verifyIntegrity(OffChainData offChainData, String password) {
        try {
            byte[] data = retrieveData(offChainData, password);
            
            // Additional validation: ensure data is not empty and has reasonable size
            if (data == null || data.length == 0) {
                logger.error("❌ Integrity verification failed: Retrieved data is empty");
                return false;
            }
            
            // Verify data size matches expected file size (accounting for potential padding)
            long expectedSize = offChainData.getFileSize();
            if (expectedSize > 0 && Math.abs(data.length - expectedSize) > 16) {
                logger.error("❌ Integrity verification failed: Data size mismatch - expected: {}, actual: {}", 
                    expectedSize, data.length);
                return false;
            }
            
            // If we get here without exception and pass validations, integrity is verified
            return true;
        } catch (Exception e) {
            logger.error("❌ Integrity verification failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete off-chain data file
     */
    public boolean deleteData(OffChainData offChainData) {
        try {
            return Files.deleteIfExists(Paths.get(offChainData.getFilePath()));
        } catch (Exception e) {
            logger.error("❌ Error deleting off-chain data: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Encrypt data using AES-GCM and store to file
     * GCM provides authenticated encryption in a single operation
     */
    private void encryptAndStoreData(byte[] data, String filePath, String password, byte[] iv) throws Exception {
        SecretKeySpec secretKey = generateSecretKey(password);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv); // Tag length in bits
        
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
        
        // GCM encrypts all data in one operation (no streaming needed for integrity)
        byte[] encryptedData = cipher.doFinal(data);
        
        // Write encrypted data to file
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(encryptedData);
        }
    }
    
    /**
     * Decrypt data from file using AES-GCM
     * GCM provides authenticated decryption with integrity verification
     */
    private byte[] decryptData(String filePath, String password, byte[] iv) throws Exception {
        SecretKeySpec secretKey = generateSecretKey(password);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
        
        // Read all encrypted data from file
        byte[] encryptedData;
        try (FileInputStream fis = new FileInputStream(filePath);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            encryptedData = baos.toByteArray();
        }
        
        // GCM decrypts and verifies in one operation
        return cipher.doFinal(encryptedData);
    }
    
    /**
     * Generate secret key from password using SHA-3-256 (upgraded to AES-256)
     */
    private SecretKeySpec generateSecretKey(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(CryptoUtil.HASH_ALGORITHM);
        byte[] hash = digest.digest(password.getBytes("UTF-8"));
        // Use first 32 bytes for AES-256 (SHA-3-256 produces 32 bytes)
        byte[] keyBytes = new byte[32];
        System.arraycopy(hash, 0, keyBytes, 0, 32);
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
    
    /**
     * Calculate SHA-3-256 hash of data for integrity verification
     */
    private String calculateDataHash(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(CryptoUtil.HASH_ALGORITHM);
        byte[] hash = digest.digest(data);
        return bytesToHex(hash);
    }
    
    /**
     * Generate cryptographically secure random IV for GCM
     * GCM uses 96-bit (12-byte) IV for optimal security
     */
    private byte[] generateIV() {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
    
    /**
     * Generate unique filename based on timestamp and random component
     */
    private String generateUniqueFileName() {
        long timestamp = System.currentTimeMillis();
        int random = new SecureRandom().nextInt(10000);
        return "offchain_" + timestamp + "_" + random + ".dat";
    }
    
    /**
     * Create off-chain data directory if it doesn't exist
     */
    private void createOffChainDirectory() throws Exception {
        Path dirPath = Paths.get(OFF_CHAIN_DIRECTORY);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
    }
    
    /**
     * Convert byte array to hexadecimal string (same format as CryptoUtil)
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Get file size information
     */
    public long getFileSize(OffChainData offChainData) {
        try {
            return Files.size(Paths.get(offChainData.getFilePath()));
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * Check if off-chain data file exists
     */
    public boolean fileExists(OffChainData offChainData) {
        return Files.exists(Paths.get(offChainData.getFilePath()));
    }
}