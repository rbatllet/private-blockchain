package com.rbatllet.blockchain.security;

import com.rbatllet.blockchain.util.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Utility class for loading cryptographic keys from various file formats
 * Supports PEM, DER, and raw Base64 formats for private and public keys
 */
public class KeyFileLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(KeyFileLoader.class);
    
    // PEM format patterns
    private static final Pattern PRIVATE_KEY_PEM_PATTERN = Pattern.compile(
        "-----BEGIN (EC )?PRIVATE KEY-----([\\s\\S]*?)-----END (EC )?PRIVATE KEY-----");
    
    private static final Pattern PUBLIC_KEY_PEM_PATTERN = Pattern.compile(
        "-----BEGIN (EC )?PUBLIC KEY-----([\\s\\S]*?)-----END (EC )?PUBLIC KEY-----");
    
    private static final Pattern PKCS8_PRIVATE_KEY_PEM_PATTERN = Pattern.compile(
        "-----BEGIN PRIVATE KEY-----([\\s\\S]*?)-----END PRIVATE KEY-----");
    
    /**
     * Load a private key from a file
     * Supports multiple formats: PEM, DER, and raw Base64
     * 
     * @param keyFilePath Path to the key file
     * @return PrivateKey object or null if loading fails
     */
    public static PrivateKey loadPrivateKeyFromFile(String keyFilePath) {
        if (keyFilePath == null || keyFilePath.trim().isEmpty()) {
            return null;
        }
        
        try {
            Path path = Paths.get(keyFilePath);
            
            // Check if file exists
            if (!Files.exists(path)) {
                logger.error("🔍 Key file not found: {}", keyFilePath);
                return null;
            }
            
            // Check if file is readable
            if (!Files.isReadable(path)) {
                logger.error("🔍 Key file is not readable: {}", keyFilePath);
                return null;
            }
            
            // Check if file is empty
            if (Files.size(path) == 0) {
                logger.error("🔍 Key file is empty: {}", keyFilePath);
                return null;
            }
            
            // Try different formats
            PrivateKey privateKey = null;
            
            // 1. First try DER format (binary) if file extension suggests it or if reading as text fails
            if (keyFilePath.toLowerCase().endsWith(".der")) {
                privateKey = loadPrivateKeyFromDER(path);
                if (privateKey != null) {
                    return privateKey;
                }
            }
            
            // 2. Try to read as text for PEM/Base64 formats
            String content = null;
            try {
                content = Files.readString(path).trim();
                
                if (content.isEmpty()) {
                    logger.error("❌ Key file is empty: {}", keyFilePath);
                    return null;
                }
                
                // 3. Try PEM format (most common)
                privateKey = loadPrivateKeyFromPEM(content);
                if (privateKey != null) {
                    return privateKey;
                }
                
                // 4. Try raw Base64 format
                privateKey = loadPrivateKeyFromBase64(content);
                if (privateKey != null) {
                    return privateKey;
                }
                
            } catch (Exception textReadException) {
                // If reading as text fails, it might be a binary DER file
                // Try DER format as fallback
                privateKey = loadPrivateKeyFromDER(path);
                if (privateKey != null) {
                    return privateKey;
                }
            }
            
            // 5. Final attempt: try DER if not tried yet
            if (!keyFilePath.toLowerCase().endsWith(".der") && content != null) {
                privateKey = loadPrivateKeyFromDER(path);
                if (privateKey != null) {
                    return privateKey;
                }
            }
            
            logger.error("🔍 Unable to parse private key from file: {}", keyFilePath);
            logger.error("🔍 Supported formats: PEM, DER, Base64");
            return null;
            
        } catch (Exception e) {
            logger.error("🔍 Error loading private key from file: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Load private key from PEM format
     */
    private static PrivateKey loadPrivateKeyFromPEM(String pemContent) {
        try {
            // Try PKCS#8 format first (BEGIN PRIVATE KEY)
            var pkcs8Matcher = PKCS8_PRIVATE_KEY_PEM_PATTERN.matcher(pemContent);
            if (pkcs8Matcher.find()) {
                String base64Key = pkcs8Matcher.group(1).replaceAll("\\s", "");
                return parsePrivateKeyPKCS8(base64Key);
            }
            
            // Try traditional EC format (BEGIN EC PRIVATE KEY) - Legacy ECDSA format, not supported
            var ecMatcher = PRIVATE_KEY_PEM_PATTERN.matcher(pemContent);
            if (ecMatcher.find()) {
                // Legacy ECDSA format not supported (project uses ML-DSA-87 post-quantum cryptography)
                // Only PKCS#8 format is supported for ML-DSA keys
                logger.warn("⚠️ Legacy EC PRIVATE KEY format detected (ECDSA). Not supported.");
                logger.warn("⚠️ This project uses ML-DSA-87 post-quantum cryptography with PKCS#8 format.");
                logger.warn("⚠️ Please generate new ML-DSA-87 keys using CryptoUtil.generateKeyPair()");
                return null;
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Load private key from raw Base64 format
     */
    private static PrivateKey loadPrivateKeyFromBase64(String base64Content) {
        try {
            // Remove any whitespace and newlines
            String cleanBase64 = base64Content.replaceAll("\\s", "");
            return parsePrivateKeyPKCS8(cleanBase64);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Load private key from DER format (binary)
     */
    private static PrivateKey loadPrivateKeyFromDER(Path keyFilePath) {
        try {
            byte[] keyBytes = Files.readAllBytes(keyFilePath);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(CryptoUtil.SIGNATURE_ALGORITHM);
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Parse private key from Base64 string using PKCS#8 format
     */
    private static PrivateKey parsePrivateKeyPKCS8(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(CryptoUtil.SIGNATURE_ALGORITHM);
        return keyFactory.generatePrivate(keySpec);
    }
    
    /**
     * Load a public key from a file
     * This method is provided for completeness, though typically
     * public keys are derived from authorized keys in the blockchain
     * 
     * @param keyFilePath Path to the public key file
     * @return PublicKey object or null if loading fails
     */
    public static PublicKey loadPublicKeyFromFile(String keyFilePath) {
        if (keyFilePath == null || keyFilePath.trim().isEmpty()) {
            return null;
        }
        
        try {
            Path path = Paths.get(keyFilePath);
            
            if (!Files.exists(path) || !Files.isReadable(path)) {
                return null;
            }
            
            String content = Files.readString(path).trim();
            
            // Try PEM format
            var matcher = PUBLIC_KEY_PEM_PATTERN.matcher(content);
            if (matcher.find()) {
                String base64Key = matcher.group(2).replaceAll("\\s", "");
                byte[] keyBytes = Base64.getDecoder().decode(base64Key);
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance(CryptoUtil.SIGNATURE_ALGORITHM);
                return keyFactory.generatePublic(keySpec);
            }
            
            // Try raw Base64
            try {
                String cleanBase64 = content.replaceAll("\\s", "");
                byte[] keyBytes = Base64.getDecoder().decode(cleanBase64);
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance(CryptoUtil.SIGNATURE_ALGORITHM);
                return keyFactory.generatePublic(keySpec);
            } catch (Exception e) {
                // Ignore and return null
            }
            
            return null;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Validate that a file path is safe to read
     */
    public static boolean isValidKeyFilePath(String keyFilePath) {
        if (keyFilePath == null || keyFilePath.trim().isEmpty()) {
            return false;
        }
        
        try {
            Path path = Paths.get(keyFilePath);
            File file = path.toFile();
            
            // Check if file exists and is a regular file
            if (!file.exists() || !file.isFile()) {
                return false;
            }
            
            // Check if file is readable
            if (!file.canRead()) {
                return false;
            }
            
            // Basic security check: don't allow reading from system directories
            String absolutePath = file.getAbsolutePath();
            if (absolutePath.startsWith("/etc/") || 
                absolutePath.startsWith("/bin/") || 
                absolutePath.startsWith("/usr/bin/") ||
                absolutePath.startsWith("/sbin/") ||
                absolutePath.startsWith("/usr/sbin/")) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Load a complete KeyPair from two separate files (private + public keys)
     * Required for ML-DSA where public keys cannot be derived from private keys
     *
     * @param privateKeyPath Path to the private key file
     * @param publicKeyPath Path to the public key file
     * @return Complete KeyPair if both keys loaded successfully, null otherwise
     */
    public static KeyPair loadKeyPairFromFiles(String privateKeyPath, String publicKeyPath) {
        try {
            PrivateKey privateKey = loadPrivateKeyFromFile(privateKeyPath);
            if (privateKey == null) {
                logger.error("❌ Failed to load private key from: {}", privateKeyPath);
                return null;
            }

            PublicKey publicKey = loadPublicKeyFromFile(publicKeyPath);
            if (publicKey == null) {
                logger.error("❌ Failed to load public key from: {}", publicKeyPath);
                return null;
            }

            return new KeyPair(publicKey, privateKey);

        } catch (Exception e) {
            logger.error("❌ Error loading KeyPair from files: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Load a complete KeyPair from a single file containing both keys
     * Format: [4 bytes: private length][private bytes PKCS#8][public bytes X.509]
     *
     * @param keyPairPath Path to the combined KeyPair file
     * @return Complete KeyPair if successful, null otherwise
     */
    public static KeyPair loadKeyPairFromFile(String keyPairPath) {
        try {
            Path path = Paths.get(keyPairPath);

            if (!Files.exists(path) || !Files.isReadable(path)) {
                logger.error("❌ KeyPair file not found or not readable: {}", keyPairPath);
                return null;
            }

            byte[] fileBytes = Files.readAllBytes(path);

            if (fileBytes.length < 4) {
                logger.error("❌ KeyPair file too small: {}", keyPairPath);
                return null;
            }

            // Read private key length (big-endian)
            int privateKeyLength = ((fileBytes[0] & 0xFF) << 24) |
                                   ((fileBytes[1] & 0xFF) << 16) |
                                   ((fileBytes[2] & 0xFF) << 8) |
                                   (fileBytes[3] & 0xFF);

            if (4 + privateKeyLength > fileBytes.length) {
                logger.error("❌ Invalid KeyPair file format: {}", keyPairPath);
                return null;
            }

            // Extract private and public key bytes
            byte[] privateKeyBytes = new byte[privateKeyLength];
            byte[] publicKeyBytes = new byte[fileBytes.length - 4 - privateKeyLength];

            System.arraycopy(fileBytes, 4, privateKeyBytes, 0, privateKeyLength);
            System.arraycopy(fileBytes, 4 + privateKeyLength, publicKeyBytes, 0, publicKeyBytes.length);

            // Reconstruct keys
            KeyFactory keyFactory = KeyFactory.getInstance(CryptoUtil.SIGNATURE_ALGORITHM);

            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            return new KeyPair(publicKey, privateKey);

        } catch (Exception e) {
            logger.error("❌ Error loading KeyPair from file: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Save a complete KeyPair to two separate files (PEM format)
     *
     * @param keyPair KeyPair to save
     * @param privateKeyPath Path for private key file (PKCS#8 PEM)
     * @param publicKeyPath Path for public key file (X.509 PEM)
     * @return true if both files saved successfully, false otherwise
     */
    public static boolean saveKeyPairToFiles(KeyPair keyPair, String privateKeyPath, String publicKeyPath) {
        try {
            if (keyPair == null || keyPair.getPrivate() == null || keyPair.getPublic() == null) {
                return false;
            }

            // Save private key in PKCS#8 PEM format
            byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
            String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" +
                Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(privateKeyBytes) +
                "\n-----END PRIVATE KEY-----\n";

            Files.write(Paths.get(privateKeyPath), privateKeyPem.getBytes(StandardCharsets.UTF_8));

            // Save public key in X.509 PEM format
            byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
            String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" +
                Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(publicKeyBytes) +
                "\n-----END PUBLIC KEY-----\n";

            Files.write(Paths.get(publicKeyPath), publicKeyPem.getBytes(StandardCharsets.UTF_8));

            return true;

        } catch (Exception e) {
            logger.error("❌ Error saving KeyPair to files: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Save a complete KeyPair to a single binary file
     * Format: [4 bytes: private length][private bytes PKCS#8][public bytes X.509]
     *
     * @param keyPair KeyPair to save
     * @param keyPairPath Path for combined KeyPair file
     * @return true if saved successfully, false otherwise
     */
    public static boolean saveKeyPairToFile(KeyPair keyPair, String keyPairPath) {
        try {
            if (keyPair == null || keyPair.getPrivate() == null || keyPair.getPublic() == null) {
                return false;
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

            // Write to file
            Files.write(Paths.get(keyPairPath), combined);

            return true;

        } catch (Exception e) {
            logger.error("❌ Error saving KeyPair to file: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get file format information for debugging
     */
    public static String detectKeyFileFormat(String keyFilePath) {
        try {
            Path path = Paths.get(keyFilePath);
            if (!Files.exists(path)) {
                return "File not found";
            }
            
            // Check if it's likely a DER file (binary) by extension or try reading as text first
            if (keyFilePath.toLowerCase().endsWith(".der")) {
                return "DER (Binary)";
            }
            
            try {
                String content = Files.readString(path).trim();
                
                if (content.contains("-----BEGIN PRIVATE KEY-----")) {
                    return "PEM (PKCS#8)";
                } else if (content.contains("-----BEGIN EC PRIVATE KEY-----")) {
                    return "PEM (PKCS#1/EC)";
                } else if (content.contains("-----BEGIN PUBLIC KEY-----")) {
                    return "PEM (Public Key)";
                } else if (content.matches("^[A-Za-z0-9+/=\\s]+$")) {
                    return "Raw Base64";
                } else {
                    return "Unknown text format";
                }
            } catch (Exception e) {
                // If reading as text fails, it's likely a binary file
                return "DER (Binary) or Unknown";
            }
            
        } catch (Exception e) {
            return "Error detecting format: " + e.getMessage();
        }
    }
}
