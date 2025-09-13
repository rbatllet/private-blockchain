package com.rbatllet.blockchain.util.validation;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.validation.BlockValidationResult;
import com.rbatllet.blockchain.core.Blockchain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.io.File;
/**
 * Utility class for blockchain validation operations
 * Migrated from CLI project to core for better reusability
 * Enhanced with off-chain data validation support
 */
public class BlockValidationUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(BlockValidationUtil.class);
    
    /**
     * Validate genesis block specifically
     * @param genesisBlock The genesis block to validate
     * @return true if the genesis block is valid
     */
    public static boolean validateGenesisBlock(Block genesisBlock) {
        boolean isValid = true;
        final String GENESIS_PREVIOUS_HASH = "0";
        
        if (genesisBlock.getBlockNumber() == null || !genesisBlock.getBlockNumber().equals(0L)) {
            isValid = false;
        }
        
        if (!GENESIS_PREVIOUS_HASH.equals(genesisBlock.getPreviousHash())) {
            isValid = false;
        }
        
        return isValid;
    }
    
    /**
     * Helper method to check if a key was authorized at a specific timestamp
     * @param blockchain The blockchain instance
     * @param publicKeyString The public key string to check
     * @param timestamp The timestamp to check authorization at
     * @return true if the key was authorized at the given timestamp
     */
    public static boolean wasKeyAuthorizedAt(Blockchain blockchain, String publicKeyString, LocalDateTime timestamp) {
        try {
            return blockchain.wasKeyAuthorizedAt(publicKeyString, timestamp);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Validate off-chain data integrity for a block
     * @param blockchain The blockchain instance
     * @param block The block to validate off-chain data for
     * @return true if off-chain data is valid or not present
     */
    public static boolean validateOffChainData(Blockchain blockchain, Block block) {
        if (!block.hasOffChainData()) {
            return true; // No off-chain data to validate
        }
        
        try {
            // Check if off-chain file exists
            String filePath = block.getOffChainData().getFilePath();
            File offChainFile = new File(filePath);
            if (!offChainFile.exists()) {
                logger.error("❌ Off-chain file missing: {}", filePath);
                return false;
            }
            
            // Verify data integrity using blockchain's built-in method
            return blockchain.verifyOffChainIntegrity(block);
            
        } catch (Exception e) {
            logger.error("❌ Error validating off-chain data", e);
            return false;
        }
    }
    
    /**
     * Check if off-chain file exists and is accessible
     * @param block The block to check
     * @return true if file exists or block has no off-chain data
     */
    public static boolean offChainFileExists(Block block) {
        if (!block.hasOffChainData()) {
            return true; // No off-chain data means no file requirement
        }
        
        try {
            String filePath = block.getOffChainData().getFilePath();
            File offChainFile = new File(filePath);
            return offChainFile.exists() && offChainFile.canRead();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Validate the integrity of off-chain data without blockchain dependency
     * This method performs basic file existence and metadata validation
     * @param block The block to validate
     * @return true if basic off-chain validation passes
     */
    public static boolean validateOffChainMetadata(Block block) {
        if (!block.hasOffChainData()) {
            return true; // No off-chain data to validate
        }
        
        try {
            var offChainData = block.getOffChainData();
            
            // Check required fields
            if (offChainData.getDataHash() == null || offChainData.getDataHash().trim().isEmpty()) {
                logger.error("❌ Off-chain data hash is missing");
                return false;
            }
            
            if (offChainData.getSignature() == null || offChainData.getSignature().trim().isEmpty()) {
                logger.error("❌ Off-chain data signature is missing");
                return false;
            }
            
            if (offChainData.getFilePath() == null || offChainData.getFilePath().trim().isEmpty()) {
                logger.error("❌ Off-chain file path is missing");
                return false;
            }
            
            if (offChainData.getEncryptionIV() == null || offChainData.getEncryptionIV().trim().isEmpty()) {
                logger.error("❌ Off-chain encryption IV is missing");
                return false;
            }
            
            // Check file size consistency (account for AES encryption padding)
            File file = new File(offChainData.getFilePath());
            if (file.exists() && offChainData.getFileSize() != null) {
                long sizeDifference = Math.abs(file.length() - offChainData.getFileSize());
                if (sizeDifference > 16) { // Allow up to 16 bytes for AES padding
                    logger.error("❌ Off-chain file size mismatch: expected {}, actual {} (difference: {})", 
                                offChainData.getFileSize(), file.length(), sizeDifference);
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Error validating off-chain metadata", e);
            return false;
        }
    }
    
    /**
     * Perform comprehensive off-chain data validation
     * This method performs all possible validations without blockchain dependency
     * @param block The block to validate
     * @return detailed validation result
     */
    public static OffChainValidationResult validateOffChainDataDetailed(Block block) {
        if (!block.hasOffChainData()) {
            return new OffChainValidationResult(true, "No off-chain data to validate");
        }
        
        var offChainData = block.getOffChainData();
        StringBuilder issues = new StringBuilder();
        boolean isValid = true;
        
        try {
            // 1. Check metadata completeness
            if (offChainData.getDataHash() == null || offChainData.getDataHash().trim().isEmpty()) {
                issues.append("Missing data hash; ");
                isValid = false;
            }
            
            if (offChainData.getSignature() == null || offChainData.getSignature().trim().isEmpty()) {
                issues.append("Missing signature; ");
                isValid = false;
            }
            
            if (offChainData.getFilePath() == null || offChainData.getFilePath().trim().isEmpty()) {
                issues.append("Missing file path; ");
                isValid = false;
            }
            
            if (offChainData.getEncryptionIV() == null || offChainData.getEncryptionIV().trim().isEmpty()) {
                issues.append("Missing encryption IV; ");
                isValid = false;
            }
            
            // 2. Check file existence and accessibility
            if (offChainData.getFilePath() != null) {
                File file = new File(offChainData.getFilePath());
                if (!file.exists()) {
                    issues.append("File does not exist; ");
                    isValid = false;
                } else {
                    if (!file.canRead()) {
                        issues.append("File is not readable; ");
                        isValid = false;
                    }
                    
                    // 3. Check file size consistency (account for AES encryption padding)
                    if (offChainData.getFileSize() != null) {
                        long actualSize = file.length();
                        long expectedSize = offChainData.getFileSize();
                        // AES encryption adds up to 16 bytes of padding
                        long sizeDifference = Math.abs(actualSize - expectedSize);
                        if (sizeDifference > 16) {
                            issues.append(String.format("File size mismatch: expected %d, actual %d (diff: %d); ", 
                                expectedSize, actualSize, sizeDifference));
                            isValid = false;
                        }
                    }
                    
                    // 4. Check if file is suspiciously small or large
                    long fileSize = file.length();
                    if (fileSize == 0) {
                        issues.append("File is empty; ");
                        isValid = false;
                    } else if (fileSize > 100 * 1024 * 1024) { // 100MB
                        issues.append("File is suspiciously large (>100MB); ");
                        // This is a warning, not necessarily invalid
                    }
                }
            }
            
            // 5. Check timestamp consistency
            if (offChainData.getCreatedAt() != null && block.getTimestamp() != null) {
                // Off-chain data should be created around the same time as the block
                var timeDiff = java.time.Duration.between(block.getTimestamp(), offChainData.getCreatedAt()).abs();
                if (timeDiff.toMinutes() > 5) { // Allow 5 minutes difference
                    issues.append("Suspicious timestamp difference between block and off-chain data; ");
                    // This is a warning, not necessarily invalid
                }
            }
            
            String message = isValid ? "Off-chain data validation passed" : 
                            "Off-chain data validation failed: " + issues.toString();
            return new OffChainValidationResult(isValid, message);
            
        } catch (Exception e) {
            return new OffChainValidationResult(false, "Validation error: " + e.getMessage());
        }
    }
    
    /**
     * Check if off-chain data shows signs of tampering
     * @param block The block to check
     * @return true if no signs of tampering detected
     */
    public static boolean detectOffChainTampering(Block block) {
        if (!block.hasOffChainData()) {
            return true; // No off-chain data means no tampering possible
        }
        
        try {
            var offChainData = block.getOffChainData();
            File file = new File(offChainData.getFilePath());
            
            if (!file.exists()) {
                return false; // Missing file is a form of tampering
            }
            
            // Check file modification time vs creation time
            long fileModified = file.lastModified();
            long dataCreated = offChainData.getCreatedAt().atZone(java.time.ZoneId.systemDefault())
                                            .toInstant().toEpochMilli();
            
            // If file was modified significantly after off-chain data creation, it might be tampered
            long timeDiffMs = Math.abs(fileModified - dataCreated);
            if (timeDiffMs > 60000) { // 1 minute tolerance
                logger.warn("⚠️ Off-chain file modification time differs from creation time");
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Error detecting tampering", e);
            return false;
        }
    }
    
    /**
     * Result class for detailed off-chain validation
     */
    public static class OffChainValidationResult {
        private final boolean valid;
        private final String message;
        
        public OffChainValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        
        @Override
        public String toString() {
            return "OffChainValidationResult{valid=" + valid + ", message='" + message + "'}";
        }
    }
    
    /**
     * Truncate a hash string for display purposes
     * @param hash The hash string to truncate
     * @return The truncated hash string
     */
    public static String truncateHash(String hash) {
        if (hash == null) return "null";
        return hash.length() > 32 ? 
            hash.substring(0, 16) + "..." + hash.substring(hash.length() - 20, hash.length()) :
            hash;
    }

    /**
     * Perform a quick integrity check on a block
     * @param block The block to check
     * @param previousBlock The previous block in the chain
     * @return BlockValidationResult containing the validation results
     */
    public static BlockValidationResult performQuickIntegrityCheck(Block block, Block previousBlock) {
        try {
            if (block == null) {
                return BlockValidationResult.failure("Block is null");
            }
            
            // Quick hash validation
            if (block.getHash() == null || block.getHash().isEmpty()) {
                return BlockValidationResult.failure("Block hash is missing");
            }
            
            // Validate hash format
            if (!block.getHash().matches("^[a-fA-F0-9]{64}$")) {
                return BlockValidationResult.failure("Invalid hash format");
            }
            
            // Validate previous hash link
            if (previousBlock != null) {
                String expectedPreviousHash = previousBlock.getHash();
                if (!expectedPreviousHash.equals(block.getPreviousHash())) {
                    return BlockValidationResult.failure("Previous hash mismatch");
                }
            }
            
            // Validate timestamp
            if (block.getTimestamp() == null) {
                return BlockValidationResult.failure("Block timestamp is missing");
            }
            
            // Validate block number sequence
            if (previousBlock != null && block.getBlockNumber() != previousBlock.getBlockNumber() + 1) {
                return BlockValidationResult.failure("Block number sequence error");
            }
            
            return BlockValidationResult.success("Quick integrity check passed");
            
        } catch (Exception e) {
            return BlockValidationResult.failure("Integrity check error: " + e.getMessage());
        }
    }
}
