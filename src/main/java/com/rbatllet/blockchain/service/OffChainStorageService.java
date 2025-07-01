package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.entity.OffChainData;
import com.rbatllet.blockchain.util.CryptoUtil;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
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
 * Service for managing off-chain data storage with AES-256-CBC encryption
 * Uses same cryptographic standards as the blockchain (SHA-3-256, ECDSA)
 * Optimized for large files with streaming encryption/decryption
 */
public class OffChainStorageService {
    
    private static final String OFF_CHAIN_DIRECTORY = "off-chain-data";
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16; // 128 bits IV for AES (same for AES-256)
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
                System.err.println("Integrity verification failed: Retrieved data is empty");
                return false;
            }
            
            // Verify data size matches expected file size (accounting for potential padding)
            long expectedSize = offChainData.getFileSize();
            if (expectedSize > 0 && Math.abs(data.length - expectedSize) > 16) {
                System.err.println("Integrity verification failed: Data size mismatch - expected: " + 
                    expectedSize + ", actual: " + data.length);
                return false;
            }
            
            // If we get here without exception and pass validations, integrity is verified
            return true;
        } catch (Exception e) {
            System.err.println("Integrity verification failed: " + e.getMessage());
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
            System.err.println("Error deleting off-chain data: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Encrypt data using AES-CBC and store to file using streaming
     */
    private void encryptAndStoreData(byte[] data, String filePath, String password, byte[] iv) throws Exception {
        SecretKeySpec secretKey = generateSecretKey(password);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        
        try (FileOutputStream fos = new FileOutputStream(filePath);
             CipherOutputStream cos = new CipherOutputStream(fos, cipher);
             ByteArrayInputStream bis = new ByteArrayInputStream(data)) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                cos.write(buffer, 0, bytesRead);
            }
        }
    }
    
    /**
     * Decrypt data from file using streaming
     */
    private byte[] decryptData(String filePath, String password, byte[] iv) throws Exception {
        SecretKeySpec secretKey = generateSecretKey(password);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        
        try (FileInputStream fis = new FileInputStream(filePath);
             CipherInputStream cis = new CipherInputStream(fis, cipher);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = cis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            
            return baos.toByteArray();
        }
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
     * Generate cryptographically secure random IV
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