package com.rbatllet.blockchain.service;

import com.rbatllet.blockchain.core.Blockchain;
import com.rbatllet.blockchain.entity.Block;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.security.PasswordUtil;
import com.rbatllet.blockchain.security.SecureKeyStorage;
import com.rbatllet.blockchain.security.KeyFileLoader;
import com.rbatllet.blockchain.security.ECKeyDerivation;
import com.rbatllet.blockchain.recovery.ChainRecoveryManager;
import com.rbatllet.blockchain.util.format.FormatUtil;
import com.rbatllet.blockchain.util.validation.BlockValidationUtil;
import com.rbatllet.blockchain.entity.OffChainData;
import com.rbatllet.blockchain.search.RevolutionarySearchEngine.EnhancedSearchResult;
import com.rbatllet.blockchain.search.metadata.TermVisibilityMap;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.util.Set;
import java.util.HashSet;

/**
 * User-friendly API for encrypted blockchain operations
 * Simplifies common tasks and provides intuitive method names
 */
public class UserFriendlyEncryptionAPI {
    
    private final Blockchain blockchain;
    private final ECKeyDerivation keyDerivation;
    private final ChainRecoveryManager recoveryManager;
    private final OffChainStorageService offChainStorage;
    private KeyPair defaultKeyPair;
    private String defaultUsername;
    
    /**
     * Initialize the API with a blockchain instance
     * @param blockchain The blockchain to operate on
     */
    public UserFriendlyEncryptionAPI(Blockchain blockchain) {
        this.blockchain = blockchain;
        this.keyDerivation = new ECKeyDerivation();
        this.recoveryManager = new ChainRecoveryManager(blockchain);
        this.offChainStorage = new OffChainStorageService();
    }
    
    /**
     * Initialize the API with default user credentials
     * @param blockchain The blockchain to operate on
     * @param defaultUsername Default username for operations
     * @param defaultKeyPair Default key pair for signing
     */
    public UserFriendlyEncryptionAPI(Blockchain blockchain, String defaultUsername, KeyPair defaultKeyPair) {
        this.blockchain = blockchain;
        this.keyDerivation = new ECKeyDerivation();
        this.recoveryManager = new ChainRecoveryManager(blockchain);
        this.offChainStorage = new OffChainStorageService();
        this.defaultUsername = defaultUsername;
        this.defaultKeyPair = defaultKeyPair;
        
        // Auto-register the user if not already registered
        try {
            String publicKeyString = CryptoUtil.publicKeyToString(defaultKeyPair.getPublic());
            blockchain.addAuthorizedKey(publicKeyString, defaultUsername);
        } catch (Exception e) {
            // Key might already exist, which is fine
        }
    }
    
    // ===== SIMPLE ENCRYPTED BLOCK OPERATIONS =====
    
    /**
     * Store sensitive data securely in the blockchain
     * @param secretData The sensitive data to encrypt and store
     * @param password The password for encryption
     * @return The created block, or null if failed
     */
    public Block storeSecret(String secretData, String password) {
        validateKeyPair();
        return blockchain.addEncryptedBlock(secretData, password, defaultKeyPair.getPrivate(), defaultKeyPair.getPublic());
    }
    
    
    
    
    /**
     * Store data with identifier-based search terms
     * Generic method that replaces category-specific storage
     * @param data The data to encrypt and store
     * @param password The password for encryption
     * @param identifier Optional identifier for searchability (e.g., "patient:123", "account:456", "case:789")
     * @return The created block, or null if failed
     */
    public Block storeDataWithIdentifier(String data, String password, String identifier) {
        validateKeyPair();
        String[] keywords = identifier != null ? new String[]{identifier} : null;
        return blockchain.addEncryptedBlockWithKeywords(data, password, keywords, null, 
                                                       defaultKeyPair.getPrivate(), defaultKeyPair.getPublic());
    }
    
    // ===== SIMPLE RETRIEVAL OPERATIONS =====
    
    /**
     * Retrieve and decrypt a secret by block ID
     * @param blockId The ID of the encrypted block
     * @param password The password for decryption
     * @return The decrypted data, or null if failed
     */
    public String retrieveSecret(Long blockId, String password) {
        return blockchain.getDecryptedBlockData(blockId, password);
    }
    
    /**
     * Check if a block contains encrypted data
     * @param blockId The ID of the block to check
     * @return true if the block is encrypted, false otherwise
     */
    public boolean isBlockEncrypted(Long blockId) {
        return blockchain.isBlockEncrypted(blockId);
    }
    
    // ===== SIMPLE SEARCH OPERATIONS =====
    
    /**
     * Search for encrypted data without revealing content using Revolutionary Search
     * @param searchTerm The term to search for in metadata
     * @return List of matching blocks (content remains encrypted)
     */
    public List<Block> findEncryptedData(String searchTerm) {
        // Use Revolutionary Search public metadata search (no password required)
        var enhancedResults = blockchain.getUnifiedSearchAPI().searchSimple(searchTerm, 50);
        List<Block> blocks = new ArrayList<>();
        for (var enhancedResult : enhancedResults) {
            try {
                Block block = blockchain.getBlockByHash(enhancedResult.getBlockHash());
                if (block != null && block.isDataEncrypted()) {
                    blocks.add(block);
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not retrieve block " + enhancedResult.getBlockHash() + ": " + e.getMessage());
            }
        }
        return blocks;
    }
    
    /**
     * Search for and decrypt matching encrypted data using Revolutionary Search
     * @param searchTerm The term to search for
     * @param password The password for decryption
     * @return List of matching blocks with decrypted content
     */
    public List<Block> findAndDecryptData(String searchTerm, String password) {
        System.out.println("🔍 Debug: findAndDecryptData called with searchTerm='" + searchTerm + "', password length=" + (password != null ? password.length() : "null"));
        // Use Revolutionary Search Engine for elegant, robust search
        return searchWithAdaptiveDecryption(searchTerm, password, 50);
    }
    
    /**
     * Search for records by specific identifier using Revolutionary Search
     * @param identifier The identifier to search for
     * @return List of matching blocks (content encrypted)
     */
    public List<Block> findRecordsByIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return searchByTerms(new String[]{identifier}, null, 50);
    }
    
    // ===== UNIFIED SEARCH OPERATIONS =====
    
    /**
     * Revolutionary Search: Search everything in the blockchain using proper architecture
     * @param searchTerm The term to search for
     * @return List of matching blocks
     */
    public List<Block> searchEverything(String searchTerm) {
        // Use Revolutionary Search public search (no password, metadata only)
        var enhancedResults = blockchain.getUnifiedSearchAPI().searchSimple(searchTerm, 50);
        List<Block> blocks = new ArrayList<>();
        for (var enhancedResult : enhancedResults) {
            try {
                Block block = blockchain.getBlockByHash(enhancedResult.getBlockHash());
                if (block != null) {
                    blocks.add(block);
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not retrieve block " + enhancedResult.getBlockHash() + ": " + e.getMessage());
            }
        }
        return blocks;
    }
    
    /**
     * Revolutionary Search: Search including encrypted content using proper architecture
     * @param searchTerm The term to search for
     * @param password The password for decrypting encrypted content
     * @return List of matching blocks including decrypted content
     */
    public List<Block> searchEverythingWithPassword(String searchTerm, String password) {
        // Use Revolutionary Search adaptive secure search
        return searchWithAdaptiveDecryption(searchTerm, password, 50);
    }
    
    // ===== ENHANCED SEARCH WITH KEYWORD EXTRACTION =====
    
    /**
     * Extract simple keywords from text content
     * @param content The text content to analyze
     * @return Space-separated keywords extracted from the content
     */
    public String extractKeywords(String content) {
        return extractSimpleKeywords(content);
    }
    
    /**
     * Simple keyword extraction without hardcoded suggestions
     */
    private String extractSimpleKeywords(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        
        String[] words = content.toLowerCase()
            .replaceAll("[^a-zA-Z0-9\\s]", " ")
            .split("\\s+");
        
        Set<String> keywords = new HashSet<>();
        for (String word : words) {
            if (word.length() > 2) {
                keywords.add(word);
            }
        }
        
        return String.join(" ", keywords);
    }
    
    /**
     * Simple search term extraction without hardcoded suggestions
     */
    private Set<String> extractSimpleSearchTerms(String content, int maxTerms) {
        if (content == null || content.trim().isEmpty()) {
            return new HashSet<>();
        }
        
        String[] words = content.toLowerCase()
            .replaceAll("[^a-zA-Z0-9\\s]", " ")
            .split("\\s+");
        
        Set<String> terms = new HashSet<>();
        for (String word : words) {
            if (word.length() > 2 && terms.size() < maxTerms) {
                terms.add(word);
            }
        }
        
        return terms;
    }
    
    /**
     * Smart search using keyword extraction and Revolutionary Search
     * Automatically extracts keywords from search query and searches for related terms
     * @param query Natural language search query
     * @return List of matching blocks found using intelligent keyword matching
     */
    public List<Block> smartSearch(String query) {
        // Extract keywords from the query
        String extractedKeywords = extractSimpleKeywords(query);
        
        // If no keywords extracted, fall back to original query
        String searchTerms = extractedKeywords.isEmpty() ? query : extractedKeywords;
        
        // Search using Revolutionary Search with extracted keywords
        var enhancedResults = blockchain.getUnifiedSearchAPI().searchSimple(searchTerms, 50);
        return convertEnhancedResultsToBlocks(enhancedResults);
    }
    
    /**
     * Smart search with password using Revolutionary Search architecture
     * Uses keyword extraction for intelligent matching across encrypted and unencrypted data
     * @param query Natural language search query
     * @param password Password for decrypting encrypted content
     * @return List of matching blocks with decrypted content where applicable
     */
    public List<Block> smartSearchWithPassword(String query, String password) {
        // Extract keywords from the query
        String extractedKeywords = extractSimpleKeywords(query);
        
        // If no keywords extracted, fall back to original query
        String searchTerms = extractedKeywords.isEmpty() ? query : extractedKeywords;
        
        // Search using Revolutionary Search with password and extracted keywords
        return searchWithAdaptiveDecryption(searchTerms, password, 50);
    }
    
    /**
     * Revolutionary Search: Advanced search with keyword extraction using proper architecture
     * @param query Natural language search query
     * @param password Optional password for decrypting encrypted content (can be null)
     * @return Comprehensive search results with keyword-enhanced matching
     */
    public List<Block> smartUnifiedSearch(String query, String password) {
        // Extract keywords from the query
        String extractedKeywords = extractSimpleKeywords(query);
        
        // If no keywords extracted, fall back to original query
        String searchTerms = extractedKeywords.isEmpty() ? query : extractedKeywords;
        
        // Perform Revolutionary Search with extracted keywords using proper architecture
        if (password != null && !password.trim().isEmpty()) {
            return searchWithAdaptiveDecryption(searchTerms, password, 50);
        } else {
            var enhancedResults = blockchain.getUnifiedSearchAPI().searchSimple(searchTerms, 50);
            return convertEnhancedResultsToBlocks(enhancedResults);
        }
    }
    
    /**
     * Find similar content based on keyword analysis
     * Extracts keywords from reference content and searches for blocks with similar keywords
     * @param referenceContent The content to find similar matches for
     * @param minimumSimilarity Minimum percentage of keyword overlap (0.0 to 1.0)
     * @return List of blocks with similar keyword profiles
     */
    public List<Block> findSimilarContent(String referenceContent, double minimumSimilarity) {
        if (minimumSimilarity < 0.0 || minimumSimilarity > 1.0) {
            throw new IllegalArgumentException("Minimum similarity must be between 0.0 and 1.0");
        }
        
        // Extract keywords from reference content
        String referenceKeywords = extractSimpleKeywords(referenceContent);
        Set<String> referenceKeywordSet = new HashSet<>();
        for (String keyword : referenceKeywords.split("\\s+")) {
            if (!keyword.trim().isEmpty()) {
                referenceKeywordSet.add(keyword.trim().toLowerCase());
            }
        }
        
        if (referenceKeywordSet.isEmpty()) {
            return List.of(); // No keywords to match against
        }
        
        // Search for blocks and filter by similarity
        List<Block> allBlocks = blockchain.getAllBlocks();
        List<Block> similarBlocks = new java.util.ArrayList<>();
        
        for (Block block : allBlocks) {
            String blockContent = block.getData();
            if (blockContent != null && !blockContent.trim().isEmpty()) {
                String blockKeywords = extractSimpleKeywords(blockContent);
                Set<String> blockKeywordSet = new HashSet<>();
                for (String keyword : blockKeywords.split("\\s+")) {
                    if (!keyword.trim().isEmpty()) {
                        blockKeywordSet.add(keyword.trim().toLowerCase());
                    }
                }
                
                // Calculate similarity (Jaccard index)
                Set<String> intersection = new HashSet<>(referenceKeywordSet);
                intersection.retainAll(blockKeywordSet);
                
                Set<String> union = new HashSet<>(referenceKeywordSet);
                union.addAll(blockKeywordSet);
                
                double similarity = union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
                
                if (similarity >= minimumSimilarity) {
                    similarBlocks.add(block);
                }
            }
        }
        
        return similarBlocks;
    }
    
    /**
     * Analyze content and provide keyword-based insights
     * Returns detailed analysis of extracted keywords and their categories
     * @param content The content to analyze
     * @return Human-readable analysis of the content's keywords and characteristics
     */
    public String analyzeContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "No content provided for analysis.";
        }
        
        String keywords = extractSimpleKeywords(content);
        
        if (keywords.isEmpty()) {
            return "No significant keywords found in the content.";
        }
        
        StringBuilder analysis = new StringBuilder();
        analysis.append("📊 CONTENT ANALYSIS REPORT\n");
        analysis.append("═".repeat(40)).append("\n\n");
        
        String[] keywordArray = keywords.split("\\s+");
        analysis.append("🔍 Extracted Keywords: ").append(keywordArray.length).append(" terms\n");
        analysis.append("📝 Keywords: ").append(keywords).append("\n\n");
        
        // Categorize keywords
        int dates = 0, emails = 0, codes = 0, numbers = 0, entities = 0, others = 0;
        
        for (String keyword : keywordArray) {
            if (keyword.matches("\\d{4}-\\d{2}-\\d{2}|\\d{2}/\\d{2}/\\d{4}|\\d{1,2}/\\d{1,2}/\\d{4}")) {
                dates++;
            } else if (keyword.matches(".*@.*\\..*")) {
                emails++;
            } else if (keyword.matches("[A-Z]+[-_]\\d+|\\b[A-Z]{2,}\\d*\\b")) {
                codes++;
            } else if (keyword.matches("\\d+(\\.\\d+)?")) {
                numbers++;
            } else if (Character.isUpperCase(keyword.charAt(0))) {
                entities++;
            } else {
                others++;
            }
        }
        
        analysis.append("📈 Keyword Categories:\n");
        if (dates > 0) analysis.append("   📅 Dates: ").append(dates).append("\n");
        if (emails > 0) analysis.append("   📧 Emails: ").append(emails).append("\n");
        if (codes > 0) analysis.append("   🔖 Codes/IDs: ").append(codes).append("\n");
        if (numbers > 0) analysis.append("   🔢 Numbers: ").append(numbers).append("\n");
        if (entities > 0) analysis.append("   🏷️ Entities: ").append(entities).append("\n");
        if (others > 0) analysis.append("   📝 Other Terms: ").append(others).append("\n");
        
        analysis.append("\n💡 Search Recommendations:\n");
        analysis.append("   • Use 'smartSearch()' for intelligent keyword-based searches\n");
        analysis.append("   • Use 'findSimilarContent()' to find related documents\n");
        analysis.append("   • Extract keywords with 'extractKeywords()' for custom searches");
        
        return analysis.toString();
    }
    
    // ===== BLOCKCHAIN INFORMATION =====
    
    /**
     * Get a summary of the blockchain's search capabilities
     * @return Human-readable summary of searchable vs encrypted content
     */
    public String getBlockchainSummary() {
        return blockchain.getSearchStatistics();
    }
    
    /**
     * Check if the blockchain contains any encrypted data
     * @return true if there are encrypted blocks, false otherwise
     */
    public boolean hasEncryptedData() {
        String stats = blockchain.getSearchStatistics();
        return stats.contains("Encrypted blocks: ") && !stats.contains("Encrypted blocks: 0");
    }
    
    /**
     * Get the total number of encrypted blocks
     * @return Number of encrypted blocks
     */
    public long getEncryptedBlockCount() {
        List<Block> allBlocks = blockchain.getAllBlocks();
        return allBlocks.stream().mapToLong(block -> block.isDataEncrypted() ? 1 : 0).sum();
    }
    
    /**
     * Get the total number of unencrypted blocks
     * @return Number of unencrypted blocks
     */
    public long getUnencryptedBlockCount() {
        List<Block> allBlocks = blockchain.getAllBlocks();
        return allBlocks.stream().mapToLong(block -> !block.isDataEncrypted() ? 1 : 0).sum();
    }
    
    // ===== VALIDATION AND UTILITIES =====
    
    /**
     * Validate that the blockchain's encrypted blocks are intact
     * @return true if all encrypted blocks are valid, false if any issues found
     */
    public boolean validateEncryptedBlocks() {
        var result = blockchain.validateChainDetailed();
        return result.isStructurallyIntact() && result.isFullyCompliant();
    }
    
    /**
     * Get a detailed validation report for the blockchain
     * @return Human-readable validation report
     */
    public String getValidationReport() {
        return blockchain.getValidationReport();
    }
    
    // ===== SETUP AND CONFIGURATION =====
    
    /**
     * Create a new user with generated keys
     * @param username The username for the new user
     * @return The generated key pair for the user
     */
    public KeyPair createUser(String username) {
        try {
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
            blockchain.addAuthorizedKey(publicKeyString, username);
            return keyPair;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        }
    }
    
    /**
     * Set default credentials for future operations
     * @param username The default username
     * @param keyPair The default key pair
     */
    public void setDefaultCredentials(String username, KeyPair keyPair) {
        this.defaultUsername = username;
        this.defaultKeyPair = keyPair;
        
        // Auto-register the user if not already registered
        try {
            String publicKeyString = CryptoUtil.publicKeyToString(keyPair.getPublic());
            blockchain.addAuthorizedKey(publicKeyString, username);
        } catch (Exception e) {
            // Key might already exist, which is fine
        }
    }
    
    /**
     * Generate a secure random password for encryption
     * @param length The desired password length (minimum 12 characters)
     * @return A cryptographically secure random password
     */
    public String generateSecurePassword(int length) {
        if (length < 12) {
            throw new IllegalArgumentException("Password must be at least 12 characters long");
        }
        
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        java.security.SecureRandom random = new java.security.SecureRandom();
        
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }
    
    /**
     * Generate a secure password that meets the requirements of a specific encryption configuration
     * @param config The encryption configuration to generate password for
     * @return A secure password meeting the configuration's requirements
     */
    public String generatePasswordForConfig(EncryptionConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Encryption configuration cannot be null");
        }
        
        // Use the maximum of config requirement and our generator minimum (12)
        int requiredLength = Math.max(config.getMinPasswordLength(), 12);
        return generateSecurePassword(requiredLength);
    }
    
    // ===== ADVANCED PASSWORD AND SECURITY UTILITIES =====
    
    /**
     * Validate if a password meets enterprise security requirements
     * Supports international characters including CJK (Chinese, Japanese, Korean)
     * @param password The password to validate
     * @return true if password meets all security requirements
     */
    public boolean validatePassword(String password) {
        return PasswordUtil.isValidPassword(password);
    }
    
    /**
     * Read a password securely from console with masked input
     * Falls back to visible input in non-console environments (IDEs, tests)
     * @param prompt The prompt to display to the user
     * @return The entered password, or null if cancelled
     */
    public String readPasswordSecurely(String prompt) {
        return PasswordUtil.readPassword(prompt);
    }
    
    /**
     * Read a password with confirmation to prevent typos
     * Ensures both password entries match before accepting
     * @param prompt The prompt to display to the user
     * @return The confirmed password, or null if passwords don't match or cancelled
     */
    public String readPasswordWithConfirmation(String prompt) {
        return PasswordUtil.readPasswordWithConfirmation(prompt);
    }
    
    /**
     * Generate a validated secure password and optionally confirm it interactively
     * Combines generation, validation, and confirmation in one convenient method
     * @param length Desired password length (minimum 12 characters)
     * @param requireConfirmation If true, asks user to confirm the generated password
     * @return A secure, validated password
     */
    public String generateValidatedPassword(int length, boolean requireConfirmation) {
        String password;
        int attempts = 0;
        int maxAttempts = 5;
        
        do {
            password = generateSecurePassword(length);
            attempts++;
            
            if (attempts > maxAttempts) {
                throw new RuntimeException("Failed to generate valid password after " + maxAttempts + " attempts");
            }
        } while (!validatePassword(password));
        
        if (requireConfirmation) {
            System.out.println("Generated secure password: " + password);
            String confirmation = readPasswordSecurely("Please re-enter the generated password to confirm: ");
            
            if (confirmation == null || !password.equals(confirmation)) {
                System.err.println("❌ Password confirmation failed");
                return null;
            }
            System.out.println("✅ Password confirmed successfully");
        }
        
        return password;
    }
    
    /**
     * Get the default encryption configuration
     * @return Default encryption configuration settings
     */
    public EncryptionConfig getDefaultEncryptionConfig() {
        return new EncryptionConfig();
    }
    
    /**
     * Get high security encryption configuration
     * Optimized for maximum security with 256-bit keys and metadata encryption
     * @return High security encryption configuration
     */
    public EncryptionConfig getHighSecurityConfig() {
        return EncryptionConfig.createHighSecurityConfig();
    }
    
    /**
     * Get performance-optimized encryption configuration  
     * Balanced for speed while maintaining good security
     * @return Performance-optimized encryption configuration
     */
    public EncryptionConfig getPerformanceConfig() {
        return EncryptionConfig.createPerformanceConfig();
    }
    
    /**
     * Get testing encryption configuration
     * Reduced security settings for faster testing (NOT for production)
     * @return Testing encryption configuration
     */
    public EncryptionConfig getTestConfig() {
        return EncryptionConfig.createTestConfig();
    }
    
    /**
     * Create a custom encryption configuration builder
     * Allows fine-grained control over all encryption parameters
     * @return Encryption configuration builder
     */
    public EncryptionConfig.Builder createCustomConfig() {
        return EncryptionConfig.builder();
    }
    
    /**
     * Get a human-readable comparison of available encryption configurations
     * @return Comparison summary of all available configurations
     */
    public String getEncryptionConfigComparison() {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 ENCRYPTION CONFIGURATION COMPARISON\n");
        sb.append("═".repeat(50)).append("\n\n");
        
        sb.append("🔒 DEFAULT CONFIGURATION:\n");
        sb.append(getDefaultEncryptionConfig().getSummary()).append("\n\n");
        
        sb.append("🛡️ HIGH SECURITY CONFIGURATION:\n");
        sb.append(getHighSecurityConfig().getSummary()).append("\n\n");
        
        sb.append("⚡ PERFORMANCE CONFIGURATION:\n");
        sb.append(getPerformanceConfig().getSummary()).append("\n\n");
        
        sb.append("🧪 TEST CONFIGURATION (Development Only):\n");
        sb.append(getTestConfig().getSummary()).append("\n\n");
        
        sb.append("💡 RECOMMENDATIONS:\n");
        sb.append("   • Use HIGH SECURITY for sensitive financial/medical data\n");
        sb.append("   • Use PERFORMANCE for high-volume applications\n");
        sb.append("   • Use DEFAULT for general-purpose encryption\n");
        sb.append("   • Use TEST only during development/testing");
        
        return sb.toString();
    }
    
    // ===== HELPER METHODS =====
    
    private void validateKeyPair() {
        if (defaultKeyPair == null) {
            throw new IllegalStateException("No default key pair set. Call setDefaultCredentials() or use methods with explicit keys.");
        }
    }
    
    
    /**
     * Get the underlying blockchain instance for advanced operations
     * @return The blockchain instance
     */
    public Blockchain getBlockchain() {
        return blockchain;
    }
    
    /**
     * Get the default username
     * @return The default username, or null if not set
     */
    public String getDefaultUsername() {
        return defaultUsername;
    }
    
    /**
     * Check if default credentials are set
     * @return true if default credentials are available
     */
    public boolean hasDefaultCredentials() {
        return defaultKeyPair != null && defaultUsername != null;
    }
    
    // ===== FORMATTING AND DISPLAY UTILITIES =====
    
    /**
     * Format block information for user-friendly display
     * @param block The block to format
     * @return Formatted block information with truncated hashes and readable timestamps
     */
    public String formatBlockDisplay(Block block) {
        return FormatUtil.formatBlockInfo(block);
    }
    
    /**
     * Format a list of blocks as a summary table
     * @param blocks List of blocks to format
     * @return Formatted table with block information
     */
    public String formatBlocksSummary(List<Block> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return "No blocks found.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("📋 BLOCKS SUMMARY (").append(blocks.size()).append(" block(s))\n");
        sb.append("═".repeat(80)).append("\n");
        sb.append(String.format("%-6s %-20s %-12s %-8s %-10s\n", 
            "Block#", "Timestamp", "Hash", "Encrypted", "Data Size"));
        sb.append("-".repeat(80)).append("\n");
        
        for (Block block : blocks) {
            String timestamp = FormatUtil.formatTimestamp(block.getTimestamp(), "MM-dd HH:mm:ss");
            String hash = FormatUtil.truncateHash(block.getHash());
            String encrypted = block.isDataEncrypted() ? "🔐 Yes" : "📖 No";
            String dataSize = block.getData().length() + " chars";
            
            sb.append(String.format("%-6s %-20s %-12s %-8s %-10s\n",
                "#" + block.getBlockNumber(),
                timestamp,
                hash.substring(0, Math.min(12, hash.length())),
                encrypted,
                dataSize));
        }
        
        return sb.toString();
    }
    
    /**
     * Format search results with enhanced display including statistics
     * @param searchTerm The search term used
     * @param results List of matching blocks
     * @return Formatted search results with statistics and block details
     */
    public String formatSearchResults(String searchTerm, List<Block> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔍 SEARCH RESULTS for '").append(searchTerm).append("'\n");
        sb.append("═".repeat(60)).append("\n\n");
        
        if (results == null || results.isEmpty()) {
            sb.append("No results found.\n\n");
            sb.append("💡 Search Tips:\n");
            sb.append("   • Try using 'smartSearch()' for natural language queries\n");
            sb.append("   • Use 'extractKeywords()' to see what terms are extracted\n");
            sb.append("   • Check if content is encrypted and use password search\n");
            return sb.toString();
        }
        
        // Statistics
        long encryptedCount = results.stream().mapToLong(b -> b.isDataEncrypted() ? 1 : 0).sum();
        long unencryptedCount = results.size() - encryptedCount;
        
        sb.append("📊 Statistics:\n");
        sb.append("   📝 Total Results: ").append(results.size()).append("\n");
        sb.append("   🔐 Encrypted: ").append(encryptedCount).append("\n");
        sb.append("   📖 Unencrypted: ").append(unencryptedCount).append("\n\n");
        
        // Block details
        sb.append("📋 Block Details:\n");
        sb.append("-".repeat(60)).append("\n");
        
        for (int i = 0; i < results.size(); i++) {
            Block block = results.get(i);
            sb.append(String.format("%d. Block #%d %s\n", 
                i + 1, 
                block.getBlockNumber(),
                block.isDataEncrypted() ? "🔐" : "📖"));
            sb.append("   Timestamp: ").append(FormatUtil.formatTimestamp(block.getTimestamp())).append("\n");
            sb.append("   Hash: ").append(FormatUtil.truncateHash(block.getHash())).append("\n");
            
            // Show data preview (first 100 chars)
            String dataPreview = block.getData();
            if (dataPreview.length() > 100) {
                dataPreview = FormatUtil.fixedWidth(dataPreview, 100);
            }
            sb.append("   Data: ").append(dataPreview).append("\n");
            
            if (i < results.size() - 1) {
                sb.append("\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Generate a comprehensive blockchain status report
     * @return Detailed status report with encryption statistics and recent activity
     */
    public String generateBlockchainStatusReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("🏗️ BLOCKCHAIN STATUS REPORT\n");
        sb.append("═".repeat(50)).append("\n\n");
        
        // Basic statistics
        List<Block> allBlocks = blockchain.getAllBlocks();
        long totalBlocks = allBlocks.size();
        long encryptedBlocks = getEncryptedBlockCount();
        long unencryptedBlocks = getUnencryptedBlockCount();
        
        sb.append("📊 Block Statistics:\n");
        sb.append("   📝 Total Blocks: ").append(totalBlocks).append("\n");
        sb.append("   🔐 Encrypted Blocks: ").append(encryptedBlocks).append("\n");
        sb.append("   📖 Unencrypted Blocks: ").append(unencryptedBlocks).append("\n");
        
        if (totalBlocks > 0) {
            double encryptionPercentage = (double) encryptedBlocks / totalBlocks * 100;
            sb.append("   📈 Encryption Rate: ").append(String.format("%.1f%%", encryptionPercentage)).append("\n");
        }
        sb.append("\n");
        
        // Recent activity (last 5 blocks)
        if (totalBlocks > 0) {
            sb.append("📋 Recent Activity (Last 5 Blocks):\n");
            sb.append("-".repeat(50)).append("\n");
            
            List<Block> recentBlocks = allBlocks.stream()
                .skip(Math.max(0, totalBlocks - 5))
                .collect(java.util.stream.Collectors.toList());
            
            for (Block block : recentBlocks) {
                sb.append(String.format("Block #%d %s - %s\n",
                    block.getBlockNumber(),
                    block.isDataEncrypted() ? "🔐" : "📖",
                    FormatUtil.formatTimestamp(block.getTimestamp(), "MM-dd HH:mm:ss")));
            }
            sb.append("\n");
        }
        
        // Validation status
        boolean isValid = validateEncryptedBlocks();
        sb.append("🔍 Validation Status:\n");
        sb.append("   ").append(isValid ? "✅ All blocks valid" : "❌ Validation issues found").append("\n\n");
        
        // Configuration summary
        EncryptionConfig defaultConfig = getDefaultEncryptionConfig();
        sb.append("🔧 Current Configuration:\n");
        sb.append("   Algorithm: ").append(defaultConfig.getEncryptionTransformation()).append("\n");
        sb.append("   Key Length: ").append(defaultConfig.getKeyLength()).append(" bits\n");
        sb.append("   Min Password: ").append(defaultConfig.getMinPasswordLength()).append(" chars\n\n");
        
        // Available features
        sb.append("🚀 Available Features:\n");
        sb.append("   • Smart search with keyword extraction\n");
        sb.append("   • Multiple encryption configurations\n");
        sb.append("   • Content similarity analysis\n");
        sb.append("   • Advanced password utilities\n");
        sb.append("   • Unified search across encrypted/unencrypted data\n");
        sb.append("   • Enterprise-grade security validation\n");
        
        return sb.toString();
    }
    
    // ===== ADVANCED KEY MANAGEMENT SERVICES =====
    
    /**
     * Save the current user's private key securely to disk
     * Uses AES-256 encryption with password protection
     * @param password Password to protect the stored key
     * @return true if key was saved successfully
     */
    public boolean saveUserKeySecurely(String password) {
        validateKeyPair();
        if (defaultUsername == null) {
            throw new IllegalStateException("No default username set. Call setDefaultCredentials() first.");
        }
        return SecureKeyStorage.savePrivateKey(defaultUsername, defaultKeyPair.getPrivate(), password);
    }
    
    /**
     * Save any user's private key securely to disk
     * Uses AES-256 encryption with password protection
     * @param username Username to associate with the key
     * @param privateKey Private key to save
     * @param password Password to protect the stored key
     * @return true if key was saved successfully
     */
    public boolean savePrivateKeySecurely(String username, PrivateKey privateKey, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key cannot be null");
        }
        return SecureKeyStorage.savePrivateKey(username, privateKey, password);
    }
    
    /**
     * Load a user's private key from secure storage
     * @param username Username associated with the key
     * @param password Password to decrypt the key
     * @return PrivateKey object or null if loading failed
     */
    public PrivateKey loadPrivateKeySecurely(String username, String password) {
        return SecureKeyStorage.loadPrivateKey(username, password);
    }
    
    /**
     * Load and set a user's credentials from secure storage
     * @param username Username to load
     * @param password Password to decrypt the key
     * @return true if credentials were loaded and set successfully
     */
    public boolean loadUserCredentials(String username, String password) {
        PrivateKey privateKey = SecureKeyStorage.loadPrivateKey(username, password);
        if (privateKey != null) {
            try {
                PublicKey publicKey = keyDerivation.derivePublicKeyFromPrivate(privateKey);
                this.defaultKeyPair = new KeyPair(publicKey, privateKey);
                this.defaultUsername = username;
                
                // Auto-register the user
                String publicKeyString = CryptoUtil.publicKeyToString(publicKey);
                blockchain.addAuthorizedKey(publicKeyString, username);
                
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
    
    /**
     * Check if a user has a stored private key
     * @param username Username to check
     * @return true if the user has a stored key
     */
    public boolean hasStoredKey(String username) {
        return SecureKeyStorage.hasPrivateKey(username);
    }
    
    /**
     * Delete a user's stored private key
     * @param username Username whose key to delete
     * @return true if key was deleted successfully
     */
    public boolean deleteStoredKey(String username) {
        return SecureKeyStorage.deletePrivateKey(username);
    }
    
    /**
     * List all users with stored private keys
     * @return Array of usernames with stored keys
     */
    public String[] listStoredUsers() {
        return SecureKeyStorage.listStoredKeys();
    }
    
    /**
     * Import a private key from a file
     * Supports PEM, DER, and Base64 formats
     * @param keyFilePath Path to the key file
     * @return PrivateKey object or null if import failed
     */
    public PrivateKey importPrivateKeyFromFile(String keyFilePath) {
        if (!KeyFileLoader.isValidKeyFilePath(keyFilePath)) {
            throw new IllegalArgumentException("Invalid key file path: " + keyFilePath);
        }
        return KeyFileLoader.loadPrivateKeyFromFile(keyFilePath);
    }
    
    /**
     * Import a public key from a file
     * Supports PEM and Base64 formats
     * @param keyFilePath Path to the key file
     * @return PublicKey object or null if import failed
     */
    public PublicKey importPublicKeyFromFile(String keyFilePath) {
        if (!KeyFileLoader.isValidKeyFilePath(keyFilePath)) {
            throw new IllegalArgumentException("Invalid key file path: " + keyFilePath);
        }
        return KeyFileLoader.loadPublicKeyFromFile(keyFilePath);
    }
    
    /**
     * Import and register a user from a key file
     * @param username Username for the imported key
     * @param keyFilePath Path to the private key file
     * @return true if user was imported and registered successfully
     */
    public boolean importAndRegisterUser(String username, String keyFilePath) {
        try {
            PrivateKey privateKey = importPrivateKeyFromFile(keyFilePath);
            if (privateKey == null) {
                return false;
            }
            
            PublicKey publicKey = keyDerivation.derivePublicKeyFromPrivate(privateKey);
            String publicKeyString = CryptoUtil.publicKeyToString(publicKey);
            
            return blockchain.addAuthorizedKey(publicKeyString, username);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Import, register, and set as default user from a key file
     * @param username Username for the imported key
     * @param keyFilePath Path to the private key file
     * @return true if user was imported, registered and set as default successfully
     */
    public boolean importAndSetDefaultUser(String username, String keyFilePath) {
        try {
            PrivateKey privateKey = importPrivateKeyFromFile(keyFilePath);
            if (privateKey == null) {
                return false;
            }
            
            PublicKey publicKey = keyDerivation.derivePublicKeyFromPrivate(privateKey);
            KeyPair keyPair = new KeyPair(publicKey, privateKey);
            
            String publicKeyString = CryptoUtil.publicKeyToString(publicKey);
            boolean registered = blockchain.addAuthorizedKey(publicKeyString, username);
            
            if (registered) {
                setDefaultCredentials(username, keyPair);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Detect the format of a key file
     * @param keyFilePath Path to the key file
     * @return Description of the detected format
     */
    public String detectKeyFileFormat(String keyFilePath) {
        return KeyFileLoader.detectKeyFileFormat(keyFilePath);
    }
    
    // ===== ADVANCED CRYPTOGRAPHIC SERVICES =====
    
    /**
     * Derive a public key from a private key using elliptic curve mathematics
     * @param privateKey The private key to derive from
     * @return The corresponding public key
     */
    public PublicKey derivePublicKeyFromPrivate(PrivateKey privateKey) {
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key cannot be null");
        }
        return keyDerivation.derivePublicKeyFromPrivate(privateKey);
    }
    
    /**
     * Verify that a private and public key form a valid pair
     * @param privateKey The private key to verify
     * @param publicKey The public key to verify
     * @return true if they form a valid key pair
     */
    public boolean verifyKeyPairConsistency(PrivateKey privateKey, PublicKey publicKey) {
        try {
            PublicKey derivedPublic = keyDerivation.derivePublicKeyFromPrivate(privateKey);
            return CryptoUtil.publicKeyToString(derivedPublic).equals(CryptoUtil.publicKeyToString(publicKey));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Create a complete key pair from just a private key
     * @param privateKey The private key
     * @return Complete KeyPair object
     */
    public KeyPair createKeyPairFromPrivate(PrivateKey privateKey) {
        PublicKey publicKey = keyDerivation.derivePublicKeyFromPrivate(privateKey);
        return new KeyPair(publicKey, privateKey);
    }
    
    /**
     * Derive a public key using custom elliptic curve parameters
     * Advanced feature for cryptographic experts requiring specific curves
     * @param privateKey The private key to derive from
     * @param curveParams Custom curve parameters to use
     * @return The derived public key using custom curve
     */
    public PublicKey derivePublicKeyWithCustomCurve(PrivateKey privateKey, ECParameterSpec curveParams) {
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key cannot be null");
        }
        if (curveParams == null) {
            throw new IllegalArgumentException("Curve parameters cannot be null");
        }
        return keyDerivation.derivePublicKeyFromPrivate(privateKey, curveParams);
    }
    
    /**
     * Get supported elliptic curve parameters for advanced cryptographic operations
     * @param curveName Name of the curve (e.g., "secp256r1")
     * @return ECParameterSpec for the requested curve
     */
    public ECParameterSpec getCurveParameters(String curveName) {
        if (curveName == null || curveName.trim().isEmpty()) {
            throw new IllegalArgumentException("Curve name cannot be null or empty");
        }
        return keyDerivation.getCurveParameters(curveName);
    }
    
    /**
     * Validate that a point lies on the specified elliptic curve
     * Advanced cryptographic validation for custom implementations
     * @param point The elliptic curve point to validate
     * @param curveParams The curve parameters to validate against
     * @return true if the point is mathematically valid on the curve
     */
    public boolean validateECPoint(ECPoint point, ECParameterSpec curveParams) {
        if (point == null || curveParams == null) {
            return false;
        }
        return keyDerivation.isPointOnCurve(point, curveParams);
    }
    
    /**
     * Perform comprehensive key pair verification using mathematical validation
     * More thorough than basic consistency check - uses EC mathematics
     * @param privateKey The private key to verify
     * @param publicKey The public key to verify
     * @return true if they form a mathematically valid EC key pair
     */
    public boolean verifyKeyPairMathematically(PrivateKey privateKey, PublicKey publicKey) {
        if (privateKey == null || publicKey == null) {
            return false;
        }
        return keyDerivation.verifyKeyPair(privateKey, publicKey);
    }
    
    // ===== BLOCKCHAIN RECOVERY AND MANAGEMENT SERVICES =====
    
    /**
     * Recover the blockchain from corruption caused by deleted keys
     * Uses multiple recovery strategies: re-authorization, rollback, and export
     * @param deletedUsername Username whose key was deleted and caused corruption
     * @return Recovery result with success status and details
     */
    public ChainRecoveryManager.RecoveryResult recoverFromCorruption(String deletedUsername) {
        if (deletedUsername == null || deletedUsername.trim().isEmpty()) {
            return new ChainRecoveryManager.RecoveryResult(false, "VALIDATION_ERROR", 
                "Username cannot be null or empty");
        }
        
        // Try to find the deleted public key by searching stored keys first
        String deletedPublicKey = null;
        
        // If we can load the key from storage, derive the public key
        if (hasStoredKey(deletedUsername)) {
            try {
                // We can't load without password, so we'll need to search blockchain history
                // For now, we'll search through blockchain authorized keys history
                var authorizedKeys = blockchain.getAuthorizedKeys();
                for (var key : authorizedKeys) {
                    if (key.getOwnerName().equals(deletedUsername)) {
                        deletedPublicKey = key.getPublicKey();
                        break;
                    }
                }
            } catch (Exception e) {
                // Continue with search
            }
        }
        
        if (deletedPublicKey == null) {
            return new ChainRecoveryManager.RecoveryResult(false, "VALIDATION_ERROR", 
                "Could not determine public key for user: " + deletedUsername + 
                ". Recovery requires the exact public key that was deleted.");
        }
        
        return recoveryManager.recoverCorruptedChain(deletedPublicKey, deletedUsername);
    }
    
    /**
     * Recover blockchain corruption with known public key
     * @param deletedPublicKey The exact public key that was deleted
     * @param originalUsername Original username for re-authorization
     * @return Recovery result with success status and details
     */
    public ChainRecoveryManager.RecoveryResult recoverFromCorruptionWithKey(String deletedPublicKey, String originalUsername) {
        return recoveryManager.recoverCorruptedChain(deletedPublicKey, originalUsername);
    }
    
    /**
     * Diagnose blockchain corruption and health
     * @return Diagnostic information about corrupted blocks
     */
    public ChainRecoveryManager.ChainDiagnostic diagnoseChainHealth() {
        return recoveryManager.diagnoseCorruption();
    }
    
    /**
     * Check if the blockchain can be recovered from any corruption
     * @return true if recovery is possible
     */
    public boolean canRecoverFromFailure() {
        try {
            var diagnostic = diagnoseChainHealth();
            return diagnostic.getCorruptedBlocks() >= 0; // -1 indicates diagnostic failure
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get a comprehensive report of blockchain recovery capabilities
     * @return Human-readable recovery status report
     */
    public String getRecoveryCapabilityReport() {
        try {
            var diagnostic = diagnoseChainHealth();
            StringBuilder sb = new StringBuilder();
            
            sb.append("🏥 BLOCKCHAIN RECOVERY CAPABILITY REPORT\\n");
            sb.append("═".repeat(50)).append("\\n\\n");
            
            sb.append("📊 Current Status:\\n");
            sb.append("   📝 Total Blocks: ").append(diagnostic.getTotalBlocks()).append("\\n");
            sb.append("   ✅ Valid Blocks: ").append(diagnostic.getValidBlocks()).append("\\n");
            sb.append("   ❌ Corrupted Blocks: ").append(diagnostic.getCorruptedBlocks()).append("\\n");
            sb.append("   💚 Chain Health: ").append(diagnostic.isHealthy() ? "HEALTHY" : "CORRUPTED").append("\\n\\n");
            
            if (diagnostic.isHealthy()) {
                sb.append("🎉 STATUS: Blockchain is healthy, no recovery needed\\n\\n");
                sb.append("🛡️ Available Recovery Features:\\n");
                sb.append("   • Secure key storage and import/export\\n");
                sb.append("   • Key pair validation and derivation\\n");
                sb.append("   • Preventive corruption detection\\n");
            } else {
                sb.append("🚨 STATUS: Blockchain corruption detected\\n\\n");
                sb.append("🔧 Available Recovery Strategies:\\n");
                sb.append("   1. Re-authorization recovery (least disruptive)\\n");
                sb.append("   2. Intelligent rollback with data preservation\\n");
                sb.append("   3. Export valid portion for manual recovery\\n\\n");
                
                sb.append("💡 Recommended Actions:\\n");
                sb.append("   • Use 'recoverFromCorruption()' to attempt automatic recovery\\n");
                sb.append("   • Ensure deleted user keys are available for re-authorization\\n");
                sb.append("   • Consider exporting valid data before attempting recovery\\n");
            }
            
            sb.append("\\n🔄 Recovery Success Rate: High (multiple strategies available)");
            
            return sb.toString();
        } catch (Exception e) {
            return "❌ Error generating recovery report: " + e.getMessage();
        }
    }
    
    // ===== ADVANCED VALIDATION AND INTEGRITY SERVICES =====
    
    /**
     * Validate a specific block with comprehensive off-chain data integrity checking
     * Uses advanced validation algorithms for enterprise-grade security
     * @param blockId The ID of the block to validate
     * @return Detailed validation result with specific failure reasons
     */
    public BlockValidationUtil.OffChainValidationResult validateBlockDetailed(Long blockId) {
        if (blockId == null) {
            return new BlockValidationUtil.OffChainValidationResult(false, "Block ID cannot be null");
        }
        
        try {
            List<Block> allBlocks = blockchain.getAllBlocks();
            Block targetBlock = null;
            
            for (Block block : allBlocks) {
                if (block.getBlockNumber().equals(blockId)) {
                    targetBlock = block;
                    break;
                }
            }
            
            if (targetBlock == null) {
                return new BlockValidationUtil.OffChainValidationResult(false, 
                    "Block #" + blockId + " not found in blockchain");
            }
            
            return BlockValidationUtil.validateOffChainDataDetailed(targetBlock);
            
        } catch (Exception e) {
            return new BlockValidationUtil.OffChainValidationResult(false, 
                "Validation error: " + e.getMessage());
        }
    }
    
    /**
     * Detect potential tampering in off-chain data using advanced algorithms
     * Analyzes file timestamps, sizes, and integrity markers
     * @param blockId The block ID to check for tampering
     * @return true if no tampering detected, false if suspicious activity found
     */
    public boolean detectDataTampering(Long blockId) {
        if (blockId == null) {
            return false;
        }
        
        try {
            List<Block> allBlocks = blockchain.getAllBlocks();
            
            for (Block block : allBlocks) {
                if (block.getBlockNumber().equals(blockId)) {
                    return BlockValidationUtil.detectOffChainTampering(block);
                }
            }
            
            return false; // Block not found
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Validate off-chain data integrity for a specific block
     * Performs comprehensive file existence, size, and metadata validation
     * @param blockId The block ID to validate
     * @return true if off-chain data is valid or not present
     */
    public boolean validateOffChainData(Long blockId) {
        if (blockId == null) {
            return false;
        }
        
        try {
            List<Block> allBlocks = blockchain.getAllBlocks();
            
            for (Block block : allBlocks) {
                if (block.getBlockNumber().equals(blockId)) {
                    return BlockValidationUtil.validateOffChainData(blockchain, block);
                }
            }
            
            return false; // Block not found
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if off-chain files exist and are accessible for a specific block
     * @param blockId The block ID to check
     * @return true if files exist or block has no off-chain data
     */
    public boolean offChainFilesExist(Long blockId) {
        if (blockId == null) {
            return false;
        }
        
        try {
            List<Block> allBlocks = blockchain.getAllBlocks();
            
            for (Block block : allBlocks) {
                if (block.getBlockNumber().equals(blockId)) {
                    return BlockValidationUtil.offChainFileExists(block);
                }
            }
            
            return false; // Block not found
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Validate genesis block with specialized validation logic
     * @return true if genesis block is properly formatted
     */
    public boolean validateGenesisBlock() {
        try {
            List<Block> allBlocks = blockchain.getAllBlocks();
            if (allBlocks.isEmpty()) {
                return false;
            }
            
            Block genesisBlock = allBlocks.get(0);
            return BlockValidationUtil.validateGenesisBlock(genesisBlock);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if a key was authorized at a specific timestamp
     * Advanced temporal authorization validation for audit trails
     * @param username Username to check
     * @param timestamp Timestamp to check authorization at
     * @return true if the key was authorized at the given time
     */
    public boolean wasKeyAuthorizedAt(String username, java.time.LocalDateTime timestamp) {
        if (username == null || timestamp == null) {
            return false;
        }
        
        try {
            // Find the public key for this username
            var authorizedKeys = blockchain.getAuthorizedKeys();
            String publicKeyString = null;
            
            for (var key : authorizedKeys) {
                if (key.getOwnerName().equals(username)) {
                    publicKeyString = key.getPublicKey();
                    break;
                }
            }
            
            if (publicKeyString == null) {
                return false;
            }
            
            return BlockValidationUtil.wasKeyAuthorizedAt(blockchain, publicKeyString, timestamp);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Generate a comprehensive blockchain integrity report
     * Analyzes all blocks for validation issues, tampering, and inconsistencies
     * @return Detailed integrity analysis report
     */
    public String generateIntegrityReport() {
        try {
            StringBuilder report = new StringBuilder();
            List<Block> allBlocks = blockchain.getAllBlocks();
            
            report.append("🔍 COMPREHENSIVE BLOCKCHAIN INTEGRITY REPORT\n");
            report.append("═".repeat(60)).append("\n\n");
            
            int totalBlocks = allBlocks.size();
            int validBlocks = 0;
            int tamperingDetected = 0;
            int offChainIssues = 0;
            int missingFiles = 0;
            
            report.append("📊 Analysis Summary:\n");
            report.append("   📝 Total Blocks: ").append(totalBlocks).append("\n\n");
            
            report.append("🔍 Detailed Block Analysis:\n");
            report.append("-".repeat(40)).append("\n");
            
            for (Block block : allBlocks) {
                Long blockId = block.getBlockNumber();
                boolean blockValid = blockchain.validateSingleBlock(block);
                boolean filesExist = BlockValidationUtil.offChainFileExists(block);
                boolean noTampering = BlockValidationUtil.detectOffChainTampering(block);
                var detailedValidation = BlockValidationUtil.validateOffChainDataDetailed(block);
                
                report.append(String.format("Block #%d: ", blockId));
                
                if (blockValid) {
                    validBlocks++;
                    report.append("✅ Valid");
                } else {
                    report.append("❌ Invalid");
                }
                
                if (block.hasOffChainData()) {
                    if (!filesExist) {
                        report.append(" | 📄 Missing Files");
                        missingFiles++;
                    } else if (!noTampering) {
                        report.append(" | ⚠️ Potential Tampering");
                        tamperingDetected++;
                    } else if (!detailedValidation.isValid()) {
                        report.append(" | 🔍 Off-chain Issues");
                        offChainIssues++;
                    } else {
                        report.append(" | 📄 Off-chain OK");
                    }
                }
                
                report.append("\n");
            }
            
            report.append("\n📈 Integrity Statistics:\n");
            report.append("   ✅ Valid Blocks: ").append(validBlocks).append("/").append(totalBlocks);
            if (totalBlocks > 0) {
                report.append(String.format(" (%.1f%%)", (double) validBlocks / totalBlocks * 100));
            }
            report.append("\n");
            report.append("   📄 Missing Off-chain Files: ").append(missingFiles).append("\n");
            report.append("   ⚠️ Potential Tampering Cases: ").append(tamperingDetected).append("\n");
            report.append("   🔍 Off-chain Validation Issues: ").append(offChainIssues).append("\n\n");
            
            // Overall assessment
            boolean isHealthy = (validBlocks == totalBlocks) && (missingFiles == 0) && 
                              (tamperingDetected == 0) && (offChainIssues == 0);
            
            report.append("🏥 Overall Blockchain Health: ");
            if (isHealthy) {
                report.append("✅ EXCELLENT - No issues detected\n");
            } else if (validBlocks == totalBlocks && tamperingDetected == 0) {
                report.append("🟡 GOOD - Minor off-chain issues\n");
            } else if (tamperingDetected == 0) {
                report.append("🟠 FAIR - Some validation issues\n");
            } else {
                report.append("🔴 CRITICAL - Tampering or corruption detected\n");
            }
            
            report.append("\n💡 Recommendations:\n");
            if (missingFiles > 0) {
                report.append("   • Restore missing off-chain files from backup\n");
            }
            if (tamperingDetected > 0) {
                report.append("   • Investigate potential tampering incidents\n");
                report.append("   • Consider rolling back to last known good state\n");
            }
            if (offChainIssues > 0) {
                report.append("   • Verify off-chain data integrity and fix metadata\n");
            }
            if (validBlocks < totalBlocks) {
                report.append("   • Use recovery tools to repair corrupted blocks\n");
            }
            if (isHealthy) {
                report.append("   • Continue regular monitoring and backup procedures\n");
            }
            
            return report.toString();
            
        } catch (Exception e) {
            return "❌ Error generating integrity report: " + e.getMessage();
        }
    }
    
    // ===== LARGE FILE STORAGE AND MANAGEMENT SERVICES =====
    
    /**
     * Store a large file off-chain with AES-256-GCM encryption
     * Perfect for documents, images, videos, and other large data that shouldn't be stored directly in blocks
     * @param fileData The file data as byte array
     * @param password Password for encrypting the file
     * @param contentType MIME type of the file (e.g., "application/pdf", "image/jpeg")
     * @return OffChainData metadata object containing file reference and encryption details
     */
    public OffChainData storeLargeFileSecurely(byte[] fileData, String password, String contentType) {
        validateKeyPair();
        if (fileData == null || fileData.length == 0) {
            throw new IllegalArgumentException("File data cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        try {
            String publicKeyString = CryptoUtil.publicKeyToString(defaultKeyPair.getPublic());
            return offChainStorage.storeData(fileData, password, defaultKeyPair.getPrivate(), 
                                           publicKeyString, contentType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to store large file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Store a large file with custom signer credentials
     * Allows storing files with different user credentials than the default
     * @param fileData The file data as byte array
     * @param password Password for encrypting the file
     * @param signerKeyPair Key pair for signing the file metadata
     * @param signerUsername Username of the file owner
     * @param contentType MIME type of the file
     * @return OffChainData metadata object
     */
    public OffChainData storeLargeFileWithSigner(byte[] fileData, String password, 
                                               KeyPair signerKeyPair, String signerUsername, 
                                               String contentType) {
        if (fileData == null || fileData.length == 0) {
            throw new IllegalArgumentException("File data cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (signerKeyPair == null) {
            throw new IllegalArgumentException("Signer key pair cannot be null");
        }
        
        try {
            String publicKeyString = CryptoUtil.publicKeyToString(signerKeyPair.getPublic());
            return offChainStorage.storeData(fileData, password, signerKeyPair.getPrivate(), 
                                           publicKeyString, contentType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to store large file with custom signer: " + e.getMessage(), e);
        }
    }
    
    /**
     * Retrieve and decrypt a large file from off-chain storage
     * @param offChainData The metadata object containing file reference
     * @param password Password for decrypting the file
     * @return Decrypted file data as byte array
     */
    public byte[] retrieveLargeFile(OffChainData offChainData, String password) {
        if (offChainData == null) {
            throw new IllegalArgumentException("OffChainData cannot be null");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        try {
            return offChainStorage.retrieveData(offChainData, password);
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve large file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Verify the integrity of an off-chain file without fully decrypting it
     * Performs hash verification and basic security checks
     * @param offChainData The metadata object to verify
     * @param password Password for accessing the file
     * @return true if file integrity is verified
     */
    public boolean verifyLargeFileIntegrity(OffChainData offChainData, String password) {
        if (offChainData == null || password == null) {
            return false;
        }
        
        try {
            return offChainStorage.verifyIntegrity(offChainData, password);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Delete an off-chain file from storage
     * WARNING: This permanently removes the file and cannot be undone
     * @param offChainData The metadata object referencing the file to delete
     * @return true if file was successfully deleted
     */
    public boolean deleteLargeFile(OffChainData offChainData) {
        if (offChainData == null) {
            return false;
        }
        
        return offChainStorage.deleteData(offChainData);
    }
    
    /**
     * Check if an off-chain file exists and is accessible
     * @param offChainData The metadata object to check
     * @return true if file exists and can be accessed
     */
    public boolean largeFileExists(OffChainData offChainData) {
        if (offChainData == null) {
            return false;
        }
        
        return offChainStorage.fileExists(offChainData);
    }
    
    /**
     * Get the size of an off-chain file
     * @param offChainData The metadata object for the file
     * @return File size in bytes, or -1 if file doesn't exist or error occurred
     */
    public long getLargeFileSize(OffChainData offChainData) {
        if (offChainData == null) {
            return -1;
        }
        
        return offChainStorage.getFileSize(offChainData);
    }
    
    /**
     * Store a large text document with automatic content type detection
     * Convenience method for storing text files, documents, and JSON data
     * @param textContent The text content to store
     * @param password Password for encryption
     * @param filename Optional filename for content type detection
     * @return OffChainData metadata object
     */
    public OffChainData storeLargeTextDocument(String textContent, String password, String filename) {
        if (textContent == null || textContent.isEmpty()) {
            throw new IllegalArgumentException("Text content cannot be null or empty");
        }
        
        // Detect content type based on filename extension
        String contentType = detectContentType(filename);
        
        byte[] contentBytes = textContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return storeLargeFileSecurely(contentBytes, password, contentType);
    }
    
    /**
     * Retrieve a large text document and return it as a string
     * Convenience method for retrieving text files with proper encoding
     * @param offChainData The metadata object for the text file
     * @param password Password for decryption
     * @return Text content as string with UTF-8 encoding
     */
    public String retrieveLargeTextDocument(OffChainData offChainData, String password) {
        byte[] fileData = retrieveLargeFile(offChainData, password);
        return new String(fileData, java.nio.charset.StandardCharsets.UTF_8);
    }
    
    /**
     * Generate a comprehensive report of all off-chain storage usage
     * Analyzes file types, sizes, and storage efficiency
     * @return Detailed storage analytics report
     */
    public String generateOffChainStorageReport() {
        try {
            StringBuilder report = new StringBuilder();
            List<Block> allBlocks = blockchain.getAllBlocks();
            
            report.append("📁 OFF-CHAIN STORAGE COMPREHENSIVE REPORT\n");
            report.append("═".repeat(60)).append("\n\n");
            
            int totalBlocks = allBlocks.size();
            int blocksWithOffChain = 0;
            long totalOffChainSize = 0;
            int missingFiles = 0;
            int corruptFiles = 0;
            
            java.util.Map<String, Integer> contentTypes = new java.util.HashMap<>();
            java.util.Map<String, Long> sizeByType = new java.util.HashMap<>();
            
            report.append("📊 Storage Analysis:\n");
            report.append("   📝 Total Blocks: ").append(totalBlocks).append("\n");
            
            for (Block block : allBlocks) {
                if (block.hasOffChainData()) {
                    blocksWithOffChain++;
                    OffChainData offChainData = block.getOffChainData();
                    
                    // Count by content type
                    String contentType = offChainData.getContentType();
                    contentTypes.put(contentType, contentTypes.getOrDefault(contentType, 0) + 1);
                    
                    // Size analysis
                    long fileSize = offChainData.getFileSize() != null ? offChainData.getFileSize() : 0;
                    totalOffChainSize += fileSize;
                    sizeByType.put(contentType, sizeByType.getOrDefault(contentType, 0L) + fileSize);
                    
                    // Check file status
                    if (!offChainStorage.fileExists(offChainData)) {
                        missingFiles++;
                    } else {
                        // Quick integrity check (we don't have password here, so basic check only)
                        try {
                            long actualSize = offChainStorage.getFileSize(offChainData);
                            if (actualSize != fileSize) {
                                corruptFiles++;
                            }
                        } catch (Exception e) {
                            corruptFiles++;
                        }
                    }
                }
            }
            
            report.append("   📄 Blocks with Off-chain Data: ").append(blocksWithOffChain).append("\n");
            report.append("   💾 Total Off-chain Storage: ").append(formatFileSize(totalOffChainSize)).append("\n");
            report.append("   📄 Missing Files: ").append(missingFiles).append("\n");
            report.append("   ⚠️ Potentially Corrupt Files: ").append(corruptFiles).append("\n\n");
            
            // Content type breakdown
            report.append("📋 Content Types:\n");
            for (java.util.Map.Entry<String, Integer> entry : contentTypes.entrySet()) {
                String type = entry.getKey();
                int count = entry.getValue();
                long size = sizeByType.getOrDefault(type, 0L);
                report.append(String.format("   %s: %d file(s), %s\n", 
                    type, count, formatFileSize(size)));
            }
            
            // Storage efficiency
            report.append("\n📈 Storage Efficiency:\n");
            if (totalBlocks > 0) {
                double offChainPercentage = (double) blocksWithOffChain / totalBlocks * 100;
                report.append(String.format("   📊 Off-chain Usage: %.1f%% of blocks\n", offChainPercentage));
            }
            
            double avgFileSize = blocksWithOffChain > 0 ? (double) totalOffChainSize / blocksWithOffChain : 0;
            report.append(String.format("   📏 Average File Size: %s\n", formatFileSize((long) avgFileSize)));
            
            // Health assessment
            report.append("\n🏥 Storage Health: ");
            if (missingFiles == 0 && corruptFiles == 0) {
                report.append("✅ EXCELLENT - All files intact\n");
            } else if (missingFiles == 0) {
                report.append("🟡 GOOD - No missing files, some integrity issues\n");
            } else {
                report.append("🔴 CRITICAL - Missing or corrupt files detected\n");
            }
            
            // Recommendations
            report.append("\n💡 Recommendations:\n");
            if (missingFiles > 0) {
                report.append("   • Restore missing files from backup immediately\n");
            }
            if (corruptFiles > 0) {
                report.append("   • Verify file integrity with proper passwords\n");
            }
            if (totalOffChainSize > 1024 * 1024 * 1024) { // 1GB
                report.append("   • Consider implementing file archival strategy\n");
            }
            if (missingFiles == 0 && corruptFiles == 0) {
                report.append("   • Continue regular backup and monitoring procedures\n");
            }
            
            return report.toString();
            
        } catch (Exception e) {
            return "❌ Error generating off-chain storage report: " + e.getMessage();
        }
    }
    
    // ===== HELPER METHODS FOR OFF-CHAIN STORAGE =====
    
    /**
     * Detect content type based on filename extension
     */
    private String detectContentType(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "text/plain";
        }
        
        String extension = filename.toLowerCase();
        if (extension.endsWith(".pdf")) return "application/pdf";
        if (extension.endsWith(".json")) return "application/json";
        if (extension.endsWith(".xml")) return "application/xml";
        if (extension.endsWith(".jpg") || extension.endsWith(".jpeg")) return "image/jpeg";
        if (extension.endsWith(".png")) return "image/png";
        if (extension.endsWith(".gif")) return "image/gif";
        if (extension.endsWith(".mp4")) return "video/mp4";
        if (extension.endsWith(".mp3")) return "audio/mpeg";
        if (extension.endsWith(".zip")) return "application/zip";
        if (extension.endsWith(".doc")) return "application/msword";
        if (extension.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        
        return "text/plain"; // Default fallback
    }
    
    /**
     * Format file size in human-readable format
     * @param bytes File size in bytes
     * @return Human-readable file size string
     */
    public String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    /**
     * Get Password Registry statistics from Revolutionary Search Engine
     * @return Password registry statistics
     */
    public Object getPasswordRegistryStats() {
        System.out.println("🔍 Debug: getPasswordRegistryStats called");
        Object stats = blockchain.getUnifiedSearchAPI().getPasswordRegistryStats();
        System.out.println("🔍 Debug: UnifiedSearchAPI instance: " + blockchain.getUnifiedSearchAPI().getClass().getSimpleName() + "@" + Integer.toHexString(blockchain.getUnifiedSearchAPI().hashCode()));
        return stats;
    }
    
    /**
     * Search with adaptive decryption using Revolutionary Search Engine
     * Converts Enhanced Search Results to Block list for API compatibility
     * @param searchTerm The term to search for
     * @param password The password for decryption
     * @param maxResults Maximum number of results to return
     * @return List of matching blocks with decrypted content
     */
    public List<Block> searchWithAdaptiveDecryption(String searchTerm, String password, int maxResults) {
        try {
            List<EnhancedSearchResult> enhancedResults = blockchain.getUnifiedSearchAPI().searchIntelligent(searchTerm, password, maxResults);
            
            List<Block> blocks = new ArrayList<>();
            for (EnhancedSearchResult enhancedResult : enhancedResults) {
                try {
                    Block block = blockchain.getBlockByHash(enhancedResult.getBlockHash());
                    if (block != null) {
                        blocks.add(block);
                    }
                } catch (Exception e) {
                    System.err.println("Warning: Could not retrieve block " + enhancedResult.getBlockHash() + ": " + e.getMessage());
                }
            }
            
            return blocks;
        } catch (Exception e) {
            System.err.println("Search with adaptive decryption failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    // Removed category-specific search methods - users now search by their own defined terms
    
    /**
     * Convert Enhanced Search Results to Block objects
     * @param enhancedResults List of enhanced search results
     * @return List of Block objects
     */
    private List<Block> convertEnhancedResultsToBlocks(List<?> enhancedResults) {
        List<Block> blocks = new ArrayList<>();
        for (var enhancedResult : enhancedResults) {
            try {
                // Use reflection to get block hash since we don't know the exact type
                String blockHash = (String) enhancedResult.getClass().getMethod("getBlockHash").invoke(enhancedResult);
                Block block = blockchain.getBlockByHash(blockHash);
                if (block != null) {
                    blocks.add(block);
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not convert enhanced result to block: " + e.getMessage());
            }
        }
        return blocks;
    }
    
    // ===== FLEXIBLE USER-DEFINED SEARCH TERMS API =====
    
    /**
     * Store searchable data with user-defined search terms
     * All terms will be encrypted and stored in private metadata for maximum privacy
     * @param data The data to encrypt and store
     * @param password The password for encryption
     * @param searchTerms Array of user-defined search terms
     * @return The created block, or null if failed
     */
    public Block storeSearchableData(String data, String password, String[] searchTerms) {
        validateKeyPair();
        return blockchain.addEncryptedBlockWithKeywords(data, password, searchTerms, null, 
                                                       defaultKeyPair.getPrivate(), defaultKeyPair.getPublic());
    }
    
    /**
     * Store searchable data with explicit control over public vs private terms
     * Gives users full control over which terms are publicly searchable
     * @param data The data to encrypt and store
     * @param password The password for encryption
     * @param publicSearchTerms Terms visible without password (use carefully!)
     * @param privateSearchTerms Terms requiring password for search
     * @return The created block, or null if failed
     */
    public Block storeSearchableDataWithLayers(String data, String password, 
                                              String[] publicSearchTerms, String[] privateSearchTerms) {
        validateKeyPair();
        
        // Convert arrays to sets
        Set<String> publicTerms = publicSearchTerms != null ? 
            new HashSet<>(Arrays.asList(publicSearchTerms)) : new HashSet<>();
        Set<String> privateTerms = privateSearchTerms != null ? 
            new HashSet<>(Arrays.asList(privateSearchTerms)) : new HashSet<>();
        
        System.out.println("🔍 Debug: storeSearchableDataWithLayers:");
        System.out.println("🔍   Public terms: " + publicTerms.size() + " -> " + publicTerms);
        System.out.println("🔍   Private terms: " + privateTerms.size() + " -> " + privateTerms);
        
        // Use the blockchain method that actually handles metadata layers
        // For now, we'll use the basic encrypted storage and rely on the search engine
        // to handle the layer separation during search operations
        Set<String> allTerms = new HashSet<>();
        allTerms.addAll(publicTerms);
        allTerms.addAll(privateTerms);
        
        // Add public terms as special keywords that can be searched without password
        Set<String> publicKeywords = new HashSet<>();
        for (String publicTerm : publicTerms) {
            publicKeywords.add("PUBLIC:" + publicTerm);  // Mark public terms with prefix
        }
        
        // Add private terms as regular keywords
        Set<String> allKeywords = new HashSet<>();
        allKeywords.addAll(publicKeywords);  // Public terms with PREFIX
        allKeywords.addAll(privateTerms);    // Private terms without prefix
        
        System.out.println("🔍 Debug: Final keywords for storage: " + allKeywords);
        
        // Store the block with encryption
        Block block = blockchain.addEncryptedBlockWithKeywords(data, password, 
                                                              allKeywords.toArray(new String[0]), "USER_DEFINED", 
                                                              defaultKeyPair.getPrivate(), defaultKeyPair.getPublic());
        
        return block;
    }
    
    /**
     * Store searchable data with granular term visibility control
     * Allows precise control over which terms are publicly searchable vs encrypted
     * 
     * @param data The data content to store
     * @param password The encryption password
     * @param allSearchTerms All search terms for this data
     * @param termVisibility Map controlling visibility of each term
     * @return The created block, or null if failed
     */
    public Block storeDataWithGranularTermControl(String data, String password, 
                                                 Set<String> allSearchTerms, 
                                                 TermVisibilityMap termVisibility) {
        validateKeyPair();
        
        if (allSearchTerms == null || allSearchTerms.isEmpty()) {
            System.out.println("⚠️ Warning: No search terms provided for granular control");
            return storeSecret(data, password);
        }
        
        if (termVisibility == null) {
            System.out.println("⚠️ Warning: No term visibility map provided, using default PUBLIC");
            termVisibility = new TermVisibilityMap(TermVisibilityMap.VisibilityLevel.PUBLIC);
        }
        
        // Get terms for each layer according to visibility map
        Set<String> publicTerms = termVisibility.getPublicTerms(allSearchTerms);
        Set<String> privateTerms = termVisibility.getPrivateTerms(allSearchTerms);
        
        System.out.println("🔍 Debug: Granular term control:");
        System.out.println("🔍   Total terms: " + allSearchTerms.size());
        System.out.println("🔍   Public terms: " + publicTerms.size() + " -> " + publicTerms);
        System.out.println("🔍   Private terms: " + privateTerms.size() + " -> " + privateTerms);
        
        // Store using the existing method with distributed terms
        return storeSearchableDataWithLayers(data, password, 
                                            publicTerms.toArray(new String[0]), 
                                            privateTerms.toArray(new String[0]));
    }
    
    /**
     * Convenience method to create granular visibility for sensitive data
     * Common pattern: general terms public, specific identifiers private
     * 
     * @param data The data content to store
     * @param password The encryption password
     * @param publicTerms Terms that should be publicly searchable
     * @param privateTerms Terms that should only be searchable with password
     * @return The created block, or null if failed
     */
    public Block storeDataWithSeparatedTerms(String data, String password,
                                            String[] publicTerms, String[] privateTerms) {
        
        Set<String> allTerms = new HashSet<>();
        if (publicTerms != null) {
            allTerms.addAll(Arrays.asList(publicTerms));
        }
        if (privateTerms != null) {
            allTerms.addAll(Arrays.asList(privateTerms));
        }
        
        TermVisibilityMap visibility = new TermVisibilityMap(TermVisibilityMap.VisibilityLevel.PRIVATE)
            .setPublic(publicTerms != null ? publicTerms : new String[0])
            .setPrivate(privateTerms != null ? privateTerms : new String[0]);
        
        return storeDataWithGranularTermControl(data, password, allTerms, visibility);
    }
    
    /**
     * Store data with both user-defined terms and suggested terms
     * Combines explicit user terms with auto-suggested terms from content
     * @param data The data to encrypt and store
     * @param password The password for encryption
     * @param userSearchTerms Array of user-defined search terms
     * @param includeSuggestedTerms Whether to include auto-suggested terms from content
     * @return The created block, or null if failed
     */
    public Block storeDataWithMixedTerms(String data, String password, String[] userSearchTerms, boolean includeSuggestedTerms) {
        validateKeyPair();
        
        String[] finalTerms = userSearchTerms;
        
        if (includeSuggestedTerms && data != null && !data.trim().isEmpty()) {
            Set<String> suggestedTerms = extractSimpleSearchTerms(data, 5);
            if (!suggestedTerms.isEmpty()) {
                // Combine user terms with suggested terms
                Set<String> allTerms = new HashSet<>();
                if (userSearchTerms != null) {
                    for (String term : userSearchTerms) {
                        allTerms.add(term.toLowerCase().trim());
                    }
                }
                allTerms.addAll(suggestedTerms);
                finalTerms = allTerms.toArray(new String[0]);
            }
        }
        
        return blockchain.addEncryptedBlockWithKeywords(data, password, finalTerms, null, 
                                                       defaultKeyPair.getPrivate(), defaultKeyPair.getPublic());
    }
    
    /**
     * Search by explicit search terms
     * Replaces category-specific search with term-based search
     * @param searchTerms Array of terms to search for
     * @param password Password for decrypting search results
     * @param maxResults Maximum number of results to return
     * @return List of matching blocks
     */
    public List<Block> searchByTerms(String[] searchTerms, String password, int maxResults) {
        if (searchTerms == null || searchTerms.length == 0) {
            return new ArrayList<>();
        }
        
        List<Block> results = new ArrayList<>();
        
        for (String term : searchTerms) {
            if (term != null && !term.trim().isEmpty()) {
                String searchTerm = term.trim();
                
                // First, try to find blocks where this term is marked as public
                List<Block> publicResults = findBlocksWithPublicTerm(searchTerm);
                for (Block block : publicResults) {
                    if (!results.contains(block) && results.size() < maxResults) {
                        results.add(block);
                    }
                }
                
                // If we have a password, also search in encrypted keywords
                if (password != null && results.size() < maxResults) {
                    List<Block> encryptedResults = findBlocksWithPrivateTerm(searchTerm, password);
                    for (Block block : encryptedResults) {
                        if (!results.contains(block) && results.size() < maxResults) {
                            results.add(block);
                        }
                    }
                }
            }
        }
        
        // Limit results to maxResults
        if (results.size() > maxResults) {
            results = results.subList(0, maxResults);
        }
        
        return results;
    }
    
    /**
     * Search by terms with automatic decryption
     * @param searchTerms Array of terms to search for
     * @param password Password for decryption
     * @param maxResults Maximum number of results to return
     * @return List of matching decrypted blocks
     */
    public List<Block> searchAndDecryptByTerms(String[] searchTerms, String password, int maxResults) {
        List<Block> encryptedResults = searchByTerms(searchTerms, password, maxResults);
        List<Block> decryptedResults = new ArrayList<>();
        
        for (Block block : encryptedResults) {
            try {
                String decryptedData = blockchain.getDecryptedBlockData(block.getId(), password);
                if (decryptedData != null) {
                    // Create a copy of the block with decrypted data for display
                    Block decryptedBlock = createDecryptedCopy(block, decryptedData);
                    decryptedResults.add(decryptedBlock);
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not decrypt block " + block.getId() + ": " + e.getMessage());
                // Include the encrypted block anyway
                decryptedResults.add(block);
            }
        }
        
        return decryptedResults;
    }
    
    // Removed category-specific storage methods with hardcoded terms - use storeSearchableData() with user-defined terms instead
    
    /**
     * Create a copy of a block with decrypted data for display purposes
     * @param originalBlock The original encrypted block
     * @param decryptedData The decrypted data
     * @return Copy of block with decrypted data
     */
    private Block createDecryptedCopy(Block originalBlock, String decryptedData) {
        Block copy = new Block();
        copy.setId(originalBlock.getId());
        copy.setBlockNumber(originalBlock.getBlockNumber());
        copy.setPreviousHash(originalBlock.getPreviousHash());
        copy.setData(decryptedData); // Show decrypted data
        copy.setHash(originalBlock.getHash());
        copy.setTimestamp(originalBlock.getTimestamp());
        copy.setSignature(originalBlock.getSignature());
        copy.setSignerPublicKey(originalBlock.getSignerPublicKey());
        copy.setIsEncrypted(true); // Keep encryption flag
        copy.setEncryptionMetadata(originalBlock.getEncryptionMetadata());
        copy.setManualKeywords(originalBlock.getManualKeywords());
        copy.setAutoKeywords(originalBlock.getAutoKeywords());
        copy.setSearchableContent(originalBlock.getSearchableContent());
        copy.setContentCategory(originalBlock.getContentCategory());
        copy.setOffChainData(originalBlock.getOffChainData());
        return copy;
    }
    
    
    /**
     * Find blocks that have a specific term marked as public (searchable without password)
     * 
     * @param searchTerm The term to search for in public metadata
     * @return List of blocks where this term is publicly searchable
     */
    private List<Block> findBlocksWithPublicTerm(String searchTerm) {
        List<Block> publicResults = new ArrayList<>();
        
        try {
            // Get all blocks and check their visibility metadata
            List<Block> allBlocks = blockchain.getAllBlocks();
            
            for (Block block : allBlocks) {
                if (isTermPublicInBlock(block, searchTerm)) {
                    publicResults.add(block);
                }
            }
            
            System.out.println("🔍 Debug: findBlocksWithPublicTerm('" + searchTerm + "') found " + publicResults.size() + " public results");
            
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Failed to search public terms: " + e.getMessage());
        }
        
        return publicResults;
    }
    
    /**
     * Find blocks containing a specific term in their encrypted private keywords
     * 
     * @param searchTerm The term to search for
     * @param password The password to decrypt the keywords
     * @return List of blocks that contain the term in their private keywords
     */
    private List<Block> findBlocksWithPrivateTerm(String searchTerm, String password) {
        List<Block> privateResults = new ArrayList<>();
        
        try {
            // Get all blocks and check their encrypted keywords
            List<Block> allBlocks = blockchain.getAllBlocks();
            
            for (Block block : allBlocks) {
                if (isTermPrivateInBlock(block, searchTerm, password)) {
                    privateResults.add(block);
                }
            }
            
            System.out.println("🔍 Debug: findBlocksWithPrivateTerm('" + searchTerm + "') found " + privateResults.size() + " private results");
            
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Failed to search private terms: " + e.getMessage());
        }
        
        return privateResults;
    }
    
    /**
     * Check if a specific term is marked as public in a block's keywords
     * 
     * @param block The block to check
     * @param searchTerm The term to look for
     * @return true if the term is marked as public in this block
     */
    private boolean isTermPublicInBlock(Block block, String searchTerm) {
        try {
            // Check if the block has the public keyword marker
            String publicKeyword = "public:" + searchTerm.toLowerCase();
            
            // Check in manual keywords
            String manualKeywords = block.getManualKeywords();
            if (manualKeywords != null) {
                String[] keywords = manualKeywords.split("\\s+");
                for (String keyword : keywords) {
                    if (keyword.trim().toLowerCase().equals(publicKeyword)) {
                        System.out.println("🔍 Debug: Block #" + block.getBlockNumber() + " - term '" + searchTerm + "' is PUBLIC (found in manual keywords)");
                        return true;
                    }
                }
            }
            
            // Check in auto keywords
            String autoKeywords = block.getAutoKeywords();
            if (autoKeywords != null) {
                String[] keywords = autoKeywords.split("\\s+");
                for (String keyword : keywords) {
                    if (keyword.trim().toLowerCase().equals(publicKeyword)) {
                        System.out.println("🔍 Debug: Block #" + block.getBlockNumber() + " - term '" + searchTerm + "' is PUBLIC (found in auto keywords)");
                        return true;
                    }
                }
            }
            
            System.out.println("🔍 Debug: Block #" + block.getBlockNumber() + " - term '" + searchTerm + "' is NOT PUBLIC ('" + publicKeyword + "' not found)");
            System.out.println("🔍 Debug: manualKeywords = '" + manualKeywords + "'");
            System.out.println("🔍 Debug: autoKeywords = '" + autoKeywords + "'");
            return false;
            
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Failed to check public term in block #" + block.getBlockNumber() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if a specific term is present in a block's encrypted private keywords
     * 
     * @param block The block to check
     * @param searchTerm The term to look for
     * @param password The password to decrypt the keywords
     * @return true if the term is found in the encrypted private keywords
     */
    private boolean isTermPrivateInBlock(Block block, String searchTerm, String password) {
        try {
            // Check only in autoKeywords where private keywords are stored encrypted
            String autoKeywords = block.getAutoKeywords();
            if (autoKeywords != null && !autoKeywords.trim().isEmpty()) {
                if (checkEncryptedKeywordsForTerm(autoKeywords, searchTerm, password, block.getBlockNumber(), "autoKeywords")) {
                    return true;
                }
            }
            
            System.out.println("🔍 Debug: Block #" + block.getBlockNumber() + " - term '" + searchTerm + "' is NOT in private keywords");
            return false;
            
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Failed to check private term in block #" + block.getBlockNumber() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Helper method to check encrypted keywords for a specific term
     */
    private boolean checkEncryptedKeywordsForTerm(String encryptedText, String searchTerm, String password, Long blockNumber, String fieldName) {
        // Split by spaces to handle multiple encrypted chunks
        String[] encryptedChunks = encryptedText.split("\\s+");
        
        for (String chunk : encryptedChunks) {
            // Skip chunks that don't look like encrypted data
            if (!chunk.contains("|") || chunk.length() < 50) {
                continue;
            }
            
            try {
                // Try to decrypt this chunk
                String decryptedKeywords = SecureBlockEncryptionService.decryptFromString(chunk, password);
                
                if (decryptedKeywords != null) {
                    // Check if our search term is in the decrypted keywords
                    String[] keywords = decryptedKeywords.toLowerCase().split("\\s+");
                    for (String keyword : keywords) {
                        if (keyword.trim().equals(searchTerm.toLowerCase())) {
                            System.out.println("🔍 Debug: Block #" + blockNumber + " - term '" + searchTerm + "' is PRIVATE (found in encrypted " + fieldName + ")");
                            return true;
                        }
                    }
                }
            } catch (Exception decryptException) {
                // This chunk couldn't be decrypted with this password, skip it
                continue;
            }
        }
        return false;
    }
}