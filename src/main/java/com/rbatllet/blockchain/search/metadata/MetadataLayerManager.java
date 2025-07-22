package com.rbatllet.blockchain.search.metadata;

import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.entity.OffChainData;
import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.CompressionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Collections;
import java.security.PrivateKey;
import com.rbatllet.blockchain.service.SecureBlockEncryptionService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Advanced Metadata Layer Manager for Encryption-First Search Architecture
 * 
 * Manages three-layer metadata system:
 * - PublicLayer: Always searchable, no password required
 * - PrivateLayer: Encrypted, requires password for access
 * 
 * This component enables search without compromising privacy, providing
 * granular control over what information is exposed at each layer.
 * 
 * @version 1.0 - Advanced Architecture
 */
public class MetadataLayerManager {
    
    private static final Logger logger = LoggerFactory.getLogger(MetadataLayerManager.class);
    
    private final ObjectMapper objectMapper;
    
    public MetadataLayerManager() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Generate metadata layers with essential parameters (uses fallback suggestions)
     */
    public BlockMetadataLayers generateMetadataLayers(Block block, 
                                                    EncryptionConfig config, 
                                                    String password, 
                                                    PrivateKey privateKey) {
        return generateMetadataLayers(block, config, password, privateKey, null, null, null);
    }
    
    /**
     * Generate metadata layers with user-defined search terms (without MIME type)
     */
    public BlockMetadataLayers generateMetadataLayers(Block block, 
                                                    EncryptionConfig config, 
                                                    String password, 
                                                    PrivateKey privateKey,
                                                    Set<String> publicSearchTerms,
                                                    Set<String> privateSearchTerms) {
        return generateMetadataLayers(block, config, password, privateKey, publicSearchTerms, privateSearchTerms, null);
    }
    
    /**
     * Generate metadata layers with granular term visibility control
     * @param block The block to generate metadata for
     * @param config Encryption configuration
     * @param password Password for encrypting private layer
     * @param privateKey Private key for signing
     * @param allSearchTerms All search terms to be distributed across layers
     * @param termVisibility Map controlling which terms go to which layer
     * @return Complete metadata layers with granular term distribution
     */
    public BlockMetadataLayers generateMetadataLayersWithGranularControl(Block block, 
                                                                        EncryptionConfig config, 
                                                                        String password, 
                                                                        PrivateKey privateKey,
                                                                        Set<String> allSearchTerms,
                                                                        TermVisibilityMap termVisibility) {
        
        // Distribute terms according to visibility map
        Set<String> publicTerms = termVisibility.getPublicTerms(allSearchTerms);
        Set<String> privateTerms = termVisibility.getPrivateTerms(allSearchTerms);
        
        logger.debug("🔍 Granular term distribution:");
        logger.debug("🔍   Public terms: {}", publicTerms);
        logger.debug("🔍   Private terms: {}", privateTerms);
        
        // Use existing method with distributed terms
        return generateMetadataLayers(block, config, password, privateKey, publicTerms, privateTerms, null);
    }
    
    /**
     * Generate complete three-layer metadata for a block with user-defined search terms
     * @param block The block to generate metadata for
     * @param config Encryption configuration determining layer distribution
     * @param password Password for encrypting private layer
     * @param privateKey Private key for signing
     * @param publicSearchTerms User-defined search terms visible without password
     * @param privateSearchTerms User-defined search terms that require password
     * @param userMimeType User-specified MIME type (optional, null for auto-detection)
     * @return Complete metadata layers
     */
    public BlockMetadataLayers generateMetadataLayers(Block block, 
                                                    EncryptionConfig config, 
                                                    String password, 
                                                    PrivateKey privateKey,
                                                    Set<String> publicSearchTerms,
                                                    Set<String> privateSearchTerms,
                                                    String userMimeType) {
        try {
            
            // Get content for keyword extraction - decrypt if encrypted and password available
            String contentForKeywords = block.getData();
            if (block.isDataEncrypted() && password != null && !password.trim().isEmpty()) {
                // For encrypted blocks, try to get real encrypted data from encryptionMetadata
                String encryptedData = block.getEncryptionMetadata();
                logger.debug("🔍 encryptionMetadata content: {}...", (encryptedData != null ? encryptedData.substring(0, Math.min(50, encryptedData.length())) : "null"));
                if (encryptedData != null && !encryptedData.trim().isEmpty()) {
                    // Check if it's an off-chain reference
                    if (encryptedData.startsWith("OFF_CHAIN_ENCRYPTED_REF:")) {
                        logger.debug("🔍 detected off-chain reference, cannot decrypt without off-chain access");
                        // For now, fall back to encrypted content
                    } else {
                        try {
                            logger.debug("🔍 attempting to decrypt block content using encryptionMetadata...");
                            
                            // Parse format: timestamp|salt|iv|encryptedData|dataHash (SecureBlockEncryptionService format)
                            String[] parts = encryptedData.split("\\|");
                            if (parts.length >= 5) {
                                String timestampStr = parts[0]; // Timestamp
                                String saltBase64 = parts[1];   // Salt (needed for key derivation)
                                String ivBase64 = parts[2];     // IV 
                                String encryptedContentBase64 = parts[3]; // Encrypted content + tag
                                String dataHashStr = parts[4];  // Data hash
                                
                                logger.debug("🔍 Debug: timestamp: {}", timestampStr);
                                logger.debug("🔍 Debug: salt: {}...", saltBase64.substring(0, Math.min(20, saltBase64.length())));
                                logger.debug("🔍 Debug: iv: {}...", ivBase64.substring(0, Math.min(20, ivBase64.length())));
                                logger.debug("🔍 Debug: encrypted content: {}...", encryptedContentBase64.substring(0, Math.min(20, encryptedContentBase64.length())));
                                logger.debug("🔍 Debug: dataHash: {}...", dataHashStr.substring(0, Math.min(20, dataHashStr.length())));
                                
                                // Use SecureBlockEncryptionService to decrypt properly
                                contentForKeywords = SecureBlockEncryptionService.decryptFromString(encryptedData, password);
                                logger.debug("🔍 Debug: decryption successful, content: {}...", contentForKeywords.substring(0, Math.min(50, contentForKeywords.length())));
                            } else {
                                logger.debug("🔍 Debug: invalid encryptionMetadata format, expected timestamp|salt|iv|encryptedData|dataHash (5 parts), got {} parts", parts.length);
                            }
                        } catch (Exception e) {
                            logger.debug("🔍 Debug: decryption failed, using encrypted content: {}", e.getMessage());
                            // Fall back to encrypted content
                        }
                    }
                } else {
                    logger.debug("🔍 Debug: no encryptionMetadata available, using placeholder data");
                }
            }
            
            // Use user-provided search terms or extract from block if none provided
            Set<String> keywordSet = new HashSet<>();
            if (publicSearchTerms != null) keywordSet.addAll(publicSearchTerms);
            if (privateSearchTerms != null) keywordSet.addAll(privateSearchTerms);
            
            // CRITICAL FIX: If no external keywords provided, extract from block's stored keywords
            if (keywordSet.isEmpty()) {
                // Try to extract keywords from autoKeywords (encrypted) and manualKeywords (public)
                logger.debug("🔍 no external keywords provided, extracting from block...");
                
                // Extract manual keywords (public)
                if (block.getManualKeywords() != null && !block.getManualKeywords().trim().isEmpty()) {
                    String[] manualKeywords = block.getManualKeywords().split("\\s+");
                    for (String keyword : manualKeywords) {
                        if (!keyword.trim().isEmpty()) {
                            keywordSet.add(keyword.trim());
                        }
                    }
                    logger.debug("🔍 extracted manual keywords: {}", keywordSet);
                }
                
                // Extract auto keywords (encrypted) if password available
                if (block.getAutoKeywords() != null && !block.getAutoKeywords().trim().isEmpty() && 
                    password != null && !password.trim().isEmpty()) {
                    try {
                        logger.debug("🔍 attempting to decrypt autoKeywords...");
                        
                        // autoKeywords contains multiple encrypted strings separated by spaces
                        // Each encrypted string has format: timestamp|salt|iv|encryptedData|dataHash
                        // We need to split by spaces and decrypt each part separately
                        String[] encryptedEntries = block.getAutoKeywords().split("\\s+");
                        logger.debug("🔍 found {} encrypted entries in autoKeywords", encryptedEntries.length);
                        
                        for (String encryptedEntry : encryptedEntries) {
                            if (encryptedEntry.trim().isEmpty()) continue;
                            
                            try {
                                String decryptedKeywords = SecureBlockEncryptionService.decryptFromString(
                                    encryptedEntry.trim(), password);
                                logger.debug("🔍 decrypted entry: {}", decryptedKeywords);
                                
                                if (decryptedKeywords != null && !decryptedKeywords.trim().isEmpty()) {
                                    String[] keywordArray = decryptedKeywords.split("\\s+");
                                    for (String keyword : keywordArray) {
                                        if (!keyword.trim().isEmpty()) {
                                            keywordSet.add(keyword.trim());
                                        }
                                    }
                                }
                            } catch (Exception entryException) {
                                logger.debug("🔍 failed to decrypt entry '{}': {}", 
                                           encryptedEntry.substring(0, Math.min(20, encryptedEntry.length())), 
                                           entryException.getMessage());
                            }
                        }
                        
                        logger.debug("🔍 extracted encrypted keywords: {}", keywordSet);
                    } catch (Exception e) {
                        logger.debug("🔍 failed to process autoKeywords: {}", e.getMessage());
                    }
                }
            }
            
            if (!keywordSet.isEmpty()) {
                logger.debug("🔍 using keywords: {}", keywordSet);
            } else {
                logger.debug("🔍 no keywords available, using minimal metadata");
            }
            
            // Analyze content characteristics using decrypted content if available
            ContentAnalysis analysis = analyzeContent(contentForKeywords, keywordSet);
            
            // Generate layers based on security configuration and user-defined terms
            PublicMetadata publicLayer = generatePublicLayer(block, analysis, config, publicSearchTerms, userMimeType);
            
            // Only generate private layer if password is provided
            PrivateMetadata privateLayer = null;
            String encryptedPrivateLayer = null;
            logger.debug("🔍 checking password condition: {}", (password != null && !password.trim().isEmpty()));
            if (password != null && !password.trim().isEmpty()) {
                logger.debug("🔍 Debug: generating private layer...");
                privateLayer = generatePrivateLayer(block, analysis, keywordSet, config, contentForKeywords, privateSearchTerms);
                logger.debug("🔍 Debug: privateLayer generated: {}", (privateLayer != null ? "yes" : "null"));
                if (privateLayer != null) {
                    logger.debug("🔍 Debug: privateLayer.isEmpty(): {}", privateLayer.isEmpty());
                }
                if (privateLayer != null && !privateLayer.isEmpty()) {
                    logger.debug("🔍 Debug: encrypting private metadata...");
                    encryptedPrivateLayer = encryptPrivateMetadata(privateLayer, password, privateKey);
                    logger.debug("🔍 Debug: encrypted private layer: {}", (encryptedPrivateLayer != null ? "success" : "failed"));
                }
            }
            
            
            
            return new BlockMetadataLayers(publicLayer, encryptedPrivateLayer);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate metadata layers: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate public metadata layer - always searchable
     * Only includes objective data and user-provided search terms
     */
    private PublicMetadata generatePublicLayer(Block block, ContentAnalysis analysis, EncryptionConfig config, Set<String> publicSearchTerms, String userMimeType) {
        PublicMetadata metadata = new PublicMetadata();
        
        // User-defined search terms (no filtering - user controls visibility)
        metadata.setGeneralKeywords(publicSearchTerms != null ? publicSearchTerms : new HashSet<>());
        
        // Time range based on security level
        metadata.setTimeRange(generateTimeRange(block.getTimestamp(), config));
        
        // MIME type: user-specified or auto-detected
        String mimeType = userMimeType != null ? userMimeType : detectMimeType(block.getData());
        metadata.setContentType(mimeType);
        
        // Generic block type based on data characteristics
        metadata.setBlockCategory(determineDataType(block));
        
        // Size range (not exact size for privacy)
        metadata.setSizeRange(determineSizeRange(block.getData().length()));
        
        // Integrity hash (for verification)
        metadata.setHashFingerprint(generateHashFingerprint(block.getHash()));
        
        
        return metadata;
    }
    
    /**
     * Generate private metadata layer - encrypted, requires password
     * Contains detailed information that needs protection
     */
    private PrivateMetadata generatePrivateLayer(Block block, ContentAnalysis analysis, 
                                               Set<String> allKeywords, EncryptionConfig config, String actualContent,
                                               Set<String> privateSearchTerms) {
        PrivateMetadata metadata = new PrivateMetadata();
        
        // Exact timestamp
        metadata.setExactTimestamp(block.getTimestamp());
        
        // User-defined private search terms take priority
        Set<String> combinedKeywords = new HashSet<>();
        if (privateSearchTerms != null && !privateSearchTerms.isEmpty()) {
            combinedKeywords.addAll(privateSearchTerms);
        } else {
            // Fallback to extracted keywords if no user terms provided
            combinedKeywords.addAll(allKeywords);
        }
        metadata.setSpecificKeywords(combinedKeywords);
        logger.debug("🔍 set specificKeywords: {}", combinedKeywords);
        
        // Additional sensitive keywords (personal names, amounts, IDs, etc.)
        Set<String> sensitiveTerms = extractSensitiveKeywords(allKeywords, analysis);
        metadata.setSensitiveTerms(sensitiveTerms);
        logger.debug("🔍 Debug: set sensitiveTerms: {}", sensitiveTerms);
        
        // Detailed category with subcategories
        metadata.setDetailedCategory(determineDetailedCategory(analysis));
        
        // Content statistics
        metadata.setContentStatistics(generateContentStatistics(analysis));
        
        // Extract detailed metadata using implemented methods
        metadata.setOwnerDetails(extractOwnerDetails(block, analysis));
        metadata.setTechnicalDetails(extractTechnicalDetails(block, analysis));
        metadata.setValidationInfo(extractValidationInfo(block, analysis));
        
        // Content summary for search (first 100 chars of actual decrypted content)
        if (analysis.getContentLength() > 0 && actualContent != null) {
            if (actualContent.length() > 100) {
                metadata.setContentSummary(actualContent.substring(0, 100) + "...");
            } else {
                metadata.setContentSummary(actualContent);
            }
        }
        
        return metadata;
    }
    
    
    /**
     * Analyze content to extract characteristics for metadata generation
     */
    private ContentAnalysis analyzeContent(String content, Set<String> keywords) {
        ContentAnalysis analysis = new ContentAnalysis();
        
        analysis.setAllKeywords(keywords);
        analysis.setContentLength(content.length());
        analysis.setWordCount(content.split("\\s+").length);
        
        // Detect sensitive information
        analysis.setSensitiveTerms(detectSensitiveTerms(keywords, content));
        
        // Detect numerical values and amounts
        analysis.setNumericalValues(extractNumericalValues(content));
        
        // Detect dates and times
        analysis.setDateReferences(extractDateReferences(content));
        
        // Detect email addresses and IDs
        analysis.setIdentifiers(extractIdentifiers(content));
        
        // Detect technical terms
        analysis.setTechnicalTerms(extractTechnicalTerms(keywords));
        
        // Content structure analysis
        analysis.setStructuralElements(analyzeStructure(content));
        
        return analysis;
    }
    
    
    /**
     * Generate time range based on security configuration
     */
    private String generateTimeRange(LocalDateTime timestamp, EncryptionConfig config) {
        if (timestamp == null) {
            return "unknown";
        }
        
        switch (config.getSecurityLevel()) {
            case MAXIMUM:
                return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM")); // Year-Month only
            case BALANCED:
                return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")); // Day precision
            case PERFORMANCE:
                return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH")); // Hour precision
            default:
                return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
    }
    
    /**
     * Detect MIME type based on objective data characteristics
     * Supports both text content and Base64-encoded binary files
     */
    private String detectMimeType(String data) {
        if (data == null || data.trim().isEmpty()) {
            return "application/octet-stream";
        }
        
        String trimmed = data.trim();
        
        // Detect Base64 encoded content (potential binary files)
        if (trimmed.matches("^[A-Za-z0-9+/=]+$") && trimmed.length() > 100) {
            return detectMimeTypeFromBase64(trimmed);
        }
        
        // Text-based content detection
        if (trimmed.startsWith("{") && (trimmed.endsWith("}") || trimmed.contains("\":\""))) {
            return "application/json";
        }
        if (trimmed.startsWith("<?xml") || (trimmed.startsWith("<") && trimmed.contains("</") && trimmed.endsWith(">"))) {
            return "text/xml";
        }
        if (trimmed.contains(",") && trimmed.contains("\n") && !trimmed.contains("{")) {
            return "text/csv";
        }
        if (trimmed.startsWith("data:")) {
            return "text/uri-list"; // Data URL
        }
        
        return "text/plain";
    }
    
    /**
     * Detect MIME type from Base64 encoded content using magic numbers
     */
    private String detectMimeTypeFromBase64(String base64Data) {
        try {
            // Decode enough bytes to check magic numbers
            String sample = base64Data.substring(0, Math.min(100, base64Data.length()));
            byte[] decoded = java.util.Base64.getDecoder().decode(sample);
            
            // PDF magic number: %PDF
            if (startsWith(decoded, new byte[]{0x25, 0x50, 0x44, 0x46})) {
                return "application/pdf";
            }
            // PNG magic number
            if (startsWith(decoded, new byte[]{(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})) {
                return "image/png";
            }
            // JPEG magic number
            if (startsWith(decoded, new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF})) {
                return "image/jpeg";
            }
            // GIF magic number
            if (startsWith(decoded, new byte[]{0x47, 0x49, 0x46, 0x38}) ||
                startsWith(decoded, new byte[]{0x47, 0x49, 0x46, 0x39})) {
                return "image/gif";
            }
            // ZIP/Office documents magic number
            if (startsWith(decoded, new byte[]{0x50, 0x4B, 0x03, 0x04})) {
                return "application/zip"; // Could be docx, xlsx, etc.
            }
            // MS Office old format
            if (startsWith(decoded, new byte[]{(byte)0xD0, (byte)0xCF, 0x11, (byte)0xE0})) {
                return "application/msword"; // Could be doc, xls, ppt
            }
            
            return "application/octet-stream";
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }
    
    /**
     * Check if byte array starts with specific magic number
     */
    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Determine objective data type based on block characteristics
     */
    private String determineDataType(Block block) {
        if (block == null) {
            return "UNKNOWN";
        }
        
        // Objective classification based on block properties
        if (block.isDataEncrypted()) {
            return "ENCRYPTED";
        }
        
        int dataSize = block.getData().length();
        if (dataSize == 0) {
            return "EMPTY";
        }
        if (dataSize < 100) {
            return "SMALL";
        }
        if (dataSize > 10000) {
            return "LARGE";
        }
        
        return "MEDIUM";
    }
    
    /**
     * Extract sensitive keywords for private layer
     */
    private Set<String> extractSensitiveKeywords(Set<String> allKeywords, ContentAnalysis analysis) {
        Set<String> sensitive = new HashSet<>();
        
        // Add all sensitive terms identified in analysis
        sensitive.addAll(analysis.getSensitiveTerms());
        
        // Add numerical values as keywords
        for (String numValue : analysis.getNumericalValues()) {
            sensitive.add(numValue);
        }
        
        // Add identifiers
        sensitive.addAll(analysis.getIdentifiers());
        
        return sensitive;
    }
    
    /**
     * Encrypt private metadata using AES-GCM with optional compression
     */
    private String encryptPrivateMetadata(PrivateMetadata metadata, String password, PrivateKey privateKey) {
        try {
            // Serialize metadata to JSON
            String jsonMetadata = serializePrivateMetadata(metadata);
            logger.debug("🔍 Debug: encryptPrivateMetadata - serialized JSON: {}...", jsonMetadata.substring(0, Math.min(150, jsonMetadata.length())));
            
            // Apply compression if enabled in configuration
            EncryptionConfig config = getCurrentEncryptionConfig();
            String processedMetadata = CompressionUtil.compressConditionally(
                jsonMetadata, config.isEnableCompression());
            
            // Log compression statistics for performance monitoring
            if (config.isEnableCompression()) {
                CompressionUtil.CompressionStats stats = CompressionUtil.getCompressionStats(jsonMetadata);
                logger.info("📦 Metadata compression: {}", stats.toString());
            }
            
            // Encrypt using CryptoUtil (reuse existing encryption infrastructure)
            return CryptoUtil.encryptWithGCM(processedMetadata, password);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt private metadata", e);
        }
    }
    
    /**
     * Decrypt private metadata with automatic decompression
     */
    public PrivateMetadata decryptPrivateMetadata(String encryptedMetadata, String password) {
        try {
            // Decrypt using CryptoUtil
            String processedMetadata = CryptoUtil.decryptWithGCM(encryptedMetadata, password);
            
            // Decompress if necessary
            String jsonMetadata;
            if (CompressionUtil.isCompressed(processedMetadata) || CompressionUtil.isUncompressed(processedMetadata)) {
                jsonMetadata = CompressionUtil.decompressString(processedMetadata);
            } else {
                // Legacy data without compression headers
                jsonMetadata = processedMetadata;
            }
            
            // Deserialize from JSON
            return deserializePrivateMetadata(jsonMetadata);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt private metadata", e);
        }
    }
    
    /**
     * Get current encryption configuration with default fallback
     */
    private EncryptionConfig getCurrentEncryptionConfig() {
        // Default configuration with compression enabled for performance
        return EncryptionConfig.createBalancedConfig();
    }
    
    // ===== UTILITY METHODS =====
    
    
    private Set<String> detectSensitiveTerms(Set<String> keywords, String content) {
        Set<String> sensitive = new HashSet<>();
        
        // Simple pattern-based detection
        for (String keyword : keywords) {
            // Numbers with 3+ digits, emails, technical codes
            if (keyword.matches(".*\\d{3,}.*") || 
                keyword.contains("@") || 
                keyword.matches("[A-Z0-9]{6,}")) {
                sensitive.add(keyword);
            }
        }
        
        return sensitive;
    }
    
    private Set<String> extractNumericalValues(String content) {
        Set<String> numbers = new HashSet<>();
        String[] words = content.split("\\s+");
        
        for (String word : words) {
            if (word.matches(".*\\d+.*")) {
                numbers.add(word);
            }
        }
        
        return numbers;
    }
    
    private Set<String> extractDateReferences(String content) {
        Set<String> dates = new HashSet<>();
        
        // Basic date pattern matching
        String[] patterns = {
            "\\d{4}-\\d{2}-\\d{2}",
            "\\d{2}/\\d{2}/\\d{4}",
            "\\d{1,2}/\\d{1,2}/\\d{4}"
        };
        
        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(content);
            while (m.find()) {
                dates.add(m.group());
            }
        }
        
        return dates;
    }
    
    private Set<String> extractIdentifiers(String content) {
        Set<String> ids = new HashSet<>();
        
        // Email pattern
        java.util.regex.Pattern emailPattern = java.util.regex.Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
        java.util.regex.Matcher emailMatcher = emailPattern.matcher(content);
        while (emailMatcher.find()) {
            ids.add(emailMatcher.group());
        }
        
        // ID patterns (e.g., TXN-123456, ACC-789)
        java.util.regex.Pattern idPattern = java.util.regex.Pattern.compile("\\b[A-Z]{2,}-\\d+\\b");
        java.util.regex.Matcher idMatcher = idPattern.matcher(content);
        while (idMatcher.find()) {
            ids.add(idMatcher.group());
        }
        
        return ids;
    }
    
    private Set<String> extractTechnicalTerms(Set<String> keywords) {
        Set<String> technical = new HashSet<>();
        
        String[] techPatterns = {
            "API", "HTTP", "JSON", "XML", "SQL", "UUID", "GUID", "SHA", "MD5"
        };
        
        for (String keyword : keywords) {
            for (String pattern : techPatterns) {
                if (keyword.toUpperCase().contains(pattern)) {
                    technical.add(keyword);
                }
            }
        }
        
        return technical;
    }
    
    // Additional helper methods would be implemented here...
    // For brevity, showing the core architecture and key methods
    
    private String determineSizeRange(int length) {
        if (length < 1000) return "small";
        if (length < 10000) return "medium";
        if (length < 100000) return "large";
        return "xlarge";
    }
    
    private String generateHashFingerprint(String hash) {
        return hash != null ? hash.substring(0, Math.min(8, hash.length())) : "unknown";
    }
    
    
    // Simplified methods for objective metadata only
    private String determineDetailedCategory(ContentAnalysis analysis) {
        // Objective classification based on content characteristics
        if (analysis.getNumericalValues().size() > 10) {
            return "DATA_INTENSIVE";
        }
        if (analysis.getIdentifiers().size() > 5) {
            return "STRUCTURED";
        }
        if (analysis.getContentLength() > 1000) {
            return "LARGE_CONTENT";
        }
        return "STANDARD";
    }
    
    /**
     * Extract owner details from block information
     * Thread-safe analysis of block ownership and signer details
     */
    private String extractOwnerDetails(Block block, ContentAnalysis analysis) {
        StringBuilder ownerInfo = new StringBuilder();
        
        // Get signer information if available
        if (block.getSignerPublicKey() != null && !block.getSignerPublicKey().trim().isEmpty()) {
            String signerHash = CryptoUtil.calculateHash(block.getSignerPublicKey());
            ownerInfo.append("signer_hash:").append(signerHash.substring(0, 16));
        }
        
        // Add block creation timestamp as ownership evidence
        if (block.getTimestamp() != null) {
            ownerInfo.append(",created:").append(block.getTimestamp().toString());
        }
        
        // Add content category as ownership context
        if (block.getContentCategory() != null && !block.getContentCategory().trim().isEmpty()) {
            ownerInfo.append(",category:").append(block.getContentCategory().toLowerCase());
        }
        
        // Add content size as ownership metadata
        ownerInfo.append(",content_size:").append(analysis.getContentLength());
        
        return ownerInfo.toString();
    }
    
    /**
     * Extract technical details from block and content analysis
     * Thread-safe generation of technical metadata for private layer
     */
    private Map<String, Object> extractTechnicalDetails(Block block, ContentAnalysis analysis) {
        Map<String, Object> techDetails = new LinkedHashMap<>();
        
        // Block technical info
        techDetails.put("block_number", block.getBlockNumber());
        techDetails.put("block_hash", block.getHash());
        techDetails.put("previous_hash", block.getPreviousHash());
        
        // Content analysis technical details
        techDetails.put("content_length", analysis.getContentLength());
        techDetails.put("word_count", analysis.getWordCount());
        techDetails.put("keyword_density", analysis.getAllKeywords().size() * 100.0 / Math.max(analysis.getWordCount(), 1));
        techDetails.put("numerical_values_count", analysis.getNumericalValues().size());
        
        // Encryption technical details
        if (block.getIsEncrypted()) {
            techDetails.put("encryption_status", "encrypted");
            techDetails.put("encryption_metadata_size", 
                block.getEncryptionMetadata() != null ? block.getEncryptionMetadata().length() : 0);
        } else {
            techDetails.put("encryption_status", "plaintext");
        }
        
        // Off-chain technical details
        if (block.hasOffChainData()) {
            OffChainData offChainData = block.getOffChainData();
            techDetails.put("offchain_file_path", offChainData.getFilePath());
            techDetails.put("offchain_file_size", offChainData.getFileSize());
            techDetails.put("offchain_content_type", offChainData.getContentType());
            techDetails.put("offchain_data_hash", offChainData.getDataHash());
        }
        
        // Timestamp technical details
        if (block.getTimestamp() != null) {
            techDetails.put("creation_timestamp", block.getTimestamp().toString());
            techDetails.put("timestamp_epoch", block.getTimestamp().toEpochSecond(java.time.ZoneOffset.UTC));
        }
        
        return Collections.unmodifiableMap(techDetails);
    }
    
    private Map<String, Object> generateContentStatistics(ContentAnalysis analysis) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("wordCount", analysis.getWordCount());
        stats.put("keywordCount", analysis.getAllKeywords().size());
        stats.put("numericalCount", analysis.getNumericalValues().size());
        return stats;
    }
    
    /**
     * Extract validation information from block
     * Thread-safe generation of cryptographic validation metadata
     */
    private Map<String, Object> extractValidationInfo(Block block, ContentAnalysis analysis) {
        Map<String, Object> validationInfo = new LinkedHashMap<>();
        
        // Cryptographic validation info
        if (block.getHash() != null) {
            validationInfo.put("block_hash_algorithm", "SHA3-256");
            validationInfo.put("block_hash_length", block.getHash().length());
            validationInfo.put("block_hash_prefix", block.getHash().substring(0, Math.min(8, block.getHash().length())));
        }
        
        // Signature validation info
        if (block.getSignature() != null && !block.getSignature().trim().isEmpty()) {
            validationInfo.put("signature_algorithm", "ECDSA");
            validationInfo.put("signature_length", block.getSignature().length());
            validationInfo.put("has_digital_signature", true);
        } else {
            validationInfo.put("has_digital_signature", false);
        }
        
        // Chain validation info
        validationInfo.put("has_previous_hash", block.getPreviousHash() != null);
        validationInfo.put("block_position", block.getBlockNumber());
        
        // Content validation info
        validationInfo.put("content_integrity_check", analysis.getContentLength() > 0);
        validationInfo.put("has_keywords", !analysis.getAllKeywords().isEmpty());
        validationInfo.put("content_category_assigned", block.getContentCategory() != null);
        
        // Encryption validation info
        if (block.getIsEncrypted()) {
            validationInfo.put("encryption_validation", "encrypted_content");
            validationInfo.put("encryption_metadata_present", block.getEncryptionMetadata() != null);
        }
        
        // Off-chain validation info
        if (block.hasOffChainData()) {
            validationInfo.put("offchain_data_validation", true);
            validationInfo.put("offchain_hash_present", block.getOffChainData().getDataHash() != null);
        }
        
        // Timestamp validation
        if (block.getTimestamp() != null) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            boolean timestampValid = block.getTimestamp().isBefore(now.plusMinutes(5)) && 
                                   block.getTimestamp().isAfter(now.minusYears(10));
            validationInfo.put("timestamp_validation", timestampValid ? "valid_range" : "suspicious_timestamp");
        }
        
        return Collections.unmodifiableMap(validationInfo);
    }
    
    
    
    
    
    private Map<String, Object> analyzeStructure(String content) {
        Map<String, Object> structure = new HashMap<>();
        
        if (content == null || content.trim().isEmpty()) {
            return structure;
        }
        
        // Detect JSON structure
        if (content.trim().startsWith("{") && content.trim().endsWith("}")) {
            structure.put("format", "json");
            structure.put("hasJsonStructure", true);
        }
        
        // Detect XML structure
        if (content.contains("<") && content.contains(">")) {
            structure.put("format", "xml_or_html");
            structure.put("hasMarkup", true);
        }
        
        // Detect CSV structure
        String[] lines = content.split("\n");
        if (lines.length > 1) {
            boolean couldBeCSV = true;
            int commaCount = -1;
            for (int i = 0; i < Math.min(3, lines.length); i++) {
                int currentCommas = lines[i].split(",").length - 1;
                if (commaCount == -1) {
                    commaCount = currentCommas;
                } else if (Math.abs(currentCommas - commaCount) > 1) {
                    couldBeCSV = false;
                    break;
                }
            }
            if (couldBeCSV && commaCount > 0) {
                structure.put("format", "csv");
                structure.put("columns", commaCount + 1);
            }
        }
        
        // Analyze line structure
        structure.put("lineCount", lines.length);
        if (lines.length > 10) {
            structure.put("isMultiLine", true);
        }
        
        // Detect URLs
        if (content.contains("http://") || content.contains("https://")) {
            structure.put("hasUrls", true);
        }
        
        // Detect code patterns
        if (content.contains("function") || content.contains("class") || content.contains("import")) {
            structure.put("couldBeCode", true);
        }
        
        return structure;
    }
    
    private String serializePrivateMetadata(PrivateMetadata metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("Private metadata cannot be null");
        }
        
        try {
            // Use a more robust approach with proper JSON structure
            Map<String, Object> jsonMap = new LinkedHashMap<>();
            
            // Add timestamp with proper format
            if (metadata.getExactTimestamp() != null) {
                jsonMap.put("exactTimestamp", metadata.getExactTimestamp().toString());
            }
            
            // Add specific keywords as array
            if (metadata.getSpecificKeywords() != null && !metadata.getSpecificKeywords().isEmpty()) {
                // Convert to sorted list for consistent serialization
                List<String> sortedKeywords = new ArrayList<>(metadata.getSpecificKeywords());
                Collections.sort(sortedKeywords);
                jsonMap.put("specificKeywords", sortedKeywords);
            }
            
            // Add detailed category
            if (metadata.getDetailedCategory() != null && !metadata.getDetailedCategory().trim().isEmpty()) {
                jsonMap.put("detailedCategory", metadata.getDetailedCategory().trim());
            }
            
            // Add owner details
            if (metadata.getOwnerDetails() != null && !metadata.getOwnerDetails().trim().isEmpty()) {
                jsonMap.put("ownerDetails", metadata.getOwnerDetails().trim());
            }
            
            // Add content statistics
            if (metadata.getContentStatistics() != null && !metadata.getContentStatistics().isEmpty()) {
                // Create a clean copy of statistics with proper validation
                Map<String, Object> cleanStats = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : metadata.getContentStatistics().entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        cleanStats.put(entry.getKey().trim(), sanitizeValue(entry.getValue()));
                    }
                }
                if (!cleanStats.isEmpty()) {
                    jsonMap.put("contentStatistics", cleanStats);
                }
            }
            
            // Add technical details
            if (metadata.getTechnicalDetails() != null && !metadata.getTechnicalDetails().isEmpty()) {
                Map<String, Object> cleanTech = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : metadata.getTechnicalDetails().entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        cleanTech.put(entry.getKey().trim(), sanitizeValue(entry.getValue()));
                    }
                }
                if (!cleanTech.isEmpty()) {
                    jsonMap.put("technicalDetails", cleanTech);
                }
            }
            
            // Add validation info
            if (metadata.getValidationInfo() != null && !metadata.getValidationInfo().isEmpty()) {
                Map<String, Object> cleanValidation = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : metadata.getValidationInfo().entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        cleanValidation.put(entry.getKey().trim(), sanitizeValue(entry.getValue()));
                    }
                }
                if (!cleanValidation.isEmpty()) {
                    jsonMap.put("validationInfo", cleanValidation);
                }
            }
            
            // Add metadata version for future compatibility
            jsonMap.put("metadataVersion", "1.0");
            
            // Convert to JSON using robust method
            return convertMapToJson(jsonMap);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize private metadata: " + e.getMessage(), e);
        }
    }
    
    private PrivateMetadata deserializePrivateMetadata(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty");
        }
        
        PrivateMetadata metadata = new PrivateMetadata();
        
        try {
            // Validate JSON structure
            if (!isValidJsonStructure(json)) {
                throw new IllegalArgumentException("Invalid JSON structure");
            }
            
            // Parse JSON into map
            Map<String, Object> jsonMap = parseJsonToMap(json);
            
            // Extract exactTimestamp with validation
            if (jsonMap.containsKey("exactTimestamp")) {
                try {
                    String timestampStr = (String) jsonMap.get("exactTimestamp");
                    if (timestampStr != null && !timestampStr.trim().isEmpty()) {
                        metadata.setExactTimestamp(LocalDateTime.parse(timestampStr));
                    }
                } catch (Exception e) {
                    logger.warn("⚠️ Failed to parse timestamp", e);
                }
            }
            
            // Extract detailed category
            if (jsonMap.containsKey("detailedCategory")) {
                String category = (String) jsonMap.get("detailedCategory");
                if (category != null && !category.trim().isEmpty()) {
                    metadata.setDetailedCategory(category.trim());
                }
            }
            
            // Extract owner details
            if (jsonMap.containsKey("ownerDetails")) {
                String owner = (String) jsonMap.get("ownerDetails");
                if (owner != null && !owner.trim().isEmpty()) {
                    metadata.setOwnerDetails(owner.trim());
                }
            }
            
            // Extract specific keywords
            if (jsonMap.containsKey("specificKeywords")) {
                Object keywordsObj = jsonMap.get("specificKeywords");
                if (keywordsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> keywordsList = (List<String>) keywordsObj;
                    Set<String> keywordSet = new HashSet<>();
                    for (String keyword : keywordsList) {
                        if (keyword != null && !keyword.trim().isEmpty()) {
                            keywordSet.add(keyword.trim());
                        }
                    }
                    metadata.setSpecificKeywords(keywordSet);
                }
            }
            
            // Extract content statistics
            if (jsonMap.containsKey("contentStatistics")) {
                Object statsObj = jsonMap.get("contentStatistics");
                if (statsObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> statsMap = (Map<String, Object>) statsObj;
                    Map<String, Object> cleanStats = new HashMap<>();
                    for (Map.Entry<String, Object> entry : statsMap.entrySet()) {
                        if (entry.getKey() != null && entry.getValue() != null) {
                            cleanStats.put(entry.getKey(), entry.getValue());
                        }
                    }
                    metadata.setContentStatistics(cleanStats);
                }
            }
            
            // Extract technical details
            if (jsonMap.containsKey("technicalDetails")) {
                Object techObj = jsonMap.get("technicalDetails");
                if (techObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> techMap = (Map<String, Object>) techObj;
                    Map<String, Object> cleanTech = new HashMap<>();
                    for (Map.Entry<String, Object> entry : techMap.entrySet()) {
                        if (entry.getKey() != null && entry.getValue() != null) {
                            cleanTech.put(entry.getKey(), entry.getValue());
                        }
                    }
                    metadata.setTechnicalDetails(cleanTech);
                }
            }
            
            // Extract validation info
            if (jsonMap.containsKey("validationInfo")) {
                Object validationObj = jsonMap.get("validationInfo");
                if (validationObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> validationMap = (Map<String, Object>) validationObj;
                    Map<String, Object> cleanValidation = new HashMap<>();
                    for (Map.Entry<String, Object> entry : validationMap.entrySet()) {
                        if (entry.getKey() != null && entry.getValue() != null) {
                            cleanValidation.put(entry.getKey(), entry.getValue());
                        }
                    }
                    metadata.setValidationInfo(cleanValidation);
                }
            }
            
            return metadata;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize private metadata: " + e.getMessage(), e);
        }
    }
    
    /**
     * Sanitize a value for safe JSON serialization
     */
    private Object sanitizeValue(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof String) {
            String strValue = (String) value;
            // Remove control characters and ensure reasonable length
            strValue = strValue.replaceAll("[\\x00-\\x1F\\x7F]", "");
            if (strValue.length() > 1000) {
                strValue = strValue.substring(0, 1000) + "...";
            }
            return strValue.trim();
        }
        
        if (value instanceof Number) {
            return value;
        }
        
        if (value instanceof Boolean) {
            return value;
        }
        
        // For other types, convert to safe string
        String stringValue = String.valueOf(value);
        stringValue = stringValue.replaceAll("[\\x00-\\x1F\\x7F]", "");
        if (stringValue.length() > 500) {
            stringValue = stringValue.substring(0, 500) + "...";
        }
        return stringValue.trim();
    }
    
    /**
     * Convert a map to JSON string using Jackson
     */
    private String convertMapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            logger.error("❌ Failed to serialize metadata to JSON", e);
            return "{}";
        }
    }
    
    
    /**
     * Validate JSON structure (basic validation)
     */
    private boolean isValidJsonStructure(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = json.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return false;
        }
        
        // Basic bracket matching
        int braceCount = 0;
        boolean inString = false;
        boolean escaped = false;
        
        for (char c : trimmed.toCharArray()) {
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                }
            }
        }
        
        return braceCount == 0 && !inString;
    }
    
    /**
     * Parse JSON string to Map (simple parser)
     */
    private Map<String, Object> parseJsonToMap(String json) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        if (json == null || json.trim().isEmpty()) {
            return result;
        }
        
        String content = json.trim();
        if (content.equals("{}")) {
            return result;
        }
        
        // Remove outer braces
        content = content.substring(1, content.length() - 1).trim();
        
        // Split by commas (not in strings)
        List<String> keyValuePairs = splitJsonElements(content);
        
        for (String pair : keyValuePairs) {
            int colonIndex = findJsonColonIndex(pair);
            if (colonIndex > 0) {
                String key = pair.substring(0, colonIndex).trim();
                String value = pair.substring(colonIndex + 1).trim();
                
                // Remove quotes from key
                if (key.startsWith("\"") && key.endsWith("\"")) {
                    key = key.substring(1, key.length() - 1);
                }
                
                // Parse value
                Object parsedValue = parseJsonValue(value);
                result.put(key, parsedValue);
            }
        }
        
        return result;
    }
    
    /**
     * Split JSON elements by commas (respecting string boundaries)
     */
    private List<String> splitJsonElements(String content) {
        List<String> elements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        
        boolean inString = false;
        boolean escaped = false;
        int braceLevel = 0;
        int bracketLevel = 0;
        
        for (char c : content.toCharArray()) {
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                current.append(c);
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                current.append(c);
                continue;
            }
            
            if (!inString) {
                if (c == '{') {
                    braceLevel++;
                } else if (c == '}') {
                    braceLevel--;
                } else if (c == '[') {
                    bracketLevel++;
                } else if (c == ']') {
                    bracketLevel--;
                } else if (c == ',' && braceLevel == 0 && bracketLevel == 0) {
                    elements.add(current.toString().trim());
                    current = new StringBuilder();
                    continue;
                }
            }
            
            current.append(c);
        }
        
        if (current.length() > 0) {
            elements.add(current.toString().trim());
        }
        
        return elements;
    }
    
    /**
     * Find the main colon index in a JSON key-value pair
     */
    private int findJsonColonIndex(String pair) {
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < pair.length(); i++) {
            char c = pair.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (!inString && c == ':') {
                return i;
            }
        }
        
        return -1;
    }
    
    /**
     * Parse a JSON value string to appropriate Java object
     */
    private Object parseJsonValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        value = value.trim();
        
        if (value.equals("null")) {
            return null;
        }
        
        if (value.equals("true")) {
            return true;
        }
        
        if (value.equals("false")) {
            return false;
        }
        
        if (value.startsWith("\"") && value.endsWith("\"")) {
            // String value
            String strValue = value.substring(1, value.length() - 1);
            return unescapeJsonString(strValue);
        }
        
        if (value.startsWith("[") && value.endsWith("]")) {
            // Array value
            String arrayContent = value.substring(1, value.length() - 1).trim();
            if (arrayContent.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<Object> list = new ArrayList<>();
            List<String> elements = splitJsonElements(arrayContent);
            for (String element : elements) {
                list.add(parseJsonValue(element));
            }
            return list;
        }
        
        if (value.startsWith("{") && value.endsWith("}")) {
            // Object value
            return parseJsonToMap(value);
        }
        
        // Try to parse as number
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                long longValue = Long.parseLong(value);
                if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                    return (int) longValue;
                }
                return longValue;
            }
        } catch (NumberFormatException e) {
            // Not a number, return as string
            return value;
        }
    }
    
    /**
     * Unescape JSON string
     */
    private String unescapeJsonString(String str) {
        if (str == null) {
            return "";
        }
        
        return str.replace("\\\"", "\"")
                 .replace("\\\\", "\\")
                 .replace("\\b", "\b")
                 .replace("\\f", "\f")
                 .replace("\\n", "\n")
                 .replace("\\r", "\r")
                 .replace("\\t", "\t");
    }
    
    // ===== GETTER METHODS =====
    
    // Removed unused cache getter methods - metadata is accessed through search results instead
    
}