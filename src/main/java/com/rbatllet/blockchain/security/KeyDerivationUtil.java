package com.rbatllet.blockchain.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

import static com.rbatllet.blockchain.util.CryptoUtil.getSecureRandom;

/**
 * Utility class for password-based key derivation using PBKDF2WithHmacSHA512.
 * Provides quantum-resistant key derivation with salt and iterations.
 *
 * <p>This utility addresses the security vulnerability of simple hash-based key derivation
 * which is vulnerable to rainbow table attacks and brute force attacks.</p>
 *
 * <p>Key derivation follows NIST recommendations:</p>
 * <ul>
 *   <li>Salt: 128-bit random salt</li>
 *   <li>Iterations: 100,000+ (configurable)</li>
 *   <li>Algorithm: PBKDF2WithHmacSHA512 (quantum-resistant)</li>
 *   <li>Key length: 256 bits for AES-256</li>
 * </ul>
 *
 * @see <a href="https://pages.nist.gov/800-63-3/sp800-63b.html#sec5">NIST Digital Identity Guidelines</a>
 */
public final class KeyDerivationUtil {

    private KeyDerivationUtil() {
        // Utility class - prevent instantiation
    }

    // PBKDF2 key derivation constants (quantum-resistant password-based key derivation)
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA512";
    private static final int DEFAULT_SALT_LENGTH = 16; // 128-bit salt
    private static final int DEFAULT_ITERATIONS = 210000; // OWASP 2023 recommendation for PBKDF2-HMAC-SHA512
    private static final int DEFAULT_KEY_LENGTH = 32; // 256-bit key for AES-256

    /**
     * Generate a random salt for key derivation.
     *
     * @return Random salt bytes (128-bit by default)
     */
    public static byte[] generateSalt() {
        return generateSalt(DEFAULT_SALT_LENGTH);
    }

    /**
     * Generate a random salt of specified length for key derivation.
     *
     * @param length Salt length in bytes
     * @return Random salt bytes
     */
    public static byte[] generateSalt(int length) {
        byte[] salt = new byte[length];
        getSecureRandom().nextBytes(salt);
        return salt;
    }

    /**
     * Derive an AES-256 key from password using PBKDF2WithHmacSHA512.
     * Uses default salt length (16 bytes), iterations (100,000), and key length (32 bytes).
     *
     * <p>This provides quantum-resistant key derivation with salt and iterations.</p>
     *
     * @param password Password to derive key from
     * @param salt Salt bytes (must be at least 16 bytes)
     * @return Derived 256-bit AES key
     * @throws RuntimeException if key derivation fails
     */
    public static byte[] deriveKey(String password, byte[] salt) {
        return deriveKey(password, salt, DEFAULT_ITERATIONS, DEFAULT_KEY_LENGTH);
    }

    /**
     * Derive a key from password using PBKDF2WithHmacSHA512 with custom parameters.
     *
     * @param password Password to derive key from
     * @param salt Salt bytes
     * @param iterations Number of PBKDF2 iterations (recommended: 100,000+)
     * @param keyLength Derived key length in bytes (e.g., 32 for AES-256)
     * @return Derived key bytes
     * @throws RuntimeException if key derivation fails
     */
    public static byte[] deriveKey(String password, byte[] salt, int iterations, int keyLength) {
        try {
            PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                iterations,
                keyLength * 8 // key length in bits
            );
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive key from password using " + PBKDF2_ALGORITHM, e);
        }
    }

    /**
     * Derive a SecretKeySpec from password using PBKDF2WithHmacSHA512.
     * Convenience method for direct use with AES cipher.
     *
     * @param password Password to derive key from
     * @param salt Salt bytes
     * @return SecretKeySpec for AES-256
     */
    public static SecretKeySpec deriveSecretKey(String password, byte[] salt) {
        byte[] keyBytes = deriveKey(password, salt);
        try {
            return new SecretKeySpec(keyBytes, "AES");
        } finally {
            // Clear sensitive data from memory
            Arrays.fill(keyBytes, (byte) 0);
        }
    }

    /**
     * Derive a SecretKeySpec from password using PBKDF2WithHmacSHA512 with custom parameters.
     *
     * @param password Password to derive key from
     * @param salt Salt bytes
     * @param iterations Number of PBKDF2 iterations
     * @param keyLength Derived key length in bytes
     * @return SecretKeySpec
     */
    public static SecretKeySpec deriveSecretKey(String password, byte[] salt, int iterations, int keyLength) {
        byte[] keyBytes = deriveKey(password, salt, iterations, keyLength);
        try {
            return new SecretKeySpec(keyBytes, "AES");
        } finally {
            // Clear sensitive data from memory
            Arrays.fill(keyBytes, (byte) 0);
        }
    }

    /**
     * Get the default salt length in bytes.
     *
     * @return Default salt length (16 bytes = 128 bits)
     */
    public static int getDefaultSaltLength() {
        return DEFAULT_SALT_LENGTH;
    }

    /**
     * Get the default iteration count for PBKDF2.
     *
     * @return Default iterations (100,000)
     */
    public static int getDefaultIterations() {
        return DEFAULT_ITERATIONS;
    }

    /**
     * Get the default key length in bytes.
     *
     * @return Default key length (32 bytes = 256 bits for AES-256)
     */
    public static int getDefaultKeyLength() {
        return DEFAULT_KEY_LENGTH;
    }
}
