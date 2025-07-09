package com.rbatllet.blockchain.util;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Enhanced cryptographic utility class with modern algorithms and key management
 * FIXED: Complete thread-safety with proper synchronization
 * - SHA-3 for hashing (improved security over SHA-2)
 * - ECDSA for digital signatures (better performance and security than RSA)
 * - Hierarchical key management (root, intermediate, operational)
 * - Key rotation and revocation capabilities
 */
public class CryptoUtil {
    
    // Hash algorithm constant
    public static final String HASH_ALGORITHM = "SHA3-256";
    
    // Signature algorithm constant
    public static final String SIGNATURE_ALGORITHM = "SHA3-256withECDSA";
    
    // EC curve for ECDSA keys
    public static final String EC_CURVE = "secp256r1"; // NIST P-256 curve
    
    // EC algorithm name
    public static final String EC_ALGORITHM = "EC";
    
    // AES-GCM constants for modern encryption
    public static final String AES_ALGORITHM = "AES";
    public static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    public static final int GCM_IV_LENGTH = 12; // 96-bit IV recommended for GCM
    public static final int GCM_TAG_LENGTH = 16; // 128-bit authentication tag
    public static final int AES_KEY_LENGTH = 32; // 256-bit key
    
    // FIXED: Global lock for thread safety on key store operations
    private static final ReentrantReadWriteLock KEY_STORE_LOCK = new ReentrantReadWriteLock();
    
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
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Generate an ECDSA key pair (thread-safe method)
     * 
     * @return A new EC key pair
     */
    public static KeyPair generateECKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(EC_ALGORITHM);
            keyGen.initialize(new ECGenParameterSpec(EC_CURVE));
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Error generating EC key pair", e);
        }
    }
    
    /**
     * Generate a key pair using EC
     * FIXED: Thread-safe with proper synchronization
     * 
     * @return A new key pair
     */
    public static KeyPair generateKeyPair() {
        try {
            // Create a root key using the hierarchical key system
            KeyInfo keyInfo = createRootKey();
            
            // Convert to standard KeyPair
            PublicKey publicKey = stringToPublicKey(keyInfo.getPublicKeyEncoded());
            PrivateKey privateKey = stringToPrivateKey(keyInfo.getPrivateKeyEncoded());
            
            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            throw new RuntimeException("Error generating key pair", e);
        }
    }
    
    /**
     * Sign data with a private key using ECDSA (thread-safe method)
     * 
     * @param data The data to sign
     * @param privateKey The private key to sign with
     * @return Base64 encoded signature
     */
    public static String signData(String data, PrivateKey privateKey) {
        try {
            // Only support EC keys
            if (!privateKey.getAlgorithm().equals(EC_ALGORITHM)) {
                throw new IllegalArgumentException("Only EC keys are supported for signing");
            }
            
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signedData = signature.sign();
            return Base64.getEncoder().encodeToString(signedData);
        } catch (Exception e) {
            throw new RuntimeException("Error signing data: " + e.getMessage(), e);
        }
    }
    
    /**
     * Verify a signature with a public key (thread-safe method)
     * 
     * @param data The original data
     * @param signature The Base64 encoded signature
     * @param publicKey The public key to verify with
     * @return True if signature is valid
     */
    public static boolean verifySignature(String data, String signature, PublicKey publicKey) {
        try {
            // Only support EC keys
            if (!publicKey.getAlgorithm().equals(EC_ALGORITHM)) {
                throw new IllegalArgumentException("Only EC keys are supported for verification");
            }
            
            Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
            sig.initVerify(publicKey);
            sig.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error verifying signature: " + e.getMessage(), e);
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
     * Convert a Base64 string to public key (thread-safe method)
     * 
     * @param publicKeyString Base64 encoded public key
     * @return The decoded public key
     */
    public static PublicKey stringToPublicKey(String publicKeyString) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyString);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(EC_ALGORITHM);
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Error converting string to public key", e);
        }
    }
    
    /**
     * Convert a Base64 string to private key (thread-safe method)
     * 
     * @param privateKeyString Base64 encoded private key
     * @return The decoded private key
     */
    public static PrivateKey stringToPrivateKey(String privateKeyString) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyString);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(EC_ALGORITHM);
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Error converting string to private key", e);
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
            KeyPair keyPair = generateECKeyPair();
            String keyId = UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plus(ROOT_KEY_VALIDITY_DAYS, ChronoUnit.DAYS);
            
            KeyInfo keyInfo = new KeyInfo(
                keyId,
                KeyType.ROOT,
                publicKeyToString(keyPair.getPublic()),
                privateKeyToString(keyPair.getPrivate()),
                now,
                expiresAt,
                null // Root key has no issuer
            );
            
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
            
            KeyPair keyPair = generateECKeyPair();
            String keyId = UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plus(INTERMEDIATE_KEY_VALIDITY_DAYS, ChronoUnit.DAYS);
            
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
            
            KeyPair keyPair = generateECKeyPair();
            String keyId = UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plus(OPERATIONAL_KEY_VALIDITY_DAYS, ChronoUnit.DAYS);
            
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
        KeyPair keyPair = generateECKeyPair();
        String keyId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plus(ROOT_KEY_VALIDITY_DAYS, ChronoUnit.DAYS);
        
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
        
        KeyPair keyPair = generateECKeyPair();
        String keyId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plus(INTERMEDIATE_KEY_VALIDITY_DAYS, ChronoUnit.DAYS);
        
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
        
        KeyPair keyPair = generateECKeyPair();
        String keyId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plus(OPERATIONAL_KEY_VALIDITY_DAYS, ChronoUnit.DAYS);
        
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
            new SecureRandom().nextBytes(iv);
            
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
        new SecureRandom().nextBytes(key);
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
}