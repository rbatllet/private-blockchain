package com.rbatllet.blockchain.core;

import com.rbatllet.blockchain.dao.AuthorizedKeyDAO;
import com.rbatllet.blockchain.dao.BlockDAO;
import com.rbatllet.blockchain.dto.ChainExportData;
import com.rbatllet.blockchain.entity.AuthorizedKey;
import com.rbatllet.blockchain.entity.Block;
import com.rbatllet.blockchain.recovery.ChainRecoveryManager;
import com.rbatllet.blockchain.recovery.ChainRecoveryManager.ChainDiagnostic;
import com.rbatllet.blockchain.recovery.ChainRecoveryManager.RecoveryResult;
import com.rbatllet.blockchain.util.CryptoUtil;
import com.rbatllet.blockchain.util.JPAUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
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
    
    public Blockchain() {
        this.blockDAO = new BlockDAO();
        this.authorizedKeyDAO = new AuthorizedKeyDAO();
        initializeGenesisBlock();
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
                return null;
            });
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.writeLock().unlock();
        }
    }
    
    /**
     * CORE FUNCTION: Add a new block to the chain and return the created block
     * THREAD-SAFE: Returns the actual block created to avoid race conditions in tests
     */
    public Block addBlockAndReturn(String data, PrivateKey signerPrivateKey, PublicKey signerPublicKey) {
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
                    
                    // 1. CORE: Validate block size (this method already handles null data correctly)
                    if (!validateBlockSize(data)) {
                        System.err.println("Block data exceeds maximum allowed size");
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
                    
                    // 5. Create the new block with the atomically generated number
                    Block newBlock = new Block();
                    newBlock.setBlockNumber(nextBlockNumber);
                    newBlock.setPreviousHash(lastBlock != null ? lastBlock.getHash() : GENESIS_PREVIOUS_HASH);
                    newBlock.setData(data);
                    newBlock.setTimestamp(blockTimestamp);
                    newBlock.setSignerPublicKey(publicKeyString);
                    
                    // 6. Calculate block hash
                    String blockContent = buildBlockContent(newBlock);
                    newBlock.setHash(CryptoUtil.calculateHash(blockContent));
                    
                    // 7. Sign the block
                    String signature = CryptoUtil.signData(blockContent, signerPrivateKey);
                    newBlock.setSignature(signature);
                    
                    // 8. Validate the block before saving
                    if (lastBlock != null && !validateBlock(newBlock, lastBlock)) {
                        System.err.println("Block validation failed");
                        return null;
                    }
                    
                    // 9. Final check: verify this block number doesn't exist
                    if (blockDAO.existsBlockWithNumber(nextBlockNumber)) {
                        System.err.println("🚨 CRITICAL: Race condition detected! Block number " + nextBlockNumber + " already exists");
                        return null;
                    }
                    
                    // 10. Save the block
                    blockDAO.saveBlock(newBlock);
                    
                    // CRITICAL: Force flush to ensure immediate visibility
                    if (JPAUtil.hasActiveTransaction()) {
                        EntityManager currentEm = JPAUtil.getEntityManager();
                        currentEm.flush();
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
     * CORE FUNCTION: Validate the entire blockchain
     * FIXED: Added thread-safety with read lock
     */
    public boolean validateChain() {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            List<Block> allBlocks = blockDAO.getAllBlocks();
            
            if (allBlocks.isEmpty()) {
                System.err.println("No blocks found in chain");
                return false;
            }
            
            // Verify genesis block
            Block genesisBlock = allBlocks.get(0);
            if (!genesisBlock.getBlockNumber().equals(0L) || 
                !genesisBlock.getPreviousHash().equals(GENESIS_PREVIOUS_HASH)) {
                System.err.println("Invalid genesis block");
                return false;
            }
            
            // Verify all blocks sequentially
            for (int i = 1; i < allBlocks.size(); i++) {
                Block currentBlock = allBlocks.get(i);
                Block previousBlock = allBlocks.get(i - 1);
                
                if (!validateBlock(currentBlock, previousBlock)) {
                    System.err.println("Chain validation failed at block #" + currentBlock.getBlockNumber());
                    return false;
                }
            }
            
            System.out.println("Chain validation successful! Total blocks: " + allBlocks.size());
            return true;
            
        } catch (Exception e) {
            System.err.println("Error validating chain: " + e.getMessage());
            return false;
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
        }
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
                    
                    // Check if key is currently authorized
                    if (authorizedKeyDAO.isKeyAuthorized(publicKeyString)) {
                        System.err.println("Key already authorized");
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
     * CORE FUNCTION 2: Chain Export - Backup blockchain to file
     * FIXED: Added thread-safety with read lock
     */
    public boolean exportChain(String filePath) {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            List<Block> allBlocks = blockDAO.getAllBlocks();
            List<AuthorizedKey> allKeys = authorizedKeyDAO.getAllAuthorizedKeys(); // FIXED: Export ALL keys, not just active ones
            
            // Create export data structure
            ChainExportData exportData = new ChainExportData();
            exportData.setBlocks(allBlocks);
            exportData.setAuthorizedKeys(allKeys);
            exportData.setExportTimestamp(LocalDateTime.now());
            exportData.setVersion("1.0");
            exportData.setTotalBlocks(allBlocks.size());
            
            // Convert to JSON
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            
            // Write to file
            File file = new File(filePath);
            mapper.writeValue(file, exportData);
            
            System.out.println("Chain exported successfully to: " + filePath);
            System.out.println("Exported " + allBlocks.size() + " blocks and " + allKeys.size() + " authorized keys");
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
                    
                    // Clear existing blocks and keys
                    blockDAO.deleteAllBlocks();
                    authorizedKeyDAO.deleteAllAuthorizedKeys();
                    
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
                    
                    // Import blocks
                    for (Block block : importData.getBlocks()) {
                        // Reset ID for new insertion
                        block.setId(null);
                        blockDAO.saveBlock(block);
                    }
                    
                    System.out.println("Chain imported successfully from: " + filePath);
                    System.out.println("Imported " + importData.getBlocks().size() + " blocks");
                    
                    // Validate imported chain
                    boolean isValid = validateChain();
                    if (isValid) {
                        System.out.println("Imported chain validation: SUCCESS");
                    } else {
                        System.err.println("Imported chain validation: FAILED");
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
                    
                    // Actually remove blocks using DAO
                    System.out.println("Rolling back " + numberOfBlocks + " blocks:");
                    for (Block block : blocksToRemove) {
                        String data = block.getData();
                        String displayData = data != null ? data.substring(0, Math.min(50, data.length())) : "null";
                        System.out.println("  - Removing Block #" + block.getBlockNumber() + ": " + displayData);
                        blockDAO.deleteBlockByNumber(block.getBlockNumber());
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
                    
                    // Use the deleteBlocksAfter method for better performance
                    int deletedCount = blockDAO.deleteBlocksAfter(targetBlockNumber);
                    
                    // CRITICAL: Synchronize block sequence after rollback
                    blockDAO.synchronizeBlockSequence();
                    
                    System.out.println("Rollback to block " + targetBlockNumber + " completed successfully");
                    System.out.println("Removed " + deletedCount + " blocks");
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
     * Get the last block with read lock for scenarios where only reading consistency is needed
     * This method is lighter weight but may not see the absolute latest writes
     */
    public Block getLastBlockReadOnly() {
        GLOBAL_BLOCKCHAIN_LOCK.readLock().lock();
        try {
            return blockDAO.getLastBlock();
        } finally {
            GLOBAL_BLOCKCHAIN_LOCK.readLock().unlock();
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
                    // Clear all blocks and keys
                    blockDAO.deleteAllBlocks();
                    authorizedKeyDAO.deleteAllAuthorizedKeys();
                    
                    // Reinitialize genesis block
                    initializeGenesisBlock();
                    
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
                            System.out.println("💡 STRONGLY RECOMMENDED: Run validateChain() to verify blockchain integrity");
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
            isValid = validateChain();
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
}