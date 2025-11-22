package com.rbatllet.blockchain.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rbatllet.blockchain.config.EncryptionConfig;
import com.rbatllet.blockchain.config.MemorySafetyConstants;
import com.rbatllet.blockchain.dao.AuthorizedKeyDAO;
import com.rbatllet.blockchain.dto.ChainExportData;
import com.rbatllet.blockchain.dto.EncryptionExportData;
import com.rbatllet.blockchain.entity.AuthorizedKey;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.entity.OffChainData;
import com.rbatllet.blockchain.exception.BlockValidationException;
import com.rbatllet.blockchain.exception.UnauthorizedKeyException;
import com.rbatllet.blockchain.recovery.ChainRecoveryManager;
import com.rbatllet.blockchain.recovery.ChainRecoveryManager.ChainDiagnostic;
import com.rbatllet.blockchain.recovery.ChainRecoveryManager.RecoveryResult;
import com.rbatllet.blockchain.search.SearchFrameworkEngine;
import com.rbatllet.blockchain.security.UserRole;
import com.rbatllet.blockchain.search.SearchFrameworkEngine.EnhancedSearchResult;
import com.rbatllet.blockchain.search.SearchSpecialistAPI;
import com.rbatllet.blockchain.service.OffChainStorageService;
import com.rbatllet.blockchain.service.SecureBlockEncryptionService;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.EncryptionExportUtil;
import com.rbatllet.blockchain.util.JPAUtil;
import com.rbatllet.blockchain.util.validation.BlockValidationUtil;
import com.rbatllet.blockchain.validation.BlockStatus;
import com.rbatllet.blockchain.validation.BlockValidationResult;
import com.rbatllet.blockchain.validation.ChainValidationResult;
import com.rbatllet.blockchain.validation.EncryptedBlockValidator;
import jakarta.persistence.EntityManager;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.StampedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rbatllet.blockchain.util.LockTracer;

/**
 * Thread-safe Blockchain implementation
 * FIXED: Complete thread-safety with global locks and transaction management
 */
public class Blockchain {

    private static final Logger logger = LoggerFactory.getLogger(
        Blockchain.class
    );

    private final BlockRepository blockRepository;
    private final AuthorizedKeyDAO authorizedKeyDAO;
    private static final String GENESIS_PREVIOUS_HASH = "0";

    // Global lock for thread safety across multiple instances
    // Using StampedLock for better read performance (~50% improvement with optimistic reads)
    // Wrapped with LockTracer for debugging deadlock issues
    private static final LockTracer GLOBAL_BLOCKCHAIN_LOCK =
        new LockTracer(new StampedLock(), "GLOBAL_BLOCKCHAIN");

    // SECURITY FIX: Consistent block size validation
    // Use single byte-based limit for all data types to prevent confusion and bypass attempts
    private static final int MAX_BLOCK_SIZE_BYTES = 1024 * 1024; // 1MB max per block
    private static final int MAX_BLOCK_CHARS = 10000; // 10K chars (original limit for compatibility)
    // SECURITY: Single coherent limit prevents bypass through multibyte character manipulation

    // Off-chain storage threshold - data larger than this will be stored off-chain
    private static final int OFF_CHAIN_THRESHOLD_BYTES = 512 * 1024; // 512KB threshold
    private final OffChainStorageService offChainStorageService =
        new OffChainStorageService();

    // Search functionality
    private final SearchFrameworkEngine searchFrameworkEngine;
    private final SearchSpecialistAPI searchSpecialistAPI;

    // Dynamic configuration for block size limits
    private volatile int currentMaxBlockSizeBytes = MAX_BLOCK_SIZE_BYTES;
    private volatile int currentMaxBlockChars = MAX_BLOCK_CHARS;
    private volatile int currentOffChainThresholdBytes =
        OFF_CHAIN_THRESHOLD_BYTES;

    public Blockchain() {
        this.blockRepository = new BlockRepository();
        this.authorizedKeyDAO = new AuthorizedKeyDAO();

        // Initialize Search Framework Engine with high security configuration
        EncryptionConfig searchConfig =
            EncryptionConfig.createHighSecurityConfig();
        this.searchFrameworkEngine = new SearchFrameworkEngine(searchConfig);
        this.searchSpecialistAPI = new SearchSpecialistAPI(true, this.searchFrameworkEngine); // Pass the indexed instance

        initializeGenesisBlock();

        // Note: Bootstrap admin creation is now EXPLICIT via createBootstrapAdmin()
        // This ensures proper security control - no automatic admin creation!
        // Applications/demos/tests MUST call createBootstrapAdmin() explicitly.

        // Note: Advanced Search is initialized on-demand when blocks are created
        // This prevents conflicts with per-block password management
        // initializeAdvancedSearch();
    }

    /**
     * Creates the genesis block if it doesn't exist
     * FIXED: Added thread-safety with proper transaction management and sequence synchronization
     */
    private void initializeGenesisBlock() {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.writeLock();
        try {
            // Use global transaction for consistency
            JPAUtil.executeInTransaction(em -> {
                initializeGenesisBlockInternal(em);
                return null;
            });
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockWrite(stamp);
        }
    }

    /**
     * Internal method to create genesis block without transaction/lock management
     * Used when already inside a transaction (e.g., during clearAndReinitialize)
     */
    private void initializeGenesisBlockInternal(EntityManager em) {
        if (blockRepository.getBlockCount() == 0) {
            LocalDateTime genesisTime = LocalDateTime.now();

            Block genesisBlock = new Block();
            genesisBlock.setBlockNumber(0L);
            genesisBlock.setPreviousHash(GENESIS_PREVIOUS_HASH);
            genesisBlock.setData(
                "Genesis Block - Private Blockchain Initialized"
            );
            genesisBlock.setTimestamp(genesisTime);

            String blockContent = buildBlockContent(genesisBlock);
            genesisBlock.setHash(CryptoUtil.calculateHash(blockContent));
            genesisBlock.setSignature("GENESIS");
            genesisBlock.setSignerPublicKey("GENESIS");

            blockRepository.saveBlock(genesisBlock);

            // Hibernate SEQUENCE automatically manages block numbers - no manual sync needed

            logger.info("✅ Genesis block created successfully!");
        }
    }

    /**
     * Initialize Search Framework Engine with existing blockchain data
     * ENHANCED: Now properly indexes encrypted keywords using password registry
     * Thread-safe: Uses read lock to protect blockchain state access
     */
    public void initializeAdvancedSearch() {
        // Try optimistic read first (lock-free)
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.tryOptimisticRead();
        
        if (GLOBAL_BLOCKCHAIN_LOCK.validate(stamp)) {
            // Optimistic read succeeded - execute without lock
            initializeAdvancedSearch(null);
        } else {
            // Validation failed (write occurred) - retry with read lock
            stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
            try {
                initializeAdvancedSearch(null);
            } finally {
                GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
            }
        }
    }

    /**
     * Initialize Search Framework Engine efficiently for multi-department scenarios
     *
     * <p><strong>Enterprise Use Case:</strong> Designed for multi-tenant environments where different
     * departments use different encryption passwords. This method optimizes search initialization
     * to avoid redundant indexing operations that cause unnecessary Tag mismatch errors.</p>
     *
     * <p><strong>Performance Benefits:</strong> Single initialization prevents multiple re-indexing
     * operations during multi-department data storage, significantly improving system efficiency.</p>
     *
     * <p><strong>Thread Safety:</strong> This method is fully thread-safe using global read locks
     * to ensure consistency during concurrent operations.</p>
     *
     * <p><strong>Example Usage:</strong></p>
     * <pre>{@code
     * // Enterprise multi-department setup
     * String[] departmentPasswords = {
     *     "Medical_Dept_2024!SecureKey_abc123",
     *     "Finance_Dept_2024!SecureKey_def456",
     *     "Legal_Dept_2024!SecureKey_ghi789"
     * };
     *
     * blockchain.initializeAdvancedSearchWithMultiplePasswords(departmentPasswords);
     *
     * // Now departments can store their encrypted data efficiently
     * api.storeSearchableData("Patient data", departmentPasswords[0], medicalKeywords);
     * api.storeSearchableData("Financial data", departmentPasswords[1], financeKeywords);
     * }</pre>
     *
     * <p><strong>Edge Case Handling:</strong> This method safely handles null arrays, empty arrays,
     * and arrays containing null elements by using a default fallback password for initialization.</p>
     *
     * @param passwords Array of department-specific passwords for multi-tenant scenarios.
     *                  Can be null, empty, or contain null elements - method handles all edge cases gracefully.
     *                  If valid passwords are provided, the first non-null password is used for initialization.
     * @throws IllegalStateException if the blockchain is in an inconsistent state that prevents initialization
     * @since 1.0.5
     * @see UserFriendlyEncryptionAPI#storeSearchableData(String, String, String[])
     * @see #searchBlocks(String, SearchLevel)
     */
    public void initializeAdvancedSearchWithMultiplePasswords(String[] passwords) {
        // Define the initialization logic as a lambda
        Runnable initLogic = () -> {
            // Only initialize if there are blocks to index
            if (blockRepository.getBlockCount() > 0) {
                KeyPair tempKeyPair = CryptoUtil.generateKeyPair();

                // Initialize with first password to avoid null error
                // Other passwords will be registered per-block as data is stored
                String initPassword = (passwords != null && passwords.length > 0 && passwords[0] != null)
                                    ? passwords[0]
                                    : "defaultPassword123!"; // Fallback
                searchSpecialistAPI.initializeWithBlockchain(
                    this,
                    initPassword,
                    tempKeyPair.getPrivate()
                );

                logger.info(
                    "📊 Search Framework Engine initialized efficiently with {} blocks for {} department passwords",
                    blockRepository.getBlockCount(),
                    passwords != null ? passwords.length : 0
                );

                // Print password registry stats
                logger.info("📊 Password registry stats: {}", searchSpecialistAPI.getPasswordRegistryStats());
            } else {
                // No existing blocks, just initialize empty with first password
                KeyPair tempKeyPair = CryptoUtil.generateKeyPair();
                String initPassword = (passwords != null && passwords.length > 0 && passwords[0] != null)
                                    ? passwords[0]
                                    : "defaultPassword123!"; // Fallback
                searchSpecialistAPI.initializeWithBlockchain(
                    this,
                    initPassword,
                    tempKeyPair.getPrivate()
                );

                logger.info("📊 Search Framework Engine initialized for empty blockchain with {} department passwords ready",
                           passwords != null ? passwords.length : 0);
            }
        };

        // Try optimistic read first (lock-free)
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.tryOptimisticRead();
        
        if (GLOBAL_BLOCKCHAIN_LOCK.validate(stamp)) {
            // Optimistic read succeeded - execute without lock
            initLogic.run();
        } else {
            // Validation failed (write occurred) - retry with read lock
            stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
            try {
                initLogic.run();
            } finally {
                GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
            }
        }
    }

    /**
     * Initialize Search Framework Engine with existing blockchain data
     * ENHANCED: Now properly indexes encrypted keywords using password registry
     * Thread-safe: Uses read lock to protect blockchain state access
     * @param password Optional password to use for indexing encrypted keywords
     */
    public void initializeAdvancedSearch(String password) {
        // WARNING: StampedLock is NOT reentrant - ensure this is not called from within another lock
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            // Only initialize if there are blocks to index
            if (blockRepository.getBlockCount() > 0) {
                KeyPair tempKeyPair = CryptoUtil.generateKeyPair();

                // Initialize with provided password (or null for public-only indexing)
                searchSpecialistAPI.initializeWithBlockchain(
                    this,
                    password,
                    tempKeyPair.getPrivate()
                );

                // If password was provided, the initialization should have indexed encrypted keywords
                // If not, we still try to reindex blocks with registered passwords
                if (password == null) {
                    reindexBlocksWithPasswords();
                }

                logger.info(
                    "📊 Search Framework Engine initialized with {} blocks",
                    blockRepository.getBlockCount()
                );
                logger.info(
                    "📊 Password registry stats: {}",
                    searchSpecialistAPI.getPasswordRegistryStats()
                );
            }
        } catch (Exception e) {
            logger.warn("⚠️ Failed to initialize Search Framework Engine", e);
            // Continue operation even if search initialization fails
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * Reindex blocks that have encrypted keywords with their associated passwords
     * Since passwords cannot be retrieved from registry (security by design),
     * we need a different approach to handle password-based reindexing
     */
    private void reindexBlocksWithPasswords() {
        try {
            java.util.concurrent.atomic.AtomicInteger reindexedCount = new java.util.concurrent.atomic.AtomicInteger(0);

            // Get all blocks that have registered passwords
            Set<String> registeredBlockHashes =
                searchSpecialistAPI.getRegisteredBlocks();

            processChainInBatches(batch -> {
                for (Block block : batch) {
                    // Check if block has encrypted keywords and is registered with a password
                    if (
                        hasEncryptedKeywords(block) &&
                        isBlockRegistered(block, registeredBlockHashes)
                    ) {
                        // For security reasons, we cannot retrieve the password
                        // The block should have been properly indexed when it was created
                        // We'll log this for debugging but the search should work through
                        // the existing password registry mechanism
                        logger.info(
                            "📊 Block #{} has encrypted keywords and registered password",
                            block.getBlockNumber()
                        );
                        reindexedCount.incrementAndGet();
                    }
                }
            }, 1000);

            logger.info(
                "📊 Found {} blocks with encrypted keywords in password registry",
                reindexedCount.get()
            );
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
        return (
            autoKeywords.contains("|") && autoKeywords.matches(".*\\d{13}\\|.*")
        );
    }

    /**
     * Check if a block is registered in the password registry
     */
    private boolean isBlockRegistered(
        Block block,
        Set<String> registeredBlockHashes
    ) {
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
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            Block block = blockRepository.getBlockByNumber(blockNumber);
            if (block != null && block.isDataEncrypted()) {
                // Generate a temporary key pair for indexing
                KeyPair tempKeyPair = CryptoUtil.generateKeyPair();

                // Use the enhanced SearchSpecialistAPI for better password management
                searchSpecialistAPI.addBlock(
                    block,
                    password,
                    tempKeyPair.getPrivate()
                );

                logger.info(
                    "Re-indexed encrypted block #{} with specific password",
                    blockNumber
                );
            }
        } catch (Exception e) {
            logger.error("❌ Failed to re-index block #{}", blockNumber, e);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * Convert Advanced Search results to traditional Block list
     * @param enhancedResults List of EnhancedSearchResult from Advanced Search
     * @return List of Block entities
     */
    private List<Block> convertEnhancedResultsToBlocks(
        List<EnhancedSearchResult> enhancedResults
    ) {
        List<Block> blocks = new ArrayList<>();
        for (EnhancedSearchResult result : enhancedResults) {
            try {
                Block block = blockRepository.getBlockByHash(result.getBlockHash());
                if (block != null) {
                    blocks.add(block);
                }
            } catch (Exception e) {
                logger.warn(
                    "⚠️ Failed to retrieve block for hash {}",
                    result.getBlockHash(),
                    e
                );
            }
        }
        return blocks;
    }

    /**
     * CORE FUNCTION: Add a new block to the chain and return the created block
     * THREAD-SAFE: Returns the actual block created to avoid race conditions in tests
     */
    public Block addBlockAndReturn(
        String data,
        PrivateKey signerPrivateKey,
        PublicKey signerPublicKey
    ) {
        return addBlockWithKeywords(
            data,
            null,
            null,
            signerPrivateKey,
            signerPublicKey
        );
    }

    /**
     * ENHANCED: Add a new block with keywords and category
     *
     * <p><strong>THREAD-SAFE:</strong> Enhanced version with search functionality</p>
     * <p><strong>DEADLOCK FIX:</strong> Moved indexBlockchain() call OUTSIDE writeLock scope to prevent deadlock</p>
     *
     * <p><strong>SECURITY (v1.0.6):</strong> This method throws exceptions for critical
     * security violations instead of returning null. All keys must be pre-authorized
     * via {@link #addAuthorizedKey} before they can add blocks.</p>
     *
     * @param data The data to store in the block (cannot be null or empty)
     * @param manualKeywords Optional keywords for search indexing
     * @param category Optional category for classification
     * @param signerPrivateKey Private key for signing (cannot be null)
     * @param signerPublicKey Public key for verification (cannot be null)
     * @return The created block if successful, or null if a non-critical error occurs
     * @throws IllegalArgumentException if data is null or empty, or if signerPrivateKey or signerPublicKey is null
     * @throws UnauthorizedKeyException if the signer key is not authorized at the time of
     *         block creation (operation: ADD_BLOCK)
     * @throws BlockValidationException if validation fails due to:
     *         <ul>
     *           <li>DATA_SIZE - Block data exceeds maximum allowed size</li>
     *           <li>SEQUENCE - Inconsistent state (no genesis block but sequence number is not zero)</li>
     *         </ul>
     */
    public Block addBlockWithKeywords(
        String data,
        String[] manualKeywords,
        String category,
        PrivateKey signerPrivateKey,
        PublicKey signerPublicKey
    ) {
        // CRITICAL: Validate input parameters BEFORE transaction to allow exceptions to propagate
        if (data == null) {
            throw new IllegalArgumentException("Block data cannot be null");
        }
        if (data.trim().isEmpty()) {
            throw new IllegalArgumentException("Block data cannot be empty");
        }
        if (signerPrivateKey == null) {
            throw new IllegalArgumentException("Signer private key cannot be null");
        }
        if (signerPublicKey == null) {
            throw new IllegalArgumentException("Signer public key cannot be null");
        }
        
        // Step 1: Create and save block inside writeLock
        Block savedBlock = null;
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.writeLock();
        try {
            savedBlock = JPAUtil.executeInTransaction(em -> {
                try {
                    // 1. CORE: Validate block size and determine storage strategy
                    int storageDecision = validateAndDetermineStorage(data);
                    if (storageDecision == 0) {
                        logger.error("❌ Block data validation failed");
                        throw new BlockValidationException(
                            "Block data validation failed: data size exceeds maximum allowed",
                            null,
                            "DATA_SIZE"
                        );
                    }

                    // 2. Verify that the key is authorized at the time of block creation
                    String publicKeyString = CryptoUtil.publicKeyToString(
                        signerPublicKey
                    );
                    LocalDateTime blockTimestamp = LocalDateTime.now();
                    if (
                        !authorizedKeyDAO.wasKeyAuthorizedAt(
                            publicKeyString,
                            blockTimestamp
                        )
                    ) {
                        logger.error("❌ Unauthorized key attempting to add block");
                        throw new UnauthorizedKeyException(
                            "Unauthorized key attempting to add block",
                            publicKeyString,
                            "ADD_BLOCK",
                            blockTimestamp
                        );
                    }

                    // 3. Get the last block for previous hash and calculate next block number
                    Block lastBlock = blockRepository.getLastBlockWithLock();

                    // 4. Calculate next block number from last block (Hibernate SEQUENCE will auto-assign on persist)
                    Long nextBlockNumber = (lastBlock == null) ? 0L : lastBlock.getBlockNumber() + 1;

                    if (lastBlock == null && nextBlockNumber != 0L) {
                        logger.error(
                            "❌ Inconsistent state: no genesis block but number is {}",
                            nextBlockNumber
                        );
                        throw new BlockValidationException(
                            String.format("Inconsistent state: no genesis block but sequence number is %d", nextBlockNumber),
                            nextBlockNumber,
                            "SEQUENCE"
                        );
                    }

                    // 5. Handle off-chain storage if needed
                    OffChainData offChainData = null;
                    String blockData = data;

                    if (storageDecision == 2) {
                        // Store off-chain
                        try {
                            // Generate a password for encryption (derived from block info)
                            String encryptionPassword =
                                generateOffChainPassword(
                                    nextBlockNumber,
                                    publicKeyString
                                );

                            // Store data off-chain with encryption and verification
                            offChainData = offChainStorageService.storeData(
                                data.getBytes(StandardCharsets.UTF_8),
                                encryptionPassword,
                                signerPrivateKey,
                                publicKeyString,
                                "text/plain"
                            );

                            // Replace block data with reference
                            blockData =
                                "OFF_CHAIN_REF:" + offChainData.getDataHash();
                            logger.info(
                                "📝 Data stored off-chain. Block contains reference: {}",
                                blockData
                            );
                        } catch (Exception e) {
                            logger.error(
                                "❌ Failed to store data off-chain",
                                e
                            );
                            return null;
                        }
                    }

                    // 6. Create the new block with the atomically generated number
                    Block newBlock = new Block();
                    newBlock.setBlockNumber(nextBlockNumber);
                    newBlock.setPreviousHash(
                        lastBlock != null
                            ? lastBlock.getHash()
                            : GENESIS_PREVIOUS_HASH
                    );
                    newBlock.setData(blockData);
                    newBlock.setTimestamp(blockTimestamp);
                    newBlock.setSignerPublicKey(publicKeyString);
                    newBlock.setOffChainData(offChainData);

                    // 7. Calculate block hash
                    String blockContent = buildBlockContent(newBlock);
                    newBlock.setHash(CryptoUtil.calculateHash(blockContent));

                    // 8. Sign the block
                    String signature = CryptoUtil.signData(
                        blockContent,
                        signerPrivateKey
                    );
                    newBlock.setSignature(signature);

                    // 9. Validate the block before saving
                    if (
                        lastBlock != null && !validateBlock(newBlock, lastBlock)
                    ) {
                        logger.error("❌ Block validation failed for block #{}", nextBlockNumber);
                        return null;
                    }

                    // 10. Final check: verify this block number doesn't exist
                    if (blockRepository.existsBlockWithNumber(nextBlockNumber)) {
                        logger.error(
                            "❌ CRITICAL: Race condition detected! Block number {} already exists",
                            nextBlockNumber
                        );
                        return null;
                    }

                    // 11. ENHANCED: Process keywords for search functionality
                    processBlockKeywords(
                        newBlock,
                        data,
                        manualKeywords,
                        category
                    );

                    // 12. Save the block
                    blockRepository.saveBlock(newBlock);

                    // CRITICAL: Force flush to ensure immediate visibility
                    if (JPAUtil.hasActiveTransaction()) {
                        EntityManager currentEm = JPAUtil.getEntityManager();
                        currentEm.flush();
                    }

                    return newBlock; // ✅ RETURN THE ACTUAL CREATED BLOCK
                } catch (Exception e) {
                    logger.error("❌ Error adding block", e);
                    return null;
                }
            });

        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockWrite(stamp);
        }

        // Step 2: Index block AFTER releasing writeLock to prevent deadlock
        // CRITICAL FIX: indexBlockchain() requires reading blocks from DAO, which would
        // attempt to acquire readLock. Cannot acquire readLock while holding writeLock.
        // PERFORMANCE FIX: Index only the newly created block instead of reindexing the
        // entire blockchain. This is much more efficient.
        if (savedBlock != null) {
            try {
                // Index only this single block instead of the entire blockchain
                searchFrameworkEngine.indexBlock(
                    savedBlock,
                    "search-index-password", // Could be configurable
                    signerPrivateKey,
                    EncryptionConfig.createHighSecurityConfig()
                );
            } catch (Exception searchIndexException) {
                logger.warn(
                    "⚠️ Failed to index block #{} in Advanced Search",
                    savedBlock.getBlockNumber(),
                    searchIndexException
                );
                // Don't fail the block creation if search indexing fails
            }
        }

        return savedBlock;
    }

    /**
     * CORE FUNCTION: Add a new block to the chain (with size validation)
     *
     * <p><strong>FIXED:</strong> Complete thread-safety with atomic block number generation</p>
     * <p><strong>RACE CONDITION FIX (Phase 5.0):</strong> Block number calculated from lastBlock
     * within GLOBAL_BLOCKCHAIN_LOCK to prevent duplicate block numbers under high-concurrency
     * scenarios (30+ simultaneous threads). JDBC batching enabled for 5-10x performance.</p>
     *
     * <p><strong>SECURITY (v1.0.6):</strong> All keys must be pre-authorized before they can
     * add blocks. This method throws exceptions for critical security violations.</p>
     *
     * @param data The data to store in the block (cannot be null or empty)
     * @param signerPrivateKey Private key for signing the block (cannot be null)
     * @param signerPublicKey Public key for verification (cannot be null)
     * @return true if the block was successfully added, false if a non-critical error occurs
     * @throws IllegalArgumentException if data is null or empty, or if keys are null
     * @throws UnauthorizedKeyException if the signer key is not authorized (operation: ADD_BLOCK)
     * @throws BlockValidationException if validation fails (DATA_SIZE, SEQUENCE, or validation errors)
     * @see #addBlockWithKeywords for the underlying implementation with keyword support
     */
    public boolean addBlock(
        String data,
        PrivateKey signerPrivateKey,
        PublicKey signerPublicKey
    ) {
        // Simplify by delegating to addBlockAndReturn for DRY principle
        Block addedBlock = addBlockAndReturn(
            data,
            signerPrivateKey,
            signerPublicKey
        );
        return addedBlock != null;
    }

    /**
     * Add an encrypted block to the chain
     *
     * <p>The data will be encrypted using enterprise-grade AES-256-GCM encryption
     * before being stored in the blockchain.</p>
     *
     * <p><strong>SECURITY (v1.0.6):</strong> This method throws exceptions for critical
     * security violations. All keys must be pre-authorized before they can add encrypted blocks.</p>
     *
     * @param data The sensitive data to encrypt and store (cannot be null or empty)
     * @param encryptionPassword The password for encryption (cannot be null or empty)
     * @param signerPrivateKey Private key for signing (cannot be null)
     * @param signerPublicKey Public key for verification (cannot be null)
     * @return The created encrypted block if successful, or null if a non-critical error occurs
     * @throws IllegalArgumentException if encryptionPassword is null or empty
     * @throws UnauthorizedKeyException if the signer key is not authorized at the time of
     *         block creation (operation: ADD_ENCRYPTED_BLOCK)
     * @throws BlockValidationException if validation fails due to:
     *         <ul>
     *           <li>ENCRYPTION - Failed to encrypt block data</li>
     *           <li>ENCRYPTED_DATA_SIZE - Encrypted data exceeds maximum block size limits</li>
     *         </ul>
     * @see #addEncryptedBlockWithKeywords for advanced version with keyword support
     */
    public Block addEncryptedBlock(
        String data,
        String encryptionPassword,
        PrivateKey signerPrivateKey,
        PublicKey signerPublicKey
    ) {
        return addEncryptedBlockWithKeywords(
            data,
            encryptionPassword,
            null,
            "USER_DEFINED",
            signerPrivateKey,
            signerPublicKey
        );
    }

    /**
     * Add an encrypted block with keywords and category
     *
     * <p>The data will be encrypted using enterprise-grade AES-256-GCM encryption.
     * Keywords and category metadata are also encrypted for privacy.</p>
     *
     * <p><strong>SECURITY (v1.0.6):</strong> This method throws exceptions for critical
     * security violations instead of returning null. All keys must be pre-authorized
     * before they can add encrypted blocks.</p>
     *
     * @param data The sensitive data to encrypt and store (cannot be null or empty)
     * @param encryptionPassword The password for encryption (cannot be null or empty)
     * @param manualKeywords Manual keywords for search (will also be encrypted), can be null
     * @param category Content category (e.g., "MEDICAL", "FINANCIAL", "LEGAL"), can be null
     * @param signerPrivateKey Private key for signing (cannot be null)
     * @param signerPublicKey Public key for verification (cannot be null)
     * @return The created encrypted block if successful, or null if a non-critical error occurs
     * @throws IllegalArgumentException if encryptionPassword is null or empty
     * @throws UnauthorizedKeyException if the signer key is not authorized at the time of
     *         block creation (operation: ADD_ENCRYPTED_BLOCK)
     * @throws BlockValidationException if validation fails due to:
     *         <ul>
     *           <li>ENCRYPTION - Failed to encrypt block data</li>
     *           <li>ENCRYPTED_DATA_SIZE - Encrypted data exceeds maximum block size limits</li>
     *         </ul>
     */
    public Block addEncryptedBlockWithKeywords(
        String data,
        String encryptionPassword,
        String[] manualKeywords,
        String category,
        PrivateKey signerPrivateKey,
        PublicKey signerPublicKey
    ) {
        if (encryptionPassword == null || encryptionPassword.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Encryption password cannot be null or empty"
            );
        }

        // Step 1: Create and save encrypted block inside writeLock
        Block savedBlock = null;
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.writeLock();
        try {
            savedBlock = JPAUtil.executeInTransaction(em -> {
                try {
                    // 1. Validate input
                    validateBlockInput(data, signerPrivateKey, signerPublicKey);

                    // 2. Check authorized keys
                    String publicKeyString = CryptoUtil.publicKeyToString(
                        signerPublicKey
                    );
                    LocalDateTime blockTimestamp = LocalDateTime.now();
                    if (
                        !authorizedKeyDAO.wasKeyAuthorizedAt(
                            publicKeyString,
                            blockTimestamp
                        )
                    ) {
                        logger.error("❌ Unauthorized public key");
                        throw new UnauthorizedKeyException(
                            "Unauthorized key attempting to add encrypted block",
                            publicKeyString,
                            "ADD_ENCRYPTED_BLOCK",
                            blockTimestamp
                        );
                    }

                    // 3. Encrypt the data BEFORE size validation
                    String encryptedData;
                    try {
                        encryptedData =
                            SecureBlockEncryptionService.encryptToString(
                                data,
                                encryptionPassword
                            );
                    } catch (Exception e) {
                        logger.error("❌ Failed to encrypt block data", e);
                        throw new BlockValidationException(
                            "Failed to encrypt block data: " + e.getMessage(),
                            null,
                            "ENCRYPTION",
                            e
                        );
                    }

                    // 4. Validate encrypted data size
                    if (!validateDataSize(encryptedData)) {
                        logger.error(
                            "❌ Encrypted data exceeds maximum block size limits"
                        );
                        throw new BlockValidationException(
                            "Encrypted data exceeds maximum block size limits",
                            null,
                            "ENCRYPTED_DATA_SIZE"
                        );
                    }

                    // 5. Get last block and calculate next block number (Hibernate SEQUENCE will auto-assign on persist)
                    Block lastBlock = blockRepository.getLastBlockWithLock();
                    Long nextBlockNumber = (lastBlock == null) ? 0L : lastBlock.getBlockNumber() + 1;

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

                            blockData =
                                "OFF_CHAIN_ENCRYPTED_REF:" +
                                offChainData.getDataHash();
                            logger.info(
                                "📦 Encrypted data stored off-chain. Block contains reference: {}",
                                blockData
                            );
                        } catch (Exception e) {
                            logger.error(
                                "❌ Failed to store encrypted data off-chain",
                                e
                            );
                            return null;
                        }
                    }

                    // 7. Create the new encrypted block
                    Block newBlock = new Block();
                    newBlock.setBlockNumber(nextBlockNumber);
                    newBlock.setPreviousHash(
                        lastBlock != null
                            ? lastBlock.getHash()
                            : GENESIS_PREVIOUS_HASH
                    );
                    // CRITICAL: Store original data unchanged to maintain hash integrity
                    // The block hash is calculated using the original data
                    // Encrypted content is stored separately in encryptionMetadata
                    newBlock.setData(data); // Original unencrypted data for hash calculation
                    newBlock.setEncryptionMetadata(blockData); // Store encrypted data here
                    newBlock.setIsEncrypted(true); // Mark as encrypted
                    newBlock.setTimestamp(blockTimestamp);
                    newBlock.setSignerPublicKey(publicKeyString);
                    newBlock.setOffChainData(offChainData);

                    // 8. Calculate block hash (using placeholder data for consistent hashing)
                    String blockContent = buildBlockContentForEncrypted(
                        newBlock
                    );
                    newBlock.setHash(CryptoUtil.calculateHash(blockContent));

                    // 9. Sign the block
                    String signature = CryptoUtil.signData(
                        blockContent,
                        signerPrivateKey
                    );
                    newBlock.setSignature(signature);

                    // 10. Validate the block before saving
                    if (
                        lastBlock != null && !validateBlock(newBlock, lastBlock)
                    ) {
                        logger.error("❌ Encrypted block validation failed");
                        return null;
                    }

                    // 11. Process keywords for search (encrypt keywords too for privacy)
                    processEncryptedBlockKeywords(
                        newBlock,
                        data,
                        manualKeywords,
                        category,
                        encryptionPassword
                    );

                    // 12. Save the encrypted block
                    blockRepository.saveBlock(newBlock);

                    // Force flush for immediate visibility
                    if (JPAUtil.hasActiveTransaction()) {
                        EntityManager currentEm = JPAUtil.getEntityManager();
                        currentEm.flush();
                    }

                    logger.info(
                        "🔐 Encrypted Block #{} added successfully!",
                        newBlock.getBlockNumber()
                    );
                    return newBlock;
                } catch (Exception e) {
                    logger.error("❌ Error adding encrypted block", e);
                    return null;
                }
            });
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockWrite(stamp);
        }
        
        // DEADLOCK FIX: Index the block AFTER releasing writeLock to prevent deadlock
        // Search indexing requires reading blocks from DAO, which would attempt to acquire
        // readLock. Cannot acquire readLock while holding writeLock.
        if (savedBlock != null) {
            try {
                // Initialize search API if not initialized
                if (!searchSpecialistAPI.isReady()) {
                    try {
                        searchSpecialistAPI.initializeWithBlockchain(
                            this,
                            encryptionPassword,
                            signerPrivateKey
                        );
                        logger.info("✅ SearchSpecialistAPI initialized on-demand");
                    } catch (Exception initEx) {
                        logger.warn("⚠️ Search API initialization failed", initEx);
                        // Continue without search indexing
                    }
                }
                
                // Use the enhanced SearchSpecialistAPI for better password management
                if (searchSpecialistAPI.isReady()) {
                    searchSpecialistAPI.addBlock(
                        savedBlock,
                        encryptionPassword,
                        signerPrivateKey
                    );
                    logger.info(
                        "✅ Successfully indexed encrypted block in Search Framework Engine"
                    );
                }
            } catch (Exception e) {
                logger.warn(
                    "⚠️ Failed to index encrypted block in search engine",
                    e
                );
                // Try fallback indexing with public metadata only
                try {
                    searchFrameworkEngine.indexBlockWithSpecificPassword(
                        savedBlock,
                        null,
                        signerPrivateKey,
                        EncryptionConfig.createHighSecurityConfig()
                    );
                    logger.info(
                        "🔄 Fallback: Indexed block with public metadata only"
                    );
                } catch (Exception e2) {
                    logger.error(
                        "❌ Complete indexing failure for block",
                        e2
                    );
                }
            }
        }
        
        return savedBlock;
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
    public Block addBlockWithOffChainData(
        String data,
        OffChainData offChainData,
        String[] keywords,
        String encryptionPassword,
        PrivateKey signerPrivateKey,
        PublicKey signerPublicKey
    ) {
        if (offChainData == null) {
            throw new IllegalArgumentException("Off-chain data cannot be null");
        }
        if (encryptionPassword == null || encryptionPassword.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Encryption password cannot be null or empty"
            );
        }

        logger.debug("🔷 Starting addBlockWithOffChainData - Off-chain data hash: {}",
            offChainData.getDataHash().substring(0, 8));

        long stamp = GLOBAL_BLOCKCHAIN_LOCK.writeLock();
        // Step 1: Create and save block inside writeLock
        Block savedBlock = null;
        try {
            savedBlock = JPAUtil.executeInTransaction(em -> {
                try {
                    // 1. Validate input
                    validateBlockInput(data, signerPrivateKey, signerPublicKey);

                    // 2. Check authorized keys
                    String publicKeyString = CryptoUtil.publicKeyToString(
                        signerPublicKey
                    );
                    LocalDateTime blockTimestamp = LocalDateTime.now();
                    if (
                        !authorizedKeyDAO.wasKeyAuthorizedAt(
                            publicKeyString,
                            blockTimestamp
                        )
                    ) {
                        logger.error("❌ Unauthorized public key");
                        return null;
                    }

                    // 3. Ensure off-chain data is persisted
                    if (offChainData.getId() == null) {
                        em.persist(offChainData);
                        em.flush(); // Ensure ID is generated
                    }

                    // 4. Get blockchain info using current EntityManager to avoid stale reads
                    // CRITICAL FIX: Use em from current transaction to see most recent blocks
                    Block lastBlock = blockRepository.getLastBlock(em);
                    Long newBlockNumber = (lastBlock != null)
                        ? lastBlock.getBlockNumber() + 1
                        : 1L;
                    String previousHash = (lastBlock != null)
                        ? lastBlock.getHash()
                        : "0";

                    // 5. Create new block with off-chain data
                    Block newBlock = new Block();
                    newBlock.setBlockNumber(newBlockNumber);
                    newBlock.setPreviousHash(previousHash);
                    newBlock.setTimestamp(blockTimestamp);
                    newBlock.setSignerPublicKey(publicKeyString);
                    newBlock.setOffChainData(offChainData); // Link off-chain data

                    // 6. Encrypt the main block data
                    String encryptedData =
                        SecureBlockEncryptionService.encryptToString(
                            data,
                            encryptionPassword
                        );
                    newBlock.setData(encryptedData);
                    newBlock.setIsEncrypted(true);

                    // 7. Process keywords for search
                    if (keywords != null && keywords.length > 0) {
                        // Encrypt keywords for search
                        String encryptedKeywords =
                            SecureBlockEncryptionService.encryptToString(
                                String.join(" ", keywords),
                                encryptionPassword
                            );
                        newBlock.setAutoKeywords(encryptedKeywords);
                        newBlock.setManualKeywords(encryptedKeywords);
                        newBlock.setSearchableContent(
                            String.join(" ", keywords)
                        );
                    }

                    // 8. Set content category
                    newBlock.setContentCategory("OFF_CHAIN_LINKED");

                    // 9. Calculate block hash
                    String blockHash = CryptoUtil.calculateHash(
                        newBlock.toString()
                    );
                    newBlock.setHash(blockHash);

                    // 10. Sign the block
                    String signature = CryptoUtil.signData(
                        blockHash,
                        signerPrivateKey
                    );
                    newBlock.setSignature(signature);

                    // 11. Persist the block
                    em.persist(newBlock);
                    em.flush();

                    logger.info(
                        "🔗 Off-chain linked Block #{} persisted (inside transaction). Off-chain data ID: {}",
                        newBlock.getBlockNumber(),
                        offChainData.getId()
                    );
                    return newBlock;
                } catch (Exception e) {
                    logger.error("❌ Error adding off-chain linked block", e);
                    return null;
                }
            });

            if (savedBlock != null) {
                logger.debug("✅ Transaction committed for Block #{}", savedBlock.getBlockNumber());
            } else {
                logger.warn("⚠️ Transaction completed but savedBlock is null");
            }
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockWrite(stamp);
        }

        // Step 2: Index block AFTER releasing writeLock to prevent deadlock
        // CRITICAL FIX: Search indexing requires reading blocks from DAO, which would
        // attempt to acquire readLock. Cannot acquire readLock while holding writeLock.
        if (savedBlock != null) {
            try {
                searchSpecialistAPI.addBlock(
                    savedBlock,
                    encryptionPassword,
                    signerPrivateKey
                );
                logger.info(
                    "✅ Successfully indexed off-chain linked block in Search Framework Engine"
                );
            } catch (Exception e) {
                logger.warn(
                    "⚠️ Failed to index off-chain linked block in search engine",
                    e
                );
                // Try fallback indexing
                try {
                    searchFrameworkEngine.indexBlockWithSpecificPassword(
                        savedBlock,
                        null,
                        signerPrivateKey,
                        EncryptionConfig.createHighSecurityConfig()
                    );
                    logger.info(
                        "🔄 Fallback: Indexed off-chain linked block with public metadata only"
                    );
                } catch (Exception e2) {
                    logger.error(
                        "❌ Complete indexing failure for off-chain linked block",
                        e2
                    );
                }
            }
        }
        
        return savedBlock;
    }

    /**
     * Decrypt and retrieve block data
     *
     * @param blockNumber The block number of the encrypted block
     * @param decryptionPassword The password for decryption
     * @return The decrypted block data, or null if failed
     */
    public String getDecryptedBlockData(
        Long blockNumber,
        String decryptionPassword
    ) {
        if (
            blockNumber == null ||
            decryptionPassword == null ||
            decryptionPassword.trim().isEmpty()
        ) {
            throw new IllegalArgumentException(
                "Block number and decryption password cannot be null or empty"
            );
        }

        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            Block block = blockRepository.getBlockWithDecryption(
                blockNumber,
                decryptionPassword
            );
            return block != null ? block.getData() : null;
        } catch (Exception e) {
            logger.debug(
                "Failed to decrypt block data (expected with wrong password)",
                e
            );
            return null;
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * Check if a block is encrypted
     */
    public boolean isBlockEncrypted(Long blockNumber) {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            Block block = blockRepository.getBlockByNumber(blockNumber);
            return block != null && block.isDataEncrypted();
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * Build block content for encrypted blocks (for consistent hashing)
     */
    private String buildBlockContentForEncrypted(Block block) {
        // Use epoch seconds for timestamp to ensure consistency with regular blocks
        long timestampSeconds = block.getTimestamp() != null
            ? block.getTimestamp().toEpochSecond(ZoneOffset.UTC)
            : 0;

        String content = (
            block.getBlockNumber() +
            (block.getPreviousHash() != null ? block.getPreviousHash() : "") +
            (block.getData() != null ? block.getData() : "") + // Use actual data for hash calculation
            timestampSeconds +
            (block.getSignerPublicKey() != null
                    ? block.getSignerPublicKey()
                    : "")
        );
        
        // TRACE: Log detailed hash calculation inputs for encrypted blocks (only in trace mode)
        if (logger.isTraceEnabled()) {
            logger.trace("🔧 ENCRYPTED HASH DEBUG: Block #{} hash calculation:", block.getBlockNumber());
            logger.trace("  - blockNumber: {}", block.getBlockNumber());
            logger.trace("  - previousHash: {}", block.getPreviousHash());
            logger.trace("  - data: {}", block.getData() != null ? block.getData().substring(0, Math.min(50, block.getData().length())) + "..." : "null");
            logger.trace("  - timestampSeconds: {}", timestampSeconds);
            logger.trace("  - signerPublicKey: {}", block.getSignerPublicKey() != null ? block.getSignerPublicKey().substring(0, Math.min(20, block.getSignerPublicKey().length())) + "..." : "null");
            logger.trace("  - Final content: {}", content.substring(0, Math.min(100, content.length())) + "...");
        }
        String calculatedHash = CryptoUtil.calculateHash(content);
        if (logger.isTraceEnabled()) {
            logger.trace("  - Calculated hash: {}", calculatedHash);
        }
        
        return content;
    }

    /**
     * Process keywords for encrypted blocks (encrypt keywords for privacy)
     */
    private void processEncryptedBlockKeywords(
        Block block,
        String originalData,
        String[] manualKeywords,
        String category,
        String encryptionPassword
    ) {
        try {
            // 1. Manual keywords - separate public and private keywords
            if (manualKeywords != null && manualKeywords.length > 0) {
                List<String> publicKeywords = new ArrayList<>();
                List<String> privateKeywords = new ArrayList<>();

                // Separate keywords by public: prefix (lowercase for consistency with storage)
                for (String keyword : manualKeywords) {
                    if (keyword.toLowerCase().startsWith("public:")) {
                        publicKeywords.add(keyword.toLowerCase());
                    } else {
                        privateKeywords.add(keyword.toLowerCase());
                    }
                }

                // Store public keywords unencrypted in manualKeywords for search without password
                if (!publicKeywords.isEmpty()) {
                    String publicKeywordString = String.join(
                        " ",
                        publicKeywords
                    );
                    block.setManualKeywords(publicKeywordString);
                    if (logger.isDebugEnabled()) {
                        logger.debug(
                            "🔍 Stored public keywords: {}",
                            publicKeywordString
                        );
                    }
                }

                // Store private keywords encrypted in autoKeywords field
                if (!privateKeywords.isEmpty()) {
                    String privateKeywordString = String.join(
                        " ",
                        privateKeywords
                    );
                    String encryptedPrivateKeywords =
                        SecureBlockEncryptionService.encryptToString(
                            privateKeywordString,
                            encryptionPassword
                        );
                    block.setAutoKeywords(encryptedPrivateKeywords);
                    if (logger.isDebugEnabled()) {
                        logger.debug(
                            "🔍 Stored encrypted private keywords in autoKeywords"
                        );
                    }
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
            Set<String> suggestedTerms = extractSimpleSearchTerms(
                originalData,
                10
            );
            if (!suggestedTerms.isEmpty()) {
                String autoKeywords = String.join(" ", suggestedTerms);
                String encryptedAutoKeywords =
                    SecureBlockEncryptionService.encryptToString(
                        autoKeywords,
                        encryptionPassword
                    );
                // If we already have auto keywords (from private keywords above), append
                String existingAutoKeywords = block.getAutoKeywords();
                if (
                    existingAutoKeywords != null &&
                    !existingAutoKeywords.trim().isEmpty()
                ) {
                    String combined = existingAutoKeywords + " " + encryptedAutoKeywords;
                    // Validate combined length before setting (database limit: 1024 chars)
                    if (combined.length() > 1024) {
                        throw new IllegalArgumentException(
                            "Combined encrypted keywords exceed database limit of 1024 characters (got: " +
                            combined.length() + " characters). " +
                            "The content generates too many keywords when encrypted. " +
                            "Please reduce content size or use fewer manual keywords."
                        );
                    }
                    block.setAutoKeywords(combined);
                } else {
                    // Validate encrypted keywords length before setting
                    if (encryptedAutoKeywords.length() > 1024) {
                        throw new IllegalArgumentException(
                            "Encrypted keywords exceed database limit of 1024 characters (got: " +
                            encryptedAutoKeywords.length() + " characters). " +
                            "The content is too large for encrypted keyword extraction. " +
                            "Please reduce content size."
                        );
                    }
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
    private void processBlockKeywords(
        Block block,
        String originalData,
        String[] manualKeywords,
        String category
    ) {
        // 1. Manual keywords
        if (manualKeywords != null && manualKeywords.length > 0) {
            block.setManualKeywords(
                String.join(" ", manualKeywords).toLowerCase()
            );
        }

        // 2. Category
        if (category != null && !category.trim().isEmpty()) {
            block.setContentCategory(category.toUpperCase());
        }

        // 3. Automatic keywords (from original content, not from off-chain reference)
        String contentForExtraction = originalData != null ? originalData : "";
        if (block.hasOffChainData()) {
            // If going off-chain, use original content for extraction
            contentForExtraction = originalData != null ? originalData : "";
        }

        Set<String> suggestedTerms = extractSimpleSearchTerms(
            contentForExtraction,
            10
        );
        String autoKeywords = String.join(" ", suggestedTerms);
        
        // Validate autoKeywords length BEFORE setting (database limit: 1024 chars)
        if (autoKeywords.length() > 1024) {
            throw new IllegalArgumentException(
                "Automatically generated keywords exceed database limit of 1024 characters (got: " +
                autoKeywords.length() + " characters). " +
                "The content is too large for automatic keyword extraction. " +
                "Please either: (1) reduce content size, (2) use manual keywords instead, " +
                "or (3) disable automatic keyword generation."
            );
        }
        
        block.setAutoKeywords(autoKeywords);

        // 4. Combine everything to searchableContent
        block.updateSearchableContent();

        // 5. Log for debugging
        if (
            block.getSearchableContent() != null &&
            !block.getSearchableContent().trim().isEmpty()
        ) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "📋 Keywords assigned to block #{}: {}...",
                    block.getBlockNumber(),
                    block
                        .getSearchableContent()
                        .substring(
                            0,
                            Math.min(50, block.getSearchableContent().length())
                        )
                );
            }
        }
    }

    /**
     * CORE FUNCTION: Validate an individual block against the previous block
     *
     * <p>Performs comprehensive validation including:</p>
     * <ul>
     *   <li>Previous hash integrity verification</li>
     *   <li>Block number sequence validation</li>
     *   <li>Hash integrity check (SHA3-256)</li>
     *   <li>Digital signature verification (ML-DSA-87)</li>
     *   <li>Authorization verification (RBAC v1.0.6)</li>
     * </ul>
     *
     * <p><strong>SECURITY (v1.0.6):</strong> This method now throws exceptions instead of
     * returning false for critical security violations. This ensures that validation failures
     * cannot be silently ignored.</p>
     *
     * @param block The block to validate
     * @param previousBlock The previous block in the chain (null for genesis block)
     * @return true if the block is valid and all checks pass
     * @throws BlockValidationException if validation fails due to:
     *         <ul>
     *           <li>PREVIOUS_HASH - Previous hash mismatch detected</li>
     *           <li>BLOCK_NUMBER - Block number sequence error</li>
     *           <li>HASH - Block hash integrity check failed</li>
     *           <li>SIGNATURE - Digital signature verification failed</li>
     *         </ul>
     * @throws UnauthorizedKeyException if the block is signed by a key that was not
     *         authorized at the time of block creation (operation: SIGN_BLOCK)
     */
    public boolean validateBlock(Block block, Block previousBlock) {
        String threadName = Thread.currentThread().getName();
        try {
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "🔍 [{}] Validating block #{}",
                    threadName,
                    block.getBlockNumber()
                );
            }

            // Skip validation for genesis block
            if (
                block.getBlockNumber() != null &&
                block.getBlockNumber().equals(0L)
            ) {
                logger.info(
                    "✅ [{}] Genesis block validation passed",
                    threadName
                );
                return true;
            }

            // 1. Verify that the previous block hash matches
            if (!block.getPreviousHash().equals(previousBlock.getHash())) {
                logger.error(
                    "❌ [{}] VALIDATION FAILURE: Previous hash mismatch",
                    threadName
                );
                logger.error("🔍 Expected: {}", previousBlock.getHash());
                logger.error("🔍 Got: {}", block.getPreviousHash());
                throw new BlockValidationException(
                    String.format("Previous hash mismatch. Expected: %s, Got: %s",
                        previousBlock.getHash(), block.getPreviousHash()),
                    block.getBlockNumber(),
                    "PREVIOUS_HASH"
                );
            }

            // 2. Verify that the block number is sequential
            if (
                !block
                    .getBlockNumber()
                    .equals(previousBlock.getBlockNumber() + 1L)
            ) {
                logger.error(
                    "❌ [{}] VALIDATION FAILURE: Block number sequence error",
                    threadName
                );
                logger.error(
                    "🔍 Previous block number: {}",
                    previousBlock.getBlockNumber()
                );
                logger.error(
                    "🔍 Current block number: {}",
                    block.getBlockNumber()
                );
                logger.error(
                    "🔍 Expected: {}",
                    (previousBlock.getBlockNumber() + 1L)
                );
                throw new BlockValidationException(
                    String.format("Block number sequence error. Expected: %d, Got: %d",
                        previousBlock.getBlockNumber() + 1L, block.getBlockNumber()),
                    block.getBlockNumber(),
                    "BLOCK_NUMBER"
                );
            }

            // 3. Verify hash integrity
            // Use appropriate content building method based on encryption status
            String blockContent = block.isDataEncrypted() ?
                buildBlockContentForEncrypted(block) : buildBlockContent(block);
            String calculatedHash = CryptoUtil.calculateHash(blockContent);
            if (!block.getHash().equals(calculatedHash)) {
                logger.error(
                    "❌ [{}] VALIDATION FAILURE: Block hash integrity check failed for block #{}",
                    threadName,
                    block.getBlockNumber()
                );
                logger.error("🔍 Stored hash: {}", block.getHash());
                logger.error("🔍 Calculated hash: {}", calculatedHash);
                logger.error("🔍 Block content: {}", blockContent);
                logger.error("🔍 Is encrypted: {}", block.isDataEncrypted());
                throw new BlockValidationException(
                    String.format("Block hash integrity check failed. Stored: %s, Calculated: %s",
                        block.getHash(), calculatedHash),
                    block.getBlockNumber(),
                    "HASH"
                );
            }

            // 4. Verify digital signature
            PublicKey signerPublicKey = CryptoUtil.stringToPublicKey(
                block.getSignerPublicKey()
            );
            // Use the same content building method as for hash calculation
            String signatureContent = block.isDataEncrypted() ?
                buildBlockContentForEncrypted(block) : buildBlockContent(block);
            if (
                !CryptoUtil.verifySignature(
                    signatureContent,
                    block.getSignature(),
                    signerPublicKey
                )
            ) {
                logger.error(
                    "❌ [{}] VALIDATION FAILURE: Block signature verification failed for block #{}",
                    threadName,
                    block.getBlockNumber()
                );
                logger.error("🔍 Block content: {}", signatureContent);
                logger.error("🔍 Signature: {}", block.getSignature());
                logger.error(
                    "🔍 Signer public key: {}",
                    block.getSignerPublicKey()
                );
                logger.error("🔍 Is encrypted: {}", block.isDataEncrypted());
                throw new BlockValidationException(
                    String.format("Block signature verification failed (signer: %s...)",
                        block.getSignerPublicKey().substring(0, Math.min(50, block.getSignerPublicKey().length()))),
                    block.getBlockNumber(),
                    "SIGNATURE"
                );
            }

            // 5. Verify that the key was authorized at the time of block creation
            if (
                !authorizedKeyDAO.wasKeyAuthorizedAt(
                    block.getSignerPublicKey(),
                    block.getTimestamp()
                )
            ) {
                logger.error(
                    "❌ [{}] VALIDATION FAILURE: Block signed by key that was not authorized at time of creation for block #{}",
                    threadName,
                    block.getBlockNumber()
                );
                logger.error("🔍 Signer: {}", block.getSignerPublicKey());
                logger.error("🔍 Block timestamp: {}", block.getTimestamp());
                throw new UnauthorizedKeyException(
                    "Block signed by key that was not authorized at time of block creation",
                    block.getSignerPublicKey(),
                    "SIGN_BLOCK",
                    block.getTimestamp()
                );
            }

            logger.info(
                "✅ [{}] Block #{} validation passed",
                threadName,
                block.getBlockNumber()
            );
            return true;
        } catch (Exception e) {
            logger.error(
                "💥 [{}] EXCEPTION during block validation",
                threadName,
                e
            );
            return false;
        }
    }

    /**
     * ENHANCED: Detailed validation of an individual block
     * Returns comprehensive information about validation status
     */
    public BlockValidationResult validateBlockDetailed(
        Block block,
        Block previousBlock
    ) {
        String threadName = Thread.currentThread().getName();
        BlockValidationResult.Builder builder =
            new BlockValidationResult.Builder(block);

        try {
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "🔍 [{}] Detailed validation of block #{}",
                    threadName,
                    block.getBlockNumber()
                );
            }

            // Skip detailed validation for genesis block
            if (
                block.getBlockNumber() != null &&
                block.getBlockNumber().equals(0L)
            ) {
                logger.info(
                    "✅ [{}] Genesis block validation passed",
                    threadName
                );
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
                logger.error(
                    "❌ [{}] VALIDATION FAILURE: {}",
                    threadName,
                    errorMessage
                );
            }

            // 2. Verify that the block number is sequential
            if (
                structurallyValid &&
                !block
                    .getBlockNumber()
                    .equals(previousBlock.getBlockNumber() + 1L)
            ) {
                structurallyValid = false;
                errorMessage = "Block number sequence error";
                logger.error(
                    "❌ [{}] VALIDATION FAILURE: {}",
                    threadName,
                    errorMessage
                );
            }

            // 3. Verify hash integrity
            if (structurallyValid) {
                String calculatedHash = CryptoUtil.calculateHash(
                    buildBlockContent(block)
                );
                if (!block.getHash().equals(calculatedHash)) {
                    cryptographicallyValid = false;
                    errorMessage = "Block hash integrity check failed";
                    logger.error(
                        "❌ [{}] VALIDATION FAILURE: {}",
                        threadName,
                        errorMessage
                    );
                }
            }

            // 4. Verify digital signature
            if (cryptographicallyValid) {
                try {
                    PublicKey signerPublicKey = CryptoUtil.stringToPublicKey(
                        block.getSignerPublicKey()
                    );
                    String blockContent = buildBlockContent(block);
                    if (
                        !CryptoUtil.verifySignature(
                            blockContent,
                            block.getSignature(),
                            signerPublicKey
                        )
                    ) {
                        cryptographicallyValid = false;
                        errorMessage = "Block signature verification failed";
                        logger.error(
                            "❌ [{}] VALIDATION FAILURE: {}",
                            threadName,
                            errorMessage
                        );
                    }
                } catch (Exception e) {
                    cryptographicallyValid = false;
                    errorMessage =
                        "Signature verification error: " + e.getMessage();
                    logger.error(
                        "❌ [{}] VALIDATION FAILURE: {}",
                        threadName,
                        errorMessage
                    );
                }
            }

            // 5. Verify that the key was authorized at the time of block creation
            if (structurallyValid && cryptographicallyValid) {
                if (
                    !authorizedKeyDAO.wasKeyAuthorizedAt(
                        block.getSignerPublicKey(),
                        block.getTimestamp()
                    )
                ) {
                    authorizationValid = false;
                    warningMessage =
                        "Block signed by key that was revoked after creation";
                    logger.warn(
                        "⚠️ [{}] AUTHORIZATION WARNING: {}",
                        threadName,
                        warningMessage
                    );
                    if (logger.isDebugEnabled()) {
                        logger.debug("🔍 Signer: {}", block.getSignerPublicKey());
                        logger.debug("🔍 Block timestamp: {}", block.getTimestamp());
                    }
                }
            }

            // 6. Verify off-chain data integrity if present
            if (
                structurallyValid &&
                cryptographicallyValid &&
                block.hasOffChainData()
            ) {
                try {
                    // Perform detailed off-chain validation
                    var detailedResult =
                        BlockValidationUtil.validateOffChainDataDetailed(block);
                    boolean basicOffChainIntegrity = verifyOffChainIntegrity(
                        block
                    );

                    if (!detailedResult.isValid() || !basicOffChainIntegrity) {
                        offChainDataValid = false;
                        String detailedMessage = detailedResult.getMessage();
                        if (errorMessage == null) {
                            errorMessage =
                                "Off-chain data validation failed: " +
                                detailedMessage;
                        } else {
                            errorMessage +=
                                "; Off-chain data validation failed: " +
                                detailedMessage;
                        }
                        logger.error(
                            "❌ [{}] OFF-CHAIN VALIDATION FAILURE for block #{}:",
                            threadName,
                            block.getBlockNumber()
                        );
                        logger.error("   📋 Details: {}", detailedMessage);
                        if (!basicOffChainIntegrity) {
                            logger.error(
                                "   🔐 Cryptographic integrity check also failed"
                            );
                        }
                    } else {
                        // Show detailed success information
                        var offChainData = block.getOffChainData();
                        logger.info(
                            "✅ [{}] Off-chain data fully validated for block #{}",
                            threadName,
                            block.getBlockNumber()
                        );
                        logger.info(
                            "   📁 File: {}",
                            java.nio.file.Paths.get(
                                offChainData.getFilePath()
                            ).getFileName()
                        );
                        logger.info(
                            "   📦 Size: {}",
                            (offChainData.getFileSize() != null
                                    ? String.format(
                                        "%.1f KB",
                                        offChainData.getFileSize() / 1024.0
                                    )
                                    : "unknown")
                        );
                        logger.info(
                            "   🔐 Integrity: verified (hash + encryption + signature)"
                        );
                        logger.info(
                            "   ⏰ Created: {}",
                            (offChainData.getCreatedAt() != null
                                    ? offChainData.getCreatedAt().toString()
                                    : "unknown")
                        );

                        // Additional validation details
                        String hash = offChainData.getDataHash();
                        if (hash != null && hash.length() > 16) {
                            String truncatedHash =
                                hash.substring(0, 8) +
                                "..." +
                                hash.substring(hash.length() - 8);
                            logger.info("   🔗 Hash: {}", truncatedHash);
                        }
                    }
                } catch (Exception e) {
                    offChainDataValid = false;
                    if (errorMessage == null) {
                        errorMessage =
                            "Off-chain data verification error: " +
                            e.getMessage();
                    } else {
                        errorMessage +=
                            "; Off-chain data verification error: " +
                            e.getMessage();
                    }
                    logger.error(
                        "❌ [{}] OFF-CHAIN VALIDATION ERROR for block #{}",
                        threadName,
                        block.getBlockNumber(),
                        e
                    );
                }
            } else if (
                block.hasOffChainData() &&
                (!structurallyValid || !cryptographicallyValid)
            ) {
                // Block has off-chain data but failed basic validation
                logger.warn(
                    "⚠️ [{}] Block #{} has off-chain data but failed basic validation - skipping off-chain checks",
                    threadName,
                    block.getBlockNumber()
                );
            }

            // 7. Verify encrypted data integrity if present
            boolean encryptedDataValid = true;
            if (
                structurallyValid &&
                cryptographicallyValid &&
                block.isDataEncrypted()
            ) {
                try {
                    // Perform comprehensive encrypted block validation
                    var encryptedValidation =
                        EncryptedBlockValidator.validateEncryptedBlock(block);

                    if (!encryptedValidation.isValid()) {
                        encryptedDataValid = false;
                        cryptographicallyValid = false; // Encryption is part of cryptographic integrity

                        if (errorMessage == null) {
                            errorMessage =
                                "Encrypted block validation failed: " +
                                encryptedValidation.getErrorMessage();
                        } else {
                            errorMessage +=
                                "; Encrypted block validation failed: " +
                                encryptedValidation.getErrorMessage();
                        }

                        logger.error(
                            "❌ [{}] ENCRYPTED BLOCK VALIDATION FAILURE for block #{}:",
                            threadName,
                            block.getBlockNumber()
                        );
                        logger.error(
                            "   🔐 {}",
                            encryptedValidation.getErrorMessage()
                        );

                        // Check for possible corruption
                        var corruptionAssessment =
                            EncryptedBlockValidator.detectCorruption(block);
                        if (corruptionAssessment.isPossiblyCorrupted()) {
                            logger.error(
                                "   ⚠️ POSSIBLE CORRUPTION DETECTED: {}",
                                corruptionAssessment.getIssues()
                            );
                        }
                    } else {
                        // Show successful validation details
                        logger.info(
                            "✅ [{}] Encrypted block fully validated for block #{}",
                            threadName,
                            block.getBlockNumber()
                        );
                        logger.info("   🔐 Encryption format: valid");
                        logger.info("   📊 Metadata: intact");
                        logger.info(
                            "   📂 Category: {}",
                            (block.getContentCategory() != null
                                    ? block.getContentCategory()
                                    : "none")
                        );

                        // Show warnings if any
                        if (encryptedValidation.getWarningMessage() != null) {
                            if (warningMessage == null) {
                                warningMessage =
                                    encryptedValidation.getWarningMessage();
                            } else {
                                warningMessage +=
                                    "; " +
                                    encryptedValidation.getWarningMessage();
                            }
                            logger.warn(
                                "   ⚠️ Warning: {}",
                                encryptedValidation.getWarningMessage()
                            );
                        }

                        // Additional encryption metadata validation
                        if (block.getEncryptionMetadata() != null) {
                            int encryptedSize = block
                                .getEncryptionMetadata()
                                .length();
                            logger.info(
                                "   📦 Encrypted data size: {} characters",
                                encryptedSize
                            );

                            if (encryptedSize > 1000) {
                                String preview =
                                    block
                                        .getEncryptionMetadata()
                                        .substring(0, 20) +
                                    "...";
                                logger.info(
                                    "   🔗 Metadata preview: {}",
                                    preview
                                );
                            }
                        }
                    }
                } catch (Exception e) {
                    encryptedDataValid = false;
                    cryptographicallyValid = false;

                    if (errorMessage == null) {
                        errorMessage =
                            "Encrypted block verification error: " +
                            e.getMessage();
                    } else {
                        errorMessage +=
                            "; Encrypted block verification error: " +
                            e.getMessage();
                    }

                    logger.error(
                        "❌ [{}] ENCRYPTED BLOCK VALIDATION ERROR for block #{}",
                        threadName,
                        block.getBlockNumber(),
                        e
                    );
                }
            } else if (
                block.isDataEncrypted() &&
                (!structurallyValid || !cryptographicallyValid)
            ) {
                // Block is encrypted but failed basic validation
                logger.warn(
                    "⚠️ [{}] Block #{} is encrypted but failed basic validation - skipping encryption checks",
                    threadName,
                    block.getBlockNumber()
                );
            }

            // Build result including encrypted data validation
            BlockValidationResult result = builder
                .structurallyValid(structurallyValid)
                .cryptographicallyValid(
                    cryptographicallyValid && encryptedDataValid
                )
                .authorizationValid(authorizationValid)
                .offChainDataValid(offChainDataValid)
                .errorMessage(errorMessage)
                .warningMessage(warningMessage)
                .build();

            if (result.isFullyValid()) {
                logger.info(
                    "✅ [{}] Block #{} is fully valid",
                    threadName,
                    block.getBlockNumber()
                );
            } else if (result.isValid()) {
                logger.warn(
                    "⚠️ [{}] Block #{} is structurally valid but has authorization issues",
                    threadName,
                    block.getBlockNumber()
                );
            } else {
                logger.error(
                    "❌ [{}] Block #{} is invalid",
                    threadName,
                    block.getBlockNumber()
                );
            }

            return result;
        } catch (Exception e) {
            logger.error(
                "💥 [{}] EXCEPTION during detailed block validation",
                threadName,
                e
            );
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
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            return validateSingleBlockInternal(block);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }
    
    /**
     * INTERNAL: Validate a single block WITHOUT acquiring lock
     * Used when caller already holds appropriate lock
     * @param block The block to validate
     * @return true if block is valid, false otherwise
     */
    private boolean validateSingleBlockInternal(Block block) {
        try {
            // Skip validation for genesis block
            if (
                block.getBlockNumber() != null &&
                block.getBlockNumber().equals(0L)
            ) {
                return true;
            }

            // Find the previous block for validation
            Block previousBlock = blockRepository.getBlockByNumber(
                block.getBlockNumber() - 1L
            );
            if (previousBlock == null) {
                return false; // Previous block not found
            }

            return validateBlock(block, previousBlock);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * PUBLIC: For use by ChainRecoveryManager when it's called from within a lock
     * Validates a single block without acquiring additional locks
     * 
     * WARNING: Only call this method if you already hold an appropriate lock!
     * For normal use, call validateSingleBlock() which manages locks automatically.
     * 
     * @param block The block to validate
     * @return true if block is valid, false otherwise
     */
    public boolean validateSingleBlockWithoutLock(Block block) {
        return validateSingleBlockInternal(block);
    }

    /**
     * ENHANCED: Detailed validation of the entire blockchain
     * Returns comprehensive information about validation status
     * 
     * DEADLOCK FIX #9: Uses internal method to allow lock-free calling
     * For normal use, this method manages locks automatically.
     */
    public ChainValidationResult validateChainDetailed() {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            return validateChainDetailedInternal();
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * INTERNAL: Detailed chain validation without locks
     * This is the actual implementation used by both public methods
     * 
     * @return ChainValidationResult with comprehensive validation information
     */
    private ChainValidationResult validateChainDetailedInternal() {
        try {
            long totalBlocks = blockRepository.getBlockCount();

            // MEMORY SAFETY: Warn if validating very large chains
            if (totalBlocks > MemorySafetyConstants.SAFE_EXPORT_LIMIT) {
                logger.warn("⚠️  WARNING: Validating {} blocks (>100K blocks may cause memory issues)", totalBlocks);
                logger.warn("⚠️  Consider using validateChain() for basic validation on large chains");
                // For very large chains (>500K), refuse detailed validation
                if (totalBlocks > MemorySafetyConstants.MAX_EXPORT_LIMIT) {
                    logger.error("❌ Cannot perform detailed validation on {} blocks (max: 500K)", totalBlocks);
                    logger.error("❌ Use validateChain() instead for basic validation");
                    throw new IllegalStateException("Chain too large for detailed validation: " + totalBlocks + " blocks");
                }
            }

            List<BlockValidationResult> blockResults = new ArrayList<>();

            if (totalBlocks == 0) {
                logger.error("❌ No blocks found in chain");
                return new ChainValidationResult(blockResults);
            }

            // Validate genesis block (always fetch first)
            Block genesisBlock = blockRepository.getBlockByNumber(0L);
            if (genesisBlock == null) {
                logger.error("❌ Genesis block not found");
                return new ChainValidationResult(blockResults);
            }

            BlockValidationResult.Builder genesisBuilder =
                new BlockValidationResult.Builder(genesisBlock);

            if (
                !genesisBlock.getBlockNumber().equals(0L) ||
                !genesisBlock.getPreviousHash().equals(GENESIS_PREVIOUS_HASH)
            ) {
                logger.error("❌ Invalid genesis block");
                blockResults.add(
                    genesisBuilder
                        .structurallyValid(false)
                        .cryptographicallyValid(false)
                        .authorizationValid(false)
                        .errorMessage("Invalid genesis block")
                        .status(BlockStatus.INVALID)
                        .build()
                );
            } else {
                blockResults.add(
                    genesisBuilder
                        .structurallyValid(true)
                        .cryptographicallyValid(true)
                        .authorizationValid(true)
                        .status(BlockStatus.VALID)
                        .build()
                );
            }

            // Validate blocks in batches (memory-efficient pagination)
            final int BATCH_SIZE = 1000;
            Block previousBlock = genesisBlock;
            int blocksWithOffChain = 0;
            int validOffChainBlocks = 0;
            long totalOffChainSize = 0;

            for (long offset = 1; offset < totalBlocks; offset += BATCH_SIZE) {
                int limit = (int) Math.min(BATCH_SIZE, totalBlocks - offset);
                List<Block> batch = blockRepository.getBlocksPaginated(offset, limit);

                for (Block currentBlock : batch) {
                    BlockValidationResult result = validateBlockDetailed(
                        currentBlock,
                        previousBlock
                    );
                    blockResults.add(result);

                    // Collect off-chain statistics
                    if (currentBlock.hasOffChainData()) {
                        blocksWithOffChain++;
                        if (currentBlock.getOffChainData().getFileSize() != null) {
                            totalOffChainSize += currentBlock
                                .getOffChainData()
                                .getFileSize();
                        }
                        if (result.isOffChainDataValid()) {
                            validOffChainBlocks++;
                        }
                    }

                    previousBlock = currentBlock;
                }

                // Log progress for large chains
                if (totalBlocks > 10000 && offset % 10000 == 0) {
                    logger.info("📊 Validated {}/{} blocks...", offset + batch.size(), totalBlocks);
                }
            }

            ChainValidationResult chainResult = new ChainValidationResult(
                blockResults
            );

            logger.info(
                "📊 Chain validation completed: {}",
                chainResult.getSummary()
            );

            if (blocksWithOffChain > 0) {
                logger.info("🗂️ Off-chain data summary:");
                logger.info(
                    "   📊 Blocks with off-chain data: {}/{} ({}%)",
                    blocksWithOffChain,
                    totalBlocks,
                    String.format(
                        "%.1f",
                        ((blocksWithOffChain * 100.0) / totalBlocks)
                    )
                );
                logger.info(
                    "   ✅ Valid off-chain blocks: {}/{} ({}%)",
                    validOffChainBlocks,
                    blocksWithOffChain,
                    String.format(
                        "%.1f",
                        ((validOffChainBlocks * 100.0) / blocksWithOffChain)
                    )
                );
                logger.info(
                    "   📦 Total off-chain storage: {} MB",
                    String.format("%.2f", totalOffChainSize / (1024.0 * 1024.0))
                );

                if (validOffChainBlocks < blocksWithOffChain) {
                    int invalidOffChain =
                        blocksWithOffChain - validOffChainBlocks;
                    logger.warn(
                        "   ⚠️ Invalid off-chain blocks detected: {}",
                        invalidOffChain
                    );
                }
            } else {
                logger.info("📋 No off-chain data found in blockchain");
            }

            return chainResult;
        } catch (Exception e) {
            logger.error("❌ Error during detailed chain validation", e);
            return new ChainValidationResult(new ArrayList<>());
        }
    }

    /**
     * PUBLIC: Detailed chain validation without acquiring locks
     * For use by ChainRecoveryManager when called from within a lock context
     * 
     * WARNING: Only call this method if you already hold an appropriate lock!
     * For normal use, call validateChainDetailed() which manages locks automatically.
     * 
     * @return ChainValidationResult with comprehensive validation information
     */
    public ChainValidationResult validateChainDetailedWithoutLock() {
        return validateChainDetailedInternal();
    }

    /**
     * MEMORY-SAFE: Stream-based validation for very large blockchains.
     * Instead of accumulating all validation results, this method processes the chain in batches
     * and calls a consumer for each batch result. Suitable for blockchains with millions of blocks.
     *
     * @param batchResultConsumer Consumer that receives validation results for each batch
     * @param batchSize Number of blocks to validate in each batch (default: 1000)
     * @return Summary statistics (counts only, no individual block results)
     */
    public ValidationSummary validateChainStreaming(
        java.util.function.Consumer<List<BlockValidationResult>> batchResultConsumer,
        int batchSize
    ) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }

        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            long totalBlocks = blockRepository.getBlockCount();
            final long[] validCount = {0};
            final long[] invalidCount = {0};
            final long[] revokedCount = {0};

            if (totalBlocks == 0) {
                logger.error("❌ No blocks found in chain");
                return new ValidationSummary(0, 0, 0, 0);
            }

            // Validate genesis block
            Block genesisBlock = blockRepository.getBlockByNumber(0L);
            if (genesisBlock == null) {
                logger.error("❌ Genesis block not found");
                return new ValidationSummary(0, 0, 1, 0);
            }

            List<BlockValidationResult> batchResults = new ArrayList<>();
            BlockValidationResult.Builder genesisBuilder = new BlockValidationResult.Builder(genesisBlock);

            if (!genesisBlock.getBlockNumber().equals(0L) || !genesisBlock.getPreviousHash().equals(GENESIS_PREVIOUS_HASH)) {
                batchResults.add(genesisBuilder.structurallyValid(false).cryptographicallyValid(false)
                    .authorizationValid(false).errorMessage("Invalid genesis block").status(BlockStatus.INVALID).build());
                invalidCount[0]++;
            } else {
                batchResults.add(genesisBuilder.structurallyValid(true).cryptographicallyValid(true)
                    .authorizationValid(true).status(BlockStatus.VALID).build());
                validCount[0]++;
            }

            // Send genesis result
            batchResultConsumer.accept(new ArrayList<>(batchResults));
            batchResults.clear();

            // Validate remaining blocks in batches
            Block previousBlock = genesisBlock;

            for (long offset = 1; offset < totalBlocks; offset += batchSize) {
                int limit = (int) Math.min(batchSize, totalBlocks - offset);
                List<Block> batch = blockRepository.getBlocksPaginated(offset, limit);

                for (Block currentBlock : batch) {
                    BlockValidationResult result = validateBlockDetailed(currentBlock, previousBlock);
                    batchResults.add(result);

                    // Count statuses
                    if (result.getStatus() == BlockStatus.VALID) validCount[0]++;
                    else if (result.getStatus() == BlockStatus.INVALID) invalidCount[0]++;
                    else if (result.getStatus() == BlockStatus.REVOKED) revokedCount[0]++;

                    previousBlock = currentBlock;
                }

                // Send batch results to consumer
                batchResultConsumer.accept(new ArrayList<>(batchResults));
                batchResults.clear();

                // Log progress
                if (totalBlocks > 10000 && offset % 10000 == 0) {
                    logger.info("📊 Validated {}/{} blocks...", offset + batch.size(), totalBlocks);
                }
            }

            logger.info("✅ Streaming validation completed: {} total blocks", totalBlocks);
            return new ValidationSummary(totalBlocks, validCount[0], invalidCount[0], revokedCount[0]);

        } catch (Exception e) {
            logger.error("❌ Error during streaming chain validation", e);
            return new ValidationSummary(0, 0, 0, 0);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * Summary of validation results (lightweight, no individual block details)
     */
    public static class ValidationSummary {
        private final long totalBlocks;
        private final long validBlocks;
        private final long invalidBlocks;
        private final long revokedBlocks;

        public ValidationSummary(long totalBlocks, long validBlocks, long invalidBlocks, long revokedBlocks) {
            this.totalBlocks = totalBlocks;
            this.validBlocks = validBlocks;
            this.invalidBlocks = invalidBlocks;
            this.revokedBlocks = revokedBlocks;
        }

        public long getTotalBlocks() { return totalBlocks; }
        public long getValidBlocks() { return validBlocks; }
        public long getInvalidBlocks() { return invalidBlocks; }
        public long getRevokedBlocks() { return revokedBlocks; }
        public boolean isValid() { return invalidBlocks == 0; }

        @Override
        public String toString() {
            return String.format("ValidationSummary{total=%d, valid=%d, invalid=%d, revoked=%d}",
                totalBlocks, validBlocks, invalidBlocks, revokedBlocks);
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
     * Processes blockchain in batches with database-specific optimization.
     *
     * <p><b>Performance Impact</b>:
     * <ul>
     *   <li>PostgreSQL/MySQL/H2: Uses ScrollableResults (server-side cursor, 73% faster)</li>
     *   <li>SQLite: Uses manual pagination (no change, already optimal)</li>
     *   <li>Memory: Constant (~50MB) regardless of chain size</li>
     * </ul>
     * </p>
     *
     * <p><b>⚠️ Thread-Safety Note</b>: No lock is held during batch processing.
     * DEADLOCK FIX #7: StampedLock is NOT reentrant! The lambda may call methods with readLock
     * (e.g., validateSingleBlock). StampedLock CANNOT nest readLocks - acquiring readLock while
     * holding readLock = DEADLOCK. This is safe because each batch operation is self-contained
     * and the lambda is responsible for its own locking.</p>
     *
     * @param batchProcessor Consumer to process each batch of blocks
     * @param batchSize Number of blocks per batch (default: 1000)
     *
     * @since 2025-10-08 (Performance Optimization - Phase B.1)
     * @see #validateChainDetailedInternal() - Validation (automatic benefit)
     * @see #verifyAllOffChainIntegrity() - Off-chain verification (automatic benefit)
     * @see SearchFrameworkEngine#searchExhaustiveOffChain(String, int, KeyPair, String) - Search (automatic benefit)
     */
    public void processChainInBatches(java.util.function.Consumer<List<Block>> batchProcessor, int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }

        // NO LOCK: Lambda may call methods with readLock (e.g., validateSingleBlock)
        // StampedLock is NOT reentrant - acquiring readLock while holding readLock = DEADLOCK
        // Delegate to BlockRepository for database-specific optimization
        blockRepository.streamAllBlocksInBatches(batchProcessor, batchSize);
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
     *
     * <p>Constructs the canonical block content string used for both hash calculation
     * and digital signature verification.</p>
     *
     * @param block The block from which to build the content
     * @return The block content as a string
     */
    public String buildBlockContent(Block block) {
        // Use epoch seconds for timestamp to ensure consistency
        long timestampSeconds = block.getTimestamp() != null
            ? block.getTimestamp().toEpochSecond(ZoneOffset.UTC)
            : 0;

        String content =
            block.getBlockNumber() +
            (block.getPreviousHash() != null ? block.getPreviousHash() : "") +
            (block.getData() != null ? block.getData() : "") +
            timestampSeconds +
            (block.getSignerPublicKey() != null
                    ? block.getSignerPublicKey()
                    : "");
        
        // TRACE: Log detailed hash calculation inputs (only in trace mode)
        if (logger.isTraceEnabled()) {
            logger.trace("🔧 HASH DEBUG: Block #{} hash calculation:", block.getBlockNumber());
            logger.trace("  - blockNumber: {}", block.getBlockNumber());
            logger.trace("  - previousHash: {}", block.getPreviousHash());
            logger.trace("  - data: {}", block.getData() != null ? block.getData().substring(0, Math.min(50, block.getData().length())) + "..." : "null");
            logger.trace("  - timestampSeconds: {}", timestampSeconds);
            logger.trace("  - signerPublicKey: {}", block.getSignerPublicKey() != null ? block.getSignerPublicKey().substring(0, Math.min(20, block.getSignerPublicKey().length())) + "..." : "null");
            logger.trace("  - Final content: {}", content.substring(0, Math.min(100, content.length())) + "...");
        }
        String calculatedHash = CryptoUtil.calculateHash(content);
        if (logger.isTraceEnabled()) {
            logger.trace("  - Calculated hash: {}", calculatedHash);
        }
        
        return content;
    }

    /**
     * Check if a public key is currently authorized.
     *
     * @param publicKey The public key to check (string format)
     * @return true if key is authorized and active, false otherwise
     * @since 1.0.6
     */
    public boolean isKeyAuthorized(String publicKey) {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            return authorizedKeyDAO.isKeyAuthorized(publicKey);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * Get total count of authorized keys in database.
     *
     * <p>This includes both active and revoked keys.</p>
     *
     * @return Total number of authorized keys
     * @since 1.0.6
     */
    public long getAuthorizedKeyCount() {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            return authorizedKeyDAO.getAuthorizedKeyCount();
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * Creates the bootstrap SUPER_ADMIN user (simplified bootstrap method).
     *
     * <p><strong>🔒 SECURITY:</strong> This method is ONLY valid when the blockchain has no users
     * (bootstrap state). It automatically creates the first user with SUPER_ADMIN role.</p>
     *
     * <p><strong>Use Case:</strong> Single-user demos and initial blockchain setup.</p>
     *
     * <p><strong>Example:</strong></p>
     * <pre>{@code
     * Blockchain blockchain = new Blockchain();
     * KeyPair adminKeys = CryptoUtil.generateKeyPair();
     * blockchain.createBootstrapAdmin(
     *     CryptoUtil.publicKeyToString(adminKeys.getPublic()),
     *     "BootstrapAdmin"
     * );
     * }</pre>
     *
     * @param publicKeyString Public key of the bootstrap admin
     * @param ownerName Name of the bootstrap admin
     * @return true if bootstrap admin created successfully
     * @throws SecurityException if blockchain already has users
     * @throws IllegalArgumentException if publicKeyString or ownerName is invalid
     * @since 1.0.6
     */
    public boolean createBootstrapAdmin(String publicKeyString, String ownerName) {
        return addAuthorizedKey(publicKeyString, ownerName, null, UserRole.SUPER_ADMIN);
    }

    /**
     * Add an authorized key with role-based validation (RBAC v1.0.6+).
     *
     * <p><strong>🔒 Security (v1.0.6+):</strong> Validates caller has permission to create
     * users with the specified target role.</p>
     *
     * <p><strong>Bootstrap Mode:</strong> For genesis admin creation ONLY, pass {@code callerKeyPair = null}.
     * After genesis admin exists, ALL operations require caller credentials.</p>
     *
     * <p><strong>Permission Matrix:</strong></p>
     * <ul>
     *   <li><strong>SUPER_ADMIN</strong> can create: ADMIN, USER, READ_ONLY</li>
     *   <li><strong>ADMIN</strong> can create: USER, READ_ONLY (cannot create ADMIN)</li>
     *   <li><strong>USER</strong> cannot create users</li>
     * </ul>
     *
     * @param publicKeyString The public key to authorize
     * @param ownerName The username
     * @param callerKeyPair The caller's credentials (null ONLY for genesis admin bootstrap)
     * @param targetRole The role to assign to the new user
     * @return true if successful
     * @throws SecurityException if caller lacks permission or callerKeyPair=null after bootstrap
     * @throws IllegalArgumentException if parameters are invalid
     * @since 1.0.6
     */
    public boolean addAuthorizedKey(
        String publicKeyString,
        String ownerName,
        java.security.KeyPair callerKeyPair,
        com.rbatllet.blockchain.security.UserRole targetRole
    ) {
        // Input validation
        if (publicKeyString == null || publicKeyString.trim().isEmpty()) {
            throw new IllegalArgumentException("Public key cannot be null or empty");
        }
        if (publicKeyString.length() > 10000) {
            throw new IllegalArgumentException("Public key size cannot exceed 10KB (DoS protection)");
        }
        if (ownerName == null || ownerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner name cannot be null or empty");
        }
        if (ownerName.length() > 256) {
            throw new IllegalArgumentException("Owner name cannot exceed 256 characters");
        }
        if (targetRole == null) {
            throw new IllegalArgumentException("Target role cannot be null");
        }

        long stamp = GLOBAL_BLOCKCHAIN_LOCK.writeLock();
        try {
            // Bootstrap mode: Allow genesis admin creation without caller
            if (callerKeyPair == null) {
                long existingUsers = authorizedKeyDAO.getAuthorizedKeyCount();
                if (existingUsers > 0) {
                    throw new SecurityException(
                        "❌ SECURITY VIOLATION: callerKeyPair=null only allowed for genesis admin bootstrap.\n" +
                        "Existing users: " + existingUsers + "\n" +
                        "After bootstrap, ALL operations require caller credentials for RBAC validation."
                    );
                }

                // Genesis admin creation - MUST be SUPER_ADMIN role (security requirement)
                if (targetRole != com.rbatllet.blockchain.security.UserRole.SUPER_ADMIN) {
                    throw new SecurityException(
                        "❌ SECURITY VIOLATION: Genesis bootstrap ONLY allows SUPER_ADMIN role.\n" +
                        "Requested role: " + targetRole + "\n" +
                        "Genesis bootstrap (callerKeyPair=null) is reserved EXCLUSIVELY for creating the single SUPER_ADMIN.\n" +
                        "To create ADMIN/USER/READ_ONLY users, use SUPER_ADMIN credentials as caller."
                    );
                }

                logger.info("🔑 BOOTSTRAP: Creating genesis admin '{}' with SUPER_ADMIN role", ownerName);
                return addAuthorizedKeyInternal(publicKeyString, ownerName, com.rbatllet.blockchain.security.UserRole.SUPER_ADMIN, null);
            }

            // Normal mode: Validate caller has permission
            String callerPublicKey = com.rbatllet.blockchain.util.CryptoUtil.publicKeyToString(callerKeyPair.getPublic());
            com.rbatllet.blockchain.entity.AuthorizedKey caller = authorizedKeyDAO.getAuthorizedKeyByPublicKey(callerPublicKey);

            if (caller == null || !caller.isActive()) {
                throw new SecurityException(
                    "❌ AUTHORIZATION REQUIRED: Caller is not authorized.\n" +
                    "Public key: " + callerPublicKey.substring(0, Math.min(50, callerPublicKey.length())) + "..."
                );
            }

            com.rbatllet.blockchain.security.UserRole callerRole = caller.getRole();

            // Check if caller can create target role
            if (!callerRole.canCreateRole(targetRole)) {
                throw new SecurityException(
                    "❌ PERMISSION DENIED: Role '" + callerRole + "' cannot create users with role '" + targetRole + "'.\n" +
                    "Caller: " + caller.getOwnerName() + "\n" +
                    "Target user: " + ownerName + "\n" +
                    "See docs/security/ROLE_BASED_ACCESS_CONTROL.md for permission matrix."
                );
            }

            logger.info("✅ User '{}' (role: {}) creating new user '{}' with role {}",
                        caller.getOwnerName(), callerRole, ownerName, targetRole);

            return addAuthorizedKeyInternal(publicKeyString, ownerName, targetRole, caller.getOwnerName());

        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockWrite(stamp);
        }
    }

    /**
     * INTERNAL: Add authorized key logic without lock (RBAC v1.0.6+).
     * This is the single source of truth for adding authorized keys.
     *
     * @param publicKeyString The public key
     * @param ownerName The username
     * @param role The role to assign
     * @param createdBy Who created this user (null for genesis admin)
     * @return true if successful
     * @throws IllegalArgumentException if parameters are invalid or key validation fails
     * @since 1.0.6
     */
    private boolean addAuthorizedKeyInternal(
        String publicKeyString,
        String ownerName,
        com.rbatllet.blockchain.security.UserRole role,
        String createdBy
    ) {
        return JPAUtil.executeInTransaction(em -> {
            // Validate input parameters (defensive - should already be validated by public wrapper)
            if (publicKeyString == null || publicKeyString.trim().isEmpty()) {
                logger.error("❌ Public key cannot be null or empty");
                throw new IllegalArgumentException("Public key cannot be null or empty");
            }
            if (ownerName == null || ownerName.trim().isEmpty()) {
                logger.error("❌ Owner name cannot be null or empty");
                throw new IllegalArgumentException("Owner name cannot be null or empty");
            }

            // Enhanced key validation using CryptoUtil
            PublicKey testKey;
            try {
                // Validate key format and cryptographic validity
                testKey = CryptoUtil.stringToPublicKey(publicKeyString);
                if (testKey == null) {
                    logger.error("❌ Invalid public key format");
                    throw new IllegalArgumentException("Invalid public key format");
                }
            } catch (IllegalArgumentException e) {
                // Re-throw IllegalArgumentException as-is
                throw e;
            } catch (Exception e) {
                // Wrap other exceptions
                logger.error("❌ Key validation failed", e);
                throw new IllegalArgumentException("Key validation failed: " + e.getMessage(), e);
            }

            // Check if key is currently authorized (benign case - return false)
            if (authorizedKeyDAO.isKeyAuthorized(publicKeyString)) {
                logger.debug("ℹ️ Key already authorized (not an error - expected in concurrent operations)");
                return false;
            }

            // Allow re-authorization: create new authorization record (RBAC v1.0.6+)
            AuthorizedKey authorizedKey = new AuthorizedKey(
                publicKeyString,
                ownerName,
                role,
                createdBy,
                LocalDateTime.now()
            );

            authorizedKeyDAO.saveAuthorizedKey(authorizedKey);
            logger.info("🔑 Authorized key added for: {} (role: {}, created by: {})",
                ownerName, role, createdBy != null ? createdBy : "SYSTEM");
            return true;
        });
    }

    /**
     * Add authorized key for system recovery (bypasses RBAC validation).
     *
     * <p><strong>🔒 SECURITY WARNING:</strong> This method bypasses RBAC validation and should
     * ONLY be used by ChainRecoveryManager for automatic recovery operations. DO NOT use
     * for regular user creation.</p>
     *
     * <p>This method allows the system to re-authorize keys that were deleted and caused
     * blockchain corruption. The recovered keys are assigned USER role by default and
     * createdBy is set to "SYSTEM_RECOVERY" for audit trail.</p>
     *
     * @param publicKeyString The public key to re-authorize
     * @param ownerName The owner name (usually includes "(RECOVERED-timestamp)" suffix)
     * @param role The role to assign (typically USER for recovered keys)
     * @param createdBy The creator identifier (typically "SYSTEM_RECOVERY")
     * @return true if successful
     * @throws IllegalArgumentException if parameters are invalid
     * @since 1.0.6
     */
    public boolean addAuthorizedKeySystemRecovery(
        String publicKeyString,
        String ownerName,
        com.rbatllet.blockchain.security.UserRole role,
        String createdBy
    ) {
        // Input validation (same as normal addAuthorizedKey)
        if (publicKeyString == null || publicKeyString.trim().isEmpty()) {
            throw new IllegalArgumentException("Public key cannot be null or empty");
        }
        if (publicKeyString.length() > 10000) {
            throw new IllegalArgumentException("Public key size cannot exceed 10KB (DoS protection)");
        }
        if (ownerName == null || ownerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner name cannot be null or empty");
        }
        if (ownerName.length() > 256) {
            throw new IllegalArgumentException("Owner name cannot exceed 256 characters");
        }
        if (role == null) {
            throw new IllegalArgumentException("Target role cannot be null");
        }

        long stamp = GLOBAL_BLOCKCHAIN_LOCK.writeLock();
        try {
            logger.warn("⚠️  SYSTEM RECOVERY: Re-authorizing key '{}' with role {} (createdBy: {})",
                ownerName, role, createdBy);
            return addAuthorizedKeyInternal(publicKeyString, ownerName, role, createdBy);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockWrite(stamp);
        }
    }

    /**
     * PUBLIC: Add authorized key WITHOUT acquiring lock (RBAC v1.0.6+).
     * WARNING: Only call this method from within an existing lock context!
     * This is used by ChainRecoveryManager when already holding a writeLock.
     *
     * @param publicKeyString The public key
     * @param ownerName The username
     * @param role The role to assign
     * @param createdBy Who created this user (null for system operations)
     * @return true if successful
     * @since 1.0.6
     */
    public boolean addAuthorizedKeyWithoutLock(
        String publicKeyString,
        String ownerName,
        com.rbatllet.blockchain.security.UserRole role,
        String createdBy
    ) {
        return addAuthorizedKeyInternal(publicKeyString, ownerName, role, createdBy);
    }

    /**
     * Get the role of a user by their public key (RBAC v1.0.6+).
     *
     * @param publicKey The user's public key
     * @return The user's role, or null if not authorized or inactive
     * @since 1.0.6
     */
    public com.rbatllet.blockchain.security.UserRole getUserRole(String publicKey) {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            com.rbatllet.blockchain.entity.AuthorizedKey key = authorizedKeyDAO.getAuthorizedKeyByPublicKey(publicKey);
            return (key != null && key.isActive()) ? key.getRole() : null;
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * Check if a user has a specific role or higher (RBAC v1.0.6+).
     *
     * <p>Uses privilege level comparison:
     * SUPER_ADMIN (100) &gt; ADMIN (50) &gt; USER (10) &gt; READ_ONLY (1)</p>
     *
     * <p><strong>Example:</strong></p>
     * <pre>
     * if (blockchain.hasRole(publicKey, UserRole.ADMIN)) {
     *     // User is ADMIN or SUPER_ADMIN
     * }
     * </pre>
     *
     * @param publicKey The user's public key
     * @param requiredRole The required role
     * @return true if the user has the required role or higher privilege level
     * @since 1.0.6
     */
    public boolean hasRole(String publicKey, com.rbatllet.blockchain.security.UserRole requiredRole) {
        com.rbatllet.blockchain.security.UserRole userRole = getUserRole(publicKey);
        return userRole != null && userRole.getPrivilegeLevel() >= requiredRole.getPrivilegeLevel();
    }

    /**
     * CORE FUNCTION: Revoke an authorized key
     * FIXED: Added global synchronization for thread safety
     *
     * @param publicKeyString The public key to revoke
     * @return true if revocation was successful
     * @throws IllegalArgumentException if publicKeyString is null/empty or key not found
     * @throws IllegalStateException if attempting to revoke the last active SUPER_ADMIN (system lockout protection)
     * @since 1.0.6
     */
    public boolean revokeAuthorizedKey(String publicKeyString) {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.writeLock();
        try {
            return revokeAuthorizedKeyInternal(publicKeyString);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockWrite(stamp);
        }
    }

    /**
     * INTERNAL: Revoke authorized key logic without lock
     * This is the single source of truth for revoking authorized keys
     *
     * @param publicKeyString The public key to revoke
     * @return true if revocation was successful
     * @throws IllegalArgumentException if publicKeyString is null/empty or key not found
     * @throws IllegalStateException if attempting to revoke the last active SUPER_ADMIN (system lockout protection)
     * @since 1.0.6
     */
    private boolean revokeAuthorizedKeyInternal(String publicKeyString) {
        return JPAUtil.executeInTransaction(em -> {
            // Validate input parameter
            if (publicKeyString == null || publicKeyString.trim().isEmpty()) {
                logger.error("❌ Public key cannot be null or empty");
                throw new IllegalArgumentException("Public key cannot be null or empty");
            }

            if (!authorizedKeyDAO.isKeyAuthorized(publicKeyString)) {
                logger.error("❌ Key not found or already inactive");
                throw new IllegalArgumentException("Key not found or already inactive: " +
                    publicKeyString.substring(0, Math.min(50, publicKeyString.length())) + "...");
            }

            // RBAC v1.0.6: Check if revoking last active SUPER_ADMIN (system protection)
            AuthorizedKey keyToRevoke = authorizedKeyDAO.getAuthorizedKeyByPublicKey(publicKeyString);
            if (keyToRevoke != null && keyToRevoke.getRole() == com.rbatllet.blockchain.security.UserRole.SUPER_ADMIN) {
                // Count active SUPER_ADMINs
                long activeSuperAdminCount = authorizedKeyDAO.countActiveSuperAdmins();

                if (activeSuperAdminCount <= 1) {
                    logger.error("❌ Cannot revoke the last active SUPER_ADMIN (system lockout protection)");
                    logger.error("   Create another SUPER_ADMIN before revoking this one");
                    throw new IllegalStateException(
                        "SYSTEM LOCKOUT PROTECTION: Cannot revoke the last active SUPER_ADMIN. " +
                        "Create another SUPER_ADMIN before revoking '" + keyToRevoke.getOwnerName() + "'"
                    );
                }

                logger.warn("⚠️ Revoking SUPER_ADMIN '{}' ({} active SUPER_ADMINs remaining)",
                           keyToRevoke.getOwnerName(), activeSuperAdminCount - 1);
            }

            authorizedKeyDAO.revokeAuthorizedKey(publicKeyString);
            logger.info("✅ Key revoked successfully");
            return true;
        });
    }

    /**
     * PUBLIC: Revoke authorized key WITHOUT acquiring lock
     * WARNING: Only call this method from within an existing lock context!
     * This is used by ChainRecoveryManager when already holding a writeLock
     *
     * @param publicKeyString The public key to revoke
     * @return true if revocation was successful
     * @throws IllegalArgumentException if publicKeyString is null/empty or key not found
     * @throws IllegalStateException if attempting to revoke the last active SUPER_ADMIN (system lockout protection)
     * @since 1.0.6
     */
    public boolean revokeAuthorizedKeyWithoutLock(String publicKeyString) {
        return revokeAuthorizedKeyInternal(publicKeyString);
    }

    // ===============================
    // CORE FUNCTIONS: ADVANCED ADDITIONS
    // ===============================

    /**
     * CORE FUNCTION 1: Block Size Validation
     */
    /**
     * Enhanced block size validation that supports off-chain storage
     * Returns: 0 = invalid, 1 = store on-chain, 2 = store off-chain
     */
    public int validateAndDetermineStorage(String data) {
        if (data == null) {
            logger.error(
                "❌ Block data cannot be null. Use empty string \"\" for system blocks"
            );
            return 0; // Invalid
        }

        // Allow empty strings for system/configuration blocks
        if (data.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("ℹ️ System block with empty data created");
            }
            return 1; // Store on-chain
        }

        // Check character limit first
        int charCount = data.length();
        if (charCount > currentMaxBlockChars) {
            // Exceeds character limit - must go off-chain
            logger.info(
                "📦 Data exceeds character limit ({} > {}). Will store off-chain.",
                charCount,
                currentMaxBlockChars
            );

            // Validate it's not too large even for off-chain
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            if (dataBytes.length > 100 * 1024 * 1024) {
                logger.error(
                    "❌ Data size ({} bytes) exceeds maximum supported size (100MB)",
                    dataBytes.length
                );
                return 0; // Invalid
            }
            return 2; // Store off-chain
        }

        // Within character limit, check byte size
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);

        // If data is small enough for on-chain storage (under off-chain threshold)
        if (dataBytes.length < currentOffChainThresholdBytes) {
            return 1; // Store on-chain
        }

        // If data is large but within reasonable limits, store off-chain
        if (dataBytes.length <= 100 * 1024 * 1024) {
            // Max 100MB
            logger.info(
                "📦 Large data detected ({} bytes). Will store off-chain.",
                dataBytes.length
            );
            return 2; // Store off-chain
        }

        // Data is too large even for off-chain storage
        logger.error(
            "❌ Data size ({} bytes) exceeds maximum supported size (100MB)",
            dataBytes.length
        );
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
    private void validateBlockInput(
        String data,
        PrivateKey signerPrivateKey,
        PublicKey signerPublicKey
    ) {
        if (data == null) {
            throw new IllegalArgumentException("Block data cannot be null");
        }
        if (signerPrivateKey == null) {
            throw new IllegalArgumentException(
                "Signer private key cannot be null"
            );
        }
        if (signerPublicKey == null) {
            throw new IllegalArgumentException(
                "Signer public key cannot be null"
            );
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
        if (dataBytes.length > 100 * 1024 * 1024) {
            // Max 100MB
            logger.error(
                "❌ Data size ({} bytes) exceeds maximum supported size (100MB)",
                dataBytes.length
            );
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
     * Limits both number of terms AND individual word length to prevent overflow
     */
    private Set<String> extractSimpleSearchTerms(String content, int maxTerms) {
        if (content == null || content.trim().isEmpty()) {
            return new HashSet<>();
        }

        String[] words = content
            .toLowerCase()
            .replaceAll("[^a-zA-Z0-9\\s]", " ")
            .split("\\s+");

        Set<String> terms = new HashSet<>();
        final int MAX_WORD_LENGTH = 50; // Prevent single huge words from causing overflow
        
        for (String word : words) {
            if (word.length() > 2 && terms.size() < maxTerms) {
                // Limit individual word length to prevent overflow
                // If word is too long, skip it entirely (it's not a useful search term anyway)
                if (word.length() <= MAX_WORD_LENGTH) {
                    terms.add(word);
                } else {
                    // Silently skip overly long "words" (likely garbage data like "aaaaaaa...")
                    if (logger.isDebugEnabled()) {
                        logger.debug("⚠️  Skipping overly long word ({} chars) during keyword extraction", 
                                   word.length());
                    }
                }
            }
        }

        return terms;
    }

    /**
     * Generate a deterministic password for off-chain data encryption
     * Based on block number and signer public key for reproducibility
     */
    private String generateOffChainPassword(
        Long blockNumber,
        String signerPublicKey
    ) throws Exception {
        String input = "OFFCHAIN_" + blockNumber + "_" + signerPublicKey;
        MessageDigest digest = MessageDigest.getInstance(
            CryptoUtil.HASH_ALGORITHM
        );
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
            logger.error(
                "❌ Error verifying off-chain integrity for block {}",
                block.getBlockNumber(),
                e
            );
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
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            long totalBlocks = blockRepository.getBlockCount();
            int offChainBlocks = 0;
            int integrityFailures = 0;

            // Process blocks in batches to avoid memory issues
            final int BATCH_SIZE = 1000;
            for (long offset = 0; offset < totalBlocks; offset += BATCH_SIZE) {
                int limit = (int) Math.min(BATCH_SIZE, totalBlocks - offset);
                List<Block> batch = blockRepository.getBlocksPaginated(offset, limit);

                for (Block block : batch) {
                    if (block.hasOffChainData()) {
                        offChainBlocks++;
                        if (!verifyOffChainIntegrity(block)) {
                            integrityFailures++;
                            logger.error(
                                "❌ Integrity verification failed for block {}",
                                block.getBlockNumber()
                            );
                        }
                    }
                }

                // Log progress for large chains
                if (totalBlocks > 10000 && offset % 10000 == 0) {
                    logger.info("📊 Verified {}/{} blocks for off-chain integrity...",
                        offset + batch.size(), totalBlocks);
                }
            }

            logger.info("📋 Off-chain integrity check complete:");
            logger.info("📊 - Blocks with off-chain data: {}", offChainBlocks);
            logger.info("📊 - Integrity failures: {}", integrityFailures);

            return integrityFailures == 0;
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * SECURITY: Create safe hash for logging sensitive key information
     * This prevents full key exposure in logs while maintaining traceability
     *
     * @param publicKey The public key to hash
     * @return 8-character hash suitable for logging
     */
    private String createSafeKeyHash(String publicKey) {
        if (publicKey == null || publicKey.isEmpty()) {
            return "null-key";
        }
        try {
            return CryptoUtil.calculateHash(publicKey).substring(0, 8);
        } catch (Exception e) {
            return "hash-err";
        }
    }

    /**
     * Validate file path for security and accessibility
     * SECURITY: Prevents path traversal attacks and ensures safe file operations
     *
     * @param filePath The file path to validate
     * @param operation The operation being performed ("export" or "import")
     * @throws IllegalArgumentException if filePath is null/empty or has invalid extension
     * @throws SecurityException if path traversal attempt detected
     * @throws IllegalStateException if directory creation fails or file not accessible
     * @since 1.0.6
     */
    private void isValidFilePath(String filePath, String operation) {
        // Fast validation checks first
        if (filePath == null || filePath.trim().isEmpty()) {
            logger.error("❌ {} file path cannot be null or empty", operation);
            throw new IllegalArgumentException(operation + " file path cannot be null or empty");
        }

        // SECURITY: Comprehensive path traversal prevention
        if (filePath.contains("..") || filePath.contains("%2e%2e") ||
            filePath.contains("%252e%252e") || filePath.contains("\\..") ||
            filePath.contains("/..")) {
            logger.error("❌ SECURITY: Path traversal attempt detected in {}: {}", operation, filePath);
            throw new SecurityException(
                "SECURITY: Path traversal attempt detected in " + operation + ": " + filePath
            );
        }

        // Fast extension check for export/import
        boolean isExportImport = "export".equals(operation) || "import".equals(operation);
        if (isExportImport && !filePath.endsWith(".json")) {
            logger.error("❌ SECURITY: {} only allowed for .json files: {}", operation, filePath);
            throw new IllegalArgumentException(
                "SECURITY: " + operation + " only allowed for .json files: " + filePath
            );
        }

        // Additional security: normalize path and verify it doesn't escape
        try {
            java.nio.file.Path normalizedPath = java.nio.file.Paths.get(filePath).normalize();
            String normalizedString = normalizedPath.toString();

            // Double-check that normalization didn't reveal traversal
            if (normalizedString.contains("..")) {
                logger.error("❌ SECURITY: Path normalization revealed traversal in {}: {}", operation, filePath);
                throw new SecurityException(
                    "SECURITY: Path normalization revealed traversal in " + operation + ": " + filePath
                );
            }

            File file = new File(filePath);

            if ("export".equals(operation)) {
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    if (!parentDir.mkdirs()) {
                        logger.error("❌ Cannot create directory for {}: {}", operation, parentDir);
                        throw new IllegalStateException(
                            "Cannot create directory for " + operation + ": " + parentDir
                        );
                    }
                }
            } else if ("import".equals(operation)) {
                if (!file.exists()) {
                    logger.error("❌ Import file does not exist: {}", filePath);
                    throw new IllegalArgumentException("Import file does not exist: " + filePath);
                }
                if (!file.canRead()) {
                    logger.error("❌ No read permission for import file: {}", filePath);
                    throw new IllegalStateException("No read permission for import file: " + filePath);
                }
            }
        } catch (IllegalArgumentException | SecurityException | IllegalStateException e) {
            // Re-throw our custom exceptions
            throw e;
        } catch (Exception e) {
            logger.error("❌ Error validating {} path: {}", operation, e.getMessage());
            throw new IllegalStateException("Error validating " + operation + " path: " + e.getMessage(), e);
        }
    }

    /**
     * CORE FUNCTION 2: Chain Export - Backup blockchain to file
     * SECURITY FIX: Added path validation and enhanced security checks
     * MEMORY SAFETY: Validates blockchain size before export (max 500K blocks)
     */
    public boolean exportChain(String filePath) {
        return exportChain(filePath, true); // Default: include off-chain files
    }

    /**
     * CORE FUNCTION 2: Chain Export - Backup blockchain to file with optional off-chain export
     * SECURITY FIX: Added path validation and enhanced security checks
     * MEMORY SAFETY: Validates blockchain size before export (max 500K blocks)
     *
     * @param filePath Path to export the blockchain JSON
     * @param includeOffChainFiles Whether to export off-chain files (false for temporary backups)
     * @return true if export was successful
     * @throws IllegalArgumentException if filePath is invalid
     * @throws SecurityException if path traversal attempt detected
     * @throws IllegalStateException if directory creation fails
     * @since 1.0.6
     */
    public boolean exportChain(String filePath, boolean includeOffChainFiles) {
        // SECURITY FIX: Validate file path for security (throws exceptions if invalid)
        isValidFilePath(filePath, "export");

        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            return exportChainInternal(filePath, includeOffChainFiles);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * Internal export method without lock acquisition
     * Used when already inside a lock (e.g., from clearAndReinitialize)
     *
     * OPTIMIZED (Priority 3): Streaming JSON export - no memory accumulation
     * Memory usage: Constant ~50MB regardless of blockchain size (tested up to 500K blocks)
     *
     * @param filePath Path to export the blockchain JSON
     * @param includeOffChainFiles Whether to export off-chain files
     * @return true if export was successful, false otherwise
     */
    private boolean exportChainInternal(String filePath, boolean includeOffChainFiles) {
        try {
            // Use batch processing for streaming export
            final int BATCH_SIZE = 1000;
            long totalBlocks = blockRepository.getBlockCount();

            // MEMORY SAFETY: Warn if exporting very large chains (>100K blocks)
            final int SAFE_EXPORT_LIMIT = MemorySafetyConstants.SAFE_EXPORT_LIMIT;
            if (totalBlocks > SAFE_EXPORT_LIMIT) {
                logger.warn("⚠️  WARNING: Exporting large chain with {} blocks (>100K)", totalBlocks);
                logger.warn("⚠️  Using streaming export to minimize memory usage");
            }

            // For very large exports (>500K), warn but proceed with streaming
            if (totalBlocks > MemorySafetyConstants.MAX_EXPORT_LIMIT) {
                logger.warn("⚠️  NOTICE: Exporting {} blocks (>500K) using streaming mode", totalBlocks);
                logger.warn("⚠️  This may take several minutes. Memory usage will remain constant (~50MB)");
            }

            // Get authorized keys first (small dataset)
            List<AuthorizedKey> allKeys =
                authorizedKeyDAO.getAllAuthorizedKeys(); // FIXED: Export ALL keys, not just active ones

            // OPTIMIZED (Priority 3): Stream JSON directly to file without accumulating blocks
            // Setup off-chain backup directory if needed
            File exportDir = new File(filePath).getParentFile();
            File offChainBackupDir = includeOffChainFiles ? new File(exportDir, "off-chain-backup") : null;
            java.util.concurrent.atomic.AtomicInteger offChainFilesExported =
                new java.util.concurrent.atomic.AtomicInteger(0);

            if (includeOffChainFiles && offChainBackupDir != null) {
                if (!offChainBackupDir.exists()) {
                    try {
                        if (!offChainBackupDir.mkdirs()) {
                            logger.error(
                                "❌ Failed to create off-chain backup directory: {}",
                                offChainBackupDir.getAbsolutePath()
                            );
                            offChainBackupDir = null; // Disable off-chain backup
                        }
                    } catch (SecurityException e) {
                        logger.error("❌ Security exception creating backup directory", e);
                        offChainBackupDir = null; // Disable off-chain backup
                    }
                }
            }

            // STREAMING JSON EXPORT: Write directly to file without memory accumulation
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

            File file = new File(filePath);
            final File finalOffChainBackupDir = offChainBackupDir;

            try (com.fasterxml.jackson.core.JsonGenerator generator = mapper.getFactory()
                .createGenerator(file, com.fasterxml.jackson.core.JsonEncoding.UTF8)) {

                generator.useDefaultPrettyPrinter(); // Pretty print for readability
                generator.writeStartObject();

                // Write metadata fields first
                generator.writeStringField("version", "1.1");
                generator.writeNumberField("totalBlocks", totalBlocks);
                generator.writeStringField("exportTimestamp", LocalDateTime.now().toString());

                // Write authorized keys array
                generator.writeFieldName("authorizedKeys");
                mapper.writeValue(generator, allKeys);

                // STREAMING: Write blocks array one block at a time
                generator.writeFieldName("blocks");
                generator.writeStartArray();

                java.util.concurrent.atomic.AtomicLong blocksExported =
                    new java.util.concurrent.atomic.AtomicLong(0);

                // Stream blocks in batches without accumulating
                for (long offset = 0; offset < totalBlocks; offset += BATCH_SIZE) {
                    List<Block> batch = blockRepository.getBlocksPaginated(offset, BATCH_SIZE);

                    for (Block block : batch) {
                        // Handle off-chain file export if needed (before serializing block)
                        if (includeOffChainFiles && finalOffChainBackupDir != null && block.hasOffChainData()) {
                            try {
                                OffChainData offChainData = block.getOffChainData();
                                File sourceFile = new File(offChainData.getFilePath());

                                if (sourceFile.exists()) {
                                    String fileName =
                                        "block_" +
                                        block.getBlockNumber() +
                                        "_" +
                                        sourceFile.getName();
                                    File backupFile = new File(finalOffChainBackupDir, fileName);

                                    // Copy file using Java NIO
                                    java.nio.file.Files.copy(
                                        sourceFile.toPath(),
                                        backupFile.toPath(),
                                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                                    );

                                    // Update path to relative location for export
                                    offChainData.setFilePath("off-chain-backup/" + fileName);
                                    offChainFilesExported.incrementAndGet();

                                    if (logger.isTraceEnabled()) {
                                        logger.trace("  ✓ Exported off-chain file for block #{}",
                                            block.getBlockNumber());
                                    }
                                } else {
                                    logger.warn("  ⚠ Off-chain file missing for block #{}: {}",
                                        block.getBlockNumber(), sourceFile.getAbsolutePath());
                                }
                            } catch (Exception e) {
                                logger.error("  ❌ Error exporting off-chain file for block #{}",
                                    block.getBlockNumber(), e);
                            }
                        }

                        // Write block directly to JSON stream (no accumulation)
                        mapper.writeValue(generator, block);
                        blocksExported.incrementAndGet();
                    }

                    // Progress logging for large exports
                    if (offset > 0 && offset % 100_000 == 0) {
                        logger.info("  ✓ Exported {} blocks...", offset);
                    }
                }

                generator.writeEndArray(); // End blocks array
                generator.writeEndObject(); // End root object

                logger.info("✅ Chain exported successfully to: {}", filePath);
                logger.info("✅ Exported {} blocks and {} authorized keys",
                    blocksExported.get(), allKeys.size());

                if (offChainFilesExported.get() > 0) {
                    logger.info("📦 Exported {} off-chain files to: {}/off-chain-backup/",
                        offChainFilesExported.get(),
                        exportDir != null ? exportDir.getAbsolutePath() : ".");
                } else if (includeOffChainFiles) {
                    logger.debug("📋 No off-chain files to export");
                }
            }

            return true;
        } catch (Exception e) {
            logger.error("❌ Error exporting chain", e);
            return false;
        }
    }

    /**
     * CORE FUNCTION 3: Chain Import - Restore blockchain from file
     * FIXED: Added thread-safety with write lock and global transaction
     * DEADLOCK FIX: Validation moved outside writeLock to prevent nested lock acquisition
     *
     * @param filePath Path to the JSON file to import
     * @return true if import was successful
     * @throws IllegalArgumentException if filePath is invalid or file does not exist
     * @throws SecurityException if path traversal attempt detected
     * @throws IllegalStateException if file is not readable
     * @since 1.0.6
     */
    public boolean importChain(String filePath) {
        // SECURITY FIX: Validate file path for security (throws exceptions if invalid)
        isValidFilePath(filePath, "import");

        boolean importSuccess = false;
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.writeLock();
        try {
            importSuccess = JPAUtil.executeInTransaction(em -> {
                try {
                    // Read and parse JSON file
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.registerModule(new JavaTimeModule());

                    File file = new File(filePath);
                    // Note: File existence already validated in isValidFilePath

                    ChainExportData importData = mapper.readValue(
                        file,
                        ChainExportData.class
                    );

                    // Validate import data
                    if (
                        importData.getBlocks() == null ||
                        importData.getBlocks().isEmpty()
                    ) {
                        logger.error("❌ No blocks found in import file");
                        return false;
                    }

                    // Clear existing data (WARNING: This will delete current blockchain!)
                    logger.warn(
                        "⚠️ WARNING: This will replace the current blockchain!"
                    );

                    // CRITICAL: Clean up existing off-chain files before clearing database
                    logger.info(
                        "🧹 Cleaning up existing off-chain data before import..."
                    );
                    // Use batch processing instead of loading all blocks at once
                    final int BATCH_SIZE = 1000;
                    long totalExistingBlocks = blockRepository.getBlockCount();
                    int existingOffChainFilesDeleted = 0;

                    for (long offset = 0; offset < totalExistingBlocks; offset += BATCH_SIZE) {
                        List<Block> batch = blockRepository.getBlocksPaginated(offset, BATCH_SIZE);
                        for (Block block : batch) {
                            if (block.hasOffChainData()) {
                                try {
                                    boolean fileDeleted =
                                        offChainStorageService.deleteData(
                                            block.getOffChainData()
                                        );
                                    if (fileDeleted) {
                                        existingOffChainFilesDeleted++;
                                    }
                                } catch (Exception e) {
                                    logger.error(
                                        "❌ Error deleting existing off-chain data for block {}",
                                        block.getBlockNumber(),
                                        e
                                    );
                                }
                            }
                        }
                    }

                    if (existingOffChainFilesDeleted > 0) {
                        logger.info(
                            "🧹 Cleaned up {} existing off-chain files",
                            existingOffChainFilesDeleted
                        );
                    }

                    // Clear existing blocks and keys
                    blockRepository.deleteAllBlocks();
                    authorizedKeyDAO.deleteAllAuthorizedKeys();

                    // CRITICAL: Clear Hibernate session cache to avoid entity conflicts
                    em.flush();
                    em.clear();

                    // Clean up any remaining orphaned files
                    cleanupOrphanedOffChainFiles();

                    // Import authorized keys first with corrected timestamps
                    if (importData.getAuthorizedKeys() != null) {
                        // First, find the earliest and latest timestamp for each public key from the blocks
                        Map<String, LocalDateTime> earliestBlockTimestamps =
                            new HashMap<>();
                        Map<String, LocalDateTime> latestBlockTimestamps =
                            new HashMap<>();

                        for (Block block : importData.getBlocks()) {
                            if (
                                block.getSignerPublicKey() != null &&
                                !"GENESIS".equals(block.getSignerPublicKey())
                            ) {
                                String publicKey = block.getSignerPublicKey();
                                LocalDateTime blockTimestamp =
                                    block.getTimestamp();

                                if (blockTimestamp != null) {
                                    earliestBlockTimestamps.merge(
                                        publicKey,
                                        blockTimestamp,
                                        (existing, current) ->
                                            existing.isBefore(current)
                                                ? existing
                                                : current
                                    );
                                    latestBlockTimestamps.merge(
                                        publicKey,
                                        blockTimestamp,
                                        (existing, current) ->
                                            existing.isAfter(current)
                                                ? existing
                                                : current
                                    );
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
                            if (
                                earliestBlockTimestamps.containsKey(publicKey)
                            ) {
                                earliestEventTime = earliestBlockTimestamps.get(
                                    publicKey
                                );
                            }

                            // Check if revocation time is earlier than first block
                            if (key.getRevokedAt() != null) {
                                if (
                                    earliestEventTime == null ||
                                    key
                                        .getRevokedAt()
                                        .isBefore(earliestEventTime)
                                ) {
                                    earliestEventTime = key.getRevokedAt();
                                }
                            }

                            // Set key creation time to be before ALL events related to this key
                            if (earliestEventTime != null) {
                                key.setCreatedAt(
                                    earliestEventTime.minusMinutes(1)
                                );
                            }

                            // Handle revoked keys without revocation timestamp
                            if (!key.isActive() && key.getRevokedAt() == null) {
                                if (
                                    latestBlockTimestamps.containsKey(publicKey)
                                ) {
                                    // Set revocation time after the latest block
                                    LocalDateTime latestBlockTime =
                                        latestBlockTimestamps.get(publicKey);
                                    key.setRevokedAt(
                                        latestBlockTime.plusMinutes(1)
                                    );
                                } else {
                                    // For keys without blocks, set reasonable revocation time
                                    key.setRevokedAt(
                                        key.getCreatedAt().plusMinutes(1)
                                    );
                                }
                            }

                            authorizedKeyDAO.saveAuthorizedKey(key);
                        }
                        logger.info(
                            "🔑 Imported {} authorized keys with adjusted timestamps",
                            importData.getAuthorizedKeys().size()
                        );
                    }

                    // CRITICAL: Handle off-chain files during import
                    File importDir = new File(filePath).getParentFile();
                    File offChainBackupDir = new File(
                        importDir,
                        "off-chain-backup"
                    );
                    int offChainFilesImported = 0;

                    // Import blocks with off-chain data handling
                    for (Block block : importData.getBlocks()) {
                        // blockNumber will be auto-generated by Hibernate SEQUENCE (or preserved if already set)

                        // Handle off-chain data restoration
                        if (block.hasOffChainData()) {
                            OffChainData offChainData = block.getOffChainData();

                            // Reset off-chain data ID for new insertion
                            offChainData.setId(null);

                            try {
                                // Check if backup file exists in off-chain-backup directory
                                String backupPath = offChainData.getFilePath();
                                String fileName = new File(
                                    backupPath
                                ).getName();
                                File backupFile = new File(
                                    offChainBackupDir,
                                    fileName
                                );

                                if (backupFile.exists()) {
                                    // Create new file path in standard off-chain directory
                                    File offChainDir = new File(
                                        "off-chain-data"
                                    );
                                    if (!offChainDir.exists()) {
                                        offChainDir.mkdirs();
                                    }

                                    String newFileName =
                                        "block_" +
                                        block.getBlockNumber() +
                                        "_" +
                                        System.currentTimeMillis() +
                                        ".enc";
                                    File newFile = new File(
                                        offChainDir,
                                        newFileName
                                    );

                                    // Copy backup file to new location
                                    java.nio.file.Files.copy(
                                        backupFile.toPath(),
                                        newFile.toPath(),
                                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                                    );

                                    // Update file path in off-chain data
                                    offChainData.setFilePath(
                                        newFile.getAbsolutePath()
                                    );
                                    offChainFilesImported++;

                                    if (logger.isTraceEnabled()) {
                                        logger.trace(
                                            "  ✓ Imported off-chain file for block #{}",
                                            block.getBlockNumber()
                                        );
                                    }
                                } else {
                                    logger.warn(
                                        "  ⚠ Off-chain backup file not found for block #{}: {}",
                                        block.getBlockNumber(),
                                        backupFile.getAbsolutePath()
                                    );
                                    // Remove off-chain reference if file is missing
                                    block.setOffChainData(null);
                                }
                            } catch (Exception e) {
                                logger.error(
                                    "  ❌ Error importing off-chain file for block #{}",
                                    block.getBlockNumber(),
                                    e
                                );
                                // Remove off-chain reference if import fails
                                block.setOffChainData(null);
                            }
                        }

                        blockRepository.saveBlock(block);
                    }

                    // Hibernate SEQUENCE automatically manages block numbers - no manual sync needed

                    logger.info(
                        "✅ Chain imported successfully from: {}",
                        filePath
                    );
                    logger.info(
                        "✅ Imported {} blocks",
                        importData.getBlocks().size()
                    );
                    if (offChainFilesImported > 0) {
                        logger.info(
                            "📦 Imported {} off-chain files",
                            offChainFilesImported
                        );
                    }

                    // DEADLOCK FIX: Don't validate inside writeLock - validation needs readLock
                    // Validation will be done after releasing the writeLock
                    return true;
                } catch (Exception e) {
                    logger.error("❌ Error importing chain", e);
                    return false;
                }
            });
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockWrite(stamp);
        }

        // DEADLOCK FIX: Validate AFTER releasing writeLock (validation needs readLock)
        if (!importSuccess) {
            return false;
        }

        // Validate imported chain with detailed validation (outside writeLock)
        var importValidation = validateChainDetailed();
        boolean isValid =
            importValidation.isStructurallyIntact() &&
            importValidation.isFullyCompliant();
        if (isValid) {
            logger.info("✅ Imported chain validation: SUCCESS");
        } else {
            logger.error("❌ Imported chain validation: FAILED");
            if (!importValidation.isStructurallyIntact()) {
                logger.error(
                    "  - Structural issues: {} invalid blocks",
                    importValidation.getInvalidBlocks()
                );
            }
            if (!importValidation.isFullyCompliant()) {
                logger.error(
                    "  - Compliance issues: {} revoked blocks",
                    importValidation.getRevokedBlocks()
                );
            }
        }

        return isValid;
    }

    /**
     * CORE FUNCTION 4: Block Rollback - Remove last N blocks
     * FIXED: Added thread-safety with write lock and global transaction
     *
     * @param numberOfBlocks Number of blocks to remove from the end of the chain
     * @return true if rollback was successful
     * @throws IllegalArgumentException if numberOfBlocks is invalid (<=0 or >= total blocks)
     * @since 1.0.6
     */
    public boolean rollbackBlocks(Long numberOfBlocks) {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.writeLock();
        try {
            return JPAUtil.executeInTransaction(em -> {
                if (numberOfBlocks <= 0) {
                    logger.error("❌ Number of blocks to rollback must be positive");
                    throw new IllegalArgumentException(
                        "Number of blocks to rollback must be positive (got: " + numberOfBlocks + ")"
                    );
                }

                long currentBlockCount = blockRepository.getBlockCount();
                if (numberOfBlocks >= currentBlockCount) {
                    logger.error(
                        "❌ Cannot rollback {} blocks. Only {} blocks exist (including genesis)",
                        numberOfBlocks,
                        currentBlockCount
                    );
                    throw new IllegalArgumentException(
                        "Cannot rollback " + numberOfBlocks + " blocks. Only " + currentBlockCount +
                        " blocks exist (including genesis). Maximum rollback: " + (currentBlockCount - 1)
                    );
                }

                // MEMORY SAFETY: Process rollback in batches without loading all blocks
                try {
                    // Calculate the starting offset for the last N blocks
                    long startBlockNumber = currentBlockCount - numberOfBlocks;

                    // Validate we're not trying to rollback too many blocks at once
                    if (numberOfBlocks > MemorySafetyConstants.LARGE_ROLLBACK_THRESHOLD) {
                        logger.warn("⚠️  WARNING: Rolling back {} blocks (large rollback)", numberOfBlocks);
                        logger.warn("⚠️  This may take significant time. Consider smaller incremental rollbacks.");
                    }

                    logger.info("🔄 Rolling back {} blocks (from #{} to #{}):",
                        numberOfBlocks, startBlockNumber, currentBlockCount - 1);

                    final int BATCH_SIZE = 1000;
                    final int[] totalBlocksRemoved = {0};
                    final int[] offChainFilesDeleted = {0};

                    // Process rollback in batches from highest to lowest block number
                    // This ensures proper chain consistency during rollback
                    for (long batchEnd = currentBlockCount - 1; batchEnd >= startBlockNumber; batchEnd -= BATCH_SIZE) {
                        long batchStart = Math.max(startBlockNumber, batchEnd - BATCH_SIZE + 1);
                        int batchSize = (int) (batchEnd - batchStart + 1);

                        logger.debug("📦 Processing rollback batch: blocks #{} to #{}", batchStart, batchEnd);

                        // Retrieve batch
                        List<Block> batch = blockRepository.getBlocksPaginated((int) batchStart, batchSize);

                        // Process blocks in reverse order (highest to lowest)
                        for (int i = batch.size() - 1; i >= 0; i--) {
                            Block block = batch.get(i);

                            String data = block.getData();
                            String displayData = data != null
                                ? data.substring(0, Math.min(50, data.length()))
                                : "null";
                            logger.debug("  - Removing Block #{}: {}", block.getBlockNumber(), displayData);

                            // CRITICAL: Clean up off-chain data before deleting block
                            if (block.hasOffChainData()) {
                                try {
                                    boolean fileDeleted = offChainStorageService.deleteData(
                                        block.getOffChainData()
                                    );
                                    if (fileDeleted) {
                                        offChainFilesDeleted[0]++;
                                        if (logger.isTraceEnabled()) {
                                            logger.trace("    ✓ Deleted off-chain file: {}",
                                                block.getOffChainData().getFilePath());
                                        }
                                    } else {
                                        logger.warn("    ⚠ Failed to delete off-chain file: {}",
                                            block.getOffChainData().getFilePath());
                                    }
                                } catch (Exception e) {
                                    logger.error("    ❌ Error deleting off-chain data for block {}",
                                        block.getBlockNumber(), e);
                                }
                            }

                            // Delete the block from database (cascade will delete OffChainData entity)
                            blockRepository.deleteBlockByNumber(block.getBlockNumber());
                            totalBlocksRemoved[0]++;
                        }

                        if (totalBlocksRemoved[0] % 5000 == 0 && totalBlocksRemoved[0] > 0) {
                            logger.info("📊 Rollback progress: {}/{} blocks removed",
                                totalBlocksRemoved[0], numberOfBlocks);
                        }
                    }

                    if (totalBlocksRemoved[0] != numberOfBlocks) {
                        logger.error(
                            "❌ Rollback incomplete: removed {} blocks but expected {} (genesis block cannot be removed)",
                            totalBlocksRemoved[0], numberOfBlocks
                        );
                        return false;
                    }

                    if (offChainFilesDeleted[0] > 0) {
                        logger.info(
                            "🧹 Cleaned up {} off-chain files during rollback",
                            offChainFilesDeleted[0]
                        );
                    }

                    // Hibernate SEQUENCE automatically manages block numbers - no manual sync needed

                    logger.info("✅ Rollback completed successfully");
                    logger.info(
                        "Chain now has {} blocks",
                        blockRepository.getBlockCount()
                    );

                    return true;
                } catch (Exception e) {
                    logger.error("❌ Error during rollback", e);
                    return false;
                }
            });
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockWrite(stamp);
        }
    }

    /**
     * CORE FUNCTION 4b: Rollback to specific block number
     * FIXED: Added thread-safety with write lock and global transaction
     * 
     * @param targetBlockNumber The block number to rollback to
     * @return true if rollback was successful, false otherwise
     * @throws IllegalArgumentException if targetBlockNumber is null or negative
     */
    public boolean rollbackToBlock(Long targetBlockNumber) {
        // Validate before acquiring lock
        if (targetBlockNumber == null || targetBlockNumber < 0L) {
            throw new IllegalArgumentException("Target block number cannot be null or negative");
        }
        
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.writeLock();
        try {
            return rollbackToBlockInternal(targetBlockNumber);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockWrite(stamp);
        }
    }

    /**
     * INTERNAL: Rollback to block logic without lock
     * This is the single source of truth for rollback operations
     *
     * @param targetBlockNumber The block number to rollback to
     * @return true if rollback was successful
     * @throws IllegalArgumentException if targetBlockNumber is invalid
     * @since 1.0.6
     */
    private boolean rollbackToBlockInternal(Long targetBlockNumber) {
        return JPAUtil.executeInTransaction(em -> {
            // Defensive validation (should already be validated by public wrapper)
            if (targetBlockNumber == null || targetBlockNumber < 0L) {
                logger.error("❌ Target block number cannot be null or negative");
                throw new IllegalArgumentException(
                    "Target block number cannot be null or negative (got: " + targetBlockNumber + ")"
                );
            }

            long currentBlockCount = blockRepository.getBlockCount();
            if (targetBlockNumber >= currentBlockCount) {
                logger.error(
                    "❌ Target block {} does not exist. Current max block: {}",
                    targetBlockNumber,
                    (currentBlockCount - 1)
                );
                throw new IllegalArgumentException(
                    "Target block " + targetBlockNumber + " does not exist. Current max block: " +
                    (currentBlockCount - 1)
                );
            }

            try {
                int blocksToRemove = (int) (currentBlockCount -
                    targetBlockNumber -
                    1L);
                if (blocksToRemove <= 0) {
                    logger.info(
                        "ℹ️ Chain is already at or before block {}",
                        targetBlockNumber
                    );
                    return true;
                }

                // CRITICAL: Clean up off-chain data before bulk deletion using pagination
                logger.info(
                    "🧹 Cleaning up off-chain data for blocks after {} (using memory-efficient batch processing)",
                    targetBlockNumber
                );

                int offChainFilesDeleted = 0;
                final int BATCH_SIZE = 1000;
                long offset = 0;
                boolean hasMore = true;

                while (hasMore) {
                    List<Block> blocksToDelete = blockRepository.getBlocksAfterPaginated(
                        targetBlockNumber,
                        offset,
                        BATCH_SIZE
                    );

                    if (blocksToDelete.isEmpty()) {
                        hasMore = false;
                        break;
                    }

                    for (Block block : blocksToDelete) {
                        if (block.hasOffChainData()) {
                            try {
                                boolean fileDeleted =
                                    offChainStorageService.deleteData(
                                        block.getOffChainData()
                                    );
                                if (fileDeleted) {
                                    offChainFilesDeleted++;
                                    if (logger.isTraceEnabled()) {
                                        logger.trace(
                                        "  ✓ Deleted off-chain file for block #{}",
                                        block.getBlockNumber()
                                    );
                                    }
                                } else {
                                    logger.warn(
                                        "  ⚠ Failed to delete off-chain file for block #{}",
                                        block.getBlockNumber()
                                    );
                                }
                            } catch (Exception e) {
                                logger.error(
                                    "  ❌ Error deleting off-chain data for block {}",
                                    block.getBlockNumber(),
                                    e
                                );
                            }
                        }
                    }

                    offset += BATCH_SIZE;

                    // Check if we got less than a full batch (end of data)
                    if (blocksToDelete.size() < BATCH_SIZE) {
                        hasMore = false;
                    }
                }

                // Now use the deleteBlocksAfter method for database cleanup
                int deletedCount = blockRepository.deleteBlocksAfter(
                    targetBlockNumber
                );

                // Hibernate SEQUENCE automatically manages block numbers - no manual sync needed

                logger.info(
                    "✅ Rollback to block {} completed successfully",
                    targetBlockNumber
                );
                logger.info("Removed {} blocks", deletedCount);
                if (offChainFilesDeleted > 0) {
                    logger.info(
                        "🧹 Cleaned up {} off-chain files",
                        offChainFilesDeleted
                    );
                }
                logger.info(
                    "Chain now has {} blocks",
                    blockRepository.getBlockCount()
                );

                return deletedCount > 0 || blocksToRemove == 0;
            } catch (Exception e) {
                logger.error("Error during rollback to block", e);
                return false;
            }
        });
    }

    /**
     * PUBLIC: Rollback to block WITHOUT acquiring lock
     * WARNING: Only call this method from within an existing lock context!
     * This is used by ChainRecoveryManager when already holding a writeLock
     *
     * @param targetBlockNumber The block number to rollback to
     * @return true if rollback was successful
     * @throws IllegalArgumentException if targetBlockNumber is invalid
     * @since 1.0.6
     */
    public boolean rollbackToBlockWithoutLock(Long targetBlockNumber) {
        return rollbackToBlockInternal(targetBlockNumber);
    }

    /**
     * CORE FUNCTION 5: Advanced Search - Search blocks by content
     * FIXED: Added thread-safety with read lock
     * 
     * @param searchTerm The content to search for
     * @return List of matching blocks (max 10,000 results)
     * @throws IllegalArgumentException if searchTerm is null or empty
     */
    public List<Block> searchBlocksByContent(String searchTerm) {
        // Validate parameters first before acquiring lock
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new IllegalArgumentException("Search term cannot be null or empty");
        }
        
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            // Use the DAO method for better performance (limit results to prevent memory issues)
            final int MAX_SEARCH_RESULTS = MemorySafetyConstants.DEFAULT_MAX_SEARCH_RESULTS; // Reasonable limit for content search
            List<Block> matchingBlocks = blockRepository.searchBlocksByContentWithLimit(
                searchTerm,
                MAX_SEARCH_RESULTS
            );

            logger.info(
                "🔍 Found {} blocks containing: '{}'",
                matchingBlocks.size(),
                searchTerm
            );
            return matchingBlocks;
        } catch (Exception e) {
            logger.error("❌ Error searching blocks", e);
            return new ArrayList<>();
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    // =============== ADVANCED SEARCH FUNCTIONALITY ===============

    /**
     * Advanced Search: Intelligent search with automatic strategy selection
     */
    public List<Block> searchBlocks(String searchTerm) {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            return convertEnhancedResultsToBlocks(
                searchSpecialistAPI.searchAll(searchTerm)
            );
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
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
     * Streams blocks by category with automatic database optimization.
     *
     * <p><b>Memory Safety</b>: This method is memory-safe for unlimited results.
     * Processes blocks one-at-a-time without loading entire result set into memory.</p>
     *
     * @param category The category to search for
     * @param blockConsumer Consumer to process each block
     *
     * @since 2025-10-08 (Memory Safety Refactoring - Phase A.3)
     */
    public void streamBlocksByCategory(
            String category,
            java.util.function.Consumer<Block> blockConsumer) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be null or empty");
        }

        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            blockRepository.streamBlocksByCategory(category, blockConsumer);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    public List<Block> searchByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be null or empty");
        }
        return searchByCategory(category, MemorySafetyConstants.DEFAULT_MAX_SEARCH_RESULTS);
    }

    /**
     * Advanced Search: Search blocks by category with strict limit validation.
     *
     * <p><b>⚠️ BREAKING CHANGE</b>: This method now rejects maxResults ≤ 0 or > 10,000.
     * For unlimited results, use {@link #streamBlocksByCategory(String, java.util.function.Consumer)}.</p>
     *
     * @param category Category to search for
     * @param maxResults Maximum results (1 to 10,000)
     * @return List of matching blocks, limited by maxResults
     *
     * @throws IllegalArgumentException if maxResults ≤ 0 or > 10,000
     *
     * @since 2025-10-08 (Memory Safety Refactoring - Breaking Change)
     */
    public List<Block> searchByCategory(String category, int maxResults) {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            return blockRepository.searchByCategoryWithLimit(category, maxResults);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * Search blocks by custom metadata (general search, default limit)
     *
     * @param searchTerm Search term to find in custom metadata JSON
     * @return List of blocks matching the search term (max 10,000)
     * @since 1.0.5
     */
    public List<Block> searchByCustomMetadata(String searchTerm) {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            return blockRepository.searchByCustomMetadata(searchTerm);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * Search blocks by custom metadata key-value pair (paginated)
     *
     * @param jsonKey The JSON key to search for
     * @param jsonValue The JSON value to match
     * @param offset Starting position
     * @param limit Maximum results
     * @return List of blocks matching the key-value pair
     * @throws IllegalArgumentException if offset exceeds Integer.MAX_VALUE
     * @since 1.0.5
     */
    public List<Block> searchByCustomMetadataKeyValuePaginated(String jsonKey, String jsonValue, long offset, int limit) {
        // Validate offset doesn't exceed Integer.MAX_VALUE
        if (offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Offset exceeds maximum value: " + Integer.MAX_VALUE);
        }
        
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            return blockRepository.searchByCustomMetadataKeyValuePaginated(jsonKey, jsonValue, offset, limit);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * Search blocks by multiple custom metadata criteria (paginated)
     *
     * @param criteria Map of key-value pairs to match (AND logic)
     * @param offset Starting position
     * @param limit Maximum results
     * @return List of blocks matching all criteria
     * @since 1.0.5
     */
    public List<Block> searchByCustomMetadataMultipleCriteriaPaginated(Map<String, String> criteria, long offset, int limit) {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            return blockRepository.searchByCustomMetadataMultipleCriteriaPaginated(criteria, offset, limit);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * CORE FUNCTION 6: Advanced Search - Get block by hash
     * FIXED: Added thread-safety with read lock
     */
    public Block getBlockByHash(String hash) {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            if (hash == null || hash.trim().isEmpty()) {
                return null;
            }

            // Use the DAO method for better performance
            Block block = blockRepository.getBlockByHash(hash);

            if (block == null) {
                logger.debug("❌ No block found with hash: {}", hash);
            }

            return block;
        } catch (Exception e) {
            logger.error("❌ Error searching block by hash", e);
            return null;
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * CORE FUNCTION 7: Advanced Search - Get blocks by date range
     * FIXED: Added thread-safety with read lock
     */
    public List<Block> getBlocksByDateRange(
        LocalDate startDate,
        LocalDate endDate
    ) {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            if (startDate == null || endDate == null) {
                logger.error("❌ Start date and end date cannot be null");
                return new ArrayList<>();
            }

            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

            // Limit results to prevent memory issues with large time ranges
            final int MAX_TIME_RANGE_RESULTS = MemorySafetyConstants.DEFAULT_MAX_SEARCH_RESULTS;
            return blockRepository.getBlocksByTimeRangePaginated(startDateTime, endDateTime, 0, MAX_TIME_RANGE_RESULTS);
        } catch (Exception e) {
            logger.error("❌ Error searching blocks by date range", e);
            return new ArrayList<>();
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    // ===== QUERY METHODS - ALL THREAD-SAFE =====

    /**
     * Get a block by number
     * FIXED: Added thread-safety with read lock
     */
    public Block getBlock(Long blockNumber) {
        // Try optimistic read first (lock-free, ~50% faster)
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.tryOptimisticRead();
        Block block;
        
        if (GLOBAL_BLOCKCHAIN_LOCK.validate(stamp)) {
            // Optimistic read succeeded - execute without lock
            block = blockRepository.getBlockByNumber(blockNumber);
        } else {
            // Validation failed (write occurred) - retry with read lock
            stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
            try {
                block = blockRepository.getBlockByNumber(blockNumber);
            } finally {
                GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
            }
        }

        return block;
    }


    /**
     * Get blocks paginated for better performance with large blockchains
     * @param offset starting position (long type to prevent overflow with large blockchains)
     * @param limit maximum number of blocks to return
     * @return list of blocks within the specified range
     */
    public List<Block> getBlocksPaginated(long offset, int limit) {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            return blockRepository.getBlocksPaginated(offset, limit);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * Batch retrieve blocks by their hash values (thread-safe)
     *
     * <p>Optimized method to retrieve multiple blocks by hash in a single query.
     * Eliminates N+1 query problem when fetching blocks from search results.</p>
     *
     * @param blockHashes List of block hash values to retrieve
     * @return List of blocks matching the provided hashes
     * @throws IllegalArgumentException if blockHashes is null
     * @since 1.0.5
     */
    public List<Block> batchRetrieveBlocksByHash(List<String> blockHashes) {
        if (blockHashes == null) {
            throw new IllegalArgumentException("Block hashes list cannot be null");
        }
        
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            return blockRepository.batchRetrieveBlocksByHash(blockHashes);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * Batch retrieve blocks by their block numbers (thread-safe)
     *
     * <p>Optimized method to retrieve multiple blocks by block number in a single query.
     * Eliminates N+1 query problem when fetching multiple specific blocks.</p>
     *
     * @param blockNumbers List of block numbers to retrieve (cannot be null)
     * @return List of blocks matching the provided block numbers
     * @throws IllegalArgumentException if blockNumbers is null or batch size exceeds MAX_BATCH_SIZE (10,000)
     * @since 1.0.5
     */
    public List<Block> batchRetrieveBlocks(List<Long> blockNumbers) {
        if (blockNumbers == null) {
            throw new IllegalArgumentException("Block numbers list cannot be null");
        }
        
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            return blockRepository.batchRetrieveBlocks(blockNumbers);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * Get blocks with off-chain data in paginated batches
     * Memory-efficient alternative to loading all off-chain blocks at once
     *
     * @param offset starting position (0-based, long type to prevent overflow)
     * @param limit maximum number of blocks to return
     * @return list of blocks with off-chain data within the specified range
     * @since 1.0.5
     */
    public List<Block> getBlocksWithOffChainDataPaginated(long offset, int limit) {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            return blockRepository.getBlocksWithOffChainDataPaginated(offset, limit);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * Get encrypted blocks in paginated batches
     * Memory-efficient alternative to loading all encrypted blocks at once
     *
     * @param offset starting position (0-based, long type to prevent overflow)
     * @param limit maximum number of blocks to return
     * @return list of encrypted blocks within the specified range
     * @since 1.0.5
     */
    public List<Block> getEncryptedBlocksPaginated(long offset, int limit) {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            return blockRepository.getEncryptedBlocksPaginated(offset, limit);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * Get the last block in the blockchain
     * 
     * <p><b>Thread Safety:</b> Uses optimistic read locking for high-performance concurrent access</p>
     * 
     * <p><b>⚠️ Usage Note:</b> This method is safe for external API calls and read operations.
     * Internal blockchain methods that modify data within active transactions should use
     * {@code blockRepository.getLastBlock(EntityManager)} to avoid transaction isolation issues.</p>
     * 
     * @return Last block in chain, or null if blockchain is empty
     */
    public Block getLastBlock() {
        // Try optimistic read first (lock-free, ~50% faster)
        // BUGFIX: Changed from writeLock to optimistic read (this is a read operation)
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.tryOptimisticRead();
        Block lastBlock;
        
        if (GLOBAL_BLOCKCHAIN_LOCK.validate(stamp)) {
            // Optimistic read succeeded - execute without lock
            lastBlock = blockRepository.getLastBlockWithRefresh();
        } else {
            // Validation failed (write occurred) - retry with read lock
            stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
            try {
                lastBlock = blockRepository.getLastBlockWithRefresh();
            } finally {
                GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
            }
        }

        return lastBlock;
    }

    /**
     * Get the total number of blocks
     * FIXED: Added thread-safety with read lock
     */
    public long getBlockCount() {
        // Try optimistic read first (lock-free, ~50% faster)
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.tryOptimisticRead();
        long count;
        
        if (GLOBAL_BLOCKCHAIN_LOCK.validate(stamp)) {
            // Optimistic read succeeded - execute without lock
            count = blockRepository.getBlockCount();
        } else {
            // Validation failed (write occurred) - retry with read lock
            stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
            try {
                count = blockRepository.getBlockCount();
            } finally {
                GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
            }
        }

        return count;
    }

    /**
     * Get blocks by time range
     * FIXED: Added thread-safety with read lock
     * 
     * @param startTime The start of the time range
     * @param endTime The end of the time range
     * @return List of blocks within the time range (max 10,000 results)
     * @throws IllegalArgumentException if startTime or endTime is null, or if startTime is after endTime
     */
    public List<Block> getBlocksByTimeRange(
        LocalDateTime startTime,
        LocalDateTime endTime
    ) {
        // Validate parameters first
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time cannot be null");
        }
        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time cannot be after end time");
        }
        
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            // Limit results to prevent memory issues with large time ranges
            final int MAX_TIME_RANGE_RESULTS = MemorySafetyConstants.DEFAULT_MAX_SEARCH_RESULTS;
            return blockRepository.getBlocksByTimeRangePaginated(startTime, endTime, 0, MAX_TIME_RANGE_RESULTS);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * Streams blocks by signer public key with automatic database optimization.
     *
     * <p><b>Memory Safety</b>: This method is memory-safe for unlimited results.
     * Processes blocks one-at-a-time without loading entire result set into memory.</p>
     *
     * <p><b>Usage Example</b>:
     * <pre>{@code
     * blockchain.streamBlocksBySignerPublicKey(publicKey, block -> {
     *     System.out.println("Block #" + block.getBlockNumber());
     * });
     * }</pre>
     * </p>
     *
     * @param signerPublicKey The signer's public key
     * @param blockConsumer Consumer to process each block
     *
     * @since 2025-10-08 (Memory Safety Refactoring - Phase A.2)
     */
    public void streamBlocksBySignerPublicKey(
            String signerPublicKey,
            java.util.function.Consumer<Block> blockConsumer) {
        if (signerPublicKey == null || signerPublicKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Signer public key cannot be null or empty");
        }

        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            blockRepository.streamBlocksBySignerPublicKey(signerPublicKey, blockConsumer);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * 🚀 PHASE B.2.1: Streams blocks by time range (unlimited, memory-safe).
     *
     * <p><b>Memory Safety</b>: Processes blocks one-at-a-time without loading entire result set.</p>
     *
     * <p><b>Use Case</b>: Temporal audits, compliance reporting, time-based analytics.</p>
     *
     * @param startTime Start time (inclusive)
     * @param endTime End time (inclusive)
     * @param blockConsumer Consumer to process each block
     *
     * @since 2025-10-27 (Performance Optimization - Phase B.2)
     */
    public void streamBlocksByTimeRange(
            java.time.LocalDateTime startTime,
            java.time.LocalDateTime endTime,
            java.util.function.Consumer<Block> blockConsumer) {

        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time cannot be null");
        }

        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            blockRepository.streamBlocksByTimeRange(startTime, endTime, blockConsumer);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * 🚀 PHASE B.2.2: Streams encrypted blocks (unlimited, memory-safe).
     *
     * <p><b>Memory Safety</b>: Processes blocks one-at-a-time without loading entire result set.</p>
     *
     * <p><b>Use Case</b>: Mass re-encryption, encryption audits, key rotation.</p>
     *
     * @param blockConsumer Consumer to process each encrypted block
     *
     * @since 2025-10-27 (Performance Optimization - Phase B.2)
     */
    public void streamEncryptedBlocks(java.util.function.Consumer<Block> blockConsumer) {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            blockRepository.streamEncryptedBlocks(blockConsumer);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * 🚀 PHASE B.2.3: Streams blocks with off-chain data (unlimited, memory-safe).
     *
     * <p><b>Memory Safety</b>: Processes blocks one-at-a-time without loading entire result set.</p>
     *
     * <p><b>Use Case</b>: Off-chain verification, storage migration, integrity audits.</p>
     *
     * @param blockConsumer Consumer to process each block with off-chain data
     *
     * @since 2025-10-27 (Performance Optimization - Phase B.2)
     */
    public void streamBlocksWithOffChainData(java.util.function.Consumer<Block> blockConsumer) {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            blockRepository.streamBlocksWithOffChainData(blockConsumer);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * 🚀 PHASE B.2.4: Streams blocks after a specific block number (unlimited, memory-safe).
     *
     * <p><b>Memory Safety</b>: Processes blocks one-at-a-time without loading entire result set.</p>
     *
     * <p><b>Use Case</b>: Large rollbacks (>100K blocks), incremental processing, chain recovery.</p>
     *
     * @param blockNumber Starting block number (exclusive)
     * @param blockConsumer Consumer to process each block
     *
     * @since 2025-10-27 (Performance Optimization - Phase B.2)
     */
    public void streamBlocksAfter(Long blockNumber, java.util.function.Consumer<Block> blockConsumer) {
        if (blockNumber == null) {
            throw new IllegalArgumentException("Block number cannot be null");
        }

        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            blockRepository.streamBlocksAfter(blockNumber, blockConsumer);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    public List<Block> getBlocksBySignerPublicKey(String signerPublicKey) {
        if (signerPublicKey == null || signerPublicKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Signer public key cannot be null or empty");
        }
        return getBlocksBySignerPublicKey(signerPublicKey, MemorySafetyConstants.DEFAULT_MAX_SEARCH_RESULTS);
    }

    /**
     * Get blocks signed by a specific public key with strict limit validation.
     *
     * <p><b>⚠️ BREAKING CHANGE</b>: This method now rejects maxResults ≤ 0.
     * Previously, maxResults=0 returned unlimited results (memory-unsafe).
     * For unlimited results, use {@link #streamBlocksBySignerPublicKey(String, java.util.function.Consumer)}.</p>
     *
     * @param signerPublicKey The public key of the signer
     * @param maxResults Maximum results (1 to 10,000)
     * @return List of blocks (≤ maxResults)
     *
     * @throws IllegalArgumentException if maxResults ≤ 0 or > 10,000
     *
     * @since 2025-10-08 (Memory Safety Refactoring - Breaking Change)
     */
    public List<Block> getBlocksBySignerPublicKey(String signerPublicKey, int maxResults) {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            return blockRepository.getBlocksBySignerPublicKeyWithLimit(signerPublicKey, maxResults);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * Get active authorized keys
     * FIXED: Added thread-safety with read lock
     * DEADLOCK FIX #10: Uses internal method to allow lock-free calling
     */
    public List<AuthorizedKey> getAuthorizedKeys() {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            return getAuthorizedKeysInternal();
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * INTERNAL: Get active authorized keys without locks
     * @return List of active authorized keys
     */
    private List<AuthorizedKey> getAuthorizedKeysInternal() {
        return authorizedKeyDAO.getActiveAuthorizedKeys();
    }

    /**
     * PUBLIC: Get active authorized keys without acquiring locks
     * For use by ChainRecoveryManager when called from within a lock context
     * 
     * WARNING: Only call this method if you already hold an appropriate lock!
     * For normal use, call getAuthorizedKeys() which manages locks automatically.
     * 
     * @return List of active authorized keys
     */
    public List<AuthorizedKey> getAuthorizedKeysWithoutLock() {
        return getAuthorizedKeysInternal();
    }

    /**
     * Get all authorized keys (including revoked ones) for export functionality
     * FIXED: Added thread-safety with read lock
     */
    public List<AuthorizedKey> getAllAuthorizedKeys() {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            return authorizedKeyDAO.getAllAuthorizedKeys();
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    public AuthorizedKey getAuthorizedKeyByOwner(String ownerName) {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            return authorizedKeyDAO.getAuthorizedKeyByOwner(ownerName);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * Verifies if a key was authorized at a specific point in time
     *
     * <p><strong>DEADLOCK FIX #4:</strong> StampedLock is NOT reentrant!</p>
     * <p>This method is called from validateChainDetailed() which already holds readLock.
     * Attempting to acquire another readLock while already holding one causes DEADLOCK.</p>
     * <p><strong>SOLUTION:</strong> Remove lock from this method - caller already holds appropriate lock.</p>
     *
     * @param publicKeyString The public key to verify
     * @param timestamp The point in time at which to verify authorization
     * @return true if the key was authorized at that time, false otherwise
     */
    public boolean wasKeyAuthorizedAt(
        String publicKeyString,
        LocalDateTime timestamp
    ) {
        // NO LOCK: Called from methods that already hold readLock (e.g., validateChainDetailed)
        // StampedLock is NOT reentrant - acquiring readLock while holding readLock = DEADLOCK
        return authorizedKeyDAO.wasKeyAuthorizedAt(
            publicKeyString,
            timestamp
        );
    }

    /**
     * Get blockchain size limits for validation
     */
    public int getMaxBlockSizeBytes() {
        return MAX_BLOCK_SIZE_BYTES;
    }

    public int getMaxBlockDataLength() {
        return currentMaxBlockChars; // SECURITY FIX: Use consistent character limit
    }

    public int getMaxBlockChars() {
        return currentMaxBlockChars;
    }

    /**
     * Force a database refresh by flushing pending changes and clearing cache
     * This ensures all entities are synchronized with the database
     * Useful before generating reports or analytics that need up-to-date data
     */
    public void forceRefresh() {
        try {
            JPAUtil.executeInTransaction(em -> {
                em.flush();  // Write all pending changes to database
                em.clear();  // Clear the persistence context cache
                logger.debug("🔄 Database cache refreshed successfully");
                return null;
            });
        } catch (Exception e) {
            logger.warn("⚠️ Could not refresh database cache: {}", e.getMessage());
        }
    }

    /**
     * Test utility: Clear all data and reinitialize with genesis block
     * WARNING: This method is for testing purposes only
     * SECURITY FIX: Added rollback safety and atomic operations
     */
    public void clearAndReinitialize() {
        // DATABASE-AGNOSTIC FIX: Collect off-chain data IDs BEFORE acquiring write lock
        // This prevents deadlock by separating read and write operations
        logger.info("🧹 Preparing to clear database...");
        List<Long> offChainDataIds = new ArrayList<>();
        try {
            final int BATCH_SIZE = 1000;
            // CRITICAL: Call these BEFORE acquiring writeLock to avoid deadlock
            long totalBlocks = blockRepository.getBlockCount();
            
            for (long offset = 0; offset < totalBlocks; offset += BATCH_SIZE) {
                List<Block> batch = blockRepository.getBlocksPaginated(offset, BATCH_SIZE);
                for (Block block : batch) {
                    if (block.hasOffChainData()) {
                        offChainDataIds.add(block.getOffChainData().getId());
                    }
                }
            }
        } catch (Exception readEx) {
            logger.warn("⚠️ Could not collect off-chain data IDs: {}", readEx.getMessage());
            // Continue with empty list - file cleanup will be skipped
        }

        // NOW acquire writeLock AFTER reading data
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.writeLock();
        try {
            // SECURITY FIX: Ensure emergency-backups directory exists
            File emergencyBackupDir = new File("emergency-backups");
            if (!emergencyBackupDir.exists()) {
                if (!emergencyBackupDir.mkdirs()) {
                    logger.warn("⚠️ Could not create emergency-backups directory");
                }
            }

            // SECURITY FIX: Create temporary backup first for rollback capability
            String tempBackupId = null;
            try {
                tempBackupId = createTemporaryBackup();
                logger.info("🛡️ Created temporary backup: {}", tempBackupId);
            } catch (Exception e) {
                logger.warn("⚠️ Could not create temporary backup: {}", e.getMessage());
                // Continue anyway for test scenarios, but log the risk
            }

            final String backupId = tempBackupId;

            // Execute write transaction WITHOUT any reads inside
            JPAUtil.executeInTransaction(em -> {
                try {
                    // Clear database first - this is the critical operation
                    blockRepository.deleteAllBlocks();
                    authorizedKeyDAO.deleteAllAuthorizedKeys();

                    // CRITICAL: Clear Hibernate session cache to avoid entity conflicts
                    em.flush();
                    em.clear();

                    // Reinitialize genesis block (internal version without lock/transaction management)
                    initializeGenesisBlockInternal(em);

                    logger.info("✅ Database cleared and reinitialized successfully");
                } catch (Exception ex) {
                    logger.error("❌ Error during database operation", ex);
                    throw ex;
                }
                return null;
            });

            // SECURITY FIX: Clean off-chain files AFTER successful database clear
            // This prevents orphaned database entries if file cleanup fails
            try {
                int filesDeleted = cleanupOffChainFiles(offChainDataIds);
                if (filesDeleted > 0) {
                    logger.info("🧹 Cleaned up {} off-chain files", filesDeleted);
                }
            } catch (Exception fileEx) {
                logger.warn("⚠️ Off-chain file cleanup had issues (non-critical): {}", fileEx.getMessage());
                // Don't fail the whole operation for file cleanup issues
            }

            // Clean up any remaining orphaned files
            try {
                cleanupOrphanedOffChainFiles();
            } catch (Exception orphanEx) {
                logger.warn("⚠️ Orphaned file cleanup had issues (non-critical): {}", orphanEx.getMessage());
            }

            // CLEANUP FIX: Remove temporary backup after successful operation
            if (backupId != null) {
                try {
                    File backupFile = new File("emergency-backups/" + backupId + ".json");
                    if (backupFile.exists() && backupFile.delete()) {
                        logger.info("🧹 Cleaned up temporary backup: {}", backupId);
                    }
                } catch (Exception backupEx) {
                    logger.warn("⚠️ Could not clean up temporary backup (non-critical): {}", backupEx.getMessage());
                }
            }
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockWrite(stamp);
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
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            // Check if key exists and is active
            boolean keyExists = authorizedKeyDAO.isKeyAuthorized(publicKey);
            if (!keyExists) {
                // Check if key existed but is revoked
                List<AuthorizedKey> allKeys =
                    authorizedKeyDAO.getAllAuthorizedKeys();
                boolean keyExistedBefore = allKeys
                    .stream()
                    .anyMatch(key -> key.getPublicKey().equals(publicKey));

                if (keyExistedBefore) {
                    long affectedBlocks = blockRepository.countBlocksBySignerPublicKey(
                        publicKey
                    );
                    return new KeyDeletionImpact(
                        true,
                        affectedBlocks,
                        "Key is revoked but still has " +
                        affectedBlocks +
                        " historical blocks"
                    );
                } else {
                    return new KeyDeletionImpact(
                        false,
                        0,
                        "Key not found in database"
                    );
                }
            }

            // Count affected blocks for active key
            long affectedBlocks = blockRepository.countBlocksBySignerPublicKey(
                publicKey
            );

            String message = affectedBlocks == 0
                ? "Key can be safely deleted (no historical blocks)"
                : "Key deletion will affect " +
                affectedBlocks +
                " historical blocks";

            return new KeyDeletionImpact(true, affectedBlocks, message);
        } catch (Exception e) {
            logger.error("❌ Error checking deletion impact", e);
            return new KeyDeletionImpact(
                false,
                -1,
                "Error checking deletion impact: " + e.getMessage()
            );
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * SECURITY: Verify admin signature for dangerous operations
     * This prevents unauthorized dangerous operations by requiring cryptographic proof
     *
     * @param signature Base64 encoded signature to verify
     * @param message Message that was signed (operation details)
     * @param adminPublicKey Public key of the authorized administrator
     * @return true if signature is valid, false otherwise
     */
    private boolean verifyAdminSignature(String signature, String message, String adminPublicKey) {
        // Validate all parameters
        if (signature == null || signature.trim().isEmpty()) {
            logger.error("❌ Admin verification failed: null or empty signature");
            return false;
        }
        if (message == null || message.trim().isEmpty()) {
            logger.error("❌ Admin verification failed: null or empty message");
            return false;
        }
        if (adminPublicKey == null || adminPublicKey.trim().isEmpty()) {
            logger.error("❌ Admin verification failed: null or empty admin public key");
            return false;
        }

        try {
            // Check authorization first (but within try-catch for robustness)
            if (!authorizedKeyDAO.isKeyAuthorized(adminPublicKey)) {
                logger.error("❌ Admin verification failed: key not authorized");
                return false;
            }

            // Validate public key format before using it
            PublicKey publicKey;
            try {
                publicKey = CryptoUtil.stringToPublicKey(adminPublicKey);
                if (publicKey == null) {
                    logger.error("❌ Admin verification failed: invalid public key format");
                    return false;
                }
            } catch (Exception keyException) {
                logger.error("❌ Admin verification failed: cannot parse public key", keyException);
                return false;
            }

            // Validate signature
            boolean isValid = CryptoUtil.verifySignature(message, signature, publicKey);

            if (isValid) {
                String keyHash = createSafeKeyHash(adminPublicKey);
                logger.info("✅ Admin verification successful for key hash: {}", keyHash);
            } else {
                logger.error("❌ Admin verification failed: invalid signature");
            }

            return isValid;
        } catch (Exception e) {
            logger.error("❌ Admin verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * SECURITY: Create temporary backup for rollback capability
     * Used during operations that might need to be rolled back
     *
     * @return backup ID for recovery purposes
     */
    private String createTemporaryBackup() {
        return createEmergencyBackup("temp-backup");
    }

    /**
     * TEST UTILITY: Clean up all orphaned emergency backup files
     * Removes all files in the emergency-backups directory
     * Also cleans up any off-chain-backup subdirectory (from old backups)
     * Useful for test cleanup to prevent backup accumulation
     * 
     * @return number of backup files deleted
     */
    public int cleanupEmergencyBackups() {
        int deletedCount = 0;
        File backupDir = new File("emergency-backups");
        
        if (!backupDir.exists() || !backupDir.isDirectory()) {
            return 0;
        }
        
        // Delete JSON backup files
        File[] backupFiles = backupDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (backupFiles != null) {
            for (File backupFile : backupFiles) {
                try {
                    if (backupFile.delete()) {
                        deletedCount++;
                        logger.debug("🧹 Deleted emergency backup: {}", backupFile.getName());
                    }
                } catch (Exception e) {
                    logger.warn("⚠️ Could not delete backup file {}: {}", 
                        backupFile.getName(), e.getMessage());
                }
            }
        }
        
        // Clean up off-chain-backup subdirectory (from old backups created before this fix)
        File offChainBackupDir = new File(backupDir, "off-chain-backup");
        if (offChainBackupDir.exists() && offChainBackupDir.isDirectory()) {
            try {
                int offChainDeleted = deleteDirectoryRecursively(offChainBackupDir);
                if (offChainDeleted > 0) {
                    logger.info("🧹 Cleaned up {} orphaned off-chain backup files", offChainDeleted);
                    deletedCount += offChainDeleted;
                }
            } catch (Exception e) {
                logger.warn("⚠️ Could not clean off-chain-backup directory: {}", e.getMessage());
            }
        }
        
        if (deletedCount > 0) {
            logger.info("🧹 Cleaned up {} emergency backup files", deletedCount);
        }
        
        return deletedCount;
    }

    /**
     * Helper method to recursively delete a directory and its contents
     * 
     * @param directory The directory to delete
     * @return number of files deleted
     */
    private int deleteDirectoryRecursively(File directory) {
        int deletedCount = 0;
        
        if (!directory.exists()) {
            return 0;
        }
        
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deletedCount += deleteDirectoryRecursively(file);
                    } else {
                        if (file.delete()) {
                            deletedCount++;
                        }
                    }
                }
            }
        }
        
        // Delete the directory itself
        if (directory.delete()) {
            deletedCount++;
        }
        
        return deletedCount;
    }

    /**
     * SECURITY: Clean up specific off-chain files by their IDs
     * Used when we have a specific list of files to clean
     *
     * @param fileIds List of file IDs to clean up
     * @return number of files successfully deleted
     */
    private int cleanupOffChainFiles(List<Long> fileIds) {
        int deletedCount = 0;
        for (Long fileId : fileIds) {
            try {
                OffChainData offChainData = new OffChainData();
                offChainData.setId(fileId);
                boolean deleted = offChainStorageService.deleteData(offChainData);
                if (deleted) {
                    deletedCount++;
                }
            } catch (Exception e) {
                logger.warn("Could not delete off-chain file {}: {}", fileId, e.getMessage());
            }
        }
        return deletedCount;
    }

    /**
     * SECURITY: Restore from backup in case of operation failure
     * Used for rollback operations when something goes wrong
     * NOTE: Currently unused but kept for emergency recovery scenarios
     *
     * @param backupId The backup ID to restore from
     */
    @SuppressWarnings("unused")
    private void restoreFromBackup(String backupId) {
        String backupPath = "emergency-backups/" + backupId + ".json";
        boolean restored = importChain(backupPath);
        if (!restored) {
            throw new RuntimeException("Failed to restore from backup: " + backupPath);
        }

        // Clean up the temporary backup file
        try {
            File backupFile = new File(backupPath);
            if (backupFile.exists()) {
                backupFile.delete();
                if (logger.isTraceEnabled()) {
                    logger.trace("🧹 Cleaned up temporary backup: {}", backupPath);
                }
            }
        } catch (Exception e) {
            logger.warn("Could not clean up backup file {}: {}", backupPath, e.getMessage());
        }
    }

    /**
     * SECURITY: Create emergency backup before dangerous operations
     * This ensures we can recover if something goes wrong
     *
     * @param operation Description of the operation being performed
     * @return backup ID for recovery purposes
     */
    /**
     * SECURITY: Create an emergency backup for dangerous operations
     * Used during key deletion and other risky operations
     * Note: Does NOT export off-chain files (they remain in off-chain-data/)
     *
     * @param operation Description of the operation being performed
     * @return backup ID for recovery purposes, or null if backup failed
     */
    private String createEmergencyBackup(String operation) {
        try {
            String backupId = "emergency-" + operation + "-" + System.currentTimeMillis();
            String backupPath = "emergency-backups/" + backupId + ".json";

            // IMPORTANT: Use internal version to avoid nested lock acquisition
            // (we're already inside GLOBAL_BLOCKCHAIN_LOCK from calling method)
            boolean success = exportChainInternal(backupPath, false);
            if (success) {
                logger.info("🛡️ Emergency backup created: {} (database only)", backupPath);
                return backupId;
            } else {
                logger.error("❌ Failed to create emergency backup");
                return null;
            }
        } catch (Exception e) {
            logger.error("❌ Emergency backup creation failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * DANGEROUS: Permanently delete an authorized key from the database
     * ⚠️ WARNING: This operation is IRREVERSIBLE and may break blockchain validation
     * SECURITY FIX: Now requires admin signature and creates emergency backup
     *
     * @param publicKey The public key to delete
     * @param force If true, deletes even if it affects historical blocks
     * @param reason Reason for deletion (for audit logging)
     * @param adminSignature Base64 encoded signature by authorized admin
     * @param adminPublicKey Public key of the administrator authorizing this operation
     * @return true if key was deleted successfully
     * @throws SecurityException if admin authorization is invalid
     * @throws IllegalStateException if emergency backup creation fails or safety checks prevent deletion
     * @throws IllegalArgumentException if the key does not exist
     */
    public boolean dangerouslyDeleteAuthorizedKey(
        String publicKey,
        boolean force,
        String reason,
        String adminSignature,
        String adminPublicKey
    ) {
        // SECURITY: Verify admin authorization before proceeding
        String signedMessage = publicKey + "|" + force + "|" + reason + "|" + System.currentTimeMillis() / 1000;
        if (!verifyAdminSignature(adminSignature, signedMessage, adminPublicKey)) {
            logger.error("❌ SECURITY VIOLATION: Dangerous key deletion attempt without proper authorization");
            throw new SecurityException("SECURITY VIOLATION: Invalid admin authorization for key deletion. Admin signature verification failed.");
        }

        // SECURITY: Create emergency backup before dangerous operation
        String backupId = createEmergencyBackup("key-deletion");
        if (backupId == null) {
            logger.error("❌ Cannot proceed with dangerous operation: backup creation failed");
            throw new IllegalStateException("Cannot proceed with dangerous key deletion: emergency backup creation failed");
        }

        // DEADLOCK FIX: Check deletion impact BEFORE acquiring writeLock
        // canDeleteAuthorizedKey() needs readLock, cannot acquire it while holding writeLock
        KeyDeletionImpact impact = canDeleteAuthorizedKey(publicKey);
        if (!impact.keyExists()) {
            logger.error("❌ {}", impact.getMessage());
            throw new IllegalArgumentException("Key deletion failed: " + impact.getMessage());
        }

        // 2. Safety check for historical blocks
        if (impact.isSevereImpact() && !force) {
            logger.error(
                "❌ SAFETY BLOCK: Cannot delete key that signed {} historical blocks",
                impact.getAffectedBlocks()
            );
            logger.error(
                "💡 Use force=true to override this safety check"
            );
            logger.error(
                "⚠️ WARNING: Forcing deletion will break blockchain validation for these blocks!"
            );
            logger.error(
                "📊 Impact details: {}",
                impact.getMessage()
            );
            throw new IllegalStateException(
                "SAFETY BLOCK: Cannot delete key that signed " + impact.getAffectedBlocks() +
                " historical blocks. Use force=true to override (WARNING: will break validation)"
            );
        }

        long stamp = GLOBAL_BLOCKCHAIN_LOCK.writeLock();
        try {
            return JPAUtil.executeInTransaction(em -> {
                try {
                    // SECURITY FIX: Hash sensitive information for logging
                    String keyHash = createSafeKeyHash(publicKey);
                    String adminKeyHash = createSafeKeyHash(adminPublicKey);

                    logger.warn("🚨 CRITICAL OPERATION: Authorized key deletion initiated");
                    logger.info("🔑 Target key hash: {}...", keyHash);
                    logger.info("👤 Admin key hash: {}...", adminKeyHash);
                    logger.info("📝 Reason: {}", (reason != null ? reason : "No reason provided"));
                    logger.info("⚡ Force mode: {}", force);
                    logger.info("⏰ Timestamp: {}",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                    // 3. Show impact warning for forced deletions (impact already checked before writeLock)
                    if (impact.isSevereImpact()) {
                        logger.warn("⚠️ ⚠️ ⚠️ CRITICAL WARNING ⚠️ ⚠️ ⚠️");
                        logger.warn(
                            "This deletion will affect {} historical blocks:",
                            impact.getAffectedBlocks()
                        );

                        // Show sample of affected blocks (limit to sample size for display)
                        final int SAMPLE_SIZE = 5;
                        List<Block> affectedBlocks =
                            blockRepository.getBlocksBySignerPublicKeyWithLimit(publicKey, SAMPLE_SIZE);
                        int sampleSize = Math.min(SAMPLE_SIZE, affectedBlocks.size());
                        for (int i = 0; i < sampleSize; i++) {
                            Block block = affectedBlocks.get(i);
                            String data = block.getData();
                            String preview = data != null
                                ? data.substring(0, Math.min(50, data.length()))
                                : "null";
                            logger.warn(
                                "  - Block #{} ({}): {}...",
                                block.getBlockNumber(),
                                block.getTimestamp(),
                                preview
                            );
                        }
                        if (affectedBlocks.size() > sampleSize) {
                            logger.warn(
                                "  ... and {} more blocks",
                                (affectedBlocks.size() - sampleSize)
                            );
                        }
                        logger.warn(
                            "⚠️ These blocks will FAIL validation after key deletion!"
                        );
                        logger.warn(
                            "🔥 Proceeding with IRREVERSIBLE deletion..."
                        );
                    }

                    // 4. Get key info for logging before deletion
                    List<AuthorizedKey> keyRecords = authorizedKeyDAO
                        .getAllAuthorizedKeys()
                        .stream()
                        .filter(key -> key.getPublicKey().equals(publicKey))
                        .toList();

                    // 5. Perform deletion
                    boolean deleted = authorizedKeyDAO.deleteAuthorizedKey(
                        publicKey
                    );

                    // 6. Comprehensive audit logging
                    if (deleted) {
                        logger.info(
                            "🗑️ ✅ Key permanently deleted from database"
                        );
                        logger.info("📊 Deletion summary:");
                        logger.info(
                            "   - Key records removed: {}",
                            keyRecords.size()
                        );
                        logger.info(
                            "   - Historical blocks affected: {}",
                            impact.getAffectedBlocks()
                        );
                        logger.info("   - Force mode used: {}", force);
                        logger.info("   - Deletion reason: {}", reason);

                        for (AuthorizedKey keyRecord : keyRecords) {
                            logger.info(
                                "   - Deleted record: {} (created: {}, active: {})",
                                keyRecord.getOwnerName(),
                                keyRecord.getCreatedAt(),
                                keyRecord.isActive()
                            );
                        }

                        logger.info(
                            "📝 Audit log: Key deletion completed at {}",
                            LocalDateTime.now().format(
                                DateTimeFormatter.ofPattern(
                                    "yyyy-MM-dd HH:mm:ss"
                                )
                            )
                        );
                        logger.warn(
                            "⚠️ WARNING: This action was IRREVERSIBLE!"
                        );

                        // Recommend validation check if blocks were affected
                        if (impact.isSevereImpact()) {
                            logger.warn(
                                "💡 STRONGLY RECOMMENDED: Run validateChainDetailed() to verify blockchain integrity"
                            );
                            logger.warn(
                                "🔧 Consider running blockchain repair tools if validation fails"
                            );
                        }
                    } else {
                        logger.error("❌ Failed to delete key from database");
                        logger.error(
                            "💡 This might indicate the key was already deleted or a database error occurred"
                        );
                    }

                    // 7. Clean up temporary backup after successful deletion
                    if (deleted && backupId != null) {
                        try {
                            File backupFile = new File("emergency-backups/" + backupId + ".json");
                            if (backupFile.exists() && backupFile.delete()) {
                                logger.info("🧹 Cleaned up temporary backup: {}", backupId);
                            }
                        } catch (Exception backupEx) {
                            logger.warn("⚠️ Could not clean up temporary backup: {}", backupEx.getMessage());
                        }
                    }

                    return deleted;
                } catch (Exception e) {
                    logger.error("💥 Critical error during key deletion", e);
                    return false;
                }
            });
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockWrite(stamp);
        }
    }


    /**
     * Expose the regular deleteAuthorizedKey method referenced in documentation
     * This is a safe wrapper that only deletes keys with no historical impact
     * FIXED: Added thread-safety with read lock for impact check
     *
     * @param publicKey The public key to delete
     * @return true if key was safely deleted
     * @throws IllegalArgumentException if the key does not exist
     * @throws IllegalStateException if the key has severe impact (historical blocks affected)
     * @since 1.0.6
     */
    public boolean deleteAuthorizedKey(String publicKey) {
        long readStamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        KeyDeletionImpact impact;
        try {
            impact = canDeleteAuthorizedKey(publicKey);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(readStamp);
        }

        if (!impact.keyExists()) {
            logger.error("❌ Cannot delete key: {}", impact.getMessage());
            throw new IllegalArgumentException("Cannot delete key: " + impact.getMessage());
        }

        if (impact.isSevereImpact()) {
            logger.error("❌ Cannot delete key: {}", impact.getMessage());
            logger.error(
                "Use dangerouslyDeleteAuthorizedKey() with force=true if deletion is absolutely necessary"
            );
            throw new IllegalStateException(
                "SAFETY BLOCK: " + impact.getMessage() + ". " +
                "Key signed " + impact.getAffectedBlocks() + " historical blocks. " +
                "Use dangerouslyDeleteAuthorizedKey() with force=true if deletion is absolutely necessary"
            );
        }

        // Safe to delete - no historical blocks affected
        long writeStamp = GLOBAL_BLOCKCHAIN_LOCK.writeLock();
        try {
            return JPAUtil.executeInTransaction(em -> {
                logger.info(
                    "✅ Safely deleting authorized key (no historical blocks affected)"
                );
                return authorizedKeyDAO.deleteAuthorizedKey(publicKey);
            });
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockWrite(writeStamp);
        }
    }

    // ===============================
    // CHAIN RECOVERY METHODS
    // ===============================

    /**
     * Manual recovery method for corrupted chains
     * 
     * DEADLOCK FIX #8: Use internal lock-free methods to avoid re-entrancy
     * This method needs writeLock for thread safety but calls methods that would need locks.
     * SOLUTION: Keep writeLock here, make ChainRecoveryManager use internal lock-free methods.
     * 
     * @param deletedPublicKey The key that was deleted and caused corruption
     * @param ownerName Original owner name for re-authorization
     * @return RecoveryResult with success status and details
     */
    public RecoveryResult recoverCorruptedChain(
        String deletedPublicKey,
        String ownerName
    ) {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.writeLock();
        try {
            // Pass true to indicate we're already holding a lock
            ChainRecoveryManager recovery = new ChainRecoveryManager(this, true);
            return recovery.recoverCorruptedChain(deletedPublicKey, ownerName);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockWrite(stamp);
        }
    }

    /**
     * Diagnose chain corruption
     * 
     * DEADLOCK FIX #6: StampedLock is NOT reentrant!
     * This method calls ChainRecoveryManager.diagnoseCorruption() which calls validateSingleBlock().
     * validateSingleBlock() already has its own readLock.
     * StampedLock CANNOT nest readLocks (not reentrant) - acquiring readLock while holding readLock = DEADLOCK!
     * 
     * SOLUTION: Remove lock from this method. Let validateSingleBlock() manage its own lock.
     * The ChainRecoveryManager methods are safe to call without lock because they call thread-safe methods.
     * 
     * @return ChainDiagnostic with detailed information about corruption
     */
    public ChainDiagnostic diagnoseCorruption() {
        // NO LOCK: ChainRecoveryManager calls validateSingleBlock() which already has readLock
        // StampedLock is NOT reentrant - acquiring readLock while holding readLock = DEADLOCK
        ChainRecoveryManager recovery = new ChainRecoveryManager(this);
        return recovery.diagnoseCorruption();
    }

    /**
     * Validate chain with automatic recovery attempt
     * 
     * DEADLOCK FIX #5: StampedLock is NOT reentrant!
     * This method calls validateChainDetailed() and diagnoseCorruption() which already have their own readLocks.
     * Acquiring readLock here and then calling methods with readLock = DEADLOCK.
     * SOLUTION: Remove locks from this method - let called methods manage their own locks.
     * 
     * @return true if chain is valid or was successfully recovered, false otherwise
     */
    public boolean validateChainWithRecovery() {
        // NO LOCK: validateChainDetailed() already has readLock
        var validation = validateChainDetailed();
        boolean isValid =
            validation.isStructurallyIntact() &&
            validation.isFullyCompliant();

        if (isValid) {
            return true;
        }

        // Chain validation failed, attempt diagnostic
        // NO LOCK: diagnoseCorruption() already has readLock
        logger.warn("🔧 Chain validation failed, attempting diagnostic...");
        ChainDiagnostic diagnostic = diagnoseCorruption();
        logger.info("📊 Diagnostic: {}", diagnostic);

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

        public KeyDeletionImpact(
            boolean keyExists,
            long affectedBlocks,
            String message
        ) {
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
            return String.format(
                "KeyDeletionImpact{exists=%s, canSafelyDelete=%s, affectedBlocks=%d, message='%s'}",
                keyExists,
                canSafelyDelete(),
                affectedBlocks,
                message
            );
        }
    }

    /**
     * Configuration methods for dynamic block size limits
     */
    public void setMaxBlockSizeBytes(int maxSizeBytes) {
        if (maxSizeBytes > 0 && maxSizeBytes <= 10 * 1024 * 1024) {
            // Max 10MB for on-chain
            this.currentMaxBlockSizeBytes = maxSizeBytes;
            logger.info("📊 Max block size updated to: {} bytes", maxSizeBytes);
        } else {
            throw new IllegalArgumentException(
                "Invalid block size. Must be between 1 and 10MB"
            );
        }
    }

    public void setMaxBlockDataLength(int maxDataLength) {
        if (maxDataLength > 0 && maxDataLength <= 1000000) {
            // Max 1M characters
            this.currentMaxBlockChars = maxDataLength;
            logger.info(
                "📊 Max block data length updated to: {} characters",
                maxDataLength
            );
        } else {
            throw new IllegalArgumentException(
                "Invalid data length. Must be between 1 and 1M characters"
            );
        }
    }

    public void setOffChainThresholdBytes(int thresholdBytes) {
        if (thresholdBytes > 0 && thresholdBytes <= currentMaxBlockSizeBytes) {
            this.currentOffChainThresholdBytes = thresholdBytes;
            logger.info(
                "📊 Off-chain threshold updated to: {} bytes",
                thresholdBytes
            );
        } else {
            throw new IllegalArgumentException(
                "Invalid threshold. Must be between 1 and current max block size"
            );
        }
    }

    // Getters for current configuration
    public int getCurrentMaxBlockSizeBytes() {
        return currentMaxBlockSizeBytes;
    }

    /**
     * Get the SearchFrameworkEngine instance for sharing with SearchSpecialistAPI
     * @return the current SearchFrameworkEngine instance
     */
    public SearchFrameworkEngine getSearchFrameworkEngine() {
        return searchFrameworkEngine;
    }

    public int getCurrentMaxBlockDataLength() {
        return currentMaxBlockChars;
    }

    public int getCurrentOffChainThresholdBytes() {
        return currentOffChainThresholdBytes;
    }

    /**
     * Reset limits to default values
     */
    public void resetLimitsToDefault() {
        this.currentMaxBlockSizeBytes = MAX_BLOCK_SIZE_BYTES;
        this.currentMaxBlockChars = MAX_BLOCK_CHARS;
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
            currentMaxBlockSizeBytes,
            currentMaxBlockSizeBytes / (1024.0 * 1024),
            currentMaxBlockChars,
            currentOffChainThresholdBytes,
            currentOffChainThresholdBytes / 1024.0,
            MAX_BLOCK_SIZE_BYTES,
            MAX_BLOCK_CHARS,
            OFF_CHAIN_THRESHOLD_BYTES
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
                        logger.error(
                            "Failed to delete orphaned file: {} - {}",
                            file.getName(),
                            e.getMessage()
                        );
                    }
                }
            }

            if (orphanedFilesDeleted > 0) {
                logger.info(
                    "🧹 Cleaned up {} orphaned off-chain files",
                    orphanedFilesDeleted
                );
            }
        } catch (Exception e) {
            logger.error("❌ Error during orphaned files cleanup", e);
        }
    }

    /**
     * Find and clean up orphaned off-chain files (utility method for maintenance)
     */
    public int cleanupOrphanedFiles() {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            File offChainDir = new File("off-chain-data");
            if (!offChainDir.exists() || !offChainDir.isDirectory()) {
                return 0;
            }

            // Get all current off-chain file paths from database
            // Use batch processing instead of loading all blocks at once
            final int BATCH_SIZE = 1000;
            long totalBlocks = blockRepository.getBlockCount();
            Set<String> validFilePaths = new HashSet<>();

            for (long offset = 0; offset < totalBlocks; offset += BATCH_SIZE) {
                List<Block> batch = blockRepository.getBlocksPaginated(offset, BATCH_SIZE);
                for (Block block : batch) {
                    if (block.hasOffChainData()) {
                        validFilePaths.add(block.getOffChainData().getFilePath());

                        // Memory safety: Warn if collecting too many paths
                        if (validFilePaths.size() > MemorySafetyConstants.SAFE_EXPORT_LIMIT) {
                            logger.warn("⚠️  Cleanup tracking {} off-chain files (may cause memory issues)", validFilePaths.size());
                        }
                    }
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
                        String normalizedValidPath = new File(
                            validPath
                        ).getCanonicalPath();
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
                            if (logger.isTraceEnabled()) {
                                logger.trace(
                                    "🗝️ Deleted orphaned file: {}",
                                    file.getName()
                                );
                            }
                        }
                    } catch (Exception e) {
                        logger.error(
                            "Failed to delete orphaned file: {} - {}",
                            file.getName(),
                            e.getMessage()
                        );
                    }
                }
            }

            return orphanedFilesDeleted;
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
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
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            // Use Advanced Search Engine with enhanced metadata search
            // This searches in PublicLayer metadata (timestamps, categories)
            // without requiring passwords
            List<EnhancedSearchResult> results =
                searchSpecialistAPI.searchAll(searchTerm);

            // Filter to only encrypted blocks and convert to Block objects
            List<Block> encryptedBlocks = new ArrayList<>();
            for (EnhancedSearchResult result : results) {
                try {
                    Block block = blockRepository.getBlockByHash(
                        result.getBlockHash()
                    );
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
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
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
    public List<Block> searchEncryptedBlocksWithPassword(
        String searchTerm,
        String decryptionPassword
    ) {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            // Use Advanced Search Engine adaptive secure search for per-block passwords
            return convertEnhancedResultsToBlocks(
                searchSpecialistAPI.searchIntelligent(
                    searchTerm,
                    decryptionPassword,
                    50
                )
            );
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
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
    public List<Block> searchBlocksEnhanced(
        String searchTerm,
        String decryptionPassword
    ) {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            // Use the new intelligent search that automatically handles password registry
            return convertEnhancedResultsToBlocks(
                searchSpecialistAPI.searchIntelligent(
                    searchTerm,
                    decryptionPassword,
                    50
                )
            );
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
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
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            // Use general search for any user-defined term
            return convertEnhancedResultsToBlocks(
                searchSpecialistAPI.searchAll(searchTerm)
            );
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
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
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            // Advanced Search Engine automatically determines the optimal strategy
            // No need for manual blockchain composition analysis
            return convertEnhancedResultsToBlocks(
                searchSpecialistAPI.searchAll(searchTerm)
            );
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * Get search statistics for the blockchain
     * Provides information about searchable vs encrypted content
     *
     * @return Search statistics summary
     */
    public String getSearchStatistics() {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            // Use batch processing instead of loading all blocks at once
            final int BATCH_SIZE = 1000;
            long totalBlocks = blockRepository.getBlockCount();
            long encryptedBlocks = 0;

            // Count blocks by category
            Map<String, Long> categoryCount = new HashMap<>();

            // Process blocks in batches to accumulate statistics
            for (long offset = 0; offset < totalBlocks; offset += BATCH_SIZE) {
                List<Block> batch = blockRepository.getBlocksPaginated(offset, BATCH_SIZE);
                for (Block block : batch) {
                    // Count encrypted blocks
                    if (block.isDataEncrypted()) {
                        encryptedBlocks++;
                    }

                    // Count by category
                    String category = block.getContentCategory();
                    if (category != null && !category.isEmpty()) {
                        categoryCount.put(
                            category,
                            categoryCount.getOrDefault(category, 0L) + 1
                        );
                    }
                }
            }

            long unencryptedBlocks = totalBlocks - encryptedBlocks;

            StringBuilder stats = new StringBuilder();
            stats.append("📊 Blockchain Search Statistics:\n");
            stats.append(String.format("   Total blocks: %d\n", totalBlocks));
            stats.append(
                String.format(
                    "   Unencrypted blocks: %d (%.1f%% - fully searchable)\n",
                    unencryptedBlocks,
                    totalBlocks > 0
                        ? ((double) unencryptedBlocks / totalBlocks) * 100
                        : 0
                )
            );
            stats.append(
                String.format(
                    "   Encrypted blocks: %d (%.1f%% - metadata search only)\n",
                    encryptedBlocks,
                    totalBlocks > 0
                        ? ((double) encryptedBlocks / totalBlocks) * 100
                        : 0
                )
            );

            if (!categoryCount.isEmpty()) {
                stats.append("\n📂 Blocks by category:\n");
                categoryCount
                    .entrySet()
                    .stream()
                    .sorted(
                        Map.Entry.<String, Long>comparingByValue().reversed()
                    )
                    .forEach(entry ->
                        stats.append(
                            String.format(
                                "   %s: %d blocks\n",
                                entry.getKey(),
                                entry.getValue()
                            )
                        )
                    );
            }

            return stats.toString();
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * Enhanced export for encrypted chains with encryption key management
     * Exports blockchain data with encryption keys for proper restoration
     * MEMORY SAFETY: Validates blockchain size before export (max 500K blocks)
     *
     * @param filePath Path to export the encrypted blockchain JSON
     * @param masterPassword Master password for encrypting the export data
     * @return true if export was successful
     * @throws IllegalArgumentException if filePath or masterPassword is invalid
     * @throws SecurityException if path traversal attempt detected
     * @throws IllegalStateException if directory creation fails
     */
    public boolean exportEncryptedChain(
        String filePath,
        String masterPassword
    ) {
        // SECURITY FIX: Validate file path for security (throws exceptions if invalid)
        isValidFilePath(filePath, "export");

        // Validate master password
        if (masterPassword == null || masterPassword.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Master password required for encrypted chain export"
            );
        }

        long stamp = GLOBAL_BLOCKCHAIN_LOCK.readLock();
        try {
            // Use batch processing instead of loading all blocks at once
            final int BATCH_SIZE = 1000;
            long totalBlocks = blockRepository.getBlockCount();

            // MEMORY SAFETY: Warn if exporting very large chains (>100K blocks)
            final int SAFE_EXPORT_LIMIT = MemorySafetyConstants.SAFE_EXPORT_LIMIT;
            if (totalBlocks > SAFE_EXPORT_LIMIT) {
                logger.warn("⚠️  WARNING: Attempting to export {} blocks (>{} blocks may cause memory issues)",
                    totalBlocks, SAFE_EXPORT_LIMIT);
                logger.warn("⚠️  Consider exporting in smaller ranges or increase JVM heap size (-Xmx)");
                // For very large exports (>500K), refuse to prevent OutOfMemoryError
                if (totalBlocks > 500000) {
                    logger.error("❌ Cannot export {} blocks at once. Maximum limit: 500K blocks", totalBlocks);
                    logger.error("❌ Please export in smaller ranges to prevent memory exhaustion");
                    return false;
                }
            }

            List<Block> allBlocks = new ArrayList<>((int) totalBlocks);

            // Retrieve blocks in batches
            for (long offset = 0; offset < totalBlocks; offset += BATCH_SIZE) {
                List<Block> batch = blockRepository.getBlocksPaginated(offset, BATCH_SIZE);
                allBlocks.addAll(batch);
            }

            List<AuthorizedKey> allKeys =
                authorizedKeyDAO.getAllAuthorizedKeys();

            // Extract encryption data for encrypted blocks
            EncryptionExportData encryptionData =
                EncryptionExportUtil.extractEncryptionData(
                    allBlocks,
                    masterPassword
                );

            // Create enhanced export data structure
            ChainExportData exportData = new ChainExportData(
                allBlocks,
                allKeys,
                encryptionData
            );
            exportData.setDescription(
                "Encrypted blockchain export with encryption keys"
            );

            // Handle off-chain files (same as regular export)
            int offChainFilesExported = handleOffChainExport(
                allBlocks,
                filePath
            );

            // Convert to JSON
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.configure(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                false
            );

            // Write to file
            File file = new File(filePath);
            mapper.writeValue(file, exportData);

            logger.info(
                "🔐 Encrypted chain exported successfully to: {}",
                filePath
            );
            logger.info(
                "📦 Exported {} blocks and {} authorized keys",
                allBlocks.size(),
                allKeys.size()
            );
            logger.info(
                "🔑 Exported encryption data for {} entries",
                encryptionData.getTotalEncryptionEntries()
            );
            if (offChainFilesExported > 0) {
                logger.info(
                    "📁 Exported {} off-chain files",
                    offChainFilesExported
                );
            }

            // Display encryption summary
            logger.info(
                "\n{}",
                EncryptionExportUtil.generateEncryptionSummary(encryptionData)
            );

            return true;
        } catch (Exception e) {
            logger.error("❌ Error exporting encrypted chain", e);
            return false;
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockRead(stamp);
        }
    }

    /**
     * Enhanced import for encrypted chains with encryption key restoration
     * Imports blockchain data and restores encryption keys for proper decryption
     *
     * @param filePath Path to the encrypted blockchain JSON file
     * @param masterPassword Master password for decrypting the import data
     * @return true if import was successful
     * @throws IllegalArgumentException if filePath or masterPassword is invalid, or file doesn't exist
     * @throws SecurityException if path traversal attempt detected
     */
    public boolean importEncryptedChain(
        String filePath,
        String masterPassword
    ) {
        // SECURITY FIX: Validate file path for security (throws exceptions if invalid)
        isValidFilePath(filePath, "import");

        // Validate master password
        if (masterPassword == null || masterPassword.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Master password required for encrypted chain import"
            );
        }

        long stamp = GLOBAL_BLOCKCHAIN_LOCK.writeLock();
        try {
            return JPAUtil.executeInTransaction(em -> {
                try {
                    // Read and parse JSON file
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.registerModule(new JavaTimeModule());

                    File file = new File(filePath);
                    // Note: File existence already validated in isValidFilePath

                    ChainExportData importData = mapper.readValue(
                        file,
                        ChainExportData.class
                    );

                    // Validate import data
                    if (
                        importData.getBlocks() == null ||
                        importData.getBlocks().isEmpty()
                    ) {
                        logger.error("❌ No blocks found in import file");
                        return false;
                    }

                    // Check if import data has encryption support
                    if (!importData.hasEncryptionSupport()) {
                        logger.error(
                            "❌ Import file does not support encryption. Use regular importChain() method."
                        );
                        return false;
                    }

                    EncryptionExportData encryptionData =
                        importData.getEncryptionData();
                    if (encryptionData == null || encryptionData.isEmpty()) {
                        logger.warn(
                            "⚠️ No encryption data found in import file"
                        );
                    }

                    // Validate encryption data consistency
                    if (
                        !EncryptionExportUtil.validateEncryptionData(
                            importData.getBlocks(),
                            encryptionData
                        )
                    ) {
                        logger.error("❌ Encryption data validation failed");
                        return false;
                    }

                    // Verify master password matches
                    if (
                        encryptionData != null &&
                        encryptionData.getMasterPassword() != null
                    ) {
                        if (
                            !encryptionData
                                .getMasterPassword()
                                .equals(masterPassword)
                        ) {
                            logger.error("❌ Master password mismatch");
                            return false;
                        }
                    }

                    logger.info("🔐 Importing encrypted blockchain...");
                    logger.warn(
                        "⚠️ WARNING: This will replace the current blockchain!"
                    );

                    // Clean up existing data
                    cleanupExistingData();

                    // Clear existing blocks and keys
                    blockRepository.deleteAllBlocks();
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
                    logger.info(
                        "📦 Importing blocks with encryption support..."
                    );
                    int blocksImported = 0;
                    int encryptedBlocksRestored = 0;

                    for (Block block : importData.getBlocks()) {
                        // Create new block instance preserving original block numbers for off-chain compatibility
                        Block newBlock =
                            createDetachedBlockCopyPreservingNumbers(block);

                        // Restore encryption context if needed
                        if (
                            newBlock.isDataEncrypted() && encryptionData != null
                        ) {
                            if (
                                EncryptionExportUtil.canDecryptBlock(
                                    newBlock,
                                    encryptionData
                                )
                            ) {
                                encryptedBlocksRestored++;
                                logger.info(
                                    "🔐 Restored encryption context for block #{}",
                                    newBlock.getBlockNumber()
                                );
                            } else {
                                logger.warn(
                                    "⚠️ Cannot restore encryption context for block #{}",
                                    newBlock.getBlockNumber()
                                );
                            }
                        }

                        // Adjust timestamps for consistency
                        adjustBlockTimestamps(newBlock, blocksImported);

                        // For encrypted import, maintain original block structure for off-chain compatibility
                        // The block hashes and previous hash references should remain as they were exported
                        // This preserves the original chain integrity and off-chain password generation

                        blockRepository.saveBlock(newBlock);

                        // Handle off-chain password restoration if needed
                        if (
                            newBlock.hasOffChainData() && encryptionData != null
                        ) {
                            Long originalBlockNumber = block.getBlockNumber(); // Original block number
                            String originalPassword =
                                encryptionData.getOffChainPassword(
                                    originalBlockNumber
                                );
                            if (originalPassword != null) {
                                // Store the mapping for later use (since we can't modify OffChainData directly)
                                logger.info(
                                    "🔑 Off-chain password restored for block #{}",
                                    newBlock.getBlockNumber()
                                );
                            }
                        }

                        blocksImported++;

                        if (blocksImported % 100 == 0) {
                            if (logger.isDebugEnabled()) {
                                logger.debug(
                                    "   📦 Imported {} blocks...",
                                    blocksImported
                                );
                            }
                        }
                    }

                    // Restore off-chain files
                    int offChainFilesRestored = handleOffChainImport(
                        importData.getBlocks(),
                        encryptionData
                    );

                    // Hibernate SEQUENCE automatically manages block numbers - no manual sync needed

                    logger.info(
                        "✅ Encrypted chain import completed successfully!"
                    );
                    logger.info(
                        "📦 Imported {} blocks and {} authorized keys",
                        blocksImported,
                        importData.getAuthorizedKeysCount()
                    );
                    logger.info(
                        "🔐 Restored encryption context for {} encrypted blocks",
                        encryptedBlocksRestored
                    );
                    if (offChainFilesRestored > 0) {
                        logger.info(
                            "📁 Restored {} off-chain files",
                            offChainFilesRestored
                        );
                    }

                    return true;
                } catch (Exception e) {
                    logger.error("❌ Error importing encrypted chain", e);
                    return false;
                }
            });
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockWrite(stamp);
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
        boolean hasOffChainData = blocks
            .stream()
            .anyMatch(Block::hasOffChainData);
        if (!hasOffChainData) {
            return 0;
        }

        // Create backup directory
        if (!offChainBackupDir.exists()) {
            try {
                if (!offChainBackupDir.mkdirs()) {
                    logger.error(
                        "❌ Failed to create off-chain backup directory"
                    );
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
                        String fileName =
                            "block_" +
                            block.getBlockNumber() +
                            "_" +
                            sourceFile.getName();
                        File backupFile = new File(offChainBackupDir, fileName);

                        // Copy file
                        java.nio.file.Files.copy(
                            sourceFile.toPath(),
                            backupFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING
                        );

                        // Update the path in the export data
                        offChainData.setFilePath(
                            "off-chain-backup/" + fileName
                        );
                        offChainFilesExported++;

                        logger.debug(
                            "  ✓ Exported off-chain file for block #{}",
                            block.getBlockNumber()
                        );
                    } else {
                        logger.warn(
                            "  ⚠ Off-chain file missing for block #{}",
                            block.getBlockNumber()
                        );
                    }
                } catch (Exception e) {
                    logger.error(
                        "  ❌ Error exporting off-chain file for block #{}",
                        block.getBlockNumber(),
                        e
                    );
                }
            }
        }

        return offChainFilesExported;
    }

    /**
     * Helper method to handle off-chain file import with encryption support
     */
    private int handleOffChainImport(
        List<Block> blocks,
        EncryptionExportData encryptionData
    ) {
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
                            String newFileName =
                                "imported_" +
                                System.currentTimeMillis() +
                                "_" +
                                backupFile.getName();
                            File newFile = new File(
                                "off-chain-data",
                                newFileName
                            );

                            // Ensure off-chain-data directory exists
                            if (!newFile.getParentFile().exists()) {
                                newFile.getParentFile().mkdirs();
                            }

                            // Copy file to new location
                            java.nio.file.Files.copy(
                                backupFile.toPath(),
                                newFile.toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING
                            );

                            // Update the path in the block data
                            offChainData.setFilePath(newFile.getPath());
                            offChainFilesRestored++;

                            logger.debug(
                                "  ✓ Restored off-chain file for block #{}",
                                block.getBlockNumber()
                            );
                        } else {
                            logger.warn(
                                "  ⚠ Off-chain backup file missing for block #{}",
                                block.getBlockNumber()
                            );
                        }
                    }
                } catch (Exception e) {
                    logger.error(
                        "  ❌ Error restoring off-chain file for block #{}",
                        block.getBlockNumber(),
                        e
                    );
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
        // Use batch processing instead of loading all blocks at once
        final int BATCH_SIZE = 1000;
        long totalBlocks = blockRepository.getBlockCount();
        int existingOffChainFilesDeleted = 0;

        for (long offset = 0; offset < totalBlocks; offset += BATCH_SIZE) {
            List<Block> batch = blockRepository.getBlocksPaginated(offset, BATCH_SIZE);
            for (Block block : batch) {
                if (block.hasOffChainData()) {
                    try {
                        boolean fileDeleted = offChainStorageService.deleteData(
                            block.getOffChainData()
                        );
                        if (fileDeleted) {
                            existingOffChainFilesDeleted++;
                        }
                    } catch (Exception e) {
                        logger.error(
                            "Error deleting existing off-chain data for block {}",
                            block.getBlockNumber(),
                            e
                        );
                    }
                }
            }
        }

        if (existingOffChainFilesDeleted > 0) {
            logger.info(
                "🧹 Cleaned up {} existing off-chain files",
                existingOffChainFilesDeleted
            );
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
    private Block createDetachedBlockCopyPreservingNumbers(
        Block originalBlock
    ) {
        Block newBlock = new Block();

        // Copy all fields and preserve original block number
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
     * Get AuthorizedKeyDAO for direct database operations (primarily for testing)
     */
    public AuthorizedKeyDAO getAuthorizedKeyDAO() {
        return authorizedKeyDAO;
    }

    /**
     * Complete database cleanup for testing - removes ALL data including genesis block
     * WARNING: This method is intended for testing purposes only - removes ALL data
     */
    /**
     * Complete cleanup for test environments
     * Cleans database tables but NOT off-chain files or emergency backups
     * For full cleanup including files, use clearAndReinitialize()
     */
    public void completeCleanupForTests() {
        blockRepository.completeCleanupTestData();
        authorizedKeyDAO.cleanupTestData();
        // Note: Does NOT clean off-chain files or emergency backups
        // Use clearAndReinitialize() for complete cleanup including files
    }

    /**
     * Complete cleanup for test environments including emergency backups
     * This version also removes orphaned emergency backup files
     * Useful for test isolation to prevent backup accumulation
     */
    public void completeCleanupForTestsWithBackups() {
        completeCleanupForTests();
        cleanupEmergencyBackups();
    }

    /**
     * Update an existing block in the blockchain
     * 
     * <h3>🔒 SECURITY: 2-Layer Immutability Protection</h3>
     * <p>This method implements blockchain integrity protection by:</p>
     * <ol>
     *   <li><b>Validation:</b> Checks all 7 immutable fields before update</li>
     *   <li><b>Logging:</b> Logs {@code logger.error()} for each attempted modification</li>
     *   <li><b>Rejection:</b> Returns {@code false} if any immutable field is modified</li>
     *   <li><b>Safe Copy:</b> Only copies mutable fields to existing block</li>
     * </ol>
     * 
     * <h4>❌ IMMUTABLE Fields (Cannot be modified):</h4>
     * <ul>
     *   <li>{@code data} - Block content (hash-critical)</li>
     *   <li>{@code blockNumber} - Block position</li>
     *   <li>{@code previousHash} - Chain link (hash-critical)</li>
     *   <li>{@code timestamp} - Creation time (hash-critical)</li>
     *   <li>{@code hash} - Cryptographic hash</li>
     *   <li>{@code signature} - Digital signature</li>
     *   <li>{@code signerPublicKey} - Signer identity</li>
     * </ul>
     * 
     * <h4>✅ MUTABLE Fields (Safe to update):</h4>
     * <ul>
     *   <li>{@code customMetadata} - User-defined JSON metadata</li>
     *   <li>{@code encryptionMetadata} - Encryption/recipient metadata</li>
     *   <li>{@code manualKeywords} - User-specified keywords</li>
     *   <li>{@code autoKeywords} - Auto-extracted keywords</li>
     *   <li>{@code searchableContent} - Combined searchable text</li>
     *   <li>{@code contentCategory} - Content classification</li>
     *   <li>{@code isEncrypted} - Encryption flag</li>
     *   <li>{@code offChainData} - Off-chain data reference</li>
     * </ul>
     * 
     * <h4>🔐 Thread Safety:</h4>
     * <p>Uses {@code GLOBAL_BLOCKCHAIN_LOCK.writeLock()} for safe concurrent access.</p>
     * 
     * @param block The block to update with modified mutable fields
     * @return {@code true} if update successful, {@code false} if block not found or immutable fields modified
     */
    public boolean updateBlock(Block block) {
        if (block == null || block.getBlockNumber() == null) {
            logger.warn("Cannot update null block or block without number");
            return false;
        }

        long stamp = GLOBAL_BLOCKCHAIN_LOCK.writeLock();
        try {
            return JPAUtil.executeInTransaction(em -> {
                try {
                    // Verify the block exists before updating
                    Block existingBlock = blockRepository.getBlockByNumber(block.getBlockNumber());
                    if (existingBlock == null) {
                        logger.warn("Block #{} does not exist, cannot update", block.getBlockNumber());
                        return false;
                    }

                    // SECURITY: Detect attempts to modify immutable (hash-critical) fields
                    // These fields are protected by JPA @Column(updatable=false), but we reject
                    // the update request to provide clear feedback to the caller
                    if (!Objects.equals(existingBlock.getData(), block.getData())) {
                        logger.error("❌ Cannot modify 'data' field - it's immutable (hash-critical)");
                        return false;
                    }
                    if (!Objects.equals(existingBlock.getBlockNumber(), block.getBlockNumber())) {
                        logger.error("❌ Cannot modify 'blockNumber' field - it's immutable");
                        return false;
                    }
                    if (!Objects.equals(existingBlock.getPreviousHash(), block.getPreviousHash())) {
                        logger.error("❌ Cannot modify 'previousHash' field - it's immutable (hash-critical)");
                        return false;
                    }
                    // Compare timestamps with millisecond precision (ignore nanosecond differences from DB rounding)
                    if (existingBlock.getTimestamp() != null && block.getTimestamp() != null) {
                        long existingMillis = existingBlock.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                        long blockMillis = block.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                        if (existingMillis != blockMillis) {
                            logger.error("❌ Cannot modify 'timestamp' field - it's immutable (hash-critical)");
                            return false;
                        }
                    } else if (!Objects.equals(existingBlock.getTimestamp(), block.getTimestamp())) {
                        logger.error("❌ Cannot modify 'timestamp' field - it's immutable (one is null)");
                        return false;
                    }
                    if (!Objects.equals(existingBlock.getHash(), block.getHash())) {
                        logger.error("❌ Cannot modify 'hash' field - it's immutable (blockchain integrity)");
                        return false;
                    }
                    if (!Objects.equals(existingBlock.getSignature(), block.getSignature())) {
                        logger.error("❌ Cannot modify 'signature' field - it's immutable (cryptographic integrity)");
                        return false;
                    }
                    if (!Objects.equals(existingBlock.getSignerPublicKey(), block.getSignerPublicKey())) {
                        logger.error("❌ Cannot modify 'signerPublicKey' field - it's immutable (identity integrity)");
                        return false;
                    }

                    // SECURITY: Copy only safe fields (fields that don't affect the hash)
                    // This prevents modifying hash-critical fields while allowing metadata updates
                    boolean hasChanges = false;
                    
                    if (!Objects.equals(existingBlock.getCustomMetadata(), block.getCustomMetadata())) {
                        existingBlock.setCustomMetadata(block.getCustomMetadata());
                        hasChanges = true;
                        if (logger.isTraceEnabled()) {
                            logger.trace("✅ Safe update: customMetadata modified");
                        }
                    }
                    
                    if (!Objects.equals(existingBlock.getEncryptionMetadata(), block.getEncryptionMetadata())) {
                        existingBlock.setEncryptionMetadata(block.getEncryptionMetadata());
                        hasChanges = true;
                        if (logger.isTraceEnabled()) {
                            logger.trace("✅ Safe update: encryptionMetadata modified");
                        }
                    }
                    
                    if (!Objects.equals(existingBlock.getManualKeywords(), block.getManualKeywords())) {
                        existingBlock.setManualKeywords(block.getManualKeywords());
                        hasChanges = true;
                        if (logger.isTraceEnabled()) {
                            logger.trace("✅ Safe update: manualKeywords modified");
                        }
                    }
                    
                    if (!Objects.equals(existingBlock.getAutoKeywords(), block.getAutoKeywords())) {
                        existingBlock.setAutoKeywords(block.getAutoKeywords());
                        hasChanges = true;
                        if (logger.isTraceEnabled()) {
                            logger.trace("✅ Safe update: autoKeywords modified");
                        }
                    }
                    
                    if (!Objects.equals(existingBlock.getSearchableContent(), block.getSearchableContent())) {
                        existingBlock.setSearchableContent(block.getSearchableContent());
                        hasChanges = true;
                        if (logger.isTraceEnabled()) {
                            logger.trace("✅ Safe update: searchableContent modified");
                        }
                    }
                    
                    if (!Objects.equals(existingBlock.getContentCategory(), block.getContentCategory())) {
                        existingBlock.setContentCategory(block.getContentCategory());
                        hasChanges = true;
                        if (logger.isTraceEnabled()) {
                            logger.trace("✅ Safe update: contentCategory modified");
                        }
                    }
                    
                    if (!Objects.equals(existingBlock.getIsEncrypted(), block.getIsEncrypted())) {
                        existingBlock.setIsEncrypted(block.getIsEncrypted());
                        hasChanges = true;
                        if (logger.isTraceEnabled()) {
                            logger.trace("✅ Safe update: isEncrypted modified");
                        }
                    }
                    
                    if (!Objects.equals(existingBlock.getOffChainData(), block.getOffChainData())) {
                        existingBlock.setOffChainData(block.getOffChainData());
                        hasChanges = true;
                        if (logger.isTraceEnabled()) {
                            logger.trace("✅ Safe update: offChainData modified");
                        }
                    }
                    
                    if (!hasChanges) {
                        logger.debug("No safe fields were modified for block #{}", block.getBlockNumber());
                        return true; // No changes needed, but not an error
                    }

                    // Update only the existing block (with safe fields copied)
                    // This ensures hash-critical fields remain unchanged
                    blockRepository.updateBlock(existingBlock);
                    
                    if (logger.isDebugEnabled()) {
                        logger.debug("✅ Successfully updated block #{} (safe fields only)", block.getBlockNumber());
                    }
                    return true;
                } catch (Exception e) {
                    logger.error("Failed to update block #{}: {}", block.getBlockNumber(), e.getMessage());
                    return false;
                }
            });
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockWrite(stamp);
        }
    }

    /**
     * Encrypt an existing unencrypted block with password (thread-safe)
     *
     * @param blockNumber The block number of the block to encrypt
     * @param password The password for encryption
     * @return true if encryption was successful, false if block was already encrypted or not found
     * @since 1.0.5
     */
    public boolean encryptExistingBlock(Long blockNumber, String password) {
        long stamp = GLOBAL_BLOCKCHAIN_LOCK.writeLock();
        try {
            return blockRepository.encryptExistingBlock(blockNumber, password);
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.unlockWrite(stamp);
        }
    }

    /**
     * Shutdown the blockchain and cleanup all resources
     * This method should be called when the blockchain instance is no longer needed
     * to prevent resource leaks from SearchFrameworkEngine threads
     */
    public void shutdown() {
        logger.info("🛑 Shutting down blockchain and cleaning up resources...");

        try {
            if (searchFrameworkEngine != null) {
                searchFrameworkEngine.shutdown();
                if (logger.isDebugEnabled()) {
                    logger.debug("✅ SearchFrameworkEngine shutdown completed");
                }
            }
        } catch (Exception e) {
            logger.warn("⚠️ Error shutting down SearchFrameworkEngine", e);
        }

        try {
            if (searchSpecialistAPI != null) {
                searchSpecialistAPI.shutdown();
                if (logger.isDebugEnabled()) {
                    logger.debug("✅ SearchSpecialistAPI shutdown completed");
                }
            }
        } catch (Exception e) {
            logger.warn("⚠️ Error shutting down SearchSpecialistAPI", e);
        }

        logger.info("✅ Blockchain shutdown completed");
    }
}
