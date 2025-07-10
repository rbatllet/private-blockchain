package com.rbatllet.blockchain.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * BLOCK PASSWORD REGISTRY
 * 
 * Secure registry for managing per-block passwords in Revolutionary Search Engine.
 * Provides secure storage and retrieval of block-specific passwords with:
 * - AES-256-GCM encryption for password storage
 * - Thread-safe operations with read/write locks
 * - Memory-only storage (passwords not persisted to disk)
 * - Secure cleanup and disposal methods
 */
public class BlockPasswordRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(BlockPasswordRegistry.class);
    
    private final Map<String, EncryptedPasswordEntry> passwordRegistry;
    private final ReentrantReadWriteLock registryLock;
    private final SecretKeySpec masterKey;
    private final SecureRandom secureRandom;
    
    // AES-GCM constants
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    public BlockPasswordRegistry() {
        this.passwordRegistry = new ConcurrentHashMap<>();
        this.registryLock = new ReentrantReadWriteLock();
        this.secureRandom = new SecureRandom();
        
        // Generate a master key for encrypting stored passwords
        byte[] masterKeyBytes = new byte[32]; // 256-bit key
        secureRandom.nextBytes(masterKeyBytes);
        this.masterKey = new SecretKeySpec(masterKeyBytes, "AES");
    }
    
    /**
     * Register a password for a specific block
     * @param blockHash The hash of the block
     * @param password The password to register (will be encrypted)
     * @return true if registration was successful
     */
    public boolean registerBlockPassword(String blockHash, String password) {
        if (blockHash == null || password == null) {
            return false;
        }
        
        registryLock.writeLock().lock();
        try {
            EncryptedPasswordEntry entry = encryptPassword(password);
            passwordRegistry.put(blockHash, entry);
            return true;
        } catch (Exception e) {
            logger.error("❌ Failed to register password for block {}: {}", blockHash, e.getMessage());
            return false;
        } finally {
            registryLock.writeLock().unlock();
        }
    }
    
    /**
     * Retrieve a password for a specific block
     * @param blockHash The hash of the block
     * @return The decrypted password or null if not found
     */
    public String getBlockPassword(String blockHash) {
        if (blockHash == null) {
            return null;
        }
        
        registryLock.readLock().lock();
        try {
            EncryptedPasswordEntry entry = passwordRegistry.get(blockHash);
            if (entry == null) {
                return null;
            }
            return decryptPassword(entry);
        } catch (Exception e) {
            logger.error("❌ Failed to retrieve password for block {}: {}", blockHash, e.getMessage());
            return null;
        } finally {
            registryLock.readLock().unlock();
        }
    }
    
    /**
     * Check if a block has a registered password
     * @param blockHash The hash of the block
     * @return true if the block has a registered password
     */
    public boolean hasBlockPassword(String blockHash) {
        if (blockHash == null) {
            return false;
        }
        
        registryLock.readLock().lock();
        try {
            return passwordRegistry.containsKey(blockHash);
        } finally {
            registryLock.readLock().unlock();
        }
    }
    
    /**
     * Get all block hashes that have registered passwords
     * @return Set of block hashes
     */
    public Set<String> getRegisteredBlocks() {
        registryLock.readLock().lock();
        try {
            return new HashSet<>(passwordRegistry.keySet());
        } finally {
            registryLock.readLock().unlock();
        }
    }
    
    /**
     * Remove a block's password registration
     * @param blockHash The hash of the block
     * @return true if removal was successful
     */
    public boolean removeBlockPassword(String blockHash) {
        if (blockHash == null) {
            return false;
        }
        
        registryLock.writeLock().lock();
        try {
            EncryptedPasswordEntry removed = passwordRegistry.remove(blockHash);
            if (removed != null) {
                // Secure cleanup
                secureCleanup(removed);
                return true;
            }
            return false;
        } finally {
            registryLock.writeLock().unlock();
        }
    }
    
    /**
     * Get registry statistics
     * @return RegistryStats with current state information
     */
    public RegistryStats getStats() {
        registryLock.readLock().lock();
        try {
            return new RegistryStats(
                passwordRegistry.size(),
                estimateMemoryUsage()
            );
        } finally {
            registryLock.readLock().unlock();
        }
    }
    
    /**
     * Clear all registered passwords
     * WARNING: This will remove all block password associations
     */
    public void clearAll() {
        registryLock.writeLock().lock();
        try {
            // Secure cleanup of all entries
            for (EncryptedPasswordEntry entry : passwordRegistry.values()) {
                secureCleanup(entry);
            }
            passwordRegistry.clear();
        } finally {
            registryLock.writeLock().unlock();
        }
    }
    
    /**
     * Secure shutdown of the registry
     */
    public void shutdown() {
        clearAll();
        // Clear master key (best effort)
        try {
            java.lang.reflect.Field keyField = SecretKeySpec.class.getDeclaredField("key");
            keyField.setAccessible(true);
            byte[] keyBytes = (byte[]) keyField.get(masterKey);
            if (keyBytes != null) {
                java.util.Arrays.fill(keyBytes, (byte) 0);
            }
        } catch (Exception e) {
            // Best effort cleanup
        }
    }
    
    // ===== PRIVATE METHODS =====
    
    /**
     * Encrypt a password using AES-GCM
     */
    private EncryptedPasswordEntry encryptPassword(String password) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        
        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, masterKey, gcmSpec);
        
        byte[] encryptedPassword = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
        
        return new EncryptedPasswordEntry(
            Base64.getEncoder().encodeToString(encryptedPassword),
            Base64.getEncoder().encodeToString(iv)
        );
    }
    
    /**
     * Decrypt a password using AES-GCM
     */
    private String decryptPassword(EncryptedPasswordEntry entry) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        
        byte[] iv = Base64.getDecoder().decode(entry.getIv());
        byte[] encryptedPassword = Base64.getDecoder().decode(entry.getEncryptedPassword());
        
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, masterKey, gcmSpec);
        
        byte[] decryptedBytes = cipher.doFinal(encryptedPassword);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
    
    /**
     * Secure cleanup of encrypted password entry
     */
    private void secureCleanup(EncryptedPasswordEntry entry) {
        // Best effort to clear sensitive data from memory
        try {
            // This is a best-effort cleanup since strings are immutable in Java
            entry.clear();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    /**
     * Estimate memory usage of the registry
     */
    private long estimateMemoryUsage() {
        // Rough estimate: each entry has ~100-200 bytes overhead
        return passwordRegistry.size() * 150L;
    }
    
    // ===== INNER CLASSES =====
    
    /**
     * Encrypted password entry
     */
    private static class EncryptedPasswordEntry {
        private String encryptedPassword;
        private String iv;
        
        public EncryptedPasswordEntry(String encryptedPassword, String iv) {
            this.encryptedPassword = encryptedPassword;
            this.iv = iv;
        }
        
        public String getEncryptedPassword() {
            return encryptedPassword;
        }
        
        public String getIv() {
            return iv;
        }
        
        public void clear() {
            // Best effort cleanup
            this.encryptedPassword = null;
            this.iv = null;
        }
    }
    
    /**
     * Registry statistics
     */
    public static class RegistryStats {
        private final int registeredBlocks;
        private final long estimatedMemoryBytes;
        
        public RegistryStats(int registeredBlocks, long estimatedMemoryBytes) {
            this.registeredBlocks = registeredBlocks;
            this.estimatedMemoryBytes = estimatedMemoryBytes;
        }
        
        public int getRegisteredBlocks() { return registeredBlocks; }
        public long getEstimatedMemoryBytes() { return estimatedMemoryBytes; }
        
        @Override
        public String toString() {
            return String.format("RegistryStats{blocks=%d, memory=%d bytes}", 
                               registeredBlocks, estimatedMemoryBytes);
        }
    }
}