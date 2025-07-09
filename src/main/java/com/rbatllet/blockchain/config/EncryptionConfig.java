package com.rbatllet.blockchain.config;

/**
 * Configuration class for encryption settings
 * Provides centralized management of encryption parameters
 */
public class EncryptionConfig {
    
    // Default encryption settings
    public static final String DEFAULT_ENCRYPTION_ALGORITHM = "AES";
    public static final String DEFAULT_ENCRYPTION_MODE = "GCM";
    public static final String DEFAULT_PADDING = "NoPadding";
    public static final int DEFAULT_KEY_LENGTH = 256; // bits
    public static final int DEFAULT_IV_LENGTH = 12; // bytes for GCM
    public static final int DEFAULT_TAG_LENGTH = 128; // bits for GCM
    public static final int DEFAULT_SALT_LENGTH = 32; // bytes
    public static final int DEFAULT_PBKDF2_ITERATIONS = 100000;
    public static final String DEFAULT_PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    
    // Performance settings
    public static final int DEFAULT_MIN_PASSWORD_LENGTH = 12;
    public static final int DEFAULT_MAX_DATA_SIZE_BYTES = 1024 * 1024; // 1MB
    public static final boolean DEFAULT_VALIDATE_ENCRYPTION_FORMAT = true;
    public static final boolean DEFAULT_ENABLE_COMPRESSION = false;
    
    // Security settings
    public static final boolean DEFAULT_SECURE_RANDOM_ENABLED = true;
    public static final boolean DEFAULT_METADATA_ENCRYPTION_ENABLED = false;
    public static final boolean DEFAULT_CORRUPTION_DETECTION_ENABLED = true;
    
    // Instance fields for customizable configuration
    private String encryptionAlgorithm;
    private String encryptionMode;
    private String padding;
    private int keyLength;
    private int ivLength;
    private int tagLength;
    private int saltLength;
    private int pbkdf2Iterations;
    private String pbkdf2Algorithm;
    private int minPasswordLength;
    private int maxDataSizeBytes;
    private boolean validateEncryptionFormat;
    private boolean enableCompression;
    private boolean secureRandomEnabled;
    private boolean metadataEncryptionEnabled;
    private boolean corruptionDetectionEnabled;
    
    /**
     * Create configuration with default settings
     */
    public EncryptionConfig() {
        this.encryptionAlgorithm = DEFAULT_ENCRYPTION_ALGORITHM;
        this.encryptionMode = DEFAULT_ENCRYPTION_MODE;
        this.padding = DEFAULT_PADDING;
        this.keyLength = DEFAULT_KEY_LENGTH;
        this.ivLength = DEFAULT_IV_LENGTH;
        this.tagLength = DEFAULT_TAG_LENGTH;
        this.saltLength = DEFAULT_SALT_LENGTH;
        this.pbkdf2Iterations = DEFAULT_PBKDF2_ITERATIONS;
        this.pbkdf2Algorithm = DEFAULT_PBKDF2_ALGORITHM;
        this.minPasswordLength = DEFAULT_MIN_PASSWORD_LENGTH;
        this.maxDataSizeBytes = DEFAULT_MAX_DATA_SIZE_BYTES;
        this.validateEncryptionFormat = DEFAULT_VALIDATE_ENCRYPTION_FORMAT;
        this.enableCompression = DEFAULT_ENABLE_COMPRESSION;
        this.secureRandomEnabled = DEFAULT_SECURE_RANDOM_ENABLED;
        this.metadataEncryptionEnabled = DEFAULT_METADATA_ENCRYPTION_ENABLED;
        this.corruptionDetectionEnabled = DEFAULT_CORRUPTION_DETECTION_ENABLED;
    }
    
    /**
     * Get the full encryption transformation string
     * @return Full transformation string (e.g., "AES/GCM/NoPadding")
     */
    public String getEncryptionTransformation() {
        return encryptionAlgorithm + "/" + encryptionMode + "/" + padding;
    }
    
    /**
     * Create a builder for custom configuration
     * @return Configuration builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Create configuration optimized for high security
     * @return High security configuration
     */
    public static EncryptionConfig createHighSecurityConfig() {
        return new Builder()
                .keyLength(256)
                .pbkdf2Iterations(150000)
                .minPasswordLength(16)
                .metadataEncryptionEnabled(true)
                .corruptionDetectionEnabled(true)
                .validateEncryptionFormat(true)
                .build();
    }
    
    /**
     * Create configuration optimized for performance
     * @return Performance optimized configuration
     */
    public static EncryptionConfig createPerformanceConfig() {
        return new Builder()
                .keyLength(128)
                .pbkdf2Iterations(50000)
                .minPasswordLength(8)
                .validateEncryptionFormat(false)
                .enableCompression(true)
                .build();
    }
    
    /**
     * Create configuration with balanced security and performance
     * @return Balanced configuration
     */
    public static EncryptionConfig createBalancedConfig() {
        return new Builder()
                .keyLength(256)
                .pbkdf2Iterations(100000)
                .minPasswordLength(12)
                .metadataEncryptionEnabled(true)
                .validateEncryptionFormat(true)
                .enableCompression(true)
                .build();
    }
    
    /**
     * Create configuration for testing purposes
     * @return Testing configuration with reduced security for speed
     */
    public static EncryptionConfig createTestConfig() {
        return new Builder()
                .keyLength(128)
                .pbkdf2Iterations(1000)
                .minPasswordLength(4)
                .validateEncryptionFormat(false)
                .corruptionDetectionEnabled(false)
                .build();
    }
    
    // Getters and setters
    
    public String getEncryptionAlgorithm() { return encryptionAlgorithm; }
    public void setEncryptionAlgorithm(String encryptionAlgorithm) { this.encryptionAlgorithm = encryptionAlgorithm; }
    
    public String getEncryptionMode() { return encryptionMode; }
    public void setEncryptionMode(String encryptionMode) { this.encryptionMode = encryptionMode; }
    
    public String getPadding() { return padding; }
    public void setPadding(String padding) { this.padding = padding; }
    
    public int getKeyLength() { return keyLength; }
    public void setKeyLength(int keyLength) { this.keyLength = keyLength; }
    
    public int getIvLength() { return ivLength; }
    public void setIvLength(int ivLength) { this.ivLength = ivLength; }
    
    public int getTagLength() { return tagLength; }
    public void setTagLength(int tagLength) { this.tagLength = tagLength; }
    
    public int getSaltLength() { return saltLength; }
    public void setSaltLength(int saltLength) { this.saltLength = saltLength; }
    
    public int getPbkdf2Iterations() { return pbkdf2Iterations; }
    public void setPbkdf2Iterations(int pbkdf2Iterations) { this.pbkdf2Iterations = pbkdf2Iterations; }
    
    public String getPbkdf2Algorithm() { return pbkdf2Algorithm; }
    public void setPbkdf2Algorithm(String pbkdf2Algorithm) { this.pbkdf2Algorithm = pbkdf2Algorithm; }
    
    public int getMinPasswordLength() { return minPasswordLength; }
    public void setMinPasswordLength(int minPasswordLength) { this.minPasswordLength = minPasswordLength; }
    
    public int getMaxDataSizeBytes() { return maxDataSizeBytes; }
    public void setMaxDataSizeBytes(int maxDataSizeBytes) { this.maxDataSizeBytes = maxDataSizeBytes; }
    
    public boolean isValidateEncryptionFormat() { return validateEncryptionFormat; }
    public void setValidateEncryptionFormat(boolean validateEncryptionFormat) { this.validateEncryptionFormat = validateEncryptionFormat; }
    
    public boolean isEnableCompression() { return enableCompression; }
    public void setEnableCompression(boolean enableCompression) { this.enableCompression = enableCompression; }
    
    public boolean isSecureRandomEnabled() { return secureRandomEnabled; }
    public void setSecureRandomEnabled(boolean secureRandomEnabled) { this.secureRandomEnabled = secureRandomEnabled; }
    
    public boolean isMetadataEncryptionEnabled() { return metadataEncryptionEnabled; }
    public void setMetadataEncryptionEnabled(boolean metadataEncryptionEnabled) { this.metadataEncryptionEnabled = metadataEncryptionEnabled; }
    
    public boolean isCorruptionDetectionEnabled() { return corruptionDetectionEnabled; }
    public void setCorruptionDetectionEnabled(boolean corruptionDetectionEnabled) { this.corruptionDetectionEnabled = corruptionDetectionEnabled; }
    
    
    /**
     * Get the security level based on current configuration
     * @return SecurityLevel enum value
     */
    public SecurityLevel getSecurityLevel() {
        if (keyLength >= 256 && pbkdf2Iterations >= 150000 && metadataEncryptionEnabled) {
            return SecurityLevel.MAXIMUM;
        } else if (keyLength >= 256 && pbkdf2Iterations >= 100000) {
            return SecurityLevel.BALANCED;
        } else {
            return SecurityLevel.PERFORMANCE;
        }
    }
    
    /**
     * Security levels enum
     */
    public enum SecurityLevel {
        PERFORMANCE("Optimized for search speed"),
        BALANCED("Balanced security and performance"), 
        MAXIMUM("Maximum privacy protection");
        
        private final String description;
        
        SecurityLevel(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Validate the configuration settings
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (keyLength != 128 && keyLength != 192 && keyLength != 256) {
            throw new IllegalArgumentException("Key length must be 128, 192, or 256 bits");
        }
        
        if (ivLength < 8 || ivLength > 16) {
            throw new IllegalArgumentException("IV length must be between 8 and 16 bytes");
        }
        
        if (tagLength < 96 || tagLength > 128 || tagLength % 8 != 0) {
            throw new IllegalArgumentException("Tag length must be 96, 104, 112, 120, or 128 bits");
        }
        
        if (saltLength < 16) {
            throw new IllegalArgumentException("Salt length must be at least 16 bytes");
        }
        
        if (pbkdf2Iterations < 1000) {
            throw new IllegalArgumentException("PBKDF2 iterations must be at least 1000");
        }
        
        if (minPasswordLength < 4) {
            throw new IllegalArgumentException("Minimum password length must be at least 4 characters");
        }
        
        if (maxDataSizeBytes < 1024) {
            throw new IllegalArgumentException("Maximum data size must be at least 1KB");
        }
    }
    
    /**
     * Get a human-readable summary of the configuration
     * @return Configuration summary
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("🔐 Encryption Configuration Summary:\n");
        sb.append("   Algorithm: ").append(getEncryptionTransformation()).append("\n");
        sb.append("   Key Length: ").append(keyLength).append(" bits\n");
        sb.append("   PBKDF2 Iterations: ").append(pbkdf2Iterations).append("\n");
        sb.append("   Min Password Length: ").append(minPasswordLength).append(" chars\n");
        sb.append("   Max Data Size: ").append(maxDataSizeBytes / 1024).append(" KB\n");
        sb.append("   Metadata Encryption: ").append(metadataEncryptionEnabled ? "✅ Enabled" : "❌ Disabled").append("\n");
        sb.append("   Compression: ").append(enableCompression ? "✅ Enabled" : "❌ Disabled").append("\n");
        sb.append("   Corruption Detection: ").append(corruptionDetectionEnabled ? "✅ Enabled" : "❌ Disabled").append("\n");
        sb.append("   Format Validation: ").append(validateEncryptionFormat ? "✅ Enabled" : "❌ Disabled");
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
    
    /**
     * Builder class for creating custom encryption configurations
     */
    public static class Builder {
        private final EncryptionConfig config;
        
        public Builder() {
            this.config = new EncryptionConfig();
        }
        
        public Builder encryptionAlgorithm(String algorithm) {
            config.setEncryptionAlgorithm(algorithm);
            return this;
        }
        
        public Builder encryptionMode(String mode) {
            config.setEncryptionMode(mode);
            return this;
        }
        
        public Builder padding(String padding) {
            config.setPadding(padding);
            return this;
        }
        
        public Builder keyLength(int keyLength) {
            config.setKeyLength(keyLength);
            return this;
        }
        
        public Builder ivLength(int ivLength) {
            config.setIvLength(ivLength);
            return this;
        }
        
        public Builder tagLength(int tagLength) {
            config.setTagLength(tagLength);
            return this;
        }
        
        public Builder saltLength(int saltLength) {
            config.setSaltLength(saltLength);
            return this;
        }
        
        public Builder pbkdf2Iterations(int iterations) {
            config.setPbkdf2Iterations(iterations);
            return this;
        }
        
        public Builder pbkdf2Algorithm(String algorithm) {
            config.setPbkdf2Algorithm(algorithm);
            return this;
        }
        
        public Builder minPasswordLength(int length) {
            config.setMinPasswordLength(length);
            return this;
        }
        
        public Builder maxDataSizeBytes(int sizeBytes) {
            config.setMaxDataSizeBytes(sizeBytes);
            return this;
        }
        
        public Builder validateEncryptionFormat(boolean validate) {
            config.setValidateEncryptionFormat(validate);
            return this;
        }
        
        public Builder enableCompression(boolean enable) {
            config.setEnableCompression(enable);
            return this;
        }
        
        public Builder secureRandomEnabled(boolean enabled) {
            config.setSecureRandomEnabled(enabled);
            return this;
        }
        
        public Builder metadataEncryptionEnabled(boolean enabled) {
            config.setMetadataEncryptionEnabled(enabled);
            return this;
        }
        
        public Builder corruptionDetectionEnabled(boolean enabled) {
            config.setCorruptionDetectionEnabled(enabled);
            return this;
        }
        
        
        public EncryptionConfig build() {
            config.validate();
            return config;
        }
    }
}