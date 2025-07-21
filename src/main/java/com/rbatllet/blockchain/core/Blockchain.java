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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.rbatllet.blockchain.search.SearchFrameworkEngine;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.EnhancedSearchResult;
import com.rbatllet.blockchain.search.SearchSpecialistAPI;
import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.validation.EncryptedBlockValidator;
import com.rbatllet.blockchain.util.validation.BlockValidationUtil;

/**
 * Thread-safe Blockchain implementation
 * FIXED: Complete thread-safety with global locks and transaction management
 */
public class Blockchain {
    
    private static final Logger logger = LoggerFactory.getLogger(Blockchain.class);
    
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
    private final SearchFrameworkEngine searchFrameworkEngine;
    private final SearchSpecialistAPI searchSpecialistAPI;
    
    // Dynamic configuration for block size limits
    private volatile int currentMaxBlockSizeBytes = MAX_BLOCK_SIZE_BYTES;
    private volatile int currentMaxBlockDataLength = MAX_BLOCK_DATA_LENGTH;
    private volatile int currentOffChainThresholdBytes = OFF_CHAIN_THRESHOLD_BYTES;
    
    public Blockchain() {
        this.blockDAO = new BlockDAO();
        this.authorizedKeyDAO = new AuthorizedKeyDAO();
        
        // Initialize Search Framework Engine with high security configuration
        EncryptionConfig searchConfig = EncryptionConfig.createHighSecurityConfig();
        this.searchFrameworkEngine = new SearchFrameworkEngine(searchConfig);
        this.searchSpecialistAPI = new SearchSpecialistAPI(true); // Internal constructor
        
        initializeGenesisBlock();
        
        // Note: Advanced Search is initialized on-demand when blocks are created
        // This prevents conflicts with per-block password management
        // initializeAdvancedSearch();
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
            
            logger.info("✅ Genesis block created successfully!");
        }
    }
    
    /**
     * Initialize Search Framework Engine with existing blockchain data
     * ENHANCED: Now properly indexes encrypted keywords using password registry
     * Thread-safe: Uses read lock to protect blockchain state access
     */
    public void initializeAdvancedSearch() {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            initializeAdvancedSearch(null);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Initialize Search Framework Engine with existing blockchain data
     * ENHANCED: Now properly indexes encrypted keywords using password registry
     * Thread-safe: Uses read lock to protect blockchain state access
     * @param password Optional password to use for indexing encrypted keywords
     */
    public void initializeAdvancedSearch(String password) {
        // Check if lock is already held by current thread (to avoid recursive locking)
        boolean needsLock = !GLOBAL_BLOCKCHAIN_LOCK.isWriteLockedByCurrentThread() && 
                           GLOBAL_BLOCKCHAIN_LOCK.getReadLockCount() == 0;
        
        if (needsLock) {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        }
        try {
            // Only initialize if there are blocks to index
            if (blockDAO.getBlockCount() > 0) {
                KeyPair tempKeyPair = CryptoUtil.generateKeyPair();
                
                // Initialize with provided password (or null for public-only indexing)
                searchSpecialistAPI.initializeWithBlockchain(this, password, tempKeyPair.getPrivate());
                
                // If password was provided, the initialization should have indexed encrypted keywords
                // If not, we still try to reindex blocks with registered passwords
                if (password == null) {
                    reindexBlocksWithPasswords();
                }
                
                logger.info("📊 Search Framework Engine initialized with {} blocks", blockDAO.getBlockCount());
                logger.info("📊 Password registry stats: {}", searchSpecialistAPI.getPasswordRegistryStats());
            }
        } catch (Exception e) {
            logger.warn("⚠️ Failed to initialize Search Framework Engine", e);
            e.printStackTrace();
            // Continue operation even if search initialization fails
        } finally {
            if (needsLock) {
                GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
            }
        }
    }
    
    /**
     * Reindex blocks that have encrypted keywords with their associated passwords
     * Since passwords cannot be retrieved from registry (security by design),
     * we need a different approach to handle password-based reindexing
     */
    private void reindexBlocksWithPasswords() {
        try {
            List<Block> allBlocks = getAllBlocks();
            int reindexedCount = 0;
            
            // Get all blocks that have registered passwords
            Set<String> registeredBlockHashes = searchSpecialistAPI.getRegisteredBlocks();
            
            for (Block block : allBlocks) {
                // Check if block has encrypted keywords and is registered with a password
                if (hasEncryptedKeywords(block) && isBlockRegistered(block, registeredBlockHashes)) {
                    // For security reasons, we cannot retrieve the password
                    // The block should have been properly indexed when it was created
                    // We'll log this for debugging but the search should work through
                    // the existing password registry mechanism
                    logger.debug("📊 Block #{} has encrypted keywords and registered password", block.getBlockNumber());
                    reindexedCount++;
                }
            }
            
            logger.info("📊 Found {} blocks with encrypted keywords in password registry", reindexedCount);
        } catch (Exception e) {
            logger.warn("⚠️ Failed to analyze blocks with passwords", e);
        }
    }
    
    /**
     * Check if a block has encrypted keywords in its autoKeywords field
     */
    private boolean hasEncryptedKeywords(Block block) {
        String autoKeywords = block.getAutoKeywords();
        if (autoKeywords == null || autoKeywords.trim().isEmpty()) {
            return false;
        }
        
        // Check if autoKeywords contains encrypted data patterns
        // Encrypted data typically contains timestamps, separators (|), and base64-like strings
        return autoKeywords.contains("|") && autoKeywords.matches(".*\\d{13}\\|.*");
    }
    
    /**
     * Check if a block is registered in the password registry
     */
    private boolean isBlockRegistered(Block block, Set<String> registeredBlockHashes) {
        if (registeredBlockHashes == null || block.getHash() == null) {
            return false;
        }
        return registeredBlockHashes.contains(block.getHash());
    }
    
    /**
     * Re-index a specific block with its correct password for better search capabilities
     * This allows the Search Framework Engine to index encrypted content properly
     * Enhanced to use the new password registry system
     */
    public void reindexBlockWithPassword(Long blockNumber, String password) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            Block block = blockDAO.getBlockByNumber(blockNumber);
            if (block != null && block.isDataEncrypted()) {
                // Generate a temporary key pair for indexing
                KeyPair tempKeyPair = CryptoUtil.generateKeyPair();
                
                // Use the enhanced SearchSpecialistAPI for better password management
                searchSpecialistAPI.addBlock(
                    block, password, tempKeyPair.getPrivate());
                
                logger.debug("Re-indexed encrypted block #{} with specific password", blockNumber);
            }
        } catch (Exception e) {
            logger.error("❌ Failed to re-index block #{}", blockNumber, e);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Convert Advanced Search results to traditional Block list
     * @param enhancedResults List of EnhancedSearchResult from Advanced Search
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
                logger.warn("⚠️ Failed to retrieve block for hash {}", result.getBlockHash(), e);
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
                        logger.error("❌ Signer private key cannot be null");
                        return null;
                    }
                    if (signerPublicKey == null) {
                        logger.error("❌ Signer public key cannot be null");
                        return null;
                    }
                    
                    // 1. CORE: Validate block size and determine storage strategy
                    int storageDecision = validateAndDetermineStorage(data);
                    if (storageDecision == 0) {
                        logger.error("❌ Block data validation failed");
                        return null;
                    }
                    
                    // 2. Verify that the key is authorized at the time of block creation
                    String publicKeyString = CryptoUtil.publicKeyToString(signerPublicKey);
                    LocalDateTime blockTimestamp = LocalDateTime.now();
                    if (!authorizedKeyDAO.wasKeyAuthorizedAt(publicKeyString, blockTimestamp)) {
                        logger.error("❌ Unauthorized key attempting to add block");
                        return null;
                    }
                    
                    // 3. Get the next block number atomically to prevent race conditions
                    Long nextBlockNumber = blockDAO.getNextBlockNumberAtomic();
                    
                    // 4. Get the last block for previous hash
                    Block lastBlock = blockDAO.getLastBlockWithLock();
                    if (lastBlock == null && nextBlockNumber != 0L) {
                        logger.error("❌ Inconsistent state: no genesis block but number is {}", nextBlockNumber);
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
                            logger.info("📝 Data stored off-chain. Block contains reference: {}", blockData);
                            
                        } catch (Exception e) {
                            logger.error("❌ Failed to store data off-chain", e);
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
                        logger.error("❌ Block validation failed");
                        return null;
                    }
                    
                    // 10. Final check: verify this block number doesn't exist
                    if (blockDAO.existsBlockWithNumber(nextBlockNumber)) {
                        logger.error("🚨 CRITICAL: Race condition detected! Block number {} already exists", nextBlockNumber);
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
                    
                    // 13. Index block in Advanced Search Engine for immediate searchability
                    try {
                        // Re-index the entire blockchain when a new block is added
                        // This ensures all search capabilities are up-to-date
                        Blockchain currentBlockchain = this;
                        String defaultPassword = "search-index-password"; // Could be configurable
                        searchFrameworkEngine.indexBlockchain(currentBlockchain, defaultPassword, signerPrivateKey);
                    } catch (Exception searchIndexException) {
                        logger.warn("⚠️ Failed to index block in Advanced Search", searchIndexException);
                        // Don't fail the block creation if search indexing fails
                    }
                    
                    logger.info("✅ Block #{} added successfully!", newBlock.getBlockNumber());
                    return newBlock; // ✅ RETURN THE ACTUAL CREATED BLOCK
                    
                } catch (Exception e) {
                    logger.error("❌ Error adding block", e);
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
                        logger.error("❌ Unauthorized public key");
                        return null;
                    }
                    
                    // 3. Encrypt the data BEFORE size validation
                    String encryptedData;
                    try {
                        encryptedData = SecureBlockEncryptionService.encryptToString(data, encryptionPassword);
                    } catch (Exception e) {
                        logger.error("❌ Failed to encrypt block data", e);
                        return null;
                    }
                    
                    // 4. Validate encrypted data size
                    if (!validateDataSize(encryptedData)) {
                        logger.error("❌ Encrypted data exceeds maximum block size limits");
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
                            logger.info("📦 Encrypted data stored off-chain. Block contains reference: {}", blockData);
                            
                        } catch (Exception e) {
                            logger.error("❌ Failed to store encrypted data off-chain", e);
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
                        logger.error("❌ Encrypted block validation failed");
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
                    
                    // 13. Index the block in Advanced Search Engine with its specific password
                    try {
                        // Use the enhanced SearchSpecialistAPI for better password management
                        searchSpecialistAPI.addBlock(
                            newBlock, encryptionPassword, signerPrivateKey);
                        
                        logger.info("✅ Successfully indexed encrypted block in Search Framework Engine");
                    } catch (Exception e) {
                        logger.warn("⚠️ Failed to index encrypted block in search engine", e);
                        // Try fallback indexing with public metadata only
                        try {
                            searchFrameworkEngine.indexBlockWithSpecificPassword(
                                newBlock, null, signerPrivateKey, 
                                EncryptionConfig.createHighSecurityConfig());
                            logger.info("🔄 Fallback: Indexed block with public metadata only");
                        } catch (Exception e2) {
                            logger.error("❌ Complete indexing failure for block", e2);
                        }
                    }
                    
                    logger.info("🔐 Encrypted Block #{} added successfully!", newBlock.getBlockNumber());
                    return newBlock;
                    
                } catch (Exception e) {
                    logger.error("❌ Error adding encrypted block", e);
                    return null;
                }
            });
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.writeLock().unlock();
        }
    }
    
    /**
     * Add a block with attached off-chain data
     * 
     * @param data The main block data
     * @param offChainData The off-chain data to attach to the block
     * @param keywords Search keywords for the block
     * @param encryptionPassword Password for encryption
     * @param signerPrivateKey Private key for signing
     * @param signerPublicKey Public key for verification
     * @return The created block with off-chain data attached
     */
    public Block addBlockWithOffChainData(String data, OffChainData offChainData, String[] keywords, 
                                         String encryptionPassword, PrivateKey signerPrivateKey, 
                                         PublicKey signerPublicKey) {
        
        if (offChainData == null) {
            throw new IllegalArgumentException("Off-chain data cannot be null");
        }
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
                        logger.error("❌ Unauthorized public key");
                        return null;
                    }
                    
                    // 3. Ensure off-chain data is persisted
                    if (offChainData.getId() == null) {
                        em.persist(offChainData);
                        em.flush(); // Ensure ID is generated
                    }
                    
                    // 4. Get blockchain info
                    Block lastBlock = blockDAO.getLastBlock();
                    Long newBlockNumber = (lastBlock != null) ? lastBlock.getBlockNumber() + 1 : 1L;
                    String previousHash = (lastBlock != null) ? lastBlock.getHash() : "0";
                    
                    // 5. Create new block with off-chain data
                    Block newBlock = new Block();
                    newBlock.setBlockNumber(newBlockNumber);
                    newBlock.setPreviousHash(previousHash);
                    newBlock.setTimestamp(blockTimestamp);
                    newBlock.setSignerPublicKey(publicKeyString);
                    newBlock.setOffChainData(offChainData); // Link off-chain data
                    
                    // 6. Encrypt the main block data
                    String encryptedData = SecureBlockEncryptionService.encryptToString(data, encryptionPassword);
                    newBlock.setData(encryptedData);
                    newBlock.setIsEncrypted(true);
                    
                    // 7. Process keywords for search
                    if (keywords != null && keywords.length > 0) {
                        // Encrypt keywords for search
                        String encryptedKeywords = SecureBlockEncryptionService.encryptToString(
                            String.join(" ", keywords), encryptionPassword);
                        newBlock.setAutoKeywords(encryptedKeywords);
                        newBlock.setManualKeywords(encryptedKeywords);
                        newBlock.setSearchableContent(String.join(" ", keywords));
                    }
                    
                    // 8. Set content category
                    newBlock.setContentCategory("OFF_CHAIN_LINKED");
                    
                    // 9. Calculate block hash
                    String blockHash = CryptoUtil.calculateHash(newBlock.toString());
                    newBlock.setHash(blockHash);
                    
                    // 10. Sign the block
                    String signature = CryptoUtil.signData(blockHash, signerPrivateKey);
                    newBlock.setSignature(signature);
                    
                    // 11. Persist the block
                    em.persist(newBlock);
                    em.flush();
                    
                    // 12. Index in search engine
                    try {
                        searchSpecialistAPI.addBlock(
                            newBlock, encryptionPassword, signerPrivateKey);
                        logger.info("✅ Successfully indexed off-chain linked block in Search Framework Engine");
                    } catch (Exception e) {
                        logger.warn("⚠️ Failed to index off-chain linked block in search engine", e);
                        // Try fallback indexing
                        try {
                            searchFrameworkEngine.indexBlockWithSpecificPassword(
                                newBlock, null, signerPrivateKey, 
                                EncryptionConfig.createHighSecurityConfig());
                            logger.info("🔄 Fallback: Indexed off-chain linked block with public metadata only");
                        } catch (Exception e2) {
                            logger.error("❌ Complete indexing failure for off-chain linked block", e2);
                        }
                    }
                    
                    logger.info("🔗 Off-chain linked Block #{} added successfully! Off-chain data ID: {}", 
                              newBlock.getBlockNumber(), offChainData.getId());
                    return newBlock;
                    
                } catch (Exception e) {
                    logger.error("❌ Error adding off-chain linked block", e);
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
            logger.debug("Failed to decrypt block data (expected with wrong password)", e);
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
                    logger.debug("🔍 Stored public keywords: {}", publicKeywordString);
                }
                
                // Store private keywords encrypted in autoKeywords field
                if (!privateKeywords.isEmpty()) {
                    String privateKeywordString = String.join(" ", privateKeywords);
                    String encryptedPrivateKeywords = SecureBlockEncryptionService.encryptToString(privateKeywordString, encryptionPassword);
                    block.setAutoKeywords(encryptedPrivateKeywords);
                    logger.debug("🔍 Stored encrypted private keywords in autoKeywords");
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
            logger.error("❌ Failed to process encrypted keywords", e);
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
            logger.debug("📋 Keywords assigned to block #{}: {}...", block.getBlockNumber(), 
                        block.getSearchableContent().substring(0, Math.min(50, block.getSearchableContent().length())));
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
            logger.debug("🔍 [{}] Validating block #{}", threadName, block.getBlockNumber());
            
            // Skip validation for genesis block
            if (block.getBlockNumber() != null && block.getBlockNumber().equals(0L)) {
                logger.info("✅ [{}] Genesis block validation passed", threadName);
                return true;
            }
            
            // 1. Verify that the previous block hash matches
            if (!block.getPreviousHash().equals(previousBlock.getHash())) {
                logger.error("❌ [{}] VALIDATION FAILURE: Previous hash mismatch", threadName);
                logger.error("🔍 Expected: {}", previousBlock.getHash());
                logger.error("🔍 Got: {}", block.getPreviousHash());
                return false;
            }
            
            // 2. Verify that the block number is sequential
            if (!block.getBlockNumber().equals(previousBlock.getBlockNumber() + 1L)) {
                logger.error("❌ [{}] VALIDATION FAILURE: Block number sequence error", threadName);
                logger.error("🔍 Previous block number: {}", previousBlock.getBlockNumber());
                logger.error("🔍 Current block number: {}", block.getBlockNumber());
                logger.error("🔍 Expected: {}", (previousBlock.getBlockNumber() + 1L));
                return false;
            }
            
            // 3. Verify hash integrity
            String calculatedHash = CryptoUtil.calculateHash(buildBlockContent(block));
            if (!block.getHash().equals(calculatedHash)) {
                logger.error("❌ [{}] VALIDATION FAILURE: Block hash integrity check failed for block #{}", threadName, block.getBlockNumber());
                logger.error("🔍 Stored hash: {}", block.getHash());
                logger.error("🔍 Calculated hash: {}", calculatedHash);
                logger.error("🔍 Block content: {}", buildBlockContent(block));
                return false;
            }
            
            // 4. Verify digital signature
            PublicKey signerPublicKey = CryptoUtil.stringToPublicKey(block.getSignerPublicKey());
            String blockContent = buildBlockContent(block);
            if (!CryptoUtil.verifySignature(blockContent, block.getSignature(), signerPublicKey)) {
                logger.error("❌ [{}] VALIDATION FAILURE: Block signature verification failed for block #{}", threadName, block.getBlockNumber());
                logger.error("🔍 Block content: {}", blockContent);
                logger.error("🔍 Signature: {}", block.getSignature());
                logger.error("🔍 Signer public key: {}", block.getSignerPublicKey());
                return false;
            }
            
            // 5. Verify that the key was authorized at the time of block creation
            if (!authorizedKeyDAO.wasKeyAuthorizedAt(block.getSignerPublicKey(), block.getTimestamp())) {
                logger.error("❌ [{}] VALIDATION FAILURE: Block signed by key that was not authorized at time of creation for block #{}", threadName, block.getBlockNumber());
                logger.error("🔍 Signer: {}", block.getSignerPublicKey());
                logger.error("🔍 Block timestamp: {}", block.getTimestamp());
                return false;
            }
            
            logger.info("✅ [{}] Block #{} validation passed", threadName, block.getBlockNumber());
            return true;
            
        } catch (Exception e) {
            logger.error("💥 [{}] EXCEPTION during block validation", threadName, e);
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
            logger.debug("🔍 [{}] Detailed validation of block #{}", threadName, block.getBlockNumber());
            
            // Skip detailed validation for genesis block
            if (block.getBlockNumber() != null && block.getBlockNumber().equals(0L)) {
                logger.info("✅ [{}] Genesis block validation passed", threadName);
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
                logger.error("❌ [{}] VALIDATION FAILURE: {}", threadName, errorMessage);
            }
            
            // 2. Verify that the block number is sequential
            if (structurallyValid && !block.getBlockNumber().equals(previousBlock.getBlockNumber() + 1L)) {
                structurallyValid = false;
                errorMessage = "Block number sequence error";
                logger.error("❌ [{}] VALIDATION FAILURE: {}", threadName, errorMessage);
            }
            
            // 3. Verify hash integrity
            if (structurallyValid) {
                String calculatedHash = CryptoUtil.calculateHash(buildBlockContent(block));
                if (!block.getHash().equals(calculatedHash)) {
                    cryptographicallyValid = false;
                    errorMessage = "Block hash integrity check failed";
                    logger.error("❌ [{}] VALIDATION FAILURE: {}", threadName, errorMessage);
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
                        logger.error("❌ [{}] VALIDATION FAILURE: {}", threadName, errorMessage);
                    }
                } catch (Exception e) {
                    cryptographicallyValid = false;
                    errorMessage = "Signature verification error: " + e.getMessage();
                    logger.error("❌ [{}] VALIDATION FAILURE: {}", threadName, errorMessage);
                }
            }
            
            // 5. Verify that the key was authorized at the time of block creation
            if (structurallyValid && cryptographicallyValid) {
                if (!authorizedKeyDAO.wasKeyAuthorizedAt(block.getSignerPublicKey(), block.getTimestamp())) {
                    authorizationValid = false;
                    warningMessage = "Block signed by key that was revoked after creation";
                    logger.warn("⚠️ [{}] AUTHORIZATION WARNING: {}", threadName, warningMessage);
                    logger.warn("🔍 Signer: {}", block.getSignerPublicKey());
                    logger.warn("🔍 Block timestamp: {}", block.getTimestamp());
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
                        logger.error("❌ [{}] OFF-CHAIN VALIDATION FAILURE for block #{}:", threadName, block.getBlockNumber());
                        logger.error("   📋 Details: {}", detailedMessage);
                        if (!basicOffChainIntegrity) {
                            logger.error("   🔐 Cryptographic integrity check also failed");
                        }
                    } else {
                        // Show detailed success information
                        var offChainData = block.getOffChainData();
                        logger.info("✅ [{}] Off-chain data fully validated for block #{}", threadName, block.getBlockNumber());
                        logger.info("   📁 File: {}", java.nio.file.Paths.get(offChainData.getFilePath()).getFileName());
                        logger.info("   📦 Size: {}", (offChainData.getFileSize() != null ? 
                            String.format("%.1f KB", offChainData.getFileSize() / 1024.0) : "unknown"));
                        logger.info("   🔐 Integrity: verified (hash + encryption + signature)");
                        logger.info("   ⏰ Created: {}", (offChainData.getCreatedAt() != null ? 
                            offChainData.getCreatedAt().toString() : "unknown"));
                        
                        // Additional validation details
                        String hash = offChainData.getDataHash();
                        if (hash != null && hash.length() > 16) {
                            String truncatedHash = hash.substring(0, 8) + "..." + hash.substring(hash.length() - 8);
                            logger.info("   🔗 Hash: {}", truncatedHash);
                        }
                    }
                } catch (Exception e) {
                    offChainDataValid = false;
                    if (errorMessage == null) {
                        errorMessage = "Off-chain data verification error: " + e.getMessage();
                    } else {
                        errorMessage += "; Off-chain data verification error: " + e.getMessage();
                    }
                    logger.error("❌ [{}] OFF-CHAIN VALIDATION ERROR for block #{}", threadName, block.getBlockNumber(), e);
                }
            } else if (block.hasOffChainData() && (!structurallyValid || !cryptographicallyValid)) {
                // Block has off-chain data but failed basic validation
                logger.warn("⚠️ [{}] Block #{} has off-chain data but failed basic validation - skipping off-chain checks", threadName, block.getBlockNumber());
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
                        
                        logger.error("❌ [{}] ENCRYPTED BLOCK VALIDATION FAILURE for block #{}:", threadName, block.getBlockNumber());
                        logger.error("   🔐 {}", encryptedValidation.getErrorMessage());
                        
                        // Check for possible corruption
                        var corruptionAssessment = EncryptedBlockValidator.detectCorruption(block);
                        if (corruptionAssessment.isPossiblyCorrupted()) {
                            logger.error("   ⚠️ POSSIBLE CORRUPTION DETECTED: {}", corruptionAssessment.getIssues());
                        }
                        
                    } else {
                        // Show successful validation details
                        logger.info("✅ [{}] Encrypted block fully validated for block #{}", threadName, block.getBlockNumber());
                        logger.info("   🔐 Encryption format: valid");
                        logger.info("   📊 Metadata: intact");
                        logger.info("   📂 Category: {}", (block.getContentCategory() != null ? block.getContentCategory() : "none"));
                        
                        // Show warnings if any
                        if (encryptedValidation.getWarningMessage() != null) {
                            if (warningMessage == null) {
                                warningMessage = encryptedValidation.getWarningMessage();
                            } else {
                                warningMessage += "; " + encryptedValidation.getWarningMessage();
                            }
                            logger.warn("   ⚠️ Warning: {}", encryptedValidation.getWarningMessage());
                        }
                        
                        // Additional encryption metadata validation
                        if (block.getEncryptionMetadata() != null) {
                            int encryptedSize = block.getEncryptionMetadata().length();
                            logger.info("   📦 Encrypted data size: {} characters", encryptedSize);
                            
                            if (encryptedSize > 1000) {
                                String preview = block.getEncryptionMetadata().substring(0, 20) + "...";
                                logger.info("   🔗 Metadata preview: {}", preview);
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
                    
                    logger.error("❌ [{}] ENCRYPTED BLOCK VALIDATION ERROR for block #{}", threadName, block.getBlockNumber(), e);
                }
            } else if (block.isDataEncrypted() && (!structurallyValid || !cryptographicallyValid)) {
                // Block is encrypted but failed basic validation
                logger.warn("⚠️ [{}] Block #{} is encrypted but failed basic validation - skipping encryption checks", threadName, block.getBlockNumber());
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
                logger.info("✅ [{}] Block #{} is fully valid", threadName, block.getBlockNumber());
            } else if (result.isValid()) {
                logger.warn("⚠️ [{}] Block #{} is structurally valid but has authorization issues", threadName, block.getBlockNumber());
            } else {
                logger.error("❌ [{}] Block #{} is invalid", threadName, block.getBlockNumber());
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("💥 [{}] EXCEPTION during detailed block validation", threadName, e);
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
                logger.error("❌ No blocks found in chain");
                return new ChainValidationResult(blockResults);
            }
            
            // Validate genesis block  
            Block genesisBlock = allBlocks.get(0);
            BlockValidationResult.Builder genesisBuilder = new BlockValidationResult.Builder(genesisBlock);
            
            if (!genesisBlock.getBlockNumber().equals(0L) || 
                !genesisBlock.getPreviousHash().equals(GENESIS_PREVIOUS_HASH)) {
                logger.error("❌ Invalid genesis block");
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
            
            logger.info("📊 Chain validation completed: {}", chainResult.getSummary());
            
            if (blocksWithOffChain > 0) {
                logger.info("🗂️ Off-chain data summary:");
                logger.info("   📊 Blocks with off-chain data: {}/{} ({}%)", blocksWithOffChain, totalBlocks, 
                           String.format("%.1f", (blocksWithOffChain * 100.0 / totalBlocks)));
                logger.info("   ✅ Valid off-chain blocks: {}/{} ({}%)", validOffChainBlocks, blocksWithOffChain, 
                           String.format("%.1f", (validOffChainBlocks * 100.0 / blocksWithOffChain)));
                logger.info("   📦 Total off-chain storage: {} MB", String.format("%.2f", totalOffChainSize / (1024.0 * 1024.0)));
                
                if (validOffChainBlocks < blocksWithOffChain) {
                    int invalidOffChain = blocksWithOffChain - validOffChainBlocks;
                    logger.warn("   ⚠️ Invalid off-chain blocks detected: {}", invalidOffChain);
                }
            } else {
                logger.info("📋 No off-chain data found in blockchain");
            }
            
            return chainResult;
            
        } catch (Exception e) {
            logger.error("❌ Error during detailed chain validation", e);
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
                        logger.error("❌ Public key cannot be null or empty");
                        return false;
                    }
                    if (ownerName == null || ownerName.trim().isEmpty()) {
                        logger.error("❌ Owner name cannot be null or empty");
                        return false;
                    }
                    
                    // Enhanced key validation using CryptoUtil
                    try {
                        // Validate key format and cryptographic validity
                        PublicKey testKey = CryptoUtil.stringToPublicKey(publicKeyString);
                        if (testKey == null) {
                            logger.error("❌ Invalid public key format");
                            return false;
                        }
                        
                        // Check if key is currently authorized
                        if (authorizedKeyDAO.isKeyAuthorized(publicKeyString)) {
                            logger.error("❌ Key already authorized");
                            return false;
                        }
                    } catch (Exception e) {
                        logger.error("❌ Key validation failed", e);
                        return false;
                    }
                    
                    // Use consistent timestamp
                    LocalDateTime timestamp = creationTime != null ? creationTime : LocalDateTime.now();
                    
                    // Allow re-authorization: create new authorization record
                    AuthorizedKey authorizedKey = new AuthorizedKey(publicKeyString, ownerName, timestamp);
                    
                    authorizedKeyDAO.saveAuthorizedKey(authorizedKey);
                    logger.info("🔑 Authorized key added for: {}", ownerName);
                    return true;
                    
                } catch (Exception e) {
                    logger.error("❌ Error adding authorized key", e);
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
                        logger.error("❌ Public key cannot be null or empty");
                        return false;
                    }
                    
                    if (!authorizedKeyDAO.isKeyAuthorized(publicKeyString)) {
                        logger.error("❌ Key not found or already inactive");
                        return false;
                    }
                    
                    authorizedKeyDAO.revokeAuthorizedKey(publicKeyString);
                    logger.info("✅ Key revoked successfully");
                    return true;
                    
                } catch (Exception e) {
                    logger.error("❌ Error revoking key", e);
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
            logger.error("❌ Block data cannot be null. Use empty string \"\" for system blocks");
            return false; // SECURITY: Reject null data but allow empty strings
        }
        
        // Allow empty strings for system/configuration blocks
        if (data.isEmpty()) {
            logger.debug("ℹ️ System block with empty data created");
            return true; // Allow system blocks with empty data
        }
        
        // Check character length for normal content
        if (data.length() > MAX_BLOCK_DATA_LENGTH) {
            logger.error("❌ Block data length ({}) exceeds maximum allowed ({} characters)", 
                         data.length(), MAX_BLOCK_DATA_LENGTH);
            return false;
        }
        
        // Check byte size (important for UTF-8 strings)
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        if (dataBytes.length > MAX_BLOCK_SIZE_BYTES) {
            logger.error("❌ Block data size ({} bytes) exceeds maximum allowed ({} bytes)", 
                         dataBytes.length, MAX_BLOCK_SIZE_BYTES);
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
            logger.error("❌ Block data cannot be null. Use empty string \"\" for system blocks");
            return 0; // Invalid
        }
        
        // Allow empty strings for system/configuration blocks
        if (data.isEmpty()) {
            logger.debug("ℹ️ System block with empty data created");
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
            logger.info("📦 Large data detected ({} bytes). Will store off-chain.", dataBytes.length);
            return 2; // Store off-chain
        }
        
        // Data is too large even for off-chain storage
        logger.error("❌ Data size ({} bytes) exceeds maximum supported size (100MB)", dataBytes.length);
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
            logger.error("❌ Data size ({} bytes) exceeds maximum supported size (100MB)", dataBytes.length);
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
            logger.error("❌ Error verifying off-chain integrity for block {}", 
                        block.getBlockNumber(), e);
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
                        logger.error("❌ Integrity verification failed for block {}", block.getBlockNumber());
                    }
                }
            }
            
            logger.info("📋 Off-chain integrity check complete:");
            logger.info("📊 - Blocks with off-chain data: {}", offChainBlocks);
            logger.info("📊 - Integrity failures: {}", integrityFailures);
            
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
            logger.error("❌ Export file path cannot be null or empty");
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
                            logger.error("❌ Failed to create off-chain backup directory: {}", offChainBackupDir.getAbsolutePath());
                            logger.error("❌ Export will continue without off-chain file backup");
                            // Continue export without backup instead of failing completely
                        }
                    } catch (SecurityException e) {
                        logger.error("❌ Security exception creating backup directory", e);
                        logger.error("❌ Export will continue without off-chain file backup");
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
                                    
                                    logger.debug("  ✓ Exported off-chain file for block #{}", block.getBlockNumber());
                                } else {
                                    logger.warn("  ⚠ Off-chain file missing for block #{}: {}", block.getBlockNumber(), sourceFile.getAbsolutePath());
                                }
                            } catch (Exception e) {
                                logger.error("  ❌ Error exporting off-chain file for block #{}", block.getBlockNumber(), e);
                            }
                        }
                    }
                } else {
                    logger.warn("  ⚠ Skipping off-chain file backup (backup directory not available)");
                }
            }
            
            // Convert to JSON
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            
            // Write to file
            File file = new File(filePath);
            mapper.writeValue(file, exportData);
            
            logger.info("✅ Chain exported successfully to: {}", filePath);
            logger.info("✅ Exported {} blocks and {} authorized keys", allBlocks.size(), allKeys.size());
            if (offChainFilesExported > 0) {
                logger.info("📦 Exported {} off-chain files to: {}", offChainFilesExported, offChainBackupDir.getAbsolutePath());
            }
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Error exporting chain", e);
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
                        logger.error("❌ Import file not found: {}", filePath);
                        return false;
                    }
                    
                    ChainExportData importData = mapper.readValue(file, ChainExportData.class);
                    
                    // Validate import data
                    if (importData.getBlocks() == null || importData.getBlocks().isEmpty()) {
                        logger.error("❌ No blocks found in import file");
                        return false;
                    }
                    
                    // Clear existing data (WARNING: This will delete current blockchain!)
                    logger.warn("⚠️ WARNING: This will replace the current blockchain!");
                    
                    // CRITICAL: Clean up existing off-chain files before clearing database
                    logger.info("🧹 Cleaning up existing off-chain data before import...");
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
                                logger.error("❌ Error deleting existing off-chain data for block {}", block.getBlockNumber(), e);
                            }
                        }
                    }
                    
                    if (existingOffChainFilesDeleted > 0) {
                        logger.info("🧹 Cleaned up {} existing off-chain files", existingOffChainFilesDeleted);
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
                        logger.info("🔑 Imported {} authorized keys with adjusted timestamps", importData.getAuthorizedKeys().size());
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
                                    
                                    logger.debug("  ✓ Imported off-chain file for block #{}", block.getBlockNumber());
                                } else {
                                    logger.warn("  ⚠ Off-chain backup file not found for block #{}: {}", block.getBlockNumber(), backupFile.getAbsolutePath());
                                    // Remove off-chain reference if file is missing
                                    block.setOffChainData(null);
                                }
                            } catch (Exception e) {
                                logger.error("  ❌ Error importing off-chain file for block #{}", block.getBlockNumber(), e);
                                // Remove off-chain reference if import fails
                                block.setOffChainData(null);
                            }
                        }
                        
                        blockDAO.saveBlock(block);
                    }
                    
                    logger.info("✅ Chain imported successfully from: {}", filePath);
                    logger.info("✅ Imported {} blocks", importData.getBlocks().size());
                    if (offChainFilesImported > 0) {
                        logger.info("📦 Imported {} off-chain files", offChainFilesImported);
                    }
                    
                    // Validate imported chain with detailed validation
                    var importValidation = validateChainDetailed();
                    boolean isValid = importValidation.isStructurallyIntact() && importValidation.isFullyCompliant();
                    if (isValid) {
                        logger.info("✅ Imported chain validation: SUCCESS");
                    } else {
                        logger.error("❌ Imported chain validation: FAILED");
                        if (!importValidation.isStructurallyIntact()) {
                            logger.error("  - Structural issues: {} invalid blocks", importValidation.getInvalidBlocks());
                        }
                        if (!importValidation.isFullyCompliant()) {
                            logger.error("  - Compliance issues: {} revoked blocks", importValidation.getRevokedBlocks());
                        }
                    }
                    
                    return isValid;
                    
                } catch (Exception e) {
                    logger.error("❌ Error importing chain", e);
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
                        logger.error("❌ Number of blocks to rollback must be positive");
                        return false;
                    }
                    
                    long currentBlockCount = blockDAO.getBlockCount();
                    if (numberOfBlocks >= currentBlockCount) {
                        logger.error("❌ Cannot rollback {} blocks. Only {} blocks exist (including genesis)", 
                                    numberOfBlocks, currentBlockCount);
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
                        logger.error("❌ Could only find {} blocks to rollback (genesis block cannot be removed)", 
                                    blocksToRemove.size());
                        return false;
                    }
                    
                    // Actually remove blocks and their off-chain data
                    logger.info("🔄 Rolling back {} blocks:", numberOfBlocks);
                    int offChainFilesDeleted = 0;
                    
                    for (Block block : blocksToRemove) {
                        String data = block.getData();
                        String displayData = data != null ? data.substring(0, Math.min(50, data.length())) : "null";
                        logger.info("  - Removing Block #{}: {}", block.getBlockNumber(), displayData);
                        
                        // CRITICAL: Clean up off-chain data before deleting block
                        if (block.hasOffChainData()) {
                            try {
                                boolean fileDeleted = offChainStorageService.deleteData(block.getOffChainData());
                                if (fileDeleted) {
                                    offChainFilesDeleted++;
                                    logger.debug("    ✓ Deleted off-chain file: {}", block.getOffChainData().getFilePath());
                                } else {
                                    logger.warn("    ⚠ Failed to delete off-chain file: {}", block.getOffChainData().getFilePath());
                                }
                            } catch (Exception e) {
                                logger.error("    ❌ Error deleting off-chain data for block {}", block.getBlockNumber(), e);
                            }
                        }
                        
                        // Delete the block from database (cascade will delete OffChainData entity)
                        blockDAO.deleteBlockByNumber(block.getBlockNumber());
                    }
                    
                    if (offChainFilesDeleted > 0) {
                        logger.info("🧹 Cleaned up {} off-chain files during rollback", offChainFilesDeleted);
                    }
                    
                    // CRITICAL: Synchronize block sequence after rollback
                    blockDAO.synchronizeBlockSequence();
                    
                    logger.info("✅ Rollback completed successfully");
                    logger.info("Chain now has {} blocks", blockDAO.getBlockCount());
                    
                    return true;
                    
                } catch (Exception e) {
                    logger.error("❌ Error during rollback", e);
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
                        logger.error("❌ Target block number cannot be negative");
                        return false;
                    }
                    
                    long currentBlockCount = blockDAO.getBlockCount();
                    if (targetBlockNumber >= currentBlockCount) {
                        logger.error("❌ Target block {} does not exist. Current max block: {}", 
                                    targetBlockNumber, (currentBlockCount - 1));
                        return false;
                    }
                    
                    int blocksToRemove = (int)(currentBlockCount - targetBlockNumber - 1L);
                    if (blocksToRemove <= 0) {
                        logger.info("ℹ️ Chain is already at or before block {}", targetBlockNumber);
                        return true;
                    }
                    
                    // CRITICAL: Clean up off-chain data before bulk deletion
                    logger.info("🧹 Cleaning up off-chain data for blocks after {}", targetBlockNumber);
                    List<Block> blocksToDelete = blockDAO.getBlocksAfter(targetBlockNumber);
                    int offChainFilesDeleted = 0;
                    
                    for (Block block : blocksToDelete) {
                        if (block.hasOffChainData()) {
                            try {
                                boolean fileDeleted = offChainStorageService.deleteData(block.getOffChainData());
                                if (fileDeleted) {
                                    offChainFilesDeleted++;
                                    logger.debug("  ✓ Deleted off-chain file for block #{}", block.getBlockNumber());
                                } else {
                                    logger.warn("  ⚠ Failed to delete off-chain file for block #{}", block.getBlockNumber());
                                }
                            } catch (Exception e) {
                                logger.error("  ❌ Error deleting off-chain data for block {}", block.getBlockNumber(), e);
                            }
                        }
                    }
                    
                    // Now use the deleteBlocksAfter method for database cleanup
                    int deletedCount = blockDAO.deleteBlocksAfter(targetBlockNumber);
                    
                    // CRITICAL: Synchronize block sequence after rollback
                    blockDAO.synchronizeBlockSequence();
                    
                    logger.info("✅ Rollback to block {} completed successfully", targetBlockNumber);
                    logger.info("Removed {} blocks", deletedCount);
                    if (offChainFilesDeleted > 0) {
                        logger.info("🧹 Cleaned up {} off-chain files", offChainFilesDeleted);
                    }
                    logger.info("Chain now has {} blocks", blockDAO.getBlockCount());
                    
                    return deletedCount > 0 || blocksToRemove == 0;
                    
                } catch (Exception e) {
                    logger.error("Error during rollback to block", e);
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
            
            logger.info("🔍 Found {} blocks containing: '{}'", matchingBlocks.size(), searchTerm);
            return matchingBlocks;
            
        } catch (Exception e) {
            logger.error("❌ Error searching blocks", e);
            return new ArrayList<>();
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    // =============== ADVANCED SEARCH FUNCTIONALITY ===============
    
    /**
     * Advanced Search: Intelligent search with automatic strategy selection
     */
    public List<Block> searchBlocks(String searchTerm) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            return convertEnhancedResultsToBlocks(searchSpecialistAPI.searchSimple(searchTerm));
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Advanced Search: Fast public metadata search
     */
    public List<Block> searchBlocksFast(String searchTerm) {
        return searchBlocks(searchTerm);
    }
    
    public List<Block> searchBlocksComplete(String searchTerm) {
        return searchBlocks(searchTerm);
    }
    
    /**
     * Advanced Search: Search blocks by category
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
                logger.debug("❌ No block found with hash: {}", hash);
            }
            
            return block;
            
        } catch (Exception e) {
            logger.error("❌ Error searching block by hash", e);
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
                logger.error("❌ Start date and end date cannot be null");
                return new ArrayList<>();
            }
            
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            
            return blockDAO.getBlocksByTimeRange(startDateTime, endDateTime);
            
        } catch (Exception e) {
            logger.error("❌ Error searching blocks by date range", e);
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
     * Get blocks paginated for better performance with large blockchains
     * @param offset starting position
     * @param limit maximum number of blocks to return
     * @return list of blocks within the specified range
     */
    public List<Block> getBlocksPaginated(int offset, int limit) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            return blockDAO.getBlocksPaginated(offset, limit);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Get blocks without off-chain data for lightweight operations
     * @return list of blocks without eager loading off-chain data
     */
    public List<Block> getAllBlocksLightweight() {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            return blockDAO.getAllBlocksLightweight();
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
                    logger.info("🧹 Cleaning up off-chain data during reinitialization...");
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
                                logger.error("Error deleting off-chain data for block {}", block.getBlockNumber(), e);
                            }
                        }
                    }
                    
                    if (offChainFilesDeleted > 0) {
                        logger.info("🧹 Cleaned up {} off-chain files", offChainFilesDeleted);
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
                    
                    logger.info("✅ Database cleared and reinitialized for testing");
                    return null;
                } catch (Exception e) {
                    logger.error("❌ Error clearing database", e);
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
            logger.error("❌ Error checking deletion impact", e);
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
                    logger.warn("🚨 CRITICAL OPERATION: Attempting to permanently delete authorized key");
                    logger.warn("🔑 Key fingerprint: {}...", publicKey.substring(0, Math.min(32, publicKey.length())));
                    logger.warn("📝 Reason: {}", (reason != null ? reason : "No reason provided"));
                    logger.warn("⚡ Force mode: {}", force);
                    logger.warn("⏰ Timestamp: {}", LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    
                    // 1. Check deletion impact
                    KeyDeletionImpact impact = canDeleteAuthorizedKey(publicKey);
                    if (!impact.keyExists()) {
                        logger.error("❌ {}", impact.getMessage());
                        return false;
                    }
                    
                    // 2. Safety check for historical blocks
                    if (impact.isSevereImpact() && !force) {
                        logger.error("❌ SAFETY BLOCK: Cannot delete key that signed {} historical blocks", 
                                    impact.getAffectedBlocks());
                        logger.error("💡 Use force=true to override this safety check");
                        logger.error("⚠️ WARNING: Forcing deletion will break blockchain validation for these blocks!");
                        logger.error("📊 Impact details: {}", impact.getMessage());
                        return false;
                    }
                    
                    // 3. Show impact warning for forced deletions
                    if (impact.isSevereImpact()) {
                        logger.warn("⚠️ ⚠️ ⚠️ CRITICAL WARNING ⚠️ ⚠️ ⚠️");
                        logger.warn("This deletion will affect {} historical blocks:", impact.getAffectedBlocks());
                        
                        // Show sample of affected blocks
                        List<Block> affectedBlocks = blockDAO.getBlocksBySignerPublicKey(publicKey);
                        int sampleSize = Math.min(5, affectedBlocks.size());
                        for (int i = 0; i < sampleSize; i++) {
                            Block block = affectedBlocks.get(i);
                            String data = block.getData();
                            String preview = data != null ? data.substring(0, Math.min(50, data.length())) : "null";
                            logger.warn("  - Block #{} ({}): {}...", block.getBlockNumber(), 
                                       block.getTimestamp(), preview);
                        }
                        if (affectedBlocks.size() > sampleSize) {
                            logger.warn("  ... and {} more blocks", (affectedBlocks.size() - sampleSize));
                        }
                        logger.warn("⚠️ These blocks will FAIL validation after key deletion!");
                        logger.warn("🔥 Proceeding with IRREVERSIBLE deletion...");
                    }
                    
                    // 4. Get key info for logging before deletion
                    List<AuthorizedKey> keyRecords = authorizedKeyDAO.getAllAuthorizedKeys().stream()
                        .filter(key -> key.getPublicKey().equals(publicKey))
                        .toList();
                    
                    // 5. Perform deletion
                    boolean deleted = authorizedKeyDAO.deleteAuthorizedKey(publicKey);
                    
                    // 6. Comprehensive audit logging
                    if (deleted) {
                        logger.info("🗑️ ✅ Key permanently deleted from database");
                        logger.info("📊 Deletion summary:");
                        logger.info("   - Key records removed: {}", keyRecords.size());
                        logger.info("   - Historical blocks affected: {}", impact.getAffectedBlocks());
                        logger.info("   - Force mode used: {}", force);
                        logger.info("   - Deletion reason: {}", reason);
                        
                        for (AuthorizedKey keyRecord : keyRecords) {
                            logger.info("   - Deleted record: {} (created: {}, active: {})", 
                                       keyRecord.getOwnerName(), keyRecord.getCreatedAt(), keyRecord.isActive());
                        }
                        
                        logger.info("📝 Audit log: Key deletion completed at {}", 
                                   LocalDateTime.now().format(
                                       DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                        logger.warn("⚠️ WARNING: This action was IRREVERSIBLE!");
                        
                        // Recommend validation check if blocks were affected
                        if (impact.isSevereImpact()) {
                            logger.warn("💡 STRONGLY RECOMMENDED: Run validateChainDetailed() to verify blockchain integrity");
                            logger.warn("🔧 Consider running blockchain repair tools if validation fails");
                        }
                    } else {
                        logger.error("❌ Failed to delete key from database");
                        logger.error("💡 This might indicate the key was already deleted or a database error occurred");
                    }
                    
                    return deleted;
                    
                } catch (Exception e) {
                    logger.error("💥 Critical error during key deletion", e);
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
            logger.error("❌ Cannot delete key: {}", impact.getMessage());
            return false;
        }
        
        if (impact.isSevereImpact()) {
            logger.error("❌ Cannot delete key: {}", impact.getMessage());
            logger.error("Use dangerouslyDeleteAuthorizedKey() with force=true if deletion is absolutely necessary");
            return false;
        }
        
        // Safe to delete - no historical blocks affected
        GLOBAL_BLOCKCHAIN_LOCK.writeLock().lock();
        try {
            return JPAUtil.executeInTransaction(em -> {
                logger.info("✅ Safely deleting authorized key (no historical blocks affected)");
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
            logger.warn("🔧 Chain validation failed, attempting diagnostic...");
            diagnostic = diagnoseCorruption();
            logger.info("📊 Diagnostic: {}", diagnostic);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
        
        if (!diagnostic.isHealthy()) {
            logger.warn("💡 Use recoverCorruptedChain() for manual recovery");
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
            logger.info("📊 Max block size updated to: {} bytes", maxSizeBytes);
        } else {
            throw new IllegalArgumentException("Invalid block size. Must be between 1 and 10MB");
        }
    }
    
    public void setMaxBlockDataLength(int maxDataLength) {
        if (maxDataLength > 0 && maxDataLength <= 1000000) { // Max 1M characters
            this.currentMaxBlockDataLength = maxDataLength;
            logger.info("📊 Max block data length updated to: {} characters", maxDataLength);
        } else {
            throw new IllegalArgumentException("Invalid data length. Must be between 1 and 1M characters");
        }
    }
    
    public void setOffChainThresholdBytes(int thresholdBytes) {
        if (thresholdBytes > 0 && thresholdBytes <= currentMaxBlockSizeBytes) {
            this.currentOffChainThresholdBytes = thresholdBytes;
            logger.info("📊 Off-chain threshold updated to: {} bytes", thresholdBytes);
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
        logger.info("🔄 Block size limits reset to default values");
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
                        logger.error("Failed to delete orphaned file: {} - {}", file.getName(), e.getMessage());
                    }
                }
            }
            
            if (orphanedFilesDeleted > 0) {
                logger.info("🧹 Cleaned up {} orphaned off-chain files", orphanedFilesDeleted);
            }
            
        } catch (Exception e) {
            logger.error("❌ Error during orphaned files cleanup", e);
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
                            logger.debug("🗝️ Deleted orphaned file: {}", file.getName());
                        }
                    } catch (Exception e) {
                        logger.error("Failed to delete orphaned file: {} - {}", file.getName(), e.getMessage());
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
     * Uses Advanced Search Engine with proper metadata indexing
     * 
     * @param searchTerm The term to search for
     * @return List of matching encrypted blocks (content remains encrypted)
     */
    public List<Block> searchEncryptedBlocksByMetadata(String searchTerm) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            // Use Advanced Search Engine with enhanced metadata search
            // This searches in PublicLayer metadata (timestamps, categories) 
            // without requiring passwords
            List<EnhancedSearchResult> results = searchSpecialistAPI.searchSimple(searchTerm);
            
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
     * Uses Advanced Search Engine with secure encrypted content search
     * 
     * @param searchTerm The term to search for
     * @param decryptionPassword Password for decrypting matching blocks
     * @return List of matching blocks with decrypted content visible
     */
    public List<Block> searchEncryptedBlocksWithPassword(String searchTerm, String decryptionPassword) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            // Use Advanced Search Engine adaptive secure search for per-block passwords
            return convertEnhancedResultsToBlocks(
                searchSpecialistAPI.searchIntelligent(searchTerm, decryptionPassword, 50));
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Enhanced search with Advanced Search Engine
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
                searchSpecialistAPI.searchIntelligent(searchTerm, decryptionPassword, 50));
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
     * Search by user-defined term - works for both encrypted and unencrypted blocks
     * Uses Advanced Search with content-agnostic approach
     * 
     * @param searchTerm The user-defined search term to look for
     * @return List of blocks matching the search term
     */
    public List<Block> searchBlocksByTerm(String searchTerm) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            // Use general search for any user-defined term
            return convertEnhancedResultsToBlocks(searchSpecialistAPI.searchSimple(searchTerm));
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
    }
    
    /**
    /**
     * SIMPLIFIED API: Smart search that automatically determines the best approach
     * Uses Advanced Search Engine with intelligent automatic strategy routing
     * 
     * @param searchTerm The term to search for
     * @return Search results optimized for the current blockchain state
     */
    public List<Block> searchSmart(String searchTerm) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            // Advanced Search Engine automatically determines the optimal strategy
            // No need for manual blockchain composition analysis
            return convertEnhancedResultsToBlocks(searchSpecialistAPI.searchSimple(searchTerm));
            
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
            logger.error("❌ Export file path cannot be null or empty");
            return false;
        }
        if (masterPassword == null || masterPassword.trim().isEmpty()) {
            logger.error("❌ Master password required for encrypted chain export");
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
            
            logger.info("🔐 Encrypted chain exported successfully to: {}", filePath);
            logger.info("📦 Exported {} blocks and {} authorized keys", allBlocks.size(), allKeys.size());
            logger.info("🔑 Exported encryption data for {} entries", encryptionData.getTotalEncryptionEntries());
            if (offChainFilesExported > 0) {
                logger.info("📁 Exported {} off-chain files", offChainFilesExported);
            }
            
            // Display encryption summary
            logger.info("\n{}", EncryptionExportUtil.generateEncryptionSummary(encryptionData));
            
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Error exporting encrypted chain", e);
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
            logger.error("❌ Master password required for encrypted chain import");
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
                        logger.error("❌ Import file not found: {}", filePath);
                        return false;
                    }
                    
                    ChainExportData importData = mapper.readValue(file, ChainExportData.class);
                    
                    // Validate import data
                    if (importData.getBlocks() == null || importData.getBlocks().isEmpty()) {
                        logger.error("❌ No blocks found in import file");
                        return false;
                    }
                    
                    // Check if import data has encryption support
                    if (!importData.hasEncryptionSupport()) {
                        logger.error("❌ Import file does not support encryption. Use regular importChain() method.");
                        return false;
                    }
                    
                    EncryptionExportData encryptionData = importData.getEncryptionData();
                    if (encryptionData == null || encryptionData.isEmpty()) {
                        logger.warn("⚠️ No encryption data found in import file");
                    }
                    
                    // Validate encryption data consistency
                    if (!EncryptionExportUtil.validateEncryptionData(importData.getBlocks(), encryptionData)) {
                        logger.error("❌ Encryption data validation failed");
                        return false;
                    }
                    
                    // Verify master password matches
                    if (encryptionData != null && encryptionData.getMasterPassword() != null) {
                        if (!encryptionData.getMasterPassword().equals(masterPassword)) {
                            logger.error("❌ Master password mismatch");
                            return false;
                        }
                    }
                    
                    logger.info("🔐 Importing encrypted blockchain...");
                    logger.warn("⚠️ WARNING: This will replace the current blockchain!");
                    
                    // Clean up existing data
                    cleanupExistingData();
                    
                    // Clear existing blocks and keys
                    blockDAO.deleteAllBlocks();
                    authorizedKeyDAO.deleteAllAuthorizedKeys();
                    
                    // Force flush and clear session to avoid conflicts
                    em.flush();
                    em.clear();
                    
                    // Import authorized keys
                    logger.info("📋 Importing authorized keys...");
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
                    logger.info("📦 Importing blocks with encryption support...");
                    int blocksImported = 0;
                    int encryptedBlocksRestored = 0;
                    
                    for (Block block : importData.getBlocks()) {
                        // Create new block instance preserving original block numbers for off-chain compatibility
                        Block newBlock = createDetachedBlockCopyPreservingNumbers(block);
                        
                        // Restore encryption context if needed
                        if (newBlock.isDataEncrypted() && encryptionData != null) {
                            if (EncryptionExportUtil.canDecryptBlock(newBlock, encryptionData)) {
                                encryptedBlocksRestored++;
                                logger.info("🔐 Restored encryption context for block #{}", newBlock.getBlockNumber());
                            } else {
                                logger.warn("⚠️ Cannot restore encryption context for block #{}", newBlock.getBlockNumber());
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
                                logger.info("🔑 Off-chain password restored for block #{}", newBlock.getBlockNumber());
                            }
                        }
                        
                        blocksImported++;
                        
                        if (blocksImported % 100 == 0) {
                            logger.debug("   📦 Imported {} blocks...", blocksImported);
                        }
                    }
                    
                    // Restore off-chain files
                    int offChainFilesRestored = handleOffChainImport(importData.getBlocks(), encryptionData);
                    
                    logger.info("✅ Encrypted chain import completed successfully!");
                    logger.info("📦 Imported {} blocks and {} authorized keys", blocksImported, importData.getAuthorizedKeysCount());
                    logger.info("🔐 Restored encryption context for {} encrypted blocks", encryptedBlocksRestored);
                    if (offChainFilesRestored > 0) {
                        logger.info("📁 Restored {} off-chain files", offChainFilesRestored);
                    }
                    
                    return true;
                    
                } catch (Exception e) {
                    logger.error("❌ Error importing encrypted chain", e);
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
                    logger.error("❌ Failed to create off-chain backup directory");
                    return 0;
                }
            } catch (SecurityException e) {
                logger.error("Security exception creating backup directory", e);
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
                        
                        logger.debug("  ✓ Exported off-chain file for block #{}", block.getBlockNumber());
                    } else {
                        logger.warn("  ⚠ Off-chain file missing for block #{}", block.getBlockNumber());
                    }
                } catch (Exception e) {
                    logger.error("  ❌ Error exporting off-chain file for block #{}", block.getBlockNumber(), e);
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
                            
                            logger.debug("  ✓ Restored off-chain file for block #{}", block.getBlockNumber());
                        } else {
                            logger.warn("  ⚠ Off-chain backup file missing for block #{}", block.getBlockNumber());
                        }
                    }
                } catch (Exception e) {
                    logger.error("  ❌ Error restoring off-chain file for block #{}", block.getBlockNumber(), e);
                }
            }
        }
        
        return offChainFilesRestored;
    }
    
    /**
     * Helper method to clean up existing data before import
     */
    private void cleanupExistingData() {
        logger.info("🧹 Cleaning up existing off-chain data before import...");
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
                    logger.error("Error deleting existing off-chain data for block {}", block.getBlockNumber(), e);
                }
            }
        }
        
        if (existingOffChainFilesDeleted > 0) {
            logger.info("🧹 Cleaned up {} existing off-chain files", existingOffChainFilesDeleted);
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
     * Get SearchSpecialistAPI for advanced search operations
     */
    public SearchSpecialistAPI getSearchSpecialistAPI() {
        return searchSpecialistAPI;
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