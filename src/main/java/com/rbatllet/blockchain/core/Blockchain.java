package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.dao.AuthorizedKeyDAO;
import com.rbatllet.blockchain.dao.BlockDAO;
import com.rbatllet.blockchain.dto.ChainExportData;
import com.rbatllet.blockchain.dto.EncryptionExportData;
import com.rbatllet.blockchain.entity.AuthorizedKey;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.entity.OffChainData;
import com.rbatllet.blockchain.service.OffChainStorageService;
import com.rbatllet.blockchain.service.SecureBlockEncryptionService;
import com.rbatllet.blockchain.recovery.ChainRecoveryManager;
import com.rbatllet.blockchain.recovery.ChainRecoveryManager.ChainDiagnostic;
import com.rbatllet.blockchain.recovery.ChainRecoveryManager.RecoveryResult;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.EncryptionExportUtil;
import com.rbatllet.blockchain.util.JPAUtil;
import com.rbatllet.blockchain.validation.BlockStatus;
import com.rbatllet.blockchain.validation.BlockValidationResult;
import com.rbatllet.blockchain.validation.ChainValidationResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import com.rbatllet.blockchain.search.RevolutionarySearchEngine;
import com.rbatllet.blockchain.search.RevolutionarySearchEngine.EnhancedSearchResult;
import com.rbatllet.blockchain.search.UnifiedRevolutionarySearchAPI;
import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.validation.EncryptedBlockValidator;
import com.rbatllet.blockchain.util.validation.BlockValidationUtil;

/**
 * Thread-safe Blockchain implementation
 * FIXED: Complete thread-safety with global locks and transaction management
 */
public class Blockchain {
    
    private final BlockDAO blockDAO;
    private final AuthorizedKeyDAO authorizedKeyDAO;
    private static final String GENESIS_PREVIOUS_HASH = "0";
    
    // Global lock for thread safety across multiple instances
    private static final ReentrantReadWriteLock GLOBAL_BLOCKCHAIN_LOCK = new ReentrantReadWriteLock();
    
    // CORE FUNCTION: Block size validation constants
    // FIXED: Added clear documentation about the relationship between limits
    private static final int MAX_BLOCK_SIZE_BYTES = 1024 * 1024; // 1MB max per block (for large binary data)
    private static final int MAX_BLOCK_DATA_LENGTH = 10000; // 10K characters max (for text content)
    // NOTE: Character limit is separate from byte limit to handle both text and binary data efficiently
    // 10K UTF-8 characters typically use ~10-40KB, well within the 1MB byte limit
    
    // Off-chain storage threshold - data larger than this will be stored off-chain
    private static final int OFF_CHAIN_THRESHOLD_BYTES = 512 * 1024; // 512KB threshold
    private final OffChainStorageService offChainStorageService = new OffChainStorageService();
    
    // Search functionality
    private final RevolutionarySearchEngine revolutionarySearchEngine;
    private final UnifiedRevolutionarySearchAPI unifiedSearchAPI;
    
    // Dynamic configuration for block size limits
    private volatile int currentMaxBlockSizeBytes = MAX_BLOCK_SIZE_BYTES;
    private volatile int currentMaxBlockDataLength = MAX_BLOCK_DATA_LENGTH;
    private volatile int currentOffChainThresholdBytes = OFF_CHAIN_THRESHOLD_BYTES;
    
    public Blockchain() {
        this.blockDAO = new BlockDAO();
        this.authorizedKeyDAO = new AuthorizedKeyDAO();
        
        // Initialize Revolutionary Search Engine with high security configuration
        EncryptionConfig searchConfig = EncryptionConfig.createHighSecurityConfig();
        this.revolutionarySearchEngine = new RevolutionarySearchEngine(searchConfig);
        this.unifiedSearchAPI = new UnifiedRevolutionarySearchAPI(searchConfig);
        
        initializeGenesisBlock();
        
        // Note: Revolutionary Search is initialized on-demand when blocks are created
        // This prevents conflicts with per-block password management
        // initializeRevolutionarySearch();
    }
    
    /**
     * Creates the genesis block if it doesn't exist
     * FIXED: Added thread-safety with proper transaction management and sequence synchronization
     */
    private void initializeGenesisBlock() {
        GLOBAL_BLOCKCHAIN_LOCK.writeLock().lock();
        try {
            // Use global transaction for consistency
            JPAUtil.executeInTransaction(em -> {
                initializeGenesisBlockInternal(em);
                return null;
            });
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.writeLock().unlock();
        }
    }
    
    /**
     * Internal method to create genesis block without transaction/lock management
     * Used when already inside a transaction (e.g., during clearAndReinitialize)
     */
    private void initializeGenesisBlockInternal(EntityManager em) {
        if (blockDAO.getBlockCount() == 0) {
            LocalDateTime genesisTime = LocalDateTime.now();
            
            Block genesisBlock = new Block();
            genesisBlock.setBlockNumber(0L);
            genesisBlock.setPreviousHash(GENESIS_PREVIOUS_HASH);
            genesisBlock.setData("Genesis Block - Private Blockchain Initialized");
            genesisBlock.setTimestamp(genesisTime);
            
            String blockContent = buildBlockContent(genesisBlock);
            genesisBlock.setHash(CryptoUtil.calculateHash(blockContent));
            genesisBlock.setSignature("GENESIS");
            genesisBlock.setSignerPublicKey("GENESIS");
            
            blockDAO.saveBlock(genesisBlock);
            
            // CRITICAL: Synchronize the block sequence after creating genesis block
            blockDAO.synchronizeBlockSequence();
            
            System.out.println("Genesis block created successfully!");
        }
    }
    
    /**
     * Initialize Revolutionary Search Engine with existing blockchain data
     */
    public void initializeRevolutionarySearch() {
        try {
            // Only initialize if there are blocks to index
            if (blockDAO.getBlockCount() > 0) {
                // Initialize without password - will index public metadata only
                // Individual blocks with passwords can be indexed later via reindexBlockWithPassword()
                KeyPair tempKeyPair = CryptoUtil.generateKeyPair();
                
                // Index the current blockchain state with public metadata only
                // The enhanced system will handle per-block password management automatically
                unifiedSearchAPI.initializeWithBlockchain(this, null, tempKeyPair.getPrivate());
                
                System.out.println("Revolutionary Search Engine initialized with " + blockDAO.getBlockCount() + " blocks");
                System.out.println("Password registry stats: " + unifiedSearchAPI.getPasswordRegistryStats());
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to initialize Revolutionary Search Engine: " + e.getMessage());
            e.printStackTrace();
            // Continue operation even if search initialization fails
        }
    }
    
    /**
     * Re-index a specific block with its correct password for better search capabilities
     * This allows the Revolutionary Search Engine to index encrypted content properly
     * Enhanced to use the new password registry system
     */
    public void reindexBlockWithPassword(Long blockNumber, String password) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            Block block = blockDAO.getBlockByNumber(blockNumber);
            if (block != null && block.isDataEncrypted()) {
                // Generate a temporary key pair for indexing
                KeyPair tempKeyPair = CryptoUtil.generateKeyPair();
                
                // Use the enhanced UnifiedSearchAPI for better password management
                unifiedSearchAPI.addBlock(
                    block, password, tempKeyPair.getPrivate());
                
                System.out.println("Re-indexed encrypted block #" + blockNumber + " with specific password");
            }
        } catch (Exception e) {
            System.err.println("Failed to re-index block #" + blockNumber + ": " + e.getMessage());
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Convert Revolutionary Search results to traditional Block list
     * @param enhancedResults List of EnhancedSearchResult from Revolutionary Search
     * @return List of Block entities
     */
    private List<Block> convertEnhancedResultsToBlocks(List<EnhancedSearchResult> enhancedResults) {
        List<Block> blocks = new ArrayList<>();
        for (EnhancedSearchResult result : enhancedResults) {
            try {
                Block block = blockDAO.getBlockByHash(result.getBlockHash());
                if (block != null) {
                    blocks.add(block);
                }
            } catch (Exception e) {
                System.err.println("Warning: Failed to retrieve block for hash " + result.getBlockHash() + ": " + e.getMessage());
            }
        }
        return blocks;
    }
    
    /**
     * CORE FUNCTION: Add a new block to the chain and return the created block
     * THREAD-SAFE: Returns the actual block created to avoid race conditions in tests
     */
    public Block addBlockAndReturn(String data, PrivateKey signerPrivateKey, PublicKey signerPublicKey) {
        return addBlockWithKeywords(data, null, null, signerPrivateKey, signerPublicKey);
    }
    
    /**
     * ENHANCED: Add a new block with keywords and category
     * THREAD-SAFE: Enhanced version with search functionality
     */
    public Block addBlockWithKeywords(String data, String[] manualKeywords, String category,
                                     PrivateKey signerPrivateKey, PublicKey signerPublicKey) {
        GLOBAL_BLOCKCHAIN_LOCK.writeLock().lock();
        try {
            return JPAUtil.executeInTransaction(em -> {
                try {
                    // 0. Validate input parameters (but allow null data)
                    if (signerPrivateKey == null) {
                        System.err.println("Signer private key cannot be null");
                        return null;
                    }
                    if (signerPublicKey == null) {
                        System.err.println("Signer public key cannot be null");
                        return null;
                    }
                    
                    // 1. CORE: Validate block size and determine storage strategy
                    int storageDecision = validateAndDetermineStorage(data);
                    if (storageDecision == 0) {
                        System.err.println("Block data validation failed");
                        return null;
                    }
                    
                    // 2. Verify that the key is authorized at the time of block creation
                    String publicKeyString = CryptoUtil.publicKeyToString(signerPublicKey);
                    LocalDateTime blockTimestamp = LocalDateTime.now();
                    if (!authorizedKeyDAO.wasKeyAuthorizedAt(publicKeyString, blockTimestamp)) {
                        System.err.println("Unauthorized key attempting to add block");
                        return null;
                    }
                    
                    // 3. Get the next block number atomically to prevent race conditions
                    Long nextBlockNumber = blockDAO.getNextBlockNumberAtomic();
                    
                    // 4. Get the last block for previous hash
                    Block lastBlock = blockDAO.getLastBlockWithLock();
                    if (lastBlock == null && nextBlockNumber != 0L) {
                        System.err.println("Inconsistent state: no genesis block but number is " + nextBlockNumber);
                        return null;
                    }
                    
                    // 5. Handle off-chain storage if needed
                    OffChainData offChainData = null;
                    String blockData = data;
                    
                    if (storageDecision == 2) { // Store off-chain
                        try {
                            // Generate a password for encryption (derived from block info)
                            String encryptionPassword = generateOffChainPassword(nextBlockNumber, publicKeyString);
                            
                            // Store data off-chain with encryption and verification
                            offChainData = offChainStorageService.storeData(
                                data.getBytes(StandardCharsets.UTF_8),
                                encryptionPassword,
                                signerPrivateKey,
                                publicKeyString,
                                "text/plain"
                            );
                            
                            // Replace block data with reference
                            blockData = "OFF_CHAIN_REF:" + offChainData.getDataHash();
                            System.out.println("Data stored off-chain. Block contains reference: " + blockData);
                            
                        } catch (Exception e) {
                            System.err.println("Failed to store data off-chain: " + e.getMessage());
                            return null;
                        }
                    }
                    
                    // 6. Create the new block with the atomically generated number
                    Block newBlock = new Block();
                    newBlock.setBlockNumber(nextBlockNumber);
                    newBlock.setPreviousHash(lastBlock != null ? lastBlock.getHash() : GENESIS_PREVIOUS_HASH);
                    newBlock.setData(blockData);
                    newBlock.setTimestamp(blockTimestamp);
                    newBlock.setSignerPublicKey(publicKeyString);
                    newBlock.setOffChainData(offChainData);
                    
                    // 7. Calculate block hash
                    String blockContent = buildBlockContent(newBlock);
                    newBlock.setHash(CryptoUtil.calculateHash(blockContent));
                    
                    // 8. Sign the block
                    String signature = CryptoUtil.signData(blockContent, signerPrivateKey);
                    newBlock.setSignature(signature);
                    
                    // 9. Validate the block before saving
                    if (lastBlock != null && !validateBlock(newBlock, lastBlock)) {
                        System.err.println("Block validation failed");
                        return null;
                    }
                    
                    // 10. Final check: verify this block number doesn't exist
                    if (blockDAO.existsBlockWithNumber(nextBlockNumber)) {
                        System.err.println("🚨 CRITICAL: Race condition detected! Block number " + nextBlockNumber + " already exists");
                        return null;
                    }
                    
                    // 11. ENHANCED: Process keywords for search functionality
                    processBlockKeywords(newBlock, data, manualKeywords, category);
                    
                    // 12. Save the block
                    blockDAO.saveBlock(newBlock);
                    
                    // CRITICAL: Force flush to ensure immediate visibility
                    if (JPAUtil.hasActiveTransaction()) {
                        EntityManager currentEm = JPAUtil.getEntityManager();
                        currentEm.flush();
                    }
                    
                    // 13. Index block in Revolutionary Search Engine for immediate searchability
                    try {
                        // Re-index the entire blockchain when a new block is added
                        // This ensures all search capabilities are up-to-date
                        Blockchain currentBlockchain = this;
                        String defaultPassword = "search-index-password"; // Could be configurable
                        revolutionarySearchEngine.indexBlockchain(currentBlockchain, defaultPassword, signerPrivateKey);
                    } catch (Exception searchIndexException) {
                        System.err.println("Warning: Failed to index block in Revolutionary Search: " + 
                                         searchIndexException.getMessage());
                        // Don't fail the block creation if search indexing fails
                    }
                    
                    System.out.println("Block #" + newBlock.getBlockNumber() + " added successfully!");
                    return newBlock; // ✅ RETURN THE ACTUAL CREATED BLOCK
                    
                } catch (Exception e) {
                    System.err.println("Error adding block: " + e.getMessage());
                    return null;
                }
            });
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.writeLock().unlock();
        }
    }

    /**
     * CORE FUNCTION: Add a new block to the chain (with size validation)
     * FIXED: Complete thread-safety with atomic block number generation
     * RACE CONDITION FIX: Now uses getNextBlockNumberAtomic() to prevent duplicate block numbers
     * under high-concurrency scenarios (30+ simultaneous threads)
     */
    public boolean addBlock(String data, PrivateKey signerPrivateKey, PublicKey signerPublicKey) {
        // Simplify by delegating to addBlockAndReturn for DRY principle
        Block addedBlock = addBlockAndReturn(data, signerPrivateKey, signerPublicKey);
        return addedBlock != null;
    }
    
    /**
     * Add an encrypted block to the chain
     * The data will be encrypted using enterprise-grade AES-256-GCM encryption
     * 
     * @param data The sensitive data to encrypt and store
     * @param encryptionPassword The password for encryption
     * @param signerPrivateKey Private key for signing
     * @param signerPublicKey Public key for verification
     * @return The created encrypted block, or null if failed
     */
    public Block addEncryptedBlock(String data, String encryptionPassword, 
                                 PrivateKey signerPrivateKey, PublicKey signerPublicKey) {
        return addEncryptedBlockWithKeywords(data, encryptionPassword, null, "USER_DEFINED", 
                                           signerPrivateKey, signerPublicKey);
    }
    
    /**
     * Add an encrypted block with keywords and category
     * 
     * @param data The sensitive data to encrypt and store
     * @param encryptionPassword The password for encryption
     * @param manualKeywords Manual keywords for search (will also be encrypted)
     * @param category Content category (e.g., "MEDICAL", "FINANCIAL", "LEGAL")
     * @param signerPrivateKey Private key for signing
     * @param signerPublicKey Public key for verification
     * @return The created encrypted block, or null if failed
     */
    public Block addEncryptedBlockWithKeywords(String data, String encryptionPassword,
                                             String[] manualKeywords, String category,
                                             PrivateKey signerPrivateKey, PublicKey signerPublicKey) {
        
        if (encryptionPassword == null || encryptionPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Encryption password cannot be null or empty");
        }
        
        GLOBAL_BLOCKCHAIN_LOCK.writeLock().lock();
        try {
            return JPAUtil.executeInTransaction(em -> {
                try {
                    // 1. Validate input
                    validateBlockInput(data, signerPrivateKey, signerPublicKey);
                    
                    // 2. Check authorized keys
                    String publicKeyString = CryptoUtil.publicKeyToString(signerPublicKey);
                    LocalDateTime blockTimestamp = LocalDateTime.now();
                    if (!authorizedKeyDAO.wasKeyAuthorizedAt(publicKeyString, blockTimestamp)) {
                        System.err.println("Unauthorized public key");
                        return null;
                    }
                    
                    // 3. Encrypt the data BEFORE size validation
                    String encryptedData;
                    try {
                        encryptedData = SecureBlockEncryptionService.encryptToString(data, encryptionPassword);
                    } catch (Exception e) {
                        System.err.println("Failed to encrypt block data: " + e.getMessage());
                        return null;
                    }
                    
                    // 4. Validate encrypted data size
                    if (!validateDataSize(encryptedData)) {
                        System.err.println("Encrypted data exceeds maximum block size limits");
                        return null;
                    }
                    
                    // 5. Get last block and generate next block number atomically
                    Block lastBlock = blockDAO.getLastBlockWithLock();
                    Long nextBlockNumber = blockDAO.getNextBlockNumberAtomic();
                    
                    // 6. Handle off-chain storage if needed (for encrypted data)
                    OffChainData offChainData = null;
                    String blockData = encryptedData;
                    
                    if (shouldStoreOffChain(encryptedData)) {
                        try {
                            // Store encrypted data off-chain with additional password protection
                            offChainData = offChainStorageService.storeData(
                                encryptedData.getBytes(StandardCharsets.UTF_8),
                                encryptionPassword + "_OFFCHAIN", // Additional security layer
                                signerPrivateKey,
                                publicKeyString,
                                "application/encrypted"
                            );
                            
                            blockData = "OFF_CHAIN_ENCRYPTED_REF:" + offChainData.getDataHash();
                            System.out.println("Encrypted data stored off-chain. Block contains reference: " + blockData);
                            
                        } catch (Exception e) {
                            System.err.println("Failed to store encrypted data off-chain: " + e.getMessage());
                            return null;
                        }
                    }
                    
                    // 7. Create the new encrypted block
                    Block newBlock = new Block();
                    newBlock.setBlockNumber(nextBlockNumber);
                    newBlock.setPreviousHash(lastBlock != null ? lastBlock.getHash() : GENESIS_PREVIOUS_HASH);
                    newBlock.setData("[ENCRYPTED]"); // Placeholder for display
                    newBlock.setEncryptionMetadata(blockData); // Store encrypted data here
                    newBlock.setIsEncrypted(true); // Mark as encrypted
                    newBlock.setTimestamp(blockTimestamp);
                    newBlock.setSignerPublicKey(publicKeyString);
                    newBlock.setOffChainData(offChainData);
                    
                    // 8. Calculate block hash (using placeholder data for consistent hashing)
                    String blockContent = buildBlockContentForEncrypted(newBlock);
                    newBlock.setHash(CryptoUtil.calculateHash(blockContent));
                    
                    // 9. Sign the block
                    String signature = CryptoUtil.signData(blockContent, signerPrivateKey);
                    newBlock.setSignature(signature);
                    
                    // 10. Validate the block before saving
                    if (lastBlock != null && !validateBlock(newBlock, lastBlock)) {
                        System.err.println("Encrypted block validation failed");
                        return null;
                    }
                    
                    // 11. Process keywords for search (encrypt keywords too for privacy)
                    processEncryptedBlockKeywords(newBlock, data, manualKeywords, category, encryptionPassword);
                    
                    // 12. Save the encrypted block
                    blockDAO.saveBlock(newBlock);
                    
                    // Force flush for immediate visibility
                    if (JPAUtil.hasActiveTransaction()) {
                        EntityManager currentEm = JPAUtil.getEntityManager();
                        currentEm.flush();
                    }
                    
                    // 13. Index the block in Revolutionary Search Engine with its specific password
                    try {
                        // Use the enhanced UnifiedSearchAPI for better password management
                        unifiedSearchAPI.addBlock(
                            newBlock, encryptionPassword, signerPrivateKey);
                        
                        System.out.println("Successfully indexed encrypted block in Revolutionary Search Engine");
                    } catch (Exception e) {
                        System.err.println("Warning: Failed to index encrypted block in search engine: " + e.getMessage());
                        // Try fallback indexing with public metadata only
                        try {
                            revolutionarySearchEngine.indexBlockWithSpecificPassword(
                                newBlock, null, signerPrivateKey, 
                                EncryptionConfig.createHighSecurityConfig());
                            System.out.println("Fallback: Indexed block with public metadata only");
                        } catch (Exception e2) {
                            System.err.println("Complete indexing failure for block: " + e2.getMessage());
                        }
                    }
                    
                    System.out.println("🔐 Encrypted Block #" + newBlock.getBlockNumber() + " added successfully!");
                    return newBlock;
                    
                } catch (Exception e) {
                    System.err.println("Error adding encrypted block: " + e.getMessage());
                    return null;
                }
            });
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.writeLock().unlock();
        }
    }
    
    /**
     * Decrypt and retrieve block data
     * 
     * @param blockId The ID of the encrypted block
     * @param decryptionPassword The password for decryption
     * @return The decrypted block data, or null if failed
     */
    public String getDecryptedBlockData(Long blockId, String decryptionPassword) {
        if (blockId == null || decryptionPassword == null || decryptionPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Block ID and decryption password cannot be null or empty");
        }
        
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            Block block = blockDAO.getBlockWithDecryption(blockId, decryptionPassword);
            return block != null ? block.getData() : null;
        } catch (Exception e) {
            System.err.println("Failed to decrypt block data: " + e.getMessage());
            return null;
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Check if a block is encrypted
     */
    public boolean isBlockEncrypted(Long blockNumber) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            Block block = blockDAO.getBlockByNumber(blockNumber);
            return block != null && block.isDataEncrypted();
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Build block content for encrypted blocks (for consistent hashing)
     */
    private String buildBlockContentForEncrypted(Block block) {
        // Use epoch seconds for timestamp to ensure consistency with regular blocks
        long timestampSeconds = block.getTimestamp() != null ? 
            block.getTimestamp().toEpochSecond(ZoneOffset.UTC) : 0;
            
        return block.getBlockNumber() + 
               (block.getPreviousHash() != null ? block.getPreviousHash() : "") + 
               "[ENCRYPTED]" + // Use placeholder for consistent hashing 
               timestampSeconds + 
               (block.getSignerPublicKey() != null ? block.getSignerPublicKey() : "");
    }
    
    /**
     * Process keywords for encrypted blocks (encrypt keywords for privacy)
     */
    private void processEncryptedBlockKeywords(Block block, String originalData, 
                                             String[] manualKeywords, String category, 
                                             String encryptionPassword) {
        try {
            // 1. Manual keywords - separate public and private keywords
            if (manualKeywords != null && manualKeywords.length > 0) {
                List<String> publicKeywords = new ArrayList<>();
                List<String> privateKeywords = new ArrayList<>();
                
                // Separate keywords by PUBLIC: prefix
                for (String keyword : manualKeywords) {
                    if (keyword.startsWith("PUBLIC:")) {
                        publicKeywords.add(keyword.toLowerCase());
                    } else {
                        privateKeywords.add(keyword.toLowerCase());
                    }
                }
                
                // Store public keywords unencrypted in manualKeywords for search without password
                if (!publicKeywords.isEmpty()) {
                    String publicKeywordString = String.join(" ", publicKeywords);
                    block.setManualKeywords(publicKeywordString);
                    System.out.println("🔍 Debug: Stored public keywords: " + publicKeywordString);
                }
                
                // Store private keywords encrypted in autoKeywords field
                if (!privateKeywords.isEmpty()) {
                    String privateKeywordString = String.join(" ", privateKeywords);
                    String encryptedPrivateKeywords = SecureBlockEncryptionService.encryptToString(privateKeywordString, encryptionPassword);
                    block.setAutoKeywords(encryptedPrivateKeywords);
                    System.out.println("🔍 Debug: Stored encrypted private keywords in autoKeywords");
                }
            }
            
            // 2. Category (can be unencrypted for classification, or encrypted for privacy)
            if (category != null && !category.trim().isEmpty()) {
                // Store category unencrypted for classification purposes
                block.setContentCategory(category.toUpperCase());
            } else {
                // No category specified - using user-defined search terms system
                block.setContentCategory("USER_DEFINED");
            }
            
            // 3. Auto keywords (encrypted) - suggest terms from original data before encryption
            Set<String> suggestedTerms = extractSimpleSearchTerms(originalData, 10);
            if (!suggestedTerms.isEmpty()) {
                String autoKeywords = String.join(" ", suggestedTerms);
                String encryptedAutoKeywords = SecureBlockEncryptionService.encryptToString(autoKeywords, encryptionPassword);
                // If we already have auto keywords (from private keywords above), append
                String existingAutoKeywords = block.getAutoKeywords();
                if (existingAutoKeywords != null && !existingAutoKeywords.trim().isEmpty()) {
                    block.setAutoKeywords(existingAutoKeywords + " " + encryptedAutoKeywords);
                } else {
                    block.setAutoKeywords(encryptedAutoKeywords);
                }
            }
            
            // 4. Searchable content - leave empty for encrypted blocks to maintain privacy
            // Search on encrypted blocks would require decryption first
            block.setSearchableContent("");
            
        } catch (Exception e) {
            System.err.println("Failed to process encrypted keywords: " + e.getMessage());
            // Set empty values if encryption fails
            block.setManualKeywords("");
            block.setAutoKeywords("");
            block.setSearchableContent("");
        }
    }
    
    /**
     * Process and assign keywords to a block for search functionality
     */
    private void processBlockKeywords(Block block, String originalData, 
                                    String[] manualKeywords, String category) {
        // 1. Keywords manuals
        if (manualKeywords != null && manualKeywords.length > 0) {
            block.setManualKeywords(String.join(" ", manualKeywords).toLowerCase());
        }
        
        // 2. Categoria
        if (category != null && !category.trim().isEmpty()) {
            block.setContentCategory(category.toUpperCase());
        }
        
        // 3. Automatic keywords (from original content, not from off-chain reference)
        String contentForExtraction = originalData != null ? originalData : "";
        if (block.hasOffChainData()) {
            // If going off-chain, use original content for extraction
            contentForExtraction = originalData != null ? originalData : "";
        }
        
        Set<String> suggestedTerms = extractSimpleSearchTerms(contentForExtraction, 10);
        String autoKeywords = String.join(" ", suggestedTerms);
        block.setAutoKeywords(autoKeywords);
        
        // 4. Combine everything to searchableContent
        block.updateSearchableContent();
        
        // 5. Log for debugging
        if (block.getSearchableContent() != null && !block.getSearchableContent().trim().isEmpty()) {
            System.out.println("📋 Keywords assigned to block #" + block.getBlockNumber() + ": " + 
                             block.getSearchableContent().substring(0, Math.min(50, block.getSearchableContent().length())) + 
                             (block.getSearchableContent().length() > 50 ? "..." : ""));
        }
    }
    /**
     * CORE FUNCTION: Validate an individual block
     */
    /**
     * Valida un bloque individual contra el bloque anterior
     * ENHANCED: Added detailed logging to detect integrity failures
     */
    public boolean validateBlock(Block block, Block previousBlock) {
        String threadName = Thread.currentThread().getName();
        try {
            System.out.println("🔍 [" + threadName + "] Validating block #" + block.getBlockNumber());
            
            // Skip validation for genesis block
            if (block.getBlockNumber() != null && block.getBlockNumber().equals(0L)) {
                System.out.println("✅ [" + threadName + "] Genesis block validation passed");
                return true;
            }
            
            // 1. Verify that the previous block hash matches
            if (!block.getPreviousHash().equals(previousBlock.getHash())) {
                System.err.println("❌ [" + threadName + "] VALIDATION FAILURE: Previous hash mismatch");
                System.err.println("🔍 Expected: " + previousBlock.getHash());
                System.err.println("🔍 Got: " + block.getPreviousHash());
                return false;
            }
            
            // 2. Verify that the block number is sequential
            if (!block.getBlockNumber().equals(previousBlock.getBlockNumber() + 1L)) {
                System.err.println("❌ [" + threadName + "] VALIDATION FAILURE: Block number sequence error");
                System.err.println("🔍 Previous block number: " + previousBlock.getBlockNumber());
                System.err.println("🔍 Current block number: " + block.getBlockNumber());
                System.err.println("🔍 Expected: " + (previousBlock.getBlockNumber() + 1L));
                return false;
            }
            
            // 3. Verify hash integrity
            String calculatedHash = CryptoUtil.calculateHash(buildBlockContent(block));
            if (!block.getHash().equals(calculatedHash)) {
                System.err.println("❌ [" + threadName + "] VALIDATION FAILURE: Block hash integrity check failed for block #" + block.getBlockNumber());
                System.err.println("🔍 Stored hash: " + block.getHash());
                System.err.println("🔍 Calculated hash: " + calculatedHash);
                System.err.println("🔍 Block content: " + buildBlockContent(block));
                return false;
            }
            
            // 4. Verify digital signature
            PublicKey signerPublicKey = CryptoUtil.stringToPublicKey(block.getSignerPublicKey());
            String blockContent = buildBlockContent(block);
            if (!CryptoUtil.verifySignature(blockContent, block.getSignature(), signerPublicKey)) {
                System.err.println("❌ [" + threadName + "] VALIDATION FAILURE: Block signature verification failed for block #" + block.getBlockNumber());
                System.err.println("🔍 Block content: " + blockContent);
                System.err.println("🔍 Signature: " + block.getSignature());
                System.err.println("🔍 Signer public key: " + block.getSignerPublicKey());
                return false;
            }
            
            // 5. Verify that the key was authorized at the time of block creation
            if (!authorizedKeyDAO.wasKeyAuthorizedAt(block.getSignerPublicKey(), block.getTimestamp())) {
                System.err.println("❌ [" + threadName + "] VALIDATION FAILURE: Block signed by key that was not authorized at time of creation for block #" + block.getBlockNumber());
                System.err.println("🔍 Signer: " + block.getSignerPublicKey());
                System.err.println("🔍 Block timestamp: " + block.getTimestamp());
                return false;
            }
            
            System.out.println("✅ [" + threadName + "] Block #" + block.getBlockNumber() + " validation passed");
            return true;
            
        } catch (Exception e) {
            System.err.println("💥 [" + threadName + "] EXCEPTION during block validation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * ENHANCED: Detailed validation of an individual block
     * Returns comprehensive information about validation status
     */
    public BlockValidationResult validateBlockDetailed(Block block, Block previousBlock) {
        String threadName = Thread.currentThread().getName();
        BlockValidationResult.Builder builder = new BlockValidationResult.Builder(block);
        
        try {
            System.out.println("🔍 [" + threadName + "] Detailed validation of block #" + block.getBlockNumber());
            
            // Skip detailed validation for genesis block
            if (block.getBlockNumber() != null && block.getBlockNumber().equals(0L)) {
                System.out.println("✅ [" + threadName + "] Genesis block validation passed");
                return builder
                    .structurallyValid(true)
                    .cryptographicallyValid(true)
                    .authorizationValid(true)
                    .status(BlockStatus.VALID)
                    .build();
            }
            
            boolean structurallyValid = true;
            boolean cryptographicallyValid = true;
            boolean authorizationValid = true;
            boolean offChainDataValid = true;
            String errorMessage = null;
            String warningMessage = null;
            
            // 1. Verify that the previous block hash matches
            if (!block.getPreviousHash().equals(previousBlock.getHash())) {
                structurallyValid = false;
                errorMessage = "Previous hash mismatch";
                System.err.println("❌ [" + threadName + "] VALIDATION FAILURE: " + errorMessage);
            }
            
            // 2. Verify that the block number is sequential
            if (structurallyValid && !block.getBlockNumber().equals(previousBlock.getBlockNumber() + 1L)) {
                structurallyValid = false;
                errorMessage = "Block number sequence error";
                System.err.println("❌ [" + threadName + "] VALIDATION FAILURE: " + errorMessage);
            }
            
            // 3. Verify hash integrity
            if (structurallyValid) {
                String calculatedHash = CryptoUtil.calculateHash(buildBlockContent(block));
                if (!block.getHash().equals(calculatedHash)) {
                    cryptographicallyValid = false;
                    errorMessage = "Block hash integrity check failed";
                    System.err.println("❌ [" + threadName + "] VALIDATION FAILURE: " + errorMessage);
                }
            }
            
            // 4. Verify digital signature
            if (cryptographicallyValid) {
                try {
                    PublicKey signerPublicKey = CryptoUtil.stringToPublicKey(block.getSignerPublicKey());
                    String blockContent = buildBlockContent(block);
                    if (!CryptoUtil.verifySignature(blockContent, block.getSignature(), signerPublicKey)) {
                        cryptographicallyValid = false;
                        errorMessage = "Block signature verification failed";
                        System.err.println("❌ [" + threadName + "] VALIDATION FAILURE: " + errorMessage);
                    }
                } catch (Exception e) {
                    cryptographicallyValid = false;
                    errorMessage = "Signature verification error: " + e.getMessage();
                    System.err.println("❌ [" + threadName + "] VALIDATION FAILURE: " + errorMessage);
                }
            }
            
            // 5. Verify that the key was authorized at the time of block creation
            if (structurallyValid && cryptographicallyValid) {
                if (!authorizedKeyDAO.wasKeyAuthorizedAt(block.getSignerPublicKey(), block.getTimestamp())) {
                    authorizationValid = false;
                    warningMessage = "Block signed by key that was revoked after creation";
                    System.err.println("⚠️ [" + threadName + "] AUTHORIZATION WARNING: " + warningMessage);
                    System.err.println("🔍 Signer: " + block.getSignerPublicKey());
                    System.err.println("🔍 Block timestamp: " + block.getTimestamp());
                }
            }
            
            // 6. Verify off-chain data integrity if present
            if (structurallyValid && cryptographicallyValid && block.hasOffChainData()) {
                try {
                    // Perform detailed off-chain validation
                    var detailedResult = BlockValidationUtil.validateOffChainDataDetailed(block);
                    boolean basicOffChainIntegrity = verifyOffChainIntegrity(block);
                    
                    if (!detailedResult.isValid() || !basicOffChainIntegrity) {
                        offChainDataValid = false;
                        String detailedMessage = detailedResult.getMessage();
                        if (errorMessage == null) {
                            errorMessage = "Off-chain data validation failed: " + detailedMessage;
                        } else {
                            errorMessage += "; Off-chain data validation failed: " + detailedMessage;
                        }
                        System.err.println("❌ [" + threadName + "] OFF-CHAIN VALIDATION FAILURE for block #" + block.getBlockNumber() + ":");
                        System.err.println("   📋 Details: " + detailedMessage);
                        if (!basicOffChainIntegrity) {
                            System.err.println("   🔐 Cryptographic integrity check also failed");
                        }
                    } else {
                        // Show detailed success information
                        var offChainData = block.getOffChainData();
                        System.out.println("✅ [" + threadName + "] Off-chain data fully validated for block #" + block.getBlockNumber());
                        System.out.println("   📁 File: " + java.nio.file.Paths.get(offChainData.getFilePath()).getFileName());
                        System.out.println("   📦 Size: " + (offChainData.getFileSize() != null ? 
                            String.format("%.1f KB", offChainData.getFileSize() / 1024.0) : "unknown"));
                        System.out.println("   🔐 Integrity: verified (hash + encryption + signature)");
                        System.out.println("   ⏰ Created: " + (offChainData.getCreatedAt() != null ? 
                            offChainData.getCreatedAt().toString() : "unknown"));
                        
                        // Additional validation details
                        String hash = offChainData.getDataHash();
                        if (hash != null && hash.length() > 16) {
                            String truncatedHash = hash.substring(0, 8) + "..." + hash.substring(hash.length() - 8);
                            System.out.println("   🔗 Hash: " + truncatedHash);
                        }
                    }
                } catch (Exception e) {
                    offChainDataValid = false;
                    if (errorMessage == null) {
                        errorMessage = "Off-chain data verification error: " + e.getMessage();
                    } else {
                        errorMessage += "; Off-chain data verification error: " + e.getMessage();
                    }
                    System.err.println("❌ [" + threadName + "] OFF-CHAIN VALIDATION ERROR for block #" + block.getBlockNumber() + ": " + e.getMessage());
                }
            } else if (block.hasOffChainData() && (!structurallyValid || !cryptographicallyValid)) {
                // Block has off-chain data but failed basic validation
                System.out.println("⚠️ [" + threadName + "] Block #" + block.getBlockNumber() + " has off-chain data but failed basic validation - skipping off-chain checks");
            }
            
            // 7. Verify encrypted data integrity if present
            boolean encryptedDataValid = true;
            if (structurallyValid && cryptographicallyValid && block.isDataEncrypted()) {
                try {
                    // Perform comprehensive encrypted block validation
                    var encryptedValidation = EncryptedBlockValidator.validateEncryptedBlock(block);
                    
                    if (!encryptedValidation.isValid()) {
                        encryptedDataValid = false;
                        cryptographicallyValid = false; // Encryption is part of cryptographic integrity
                        
                        if (errorMessage == null) {
                            errorMessage = "Encrypted block validation failed: " + encryptedValidation.getErrorMessage();
                        } else {
                            errorMessage += "; Encrypted block validation failed: " + encryptedValidation.getErrorMessage();
                        }
                        
                        System.err.println("❌ [" + threadName + "] ENCRYPTED BLOCK VALIDATION FAILURE for block #" + block.getBlockNumber() + ":");
                        System.err.println("   🔐 " + encryptedValidation.getErrorMessage());
                        
                        // Check for possible corruption
                        var corruptionAssessment = EncryptedBlockValidator.detectCorruption(block);
                        if (corruptionAssessment.isPossiblyCorrupted()) {
                            System.err.println("   ⚠️ POSSIBLE CORRUPTION DETECTED: " + corruptionAssessment.getIssues());
                        }
                        
                    } else {
                        // Show successful validation details
                        System.out.println("✅ [" + threadName + "] Encrypted block fully validated for block #" + block.getBlockNumber());
                        System.out.println("   🔐 Encryption format: valid");
                        System.out.println("   📊 Metadata: intact");
                        System.out.println("   📂 Category: " + (block.getContentCategory() != null ? block.getContentCategory() : "none"));
                        
                        // Show warnings if any
                        if (encryptedValidation.getWarningMessage() != null) {
                            if (warningMessage == null) {
                                warningMessage = encryptedValidation.getWarningMessage();
                            } else {
                                warningMessage += "; " + encryptedValidation.getWarningMessage();
                            }
                            System.out.println("   ⚠️ Warning: " + encryptedValidation.getWarningMessage());
                        }
                        
                        // Additional encryption metadata validation
                        if (block.getEncryptionMetadata() != null) {
                            int encryptedSize = block.getEncryptionMetadata().length();
                            System.out.println("   📦 Encrypted data size: " + encryptedSize + " characters");
                            
                            if (encryptedSize > 1000) {
                                String preview = block.getEncryptionMetadata().substring(0, 20) + "...";
                                System.out.println("   🔗 Metadata preview: " + preview);
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    encryptedDataValid = false;
                    cryptographicallyValid = false;
                    
                    if (errorMessage == null) {
                        errorMessage = "Encrypted block verification error: " + e.getMessage();
                    } else {
                        errorMessage += "; Encrypted block verification error: " + e.getMessage();
                    }
                    
                    System.err.println("❌ [" + threadName + "] ENCRYPTED BLOCK VALIDATION ERROR for block #" + block.getBlockNumber() + ": " + e.getMessage());
                }
            } else if (block.isDataEncrypted() && (!structurallyValid || !cryptographicallyValid)) {
                // Block is encrypted but failed basic validation
                System.out.println("⚠️ [" + threadName + "] Block #" + block.getBlockNumber() + " is encrypted but failed basic validation - skipping encryption checks");
            }
            
            // Build result including encrypted data validation
            BlockValidationResult result = builder
                .structurallyValid(structurallyValid)
                .cryptographicallyValid(cryptographicallyValid && encryptedDataValid)
                .authorizationValid(authorizationValid)
                .offChainDataValid(offChainDataValid)
                .errorMessage(errorMessage)
                .warningMessage(warningMessage)
                .build();
            
            if (result.isFullyValid()) {
                System.out.println("✅ [" + threadName + "] Block #" + block.getBlockNumber() + " is fully valid");
            } else if (result.isValid()) {
                System.out.println("⚠️ [" + threadName + "] Block #" + block.getBlockNumber() + " is structurally valid but has authorization issues");
            } else {
                System.out.println("❌ [" + threadName + "] Block #" + block.getBlockNumber() + " is invalid");
            }
            
            return result;
            
        } catch (Exception e) {
            System.err.println("💥 [" + threadName + "] EXCEPTION during detailed block validation: " + e.getMessage());
            e.printStackTrace();
            return builder
                .structurallyValid(false)
                .cryptographicallyValid(false)
                .authorizationValid(false)
                .errorMessage("Validation exception: " + e.getMessage())
                .status(BlockStatus.INVALID)
                .build();
        }
    }
    
    /**
     * PUBLIC: Validate a single block (used by recovery manager)
     * @param block The block to validate
     * @return true if block is valid, false otherwise
     */
    public boolean validateSingleBlock(Block block) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            // Skip validation for genesis block
            if (block.getBlockNumber() != null && block.getBlockNumber().equals(0L)) {
                return true;
            }
            
            // Find the previous block for validation
            Block previousBlock = blockDAO.getBlockByNumber(block.getBlockNumber() - 1L);
            if (previousBlock == null) {
                return false; // Previous block not found
            }
            
            return validateBlock(block, previousBlock);
        } catch (Exception e) {
            return false;
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * ENHANCED: Detailed validation of the entire blockchain
     * Returns comprehensive information about validation status
     */
    public ChainValidationResult validateChainDetailed() {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            List<Block> allBlocks = blockDAO.getAllBlocks();
            List<BlockValidationResult> blockResults = new ArrayList<>();
            
            if (allBlocks.isEmpty()) {
                System.err.println("No blocks found in chain");
                return new ChainValidationResult(blockResults);
            }
            
            // Validate genesis block  
            Block genesisBlock = allBlocks.get(0);
            BlockValidationResult.Builder genesisBuilder = new BlockValidationResult.Builder(genesisBlock);
            
            if (!genesisBlock.getBlockNumber().equals(0L) || 
                !genesisBlock.getPreviousHash().equals(GENESIS_PREVIOUS_HASH)) {
                System.err.println("Invalid genesis block");
                blockResults.add(genesisBuilder
                    .structurallyValid(false)
                    .cryptographicallyValid(false)
                    .authorizationValid(false)
                    .errorMessage("Invalid genesis block")
                    .status(BlockStatus.INVALID)
                    .build());
            } else {
                blockResults.add(genesisBuilder
                    .structurallyValid(true)
                    .cryptographicallyValid(true)
                    .authorizationValid(true)
                    .status(BlockStatus.VALID)
                    .build());
            }
            
            // Validate all blocks sequentially using detailed validation
            for (int i = 1; i < allBlocks.size(); i++) {
                Block currentBlock = allBlocks.get(i);
                Block previousBlock = allBlocks.get(i - 1);
                
                BlockValidationResult result = validateBlockDetailed(currentBlock, previousBlock);
                blockResults.add(result);
            }
            
            ChainValidationResult chainResult = new ChainValidationResult(blockResults);
            
            // Generate off-chain data summary
            int totalBlocks = allBlocks.size();
            int blocksWithOffChain = 0;
            int validOffChainBlocks = 0;
            long totalOffChainSize = 0;
            
            for (Block block : allBlocks) {
                if (block.hasOffChainData()) {
                    blocksWithOffChain++;
                    if (block.getOffChainData().getFileSize() != null) {
                        totalOffChainSize += block.getOffChainData().getFileSize();
                    }
                }
            }
            
            for (BlockValidationResult result : blockResults) {
                if (result.getBlock().hasOffChainData() && result.isOffChainDataValid()) {
                    validOffChainBlocks++;
                }
            }
            
            System.out.println("📊 Chain validation completed: " + chainResult.getSummary());
            
            if (blocksWithOffChain > 0) {
                System.out.println("🗂️ Off-chain data summary:");
                System.out.println("   📊 Blocks with off-chain data: " + blocksWithOffChain + "/" + totalBlocks + 
                    " (" + String.format("%.1f%%", (blocksWithOffChain * 100.0 / totalBlocks)) + ")");
                System.out.println("   ✅ Valid off-chain blocks: " + validOffChainBlocks + "/" + blocksWithOffChain + 
                    " (" + String.format("%.1f%%", (validOffChainBlocks * 100.0 / blocksWithOffChain)) + ")");
                System.out.println("   📦 Total off-chain storage: " + String.format("%.2f MB", totalOffChainSize / (1024.0 * 1024.0)));
                
                if (validOffChainBlocks < blocksWithOffChain) {
                    int invalidOffChain = blocksWithOffChain - validOffChainBlocks;
                    System.err.println("   ⚠️ Invalid off-chain blocks detected: " + invalidOffChain);
                }
            } else {
                System.out.println("📋 No off-chain data found in blockchain");
            }
            
            return chainResult;
            
        } catch (Exception e) {
            System.err.println("Error during detailed chain validation: " + e.getMessage());
            e.printStackTrace();
            return new ChainValidationResult(new ArrayList<>());
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * ENHANCED: Check if chain is structurally intact (ignoring authorization issues)
     */
    public boolean isStructurallyIntact() {
        ChainValidationResult result = validateChainDetailed();
        return result.isStructurallyIntact();
    }
    
    /**
     * ENHANCED: Check if chain is fully compliant (structure + authorization)
     */
    public boolean isFullyCompliant() {
        ChainValidationResult result = validateChainDetailed();
        return result.isFullyCompliant();
    }
    
    /**
     * ENHANCED: Get blocks that have been orphaned by key revocations
     */
    public List<Block> getOrphanedBlocks() {
        ChainValidationResult result = validateChainDetailed();
        return result.getOrphanedBlocks();
    }
    
    /**
     * ENHANCED: Get only valid blocks (excludes revoked and invalid)
     */
    public List<Block> getValidChain() {
        ChainValidationResult result = validateChainDetailed();
        return result.getValidBlocksList();
    }
    
    /**
     * ENHANCED: Get all blocks with their validation status
     */
    public List<Block> getFullChain() {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            return blockDAO.getAllBlocks();
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * ENHANCED: Get detailed validation report for auditing
     */
    public String getValidationReport() {
        ChainValidationResult result = validateChainDetailed();
        return result.getDetailedReport();
    }
    
    /**
     * CORE FUNCTION: Build block content for hashing and signing (without signature)
     */
    /**
     * Construye el contenido del bloque para verificación de hash y firma
     * 
     * @param block El bloque del que construir el contenido
     * @return El contenido del bloque como string
     */
    public String buildBlockContent(Block block) {
        // Use epoch seconds for timestamp to ensure consistency
        long timestampSeconds = block.getTimestamp() != null ? 
            block.getTimestamp().toEpochSecond(ZoneOffset.UTC) : 0;
            
        String content = block.getBlockNumber() + 
               (block.getPreviousHash() != null ? block.getPreviousHash() : "") + 
               (block.getData() != null ? block.getData() : "") + 
               timestampSeconds +
               (block.getSignerPublicKey() != null ? block.getSignerPublicKey() : "");
        return content;
    }    

    /**
     * CORE FUNCTION: Add an authorized key
     * IMPROVED: Now allows re-authorization of previously revoked keys
     * FIXED: Added global synchronization and consistent timestamps
     */
    public boolean addAuthorizedKey(String publicKeyString, String ownerName) {
        return addAuthorizedKey(publicKeyString, ownerName, null);
    }
    
    /**
     * CORE FUNCTION: Add an authorized key with specific timestamp (for CLI operations)
     * FIXED: Thread-safe with global transaction and consistent timestamps
     */
    public boolean addAuthorizedKey(String publicKeyString, String ownerName, LocalDateTime creationTime) {
        GLOBAL_BLOCKCHAIN_LOCK.writeLock().lock();
        try {
            return JPAUtil.executeInTransaction(em -> {
                try {
                    // Validate input parameters
                    if (publicKeyString == null || publicKeyString.trim().isEmpty()) {
                        System.err.println("Public key cannot be null or empty");
                        return false;
                    }
                    if (ownerName == null || ownerName.trim().isEmpty()) {
                        System.err.println("Owner name cannot be null or empty");
                        return false;
                    }
                    
                    // Enhanced key validation using CryptoUtil
                    try {
                        // Validate key format and cryptographic validity
                        PublicKey testKey = CryptoUtil.stringToPublicKey(publicKeyString);
                        if (testKey == null) {
                            System.err.println("Invalid public key format");
                            return false;
                        }
                        
                        // Check if key is currently authorized
                        if (authorizedKeyDAO.isKeyAuthorized(publicKeyString)) {
                            System.err.println("Key already authorized");
                            return false;
                        }
                    } catch (Exception e) {
                        System.err.println("Key validation failed: " + e.getMessage());
                        return false;
                    }
                    
                    // Use consistent timestamp
                    LocalDateTime timestamp = creationTime != null ? creationTime : LocalDateTime.now();
                    
                    // Allow re-authorization: create new authorization record
                    AuthorizedKey authorizedKey = new AuthorizedKey(publicKeyString, ownerName, timestamp);
                    
                    authorizedKeyDAO.saveAuthorizedKey(authorizedKey);
                    System.out.println("Authorized key added for: " + ownerName);
                    return true;
                    
                } catch (Exception e) {
                    System.err.println("Error adding authorized key: " + e.getMessage());
                    return false;
                }
            });
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.writeLock().unlock();
        }
    }
    
    /**
     * CORE FUNCTION: Revoke an authorized key
     * FIXED: Added global synchronization for thread safety
     */
    public boolean revokeAuthorizedKey(String publicKeyString) {
        GLOBAL_BLOCKCHAIN_LOCK.writeLock().lock();
        try {
            return JPAUtil.executeInTransaction(em -> {
                try {
                    // Validate input parameter
                    if (publicKeyString == null || publicKeyString.trim().isEmpty()) {
                        System.err.println("Public key cannot be null or empty");
                        return false;
                    }
                    
                    if (!authorizedKeyDAO.isKeyAuthorized(publicKeyString)) {
                        System.err.println("Key not found or already inactive");
                        return false;
                    }
                    
                    authorizedKeyDAO.revokeAuthorizedKey(publicKeyString);
                    System.out.println("Key revoked successfully");
                    return true;
                    
                } catch (Exception e) {
                    System.err.println("Error revoking key: " + e.getMessage());
                    return false;
                }
            });
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.writeLock().unlock();
        }
    }    
    // ===============================
    // CORE FUNCTIONS: ADVANCED ADDITIONS
    // ===============================
    
    /**
     * CORE FUNCTION 1: Block Size Validation
     */
    /**
     * Valida que el tamaño del bloque no exceda el límite máximo
     * 
     * @param data Los datos del bloque a validar
     * @return true si el tamaño es válido, false si excede el límite
     */
    public boolean validateBlockSize(String data) {
        if (data == null) {
            System.err.println("Block data cannot be null. Use empty string \"\" for system blocks");
            return false; // SECURITY: Reject null data but allow empty strings
        }
        
        // Allow empty strings for system/configuration blocks
        if (data.isEmpty()) {
            System.out.println("System block with empty data created");
            return true; // Allow system blocks with empty data
        }
        
        // Check character length for normal content
        if (data.length() > MAX_BLOCK_DATA_LENGTH) {
            System.err.println("Block data length (" + data.length() + 
                             ") exceeds maximum allowed (" + MAX_BLOCK_DATA_LENGTH + " characters)");
            return false;
        }
        
        // Check byte size (important for UTF-8 strings)
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        if (dataBytes.length > MAX_BLOCK_SIZE_BYTES) {
            System.err.println("Block data size (" + dataBytes.length + 
                             " bytes) exceeds maximum allowed (" + MAX_BLOCK_SIZE_BYTES + " bytes)");
            return false;
        }
        
        return true;
    }
    
    /**
     * Enhanced block size validation that supports off-chain storage
     * Returns: 0 = invalid, 1 = store on-chain, 2 = store off-chain
     */
    public int validateAndDetermineStorage(String data) {
        if (data == null) {
            System.err.println("Block data cannot be null. Use empty string \"\" for system blocks");
            return 0; // Invalid
        }
        
        // Allow empty strings for system/configuration blocks
        if (data.isEmpty()) {
            System.out.println("System block with empty data created");
            return 1; // Store on-chain
        }
        
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        
        // FIXED: Simplified and correct logic based on data size only
        // If data is small enough for on-chain storage (under off-chain threshold)
        if (dataBytes.length < currentOffChainThresholdBytes) {
            return 1; // Store on-chain
        }
        
        // If data is large but within reasonable limits, store off-chain
        if (dataBytes.length <= 100 * 1024 * 1024) { // Max 100MB
            System.out.println("Large data detected (" + dataBytes.length + " bytes). Will store off-chain.");
            return 2; // Store off-chain
        }
        
        // Data is too large even for off-chain storage
        System.err.println("Data size (" + dataBytes.length + " bytes) exceeds maximum supported size (100MB)");
        return 0; // Invalid
    }
    
    /**
     * Validate block input parameters for encrypted blocks
     * 
     * @param data The data to validate
     * @param signerPrivateKey Private key for signing
     * @param signerPublicKey Public key for verification
     * @throws IllegalArgumentException if validation fails
     */
    private void validateBlockInput(String data, PrivateKey signerPrivateKey, PublicKey signerPublicKey) {
        if (data == null) {
            throw new IllegalArgumentException("Block data cannot be null");
        }
        if (signerPrivateKey == null) {
            throw new IllegalArgumentException("Signer private key cannot be null");
        }
        if (signerPublicKey == null) {
            throw new IllegalArgumentException("Signer public key cannot be null");
        }
    }
    
    /**
     * Validate data size for encrypted blocks
     * 
     * @param data The data to validate
     * @return true if data size is acceptable, false otherwise
     */
    private boolean validateDataSize(String data) {
        if (data == null) {
            return false;
        }
        
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        
        // Check against maximum size limits
        if (dataBytes.length > 100 * 1024 * 1024) { // Max 100MB
            System.err.println("Data size (" + dataBytes.length + " bytes) exceeds maximum supported size (100MB)");
            return false;
        }
        
        return true;
    }
    
    /**
     * Determine if data should be stored off-chain
     * 
     * @param data The data to check
     * @return true if data should be stored off-chain, false for on-chain storage
     */
    private boolean shouldStoreOffChain(String data) {
        if (data == null) {
            return false;
        }
        
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        return dataBytes.length >= currentOffChainThresholdBytes;
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
     * Generate a deterministic password for off-chain data encryption
     * Based on block number and signer public key for reproducibility
     */
    private String generateOffChainPassword(Long blockNumber, String signerPublicKey) throws Exception {
        String input = "OFFCHAIN_" + blockNumber + "_" + signerPublicKey;
        MessageDigest digest = MessageDigest.getInstance(CryptoUtil.HASH_ALGORITHM);
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        
        // Convert to Base64 for password use
        return Base64.getEncoder().encodeToString(hash).substring(0, 32); // Use first 32 chars
    }
    
    /**
     * Retrieve off-chain data for a block
     */
    public String getOffChainData(Block block) throws Exception {
        if (!block.hasOffChainData()) {
            return null;
        }
        
        String encryptionPassword = generateOffChainPassword(
            block.getBlockNumber(), 
            block.getSignerPublicKey()
        );
        
        byte[] decryptedData = offChainStorageService.retrieveData(
            block.getOffChainData(), 
            encryptionPassword
        );
        
        return new String(decryptedData, StandardCharsets.UTF_8);
    }
    
    /**
     * Verify integrity of off-chain data for a block
     */
    public boolean verifyOffChainIntegrity(Block block) {
        if (!block.hasOffChainData()) {
            return true; // No off-chain data to verify
        }
        
        try {
            String encryptionPassword = generateOffChainPassword(
                block.getBlockNumber(), 
                block.getSignerPublicKey()
            );
            
            return offChainStorageService.verifyIntegrity(
                block.getOffChainData(), 
                encryptionPassword
            );
        } catch (Exception e) {
            System.err.println("Error verifying off-chain integrity for block " + 
                             block.getBlockNumber() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get complete block data (on-chain + off-chain if applicable)
     */
    public String getCompleteBlockData(Block block) throws Exception {
        String blockData = block.getData();
        
        // Check if this is an off-chain reference
        if (blockData != null && blockData.startsWith("OFF_CHAIN_REF:")) {
            return getOffChainData(block);
        }
        
        return blockData;
    }
    
    /**
     * Verify all off-chain data integrity in the blockchain
     */
    public boolean verifyAllOffChainIntegrity() {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            List<Block> allBlocks = blockDAO.getAllBlocks();
            int offChainBlocks = 0;
            int integrityFailures = 0;
            
            for (Block block : allBlocks) {
                if (block.hasOffChainData()) {
                    offChainBlocks++;
                    if (!verifyOffChainIntegrity(block)) {
                        integrityFailures++;
                        System.err.println("Integrity verification failed for block " + block.getBlockNumber());
                    }
                }
            }
            
            System.out.println("Off-chain integrity check complete:");
            System.out.println("- Blocks with off-chain data: " + offChainBlocks);
            System.out.println("- Integrity failures: " + integrityFailures);
            
            return integrityFailures == 0;
            
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * CORE FUNCTION 2: Chain Export - Backup blockchain to file
     * FIXED: Added thread-safety with read lock and off-chain file handling
     */
    public boolean exportChain(String filePath) {
        // Validate input parameters
        if (filePath == null || filePath.trim().isEmpty()) {
            System.err.println("Export file path cannot be null or empty");
            return false;
        }
        
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            List<Block> allBlocks = blockDAO.getAllBlocks();
            List<AuthorizedKey> allKeys = authorizedKeyDAO.getAllAuthorizedKeys(); // FIXED: Export ALL keys, not just active ones
            
            // Create export data structure
            ChainExportData exportData = new ChainExportData();
            exportData.setBlocks(allBlocks);
            exportData.setAuthorizedKeys(allKeys);
            exportData.setExportTimestamp(LocalDateTime.now());
            exportData.setVersion("1.1"); // Updated version for off-chain support
            exportData.setTotalBlocks(allBlocks.size());
            
            // CRITICAL: Handle off-chain files during export
            int offChainFilesExported = 0;
            File exportDir = new File(filePath).getParentFile();
            File offChainBackupDir = new File(exportDir, "off-chain-backup");
            
            // Create backup directory for off-chain files if needed
            boolean hasOffChainData = allBlocks.stream().anyMatch(Block::hasOffChainData);
            if (hasOffChainData) {
                if (!offChainBackupDir.exists()) {
                    try {
                        if (!offChainBackupDir.mkdirs()) {
                            System.err.println("Failed to create off-chain backup directory: " + offChainBackupDir.getAbsolutePath());
                            System.err.println("Export will continue without off-chain file backup");
                            // Continue export without backup instead of failing completely
                        }
                    } catch (SecurityException e) {
                        System.err.println("Security exception creating backup directory: " + e.getMessage());
                        System.err.println("Export will continue without off-chain file backup");
                        // Continue export without backup instead of failing completely
                    }
                }
                
                // Copy off-chain files to backup directory (only if backup directory exists)
                if (offChainBackupDir.exists()) {
                    for (Block block : allBlocks) {
                        if (block.hasOffChainData()) {
                            try {
                                OffChainData offChainData = block.getOffChainData();
                                File sourceFile = new File(offChainData.getFilePath());
                                
                                if (sourceFile.exists()) {
                                    String fileName = "block_" + block.getBlockNumber() + "_" + sourceFile.getName();
                                    File backupFile = new File(offChainBackupDir, fileName);
                                    
                                    // Copy file using Java NIO for better performance
                                    java.nio.file.Files.copy(sourceFile.toPath(), backupFile.toPath(), 
                                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                    
                                    // Update the path in the export data to point to backup location
                                    offChainData.setFilePath("off-chain-backup/" + fileName);
                                    offChainFilesExported++;
                                    
                                    System.out.println("  ✓ Exported off-chain file for block #" + block.getBlockNumber());
                                } else {
                                    System.err.println("  ⚠ Off-chain file missing for block #" + block.getBlockNumber() + ": " + sourceFile.getAbsolutePath());
                                }
                            } catch (Exception e) {
                                System.err.println("  ❌ Error exporting off-chain file for block #" + block.getBlockNumber() + ": " + e.getMessage());
                            }
                        }
                    }
                } else {
                    System.out.println("  ⚠ Skipping off-chain file backup (backup directory not available)");
                }
            }
            
            // Convert to JSON
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            
            // Write to file
            File file = new File(filePath);
            mapper.writeValue(file, exportData);
            
            System.out.println("Chain exported successfully to: " + filePath);
            System.out.println("Exported " + allBlocks.size() + " blocks and " + allKeys.size() + " authorized keys");
            if (offChainFilesExported > 0) {
                System.out.println("Exported " + offChainFilesExported + " off-chain files to: " + offChainBackupDir.getAbsolutePath());
            }
            return true;
            
        } catch (Exception e) {
            System.err.println("Error exporting chain: " + e.getMessage());
            return false;
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * CORE FUNCTION 3: Chain Import - Restore blockchain from file
     * FIXED: Added thread-safety with write lock and global transaction
     */
    public boolean importChain(String filePath) {
        GLOBAL_BLOCKCHAIN_LOCK.writeLock().lock();
        try {
            return JPAUtil.executeInTransaction(em -> {
                try {
                    // Read and parse JSON file
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.registerModule(new JavaTimeModule());
                    
                    File file = new File(filePath);
                    if (!file.exists()) {
                        System.err.println("Import file not found: " + filePath);
                        return false;
                    }
                    
                    ChainExportData importData = mapper.readValue(file, ChainExportData.class);
                    
                    // Validate import data
                    if (importData.getBlocks() == null || importData.getBlocks().isEmpty()) {
                        System.err.println("No blocks found in import file");
                        return false;
                    }
                    
                    // Clear existing data (WARNING: This will delete current blockchain!)
                    System.out.println("WARNING: This will replace the current blockchain!");
                    
                    // CRITICAL: Clean up existing off-chain files before clearing database
                    System.out.println("Cleaning up existing off-chain data before import...");
                    List<Block> existingBlocks = blockDAO.getAllBlocks();
                    int existingOffChainFilesDeleted = 0;
                    
                    for (Block block : existingBlocks) {
                        if (block.hasOffChainData()) {
                            try {
                                boolean fileDeleted = offChainStorageService.deleteData(block.getOffChainData());
                                if (fileDeleted) {
                                    existingOffChainFilesDeleted++;
                                }
                            } catch (Exception e) {
                                System.err.println("Error deleting existing off-chain data for block " + block.getBlockNumber() + ": " + e.getMessage());
                            }
                        }
                    }
                    
                    if (existingOffChainFilesDeleted > 0) {
                        System.out.println("Cleaned up " + existingOffChainFilesDeleted + " existing off-chain files");
                    }
                    
                    // Clear existing blocks and keys
                    blockDAO.deleteAllBlocks();
                    authorizedKeyDAO.deleteAllAuthorizedKeys();
                    
                    // CRITICAL: Clear Hibernate session cache to avoid entity conflicts
                    em.flush();
                    em.clear();
                    
                    // Clean up any remaining orphaned files
                    cleanupOrphanedOffChainFiles();
                    
                    // Import authorized keys first with corrected timestamps
                    if (importData.getAuthorizedKeys() != null) {
                        // First, find the earliest and latest timestamp for each public key from the blocks
                        Map<String, LocalDateTime> earliestBlockTimestamps = new HashMap<>();
                        Map<String, LocalDateTime> latestBlockTimestamps = new HashMap<>();
                        
                        for (Block block : importData.getBlocks()) {
                            if (block.getSignerPublicKey() != null && !"GENESIS".equals(block.getSignerPublicKey())) {
                                String publicKey = block.getSignerPublicKey();
                                LocalDateTime blockTimestamp = block.getTimestamp();
                                
                                if (blockTimestamp != null) {
                                    earliestBlockTimestamps.merge(publicKey, blockTimestamp, 
                                        (existing, current) -> existing.isBefore(current) ? existing : current);
                                    latestBlockTimestamps.merge(publicKey, blockTimestamp, 
                                        (existing, current) -> existing.isAfter(current) ? existing : current);
                                }
                            }
                        }
                        
                        // Import authorized keys with adjusted timestamps
                        for (AuthorizedKey key : importData.getAuthorizedKeys()) {
                            // Reset ID for new insertion
                            key.setId(null);
                            
                            // FIXED: Maintain temporal consistency during import considering ALL events
                            String publicKey = key.getPublicKey();
                            
                            // Find the earliest event timestamp for this key (blocks or revocation)
                            LocalDateTime earliestEventTime = null;
                            
                            // Check earliest block signed by this key
                            if (earliestBlockTimestamps.containsKey(publicKey)) {
                                earliestEventTime = earliestBlockTimestamps.get(publicKey);
                            }
                            
                            // Check if revocation time is earlier than first block
                            if (key.getRevokedAt() != null) {
                                if (earliestEventTime == null || key.getRevokedAt().isBefore(earliestEventTime)) {
                                    earliestEventTime = key.getRevokedAt();
                                }
                            }
                            
                            // Set key creation time to be before ALL events related to this key
                            if (earliestEventTime != null) {
                                key.setCreatedAt(earliestEventTime.minusMinutes(1));
                            }
                            
                            // Handle revoked keys without revocation timestamp
                            if (!key.isActive() && key.getRevokedAt() == null) {
                                if (latestBlockTimestamps.containsKey(publicKey)) {
                                    // Set revocation time after the latest block
                                    LocalDateTime latestBlockTime = latestBlockTimestamps.get(publicKey);
                                    key.setRevokedAt(latestBlockTime.plusMinutes(1));
                                } else {
                                    // For keys without blocks, set reasonable revocation time
                                    key.setRevokedAt(key.getCreatedAt().plusMinutes(1));
                                }
                            }
                            
                            authorizedKeyDAO.saveAuthorizedKey(key);
                        }
                        System.out.println("Imported " + importData.getAuthorizedKeys().size() + " authorized keys with adjusted timestamps");
                    }
                    
                    // CRITICAL: Handle off-chain files during import
                    File importDir = new File(filePath).getParentFile();
                    File offChainBackupDir = new File(importDir, "off-chain-backup");
                    int offChainFilesImported = 0;
                    
                    // Import blocks with off-chain data handling
                    for (Block block : importData.getBlocks()) {
                        // Reset ID for new insertion
                        block.setId(null);
                        
                        // Handle off-chain data restoration
                        if (block.hasOffChainData()) {
                            OffChainData offChainData = block.getOffChainData();
                            
                            // Reset off-chain data ID for new insertion
                            offChainData.setId(null);
                            
                            try {
                                // Check if backup file exists in off-chain-backup directory
                                String backupPath = offChainData.getFilePath();
                                String fileName = new File(backupPath).getName();
                                File backupFile = new File(offChainBackupDir, fileName);
                                
                                if (backupFile.exists()) {
                                    // Create new file path in standard off-chain directory
                                    File offChainDir = new File("off-chain-data");
                                    if (!offChainDir.exists()) {
                                        offChainDir.mkdirs();
                                    }
                                    
                                    String newFileName = "block_" + block.getBlockNumber() + "_" + System.currentTimeMillis() + ".enc";
                                    File newFile = new File(offChainDir, newFileName);
                                    
                                    // Copy backup file to new location
                                    java.nio.file.Files.copy(backupFile.toPath(), newFile.toPath(), 
                                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                    
                                    // Update file path in off-chain data
                                    offChainData.setFilePath(newFile.getAbsolutePath());
                                    offChainFilesImported++;
                                    
                                    System.out.println("  ✓ Imported off-chain file for block #" + block.getBlockNumber());
                                } else {
                                    System.err.println("  ⚠ Off-chain backup file not found for block #" + block.getBlockNumber() + ": " + backupFile.getAbsolutePath());
                                    // Remove off-chain reference if file is missing
                                    block.setOffChainData(null);
                                }
                            } catch (Exception e) {
                                System.err.println("  ❌ Error importing off-chain file for block #" + block.getBlockNumber() + ": " + e.getMessage());
                                // Remove off-chain reference if import fails
                                block.setOffChainData(null);
                            }
                        }
                        
                        blockDAO.saveBlock(block);
                    }
                    
                    System.out.println("Chain imported successfully from: " + filePath);
                    System.out.println("Imported " + importData.getBlocks().size() + " blocks");
                    if (offChainFilesImported > 0) {
                        System.out.println("Imported " + offChainFilesImported + " off-chain files");
                    }
                    
                    // Validate imported chain with detailed validation
                    var importValidation = validateChainDetailed();
                    boolean isValid = importValidation.isStructurallyIntact() && importValidation.isFullyCompliant();
                    if (isValid) {
                        System.out.println("Imported chain validation: SUCCESS");
                    } else {
                        System.err.println("Imported chain validation: FAILED");
                        if (!importValidation.isStructurallyIntact()) {
                            System.err.println("  - Structural issues: " + importValidation.getInvalidBlocks() + " invalid blocks");
                        }
                        if (!importValidation.isFullyCompliant()) {
                            System.err.println("  - Compliance issues: " + importValidation.getRevokedBlocks() + " revoked blocks");
                        }
                    }
                    
                    return isValid;
                    
                } catch (Exception e) {
                    System.err.println("Error importing chain: " + e.getMessage());
                    return false;
                }
            });
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.writeLock().unlock();
        }
    }
    /**
     * CORE FUNCTION 4: Block Rollback - Remove last N blocks
     * FIXED: Added thread-safety with write lock and global transaction
     */
    public boolean rollbackBlocks(Long numberOfBlocks) {
        GLOBAL_BLOCKCHAIN_LOCK.writeLock().lock();
        try {
            return JPAUtil.executeInTransaction(em -> {
                try {
                    if (numberOfBlocks <= 0) {
                        System.err.println("Number of blocks to rollback must be positive");
                        return false;
                    }
                    
                    long currentBlockCount = blockDAO.getBlockCount();
                    if (numberOfBlocks >= currentBlockCount) {
                        System.err.println("Cannot rollback " + numberOfBlocks + 
                                         " blocks. Only " + currentBlockCount + " blocks exist (including genesis)");
                        return false;
                    }
                    
                    // Get blocks to remove (excluding genesis block)
                    List<Block> allBlocks = blockDAO.getAllBlocks();
                    List<Block> blocksToRemove = new ArrayList<>();
                    
                    // Collect last N blocks (but not genesis)
                    for (int i = allBlocks.size() - 1; i >= 1 && blocksToRemove.size() < numberOfBlocks; i--) {
                        blocksToRemove.add(allBlocks.get(i));
                    }
                    
                    if (blocksToRemove.size() != numberOfBlocks) {
                        System.err.println("Could only find " + blocksToRemove.size() + 
                                         " blocks to rollback (genesis block cannot be removed)");
                        return false;
                    }
                    
                    // Actually remove blocks and their off-chain data
                    System.out.println("Rolling back " + numberOfBlocks + " blocks:");
                    int offChainFilesDeleted = 0;
                    
                    for (Block block : blocksToRemove) {
                        String data = block.getData();
                        String displayData = data != null ? data.substring(0, Math.min(50, data.length())) : "null";
                        System.out.println("  - Removing Block #" + block.getBlockNumber() + ": " + displayData);
                        
                        // CRITICAL: Clean up off-chain data before deleting block
                        if (block.hasOffChainData()) {
                            try {
                                boolean fileDeleted = offChainStorageService.deleteData(block.getOffChainData());
                                if (fileDeleted) {
                                    offChainFilesDeleted++;
                                    System.out.println("    ✓ Deleted off-chain file: " + block.getOffChainData().getFilePath());
                                } else {
                                    System.err.println("    ⚠ Failed to delete off-chain file: " + block.getOffChainData().getFilePath());
                                }
                            } catch (Exception e) {
                                System.err.println("    ❌ Error deleting off-chain data for block " + block.getBlockNumber() + ": " + e.getMessage());
                            }
                        }
                        
                        // Delete the block from database (cascade will delete OffChainData entity)
                        blockDAO.deleteBlockByNumber(block.getBlockNumber());
                    }
                    
                    if (offChainFilesDeleted > 0) {
                        System.out.println("Cleaned up " + offChainFilesDeleted + " off-chain files during rollback");
                    }
                    
                    // CRITICAL: Synchronize block sequence after rollback
                    blockDAO.synchronizeBlockSequence();
                    
                    System.out.println("Rollback completed successfully");
                    System.out.println("Chain now has " + blockDAO.getBlockCount() + " blocks");
                    
                    return true;
                    
                } catch (Exception e) {
                    System.err.println("Error during rollback: " + e.getMessage());
                    return false;
                }
            });
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.writeLock().unlock();
        }
    }

    /**
     * CORE FUNCTION 4b: Rollback to specific block number
     * FIXED: Added thread-safety with write lock and global transaction
     */
    public boolean rollbackToBlock(Long targetBlockNumber) {
        GLOBAL_BLOCKCHAIN_LOCK.writeLock().lock();
        try {
            return JPAUtil.executeInTransaction(em -> {
                try {
                    if (targetBlockNumber == null || targetBlockNumber < 0L) {
                        System.err.println("Target block number cannot be negative");
                        return false;
                    }
                    
                    long currentBlockCount = blockDAO.getBlockCount();
                    if (targetBlockNumber >= currentBlockCount) {
                        System.err.println("Target block " + targetBlockNumber + 
                                         " does not exist. Current max block: " + (currentBlockCount - 1));
                        return false;
                    }
                    
                    int blocksToRemove = (int)(currentBlockCount - targetBlockNumber - 1L);
                    if (blocksToRemove <= 0) {
                        System.out.println("Chain is already at or before block " + targetBlockNumber);
                        return true;
                    }
                    
                    // CRITICAL: Clean up off-chain data before bulk deletion
                    System.out.println("Cleaning up off-chain data for blocks after " + targetBlockNumber);
                    List<Block> blocksToDelete = blockDAO.getBlocksAfter(targetBlockNumber);
                    int offChainFilesDeleted = 0;
                    
                    for (Block block : blocksToDelete) {
                        if (block.hasOffChainData()) {
                            try {
                                boolean fileDeleted = offChainStorageService.deleteData(block.getOffChainData());
                                if (fileDeleted) {
                                    offChainFilesDeleted++;
                                    System.out.println("  ✓ Deleted off-chain file for block #" + block.getBlockNumber());
                                } else {
                                    System.err.println("  ⚠ Failed to delete off-chain file for block #" + block.getBlockNumber());
                                }
                            } catch (Exception e) {
                                System.err.println("  ❌ Error deleting off-chain data for block " + block.getBlockNumber() + ": " + e.getMessage());
                            }
                        }
                    }
                    
                    // Now use the deleteBlocksAfter method for database cleanup
                    int deletedCount = blockDAO.deleteBlocksAfter(targetBlockNumber);
                    
                    // CRITICAL: Synchronize block sequence after rollback
                    blockDAO.synchronizeBlockSequence();
                    
                    System.out.println("Rollback to block " + targetBlockNumber + " completed successfully");
                    System.out.println("Removed " + deletedCount + " blocks");
                    if (offChainFilesDeleted > 0) {
                        System.out.println("Cleaned up " + offChainFilesDeleted + " off-chain files");
                    }
                    System.out.println("Chain now has " + blockDAO.getBlockCount() + " blocks");
                    
                    return deletedCount > 0 || blocksToRemove == 0;
                    
                } catch (Exception e) {
                    System.err.println("Error during rollback to block: " + e.getMessage());
                    return false;
                }
            });
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.writeLock().unlock();
        }
    }

    /**
     * CORE FUNCTION 5: Advanced Search - Search blocks by content
     * FIXED: Added thread-safety with read lock
     */
    public List<Block> searchBlocksByContent(String searchTerm) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return new ArrayList<>();
            }
            
            // Use the DAO method for better performance
            List<Block> matchingBlocks = blockDAO.searchBlocksByContent(searchTerm);
            
            System.out.println("Found " + matchingBlocks.size() + 
                             " blocks containing: '" + searchTerm + "'");
            return matchingBlocks;
            
        } catch (Exception e) {
            System.err.println("Error searching blocks: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    // =============== REVOLUTIONARY SEARCH FUNCTIONALITY ===============
    
    /**
     * Revolutionary Search: Intelligent search with automatic strategy selection
     */
    public List<Block> searchBlocks(String searchTerm) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            return convertEnhancedResultsToBlocks(unifiedSearchAPI.searchSimple(searchTerm));
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Revolutionary Search: Fast public metadata search
     */
    public List<Block> searchBlocksFast(String searchTerm) {
        return searchBlocks(searchTerm);
    }
    
    public List<Block> searchBlocksComplete(String searchTerm) {
        return searchBlocks(searchTerm);
    }
    
    /**
     * Revolutionary Search: Search blocks by category
     */
    public List<Block> searchByCategory(String category) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            return blockDAO.searchByCategory(category);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }    

    /**
     * CORE FUNCTION 6: Advanced Search - Get block by hash
     * FIXED: Added thread-safety with read lock
     */
    public Block getBlockByHash(String hash) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            if (hash == null || hash.trim().isEmpty()) {
                return null;
            }
            
            // Use the DAO method for better performance
            Block block = blockDAO.getBlockByHash(hash);
            
            if (block == null) {
                System.out.println("No block found with hash: " + hash);
            }
            
            return block;
            
        } catch (Exception e) {
            System.err.println("Error searching block by hash: " + e.getMessage());
            return null;
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }

    /**
     * CORE FUNCTION 7: Advanced Search - Get blocks by date range
     * FIXED: Added thread-safety with read lock
     */
    public List<Block> getBlocksByDateRange(LocalDate startDate, LocalDate endDate) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            if (startDate == null || endDate == null) {
                System.err.println("Start date and end date cannot be null");
                return new ArrayList<>();
            }
            
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            
            return blockDAO.getBlocksByTimeRange(startDateTime, endDateTime);
            
        } catch (Exception e) {
            System.err.println("Error searching blocks by date range: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    // ===== QUERY METHODS - ALL THREAD-SAFE =====
    
    /**
     * Get a block by number
     * FIXED: Added thread-safety with read lock
     */
    public Block getBlock(Long blockNumber) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            return blockDAO.getBlockByNumber(blockNumber);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Get all blocks
     * FIXED: Added thread-safety with read lock
     */
    public List<Block> getAllBlocks() {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            return blockDAO.getAllBlocks();
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Get the last block
     * FIXED: Now uses global write lock to ensure complete serialization with addBlock()
     * This method is now completely thread-safe for post-write operations
     */
    public Block getLastBlock() {
        GLOBAL_BLOCKCHAIN_LOCK.writeLock().lock();
        try {
            // Use the thread-safe implementation with refresh and complete serialization
            return blockDAO.getLastBlockWithRefresh();
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.writeLock().unlock();
        }
    }

    
    /**
     * Get the total number of blocks
     * FIXED: Added thread-safety with read lock
     */
    public long getBlockCount() {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            return blockDAO.getBlockCount();
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Get blocks by time range
     * FIXED: Added thread-safety with read lock
     */
    public List<Block> getBlocksByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            return blockDAO.getBlocksByTimeRange(startTime, endTime);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Get active authorized keys
     * FIXED: Added thread-safety with read lock
     */
    public List<AuthorizedKey> getAuthorizedKeys() {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            return authorizedKeyDAO.getActiveAuthorizedKeys();
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Get all authorized keys (including revoked ones) for export functionality
     * FIXED: Added thread-safety with read lock
     */
    public List<AuthorizedKey> getAllAuthorizedKeys() {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            return authorizedKeyDAO.getAllAuthorizedKeys();
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }

    public AuthorizedKey getAuthorizedKeyByOwner(String ownerName) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            return authorizedKeyDAO.getAuthorizedKeyByOwner(ownerName);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Verifica si una clave estaba autorizada en un momento específico
     * FIXED: Added thread-safety with read lock
     * 
     * @param publicKeyString La clave pública a verificar
     * @param timestamp El momento en el que se quiere verificar la autorización
     * @return true si la clave estaba autorizada en ese momento, false en caso contrario
     */
    public boolean wasKeyAuthorizedAt(String publicKeyString, LocalDateTime timestamp) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            return authorizedKeyDAO.wasKeyAuthorizedAt(publicKeyString, timestamp);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }    
    /**
     * Get blockchain size limits for validation
     */
    public int getMaxBlockSizeBytes() {
        return MAX_BLOCK_SIZE_BYTES;
    }
    
    public int getMaxBlockDataLength() {
        return MAX_BLOCK_DATA_LENGTH;
    }
    
    /**
     * Test utility: Clear all data and reinitialize with genesis block
     * WARNING: This method is for testing purposes only
     * FIXED: Added thread-safety with write lock and global transaction
     */
    public void clearAndReinitialize() {
        GLOBAL_BLOCKCHAIN_LOCK.writeLock().lock();
        try {
            JPAUtil.executeInTransaction(em -> {
                try {
                    // CRITICAL: Clean up all off-chain files before clearing database
                    System.out.println("Cleaning up off-chain data during reinitialization...");
                    List<Block> allBlocks = blockDAO.getAllBlocks();
                    int offChainFilesDeleted = 0;
                    
                    for (Block block : allBlocks) {
                        if (block.hasOffChainData()) {
                            try {
                                boolean fileDeleted = offChainStorageService.deleteData(block.getOffChainData());
                                if (fileDeleted) {
                                    offChainFilesDeleted++;
                                }
                            } catch (Exception e) {
                                System.err.println("Error deleting off-chain data for block " + block.getBlockNumber() + ": " + e.getMessage());
                            }
                        }
                    }
                    
                    if (offChainFilesDeleted > 0) {
                        System.out.println("Cleaned up " + offChainFilesDeleted + " off-chain files");
                    }
                    
                    // Clear all blocks and keys from database
                    blockDAO.deleteAllBlocks();
                    authorizedKeyDAO.deleteAllAuthorizedKeys();
                    
                    // CRITICAL: Clear Hibernate session cache to avoid entity conflicts
                    em.flush();
                    em.clear();
                    
                    // Clean up any remaining orphaned files in off-chain directory
                    cleanupOrphanedOffChainFiles();
                    
                    // Reinitialize genesis block (internal version without lock/transaction management)
                    initializeGenesisBlockInternal(em);
                    
                    System.out.println("Database cleared and reinitialized for testing");
                    return null;
                } catch (Exception e) {
                    System.err.println("Error clearing database: " + e.getMessage());
                    throw e;
                }
            });
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.writeLock().unlock();
        }
    }

    // ===============================
    // DANGEROUS KEY DELETION METHODS
    // ===============================

    /**
     * Check if an authorized key can be safely deleted
     * Returns information about the impact of deletion
     * FIXED: Added thread-safety with read lock
     * 
     * @param publicKey The public key to analyze
     * @return KeyDeletionImpact object with safety information
     */
    public KeyDeletionImpact canDeleteAuthorizedKey(String publicKey) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            // Check if key exists and is active
            boolean keyExists = authorizedKeyDAO.isKeyAuthorized(publicKey);
            if (!keyExists) {
                // Check if key existed but is revoked
                List<AuthorizedKey> allKeys = authorizedKeyDAO.getAllAuthorizedKeys();
                boolean keyExistedBefore = allKeys.stream()
                    .anyMatch(key -> key.getPublicKey().equals(publicKey));
                
                if (keyExistedBefore) {
                    long affectedBlocks = blockDAO.countBlocksBySignerPublicKey(publicKey);
                    return new KeyDeletionImpact(true, affectedBlocks, 
                        "Key is revoked but still has " + affectedBlocks + " historical blocks");
                } else {
                    return new KeyDeletionImpact(false, 0, "Key not found in database");
                }
            }
            
            // Count affected blocks for active key
            long affectedBlocks = blockDAO.countBlocksBySignerPublicKey(publicKey);
            
            String message = affectedBlocks == 0 ? 
                "Key can be safely deleted (no historical blocks)" : 
                "Key deletion will affect " + affectedBlocks + " historical blocks";
                
            return new KeyDeletionImpact(true, affectedBlocks, message);
            
        } catch (Exception e) {
            System.err.println("Error checking deletion impact: " + e.getMessage());
            return new KeyDeletionImpact(false, -1, "Error checking deletion impact: " + e.getMessage());
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }

    /**
     * DANGEROUS: Permanently delete an authorized key from the database
     * ⚠️ WARNING: This operation is IRREVERSIBLE and may break blockchain validation
     * FIXED: Added thread-safety with write lock and global transaction
     * 
     * @param publicKey The public key to delete
     * @param force If true, deletes even if it affects historical blocks
     * @param reason Reason for deletion (for audit logging)
     * @return true if key was deleted, false otherwise
     */
    public boolean dangerouslyDeleteAuthorizedKey(String publicKey, boolean force, String reason) {
        GLOBAL_BLOCKCHAIN_LOCK.writeLock().lock();
        try {
            return JPAUtil.executeInTransaction(em -> {
                try {
                    System.out.println("🚨 CRITICAL OPERATION: Attempting to permanently delete authorized key");
                    System.out.println("🔑 Key fingerprint: " + publicKey.substring(0, Math.min(32, publicKey.length())) + "...");
                    System.out.println("📝 Reason: " + (reason != null ? reason : "No reason provided"));
                    System.out.println("⚡ Force mode: " + force);
                    System.out.println("⏰ Timestamp: " + LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    
                    // 1. Check deletion impact
                    KeyDeletionImpact impact = canDeleteAuthorizedKey(publicKey);
                    if (!impact.keyExists()) {
                        System.err.println("❌ " + impact.getMessage());
                        return false;
                    }
                    
                    // 2. Safety check for historical blocks
                    if (impact.isSevereImpact() && !force) {
                        System.err.println("❌ SAFETY BLOCK: Cannot delete key that signed " + 
                                         impact.getAffectedBlocks() + " historical blocks");
                        System.err.println("💡 Use force=true to override this safety check");
                        System.err.println("⚠️ WARNING: Forcing deletion will break blockchain validation for these blocks!");
                        System.err.println("📊 Impact details: " + impact.getMessage());
                        return false;
                    }
                    
                    // 3. Show impact warning for forced deletions
                    if (impact.isSevereImpact()) {
                        System.out.println("⚠️ ⚠️ ⚠️ CRITICAL WARNING ⚠️ ⚠️ ⚠️");
                        System.out.println("This deletion will affect " + impact.getAffectedBlocks() + " historical blocks:");
                        
                        // Show sample of affected blocks
                        List<Block> affectedBlocks = blockDAO.getBlocksBySignerPublicKey(publicKey);
                        int sampleSize = Math.min(5, affectedBlocks.size());
                        for (int i = 0; i < sampleSize; i++) {
                            Block block = affectedBlocks.get(i);
                            String data = block.getData();
                            String preview = data != null ? data.substring(0, Math.min(50, data.length())) : "null";
                            System.out.println("  - Block #" + block.getBlockNumber() + 
                                             " (" + block.getTimestamp() + "): " + preview + "...");
                        }
                        if (affectedBlocks.size() > sampleSize) {
                            System.out.println("  ... and " + (affectedBlocks.size() - sampleSize) + " more blocks");
                        }
                        System.out.println("⚠️ These blocks will FAIL validation after key deletion!");
                        System.out.println("🔥 Proceeding with IRREVERSIBLE deletion...");
                    }
                    
                    // 4. Get key info for logging before deletion
                    List<AuthorizedKey> keyRecords = authorizedKeyDAO.getAllAuthorizedKeys().stream()
                        .filter(key -> key.getPublicKey().equals(publicKey))
                        .toList();
                    
                    // 5. Perform deletion
                    boolean deleted = authorizedKeyDAO.deleteAuthorizedKey(publicKey);
                    
                    // 6. Comprehensive audit logging
                    if (deleted) {
                        System.out.println("🗑️ ✅ Key permanently deleted from database");
                        System.out.println("📊 Deletion summary:");
                        System.out.println("   - Key records removed: " + keyRecords.size());
                        System.out.println("   - Historical blocks affected: " + impact.getAffectedBlocks());
                        System.out.println("   - Force mode used: " + force);
                        System.out.println("   - Deletion reason: " + reason);
                        
                        for (AuthorizedKey keyRecord : keyRecords) {
                            System.out.println("   - Deleted record: " + keyRecord.getOwnerName() + 
                                             " (created: " + keyRecord.getCreatedAt() + 
                                             ", active: " + keyRecord.isActive() + ")");
                        }
                        
                        System.out.println("📝 Audit log: Key deletion completed at " + 
                                         LocalDateTime.now().format(
                                             DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                        System.out.println("⚠️ WARNING: This action was IRREVERSIBLE!");
                        
                        // Recommend validation check if blocks were affected
                        if (impact.isSevereImpact()) {
                            System.out.println("💡 STRONGLY RECOMMENDED: Run validateChainDetailed() to verify blockchain integrity");
                            System.out.println("🔧 Consider running blockchain repair tools if validation fails");
                        }
                    } else {
                        System.err.println("❌ Failed to delete key from database");
                        System.err.println("💡 This might indicate the key was already deleted or a database error occurred");
                    }
                    
                    return deleted;
                    
                } catch (Exception e) {
                    System.err.println("💥 Critical error during key deletion: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            });
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.writeLock().unlock();
        }
    }

    /**
     * Convenience method for dangerous key deletion with force=false
     * @param publicKey The public key to delete
     * @param reason Reason for deletion (for audit logging)
     * @return true if key was deleted, false otherwise
     */
    public boolean dangerouslyDeleteAuthorizedKey(String publicKey, String reason) {
        return dangerouslyDeleteAuthorizedKey(publicKey, false, reason);
    }

    /**
     * Expose the regular deleteAuthorizedKey method referenced in documentation
     * This is a safe wrapper that only deletes keys with no historical impact
     * FIXED: Added thread-safety with read lock for impact check
     * @param publicKey The public key to delete
     * @return true if key was safely deleted, false otherwise
     */
    public boolean deleteAuthorizedKey(String publicKey) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        KeyDeletionImpact impact;
        try {
            impact = canDeleteAuthorizedKey(publicKey);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
        
        if (!impact.keyExists()) {
            System.err.println("Cannot delete key: " + impact.getMessage());
            return false;
        }
        
        if (impact.isSevereImpact()) {
            System.err.println("Cannot delete key: " + impact.getMessage());
            System.err.println("Use dangerouslyDeleteAuthorizedKey() with force=true if deletion is absolutely necessary");
            return false;
        }
        
        // Safe to delete - no historical blocks affected
        GLOBAL_BLOCKCHAIN_LOCK.writeLock().lock();
        try {
            return JPAUtil.executeInTransaction(em -> {
                System.out.println("Safely deleting authorized key (no historical blocks affected)");
                return authorizedKeyDAO.deleteAuthorizedKey(publicKey);
            });
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.writeLock().unlock();
        }
    }
    // ===============================
    // CHAIN RECOVERY METHODS
    // ===============================

    /**
     * Manual recovery method for corrupted chains
     * FIXED: Added thread-safety with write lock
     * @param deletedPublicKey The key that was deleted and caused corruption
     * @param ownerName Original owner name for re-authorization
     * @return RecoveryResult with success status and details
     */
    public RecoveryResult recoverCorruptedChain(String deletedPublicKey, String ownerName) {
        GLOBAL_BLOCKCHAIN_LOCK.writeLock().lock();
        try {
            ChainRecoveryManager recovery = 
                new ChainRecoveryManager(this);
            return recovery.recoverCorruptedChain(deletedPublicKey, ownerName);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.writeLock().unlock();
        }
    }

    /**
     * Diagnose chain corruption
     * FIXED: Added thread-safety with read lock
     * @return ChainDiagnostic with detailed information about corruption
     */
    public ChainDiagnostic diagnoseCorruption() {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            ChainRecoveryManager recovery = 
                new ChainRecoveryManager(this);
            return recovery.diagnoseCorruption();
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }

    /**
     * Validate chain with automatic recovery attempt
     * FIXED: Added thread-safety with read lock for validation, write lock for recovery
     * @return true if chain is valid or was successfully recovered, false otherwise
     */
    public boolean validateChainWithRecovery() {
        // First try validation with read lock
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        boolean isValid;
        try {
            var validation = validateChainDetailed();
            isValid = validation.isStructurallyIntact() && validation.isFullyCompliant();
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
        
        if (isValid) {
            return true;
        }
        
        // Chain validation failed, attempt diagnostic with read lock
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        ChainDiagnostic diagnostic;
        try {
            System.out.println("🔧 Chain validation failed, attempting diagnostic...");
            diagnostic = diagnoseCorruption();
            System.out.println("📊 Diagnostic: " + diagnostic);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
        
        if (!diagnostic.isHealthy()) {
            System.out.println("💡 Use recoverCorruptedChain() for manual recovery");
        }
        
        return false;
    }

    /**
     * Information about the impact of deleting an authorized key
     * Used to assess safety before performing dangerous key deletion operations
     */
    public static class KeyDeletionImpact {
        private final boolean keyExists;
        private final long affectedBlocks;
        private final String message;
        
        public KeyDeletionImpact(boolean keyExists, long affectedBlocks, String message) {
            this.keyExists = keyExists;
            this.affectedBlocks = affectedBlocks;
            this.message = message;
        }
        
        /**
         * @return true if the key exists in the database
         */
        public boolean keyExists() { 
            return keyExists; 
        }
        
        /**
         * @return true if the key can be deleted without affecting any historical blocks
         */
        public boolean canSafelyDelete() { 
            return keyExists && affectedBlocks == 0; 
        }
        
        /**
         * @return number of historical blocks that would be affected by deletion
         */
        public long getAffectedBlocks() { 
            return affectedBlocks; 
        }
        
        /**
         * @return human-readable message describing the impact
         */
        public String getMessage() { 
            return message; 
        }
        
        /**
         * @return true if deletion would have severe consequences (affects blocks)
         */
        public boolean isSevereImpact() {
            return affectedBlocks > 0;
        }
        
        @Override
        public String toString() {
            return String.format("KeyDeletionImpact{exists=%s, canSafelyDelete=%s, affectedBlocks=%d, message='%s'}", 
                               keyExists, canSafelyDelete(), affectedBlocks, message);
        }
    }
    
    /**
     * Configuration methods for dynamic block size limits
     */
    public void setMaxBlockSizeBytes(int maxSizeBytes) {
        if (maxSizeBytes > 0 && maxSizeBytes <= 10 * 1024 * 1024) { // Max 10MB for on-chain
            this.currentMaxBlockSizeBytes = maxSizeBytes;
            System.out.println("Max block size updated to: " + maxSizeBytes + " bytes");
        } else {
            throw new IllegalArgumentException("Invalid block size. Must be between 1 and 10MB");
        }
    }
    
    public void setMaxBlockDataLength(int maxDataLength) {
        if (maxDataLength > 0 && maxDataLength <= 1000000) { // Max 1M characters
            this.currentMaxBlockDataLength = maxDataLength;
            System.out.println("Max block data length updated to: " + maxDataLength + " characters");
        } else {
            throw new IllegalArgumentException("Invalid data length. Must be between 1 and 1M characters");
        }
    }
    
    public void setOffChainThresholdBytes(int thresholdBytes) {
        if (thresholdBytes > 0 && thresholdBytes <= currentMaxBlockSizeBytes) {
            this.currentOffChainThresholdBytes = thresholdBytes;
            System.out.println("Off-chain threshold updated to: " + thresholdBytes + " bytes");
        } else {
            throw new IllegalArgumentException("Invalid threshold. Must be between 1 and current max block size");
        }
    }
    
    // Getters for current configuration
    public int getCurrentMaxBlockSizeBytes() { return currentMaxBlockSizeBytes; }
    public int getCurrentMaxBlockDataLength() { return currentMaxBlockDataLength; }
    public int getCurrentOffChainThresholdBytes() { return currentOffChainThresholdBytes; }
    
    /**
     * Reset limits to default values
     */
    public void resetLimitsToDefault() {
        this.currentMaxBlockSizeBytes = MAX_BLOCK_SIZE_BYTES;
        this.currentMaxBlockDataLength = MAX_BLOCK_DATA_LENGTH;
        this.currentOffChainThresholdBytes = OFF_CHAIN_THRESHOLD_BYTES;
        System.out.println("Block size limits reset to default values");
    }
    
    /**
     * Get current configuration summary
     */
    public String getConfigurationSummary() {
        return String.format(
            "Block Size Configuration:%n" +
            "- Max block size: %,d bytes (%.1f MB)%n" +
            "- Max data length: %,d characters%n" +
            "- Off-chain threshold: %,d bytes (%.1f KB)%n" +
            "- Default values: %,d bytes / %,d chars / %,d bytes",
            currentMaxBlockSizeBytes, currentMaxBlockSizeBytes / (1024.0 * 1024),
            currentMaxBlockDataLength,
            currentOffChainThresholdBytes, currentOffChainThresholdBytes / 1024.0,
            MAX_BLOCK_SIZE_BYTES, MAX_BLOCK_DATA_LENGTH, OFF_CHAIN_THRESHOLD_BYTES
        );
    }
    
    /**
     * Clean up orphaned off-chain files that have no corresponding database entries
     */
    private void cleanupOrphanedOffChainFiles() {
        try {
            File offChainDir = new File("off-chain-data");
            if (!offChainDir.exists() || !offChainDir.isDirectory()) {
                return; // Nothing to clean up
            }
            
            // Delete all files in off-chain directory (used for complete cleanup during reinitialization)
            File[] files = offChainDir.listFiles();
            if (files == null || files.length == 0) {
                return; // No off-chain files found
            }
            
            int orphanedFilesDeleted = 0;
            for (File file : files) {
                if (file.isFile()) {
                    try {
                        boolean deleted = file.delete();
                        if (deleted) {
                            orphanedFilesDeleted++;
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to delete orphaned file: " + file.getName() + " - " + e.getMessage());
                    }
                }
            }
            
            if (orphanedFilesDeleted > 0) {
                System.out.println("Cleaned up " + orphanedFilesDeleted + " orphaned off-chain files");
            }
            
        } catch (Exception e) {
            System.err.println("Error during orphaned files cleanup: " + e.getMessage());
        }
    }
    
    /**
     * Find and clean up orphaned off-chain files (utility method for maintenance)
     */
    public int cleanupOrphanedFiles() {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            File offChainDir = new File("off-chain-data");
            if (!offChainDir.exists() || !offChainDir.isDirectory()) {
                return 0;
            }
            
            // Get all current off-chain file paths from database
            List<Block> allBlocks = blockDAO.getAllBlocks();
            Set<String> validFilePaths = new HashSet<>();
            
            for (Block block : allBlocks) {
                if (block.hasOffChainData()) {
                    validFilePaths.add(block.getOffChainData().getFilePath());
                }
            }
            
            // Find orphaned files (check all files, not just specific patterns)
            File[] files = offChainDir.listFiles(File::isFile);
            if (files == null) {
                return 0;
            }
            
            int orphanedFilesDeleted = 0;
            for (File file : files) {
                String absoluteFilePath = file.getAbsolutePath();
                boolean isOrphaned = true;
                
                // Check if this file path matches any valid off-chain data
                for (String validPath : validFilePaths) {
                    try {
                        // Normalize both paths for comparison (handle relative vs absolute paths)
                        String normalizedValidPath = new File(validPath).getCanonicalPath();
                        String normalizedFilePath = file.getCanonicalPath();
                        
                        if (normalizedValidPath.equals(normalizedFilePath)) {
                            isOrphaned = false;
                            break;
                        }
                    } catch (Exception e) {
                        // Fallback to simple path comparison if normalization fails
                        if (validPath.equals(absoluteFilePath)) {
                            isOrphaned = false;
                            break;
                        }
                    }
                }
                
                if (isOrphaned) {
                    try {
                        boolean deleted = file.delete();
                        if (deleted) {
                            orphanedFilesDeleted++;
                            System.out.println("Deleted orphaned file: " + file.getName());
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to delete orphaned file: " + file.getName() + " - " + e.getMessage());
                    }
                }
            }
            
            return orphanedFilesDeleted;
            
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    // ==========================================
    // ENCRYPTED BLOCK SEARCH FUNCTIONALITY
    // ==========================================
    
    /**
     * Search encrypted blocks by metadata only (category, timestamps, etc.)
     * This preserves privacy as it doesn't decrypt content
     * Uses Revolutionary Search Engine with proper metadata indexing
     * 
     * @param searchTerm The term to search for
     * @return List of matching encrypted blocks (content remains encrypted)
     */
    public List<Block> searchEncryptedBlocksByMetadata(String searchTerm) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            // Use Revolutionary Search Engine with enhanced metadata search
            // This searches in PublicLayer metadata (timestamps, categories) 
            // without requiring passwords
            List<EnhancedSearchResult> results = unifiedSearchAPI.searchSimple(searchTerm);
            
            // Filter to only encrypted blocks and convert to Block objects
            List<Block> encryptedBlocks = new ArrayList<>();
            for (EnhancedSearchResult result : results) {
                try {
                    Block block = blockDAO.getBlockByHash(result.getBlockHash());
                    if (block != null && block.isDataEncrypted()) {
                        encryptedBlocks.add(block);
                    }
                } catch (Exception e) {
                    // Skip blocks that can't be retrieved
                    continue;
                }
            }
            
            return encryptedBlocks;
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Search encrypted blocks with password-based decryption
     * SECURITY: Only decrypts blocks that match criteria
     * Uses Revolutionary Search Engine with secure encrypted content search
     * 
     * @param searchTerm The term to search for
     * @param decryptionPassword Password for decrypting matching blocks
     * @return List of matching blocks with decrypted content visible
     */
    public List<Block> searchEncryptedBlocksWithPassword(String searchTerm, String decryptionPassword) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            // Use Revolutionary Search Engine adaptive secure search for per-block passwords
            return convertEnhancedResultsToBlocks(
                unifiedSearchAPI.searchIntelligent(searchTerm, decryptionPassword, 50));
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Enhanced search with Revolutionary Search Engine
     * Automatically determines optimal strategy based on password presence
     * Uses the new intelligent search capabilities
     * 
     * @param searchTerm The term to search for
     * @param decryptionPassword Optional password for encrypted content (can be null)
     * @return List of matching blocks
     */
    public List<Block> searchBlocksEnhanced(String searchTerm, String decryptionPassword) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            // Use the new intelligent search that automatically handles password registry
            return convertEnhancedResultsToBlocks(
                unifiedSearchAPI.searchIntelligent(searchTerm, decryptionPassword, 50));
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Search by user-defined term - works for both encrypted and unencrypted blocks
     * Uses Revolutionary Search with content-agnostic approach
     * 
     * @param searchTerm The user-defined search term to look for
     * @return List of blocks matching the search term
     */
    public List<Block> searchBlocksByTerm(String searchTerm) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            // Use general search for any user-defined term
            return convertEnhancedResultsToBlocks(unifiedSearchAPI.searchSimple(searchTerm));
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
    /**
     * SIMPLIFIED API: Smart search that automatically determines the best approach
     * Uses Revolutionary Search Engine with intelligent automatic strategy routing
     * 
     * @param searchTerm The term to search for
     * @return Search results optimized for the current blockchain state
     */
    public List<Block> searchSmart(String searchTerm) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            // Revolutionary Search Engine automatically determines the optimal strategy
            // No need for manual blockchain composition analysis
            return convertEnhancedResultsToBlocks(unifiedSearchAPI.searchSimple(searchTerm));
            
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Get search statistics for the blockchain
     * Provides information about searchable vs encrypted content
     * 
     * @return Search statistics summary
     */
    public String getSearchStatistics() {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            List<Block> allBlocks = blockDAO.getAllBlocks();
            long totalBlocks = allBlocks.size();
            long encryptedBlocks = allBlocks.stream()
                .mapToLong(block -> block.isDataEncrypted() ? 1 : 0)
                .sum();
            long unencryptedBlocks = totalBlocks - encryptedBlocks;
            
            // Count blocks by category
            Map<String, Long> categoryCount = new HashMap<>();
            for (Block block : allBlocks) {
                String category = block.getContentCategory();
                if (category != null && !category.isEmpty()) {
                    categoryCount.put(category, categoryCount.getOrDefault(category, 0L) + 1);
                }
            }
            
            StringBuilder stats = new StringBuilder();
            stats.append("📊 Blockchain Search Statistics:\n");
            stats.append(String.format("   Total blocks: %d\n", totalBlocks));
            stats.append(String.format("   Unencrypted blocks: %d (%.1f%% - fully searchable)\n", 
                unencryptedBlocks, totalBlocks > 0 ? (double)unencryptedBlocks / totalBlocks * 100 : 0));
            stats.append(String.format("   Encrypted blocks: %d (%.1f%% - metadata search only)\n", 
                encryptedBlocks, totalBlocks > 0 ? (double)encryptedBlocks / totalBlocks * 100 : 0));
            
            if (!categoryCount.isEmpty()) {
                stats.append("\n📂 Blocks by category:\n");
                categoryCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(entry -> stats.append(String.format("   %s: %d blocks\n", 
                        entry.getKey(), entry.getValue())));
            }
            
            return stats.toString();
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Enhanced export for encrypted chains with encryption key management
     * Exports blockchain data with encryption keys for proper restoration
     */
    public boolean exportEncryptedChain(String filePath, String masterPassword) {
        // Validate input parameters
        if (filePath == null || filePath.trim().isEmpty()) {
            System.err.println("Export file path cannot be null or empty");
            return false;
        }
        if (masterPassword == null || masterPassword.trim().isEmpty()) {
            System.err.println("Master password required for encrypted chain export");
            return false;
        }
        
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            List<Block> allBlocks = blockDAO.getAllBlocks();
            List<AuthorizedKey> allKeys = authorizedKeyDAO.getAllAuthorizedKeys();
            
            // Extract encryption data for encrypted blocks
            EncryptionExportData encryptionData = EncryptionExportUtil.extractEncryptionData(allBlocks, masterPassword);
            
            // Create enhanced export data structure
            ChainExportData exportData = new ChainExportData(allBlocks, allKeys, encryptionData);
            exportData.setDescription("Encrypted blockchain export with encryption keys");
            
            // Handle off-chain files (same as regular export)
            int offChainFilesExported = handleOffChainExport(allBlocks, filePath);
            
            // Convert to JSON
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            
            // Write to file
            File file = new File(filePath);
            mapper.writeValue(file, exportData);
            
            System.out.println("🔐 Encrypted chain exported successfully to: " + filePath);
            System.out.println("📦 Exported " + allBlocks.size() + " blocks and " + allKeys.size() + " authorized keys");
            System.out.println("🔑 Exported encryption data for " + encryptionData.getTotalEncryptionEntries() + " entries");
            if (offChainFilesExported > 0) {
                System.out.println("📁 Exported " + offChainFilesExported + " off-chain files");
            }
            
            // Display encryption summary
            System.out.println("\n" + EncryptionExportUtil.generateEncryptionSummary(encryptionData));
            
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error exporting encrypted chain: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Enhanced import for encrypted chains with encryption key restoration
     * Imports blockchain data and restores encryption keys for proper decryption
     */
    public boolean importEncryptedChain(String filePath, String masterPassword) {
        if (masterPassword == null || masterPassword.trim().isEmpty()) {
            System.err.println("Master password required for encrypted chain import");
            return false;
        }
        
        GLOBAL_BLOCKCHAIN_LOCK.writeLock().lock();
        try {
            return JPAUtil.executeInTransaction(em -> {
                try {
                    // Read and parse JSON file
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.registerModule(new JavaTimeModule());
                    
                    File file = new File(filePath);
                    if (!file.exists()) {
                        System.err.println("Import file not found: " + filePath);
                        return false;
                    }
                    
                    ChainExportData importData = mapper.readValue(file, ChainExportData.class);
                    
                    // Validate import data
                    if (importData.getBlocks() == null || importData.getBlocks().isEmpty()) {
                        System.err.println("No blocks found in import file");
                        return false;
                    }
                    
                    // Check if import data has encryption support
                    if (!importData.hasEncryptionSupport()) {
                        System.err.println("❌ Import file does not support encryption. Use regular importChain() method.");
                        return false;
                    }
                    
                    EncryptionExportData encryptionData = importData.getEncryptionData();
                    if (encryptionData == null || encryptionData.isEmpty()) {
                        System.err.println("⚠️ No encryption data found in import file");
                    }
                    
                    // Validate encryption data consistency
                    if (!EncryptionExportUtil.validateEncryptionData(importData.getBlocks(), encryptionData)) {
                        System.err.println("❌ Encryption data validation failed");
                        return false;
                    }
                    
                    // Verify master password matches
                    if (encryptionData != null && encryptionData.getMasterPassword() != null) {
                        if (!encryptionData.getMasterPassword().equals(masterPassword)) {
                            System.err.println("❌ Master password mismatch");
                            return false;
                        }
                    }
                    
                    System.out.println("🔐 Importing encrypted blockchain...");
                    System.out.println("WARNING: This will replace the current blockchain!");
                    
                    // Clean up existing data
                    cleanupExistingData();
                    
                    // Clear existing blocks and keys
                    blockDAO.deleteAllBlocks();
                    authorizedKeyDAO.deleteAllAuthorizedKeys();
                    
                    // Force flush and clear session to avoid conflicts
                    em.flush();
                    em.clear();
                    
                    // Import authorized keys
                    System.out.println("📋 Importing authorized keys...");
                    for (AuthorizedKey originalKey : importData.getAuthorizedKeys()) {
                        // Create new detached copy to avoid JPA conflicts
                        AuthorizedKey newKey = new AuthorizedKey();
                        newKey.setId(null); // Will be auto-generated
                        newKey.setPublicKey(originalKey.getPublicKey());
                        newKey.setOwnerName(originalKey.getOwnerName());
                        newKey.setCreatedAt(originalKey.getCreatedAt());
                        newKey.setRevokedAt(originalKey.getRevokedAt());
                        newKey.setActive(originalKey.isActive());
                        
                        authorizedKeyDAO.saveAuthorizedKey(newKey);
                    }
                    
                    // Import blocks with encryption key restoration
                    System.out.println("📦 Importing blocks with encryption support...");
                    int blocksImported = 0;
                    int encryptedBlocksRestored = 0;
                    
                    for (Block block : importData.getBlocks()) {
                        // Create new block instance preserving original block numbers for off-chain compatibility
                        Block newBlock = createDetachedBlockCopyPreservingNumbers(block);
                        
                        // Restore encryption context if needed
                        if (newBlock.isDataEncrypted() && encryptionData != null) {
                            if (EncryptionExportUtil.canDecryptBlock(newBlock, encryptionData)) {
                                encryptedBlocksRestored++;
                                System.out.println("🔐 Restored encryption context for block #" + newBlock.getBlockNumber());
                            } else {
                                System.err.println("⚠️ Cannot restore encryption context for block #" + newBlock.getBlockNumber());
                            }
                        }
                        
                        // Adjust timestamps for consistency
                        adjustBlockTimestamps(newBlock, blocksImported);
                        
                        // For encrypted import, maintain original block structure for off-chain compatibility
                        // The block hashes and previous hash references should remain as they were exported
                        // This preserves the original chain integrity and off-chain password generation
                        
                        blockDAO.saveBlock(newBlock);
                        
                        // Handle off-chain password restoration if needed
                        if (newBlock.hasOffChainData() && encryptionData != null) {
                            Long originalBlockNumber = block.getBlockNumber(); // Original block number
                            String originalPassword = encryptionData.getOffChainPassword(originalBlockNumber);
                            if (originalPassword != null) {
                                // Store the mapping for later use (since we can't modify OffChainData directly)
                                System.out.println("🔑 Off-chain password restored for block #" + newBlock.getBlockNumber());
                            }
                        }
                        
                        blocksImported++;
                        
                        if (blocksImported % 100 == 0) {
                            System.out.println("   📦 Imported " + blocksImported + " blocks...");
                        }
                    }
                    
                    // Restore off-chain files
                    int offChainFilesRestored = handleOffChainImport(importData.getBlocks(), encryptionData);
                    
                    System.out.println("✅ Encrypted chain import completed successfully!");
                    System.out.println("📦 Imported " + blocksImported + " blocks and " + importData.getAuthorizedKeysCount() + " authorized keys");
                    System.out.println("🔐 Restored encryption context for " + encryptedBlocksRestored + " encrypted blocks");
                    if (offChainFilesRestored > 0) {
                        System.out.println("📁 Restored " + offChainFilesRestored + " off-chain files");
                    }
                    
                    return true;
                    
                } catch (Exception e) {
                    System.err.println("❌ Error importing encrypted chain: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            });
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.writeLock().unlock();
        }
    }
    
    /**
     * Helper method to handle off-chain file export
     */
    private int handleOffChainExport(List<Block> blocks, String filePath) {
        int offChainFilesExported = 0;
        File exportDir = new File(filePath).getParentFile();
        File offChainBackupDir = new File(exportDir, "off-chain-backup");
        
        // Check if any blocks have off-chain data
        boolean hasOffChainData = blocks.stream().anyMatch(Block::hasOffChainData);
        if (!hasOffChainData) {
            return 0;
        }
        
        // Create backup directory
        if (!offChainBackupDir.exists()) {
            try {
                if (!offChainBackupDir.mkdirs()) {
                    System.err.println("Failed to create off-chain backup directory");
                    return 0;
                }
            } catch (SecurityException e) {
                System.err.println("Security exception creating backup directory: " + e.getMessage());
                return 0;
            }
        }
        
        // Copy off-chain files to backup directory
        for (Block block : blocks) {
            if (block.hasOffChainData()) {
                try {
                    OffChainData offChainData = block.getOffChainData();
                    File sourceFile = new File(offChainData.getFilePath());
                    
                    if (sourceFile.exists()) {
                        String fileName = "block_" + block.getBlockNumber() + "_" + sourceFile.getName();
                        File backupFile = new File(offChainBackupDir, fileName);
                        
                        // Copy file
                        java.nio.file.Files.copy(sourceFile.toPath(), backupFile.toPath(), 
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        
                        // Update the path in the export data
                        offChainData.setFilePath("off-chain-backup/" + fileName);
                        offChainFilesExported++;
                        
                        System.out.println("  ✓ Exported off-chain file for block #" + block.getBlockNumber());
                    } else {
                        System.err.println("  ⚠ Off-chain file missing for block #" + block.getBlockNumber());
                    }
                } catch (Exception e) {
                    System.err.println("  ❌ Error exporting off-chain file for block #" + block.getBlockNumber() + ": " + e.getMessage());
                }
            }
        }
        
        return offChainFilesExported;
    }
    
    /**
     * Helper method to handle off-chain file import with encryption support
     */
    private int handleOffChainImport(List<Block> blocks, EncryptionExportData encryptionData) {
        int offChainFilesRestored = 0;
        
        for (Block block : blocks) {
            if (block.hasOffChainData()) {
                try {
                    OffChainData offChainData = block.getOffChainData();
                    String originalPath = offChainData.getFilePath();
                    
                    // Check if this is a backup path
                    if (originalPath.startsWith("off-chain-backup/")) {
                        File backupFile = new File(originalPath);
                        
                        if (backupFile.exists()) {
                            // Generate new file path in off-chain-data directory
                            String newFileName = "imported_" + System.currentTimeMillis() + "_" + backupFile.getName();
                            File newFile = new File("off-chain-data", newFileName);
                            
                            // Ensure off-chain-data directory exists
                            if (!newFile.getParentFile().exists()) {
                                newFile.getParentFile().mkdirs();
                            }
                            
                            // Copy file to new location
                            java.nio.file.Files.copy(backupFile.toPath(), newFile.toPath(), 
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            
                            // Update the path in the block data
                            offChainData.setFilePath(newFile.getPath());
                            offChainFilesRestored++;
                            
                            System.out.println("  ✓ Restored off-chain file for block #" + block.getBlockNumber());
                        } else {
                            System.err.println("  ⚠ Off-chain backup file missing for block #" + block.getBlockNumber());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("  ❌ Error restoring off-chain file for block #" + block.getBlockNumber() + ": " + e.getMessage());
                }
            }
        }
        
        return offChainFilesRestored;
    }
    
    /**
     * Helper method to clean up existing data before import
     */
    private void cleanupExistingData() {
        System.out.println("🧹 Cleaning up existing off-chain data before import...");
        List<Block> existingBlocks = blockDAO.getAllBlocks();
        int existingOffChainFilesDeleted = 0;
        
        for (Block block : existingBlocks) {
            if (block.hasOffChainData()) {
                try {
                    boolean fileDeleted = offChainStorageService.deleteData(block.getOffChainData());
                    if (fileDeleted) {
                        existingOffChainFilesDeleted++;
                    }
                } catch (Exception e) {
                    System.err.println("Error deleting existing off-chain data for block " + block.getBlockNumber() + ": " + e.getMessage());
                }
            }
        }
        
        if (existingOffChainFilesDeleted > 0) {
            System.out.println("Cleaned up " + existingOffChainFilesDeleted + " existing off-chain files");
        }
    }
    
    /**
     * Helper method to adjust block timestamps for consistency
     */
    private void adjustBlockTimestamps(Block block, int index) {
        // Adjust timestamp if needed for import consistency
        if (block.getTimestamp() != null) {
            // Add small offset to prevent temporal inconsistencies
            block.setTimestamp(block.getTimestamp().plusNanos(index * 1000)); // Add microseconds offset
        }
    }
    
    /**
     * Create a detached copy of a block preserving original block numbers
     * Used for encrypted chain import to maintain off-chain password compatibility
     */
    private Block createDetachedBlockCopyPreservingNumbers(Block originalBlock) {
        Block newBlock = new Block();
        
        // Copy all fields but reset IDs and preserve original block number
        newBlock.setId(null); // Will be auto-generated
        newBlock.setBlockNumber(originalBlock.getBlockNumber()); // Preserve original number
        newBlock.setData(originalBlock.getData());
        newBlock.setHash(originalBlock.getHash());
        newBlock.setPreviousHash(originalBlock.getPreviousHash());
        newBlock.setTimestamp(originalBlock.getTimestamp());
        newBlock.setSignerPublicKey(originalBlock.getSignerPublicKey());
        newBlock.setSignature(originalBlock.getSignature());
        newBlock.setIsEncrypted(originalBlock.isDataEncrypted());
        newBlock.setEncryptionMetadata(originalBlock.getEncryptionMetadata());
        newBlock.setManualKeywords(originalBlock.getManualKeywords());
        newBlock.setAutoKeywords(originalBlock.getAutoKeywords());
        newBlock.setSearchableContent(originalBlock.getSearchableContent());
        newBlock.setContentCategory(originalBlock.getContentCategory());
        
        // Copy off-chain data if present
        if (originalBlock.hasOffChainData()) {
            OffChainData originalOffChain = originalBlock.getOffChainData();
            OffChainData newOffChain = new OffChainData(
                originalOffChain.getDataHash(),
                originalOffChain.getSignature(),
                originalOffChain.getFilePath(),
                originalOffChain.getFileSize(),
                originalOffChain.getEncryptionIV(),
                originalOffChain.getContentType(),
                originalOffChain.getSignerPublicKey()
            );
            newOffChain.setId(null); // Reset ID for new persistence
            newBlock.setOffChainData(newOffChain);
        }
        
        return newBlock;
    }
    
    /**
     * Get UnifiedSearchAPI for advanced search operations
     */
    public UnifiedRevolutionarySearchAPI getUnifiedSearchAPI() {
        return unifiedSearchAPI;
    }
    
    /**
     * Get BlockDAO for direct database operations (primarily for testing)
     */
    public BlockDAO getBlockDAO() {
        return blockDAO;
    }
    
    /**
     * Get AuthorizedKeyDAO for direct database operations (primarily for testing)
     */
    public AuthorizedKeyDAO getAuthorizedKeyDAO() {
        return authorizedKeyDAO;
    }
    
    /**
     * Complete database cleanup for testing - removes ALL data including genesis block
     * WARNING: This method is intended for testing purposes only - removes ALL data
     */
    public void completeCleanupForTests() {
        blockDAO.completeCleanupTestData();
    }
    
}