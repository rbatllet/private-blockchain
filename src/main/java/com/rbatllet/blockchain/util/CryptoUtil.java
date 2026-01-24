package com.rbatllet.blockchain.util;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Enhanced cryptographic utility class with 256-bit security throughout
 * FIXED: Complete thread-safety with proper synchronization
 *
 * Cryptographic Consistency (256-bit quantum-resistant):
 * - SHA3-256 for hashing (256-bit security, quantum-resistant)
 * - ML-DSA-87 for digital signatures (256-bit security, NIST FIPS 204, quantum-resistant)
 * - AES-256-GCM for encryption (256-bit security, quantum-resistant)
 *
 * Features:
 * - Hierarchical key management (root, intermediate, operational)
 * - Key rotation and revocation capabilities
 * - Optimized for medical/sensitive data (HIPAA/GDPR compliance)
 *
 * @since 1.0.0
 * @version 2.0.0 (Post-Quantum Migration)
 */
public class CryptoUtil {

    // Hash algorithm constant - SHA3-256 (256-bit security, quantum-resistant)
    public static final String HASH_ALGORITHM = "SHA3-256";

    // Post-Quantum Signature Algorithms - ML-DSA (Module-Lattice Digital Signature Algorithm)
    // NIST FIPS 204 standardized quantum-resistant signature algorithm

    /** ML-DSA-87 algorithm name for KeyPairGenerator (256-bit security) */
    public static final String SIGNATURE_ALGORITHM = "ML-DSA-87";

    /** Generic ML-DSA algorithm name for Signature instances */
    public static final String SIGNATURE_INSTANCE = "ML-DSA";

    /** Algorithm display name (use this for user-facing messages and logs) */
    public static final String ALGORITHM_DISPLAY_NAME = SIGNATURE_ALGORITHM;

    /** Full algorithm description for documentation and messages */
    public static final String ALGORITHM_DESCRIPTION = "Module-Lattice Digital Signature Algorithm (NIST FIPS 204)";

    /** Security level in bits (256-bit quantum-resistant security) */
    public static final int SECURITY_LEVEL_BITS = 256;

    /** ML-DSA-87 public key size in bytes (X.509 format) */
    public static final int PUBLIC_KEY_SIZE_BYTES = 2592;

    /** ML-DSA-87 private key size in bytes (PKCS#8 format) */
    public static final int PRIVATE_KEY_SIZE_BYTES = 4896;

    /** ML-DSA-87 signature size in bytes */
    public static final int SIGNATURE_SIZE_BYTES = 4627;
    
    // AES-GCM constants for modern encryption
    public static final String AES_ALGORITHM = "AES";
    public static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    public static final int GCM_IV_LENGTH = 12; // 96-bit IV recommended for GCM
    public static final int GCM_TAG_LENGTH = 16; // 128-bit authentication tag
    public static final int AES_KEY_LENGTH = 32; // 256-bit key
    
    // FIXED: Global lock for thread safety on key store operations
    private static final ReentrantReadWriteLock KEY_STORE_LOCK = new ReentrantReadWriteLock();

    /**
     * Shared SecureRandom singleton for cryptographically secure random number generation.
     *
     * <p>SecureRandom is thread-safe according to Java documentation and can be safely
     * shared across threads. Using a single instance provides:</p>
     * <ul>
     *   <li>Memory efficiency (single instance vs. one per thread)</li>
     *   <li>Single initialization cost instead of per-thread</li>
     *   <li>Native thread-safety without contention</li>
     *   <li>Better entropy pooling across operations</li>
     * </ul>
     *
     * <p>From Oracle Java Docs: "SecureRandom objects are safe for use by multiple
     * concurrent threads."</p>
     *
     * @since 1.0.6
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Get the shared SecureRandom instance.
     *
     * <p>This method is thread-safe and can be called concurrently.</p>
     *
     * @return A thread-safe SecureRandom instance
     * @since 1.0.6
     */
    public static SecureRandom getSecureRandom() {
        return SECURE_RANDOM;
    }
    
    // Key type constants
    public enum KeyType {
        ROOT,
        INTERMEDIATE,
        OPERATIONAL
    }
    
    // Key status constants
    public enum KeyStatus {
        ACTIVE,
        ROTATING,
        REVOKED,
        EXPIRED
    }
    
    // In-memory key store (in production, this should be persisted securely)
    private static final Map<String, KeyInfo> keyStore = new ConcurrentHashMap<>();
    
    // Memory management for keyStore
    private static final int MAX_KEY_STORE_SIZE = 10000;
    private static final long KEY_CLEANUP_INTERVAL_MS = 3600000; // 1 hour
    private static volatile long lastCleanupTime = System.currentTimeMillis();
    
    // Default key validity periods (in days)
    private static final int ROOT_KEY_VALIDITY_DAYS = 1825; // 5 years
    private static final int INTERMEDIATE_KEY_VALIDITY_DAYS = 365; // 1 year
    private static final int OPERATIONAL_KEY_VALIDITY_DAYS = 90; // 90 days
    
    /**
     * Key information class for storing key metadata
     * FIXED: Thread-safe with synchronized methods
     */
    public static class KeyInfo {
        private final String keyId;
        private final KeyType keyType;
        private volatile KeyStatus status; // volatile for thread safety
        private final LocalDateTime createdAt;
        private volatile LocalDateTime expiresAt; // volatile for thread safety
        private volatile LocalDateTime revokedAt; // volatile for thread safety
        private final String publicKeyEncoded;
        private final String privateKeyEncoded; // In production, private keys should be stored securely
        private final String issuerId; // ID of the key that signed this key (null for root)
        private final Set<String> issuedKeyIds; // Thread-safe set
        
        public KeyInfo(String keyId, KeyType keyType, String publicKeyEncoded, 
                      String privateKeyEncoded, LocalDateTime createdAt, 
                      LocalDateTime expiresAt, String issuerId) {
            this.keyId = keyId;
            this.keyType = keyType;
            this.status = KeyStatus.ACTIVE;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.publicKeyEncoded = publicKeyEncoded;
            this.privateKeyEncoded = privateKeyEncoded;
            this.issuerId = issuerId;
            this.issuedKeyIds = Collections.synchronizedSet(new HashSet<>()); // FIXED: Thread-safe set
        }
        
        // Getters (thread-safe)
        public String getKeyId() { return keyId; }
        public KeyType getKeyType() { return keyType; }
        public synchronized KeyStatus getStatus() { return status; }
        public synchronized void setStatus(KeyStatus status) { this.status = status; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public synchronized LocalDateTime getExpiresAt() { return expiresAt; }
        public synchronized void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
        public synchronized LocalDateTime getRevokedAt() { return revokedAt; }
        public synchronized void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
        public String getPublicKeyEncoded() { return publicKeyEncoded; }
        public String getPrivateKeyEncoded() { return privateKeyEncoded; }
        public String getIssuerId() { return issuerId; }
        public Set<String> getIssuedKeyIds() { 
            // Return a defensive copy for thread safety
            synchronized (issuedKeyIds) {
                return new HashSet<>(issuedKeyIds);
            }
        }
        public void addIssuedKeyId(String keyId) { 
            issuedKeyIds.add(keyId); // Synchronized set handles thread safety
        }
    }    
    /**
     * Calculate hash using SHA-3 (thread-safe method)
     * 
     * @param input The string to hash
     * @return The hexadecimal hash string
     */
    public static String calculateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error calculating hash", e);
        }
    }
    
    /**
     * Convert bytes to hexadecimal (thread-safe method)
     *
     * @param bytes The byte array to convert
     * @return Hexadecimal string representation
     */
    private static String bytesToHex(byte[] bytes) {
        // Pre-calculate capacity: each byte becomes 2 hex characters
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Generate an ML-DSA-87 key pair (thread-safe method)
     * Uses ML-DSA-87 parameter set (256-bit security level, NIST FIPS 204)
     *
     * Key sizes (NIST FIPS 204):
     * - Public key: 2,592 bytes (X.509 format)
     * - Private key: 4,896 bytes (PKCS#8 format)
     * - Signature: 4,627 bytes
     *
     * Security level: 256-bit quantum-resistant (Category 5)
     * Recommended for: Medical/sensitive data, HIPAA/GDPR compliance
     *
     * @return A new ML-DSA-87 key pair
     * @since 2.0.0
     */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(SIGNATURE_ALGORITHM);
            // ML-DSA-87 is initialized automatically when using "ML-DSA-87" algorithm name
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Error generating ML-DSA-87 key pair: " + e.getMessage(), e);
        }
    }
    
    /**
     * Clean up expired keys from the key store to prevent memory leaks
     * This method removes expired, revoked keys and enforces size limits
     */
    public static void cleanupExpiredKeys() {
        long currentTime = System.currentTimeMillis();
        
        // Only cleanup if enough time has passed
        if (currentTime - lastCleanupTime < KEY_CLEANUP_INTERVAL_MS) {
            return;
        }
        
        KEY_STORE_LOCK.writeLock().lock();
        try {
            LocalDateTime now = LocalDateTime.now();
            int initialSize = keyStore.size();
            
            // Remove expired and revoked keys
            keyStore.entrySet().removeIf(entry -> {
                KeyInfo keyInfo = entry.getValue();
                return keyInfo.getStatus() == KeyStatus.EXPIRED ||
                       keyInfo.getStatus() == KeyStatus.REVOKED ||
                       (keyInfo.getExpiresAt() != null && now.isAfter(keyInfo.getExpiresAt()));
            });
            
            // If still too large, remove oldest keys
            if (keyStore.size() > MAX_KEY_STORE_SIZE) {
                List<Map.Entry<String, KeyInfo>> entries = new ArrayList<>(keyStore.entrySet());
                entries.sort((a, b) -> a.getValue().getCreatedAt().compareTo(b.getValue().getCreatedAt()));
                
                int toRemove = keyStore.size() - MAX_KEY_STORE_SIZE;
                for (int i = 0; i < toRemove; i++) {
                    keyStore.remove(entries.get(i).getKey());
                }
            }
            
            lastCleanupTime = currentTime;
            int finalSize = keyStore.size();
            
            if (initialSize != finalSize) {
                System.out.println("🧹 Key store cleanup: removed " + (initialSize - finalSize) + 
                                 " keys, size: " + finalSize);
            }
        } finally {
            KEY_STORE_LOCK.writeLock().unlock();
        }
    }
    
    /**
     * Force cleanup of the key store (for testing and shutdown)
     */
    public static void forceCleanupKeyStore() {
        KEY_STORE_LOCK.writeLock().lock();
        try {
            keyStore.clear();
            System.out.println("🧹 Key store force cleanup completed");
        } finally {
            KEY_STORE_LOCK.writeLock().unlock();
        }
    }
    
    /**
     * Get key store statistics for monitoring
     */
    public static Map<String, Object> getKeyStoreStats() {
        KEY_STORE_LOCK.readLock().lock();
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalKeys", keyStore.size());
            stats.put("maxSize", MAX_KEY_STORE_SIZE);
            stats.put("lastCleanup", new Date(lastCleanupTime));
            
            // Count by status
            Map<KeyStatus, Long> statusCounts = keyStore.values().stream()
                .collect(Collectors.groupingBy(KeyInfo::getStatus, Collectors.counting()));
            stats.put("statusCounts", statusCounts);
            
            return stats;
        } finally {
            KEY_STORE_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Check if automatic cleanup is needed and perform it
     */
    private static void checkAndPerformCleanup() {
        if (keyStore.size() > MAX_KEY_STORE_SIZE * 0.9) { // 90% threshold
            cleanupExpiredKeys();
        }
    }
    
    /**
     * Generate a hierarchical root key pair (ML-DSA)
     * FIXED: Thread-safe with proper synchronization
     *
     * @return A new root key pair with metadata
     */
    public static KeyPair generateHierarchicalKeyPair() {
        try {
            // Create a root key using the hierarchical key system
            KeyInfo keyInfo = createRootKey();

            // Convert to standard KeyPair
            PublicKey publicKey = stringToPublicKey(keyInfo.getPublicKeyEncoded());
            PrivateKey privateKey = stringToPrivateKey(keyInfo.getPrivateKeyEncoded());

            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            throw new RuntimeException("Error generating hierarchical key pair", e);
        }
    }
    
    /**
     * Sign data with a private key using ML-DSA-87 (thread-safe method)
     *
     * Produces a 4,627-byte signature (NIST FIPS 204, 256-bit security)
     *
     * @param data The data to sign
     * @param privateKey The ML-DSA-87 private key to sign with
     * @return Base64 encoded signature (~6,169 chars Base64)
     * @throws IllegalArgumentException if the key is not ML-DSA
     * @since 2.0.0
     */
    public static String signData(String data, PrivateKey privateKey) {
        try {
            String keyAlgorithm = privateKey.getAlgorithm();
            if (!keyAlgorithm.equals("ML-DSA") && !keyAlgorithm.startsWith("ML-DSA-")) {
                throw new IllegalArgumentException(
                    "Only ML-DSA keys are supported for signing. Got: " + keyAlgorithm
                );
            }

            Signature signature = Signature.getInstance(SIGNATURE_INSTANCE);
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signedData = signature.sign();
            return Base64.getEncoder().encodeToString(signedData);
        } catch (Exception e) {
            throw new RuntimeException("Error signing data with ML-DSA-87: " + e.getMessage(), e);
        }
    }
    
    /**
     * Verify a signature with a public key using ML-DSA-87 (thread-safe method)
     *
     * Verifies a 4,627-byte ML-DSA-87 signature (NIST FIPS 204, 256-bit security)
     *
     * @param data The original data
     * @param signature The Base64 encoded ML-DSA-87 signature
     * @param publicKey The ML-DSA-87 public key to verify with
     * @return True if signature is valid
     * @throws IllegalArgumentException if the key is not ML-DSA
     * @since 2.0.0
     */
    public static boolean verifySignature(String data, String signature, PublicKey publicKey) {
        try {
            String keyAlgorithm = publicKey.getAlgorithm();
            if (!keyAlgorithm.equals("ML-DSA") && !keyAlgorithm.startsWith("ML-DSA-")) {
                throw new IllegalArgumentException(
                    "Only ML-DSA keys are supported for verification. Got: " + keyAlgorithm
                );
            }

            Signature sig = Signature.getInstance(SIGNATURE_INSTANCE);
            sig.initVerify(publicKey);
            sig.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error verifying ML-DSA-87 signature: " + e.getMessage(), e);
        }
    }
    
    /**
     * Convert a public key to Base64 string (thread-safe method)
     */
    public static String publicKeyToString(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
    
    /**
     * Convert a private key to Base64 string (thread-safe method)
     */
    public static String privateKeyToString(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }
    
    /**
     * Convert a Base64 string to ML-DSA-87 public key (thread-safe method)
     *
     * Expects 2,592-byte X.509 encoded public key (NIST FIPS 204)
     *
     * @param publicKeyString Base64 encoded ML-DSA-87 public key (~3,456 chars Base64)
     * @return The decoded ML-DSA-87 public key
     * @since 2.0.0
     */
    public static PublicKey stringToPublicKey(String publicKeyString) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyString);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(SIGNATURE_ALGORITHM);
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Error converting string to ML-DSA-87 public key: " + e.getMessage(), e);
        }
    }

    /**
     * Convert a Base64 string to ML-DSA-87 private key (thread-safe method)
     *
     * Expects 4,896-byte PKCS#8 encoded private key (NIST FIPS 204)
     *
     * @param privateKeyString Base64 encoded ML-DSA-87 private key (~6,528 chars Base64)
     * @return The decoded ML-DSA-87 private key
     * @since 2.0.0
     */
    public static PrivateKey stringToPrivateKey(String privateKeyString) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyString);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(SIGNATURE_ALGORITHM);
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Error converting string to ML-DSA-87 private key: " + e.getMessage(), e);
        }
    }    
    /**
     * Create a new root key
     * FIXED: Thread-safe with write lock
     * 
     * @return The key info for the new root key
     */
    public static KeyInfo createRootKey() {
        KEY_STORE_LOCK.writeLock().lock();
        try {
            KeyPair keyPair = generateKeyPair();
            String keyId = UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusDays(ROOT_KEY_VALIDITY_DAYS);
            
            KeyInfo keyInfo = new KeyInfo(
                keyId,
                KeyType.ROOT,
                publicKeyToString(keyPair.getPublic()),
                privateKeyToString(keyPair.getPrivate()),
                now,
                expiresAt,
                null // Root key has no issuer
            );
            
            // Check if cleanup is needed before adding new key
            checkAndPerformCleanup();
            // Check if cleanup is needed before adding new key
            checkAndPerformCleanup();
            keyStore.put(keyId, keyInfo);
            return keyInfo;
        } finally {
            KEY_STORE_LOCK.writeLock().unlock();
        }
    }
    
    /**
     * Create an intermediate key signed by a parent key
     * FIXED: Thread-safe with write lock
     * 
     * @param parentKeyId The ID of the parent key
     * @return The key info for the new intermediate key
     */
    public static KeyInfo createIntermediateKey(String parentKeyId) {
        KEY_STORE_LOCK.writeLock().lock();
        try {
            KeyInfo parentKey = keyStore.get(parentKeyId);
            if (parentKey == null) {
                throw new IllegalArgumentException("Parent key not found: " + parentKeyId);
            }
            
            if (parentKey.getStatus() != KeyStatus.ACTIVE) {
                throw new IllegalStateException("Parent key is not active");
            }
            
            if (parentKey.getKeyType() == KeyType.OPERATIONAL) {
                throw new IllegalArgumentException("Operational keys cannot issue intermediate keys");
            }
            
            KeyPair keyPair = generateKeyPair();
            String keyId = UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusDays(INTERMEDIATE_KEY_VALIDITY_DAYS);
            
            // Ensure child key doesn't outlive parent
            if (expiresAt.isAfter(parentKey.getExpiresAt())) {
                expiresAt = parentKey.getExpiresAt();
            }
            
            KeyInfo keyInfo = new KeyInfo(
                keyId,
                KeyType.INTERMEDIATE,
                publicKeyToString(keyPair.getPublic()),
                privateKeyToString(keyPair.getPrivate()),
                now,
                expiresAt,
                parentKeyId
            );
            
            // Update parent's issued keys list (thread-safe)
            parentKey.addIssuedKeyId(keyId);
            
            // Check if cleanup is needed before adding new key
            checkAndPerformCleanup();
            keyStore.put(keyId, keyInfo);
            return keyInfo;
        } finally {
            KEY_STORE_LOCK.writeLock().unlock();
        }
    }
    
    /**
     * Create an operational key signed by a parent key
     * FIXED: Thread-safe with write lock
     * 
     * @param parentKeyId The ID of the parent key
     * @return The key info for the new operational key
     */
    public static KeyInfo createOperationalKey(String parentKeyId) {
        KEY_STORE_LOCK.writeLock().lock();
        try {
            KeyInfo parentKey = keyStore.get(parentKeyId);
            if (parentKey == null) {
                throw new IllegalArgumentException("Parent key not found: " + parentKeyId);
            }
            
            if (parentKey.getStatus() != KeyStatus.ACTIVE) {
                throw new IllegalStateException("Parent key is not active");
            }
            
            if (parentKey.getKeyType() == KeyType.OPERATIONAL) {
                throw new IllegalArgumentException("Operational keys cannot issue other keys");
            }
            
            KeyPair keyPair = generateKeyPair();
            String keyId = UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusDays(OPERATIONAL_KEY_VALIDITY_DAYS);
            
            // Ensure child key doesn't outlive parent
            if (expiresAt.isAfter(parentKey.getExpiresAt())) {
                expiresAt = parentKey.getExpiresAt();
            }
            
            KeyInfo keyInfo = new KeyInfo(
                keyId,
                KeyType.OPERATIONAL,
                publicKeyToString(keyPair.getPublic()),
                privateKeyToString(keyPair.getPrivate()),
                now,
                expiresAt,
                parentKeyId
            );
            
            // Update parent's issued keys list (thread-safe)
            parentKey.addIssuedKeyId(keyId);
            
            // Check if cleanup is needed before adding new key
            checkAndPerformCleanup();
            keyStore.put(keyId, keyInfo);
            return keyInfo;
        } finally {
            KEY_STORE_LOCK.writeLock().unlock();
        }
    }
    
    /**
     * Revoke a key and optionally all keys issued by it
     * FIXED: Thread-safe with write lock
     * 
     * @param keyId The ID of the key to revoke
     * @param cascade If true, also revoke all keys issued by this key
     */
    public static void revokeKey(String keyId, boolean cascade) {
        KEY_STORE_LOCK.writeLock().lock();
        try {
            KeyInfo keyInfo = keyStore.get(keyId);
            if (keyInfo == null) {
                throw new IllegalArgumentException("Key not found: " + keyId);
            }
            
            keyInfo.setStatus(KeyStatus.REVOKED);
            keyInfo.setRevokedAt(LocalDateTime.now());
            
            if (cascade) {
                Set<String> issuedKeyIds = keyInfo.getIssuedKeyIds(); // Gets defensive copy
                for (String issuedKeyId : issuedKeyIds) {
                    revokeKey(issuedKeyId, true); // Recursive call within same lock
                }
            }
        } finally {
            KEY_STORE_LOCK.writeLock().unlock();
        }
    }    /**
     * Rotate a key by creating a new key of the same type and marking the old one as rotating
     * FIXED: Thread-safe with write lock
     * 
     * @param keyId The ID of the key to rotate
     * @return The new key info
     */
    public static KeyInfo rotateKey(String keyId) {
        KEY_STORE_LOCK.writeLock().lock();
        try {
            KeyInfo oldKeyInfo = keyStore.get(keyId);
            if (oldKeyInfo == null) {
                throw new IllegalArgumentException("Key not found: " + keyId);
            }
            
            if (oldKeyInfo.getStatus() != KeyStatus.ACTIVE) {
                throw new IllegalStateException("Only active keys can be rotated");
            }
            
            // Create new key of same type (internal methods to avoid double locking)
            KeyInfo newKeyInfo;
            switch (oldKeyInfo.getKeyType()) {
                case ROOT:
                    newKeyInfo = createRootKeyInternal();
                    break;
                case INTERMEDIATE:
                    if (oldKeyInfo.getIssuerId() == null) {
                        throw new IllegalStateException("Intermediate key has no issuer");
                    }
                    newKeyInfo = createIntermediateKeyInternal(oldKeyInfo.getIssuerId());
                    break;
                case OPERATIONAL:
                    if (oldKeyInfo.getIssuerId() == null) {
                        throw new IllegalStateException("Operational key has no issuer");
                    }
                    newKeyInfo = createOperationalKeyInternal(oldKeyInfo.getIssuerId());
                    break;
                default:
                    throw new IllegalStateException("Unknown key type");
            }
            
            // Mark old key as rotating
            oldKeyInfo.setStatus(KeyStatus.ROTATING);
            
            return newKeyInfo;
        } finally {
            KEY_STORE_LOCK.writeLock().unlock();
        }
    }
    
    /**
     * Internal method to create root key (assumes lock is already held)
     */
    private static KeyInfo createRootKeyInternal() {
        KeyPair keyPair = generateKeyPair();
        String keyId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(ROOT_KEY_VALIDITY_DAYS);
        
        KeyInfo keyInfo = new KeyInfo(
            keyId,
            KeyType.ROOT,
            publicKeyToString(keyPair.getPublic()),
            privateKeyToString(keyPair.getPrivate()),
            now,
            expiresAt,
            null
        );
        
        keyStore.put(keyId, keyInfo);
        return keyInfo;
    }
    
    /**
     * Internal method to create intermediate key (assumes lock is already held)
     */
    private static KeyInfo createIntermediateKeyInternal(String parentKeyId) {
        KeyInfo parentKey = keyStore.get(parentKeyId);
        if (parentKey == null) {
            throw new IllegalArgumentException("Parent key not found: " + parentKeyId);
        }
        
        KeyPair keyPair = generateKeyPair();
        String keyId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(INTERMEDIATE_KEY_VALIDITY_DAYS);
        
        if (expiresAt.isAfter(parentKey.getExpiresAt())) {
            expiresAt = parentKey.getExpiresAt();
        }
        
        KeyInfo keyInfo = new KeyInfo(
            keyId,
            KeyType.INTERMEDIATE,
            publicKeyToString(keyPair.getPublic()),
            privateKeyToString(keyPair.getPrivate()),
            now,
            expiresAt,
            parentKeyId
        );
        
        parentKey.addIssuedKeyId(keyId);
        keyStore.put(keyId, keyInfo);
        return keyInfo;
    }
    
    /**
     * Internal method to create operational key (assumes lock is already held)
     */
    private static KeyInfo createOperationalKeyInternal(String parentKeyId) {
        KeyInfo parentKey = keyStore.get(parentKeyId);
        if (parentKey == null) {
            throw new IllegalArgumentException("Parent key not found: " + parentKeyId);
        }
        
        KeyPair keyPair = generateKeyPair();
        String keyId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(OPERATIONAL_KEY_VALIDITY_DAYS);
        
        if (expiresAt.isAfter(parentKey.getExpiresAt())) {
            expiresAt = parentKey.getExpiresAt();
        }
        
        KeyInfo keyInfo = new KeyInfo(
            keyId,
            KeyType.OPERATIONAL,
            publicKeyToString(keyPair.getPublic()),
            privateKeyToString(keyPair.getPrivate()),
            now,
            expiresAt,
            parentKeyId
        );
        
        parentKey.addIssuedKeyId(keyId);
        keyStore.put(keyId, keyInfo);
        return keyInfo;
    }
    
    /**
     * Get all keys of a specific type
     * FIXED: Thread-safe with read lock
     * 
     * @param keyType The type of keys to get
     * @return A list of key infos
     */
    public static List<KeyInfo> getKeysByType(KeyType keyType) {
        KEY_STORE_LOCK.readLock().lock();
        try {
            return keyStore.values().stream()
                .filter(keyInfo -> keyInfo.getKeyType() == keyType)
                .collect(Collectors.toList());
        } finally {
            KEY_STORE_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Get all active keys
     * FIXED: Thread-safe with read lock
     * 
     * @return A list of active key infos
     */
    public static List<KeyInfo> getActiveKeys() {
        KEY_STORE_LOCK.readLock().lock();
        try {
            return keyStore.values().stream()
                .filter(keyInfo -> keyInfo.getStatus() == KeyStatus.ACTIVE)
                .collect(Collectors.toList());
        } finally {
            KEY_STORE_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Check if a key is valid (active and not expired)
     * FIXED: Thread-safe with read lock
     * 
     * @param keyId The ID of the key to check
     * @return True if the key is valid
     */
    public static boolean isKeyValid(String keyId) {
        KEY_STORE_LOCK.readLock().lock();
        try {
            KeyInfo keyInfo = keyStore.get(keyId);
            if (keyInfo == null) {
                return false;
            }
            
            return keyInfo.getStatus() == KeyStatus.ACTIVE && 
                   LocalDateTime.now().isBefore(keyInfo.getExpiresAt());
        } finally {
            KEY_STORE_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Encrypt data using AES-256-GCM with password-based key derivation
     * Thread-safe method for authenticated encryption
     * 
     * @param plaintext The data to encrypt
     * @param password The password for key derivation
     * @return Base64 encoded encrypted data (IV + ciphertext + auth tag)
     */
    public static String encryptWithGCM(String plaintext, String password) {
        try {
            if (plaintext == null || plaintext.isEmpty()) {
                throw new IllegalArgumentException("Plaintext cannot be null or empty");
            }
            if (password == null || password.isEmpty()) {
                throw new IllegalArgumentException("Password cannot be null or empty");
            }
            
            // Derive key from password using SHA-3-256
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] key = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            getSecureRandom().nextBytes(iv);
            
            // Encrypt with AES-256-GCM
            SecretKeySpec secretKey = new SecretKeySpec(key, AES_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV + ciphertext (includes auth tag)
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            
            // Clear sensitive data
            Arrays.fill(key, (byte) 0);
            
            return Base64.getEncoder().encodeToString(combined);
            
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting with GCM: " + e.getMessage(), e);
        }
    }
    
    /**
     * Decrypt data using AES-256-GCM with password-based key derivation
     * Thread-safe method for authenticated decryption
     * 
     * @param encryptedData Base64 encoded encrypted data (IV + ciphertext + auth tag)
     * @param password The password for key derivation
     * @return The decrypted plaintext
     */
    public static String decryptWithGCM(String encryptedData, String password) {
        try {
            if (encryptedData == null || encryptedData.isEmpty()) {
                throw new IllegalArgumentException("Encrypted data cannot be null or empty");
            }
            if (password == null || password.isEmpty()) {
                throw new IllegalArgumentException("Password cannot be null or empty");
            }
            
            // Derive key from password using SHA-3-256
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] key = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            
            // Decode the combined data
            byte[] combined = Base64.getDecoder().decode(encryptedData);
            
            if (combined.length < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted data length");
            }
            
            // Extract IV and ciphertext
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
            
            // Decrypt with AES-256-GCM
            SecretKeySpec secretKey = new SecretKeySpec(key, AES_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            
            byte[] plaintext = cipher.doFinal(ciphertext);
            
            // Clear sensitive data
            Arrays.fill(key, (byte) 0);
            
            return new String(plaintext, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting with GCM: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate a random AES-256 key
     * Thread-safe method for key generation
     * 
     * @return Base64 encoded random key
     */
    public static String generateAESKey() {
        byte[] key = new byte[AES_KEY_LENGTH];
        getSecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
    
    /**
     * Derive a key from password using SHA-3-256
     * Thread-safe method for password-based key derivation
     * 
     * @param password The password to derive key from
     * @param salt Optional salt (can be null)
     * @return Base64 encoded derived key
     */
    public static String deriveKeyFromPassword(String password, String salt) {
        try {
            if (password == null || password.isEmpty()) {
                throw new IllegalArgumentException("Password cannot be null or empty");
            }

            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);

            if (salt != null && !salt.isEmpty()) {
                digest.update(salt.getBytes(StandardCharsets.UTF_8));
            }

            byte[] key = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(key);

        } catch (Exception e) {
            throw new RuntimeException("Error deriving key from password: " + e.getMessage(), e);
        }
    }

    /**
     * Create an admin signature for dangerous operations
     * This helper method centralizes the signature creation logic used across multiple classes
     *
     * @param publicKey The public key being deleted
     * @param force Whether to force the deletion
     * @param reason The reason for the deletion
     * @param adminPrivateKey The admin's private key for signing
     * @return The admin signature string
     */
    public static String createAdminSignature(String publicKey, boolean force, String reason, PrivateKey adminPrivateKey) {
        long timestamp = System.currentTimeMillis() / 1000;
        // Use StringBuilder for efficient string concatenation
        StringBuilder message = new StringBuilder(publicKey.length() + reason.length() + 32);
        message.append(publicKey).append('|').append(force).append('|').append(reason).append('|').append(timestamp);
        return signData(message.toString(), adminPrivateKey);
    }
}