package com.rbatllet.blockchain.validation;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.service.SecureBlockEncryptionService;

/**
 * Specialized validator for encrypted blocks
 * Provides comprehensive validation of encrypted block integrity and format
 */
public class EncryptedBlockValidator {
    
    public static class EncryptedBlockValidationResult {
        private final boolean isValid;
        private final boolean encryptionIntact;
        private final boolean metadataValid;
        private final boolean formatCorrect;
        private final String errorMessage;
        private final String warningMessage;
        
        public EncryptedBlockValidationResult(boolean isValid, boolean encryptionIntact, 
                                            boolean metadataValid, boolean formatCorrect,
                                            String errorMessage, String warningMessage) {
            this.isValid = isValid;
            this.encryptionIntact = encryptionIntact;
            this.metadataValid = metadataValid;
            this.formatCorrect = formatCorrect;
            this.errorMessage = errorMessage;
            this.warningMessage = warningMessage;
        }
        
        public boolean isValid() { return isValid; }
        public boolean isEncryptionIntact() { return encryptionIntact; }
        public boolean isMetadataValid() { return metadataValid; }
        public boolean isFormatCorrect() { return formatCorrect; }
        public String getErrorMessage() { return errorMessage; }
        public String getWarningMessage() { return warningMessage; }
        
        public String getDetailedReport() {
            StringBuilder report = new StringBuilder();
            report.append("🔐 Encrypted Block Validation Report:\n");
            report.append(String.format("   Overall valid: %s\n", isValid ? "✅ YES" : "❌ NO"));
            report.append(String.format("   Encryption intact: %s\n", encryptionIntact ? "✅ YES" : "❌ NO"));
            report.append(String.format("   Metadata valid: %s\n", metadataValid ? "✅ YES" : "❌ NO"));
            report.append(String.format("   Format correct: %s\n", formatCorrect ? "✅ YES" : "❌ NO"));
            
            if (errorMessage != null) {
                report.append(String.format("   ❌ Error: %s\n", errorMessage));
            }
            if (warningMessage != null) {
                report.append(String.format("   ⚠️ Warning: %s\n", warningMessage));
            }
            
            return report.toString();
        }
    }
    
    /**
     * Validate an encrypted block comprehensively
     * 
     * @param block The encrypted block to validate
     * @return Detailed validation result
     */
    public static EncryptedBlockValidationResult validateEncryptedBlock(Block block) {
        if (!block.isDataEncrypted()) {
            return new EncryptedBlockValidationResult(false, false, false, false,
                "Block is not marked as encrypted", null);
        }
        
        boolean encryptionIntact = true;
        boolean metadataValid = true;
        boolean formatCorrect = true;
        String errorMessage = null;
        String warningMessage = null;
        
        // 1. Validate encryption metadata exists and is not empty
        if (block.getEncryptionMetadata() == null || block.getEncryptionMetadata().trim().isEmpty()) {
            encryptionIntact = false;
            metadataValid = false;
            errorMessage = "Encryption metadata is missing or empty";
        }
        
        // 2. Validate that data field shows "[ENCRYPTED]" placeholder
        if (!block.getData().equals("[ENCRYPTED]")) {
            formatCorrect = false;
            if (errorMessage == null) {
                errorMessage = "Data field should show '[ENCRYPTED]' for encrypted blocks";
            } else {
                errorMessage += "; Data field format incorrect";
            }
        }
        
        // 3. Validate encryption metadata format (should be valid Base64 or encrypted format)
        if (metadataValid && block.getEncryptionMetadata() != null) {
            if (!isValidEncryptionFormat(block.getEncryptionMetadata())) {
                encryptionIntact = false;
                if (errorMessage == null) {
                    errorMessage = "Encryption metadata appears corrupted or invalid format";
                } else {
                    errorMessage += "; Invalid encryption format";
                }
            }
        }
        
        // 4. Check if encryption looks like it could be decrypted (basic structure check)
        if (encryptionIntact && block.getEncryptionMetadata() != null) {
            try {
                // Don't actually decrypt, just check if it has the right structure
                if (!hasValidEncryptionStructure(block.getEncryptionMetadata())) {
                    warningMessage = "Encryption metadata may be corrupted - structure validation failed";
                }
            } catch (Exception e) {
                warningMessage = "Could not validate encryption structure: " + e.getMessage();
            }
        }
        
        // 5. Validate category for encrypted blocks (should be present)
        if (block.getContentCategory() == null || block.getContentCategory().trim().isEmpty()) {
            if (warningMessage == null) {
                warningMessage = "Encrypted blocks should have a content category for searchability";
            } else {
                warningMessage += "; Missing content category";
            }
        }
        
        // 6. Check timestamp consistency
        if (block.getTimestamp() == null) {
            metadataValid = false;
            if (errorMessage == null) {
                errorMessage = "Block timestamp is missing";
            } else {
                errorMessage += "; Missing timestamp";
            }
        }
        
        boolean isValid = encryptionIntact && metadataValid && formatCorrect;
        
        return new EncryptedBlockValidationResult(isValid, encryptionIntact, metadataValid, 
                                                formatCorrect, errorMessage, warningMessage);
    }
    
    /**
     * Validate that encrypted block can be decrypted with given password
     * 
     * @param block The encrypted block
     * @param password The decryption password
     * @return true if block can be successfully decrypted
     */
    public static boolean canDecryptWithPassword(Block block, String password) {
        if (!block.isDataEncrypted() || block.getEncryptionMetadata() == null) {
            return false;
        }
        
        try {
            // First, use advanced integrity verification without decryption
            var encryptedData = SecureBlockEncryptionService.SecureEncryptedData.deserialize(
                block.getEncryptionMetadata());
            boolean integrityOk = SecureBlockEncryptionService.verifyIntegrity(encryptedData, password);
            
            if (!integrityOk) {
                return false; // Failed integrity check
            }
            
            // If integrity check passes, verify actual decryption
            String decrypted = SecureBlockEncryptionService.decryptFromString(
                block.getEncryptionMetadata(), password);
            return decrypted != null && !decrypted.trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Detect potential corruption in encrypted block
     * 
     * @param block The encrypted block to check
     * @return Corruption assessment
     */
    public static CorruptionAssessment detectCorruption(Block block) {
        if (!block.isDataEncrypted()) {
            return new CorruptionAssessment(false, "Block is not encrypted");
        }
        
        EncryptedBlockValidationResult validation = validateEncryptedBlock(block);
        
        boolean possiblyCorrupted = false;
        StringBuilder issues = new StringBuilder();
        
        if (!validation.isEncryptionIntact()) {
            possiblyCorrupted = true;
            issues.append("Encryption integrity compromised; ");
        }
        
        if (!validation.isMetadataValid()) {
            possiblyCorrupted = true;
            issues.append("Metadata validation failed; ");
        }
        
        if (!validation.isFormatCorrect()) {
            possiblyCorrupted = true;
            issues.append("Format validation failed; ");
        }
        
        // Check for specific corruption patterns
        if (block.getEncryptionMetadata() != null) {
            String metadata = block.getEncryptionMetadata();
            
            // Check for truncated data
            if (metadata.length() < 50) { // Very short for encrypted data
                possiblyCorrupted = true;
                issues.append("Suspiciously short encryption data; ");
            }
            
            // Check for obvious corruption patterns
            if (metadata.contains("null") || metadata.contains("undefined") || metadata.startsWith("error")) {
                possiblyCorrupted = true;
                issues.append("Corruption markers detected; ");
            }
        }
        
        return new CorruptionAssessment(possiblyCorrupted, issues.toString());
    }
    
    // Helper methods
    
    private static boolean isValidEncryptionFormat(String encryptionMetadata) {
        if (encryptionMetadata == null || encryptionMetadata.trim().isEmpty()) {
            return false;
        }
        
        // Check basic format - should be Base64-like or structured encryption format
        // This is a basic check - actual format depends on SecureBlockEncryptionService
        try {
            // Must contain at least some structure indicators
            return encryptionMetadata.length() > 20 && 
                   !encryptionMetadata.contains(" ") && // No spaces in encrypted data
                   !encryptionMetadata.startsWith("[") && // Not a placeholder
                   !encryptionMetadata.toLowerCase().contains("error");
        } catch (Exception e) {
            return false;
        }
    }
    
    private static boolean hasValidEncryptionStructure(String encryptionMetadata) {
        try {
            // Try to validate the structure without decrypting
            // This checks if it looks like valid encrypted data from our service
            return SecureBlockEncryptionService.isValidEncryptedFormat(encryptionMetadata);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Assessment of potential corruption in an encrypted block
     */
    public static class CorruptionAssessment {
        private final boolean possiblyCorrupted;
        private final String issues;
        
        public CorruptionAssessment(boolean possiblyCorrupted, String issues) {
            this.possiblyCorrupted = possiblyCorrupted;
            this.issues = issues;
        }
        
        public boolean isPossiblyCorrupted() { return possiblyCorrupted; }
        public String getIssues() { return issues; }
        
        @Override
        public String toString() {
            return String.format("Corruption Assessment: %s%s", 
                possiblyCorrupted ? "POSSIBLE CORRUPTION" : "APPEARS INTACT",
                issues.isEmpty() ? "" : " - Issues: " + issues);
        }
    }
}